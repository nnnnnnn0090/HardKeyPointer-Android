/*
 * HardKeyPointer-Android
 * Copyright (c) 2024-2026 nnnnnnn0090
 * 作者: nnnnnnn0090
 * ライセンス: リポジトリの LICENSE を参照してください。
 */
package com.nnnnnnn0090.hardkeypointer

import android.content.Context
import android.view.KeyEvent

/** Android のキーコード定数を設定画面向けの表示名へ変換します。 */
object KeyNameFormatter {
    /** キーコードを人間が読める設定画面用の文字列へ変換します。 */
    fun format(context: Context, keyCode: Int): String {
        if (keyCode == SettingsRepository.NOT_SET || keyCode == KeyEvent.KEYCODE_UNKNOWN) {
            return context.getString(R.string.not_set)
        }
        return KeyEvent.keyCodeToString(keyCode)
            .removePrefix("KEYCODE_")
            .replace('_', ' ')
            .lowercase()
            .split(' ')
            .joinToString(" ") { word -> word.replaceFirstChar(Char::uppercase) }
    }
}
