package com.nnnnnnn0090.hardkeypointer

import android.content.Context
import android.content.SharedPreferences
import android.view.KeyEvent

object SettingsManager {
    private const val PREF_NAME = "com.nnnnnnn0090.hardkeypointer.PREFS"
    
    private val defaultKeys = mapOf(
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
    
    @Volatile
    private var prefs: SharedPreferences? = null
    
    private fun getPrefs(context: Context): SharedPreferences {
        return prefs ?: synchronized(this) {
            prefs ?: context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE).also { prefs = it }
        }
    }
    
    fun getKeyCode(context: Context, action: String): Int =
        getPrefs(context).getInt(action, defaultKeys[action] ?: 0)
    
    fun setKeyCode(context: Context, action: String, keyCode: Int) =
        getPrefs(context).edit().putInt(action, keyCode).apply()
    
    fun getMoveSpeed(context: Context): Int = getPrefs(context).getInt("moveSpeed", 30)
    fun setMoveSpeed(context: Context, speed: Int) = getPrefs(context).edit().putInt("moveSpeed", speed).apply()
    
    fun getMoveAccel(context: Context): Int = getPrefs(context).getInt("moveAccel", 100)
    fun setMoveAccel(context: Context, accel: Int) = getPrefs(context).edit().putInt("moveAccel", accel).apply()
    
    fun getAllKeyCodes(context: Context): Map<String, Int> =
        defaultKeys.mapValues { (action, default) -> getKeyCode(context, action) }
}