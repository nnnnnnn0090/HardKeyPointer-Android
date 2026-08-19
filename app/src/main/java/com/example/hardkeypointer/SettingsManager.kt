/*
 * HardKeyPointer-Android
 * Copyright (c) 2024-2026 nnnnnnn0090
 * 作者: nnnnnnn0090
 * ライセンス: リポジトリの LICENSE を参照してください。
 */
package com.nnnnnnn0090.hardkeypointer

import android.content.Context

/**
 * 旧 API を利用するコードのための互換Facadeです。
 * 新しいコードでは [SettingsRepository] と [KeyCaptureState] を直接利用してください。
 */
@Deprecated("Use SettingsRepository and KeyCaptureState")
object SettingsManager {
    const val NOT_SET = SettingsRepository.NOT_SET
    const val DEFAULT_MOVE_SPEED = SettingsRepository.DEFAULT_MOVE_SPEED
    const val DEFAULT_MOVE_ACCEL = SettingsRepository.DEFAULT_MOVE_ACCELERATION
    const val DEFAULT_SCROLL_DISTANCE = SettingsRepository.DEFAULT_SCROLL_DISTANCE
    const val MIN_MOVE_SPEED = SettingsRepository.MIN_MOVE_SPEED
    const val MAX_MOVE_SPEED = SettingsRepository.MAX_MOVE_SPEED
    const val MIN_MOVE_ACCEL = SettingsRepository.MIN_MOVE_ACCELERATION
    const val MAX_MOVE_ACCEL = SettingsRepository.MAX_MOVE_ACCELERATION
    const val MIN_SCROLL_DISTANCE = SettingsRepository.MIN_SCROLL_DISTANCE
    const val MAX_SCROLL_DISTANCE = SettingsRepository.MAX_SCROLL_DISTANCE

    /** 旧API向けのキー入力待ち状態を変更します。 */
    fun setKeyCaptureMode(enabled: Boolean) {
        if (enabled) KeyCaptureState.begin() else KeyCaptureState.finish()
    }

    /** 旧API向けにキー入力待ち状態を返します。 */
    fun isKeyCaptureMode(): Boolean = KeyCaptureState.isActive

    /** 旧形式の保存キー名からキーコードを取得します。 */
    fun getKeyCode(context: Context, action: String): Int {
        val binding = PointerAction.fromStorageKey(action) ?: return NOT_SET
        return SettingsRepository(context).getKeyCode(binding)
    }

    /** 旧形式の保存キー名へキーコードを保存します。 */
    fun setKeyCode(context: Context, action: String, keyCode: Int) {
        PointerAction.fromStorageKey(action)?.let {
            SettingsRepository(context).setKeyCode(it, keyCode)
        }
    }

    /** 旧API向けに移動速度を取得します。 */
    fun getMoveSpeed(context: Context): Int = SettingsRepository(context).getMoveSpeed()
    /** 旧API向けに移動速度を保存します。 */
    fun setMoveSpeed(context: Context, speed: Int) = SettingsRepository(context).setMoveSpeed(speed)

    /** 旧API向けに加速度を取得します。 */
    fun getMoveAccel(context: Context): Int = SettingsRepository(context).getMoveAcceleration()
    /** 旧API向けに加速度を保存します。 */
    fun setMoveAccel(context: Context, accel: Int) =
        SettingsRepository(context).setMoveAcceleration(accel)

    /** 旧API向けにスクロール距離を取得します。 */
    fun getScrollDistance(context: Context): Int = SettingsRepository(context).getScrollDistance()
    /** 旧API向けにスクロール距離を保存します。 */
    fun setScrollDistance(context: Context, distance: Int) =
        SettingsRepository(context).setScrollDistance(distance)

    /** 旧形式のキー名を使った全割り当てを返します。 */
    fun getAllKeyCodes(context: Context): Map<String, Int> =
        SettingsRepository(context).getKeyCodes().mapKeys { it.key.storageKey }
}
