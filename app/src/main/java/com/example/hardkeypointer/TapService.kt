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
import android.view.ViewConfiguration
import android.view.WindowManager
import android.view.accessibility.AccessibilityEvent
import android.widget.Toast

/** フィルタしたハードキー入力をポインタ操作へ変換する入口です。 */
class TapService : AccessibilityService() {
    private lateinit var settings: SettingsRepository
    private lateinit var pointer: PointerOverlayController
    private lateinit var gestures: GestureController
    private lateinit var movement: PointerMovementController
    private lateinit var scrollRepeater: ScrollRepeater
    private lateinit var zoomRepeater: ZoomRepeater

    private val handler = Handler(Looper.getMainLooper())
    private val movementHandler = Handler(Looper.getMainLooper())
    private val capturedKeys = mutableSetOf<Int>()
    private val pressedActions = mutableSetOf<PointerAction>()
    private val activatedActions = mutableSetOf<PointerAction>()
    private val longPressTasks = mutableMapOf<PointerAction, Runnable>()
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
                spatialSettingsProvider = settings::getSpatialSettings,
                scrollDurationProvider = { scrollIntervalMillis(settings.getScrollSpeed()).toInt() },
                zoomDurationProvider = settings::getZoomDuration,
                rotationProvider = ::currentRotation,
                dispatch = ::dispatchGestureSafely
            )
            movement = PointerMovementController(
                handler = movementHandler,
                settingsProvider = settings::getMovementSettings,
                screenSizeProvider = pointer::screenSize,
                move = pointer::moveBy
            )
            scrollRepeater = ScrollRepeater(
                handler = handler,
                onScroll = gestures::scroll,
                intervalProvider = { scrollIntervalMillis(settings.getScrollSpeed()).toInt() }
            )
            zoomRepeater = ZoomRepeater(
                handler = handler,
                onZoom = { expand ->
                    if (expand) gestures.zoomIn() else gestures.zoomOut()
                },
                intervalProvider = { settings.getZoomDuration() }
            )
            showPointer()
            returnToAppIfRequested()
        } catch (error: Exception) {
            Log.e(TAG, "Failed to initialize service", error)
        }
    }

    /** 設定画面からサービスを有効化した場合だけアプリへ戻します。 */
    private fun returnToAppIfRequested() {
        if (!AccessibilityUtils.consumeReturnToAppRequest(this)) return
        try {
            AccessibilityUtils.returnToMainActivity(this)
        } catch (error: Exception) {
            Log.w(TAG, "Failed to return to the main activity", error)
        }
    }

    /** フィルタされたキーイベントを対応するポインタ操作へ振り分けます。 */
    override fun onKeyEvent(event: KeyEvent?): Boolean {
        val keyEvent = event ?: return false
        return try {
            if (handleKeyCaptureEvent(keyEvent)) return true
            if (!::settings.isInitialized || !::pointer.isInitialized) return false

            val keyCodes = settings.getKeyCodes()
            val action = configuredAction(keyEvent.keyCode, keyCodes)
            if (!pointer.isVisible && action != PointerAction.TOGGLE) {
                if (keyEvent.action == KeyEvent.ACTION_UP && action != null) {
                    cancelActionState(action)
                }
                return false
            }
            if (action != null) return handleConfiguredAction(action, keyEvent)
            if (keyEvent.keyCode == KeyEvent.KEYCODE_BACK) return handleBack(keyEvent)
            false
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

    /** キーコードに対応する操作を検索します。 */
    private fun configuredAction(
        keyCode: Int,
        keyCodes: Map<PointerAction, Int>
    ): PointerAction? = PointerAction.entries.firstOrNull { action ->
        keyCodes[action] == keyCode && keyCode != SettingsRepository.NOT_SET
    }

    /** 設定された発動方式に従ってキーイベントを処理します。 */
    private fun handleConfiguredAction(action: PointerAction, event: KeyEvent): Boolean {
        when (event.action) {
            KeyEvent.ACTION_DOWN -> {
                if (!pressedActions.add(action)) return true
                if (settings.getTriggerMode(action) == TriggerMode.IMMEDIATE) {
                    activateAction(action)
                } else {
                    scheduleLongPress(action)
                }
            }
            KeyEvent.ACTION_UP -> {
                if (!pressedActions.remove(action)) return true
                cancelLongPress(action)
                if (activatedActions.remove(action)) deactivateAction(action)
            }
        }
        return true
    }

    /** 長押し時間を経過した操作を発動するタスクを登録します。 */
    private fun scheduleLongPress(action: PointerAction) {
        val task = Runnable {
            longPressTasks.remove(action)
            if (pressedActions.contains(action)) activateAction(action)
        }
        longPressTasks[action] = task
        handler.postDelayed(task, ViewConfiguration.getLongPressTimeout().toLong())
    }

    /** 長押し待ちタスクを解除します。 */
    private fun cancelLongPress(action: PointerAction) {
        longPressTasks.remove(action)?.let(handler::removeCallbacks)
    }

    /** 操作を発動し、キーを押している間の処理を開始します。 */
    private fun activateAction(action: PointerAction) {
        if (!activatedActions.add(action)) return
        when (action) {
            PointerAction.UP,
            PointerAction.DOWN,
            PointerAction.LEFT,
            PointerAction.RIGHT -> {
                val direction = movementDirection(action).rotated(currentRotation())
                movement.start(direction.dx, direction.dy)
            }
            PointerAction.TAP -> tapStartedAt = System.currentTimeMillis()
            PointerAction.TOGGLE -> if (pointer.isVisible) removePointer() else showPointer()
            PointerAction.SCROLL_UP,
            PointerAction.SCROLL_DOWN,
            PointerAction.SCROLL_LEFT,
            PointerAction.SCROLL_RIGHT -> scrollRepeater.start(scrollDirection(action))
            PointerAction.ZOOM_IN -> zoomRepeater.start(expand = true)
            PointerAction.ZOOM_OUT -> zoomRepeater.start(expand = false)
        }
    }

    /** 操作キーの解放時に継続処理を停止し、タップを確定します。 */
    private fun deactivateAction(action: PointerAction) {
        when (action) {
            PointerAction.UP,
            PointerAction.DOWN,
            PointerAction.LEFT,
            PointerAction.RIGHT -> movement.stop()
            PointerAction.TAP -> {
                if (tapStartedAt != 0L) {
                    gestures.tap(System.currentTimeMillis() - tapStartedAt)
                }
                tapStartedAt = 0L
            }
            PointerAction.SCROLL_UP,
            PointerAction.SCROLL_DOWN,
            PointerAction.SCROLL_LEFT,
            PointerAction.SCROLL_RIGHT -> scrollRepeater.stop(scrollDirection(action))
            PointerAction.TOGGLE -> Unit
            PointerAction.ZOOM_IN -> zoomRepeater.stop(expand = true)
            PointerAction.ZOOM_OUT -> zoomRepeater.stop(expand = false)
        }
    }

    /** 操作の保留状態を解除し、発動前の長押しも取り消します。 */
    private fun cancelActionState(action: PointerAction) {
        pressedActions.remove(action)
        cancelLongPress(action)
        if (activatedActions.remove(action)) deactivateAction(action)
    }

    /** すべての操作キー状態と長押し待ちタスクを解除します。 */
    private fun clearActionStates() {
        longPressTasks.values.toList().forEach(handler::removeCallbacks)
        longPressTasks.clear()
        pressedActions.clear()
        activatedActions.clear()
        tapStartedAt = 0L
        if (::scrollRepeater.isInitialized) scrollRepeater.stopAll()
        if (::zoomRepeater.isInitialized) zoomRepeater.stopAll()
    }

    /** 移動操作を論理方向へ変換します。 */
    private fun movementDirection(action: PointerAction): PointerDirection = when (action) {
        PointerAction.UP -> PointerDirection.UP
        PointerAction.DOWN -> PointerDirection.DOWN
        PointerAction.LEFT -> PointerDirection.LEFT
        PointerAction.RIGHT -> PointerDirection.RIGHT
        else -> error("Not a movement action: $action")
    }

    /** スクロール操作を方向へ変換します。 */
    private fun scrollDirection(action: PointerAction): PointerDirection = when (action) {
        PointerAction.SCROLL_UP -> PointerDirection.UP
        PointerAction.SCROLL_DOWN -> PointerDirection.DOWN
        PointerAction.SCROLL_LEFT -> PointerDirection.LEFT
        PointerAction.SCROLL_RIGHT -> PointerDirection.RIGHT
        else -> error("Not a scroll action: $action")
    }

    /** スクロール速度レベルをジェスチャー間隔へ変換します。 */
    private fun scrollIntervalMillis(speed: Int): Long =
        MAX_SCROLL_INTERVAL_MS -
            (speed.coerceIn(MIN_SCROLL_SPEED, MAX_SCROLL_SPEED) - MIN_SCROLL_SPEED) *
            (MAX_SCROLL_INTERVAL_MS - MIN_SCROLL_INTERVAL_MS).toLong() /
            (MAX_SCROLL_SPEED - MIN_SCROLL_SPEED)

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
        if (::scrollRepeater.isInitialized) scrollRepeater.stopAll()
        if (::movement.isInitialized) movement.stop()
        clearActionStates()
        if (::pointer.isInitialized && pointer.hide()) {
            Toast.makeText(this, R.string.pointer_removed, Toast.LENGTH_SHORT).show()
        }
    }

    /** ジェスチャー実行時のサービス例外を捕捉して安全に処理します。 */
    private fun dispatchGestureSafely(gesture: android.accessibilityservice.GestureDescription) {
        try {
            dispatchGesture(gesture, null, null)
        } catch (error: Exception) {
            Log.e(TAG, "Failed to dispatch gesture", error)
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
        private const val MIN_SCROLL_INTERVAL_MS = 50L
        private const val MAX_SCROLL_INTERVAL_MS = 500L
        private const val MIN_SCROLL_SPEED = 1
    private const val MAX_SCROLL_SPEED = 10
    }
}
