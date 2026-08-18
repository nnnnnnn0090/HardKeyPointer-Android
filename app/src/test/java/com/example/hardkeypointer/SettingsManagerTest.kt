package com.nnnnnnn0090.hardkeypointer

import android.content.Context
import android.content.SharedPreferences
import android.view.KeyEvent
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.*

class SettingsManagerTest {

    private lateinit var mockContext: Context
    private lateinit var mockPrefs: SharedPreferences
    private lateinit var mockEditor: SharedPreferences.Editor

    @Before
    fun setup() {
        mockContext = mock(Context::class.java)
        mockPrefs = mock(SharedPreferences::class.java)
        mockEditor = mock(SharedPreferences.Editor::class.java)

        `when`(mockContext.getSharedPreferences(anyString(), anyInt())).thenReturn(mockPrefs)
        `when`(mockPrefs.edit()).thenReturn(mockEditor)
        `when`(mockEditor.putInt(anyString(), anyInt())).thenReturn(mockEditor)

        // Reset static prefs field via reflection
        val field = SettingsManager::class.java.getDeclaredField("prefs")
        field.isAccessible = true
        field.set(null, null)
    }

    @Test
    fun testGetKeyCode_returnsDefaultWhenNotSet() {
        `when`(mockPrefs.getInt("up", KeyEvent.KEYCODE_DPAD_UP)).thenReturn(KeyEvent.KEYCODE_DPAD_UP)
        
        val keyCode = SettingsManager.getKeyCode(mockContext, "up")
        
        assertEquals(KeyEvent.KEYCODE_DPAD_UP, keyCode)
    }

    @Test
    fun testGetKeyCode_returnsCustomValueWhenSet() {
        `when`(mockPrefs.getInt("up", KeyEvent.KEYCODE_DPAD_UP)).thenReturn(KeyEvent.KEYCODE_A)
        
        val keyCode = SettingsManager.getKeyCode(mockContext, "up")
        
        assertEquals(KeyEvent.KEYCODE_A, keyCode)
    }

    @Test
    fun testSetKeyCode_savesToPrefs() {
        SettingsManager.setKeyCode(mockContext, "up", KeyEvent.KEYCODE_A)
        
        verify(mockPrefs.edit()).putInt("up", KeyEvent.KEYCODE_A)
        verify(mockEditor).apply()
    }

    @Test
    fun testGetMoveSpeed_returnsDefault() {
        `when`(mockPrefs.getInt("moveSpeed", 30)).thenReturn(30)
        
        val speed = SettingsManager.getMoveSpeed(mockContext)
        
        assertEquals(30, speed)
    }

    @Test
    fun testSetMoveSpeed_savesToPrefs() {
        SettingsManager.setMoveSpeed(mockContext, 50)
        
        verify(mockPrefs.edit()).putInt("moveSpeed", 50)
        verify(mockEditor).apply()
    }

    @Test
    fun testGetMoveAccel_returnsDefault() {
        `when`(mockPrefs.getInt("moveAccel", 100)).thenReturn(100)
        
        val accel = SettingsManager.getMoveAccel(mockContext)
        
        assertEquals(100, accel)
    }

    @Test
    fun testSetMoveAccel_savesToPrefs() {
        SettingsManager.setMoveAccel(mockContext, 200)
        
        verify(mockPrefs.edit()).putInt("moveAccel", 200)
        verify(mockEditor).apply()
    }

    @Test
    fun testGetScrollDistance_returnsDefault() {
        `when`(mockPrefs.getInt("scrollDistance", 200)).thenReturn(200)
        
        val distance = SettingsManager.getScrollDistance(mockContext)
        
        assertEquals(200, distance)
    }

    @Test
    fun testSetScrollDistance_savesToPrefs() {
        SettingsManager.setScrollDistance(mockContext, 300)
        
        verify(mockPrefs.edit()).putInt("scrollDistance", 300)
        verify(mockEditor).apply()
    }

    @Test
    fun testGetAllKeyCodes_returnsMapWithAllKeys() {
        val defaultKeys = mapOf(
            "up" to KeyEvent.KEYCODE_DPAD_UP,
            "down" to KeyEvent.KEYCODE_DPAD_DOWN,
            "left" to KeyEvent.KEYCODE_DPAD_LEFT,
            "right" to KeyEvent.KEYCODE_DPAD_RIGHT,
            "tap" to KeyEvent.KEYCODE_ENTER,
            "disable" to KeyEvent.KEYCODE_VOLUME_DOWN,
            "scrollup" to KeyEvent.KEYCODE_2,
            "scrolldown" to KeyEvent.KEYCODE_5,
            "scrollleft" to KeyEvent.KEYCODE_4,
            "scrollright" to KeyEvent.KEYCODE_6
        )
        
        defaultKeys.forEach { (key, value) ->
            `when`(mockPrefs.getInt(key, value)).thenReturn(value)
        }
        
        val allCodes = SettingsManager.getAllKeyCodes(mockContext)
        
        assertEquals(10, allCodes.size)
        defaultKeys.forEach { (key, value) ->
            assertEquals(value, allCodes[key])
        }
    }
}