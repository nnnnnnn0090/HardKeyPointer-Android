package com.nnnnnnn0090.hardkeypointer

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.accessibilityservice.GestureDescription.StrokeDescription
import android.graphics.Path
import android.graphics.PixelFormat
import android.os.Handler
import android.os.Looper
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.Surface
import android.view.View
import android.view.WindowManager
import android.view.accessibility.AccessibilityEvent
import android.widget.Toast

class TapService : AccessibilityService() {
    private lateinit var windowManager: WindowManager
    private var pointerView: View? = null
    private val handler = Handler(Looper.getMainLooper())
    private val scrollRunnables = mutableMapOf<String, Runnable?>()

    private var pointerXPosition = 100
    private var pointerYPosition = 100
    private var moveSpeed = 10
    private var moveAccel = 100

    private var keyPressStartTime: Long = 0
    private var isMoving = false
    private var movementStartTime: Long = 0

    override fun onServiceConnected() {
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        showPointer()
    }

    private fun getKeyCodes(): Map<String, Int> {
        moveSpeed = SettingsManager.getMoveSpeed(this)
        moveAccel = SettingsManager.getMoveAccel(this)
        return SettingsManager.getAllKeyCodes(this)
    }

    private fun getPointerCoordinates(): Pair<Float, Float>? {
        pointerView?.let {
            val location = IntArray(2)
            it.getLocationOnScreen(location)
            return Pair(location[0].toFloat(), location[1].toFloat())
        }
        return null
    }

    private fun getRotatedDirection(direction: String, rotation: Int): String {
        return when (rotation) {
            Surface.ROTATION_90 -> when (direction) {
                "up" -> "left"
                "left" -> "down"
                "down" -> "right"
                "right" -> "up"
                else -> direction
            }
            Surface.ROTATION_180 -> when (direction) {
                "up" -> "down"
                "left" -> "right"
                "down" -> "up"
                "right" -> "left"
                else -> direction
            }
            Surface.ROTATION_270 -> when (direction) {
                "up" -> "right"
                "right" -> "down"
                "down" -> "left"
                "left" -> "up"
                else -> direction
            }
            else -> direction
        }
    }

    private fun simulateScroll(direction: String) {
        getPointerCoordinates()?.let { (x, y) ->
            val displayMetrics = resources.displayMetrics
            val screenWidth = displayMetrics.widthPixels
            val screenHeight = displayMetrics.heightPixels
            
            if (x < 0 || x >= screenWidth || y < 0 || y >= screenHeight) {
                return
            }
            
            val rotation = windowManager.defaultDisplay.rotation
            val rotatedDirection = getRotatedDirection(direction, rotation)
            val (dx, dy) = when (rotatedDirection) {
                "up" -> Pair(0, -200)
                "down" -> Pair(0, 200)
                "left" -> Pair(-200, 0)
                "right" -> Pair(200, 0)
                else -> return
            }
            
            val endX = (x + dx).coerceIn(0f, screenWidth.toFloat())
            val endY = (y + dy).coerceIn(0f, screenHeight.toFloat())
            
            val path = Path().apply {
                moveTo(x, y)
                lineTo(endX, endY)
            }
            val gesture = GestureDescription.Builder()
                .addStroke(StrokeDescription(path, 0, 150))
                .build()
            dispatchGesture(gesture, null, null)
        }
    }

    private fun startScrolling(scrollAction: (String) -> Unit, direction: String) {
        if (scrollRunnables[direction] != null) return

        val newRunnable = object : Runnable {
            override fun run() {
                scrollAction(direction)
                handler.postDelayed(this, 150)
            }
        }
        scrollRunnables[direction] = newRunnable
        handler.post(newRunnable)
    }

    private fun stopScrolling(direction: String) {
        scrollRunnables[direction]?.let {
            handler.removeCallbacks(it)
            scrollRunnables[direction] = null
        }
    }

    override fun onKeyEvent(event: KeyEvent?): Boolean {
        event?.let {
            val keyCodes = getKeyCodes()
            if (pointerView != null) {
                when (it.keyCode) {
                    keyCodes["up"], keyCodes["down"], keyCodes["left"], keyCodes["right"], keyCodes["tap"] -> {
                        when (it.action) {
                            KeyEvent.ACTION_DOWN -> handleKeyDown(it, keyCodes)
                            KeyEvent.ACTION_UP -> handleKeyUp(it, keyCodes)
                        }
                        return true
                    }
                    keyCodes["scrollup"], keyCodes["scrolldown"], keyCodes["scrollleft"], keyCodes["scrollright"] -> {
                        val direction = when (it.keyCode) {
                            keyCodes["scrollup"] -> "up"
                            keyCodes["scrolldown"] -> "down"
                            keyCodes["scrollleft"] -> "left"
                            keyCodes["scrollright"] -> "right"
                            else -> return false
                        }
                        handleScrollKey(it, direction)
                        return true
                    }
                }
            }
            if (it.keyCode == keyCodes["disable"] && it.action == KeyEvent.ACTION_DOWN) {
                if (pointerView == null) showPointer() else removePointer()
            }
        }
        return false
    }

    private fun handleScrollKey(event: KeyEvent, direction: String) {
        when (event.action) {
            KeyEvent.ACTION_DOWN -> startScrolling(::simulateScroll, direction)
            KeyEvent.ACTION_UP -> stopScrolling(direction)
        }
    }

    private fun handleKeyDown(event: KeyEvent, keyCodes: Map<String, Int>) {
        val direction = when (event.keyCode) {
            keyCodes["up"] -> "up"
            keyCodes["down"] -> "down"
            keyCodes["left"] -> "left"
            keyCodes["right"] -> "right"
            else -> null
        }

        if (direction != null) {
            val rotation = windowManager.defaultDisplay.rotation
            val rotatedDirection = getRotatedDirection(direction, rotation)
            when (rotatedDirection) {
                "up" -> movePointer(0, -1)
                "down" -> movePointer(0, 1)
                "left" -> movePointer(-1, 0)
                "right" -> movePointer(1, 0)
            }
        } else if (event.keyCode == keyCodes["tap"]) {
            keyPressStartTime = System.currentTimeMillis()
        }
    }

    private fun handleKeyUp(event: KeyEvent, keyCodes: Map<String, Int>) {
        if (event.keyCode == keyCodes["tap"]) {
            val pressDuration = System.currentTimeMillis() - keyPressStartTime
            simulatePressAtPointer(pressDuration)
        }
        stopPointerMovement()
    }

    private fun movePointer(dx: Int, dy: Int) {
        if (!isMoving) {
            isMoving = true
            movementStartTime = System.currentTimeMillis()
        }
        
        handler.post(object : Runnable {
            override fun run() {
                if (!isMoving) return
                
                val elapsed = System.currentTimeMillis() - movementStartTime
                val accel = if (moveAccel > 0) 1.0 + (elapsed * moveAccel / 10000.0) else 1.0
                
                val newX = pointerXPosition + (dx * moveSpeed * accel * 0.2).toInt()
                val newY = pointerYPosition + (dy * moveSpeed * accel * 0.2).toInt()
                
                val displayMetrics = resources.displayMetrics
                val halfWidth = displayMetrics.widthPixels / 2
                val halfHeight = displayMetrics.heightPixels / 2
                pointerXPosition = newX.coerceIn(-halfWidth + 20, halfWidth + 17)
                pointerYPosition = newY.coerceIn(-halfHeight + 30, halfHeight + 28)
                
                updatePointerPosition()
                
                handler.postDelayed(this, 16)
            }
        })
    }

    private fun stopPointerMovement() {
        isMoving = false
        handler.removeCallbacksAndMessages(null)
    }

    private fun showPointer() {
        if (pointerView == null) {
            pointerView = LayoutInflater.from(this).inflate(R.layout.pointer_view, null)
            val params = WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or 
                WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or 
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                PixelFormat.TRANSLUCENT
            ).apply {
                x = pointerXPosition
                y = pointerYPosition
            }
            windowManager.addView(pointerView, params)
            Toast.makeText(this, getString(R.string.pointer_shown), Toast.LENGTH_SHORT).show()
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
            val displayMetrics = resources.displayMetrics
            val screenWidth = displayMetrics.widthPixels
            val screenHeight = displayMetrics.heightPixels
            
            if (x < 0 || x >= screenWidth || y < 0 || y >= screenHeight) {
                return
            }
            
            val path = Path().apply { moveTo(x, y) }
            val gesture = GestureDescription.Builder()
                .addStroke(StrokeDescription(path, 0, pressDuration))
                .build()
            dispatchGesture(gesture, null, null)
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
            Toast.makeText(this, getString(R.string.pointer_removed), Toast.LENGTH_SHORT).show()
        }
    }
}
