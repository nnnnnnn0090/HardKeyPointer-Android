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
        return accessibilityManager.getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_ALL_MASK)
            .any { it.resolveInfo.serviceInfo.let { info -> 
                info.packageName == context.packageName && info.name == TapService::class.java.name 
            }}
    }

    fun redirectToAccessibilitySettings(activity: AppCompatActivity) {
        Toast.makeText(activity, "アクセシビリティサービスが有効ではありません", Toast.LENGTH_SHORT).show()
        activity.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).apply {
            addFlags(Intent.FLAG_RECEIVER_REGISTERED_ONLY or Intent.FLAG_ACTIVITY_NEW_TASK)
        })
        Toast.makeText(activity, "設定画面で、HardKeyPointer をタップして有効にしてください。", Toast.LENGTH_LONG).show()
    }
}
