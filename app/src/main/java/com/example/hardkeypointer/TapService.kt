package com.nnnnnnn0090.hardkeypointer

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.accessibilityservice.GestureDescription.StrokeDescription
import android.graphics.Path
import android.graphics.PixelFormat
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.KeyEvent
import android.view.WindowManager
import android.content.Intent
import android.view.accessibility.AccessibilityEvent

class TapService : AccessibilityService() {
    private lateinit var windowManager: WindowManager
    private var pointerView: View? = null
    private val handler = Handler(Looper.getMainLooper())

    private var pointerXPosition = 500
    private var pointerYPosition = 500
    private var moveSpeed = 10

    private var keyCodes = mutableMapOf(
        "up" to KeyEvent.KEYCODE_DPAD_UP,
        "down" to KeyEvent.KEYCODE_DPAD_DOWN,
        "left" to KeyEvent.KEYCODE_DPAD_LEFT,
        "right" to KeyEvent.KEYCODE_DPAD_RIGHT,
        "tap" to KeyEvent.KEYCODE_ENTER,
        "enable" to KeyEvent.KEYCODE_VOLUME_UP,
        "disable" to KeyEvent.KEYCODE_VOLUME_DOWN
    )

    private var keyPressStartTime: Long = 0
    private var keyPressEndTime: Long = 0
    private val longPressThreshold: Long = 500 // milliseconds to detect long press

    companion object {
        private const val TAG = "KeyDetectionService"
    }

    override fun onServiceConnected() {
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        showPointer()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        intent?.let {
            keyCodes["up"] = it.getIntExtra(MainActivity.KEY_UP_CODE, KeyEvent.KEYCODE_DPAD_UP)
            keyCodes["down"] = it.getIntExtra(MainActivity.KEY_DOWN_CODE, KeyEvent.KEYCODE_DPAD_DOWN)
            keyCodes["left"] = it.getIntExtra(MainActivity.KEY_LEFT_CODE, KeyEvent.KEYCODE_DPAD_LEFT)
            keyCodes["right"] = it.getIntExtra(MainActivity.KEY_RIGHT_CODE, KeyEvent.KEYCODE_DPAD_RIGHT)
            keyCodes["tap"] = it.getIntExtra(MainActivity.KEY_TAP_CODE, KeyEvent.KEYCODE_ENTER)
            keyCodes["enable"] = it.getIntExtra(MainActivity.KEY_ENABLE_CODE, KeyEvent.KEYCODE_VOLUME_UP)
            keyCodes["disable"] = it.getIntExtra(MainActivity.KEY_DISABLE_CODE, KeyEvent.KEYCODE_VOLUME_DOWN)

            moveSpeed = it.getIntExtra(MainActivity.KEY_MOVE_SPEED, 10)
        }
        return START_STICKY
    }

    override fun onKeyEvent(event: KeyEvent?): Boolean {
        event?.let {
            Log.d(TAG, "EventKeycode: ${it.keyCode}")
            if (pointerView != null) {
                when (it.keyCode) {
                    keyCodes["up"], keyCodes["down"], keyCodes["left"], keyCodes["right"], keyCodes["tap"] -> {
                        when (it.action) {
                            KeyEvent.ACTION_DOWN -> handleKeyDown(it)
                            KeyEvent.ACTION_UP -> handleKeyUp(it)
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
                else -> {
                    return super.onKeyEvent(event)
                }
            }
        }
        return false
    }

    private fun handleKeyDown(event: KeyEvent) {
        when (event.keyCode) {
            keyCodes["up"] -> movePointer(0, -10)
            keyCodes["down"] -> movePointer(0, 10)
            keyCodes["left"] -> movePointer(-10, 0)
            keyCodes["right"] -> movePointer(10, 0)
            keyCodes["tap"] -> {
                keyPressStartTime = System.currentTimeMillis() // タップ開始時の時間を記録
            }
        }
    }

    private fun handleKeyUp(event: KeyEvent) {
        if (event.keyCode == keyCodes["tap"]) {
            keyPressEndTime = System.currentTimeMillis() // キーが離された時間を記録
            val pressDuration = keyPressEndTime - keyPressStartTime // 実際の押された時間
            simulatePressAtPointer(pressDuration) // 長押しかタップかを判定して実行
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
        pointerView?.let {
            val location = IntArray(2)
            it.getLocationOnScreen(location)
            val x = location[0].toFloat()
            val y = location[1].toFloat()

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
