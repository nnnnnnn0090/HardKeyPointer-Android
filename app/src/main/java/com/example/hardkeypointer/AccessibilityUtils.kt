package com.nnnnnnn0090.hardkeypointer

import android.accessibilityservice.AccessibilityServiceInfo
import android.content.Context
import android.content.Intent
import android.provider.Settings
import android.view.accessibility.AccessibilityManager
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

object AccessibilityUtils {
    fun isAccessibilityServiceEnabled(context: Context): Boolean {
        val accessibilityManager = context.getSystemService(Context.ACCESSIBILITY_SERVICE) as AccessibilityManager
        val enabledServices = accessibilityManager.getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_ALL_MASK)
        for (enabledService in enabledServices) {
            val serviceInfo = enabledService.resolveInfo.serviceInfo
            if (serviceInfo.packageName == context.packageName && serviceInfo.name == TapService::class.java.name) {
                return true
            }
        }
        return false
    }

    fun redirectToAccessibilitySettings(activity: AppCompatActivity) {
        Toast.makeText(activity, "アクセシビリティサービスが有効ではありません", Toast.LENGTH_SHORT).show()

        val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
        intent.addFlags(Intent.FLAG_RECEIVER_REGISTERED_ONLY)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        activity.startActivity(intent)
        Toast.makeText(activity, "設定画面で、HardKeyPointer をタップして有効にしてください。", Toast.LENGTH_LONG).show()
    }
}
