/*
 * HardKeyPointer-Android
 * Copyright (c) 2024-2026 nnnnnnn0090
 * 作者: nnnnnnn0090
 * ライセンス: リポジトリの LICENSE を参照してください。
 */
package com.nnnnnnn0090.hardkeypointer

import android.view.KeyEvent

/** 設定可能なポインタ操作を型安全に表す識別子です。 */
enum class PointerAction(
    val storageKey: String,
    val defaultKeyCode: Int
) {
    UP("up", KeyEvent.KEYCODE_DPAD_UP),
    DOWN("down", KeyEvent.KEYCODE_DPAD_DOWN),
    LEFT("left", KeyEvent.KEYCODE_DPAD_LEFT),
    RIGHT("right", KeyEvent.KEYCODE_DPAD_RIGHT),
    TAP("tap", KeyEvent.KEYCODE_ENTER),
    TOGGLE("disable", KeyEvent.KEYCODE_VOLUME_DOWN),
    SCROLL_UP("scrollup", KeyEvent.KEYCODE_2),
    SCROLL_DOWN("scrolldown", KeyEvent.KEYCODE_5),
    SCROLL_LEFT("scrollleft", KeyEvent.KEYCODE_4),
    SCROLL_RIGHT("scrollright", KeyEvent.KEYCODE_6),
    ZOOM_IN("zoomin", KeyEvent.KEYCODE_9),
    ZOOM_OUT("zoomout", KeyEvent.KEYCODE_7);

    companion object {
        /** 保存キー名から対応する操作を検索します。 */
        fun fromStorageKey(storageKey: String): PointerAction? =
            entries.firstOrNull { it.storageKey == storageKey }
    }
}
