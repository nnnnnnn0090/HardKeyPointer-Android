/*
 * HardKeyPointer-Android
 * Copyright (c) 2024-2026 nnnnnnn0090
 * 作者: nnnnnnn0090
 * ライセンス: リポジトリの LICENSE を参照してください。
 */
package com.nnnnnnn0090.hardkeypointer

import android.accessibilityservice.AccessibilityService
import android.hardware.display.DisplayManager
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.Display
import android.view.KeyEvent
import android.view.Surface
import android.view.WindowManager
import android.view.accessibility.AccessibilityEvent
import android.widget.Toast

/** フィルタしたハードキー入力をポインタ操作へ変換する入口です。 */
class TapService : AccessibilityService() {
    private lateinit var settings: SettingsRepository
    private lateinit var pointer: PointerOverlayController
    private lateinit var gestures: GestureController
    private lateinit var movement: PointerMovementController
    private lateinit var scrollController: ScrollController

    private val handler = Handler(Looper.getMainLooper())
    private val movementHandler = Handler(Looper.getMainLooper())
    private val capturedKeys = mutableSetOf<Int>()
    private var tapStartedAt = 0L
    private var backActionInProgress = false

    /** サービス接続時に設定、オーバーレイ、入力制御を初期化します。 */
    override fun onServiceConnected() {
        try {
            settings = SettingsRepository(applicationContext)
            val windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
            pointer = PointerOverlayController(this, windowManager)
            gestures = GestureController(
                overlay = pointer,
                scrollDistanceProvider = settings::getScrollDistance,
                rotationProvider = ::currentRotation,
                handler = handler,
                dispatch = ::dispatchGestureSafely
            )
            movement = PointerMovementController(
                handler = movementHandler,
                settingsProvider = settings::getMovementSettings,
                move = pointer::moveBy
            )
            scrollController = ScrollController(
                handler = handler,
                accessibilityScroll = AccessibilityScrollController(
                    rootProvider = { rootInActiveWindow },
                    rotationProvider = ::currentRotation
                ),
                gestures = gestures
            )
            showPointer()
        } catch (error: Exception) {
            Log.e(TAG, "Failed to initialize service", error)
        }
    }

    /** フィルタされたキーイベントを対応するポインタ操作へ振り分けます。 */
    override fun onKeyEvent(event: KeyEvent?): Boolean {
        val keyEvent = event ?: return false
        return try {
            if (handleKeyCaptureEvent(keyEvent)) return true
            if (!::settings.isInitialized) return false

            val keyCodes = settings.getKeyCodes()
            if (!pointer.isVisible) return handleHiddenPointerKey(keyEvent, keyCodes)

            pointer.refreshBounds()
            if (isToggleKey(keyEvent, keyCodes)) {
                removePointer()
                return true
            }

            when {
                isMovementOrTapKey(keyEvent, keyCodes) -> {
                    handleMovementOrTap(keyEvent, keyCodes)
                    true
                }
                isScrollKey(keyEvent, keyCodes) -> {
                    handleScroll(keyEvent, keyCodes)
                    true
                }
                keyEvent.keyCode == KeyEvent.KEYCODE_BACK -> handleBack(keyEvent)
                else -> false
            }
        } catch (error: Exception) {
            Log.e(TAG, "Error handling key event", error)
            false
        }
    }

    /** 設定画面で選択中のキーを記録し、設定直後の誤動作を防ぎます。 */
    private fun handleKeyCaptureEvent(event: KeyEvent): Boolean {
        if (KeyCaptureState.isActive) {
            when (event.action) {
                KeyEvent.ACTION_DOWN -> capturedKeys.add(event.keyCode)
                KeyEvent.ACTION_UP -> capturedKeys.remove(event.keyCode)
            }
            // false を返して MainActivity にキーを渡します。同時にキーを記録し、
            // リピート入力が設定直後の操作を実行しないようにします。
            return false
        }
        if (event.keyCode !in capturedKeys) return false
        if (event.action == KeyEvent.ACTION_UP) capturedKeys.remove(event.keyCode)
        return true
    }

    /** ポインタ非表示中は表示切替キーだけを処理します。 */
    private fun handleHiddenPointerKey(
        event: KeyEvent,
        keyCodes: Map<PointerAction, Int>
    ): Boolean {
        if (event.keyCode == keyCodes.getValue(PointerAction.TOGGLE) &&
            event.action == KeyEvent.ACTION_DOWN
        ) {
            showPointer()
            return true
        }
        return false
    }

    /** イベントがポインタ表示切替キーの押下か判定します。 */
    private fun isToggleKey(event: KeyEvent, keyCodes: Map<PointerAction, Int>): Boolean =
        event.keyCode == keyCodes.getValue(PointerAction.TOGGLE) &&
            event.action == KeyEvent.ACTION_DOWN

    /** イベントが移動またはタップ操作のキーか判定します。 */
    private fun isMovementOrTapKey(
        event: KeyEvent,
        keyCodes: Map<PointerAction, Int>
    ): Boolean =
        event.keyCode == keyCodes.getValue(PointerAction.UP) ||
            event.keyCode == keyCodes.getValue(PointerAction.DOWN) ||
            event.keyCode == keyCodes.getValue(PointerAction.LEFT) ||
            event.keyCode == keyCodes.getValue(PointerAction.RIGHT) ||
            event.keyCode == keyCodes.getValue(PointerAction.TAP)

    /** イベントがいずれかのスクロール操作キーか判定します。 */
    private fun isScrollKey(
        event: KeyEvent,
        keyCodes: Map<PointerAction, Int>
    ): Boolean =
        event.keyCode == keyCodes.getValue(PointerAction.SCROLL_UP) ||
            event.keyCode == keyCodes.getValue(PointerAction.SCROLL_DOWN) ||
            event.keyCode == keyCodes.getValue(PointerAction.SCROLL_LEFT) ||
            event.keyCode == keyCodes.getValue(PointerAction.SCROLL_RIGHT)

    /** 移動キーの押下・解放とタップの押下時間を処理します。 */
    private fun handleMovementOrTap(
        event: KeyEvent,
        keyCodes: Map<PointerAction, Int>
    ) {
        val direction = movementDirection(event.keyCode, keyCodes)
        if (direction != null) {
            when (event.action) {
                KeyEvent.ACTION_DOWN -> {
                    val rotated = direction.rotated(currentRotation())
                    movement.start(rotated.dx, rotated.dy)
                }
                KeyEvent.ACTION_UP -> movement.stop()
            }
            return
        }

        if (event.keyCode == keyCodes.getValue(PointerAction.TAP)) {
            when (event.action) {
                KeyEvent.ACTION_DOWN -> if (tapStartedAt == 0L) {
                    tapStartedAt = System.currentTimeMillis()
                }
                KeyEvent.ACTION_UP -> {
                    if (tapStartedAt != 0L) {
                        gestures.tap(System.currentTimeMillis() - tapStartedAt)
                    }
                    tapStartedAt = 0L
                }
            }
        }
    }

    /** スクロール方向の繰り返し開始・停止を処理します。 */
    private fun handleScroll(event: KeyEvent, keyCodes: Map<PointerAction, Int>) {
        val direction = when (event.keyCode) {
            keyCodes.getValue(PointerAction.SCROLL_UP) -> PointerDirection.UP
            keyCodes.getValue(PointerAction.SCROLL_DOWN) -> PointerDirection.DOWN
            keyCodes.getValue(PointerAction.SCROLL_LEFT) -> PointerDirection.LEFT
            keyCodes.getValue(PointerAction.SCROLL_RIGHT) -> PointerDirection.RIGHT
            else -> return
        }
        when (event.action) {
            KeyEvent.ACTION_DOWN -> scrollController.start(direction)
            KeyEvent.ACTION_UP -> scrollController.stop(direction)
        }
    }

    /** キーコードを画面回転前の論理移動方向へ変換します。 */
    private fun movementDirection(
        keyCode: Int,
        keyCodes: Map<PointerAction, Int>
    ): PointerDirection? = when (keyCode) {
        keyCodes.getValue(PointerAction.UP) -> PointerDirection.UP
        keyCodes.getValue(PointerAction.DOWN) -> PointerDirection.DOWN
        keyCodes.getValue(PointerAction.LEFT) -> PointerDirection.LEFT
        keyCodes.getValue(PointerAction.RIGHT) -> PointerDirection.RIGHT
        else -> null
    }

    /** 戻るキーをデバウンスし、システムの戻る操作を実行します。 */
    private fun handleBack(event: KeyEvent): Boolean {
        if (event.action == KeyEvent.ACTION_DOWN && !backActionInProgress) {
            backActionInProgress = true
            try {
                performGlobalAction(GLOBAL_ACTION_BACK)
            } catch (error: Exception) {
                Log.e(TAG, "Failed to perform global back action", error)
            }
            handler.postDelayed({ backActionInProgress = false }, BACK_DEBOUNCE_MS)
        }
        return true
    }

    /** ポインタを表示し、表示成功時だけ通知を出します。 */
    private fun showPointer() {
        if (pointer.show()) Toast.makeText(this, R.string.pointer_shown, Toast.LENGTH_SHORT).show()
    }

    /** ポインタと移動・スクロールの保留処理をまとめて停止します。 */
    private fun removePointer() {
        if (::scrollController.isInitialized) scrollController.stopAll()
        if (::movement.isInitialized) movement.stop()
        tapStartedAt = 0L
        if (::pointer.isInitialized && pointer.hide()) {
            Toast.makeText(this, R.string.pointer_removed, Toast.LENGTH_SHORT).show()
        }
    }

    /** ジェスチャー実行時のサービス例外を捕捉して安全に処理します。 */
    private fun dispatchGestureSafely(
        gesture: android.accessibilityservice.GestureDescription,
        callback: android.accessibilityservice.AccessibilityService.GestureResultCallback?
    ): Boolean {
        try {
            return dispatchGesture(gesture, callback, handler)
        } catch (error: Exception) {
            Log.e(TAG, "Failed to dispatch gesture", error)
            return false
        }
    }

    /** 現在のメインディスプレイ回転を取得します。 */
    private fun currentRotation(): Int = getSystemService(DisplayManager::class.java)
        ?.getDisplay(Display.DEFAULT_DISPLAY)?.rotation ?: Surface.ROTATION_0

    /** アクセシビリティイベント自体は利用しないため何もしません。 */
    override fun onAccessibilityEvent(event: AccessibilityEvent) = Unit

    /** サービス中断時に入力記録とポインタを解放します。 */
    override fun onInterrupt() {
        capturedKeys.clear()
        if (::pointer.isInitialized) removePointer()
    }

    /** サービス破棄時に全コールバック、入力状態、オーバーレイを解放します。 */
    override fun onDestroy() {
        capturedKeys.clear()
        if (::pointer.isInitialized) removePointer()
        handler.removeCallbacksAndMessages(null)
        movementHandler.removeCallbacksAndMessages(null)
        super.onDestroy()
    }

    companion object {
        private const val TAG = "TapService"
        private const val BACK_DEBOUNCE_MS = 300L
    }
}
