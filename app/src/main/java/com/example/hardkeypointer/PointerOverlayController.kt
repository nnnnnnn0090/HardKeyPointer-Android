/*
 * HardKeyPointer-Android
 * Copyright (c) 2024-2026 nnnnnnn0090
 * 作者: nnnnnnn0090
 * ライセンス: リポジトリの LICENSE を参照してください。
 */
package com.nnnnnnn0090.hardkeypointer

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.PixelFormat
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import android.graphics.PointF
import kotlin.math.max

/** 画面に触れないユーザー補助オーバーレイと座標を管理します。 */
class PointerOverlayController(
    private val context: Context,
    private val windowManager: WindowManager
) {
    private var pointerView: View? = null
    private var x = 0
    private var y = 0
    private var width = 0
    private var height = 0
    private var hasInitialPosition = false

    val isVisible: Boolean
        get() = pointerView != null

    /** オーバーレイを一度だけ追加し、最後の位置を復元します。 */
    fun show(): Boolean {
        if (pointerView != null) return false

        return try {
            val view = inflatePointerView()
            pointerView = view
            val (screenWidth, screenHeight) = screenSize()
            val estimatedSize = pointerSizeFallback()
            if (!hasInitialPosition) {
                x = (screenWidth - estimatedSize) / 2
                y = (screenHeight - estimatedSize) / 2
                hasInitialPosition = true
            }

            val params = WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
                PixelFormat.TRANSLUCENT
            ).apply {
                gravity = Gravity.TOP or Gravity.START
                this.x = this@PointerOverlayController.x
                this.y = this@PointerOverlayController.y
            }
            windowManager.addView(view, params)
            view.post {
                if (pointerView !== view) return@post
                width = view.width
                height = view.height
                refreshBounds()
            }
            true
        } catch (error: Exception) {
            pointerView = null
            false
        }
    }

    /** オーバーレイを削除し、実際に削除できたかを返します。 */
    fun hide(): Boolean {
        val view = pointerView ?: return false
        pointerView = null
        return try {
            windowManager.removeView(view)
            true
        } catch (_: Exception) {
            false
        }
    }

    /** 左上座標を移動し、現在の画面内へ収めます。 */
    fun moveBy(deltaX: Int, deltaY: Int) {
        if (!isVisible) return
        x += deltaX
        y += deltaY
        refreshBounds()
    }

    /** ポインタが画面外へ出ないよう位置とレイアウトを更新します。 */
    fun refreshBounds() {
        val (screenWidth, screenHeight) = screenSize()
        val (pointerWidth, pointerHeight) = pointerSize()
        x = x.coerceIn(0, (screenWidth - pointerWidth).coerceAtLeast(0))
        y = y.coerceIn(0, (screenHeight - pointerHeight).coerceAtLeast(0))
        updateLayout()
    }

    /** 左上ではなく、ジェスチャーの基準に使う見た目上の中心を返します。 */
    fun centerPoint(): PointF? {
        val view = pointerView ?: return null
        val location = IntArray(2)
        view.getLocationOnScreen(location)
        val (pointerWidth, pointerHeight) = pointerSize()
        return PointF(
            location[0] + pointerWidth / 2f,
            location[1] + pointerHeight / 2f
        )
    }

    /** 現在のディスプレイサイズをピクセル単位で返します。 */
    fun screenSize(): Pair<Int, Int> {
        val metrics = context.resources.displayMetrics
        return Pair(max(1, metrics.widthPixels), max(1, metrics.heightPixels))
    }

    /** 実測できない初期状態も考慮してポインタサイズを返します。 */
    private fun pointerSize(): Pair<Int, Int> {
        val view = pointerView
        val fallback = pointerSizeFallback()
        return Pair(
            (view?.width ?: 0).takeIf { it > 0 } ?: width.takeIf { it > 0 } ?: fallback,
            (view?.height ?: 0).takeIf { it > 0 } ?: height.takeIf { it > 0 } ?: fallback
        )
    }

    /** dp換算したフォールバックのポインタサイズを返します。 */
    private fun pointerSizeFallback(): Int =
        (50 * context.resources.displayMetrics.density).toInt().coerceAtLeast(1)

    /** WindowManagerへ現在位置を反映します。 */
    private fun updateLayout() {
        val view = pointerView ?: return
        try {
            val params = view.layoutParams as WindowManager.LayoutParams
            params.x = x
            params.y = y
            windowManager.updateViewLayout(view, params)
        } catch (_: Exception) {
            // 予約済みフレーム実行中にサービスが終了する場合があります。
        }
    }

    @SuppressLint("InflateParams")
    /** ポインタレイアウトを生成します。 */
    private fun inflatePointerView(): View =
        LayoutInflater.from(context).inflate(R.layout.pointer_view, null)
}
