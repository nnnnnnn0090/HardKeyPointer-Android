package com.nnnnnnn0090.hardkeypointer

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.graphics.Path
import android.graphics.PixelFormat
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import android.view.accessibility.AccessibilityEvent
import android.content.res.Configuration

class TapService : AccessibilityService() {
    private lateinit var windowManager: WindowManager
    private var pointerView: View? = null
    private val handler = Handler(Looper.getMainLooper())
    private val scrollHandler = Handler(Looper.getMainLooper())
    private var scrollRunnable: Runnable? = null
    private var pointerX = 500
    private var pointerY = 500
    private var moveSpeed = 10
    private var keyPressStart: Long = 0
    private val longPressThreshold = 500L

    companion object {
        private const val TAG = "KeyDetectionService"
    }

    override fun onServiceConnected() {
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        showPointer()
    }

    private fun getKeyCodes(): Map<String, Int> {
        val prefs = getSharedPreferences("com.nnnnnnn0090.hardkeypointer.PREFS", MODE_PRIVATE)
        moveSpeed = prefs.getInt(MainActivity.KEY_MOVE_SPEED, 1)
        return mapOf(
            "up" to prefs.getInt(MainActivity.KEY_UP_CODE, KeyEvent.KEYCODE_DPAD_UP),
            "down" to prefs.getInt(MainActivity.KEY_DOWN_CODE, KeyEvent.KEYCODE_DPAD_DOWN),
            "left" to prefs.getInt(MainActivity.KEY_LEFT_CODE, KeyEvent.KEYCODE_DPAD_LEFT),
            "right" to prefs.getInt(MainActivity.KEY_RIGHT_CODE, KeyEvent.KEYCODE_DPAD_RIGHT),
            "tap" to prefs.getInt(MainActivity.KEY_TAP_CODE, KeyEvent.KEYCODE_ENTER),
            "enable" to prefs.getInt(MainActivity.KEY_ENABLE_CODE, KeyEvent.KEYCODE_VOLUME_UP),
            "disable" to prefs.getInt(MainActivity.KEY_DISABLE_CODE, KeyEvent.KEYCODE_VOLUME_DOWN),
            "scrollup" to prefs.getInt(MainActivity.KEY_SCROLLUP_CODE, KeyEvent.KEYCODE_2),
            "scrolldown" to prefs.getInt(MainActivity.KEY_SCROLLDOWN_CODE, KeyEvent.KEYCODE_5),
            "scrollleft" to prefs.getInt(MainActivity.KEY_SCROLLLEFT_CODE, KeyEvent.KEYCODE_4),
            "scrollright" to prefs.getInt(MainActivity.KEY_SCROLLRIGHT_CODE, KeyEvent.KEYCODE_6)
        )
    }

    private fun handleScroll(action: () -> Unit, start: Boolean) {
        if (start) {
            scrollRunnable = Runnable { action(); scrollHandler.postDelayed(scrollRunnable!!, 300) }
            scrollHandler.post(scrollRunnable!!)
        } else {
            scrollRunnable?.let { scrollHandler.removeCallbacks(it) }
        }
    }

    private fun handleKey(event: KeyEvent, isDown: Boolean) {
        val codes = getKeyCodes()
        if (pointerView == null) return
        when (event.keyCode) {
            codes["up"], codes["down"], codes["left"], codes["right"] -> {
                if (isDown) movePointer(getDirection(event.keyCode, codes))
                else stopPointerMovement()
            }
            codes["tap"] -> if (isDown) keyPressStart = System.currentTimeMillis() else simulatePress()
            codes["scrollup"], codes["scrolldown"], codes["scrollleft"], codes["scrollright"] -> {
                val action = when (event.keyCode) {
                    codes["scrollup"] -> ::scrollUp
                    codes["scrolldown"] -> ::scrollDown
                    codes["scrollleft"] -> ::scrollLeft
                    else -> ::scrollRight
                }
                handleScroll(action, isDown)
            }
            codes["enable"] -> if (isDown) showPointer()
            codes["disable"] -> if (isDown) removePointer()
        }
    }

    private fun movePointer(delta: Pair<Int, Int>) {
        handler.post(object : Runnable {
            override fun run() {
                pointerX += delta.first
                pointerY += delta.second
                updatePointer()
                handler.postDelayed(this, moveSpeed.toLong())
            }
        })
    }

    private fun stopPointerMovement() = handler.removeCallbacksAndMessages(null)

    private fun simulatePress() {
        getPointerCoordinates()?.let { (x, y) ->
            val duration = System.currentTimeMillis() - keyPressStart
            val path = Path().apply { moveTo(x, y) }
            dispatchGesture(
                GestureDescription.Builder().addStroke(
                    GestureDescription.StrokeDescription(path, 0, duration)
                ).build(),
                object : GestureResultCallback() {
                    override fun onCompleted(gesture: GestureDescription) {
                        Log.d(TAG, if (duration >= longPressThreshold) "Long press completed" else "Tap completed")
                    }
                }, null
            )
        }
    }

    private fun getDirection(keyCode: Int, codes: Map<String, Int>): Pair<Int, Int> {
        val direction = when (keyCode) {
            codes["up"] -> 0 to -20
            codes["down"] -> 0 to 20
            codes["left"] -> -20 to 0
            else -> 20 to 0
        }
        return if (resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE)
            direction.second to -direction.first else direction
    }

    private fun showPointer() {
        if (pointerView == null) {
            pointerView = LayoutInflater.from(this).inflate(R.layout.pointer_view, null)
            windowManager.addView(
                pointerView, WindowManager.LayoutParams(
                    WindowManager.LayoutParams.WRAP_CONTENT,
                    WindowManager.LayoutParams.WRAP_CONTENT,
                    WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
                    WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE,
                    PixelFormat.TRANSLUCENT
                ).apply { x = pointerX; y = pointerY }
            )
        }
    }

    private fun updatePointer() {
        pointerView?.let {
            val params = it.layoutParams as WindowManager.LayoutParams
            params.x = pointerX
            params.y = pointerY
            windowManager.updateViewLayout(it, params)
        }
    }

    private fun removePointer() {
        pointerView?.let {
            windowManager.removeView(it)
            pointerView = null
        }
    }

    private fun scrollUp() = simulateScroll(0, -200)
    private fun scrollDown() = simulateScroll(0, 200)
    private fun scrollLeft() = simulateScroll(-200, 0)
    private fun scrollRight() = simulateScroll(200, 0)

    private fun simulateScroll(dx: Int, dy: Int) {
        getPointerCoordinates()?.let { (x, y) ->
            val path = Path().apply {
                moveTo(x, y)
                lineTo((x + dx).coerceAtLeast(0f), (y + dy).coerceAtLeast(0f))
            }
            dispatchGesture(
                GestureDescription.Builder().addStroke(
                    GestureDescription.StrokeDescription(path, 0, 300)
                ).build(), null, null
            )
        }
    }

    private fun getPointerCoordinates(): Pair<Float, Float>? {
        pointerView?.let {
            val location = IntArray(2)
            it.getLocationOnScreen(location)
            return location[0].toFloat() to location[1].toFloat()
        }
        return null
    }

    override fun onKeyEvent(event: KeyEvent?): Boolean {
        event?.let { handleKey(it, it.action == KeyEvent.ACTION_DOWN) }
        return true
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent) {}

    override fun onInterrupt() = removePointer()

    override fun onDestroy() {
        super.onDestroy()
        removePointer()
    }
}
