/*
 * HardKeyPointer-Android
 * Copyright (c) 2024-2026 nnnnnnn0090
 * 作者: nnnnnnn0090
 * ライセンス: リポジトリの LICENSE を参照してください。
 */
package com.nnnnnnn0090.hardkeypointer

import android.accessibilityservice.GestureDescription
import android.accessibilityservice.GestureDescription.StrokeDescription
import android.graphics.Path
import android.graphics.PointF

/** ポインタ操作を画面座標のユーザー補助ジェスチャーへ変換します。 */
class GestureController(
    private val overlay: PointerOverlayController,
    private val scrollDistanceProvider: () -> Int,
    private val rotationProvider: () -> Int,
    private val dispatch: (GestureDescription) -> Unit
) {
    /** ポインタの見た目上の中心を押下します。 */
    fun tap(pressDuration: Long) {
        val point = overlay.centerPoint() ?: return
        val (screenWidth, screenHeight) = overlay.screenSize()
        val x = point.x.coerceIn(0f, screenWidth.toFloat() - 1f)
        val y = point.y.coerceIn(0f, screenHeight.toFloat() - 1f)
        val path = Path().apply { moveTo(x, y) }
        dispatch(buildGesture(path, pressDuration.coerceIn(MIN_GESTURE_DURATION, MAX_GESTURE_DURATION)))
    }

    /** 画面回転を考慮した指定方向へ短いドラッグを送ります。 */
    fun scroll(direction: PointerDirection) {
        val start = overlay.centerPoint() ?: return
        val (screenWidth, screenHeight) = overlay.screenSize()
        if (!isOnScreen(start, screenWidth, screenHeight)) return

        val actualDirection = direction.rotated(rotationProvider())
        val distance = scrollDistanceProvider()
        val end = PointF(
            (start.x + actualDirection.dx * distance).coerceIn(0f, screenWidth.toFloat() - 1f),
            (start.y + actualDirection.dy * distance).coerceIn(0f, screenHeight.toFloat() - 1f)
        )
        if (start.x == end.x && start.y == end.y) return

        val path = Path().apply {
            moveTo(start.x, start.y)
            lineTo(end.x, end.y)
        }
        dispatch(buildGesture(path, SCROLL_DURATION_MS))
    }

    /** パスと押下時間から Android のジェスチャー定義を作成します。 */
    private fun buildGesture(path: Path, duration: Long): GestureDescription =
        GestureDescription.Builder()
            .addStroke(StrokeDescription(path, 0, duration))
            .build()

    /** ジェスチャー開始点が現在の画面内にあるか確認します。 */
    private fun isOnScreen(point: PointF, width: Int, height: Int): Boolean =
        point.x in 0f..width.toFloat() && point.y in 0f..height.toFloat()

    companion object {
        private const val SCROLL_DURATION_MS = 150L
        private const val MIN_GESTURE_DURATION = 1L
        private const val MAX_GESTURE_DURATION = 60_000L
    }
}
