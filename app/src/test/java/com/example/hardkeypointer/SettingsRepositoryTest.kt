/*
 * HardKeyPointer-Android
 * Copyright (c) 2024-2026 nnnnnnn0090
 * 作者: nnnnnnn0090
 * ライセンス: リポジトリの LICENSE を参照してください。
 */
package com.nnnnnnn0090.hardkeypointer

import android.content.Context
import android.content.SharedPreferences
import android.view.KeyEvent
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.anyInt
import org.mockito.Mockito.anyString
import org.mockito.Mockito.`when`
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify

class SettingsRepositoryTest {

    private lateinit var mockContext: Context
    private lateinit var mockPrefs: SharedPreferences
    private lateinit var mockEditor: SharedPreferences.Editor
    private lateinit var repository: SettingsRepository

    /** Mockitoの共有設定依存を初期化します。 */
    @Before
    fun setup() {
        mockContext = mock(Context::class.java)
        mockPrefs = mock(SharedPreferences::class.java)
        mockEditor = mock(SharedPreferences.Editor::class.java)

        `when`(mockContext.getSharedPreferences(anyString(), anyInt())).thenReturn(mockPrefs)
        `when`(mockPrefs.edit()).thenReturn(mockEditor)
        `when`(mockEditor.putInt(anyString(), anyInt())).thenReturn(mockEditor)
        repository = SettingsRepository(mockContext)
    }

    /** 未保存時に上方向キーの既定値を返すことを確認します。 */
    @Test
    fun getKeyCode_returnsDefaultWhenNotSet() {
        `when`(mockPrefs.getInt(PointerAction.UP.storageKey, PointerAction.UP.defaultKeyCode))
            .thenReturn(PointerAction.UP.defaultKeyCode)

        assertEquals(PointerAction.UP.defaultKeyCode, repository.getKeyCode(PointerAction.UP))
    }

    /** 保存済みのカスタムキーコードを返すことを確認します。 */
    @Test
    fun getKeyCode_returnsCustomValueWhenSet() {
        `when`(mockPrefs.getInt(PointerAction.UP.storageKey, PointerAction.UP.defaultKeyCode))
            .thenReturn(KeyEvent.KEYCODE_A)

        assertEquals(KeyEvent.KEYCODE_A, repository.getKeyCode(PointerAction.UP))
    }

    /** キーコードが設定値として保存されることを確認します。 */
    @Test
    fun setKeyCode_savesToPrefs() {
        repository.setKeyCode(PointerAction.UP, KeyEvent.KEYCODE_A)

        verify(mockPrefs.edit()).putInt(PointerAction.UP.storageKey, KeyEvent.KEYCODE_A)
        verify(mockEditor).apply()
    }

    /** 数値設定の既定値が返されることを確認します。 */
    @Test
    fun numericSettings_useDefaults() {
        `when`(mockPrefs.getInt("moveSpeedPx", SettingsRepository.DEFAULT_MOVE_SPEED_PX))
            .thenReturn(SettingsRepository.DEFAULT_MOVE_SPEED_PX)
        `when`(mockPrefs.getInt("moveAccelerationPx", SettingsRepository.DEFAULT_MOVE_ACCELERATION))
            .thenReturn(SettingsRepository.DEFAULT_MOVE_ACCELERATION)
        `when`(mockPrefs.getInt("scrollDistancePx", SettingsRepository.DEFAULT_SCROLL_DISTANCE_PX))
            .thenReturn(SettingsRepository.DEFAULT_SCROLL_DISTANCE_PX)
        `when`(mockPrefs.getInt("scrollSpeedPx", SettingsRepository.DEFAULT_SCROLL_SPEED))
            .thenReturn(SettingsRepository.DEFAULT_SCROLL_SPEED)
        `when`(mockPrefs.getInt("zoomAmountPx", SettingsRepository.DEFAULT_ZOOM_AMOUNT_PX))
            .thenReturn(SettingsRepository.DEFAULT_ZOOM_AMOUNT_PX)
        `when`(mockPrefs.getInt("zoomDurationPx", SettingsRepository.DEFAULT_ZOOM_DURATION))
            .thenReturn(SettingsRepository.DEFAULT_ZOOM_DURATION)

        assertEquals(SettingsRepository.DEFAULT_MOVE_SPEED_PX, repository.getMoveSpeed())
        assertEquals(SettingsRepository.DEFAULT_MOVE_ACCELERATION, repository.getMoveAcceleration())
        assertEquals(SettingsRepository.DEFAULT_SCROLL_DISTANCE_PX, repository.getScrollDistance())
        assertEquals(SettingsRepository.DEFAULT_SCROLL_SPEED, repository.getScrollSpeed())
        assertEquals(SettingsRepository.DEFAULT_ZOOM_AMOUNT_PX, repository.getZoomAmount())
        assertEquals(SettingsRepository.DEFAULT_ZOOM_DURATION, repository.getZoomDuration())
    }

    /** 読み込み時に数値設定が許容範囲へ補正されることを確認します。 */
    @Test
    fun numericSettings_areClampedToSupportedRanges() {
        `when`(mockPrefs.getInt("moveSpeedPx", SettingsRepository.DEFAULT_MOVE_SPEED_PX)).thenReturn(999)
        `when`(mockPrefs.getInt("moveAccelerationPx", SettingsRepository.DEFAULT_MOVE_ACCELERATION)).thenReturn(-10)
        `when`(mockPrefs.getInt("scrollDistancePx", SettingsRepository.DEFAULT_SCROLL_DISTANCE_PX)).thenReturn(1)

        assertEquals(SettingsRepository.MAX_MOVE_SPEED_PX, repository.getMoveSpeed())
        assertEquals(SettingsRepository.MIN_MOVE_ACCELERATION, repository.getMoveAcceleration())
        assertEquals(SettingsRepository.MIN_SCROLL_DISTANCE_PX, repository.getScrollDistance())
    }

    /** 保存時にも数値設定が許容範囲へ補正されることを確認します。 */
    @Test
    fun numericSettings_settersClampBeforeSaving() {
        repository.setMoveSpeed(Int.MAX_VALUE)
        repository.setMoveAcceleration(Int.MIN_VALUE)
        repository.setScrollDistance(Int.MAX_VALUE)

        verify(mockPrefs.edit()).putInt("moveSpeedPx", SettingsRepository.MAX_MOVE_SPEED_PX)
        verify(mockPrefs.edit()).putInt("moveAccelerationPx", SettingsRepository.MIN_MOVE_ACCELERATION)
        verify(mockPrefs.edit()).putInt("scrollDistancePx", SettingsRepository.MAX_SCROLL_DISTANCE_PX)
    }

    /** 全操作のキー割り当てが取得できることを確認します。 */
    @Test
    fun getKeyCodes_returnsAllActions() {
        val values = PointerAction.entries.associateWith { it.defaultKeyCode }
        values.forEach { (action, keyCode) ->
            `when`(mockPrefs.getInt(action.storageKey, action.defaultKeyCode)).thenReturn(keyCode)
        }

        val keyCodes = repository.getKeyCodes()

        assertEquals(PointerAction.entries.size, keyCodes.size)
        assertTrue(values.all { (action, keyCode) -> keyCodes[action] == keyCode })
    }

    /** 不明キーが未設定値へ正規化されることを確認します。 */
    @Test
    fun setKeyCode_convertsUnknownKeyToNotSet() {
        repository.setKeyCode(PointerAction.UP, KeyEvent.KEYCODE_UNKNOWN)

        verify(mockPrefs.edit()).putInt(PointerAction.UP.storageKey, SettingsRepository.NOT_SET)
        verify(mockEditor).apply()
    }
}
