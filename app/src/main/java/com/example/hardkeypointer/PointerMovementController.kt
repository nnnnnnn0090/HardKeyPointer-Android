/*
 * HardKeyPointer-Android
 * Copyright (c) 2024-2026 nnnnnnn0090
 * 作者: nnnnnnn0090
 * ライセンス: リポジトリの LICENSE を参照してください。
 */
package com.nnnnnnn0090.hardkeypointer

import android.os.Handler

/** メインスレッドの一定間隔で加速付きポインタ移動を実行します。 */
class PointerMovementController(
    private val handler: Handler,
    private val settingsProvider: () -> MovementSettings,
    private val screenSizeProvider: () -> Pair<Int, Int>,
    private val move: (Int, Int) -> Unit
) {
    private var directionX = 0
    private var directionY = 0
    private var startedAt = 0L
    private var isMoving = false

    private val movementTask = object : Runnable {
        /** 現在の設定と経過時間から1フレーム分の移動を実行します。 */
        override fun run() {
            if (!isMoving) return
            val settings = settingsProvider()
            val elapsed = System.currentTimeMillis() - startedAt
            val acceleration = if (settings.acceleration > 0) {
                1.0 + (elapsed * settings.acceleration / ACCELERATION_SCALE)
            } else {
                1.0
            }
            // 各フレームで最新設定を読み、サービスを再起動せずに
            // スライダー変更を即時反映します。
            val (screenWidth, screenHeight) = screenSizeProvider()
            val axisPixels = if (directionX != 0) screenWidth else screenHeight
            val baseDistance = if (settings.coordinateMode == CoordinateMode.PIXELS) {
                settings.speed * FRAME_FACTOR
            } else {
                axisPixels.coerceAtLeast(1).toFloat() * settings.speed / RATIO_SCALE *
                    FRAME_INTERVAL_MS.toFloat() / MILLIS_PER_SECOND
            }
            val distance = (baseDistance * acceleration).toInt().coerceAtLeast(1)
            move(directionX * distance, directionY * distance)
            handler.postDelayed(this, FRAME_INTERVAL_MS)
        }
    }

    /** キーを押している間の繰り返しタスクを1つだけ開始します。 */
    fun start(directionX: Int, directionY: Int) {
        this.directionX = directionX
        this.directionY = directionY
        if (isMoving) return

        isMoving = true
        startedAt = System.currentTimeMillis()
        handler.post(movementTask)
    }

    /** 移動を停止し、予約済みフレーム処理も削除します。 */
    fun stop() {
        isMoving = false
        handler.removeCallbacks(movementTask)
    }

    companion object {
        private const val FRAME_INTERVAL_MS = 16L
        private const val ACCELERATION_SCALE = 10_000.0
        private const val FRAME_FACTOR = 0.2f
        private const val RATIO_SCALE = 100f
        private const val MILLIS_PER_SECOND = 1_000f
    }
}
