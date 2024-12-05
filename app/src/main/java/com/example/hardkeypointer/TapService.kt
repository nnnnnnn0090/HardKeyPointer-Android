package com.nnnnnnn0090.hardkeypointer

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.accessibilityservice.GestureDescription.StrokeDescription
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

    private var pointerXPosition = 500
    private var pointerYPosition = 500
    private var moveSpeed = 10

    private var keyPressStartTime: Long = 0
    private var keyPressEndTime: Long = 0
    private val longPressThreshold: Long = 500

    companion object {
        private const val TAG = "KeyDetectionService"
    }

    override fun onServiceConnected() {
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        showPointer()
    }

    fun getKeyCodesFromPreferences(): Map<String, Int> {
        val sharedPreferences = getSharedPreferences("com.nnnnnnn0090.hardkeypointer.PREFS", MODE_PRIVATE)
        moveSpeed = sharedPreferences.getInt(MainActivity.KEY_MOVE_SPEED, 1)
        return mapOf(
            "up" to sharedPreferences.getInt(MainActivity.KEY_UP_CODE, KeyEvent.KEYCODE_DPAD_UP),
            "down" to sharedPreferences.getInt(MainActivity.KEY_DOWN_CODE, KeyEvent.KEYCODE_DPAD_DOWN),
            "left" to sharedPreferences.getInt(MainActivity.KEY_LEFT_CODE, KeyEvent.KEYCODE_DPAD_LEFT),
            "right" to sharedPreferences.getInt(MainActivity.KEY_RIGHT_CODE, KeyEvent.KEYCODE_DPAD_RIGHT),
            "tap" to sharedPreferences.getInt(MainActivity.KEY_TAP_CODE, KeyEvent.KEYCODE_ENTER),
            "enable" to sharedPreferences.getInt(MainActivity.KEY_ENABLE_CODE, KeyEvent.KEYCODE_VOLUME_UP),
            "disable" to sharedPreferences.getInt(MainActivity.KEY_DISABLE_CODE, KeyEvent.KEYCODE_VOLUME_DOWN),
            "scrollup" to sharedPreferences.getInt(MainActivity.KEY_SCROLLUP_CODE, KeyEvent.KEYCODE_1),
            "scrolldown" to sharedPreferences.getInt(MainActivity.KEY_SCROLLDOWN_CODE, KeyEvent.KEYCODE_2)
        )
    }
    private fun getPointerCoordinates(): Pair<Float, Float>? {
        pointerView?.let {
            val location = IntArray(2)
            it.getLocationOnScreen(location)
            val x = location[0].toFloat()
            val y = location[1].toFloat()
            return Pair(x, y)
        }
        return null
    }

    private fun simulateScrollUp() {
        getPointerCoordinates()?.let { (x, y) ->
            val path = Path().apply {
                moveTo(x, y)
                lineTo(x, y - 200f)
            }
            executeGesture(path, "Scroll up")
        }
    }

    private fun simulateScrollDown() {
        getPointerCoordinates()?.let { (x, y) ->
            val path = Path().apply {
                moveTo(x, y)
                lineTo(x, y + 200f)
            }
            executeGesture(path, "Scroll down")
        }
    }


    private fun executeGesture(path: Path, gestureName: String) {
        val strokeDescription = StrokeDescription(path, 0, 300)
        val gesture = GestureDescription.Builder().addStroke(strokeDescription).build()

        dispatchGesture(gesture, object : GestureResultCallback() {
            override fun onCompleted(gestureDescription: GestureDescription) {
                Log.d(TAG, "$gestureName completed")
            }

            override fun onCancelled(gestureDescription: GestureDescription) {
                Log.d(TAG, "$gestureName cancelled")
            }
        }, null)
    }


    override fun onKeyEvent(event: KeyEvent?): Boolean {
        event?.let {
            val keyCodes = getKeyCodesFromPreferences()
            if (pointerView != null) {
                Log.d(TAG, it.keyCode.toString())
                Log.d(TAG, keyCodes["scrolldown"].toString())
                when (it.keyCode) {
                    keyCodes["up"], keyCodes["down"], keyCodes["left"], keyCodes["right"], keyCodes["tap"] -> {
                        when (it.action) {
                            KeyEvent.ACTION_DOWN -> handleKeyDown(it)
                            KeyEvent.ACTION_UP -> handleKeyUp(it)
                        }
                        return true
                    }
                    keyCodes["scrollup"] -> {
                        when (it.action) {
                            KeyEvent.ACTION_DOWN -> startScrolling(::simulateScrollUp)
                            KeyEvent.ACTION_UP -> stopScrolling()
                        }
                        return true
                    }
                    keyCodes["scrolldown"] -> {
                        when (it.action) {
                            KeyEvent.ACTION_DOWN -> startScrolling(::simulateScrollDown)
                            KeyEvent.ACTION_UP -> stopScrolling()
                        }
                        return true
                    }
                }
            }
            when (it.keyCode) {
                keyCodes["enable"] -> {
                    if (it.action == KeyEvent.ACTION_DOWN) {
                        showPointer()
                    }
                }
                keyCodes["disable"] -> {
                    if (it.action == KeyEvent.ACTION_DOWN) {
                        removePointer()
                    }
                }
                else -> return super.onKeyEvent(event)
            }
        }
        return false
    }

    private fun startScrolling(scrollAction: () -> Unit) {
        scrollRunnable = object : Runnable {
            override fun run() {
                scrollAction()
                scrollHandler.postDelayed(this, 300)
            }
        }
        scrollHandler.post(scrollRunnable!!)
    }

    private fun stopScrolling() {
        scrollRunnable?.let {
            scrollHandler.removeCallbacks(it)
            scrollRunnable = null
        }
    }

    private fun getAdjustedDirection(dx: Int, dy: Int): Pair<Int, Int> {
        val rotation = resources.configuration.orientation
        return when (rotation) {
            Configuration.ORIENTATION_LANDSCAPE -> Pair(dy, -dx)
            Configuration.ORIENTATION_PORTRAIT -> Pair(dx, dy)
            else -> Pair(dx, dy)
        }
    }

    private fun handleKeyDown(event: KeyEvent) {
        val keyCodes = getKeyCodesFromPreferences()
        when (event.keyCode) {
            keyCodes["up"] -> {
                val (dx, dy) = getAdjustedDirection(0, -20)
                movePointer(dx, dy)
            }
            keyCodes["down"] -> {
                val (dx, dy) = getAdjustedDirection(0, 20)
                movePointer(dx, dy)
            }
            keyCodes["left"] -> {
                val (dx, dy) = getAdjustedDirection(-20, 0)
                movePointer(dx, dy)
            }
            keyCodes["right"] -> {
                val (dx, dy) = getAdjustedDirection(20, 0)
                movePointer(dx, dy)
            }
            keyCodes["tap"] -> keyPressStartTime = System.currentTimeMillis()
        }
    }

    private fun handleKeyUp(event: KeyEvent) {
        val keyCodes = getKeyCodesFromPreferences()
        if (event.keyCode == keyCodes["tap"]) {
            keyPressEndTime = System.currentTimeMillis()
            val pressDuration = keyPressEndTime - keyPressStartTime
            simulatePressAtPointer(pressDuration)
        }
        if (event.keyCode in keyCodes.values) {
            stopPointerMovement()
        }
    }

    private fun movePointer(dx: Int, dy: Int) {
        handler.post(object : Runnable {
            override fun run() {
                pointerXPosition += dx
                pointerYPosition += dy
                updatePointerPosition()
                handler.postDelayed(this, moveSpeed.toLong())
            }
        })
    }

    private fun stopPointerMovement() {
        handler.removeCallbacksAndMessages(null)
    }

    private fun showPointer() {
        if (pointerView == null) {
            pointerView = LayoutInflater.from(this).inflate(R.layout.pointer_view, null)
            val params = WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE,
                PixelFormat.TRANSLUCENT
            ).apply {
                x = pointerXPosition
                y = pointerYPosition
            }
            windowManager.addView(pointerView, params)
        }
    }

    private fun updatePointerPosition() {
        pointerView?.let {
            val params = it.layoutParams as WindowManager.LayoutParams
            params.x = pointerXPosition
            params.y = pointerYPosition
            windowManager.updateViewLayout(it, params)
        }
    }

    private fun simulatePressAtPointer(pressDuration: Long) {
        getPointerCoordinates()?.let { (x, y) ->
            val path = Path().apply { moveTo(x, y) }

            val strokeDescription = StrokeDescription(path, 0, pressDuration)
            val gesture = GestureDescription.Builder().addStroke(strokeDescription).build()

            dispatchGesture(gesture, object : GestureResultCallback() {
                override fun onCompleted(gestureDescription: GestureDescription) {
                    if (pressDuration >= longPressThreshold) {
                        Log.d(TAG, "Long press completed")
                    } else {
                        Log.d(TAG, "Tap completed")
                    }
                }

                override fun onCancelled(gestureDescription: GestureDescription) {
                    Log.d(TAG, "Press cancelled")
                }
            }, null)
        }
    }


    override fun onAccessibilityEvent(event: AccessibilityEvent) {}

    override fun onInterrupt() {
        removePointer()
    }

    override fun onDestroy() {
        super.onDestroy()
        removePointer()
    }

    private fun removePointer() {
        pointerView?.let {
            windowManager.removeView(it)
            pointerView = null
        }
    }
}
