/*
 * HardKeyPointer-Android
 * Copyright (c) 2024-2026 nnnnnnn0090
 * 作者: nnnnnnn0090
 * ライセンス: リポジトリの LICENSE を参照してください。
 */
package com.nnnnnnn0090.hardkeypointer

/** 設定画面とユーザー補助サービスで共有するキー入力待ち状態です。 */
object KeyCaptureState {
    @Volatile
    var isActive: Boolean = false
        private set

    /** 設定画面がキーを選択している間、サービスの動作を一時停止します。 */
    fun begin() {
        isActive = true
    }

    /** 対応する物理キーの KEY_UP を受け取った後に入力待ちを終了します。 */
    fun finish() {
        isActive = false
    }
}
