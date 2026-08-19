/*
 * HardKeyPointer-Android
 * Copyright (c) 2024-2026 nnnnnnn0090
 * 作者: nnnnnnn0090
 * ライセンス: リポジトリの LICENSE を参照してください。
 */
package com.nnnnnnn0090.hardkeypointer

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.accessibilityservice.GestureDescription.StrokeDescription
import android.graphics.Path
import android.graphics.PointF
import android.os.Build
import android.os.Handler

/** ポインタ位置を基準に、タップと継続スクロールのジェスチャーを作成します。 */
class GestureController(
    private val overlay: PointerOverlayController,
    private val scrollDistanceProvider: () -> Int,
    private val rotationProvider: () -> Int,
    private val handler: Handler,
    private val dispatch: (
        GestureDescription,
        AccessibilityService.GestureResultCallback?
    ) -> Boolean
) {
    private val continuousSessions = mutableMapOf<PointerDirection, ScrollSession>()

    /** ポインタの見た目上の中心を押下します。 */
    fun tap(pressDuration: Long) {
        val point = overlay.centerPoint() ?: return
        val (screenWidth, screenHeight) = overlay.screenSize()
        val x = point.x.coerceIn(0f, screenWidth.toFloat() - 1f)
        val y = point.y.coerceIn(0f, screenHeight.toFloat() - 1f)
        val path = Path().apply { moveTo(x, y) }
        dispatch(
            buildGesture(path, pressDuration.coerceIn(MIN_GESTURE_DURATION, MAX_GESTURE_DURATION)),
            null
        )
    }

    /** Android 8.0以上では指を保持し、それ未満では短い操作を連続実行します。 */
    fun startContinuousScroll(direction: PointerDirection) {
        if (continuousSessions.containsKey(direction)) return
        val session = ScrollSession(direction)
        continuousSessions[direction] = session
        dispatchNext(session)
    }

    /** 継続中のスクロールを停止し、必要なら最後に指を離します。 */
    fun stopContinuousScroll(direction: PointerDirection) {
        val session = continuousSessions[direction] ?: return
        session.pressed = false
        if (!session.dispatchInProgress) finishSession(session)
    }

    /** 全方向の継続スクロールを停止します。 */
    fun stopAllContinuousScroll() {
        continuousSessions.keys.toList().forEach(::stopContinuousScroll)
    }

    /** 前回ジェスチャー完了後に、同じセッションの次の区間を送ります。 */
    private fun dispatchNext(session: ScrollSession) {
        if (
            continuousSessions[session.direction] !== session ||
            session.finishing ||
            session.dispatchInProgress
        ) return
        val start = session.currentPoint ?: overlay.centerPoint() ?: run {
            continuousSessions.remove(session.direction)
            return
        }
        val segment = createScrollSegment(session.direction, start) ?: run {
            continuousSessions.remove(session.direction)
            return
        }
        val shouldContinue = supportsStrokeContinuation && session.pressed
        val stroke = if (shouldContinue && session.stroke != null) {
            session.stroke!!.continueStroke(segment.path, 0, SCROLL_DURATION_MS, true)
        } else {
            createStroke(segment.path, SCROLL_DURATION_MS, shouldContinue)
        }
        session.currentPoint = segment.end
        session.stroke = stroke
        session.dispatchInProgress = true
        val callback = gestureCallback(session)
        if (!dispatch(buildGesture(segment.path, SCROLL_DURATION_MS, stroke), callback)) {
            session.dispatchInProgress = false
            retryOrFinish(session)
        }
    }

    /** ジェスチャー完了後に継続または終了を選択するコールバックを作ります。 */
    private fun gestureCallback(session: ScrollSession): AccessibilityService.GestureResultCallback =
        object : AccessibilityService.GestureResultCallback() {
            /** 区間が完了したら、押下中なら次の区間へ進みます。 */
            override fun onCompleted(gestureDescription: GestureDescription) {
                if (continuousSessions[session.direction] !== session) return
                session.dispatchInProgress = false
                if (session.pressed) dispatchNext(session) else finishSession(session)
            }

            /** キャンセル時は短い待ち時間の後に再試行します。 */
            override fun onCancelled(gestureDescription: GestureDescription) {
                if (continuousSessions[session.direction] !== session) return
                session.dispatchInProgress = false
                retryOrFinish(session)
            }
        }

    /** 送信失敗やキャンセル後の再試行、またはセッション終了を処理します。 */
    private fun retryOrFinish(session: ScrollSession) {
        if (!session.pressed) {
            finishSession(session)
            return
        }
        if (session.retryScheduled) return
        session.retryScheduled = true
        handler.postDelayed({
            session.retryScheduled = false
            dispatchNext(session)
        }, RETRY_DELAY_MS)
    }

    /** 継続ストロークを終了して、画面上の仮想タッチを解放します。 */
    private fun finishSession(session: ScrollSession) {
        if (continuousSessions[session.direction] !== session) return
        if (!supportsStrokeContinuation || session.stroke == null || !session.stroke!!.willContinue()) {
            continuousSessions.remove(session.direction)
            return
        }
        if (session.finishing) return
        session.finishing = true
        val point = session.currentPoint ?: run {
            continuousSessions.remove(session.direction)
            return
        }
        val path = Path().apply { moveTo(point.x, point.y) }
        val finalStroke = session.stroke!!.continueStroke(path, 0, 1L, false)
        val callback = object : AccessibilityService.GestureResultCallback() {
            /** 指を離す最終区間が完了したらセッションを破棄します。 */
            override fun onCompleted(gestureDescription: GestureDescription) {
                continuousSessions.remove(session.direction)
            }

            /** 最終区間がキャンセルされてもセッションを破棄します。 */
            override fun onCancelled(gestureDescription: GestureDescription) {
                continuousSessions.remove(session.direction)
            }
        }
        if (!dispatch(buildGesture(path, 1L, finalStroke), callback)) {
            continuousSessions.remove(session.direction)
        }
    }

    /** 指定方向のスクロール区間を画面内に収めて作成します。 */
    private fun createScrollSegment(
        direction: PointerDirection,
        start: PointF?
    ): ScrollSegment? {
        val origin = start ?: return null
        val (screenWidth, screenHeight) = overlay.screenSize()
        if (!isOnScreen(origin, screenWidth, screenHeight)) return null
        val actualDirection = direction.rotated(rotationProvider())
        val distance = scrollDistanceProvider()
        val end = PointF(
            (origin.x + actualDirection.dx * distance)
                .coerceIn(0f, screenWidth.toFloat() - 1f),
            (origin.y + actualDirection.dy * distance)
                .coerceIn(0f, screenHeight.toFloat() - 1f)
        )
        if (origin.x == end.x && origin.y == end.y) return null
        return ScrollSegment(
            path = Path().apply {
                moveTo(origin.x, origin.y)
                lineTo(end.x, end.y)
            },
            end = end
        )
    }

    /** パスと押下時間からジェスチャー定義を作成します。 */
    private fun buildGesture(
        path: Path,
        duration: Long,
        stroke: StrokeDescription = createStroke(path, duration, false)
    ): GestureDescription = GestureDescription.Builder().addStroke(stroke).build()

    /** Androidのバージョンに応じて継続可能なStrokeDescriptionを作成します。 */
    private fun createStroke(path: Path, duration: Long, willContinue: Boolean): StrokeDescription =
        if (supportsStrokeContinuation) {
            StrokeDescription(path, 0, duration, willContinue)
        } else {
            StrokeDescription(path, 0, duration)
        }

    /** ジェスチャー開始点が現在の画面内にあるか確認します。 */
    private fun isOnScreen(point: PointF, width: Int, height: Int): Boolean =
        point.x in 0f..width.toFloat() && point.y in 0f..height.toFloat()

    private val supportsStrokeContinuation: Boolean
        get() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O

    private data class ScrollSegment(val path: Path, val end: PointF)

    private class ScrollSession(val direction: PointerDirection) {
        var pressed = true
        var finishing = false
        var dispatchInProgress = false
        var retryScheduled = false
        var currentPoint: PointF? = null
        var stroke: StrokeDescription? = null
    }

    companion object {
        private const val SCROLL_DURATION_MS = 150L
        private const val RETRY_DELAY_MS = 50L
        private const val MIN_GESTURE_DURATION = 1L
        private const val MAX_GESTURE_DURATION = 60_000L
    }
}
