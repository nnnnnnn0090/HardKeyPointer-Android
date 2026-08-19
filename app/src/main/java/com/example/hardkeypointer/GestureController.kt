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
    private val spatialSettingsProvider: () -> SpatialSettings,
    private val scrollDurationProvider: () -> Int,
    private val zoomDurationProvider: () -> Int,
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
        val settings = spatialSettingsProvider()
        val distance = distanceInPixels(
            value = settings.scrollDistance,
            mode = settings.coordinateMode,
            referencePixels = if (actualDirection.dx != 0) screenWidth else screenHeight
        )
        val end = PointF(
            (start.x + actualDirection.dx * distance).coerceIn(0f, screenWidth.toFloat() - 1f),
            (start.y + actualDirection.dy * distance).coerceIn(0f, screenHeight.toFloat() - 1f)
        )
        if (start.x == end.x && start.y == end.y) return

        val path = Path().apply {
            moveTo(start.x, start.y)
            lineTo(end.x, end.y)
        }
        val duration = scrollDurationProvider()
            .coerceIn(MIN_SCROLL_DURATION_MS, MAX_SCROLL_DURATION_MS)
            .toLong()
        dispatch(buildGesture(path, duration))
    }

    /** 2本の指を広げ、対応アプリの表示を拡大します。 */
    fun zoomIn() {
        pinch(expand = true)
    }

    /** 2本の指を近づけ、対応アプリの表示を縮小します。 */
    fun zoomOut() {
        pinch(expand = false)
    }

    /** 指の開始位置と終了位置から2本指ピンチジェスチャーを作成します。 */
    private fun pinch(expand: Boolean) {
        val center = overlay.centerPoint() ?: return
        val (screenWidth, screenHeight) = overlay.screenSize()
        if (!isOnScreen(center, screenWidth, screenHeight)) return

        val horizontalSpace = minOf(center.x, screenWidth - 1f - center.x).coerceAtLeast(0f)
        val verticalSpace = minOf(center.y, screenHeight - 1f - center.y).coerceAtLeast(0f)
        val availableSpace = maxOf(horizontalSpace, verticalSpace)
        if (availableSpace <= 1f) return

        val settings = spatialSettingsProvider()
        val configuredRadius = distanceInPixels(
            value = settings.zoomAmount,
            mode = settings.coordinateMode,
            referencePixels = minOf(screenWidth, screenHeight)
        )
        val safeRadius = configuredRadius
            .coerceAtMost(availableSpace)
        if (safeRadius <= 1f) return
        val innerRadius = safeRadius * PINCH_INNER_RADIUS_RATIO
        val outerRadius = if (expand) safeRadius else innerRadius
        val innerStartRadius = if (expand) innerRadius else safeRadius
        val useHorizontal = horizontalSpace >= verticalSpace
        val leftStart = clampPoint(
            pointAlongAxis(center, -innerStartRadius, useHorizontal),
            screenWidth,
            screenHeight
        )
        val rightStart = clampPoint(
            pointAlongAxis(center, innerStartRadius, useHorizontal),
            screenWidth,
            screenHeight
        )
        val leftEnd = clampPoint(
            pointAlongAxis(center, -outerRadius, useHorizontal),
            screenWidth,
            screenHeight
        )
        val rightEnd = clampPoint(
            pointAlongAxis(center, outerRadius, useHorizontal),
            screenWidth,
            screenHeight
        )

        if (leftStart == leftEnd || rightStart == rightEnd) return
        val gesture = GestureDescription.Builder()
            .addStroke(buildStroke(leftStart, leftEnd))
            .addStroke(buildStroke(rightStart, rightEnd))
            .build()
        dispatch(gesture)
    }

    /** モードに応じた距離設定を現在の画面ピクセルへ変換します。 */
    private fun distanceInPixels(
        value: Int,
        mode: CoordinateMode,
        referencePixels: Int
    ): Float = if (mode == CoordinateMode.PIXELS) {
        value.toFloat()
    } else {
        referencePixels * value / RATIO_SCALE
    }

    /** 中心点から水平または垂直方向へずらした座標を返します。 */
    private fun pointAlongAxis(center: PointF, offset: Float, horizontal: Boolean): PointF =
        if (horizontal) PointF(center.x + offset, center.y)
        else PointF(center.x, center.y + offset)

    /** 2点間を一定時間で移動するストロークを作成します。 */
    private fun buildStroke(start: PointF, end: PointF): StrokeDescription {
        val path = Path().apply {
            moveTo(start.x, start.y)
            lineTo(end.x, end.y)
        }
        val duration = zoomDurationProvider()
            .coerceIn(MIN_PINCH_DURATION_MS, MAX_PINCH_DURATION_MS)
            .toLong()
        return StrokeDescription(path, 0, duration)
    }

    /** 指定点を画面内の有効な座標へ収めます。 */
    private fun clampPoint(point: PointF, width: Int, height: Int): PointF = PointF(
        point.x.coerceIn(0f, width.toFloat() - 1f),
        point.y.coerceIn(0f, height.toFloat() - 1f)
    )

    /** パスと押下時間から Android のジェスチャー定義を作成します。 */
    private fun buildGesture(path: Path, duration: Long): GestureDescription =
        GestureDescription.Builder()
            .addStroke(StrokeDescription(path, 0, duration))
            .build()

    /** ジェスチャー開始点が現在の画面内にあるか確認します。 */
    private fun isOnScreen(point: PointF, width: Int, height: Int): Boolean =
        point.x in 0f..width.toFloat() && point.y in 0f..height.toFloat()

    companion object {
        private const val PINCH_INNER_RADIUS_RATIO = 0.45f
        private const val RATIO_SCALE = 100f
        private const val MIN_PINCH_DURATION_MS = 100
        private const val MAX_PINCH_DURATION_MS = 1_000
        private const val MIN_SCROLL_DURATION_MS = 50
        private const val MAX_SCROLL_DURATION_MS = 500
        private const val MIN_GESTURE_DURATION = 1L
        private const val MAX_GESTURE_DURATION = 60_000L
    }
}
