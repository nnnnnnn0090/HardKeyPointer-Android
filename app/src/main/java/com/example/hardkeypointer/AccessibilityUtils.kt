package com.nnnnnnn0090.hardkeypointer

import android.content.Context
import android.content.Intent
import android.provider.Settings
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

object AccessibilityUtils {

    fun isAccessibilityServiceEnabled(context: Context): Boolean {
        var accessibilityEnabled = 0
        try {
            accessibilityEnabled = Settings.Secure.getInt(
                context.contentResolver,
                Settings.Secure.ACCESSIBILITY_ENABLED
            )
        } catch (e: Settings.SettingNotFoundException) {
            e.printStackTrace()
        }
        return accessibilityEnabled == 1
    }

    fun redirectToAccessibilitySettings(activity: AppCompatActivity) {
        Toast.makeText(activity, "アクセシビリティサービスが有効ではありません", Toast.LENGTH_SHORT).show()
        val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
        activity.startActivity(intent)
        Toast.makeText(activity, "設定画面で、HardKeyPointer をタップして有効にしてください。", Toast.LENGTH_LONG).show()
    }
}
