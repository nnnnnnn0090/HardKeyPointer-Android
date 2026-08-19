/*
 * HardKeyPointer-Android
 * Copyright (c) 2024-2026 nnnnnnn0090
 * 作者: nnnnnnn0090
 * ライセンス: リポジトリの LICENSE を参照してください。
 */
package com.nnnnnnn0090.hardkeypointer

import android.os.Handler
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.mockito.ArgumentCaptor
import org.mockito.Mockito.mock
import org.mockito.Mockito.times
import org.mockito.Mockito.verify

class ZoomRepeaterTest {
    private lateinit var handler: Handler

    /** 繰り返し処理が使うハンドラーを初期化します。 */
    @Before
    fun setUp() {
        handler = mock(Handler::class.java)
    }

    /** 開始直後にズームを発行し、設定間隔で次回処理を予約します。 */
    @Test
    fun start_runsImmediatelyAndSchedulesNextZoom() {
        val directions = mutableListOf<Boolean>()
        val repeater = ZoomRepeater(handler, directions::add) { 250 }
        repeater.start(expand = true)

        val taskCaptor = ArgumentCaptor.forClass(Runnable::class.java)
        verify(handler).post(taskCaptor.capture())
        val task = taskCaptor.value
        task.run()
        task.run()

        assertEquals(listOf(true, true), directions)
        verify(handler, times(2)).postDelayed(task, 250L)
    }

    /** 同じ方向を二重に開始してもズーム処理を重複登録しません。 */
    @Test
    fun start_ignoresDuplicateDirection() {
        val repeater = ZoomRepeater(handler, {}) { 250 }
        repeater.start(expand = false)
        repeater.start(expand = false)

        verify(handler, times(1)).post(org.mockito.ArgumentMatchers.any(Runnable::class.java))
    }

    /** 停止時に予約済みのズーム処理を取り除きます。 */
    @Test
    fun stop_removesScheduledTask() {
        val repeater = ZoomRepeater(handler, {}) { 250 }
        repeater.start(expand = true)
        val taskCaptor = ArgumentCaptor.forClass(Runnable::class.java)
        verify(handler).post(taskCaptor.capture())

        repeater.stop(expand = true)

        verify(handler).removeCallbacks(taskCaptor.value)
    }
}
