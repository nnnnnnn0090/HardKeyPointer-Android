/*
 * HardKeyPointer-Android
 * Copyright (c) 2024-2026 nnnnnnn0090
 * 作者: nnnnnnn0090
 * ライセンス: リポジトリの LICENSE を参照してください。
 */
package com.nnnnnnn0090.hardkeypointer

/** キーを押してから操作を発動するタイミングを表します。 */
enum class TriggerMode {
    /** キーを押した直後に操作を発動します。 */
    IMMEDIATE,

    /** キーを一定時間押し続けたときに操作を発動します。 */
    LONG_PRESS
}
