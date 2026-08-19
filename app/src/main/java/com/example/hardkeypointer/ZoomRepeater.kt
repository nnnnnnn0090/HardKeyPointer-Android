/*
 * HardKeyPointer-Android
 * Copyright (c) 2024-2026 nnnnnnn0090
 * 作者: nnnnnnn0090
 * ライセンス: リポジトリの LICENSE を参照してください。
 */
package com.nnnnnnn0090.hardkeypointer

import android.os.Handler

/** ズームジェスチャーを、キーが押されている間だけ一定間隔で繰り返します。 */
class ZoomRepeater(
    private val handler: Handler,
    private val onZoom: (Boolean) -> Unit,
    private val intervalProvider: () -> Int = { DEFAULT_INTERVAL_MS }
) {
    private val tasks = mutableMapOf<Boolean, Runnable>()

    /** 指定方向のズームを即時に開始し、次のジェスチャーを予約します。 */
    fun start(expand: Boolean) {
        if (tasks.containsKey(expand)) return
        val task = object : Runnable {
            /** ズームを発行し、現在の設定に合わせて次回実行を予約します。 */
            override fun run() {
                if (tasks[expand] !== this) return
                onZoom(expand)
                val interval = intervalProvider()
                    .coerceIn(MIN_INTERVAL_MS, MAX_INTERVAL_MS)
                    .toLong()
                handler.postDelayed(this, interval)
            }
        }
        tasks[expand] = task
        handler.post(task)
    }

    /** 指定方向のズーム繰り返しを停止します。 */
    fun stop(expand: Boolean) {
        tasks.remove(expand)?.let(handler::removeCallbacks)
    }

    /** すべてのズーム繰り返しを停止します。 */
    fun stopAll() {
        tasks.keys.toList().forEach(::stop)
    }

    companion object {
        private const val DEFAULT_INTERVAL_MS = 300
        private const val MIN_INTERVAL_MS = 100
        private const val MAX_INTERVAL_MS = 1_000
    }
}
