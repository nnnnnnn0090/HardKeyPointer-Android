/*
 * HardKeyPointer-Android
 * Copyright (c) 2024-2026 nnnnnnn0090
 * 作者: nnnnnnn0090
 * ライセンス: リポジトリの LICENSE を参照してください。
 */
package com.nnnnnnn0090.hardkeypointer

import android.accessibilityservice.AccessibilityServiceInfo
import android.content.Context
import android.content.Intent
import android.provider.Settings
import android.view.accessibility.AccessibilityManager
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

/** ユーザー補助サービスの状態確認と設定画面遷移をまとめたヘルパーです。 */
object AccessibilityUtils {
    /** このアプリのサービスが現在有効な場合だけ true を返します。 */
    fun isAccessibilityServiceEnabled(context: Context): Boolean {
        val accessibilityManager = context.getSystemService(Context.ACCESSIBILITY_SERVICE)
            as? AccessibilityManager ?: return false
        return accessibilityManager
            .getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_ALL_MASK)
            .any { service ->
                val info = service.resolveInfo?.serviceInfo ?: return@any false
                info.packageName == context.packageName && info.name == TapService::class.java.name
            }
    }

    /** サービスを有効化できるシステム設定画面を開きます。 */
    fun openAccessibilitySettings(context: Context) {
        context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        })
    }

    /** 有効化を促す通知を表示して、ユーザー補助設定を開きます。 */
    fun redirectToAccessibilitySettings(activity: AppCompatActivity) {
        Toast.makeText(activity, "アクセシビリティサービスが有効ではありません", Toast.LENGTH_SHORT).show()
        openAccessibilitySettings(activity)
        Toast.makeText(activity, "設定画面で、HardKeyPointer をタップして有効にしてください。", Toast.LENGTH_LONG).show()
    }
}
