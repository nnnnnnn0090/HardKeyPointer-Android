/*
 * HardKeyPointer-Android
 * Copyright (c) 2024-2026 nnnnnnn0090
 * 作者: nnnnnnn0090
 * ライセンス: リポジトリの LICENSE を参照してください。
 */
package com.nnnnnnn0090.hardkeypointer

import android.content.Context
import android.content.SharedPreferences
import android.view.KeyEvent

/** サービスが利用する移動関連設定のスナップショットです。 */
data class MovementSettings(
    val speed: Int,
    val acceleration: Int,
    val scrollDistance: Int
)

/** キー割り当てと移動設定を保存・取得する唯一の窓口です。 */
class SettingsRepository(context: Context) {
    private val preferences: SharedPreferences =
        (context.applicationContext ?: context)
            .getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)

    /** 1つの割り当てを読み込み、NOT_SET の特別値を維持します。 */
    fun getKeyCode(action: PointerAction): Int {
        val stored = preferences.getInt(action.storageKey, action.defaultKeyCode)
        return if (stored >= 0 || stored == NOT_SET) stored else action.defaultKeyCode
    }

    /** 不明・不正なキーコードを NOT_SET に変換して保存します。 */
    fun setKeyCode(action: PointerAction, keyCode: Int) {
        val normalized = if (keyCode <= KeyEvent.KEYCODE_UNKNOWN) NOT_SET else keyCode
        preferences.edit().putInt(action.storageKey, normalized).apply()
    }

    /** 全操作のキー割り当てを一度に取得します。 */
    fun getKeyCodes(): Map<PointerAction, Int> =
        PointerAction.entries.associateWith(::getKeyCode)

    /** 保存済みの移動速度を範囲内に補正して返します。 */
    fun getMoveSpeed(): Int = preferences
        .getInt(KEY_MOVE_SPEED, DEFAULT_MOVE_SPEED)
        .coerceIn(MIN_MOVE_SPEED, MAX_MOVE_SPEED)

    /** 移動速度を許容範囲に補正して保存します。 */
    fun setMoveSpeed(speed: Int) = preferences.edit()
        .putInt(KEY_MOVE_SPEED, speed.coerceIn(MIN_MOVE_SPEED, MAX_MOVE_SPEED)).apply()

    /** 保存済みの加速度を範囲内に補正して返します。 */
    fun getMoveAcceleration(): Int = preferences
        .getInt(KEY_MOVE_ACCELERATION, DEFAULT_MOVE_ACCELERATION)
        .coerceIn(MIN_MOVE_ACCELERATION, MAX_MOVE_ACCELERATION)

    /** 加速度を許容範囲に補正して保存します。 */
    fun setMoveAcceleration(acceleration: Int) = preferences.edit()
        .putInt(KEY_MOVE_ACCELERATION, acceleration.coerceIn(MIN_MOVE_ACCELERATION, MAX_MOVE_ACCELERATION))
        .apply()

    /** 保存済みのスクロール距離を範囲内に補正して返します。 */
    fun getScrollDistance(): Int = preferences
        .getInt(KEY_SCROLL_DISTANCE, DEFAULT_SCROLL_DISTANCE)
        .coerceIn(MIN_SCROLL_DISTANCE, MAX_SCROLL_DISTANCE)

    /** スクロール距離を許容範囲に補正して保存します。 */
    fun setScrollDistance(distance: Int) = preferences.edit()
        .putInt(KEY_SCROLL_DISTANCE, distance.coerceIn(MIN_SCROLL_DISTANCE, MAX_SCROLL_DISTANCE))
        .apply()

    /** 移動処理が必要とする設定を1つの値として返します。 */
    fun getMovementSettings(): MovementSettings = MovementSettings(
        speed = getMoveSpeed(),
        acceleration = getMoveAcceleration(),
        scrollDistance = getScrollDistance()
    )

    companion object {
        const val NOT_SET = 3000
        const val DEFAULT_MOVE_SPEED = 30
        const val DEFAULT_MOVE_ACCELERATION = 100
        const val DEFAULT_SCROLL_DISTANCE = 200
        const val MIN_MOVE_SPEED = 5
        const val MAX_MOVE_SPEED = 100
        const val MIN_MOVE_ACCELERATION = 0
        const val MAX_MOVE_ACCELERATION = 500
        const val MIN_SCROLL_DISTANCE = 50
        const val MAX_SCROLL_DISTANCE = 500

        private const val PREF_NAME = "com.nnnnnnn0090.hardkeypointer.PREFS"
        private const val KEY_MOVE_SPEED = "moveSpeed"
        private const val KEY_MOVE_ACCELERATION = "moveAccel"
        private const val KEY_SCROLL_DISTANCE = "scrollDistance"
    }
}
