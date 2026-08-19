/*
 * HardKeyPointer-Android
 * Copyright (c) 2024-2026 nnnnnnn0090
 * 作者: nnnnnnn0090
 * ライセンス: リポジトリの LICENSE を参照してください。
 */
package com.nnnnnnn0090.hardkeypointer

import android.os.Handler

/** 標準スクロールを優先し、非対応時だけジェスチャーへ切り替えます。 */
class ScrollController(
    private val handler: Handler,
    private val accessibilityScroll: AccessibilityScrollController,
    private val gestures: GestureController
) {
    private val semanticTasks = mutableMapOf<PointerDirection, Runnable>()

    /** キー押下時に標準操作または継続ジェスチャーを開始します。 */
    fun start(direction: PointerDirection) {
        if (semanticTasks.containsKey(direction)) return
        if (accessibilityScroll.scroll(direction)) {
            startSemanticRepeat(direction)
        } else {
            gestures.startContinuousScroll(direction)
        }
    }

    /** キー解放時に標準操作または継続ジェスチャーを停止します。 */
    fun stop(direction: PointerDirection) {
        semanticTasks.remove(direction)?.let(handler::removeCallbacks)
        gestures.stopContinuousScroll(direction)
    }

    /** 全方向のスクロール処理を停止します。 */
    fun stopAll() {
        semanticTasks.keys.toList().forEach(::stop)
        gestures.stopAllContinuousScroll()
    }

    /** 標準アクションを一定間隔で繰り返し、途中で非対応になればフォールバックします。 */
    private fun startSemanticRepeat(direction: PointerDirection) {
        val task = object : Runnable {
            /** 標準スクロールを発行し、次回実行を予約します。 */
            override fun run() {
                if (semanticTasks[direction] !== this) return
                if (!accessibilityScroll.scroll(direction)) {
                    semanticTasks.remove(direction)
                    gestures.startContinuousScroll(direction)
                    return
                }
                handler.postDelayed(this, SEMANTIC_REPEAT_INTERVAL_MS)
            }
        }
        semanticTasks[direction] = task
        handler.postDelayed(task, SEMANTIC_REPEAT_INTERVAL_MS)
    }

    companion object {
        private const val SEMANTIC_REPEAT_INTERVAL_MS = 150L
    }
}
