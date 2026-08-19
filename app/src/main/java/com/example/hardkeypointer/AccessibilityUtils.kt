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
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_RETURN_TO_APP, true)
            .apply()
        context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        })
    }

    /** 設定画面から戻る必要があるかを一度だけ取得します。 */
    fun consumeReturnToAppRequest(context: Context): Boolean {
        val preferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        if (!preferences.getBoolean(KEY_RETURN_TO_APP, false)) return false
        preferences.edit().remove(KEY_RETURN_TO_APP).apply()
        return true
    }

    /** 有効化されずに設定画面から戻った場合の保留状態を破棄します。 */
    fun clearReturnToAppRequest(context: Context) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .remove(KEY_RETURN_TO_APP)
            .apply()
    }

    /** サービスから設定画面を開く前のアプリを前面へ戻します。 */
    fun returnToMainActivity(context: Context) {
        context.startActivity(Intent(context, MainActivity::class.java).apply {
            addFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_CLEAR_TOP or
                    Intent.FLAG_ACTIVITY_SINGLE_TOP
            )
        })
    }

    /** 有効化を促す通知を表示して、ユーザー補助設定を開きます。 */
    fun redirectToAccessibilitySettings(activity: AppCompatActivity) {
        Toast.makeText(activity, "アクセシビリティサービスが有効ではありません", Toast.LENGTH_SHORT).show()
        openAccessibilitySettings(activity)
        Toast.makeText(activity, "設定画面で、HardKeyPointer をタップして有効にしてください。", Toast.LENGTH_LONG).show()
    }

    private const val PREFS_NAME = "accessibility_navigation"
    private const val KEY_RETURN_TO_APP = "return_to_app_after_enable"
}
