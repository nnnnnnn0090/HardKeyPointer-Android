/*
 * HardKeyPointer-Android
 * Copyright (c) 2024-2026 nnnnnnn0090
 * 作者: nnnnnnn0090
 * ライセンス: リポジトリの LICENSE を参照してください。
 */
package com.nnnnnnn0090.hardkeypointer

import android.view.Surface

/** ポインタ移動とスクロールで共用する単位方向ベクトルです。 */
enum class PointerDirection(val dx: Int, val dy: Int) {
    UP(0, -1),
    DOWN(0, 1),
    LEFT(-1, 0),
    RIGHT(1, 0);

    /** 画面回転後の物理画面座標へ論理方向を変換します。 */
    fun rotated(rotation: Int): PointerDirection = when (rotation) {
        Surface.ROTATION_90 -> when (this) {
            UP -> LEFT
            LEFT -> DOWN
            DOWN -> RIGHT
            RIGHT -> UP
        }
        Surface.ROTATION_180 -> when (this) {
            UP -> DOWN
            LEFT -> RIGHT
            DOWN -> UP
            RIGHT -> LEFT
        }
        Surface.ROTATION_270 -> when (this) {
            UP -> RIGHT
            RIGHT -> DOWN
            DOWN -> LEFT
            LEFT -> UP
        }
        else -> this
    }
}
