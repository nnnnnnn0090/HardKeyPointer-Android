/*
 * HardKeyPointer-Android
 * Copyright (c) 2024-2026 nnnnnnn0090
 * 作者: nnnnnnn0090
 * ライセンス: リポジトリの LICENSE を参照してください。
 */
package com.nnnnnnn0090.hardkeypointer

import android.os.Handler

/** 方向ごとに重複しないスクロール繰り返しタスクを管理します。 */
class ScrollRepeater(
    private val handler: Handler,
    private val onScroll: (PointerDirection) -> Unit,
    private val intervalProvider: () -> Int = { DEFAULT_INTERVAL_MS }
) {
    private val tasks = mutableMapOf<PointerDirection, Runnable>()

    /** 方向キーを押し続けても、方向ごとに1回だけ開始します。 */
    fun start(direction: PointerDirection) {
        if (tasks.containsKey(direction)) return
        val task = object : Runnable {
            /** スクロールを発行し、次回実行を予約します。 */
            override fun run() {
                onScroll(direction)
                val interval = intervalProvider()
                    .coerceIn(MIN_INTERVAL_MS, MAX_INTERVAL_MS)
                    .toLong()
                handler.postDelayed(this, interval)
            }
        }
        tasks[direction] = task
        handler.post(task)
    }

    /** 離された方向キーに対応するタスクを停止します。 */
    fun stop(direction: PointerDirection) {
        tasks.remove(direction)?.let(handler::removeCallbacks)
    }

    /** 全方向のスクロール処理を停止します。 */
    fun stopAll() {
        tasks.keys.toList().forEach(::stop)
    }

    companion object {
        private const val DEFAULT_INTERVAL_MS = 150
        private const val MIN_INTERVAL_MS = 50
        private const val MAX_INTERVAL_MS = 500
    }
}
