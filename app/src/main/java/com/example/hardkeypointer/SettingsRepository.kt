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

/** 移動処理が利用する現在の設定値をまとめたスナップショットです。 */
data class MovementSettings(
    val speed: Int,
    val acceleration: Int,
    val coordinateMode: CoordinateMode
)

/** 距離系操作の現在値をまとめたスナップショットです。 */
data class SpatialSettings(
    val coordinateMode: CoordinateMode,
    val moveSpeed: Int,
    val scrollDistance: Int,
    val zoomAmount: Int
)

/** キー割り当てと操作設定を保存・取得する唯一の窓口です。 */
class SettingsRepository(context: Context) {
    private val preferences: SharedPreferences =
        (context.applicationContext ?: context)
            .getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)

    /** 保存キーに対応するキーコードを取得します。 */
    fun getKeyCode(action: PointerAction): Int {
        val stored = preferences.getInt(action.storageKey, action.defaultKeyCode)
        return if (stored >= 0 || stored == NOT_SET) stored else action.defaultKeyCode
    }

    /** キーコードを検証して保存します。 */
    fun setKeyCode(action: PointerAction, keyCode: Int) {
        val normalized = if (keyCode <= KeyEvent.KEYCODE_UNKNOWN) NOT_SET else keyCode
        preferences.edit().putInt(action.storageKey, normalized).apply()
    }

    /** 全操作のキー割り当てを取得します。 */
    fun getKeyCodes(): Map<PointerAction, Int> =
        PointerAction.entries.associateWith(::getKeyCode)

    /** 操作の発動方式を取得します。 */
    fun getTriggerMode(action: PointerAction): TriggerMode =
        preferences.getString(triggerModeKey(action), TriggerMode.IMMEDIATE.name)
            ?.let { value -> TriggerMode.entries.firstOrNull { it.name == value } }
            ?: TriggerMode.IMMEDIATE

    /** 操作の発動方式を保存します。 */
    fun setTriggerMode(action: PointerAction, mode: TriggerMode) {
        preferences.edit().putString(triggerModeKey(action), mode.name).apply()
    }

    /** キー割り当てと全設定値を保存領域から削除し、初期値へ戻します。 */
    fun resetAll() {
        preferences.edit().clear().apply()
    }

    /** 現在の距離解釈モードを取得します。 */
    fun getCoordinateMode(): CoordinateMode =
        preferences.getString(KEY_COORDINATE_MODE, CoordinateMode.PIXELS.name)
            ?.let { value -> CoordinateMode.entries.firstOrNull { it.name == value } }
            ?: CoordinateMode.PIXELS

    /** 距離解釈モードを保存します。 */
    fun setCoordinateMode(mode: CoordinateMode) {
        preferences.edit().putString(KEY_COORDINATE_MODE, mode.name).apply()
    }

    /** 現在モードの移動速度を取得します。 */
    fun getMoveSpeed(): Int = getMoveSpeed(getCoordinateMode())

    /** 指定モードの移動速度を取得します。 */
    fun getMoveSpeed(mode: CoordinateMode): Int = getBoundedInt(
        key = moveSpeedKey(mode),
        default = moveSpeedDefault(mode),
        min = moveSpeedMin(mode),
        max = moveSpeedMax(mode)
    )

    /** 現在モードの移動速度を保存します。 */
    fun setMoveSpeed(value: Int) = setMoveSpeed(getCoordinateMode(), value)

    /** 指定モードの移動速度を保存します。 */
    fun setMoveSpeed(mode: CoordinateMode, value: Int) {
        setBoundedInt(moveSpeedKey(mode), value, moveSpeedMin(mode), moveSpeedMax(mode))
    }

    /** 現在モードの加速度を取得します。 */
    fun getMoveAcceleration(): Int = getMoveAcceleration(getCoordinateMode())

    /** 指定モードの加速度を取得します。 */
    fun getMoveAcceleration(mode: CoordinateMode): Int = getBoundedInt(
        moveAccelerationKey(mode),
        DEFAULT_MOVE_ACCELERATION,
        MIN_MOVE_ACCELERATION,
        MAX_MOVE_ACCELERATION
    )

    /** 加速度を保存します。 */
    fun setMoveAcceleration(value: Int) = setMoveAcceleration(getCoordinateMode(), value)

    /** 指定モードの加速度を保存します。 */
    fun setMoveAcceleration(mode: CoordinateMode, value: Int) {
        setBoundedInt(
            moveAccelerationKey(mode),
            value,
            MIN_MOVE_ACCELERATION,
            MAX_MOVE_ACCELERATION
        )
    }

    /** 現在モードのスクロール距離を取得します。 */
    fun getScrollDistance(): Int = getScrollDistance(getCoordinateMode())

    /** 指定モードのスクロール距離または割合を取得します。 */
    fun getScrollDistance(mode: CoordinateMode): Int = getBoundedInt(
        scrollDistanceKey(mode),
        scrollDistanceDefault(mode),
        scrollDistanceMin(mode),
        scrollDistanceMax(mode)
    )

    /** 現在モードのスクロール距離を保存します。 */
    fun setScrollDistance(value: Int) = setScrollDistance(getCoordinateMode(), value)

    /** 指定モードのスクロール距離または割合を保存します。 */
    fun setScrollDistance(mode: CoordinateMode, value: Int) {
        setBoundedInt(
            scrollDistanceKey(mode),
            value,
            scrollDistanceMin(mode),
            scrollDistanceMax(mode)
        )
    }

    /** 現在モードのスクロール速度レベルを取得します。 */
    fun getScrollSpeed(): Int = getScrollSpeed(getCoordinateMode())

    /** 指定モードのスクロール速度レベルを取得します。 */
    fun getScrollSpeed(mode: CoordinateMode): Int = getBoundedInt(
        scrollSpeedKey(mode),
        DEFAULT_SCROLL_SPEED,
        MIN_SCROLL_SPEED,
        MAX_SCROLL_SPEED
    )

    /** スクロール速度レベルを保存します。 */
    fun setScrollSpeed(value: Int) = setScrollSpeed(getCoordinateMode(), value)

    /** 指定モードのスクロール速度レベルを保存します。 */
    fun setScrollSpeed(mode: CoordinateMode, value: Int) {
        setBoundedInt(scrollSpeedKey(mode), value, MIN_SCROLL_SPEED, MAX_SCROLL_SPEED)
    }

    /** 現在モードのズーム量を取得します。 */
    fun getZoomAmount(): Int = getZoomAmount(getCoordinateMode())

    /** 指定モードのズーム量または割合を取得します。 */
    fun getZoomAmount(mode: CoordinateMode): Int = getBoundedInt(
        zoomAmountKey(mode),
        zoomAmountDefault(mode),
        zoomAmountMin(mode),
        zoomAmountMax(mode)
    )

    /** 現在モードのズーム量を保存します。 */
    fun setZoomAmount(value: Int) = setZoomAmount(getCoordinateMode(), value)

    /** 指定モードのズーム量または割合を保存します。 */
    fun setZoomAmount(mode: CoordinateMode, value: Int) {
        setBoundedInt(zoomAmountKey(mode), value, zoomAmountMin(mode), zoomAmountMax(mode))
    }

    /** 現在モードのズームジェスチャー時間を取得します。 */
    fun getZoomDuration(): Int = getZoomDuration(getCoordinateMode())

    /** 指定モードのズームジェスチャー時間を取得します。 */
    fun getZoomDuration(mode: CoordinateMode): Int = getBoundedInt(
        zoomDurationKey(mode),
        DEFAULT_ZOOM_DURATION,
        MIN_ZOOM_DURATION,
        MAX_ZOOM_DURATION
    )

    /** ズームジェスチャー時間を保存します。 */
    fun setZoomDuration(value: Int) = setZoomDuration(getCoordinateMode(), value)

    /** 指定モードのズームジェスチャー時間を保存します。 */
    fun setZoomDuration(mode: CoordinateMode, value: Int) {
        setBoundedInt(zoomDurationKey(mode), value, MIN_ZOOM_DURATION, MAX_ZOOM_DURATION)
    }

    /** 現在モードの移動設定を取得します。 */
    fun getMovementSettings(): MovementSettings = MovementSettings(
        speed = getMoveSpeed(),
        acceleration = getMoveAcceleration(getCoordinateMode()),
        coordinateMode = getCoordinateMode()
    )

    /** 現在モードの距離系設定を取得します。 */
    fun getSpatialSettings(): SpatialSettings = SpatialSettings(
        coordinateMode = getCoordinateMode(),
        moveSpeed = getMoveSpeed(),
        scrollDistance = getScrollDistance(),
        zoomAmount = getZoomAmount()
    )

    /** 保存値を指定範囲に補正して取得します。 */
    private fun getBoundedInt(key: String, default: Int, min: Int, max: Int): Int =
        preferences.getInt(key, default).coerceIn(min, max)

    /** 指定値を指定範囲に補正して保存します。 */
    private fun setBoundedInt(key: String, value: Int, min: Int, max: Int) {
        preferences.edit().putInt(key, value.coerceIn(min, max)).apply()
    }

    /** 操作ごとの発動方式保存キーを返します。 */
    private fun triggerModeKey(action: PointerAction): String =
        "triggerMode_${action.storageKey}"

    /** モード別の移動速度保存キーを返します。 */
    private fun moveSpeedKey(mode: CoordinateMode): String =
        if (mode == CoordinateMode.PIXELS) KEY_MOVE_SPEED_PX else KEY_MOVE_SPEED_RATIO

    /** モード別の加速度保存キーを返します。 */
    private fun moveAccelerationKey(mode: CoordinateMode): String =
        if (mode == CoordinateMode.PIXELS) KEY_MOVE_ACCELERATION_PX else KEY_MOVE_ACCELERATION_RATIO

    /** モード別のスクロール距離保存キーを返します。 */
    private fun scrollDistanceKey(mode: CoordinateMode): String =
        if (mode == CoordinateMode.PIXELS) KEY_SCROLL_DISTANCE_PX else KEY_SCROLL_DISTANCE_RATIO

    /** モード別のズーム量保存キーを返します。 */
    private fun zoomAmountKey(mode: CoordinateMode): String =
        if (mode == CoordinateMode.PIXELS) KEY_ZOOM_AMOUNT_PX else KEY_ZOOM_AMOUNT_RATIO

    /** モード別のスクロール速度保存キーを返します。 */
    private fun scrollSpeedKey(mode: CoordinateMode): String =
        if (mode == CoordinateMode.PIXELS) KEY_SCROLL_SPEED_PX else KEY_SCROLL_SPEED_RATIO

    /** モード別のズーム時間保存キーを返します。 */
    private fun zoomDurationKey(mode: CoordinateMode): String =
        if (mode == CoordinateMode.PIXELS) KEY_ZOOM_DURATION_PX else KEY_ZOOM_DURATION_RATIO

    /** モード別の移動速度既定値を返します。 */
    private fun moveSpeedDefault(mode: CoordinateMode): Int =
        if (mode == CoordinateMode.PIXELS) DEFAULT_MOVE_SPEED_PX else DEFAULT_MOVE_SPEED_RATIO

    /** モード別の移動速度最小値を返します。 */
    private fun moveSpeedMin(mode: CoordinateMode): Int =
        if (mode == CoordinateMode.PIXELS) MIN_MOVE_SPEED_PX else MIN_MOVE_SPEED_RATIO

    /** モード別の移動速度最大値を返します。 */
    private fun moveSpeedMax(mode: CoordinateMode): Int =
        if (mode == CoordinateMode.PIXELS) MAX_MOVE_SPEED_PX else MAX_MOVE_SPEED_RATIO

    /** モード別のスクロール距離既定値を返します。 */
    private fun scrollDistanceDefault(mode: CoordinateMode): Int =
        if (mode == CoordinateMode.PIXELS) DEFAULT_SCROLL_DISTANCE_PX else DEFAULT_SCROLL_DISTANCE_RATIO

    /** モード別のスクロール距離最小値を返します。 */
    private fun scrollDistanceMin(mode: CoordinateMode): Int =
        if (mode == CoordinateMode.PIXELS) MIN_SCROLL_DISTANCE_PX else MIN_SCROLL_DISTANCE_RATIO

    /** モード別のスクロール距離最大値を返します。 */
    private fun scrollDistanceMax(mode: CoordinateMode): Int =
        if (mode == CoordinateMode.PIXELS) MAX_SCROLL_DISTANCE_PX else MAX_SCROLL_DISTANCE_RATIO

    /** モード別のズーム量既定値を返します。 */
    private fun zoomAmountDefault(mode: CoordinateMode): Int =
        if (mode == CoordinateMode.PIXELS) DEFAULT_ZOOM_AMOUNT_PX else DEFAULT_ZOOM_AMOUNT_RATIO

    /** モード別のズーム量最小値を返します。 */
    private fun zoomAmountMin(mode: CoordinateMode): Int =
        if (mode == CoordinateMode.PIXELS) MIN_ZOOM_AMOUNT_PX else MIN_ZOOM_AMOUNT_RATIO

    /** モード別のズーム量最大値を返します。 */
    private fun zoomAmountMax(mode: CoordinateMode): Int =
        if (mode == CoordinateMode.PIXELS) MAX_ZOOM_AMOUNT_PX else MAX_ZOOM_AMOUNT_RATIO

    companion object {
        const val NOT_SET = 3000
        const val DEFAULT_MOVE_SPEED_PX = 30
        const val DEFAULT_MOVE_SPEED_RATIO = 35
        const val MIN_MOVE_SPEED_PX = 5
        const val MAX_MOVE_SPEED_PX = 100
        const val MIN_MOVE_SPEED_RATIO = 1
        const val MAX_MOVE_SPEED_RATIO = 100
        const val DEFAULT_MOVE_ACCELERATION = 100
        const val MIN_MOVE_ACCELERATION = 0
        const val MAX_MOVE_ACCELERATION = 500
        const val DEFAULT_SCROLL_DISTANCE_PX = 200
        const val MIN_SCROLL_DISTANCE_PX = 50
        const val MAX_SCROLL_DISTANCE_PX = 500
        const val DEFAULT_SCROLL_DISTANCE_RATIO = 20
        const val MIN_SCROLL_DISTANCE_RATIO = 1
        const val MAX_SCROLL_DISTANCE_RATIO = 50
        const val DEFAULT_SCROLL_SPEED = 8
        const val MIN_SCROLL_SPEED = 1
        const val MAX_SCROLL_SPEED = 10
        const val DEFAULT_ZOOM_AMOUNT_PX = 120
        const val MIN_ZOOM_AMOUNT_PX = 48
        const val MAX_ZOOM_AMOUNT_PX = 300
        const val DEFAULT_ZOOM_AMOUNT_RATIO = 12
        const val MIN_ZOOM_AMOUNT_RATIO = 2
        const val MAX_ZOOM_AMOUNT_RATIO = 30
        const val DEFAULT_ZOOM_DURATION = 300
        const val MIN_ZOOM_DURATION = 100
        const val MAX_ZOOM_DURATION = 1000

        private const val PREF_NAME = "com.nnnnnnn0090.hardkeypointer.PREFS_V2"
        private const val KEY_COORDINATE_MODE = "coordinateMode"
        private const val KEY_MOVE_SPEED_PX = "moveSpeedPx"
        private const val KEY_MOVE_SPEED_RATIO = "moveSpeedRatio"
        private const val KEY_MOVE_ACCELERATION_PX = "moveAccelerationPx"
        private const val KEY_MOVE_ACCELERATION_RATIO = "moveAccelerationRatio"
        private const val KEY_SCROLL_DISTANCE_PX = "scrollDistancePx"
        private const val KEY_SCROLL_DISTANCE_RATIO = "scrollDistanceRatio"
        private const val KEY_SCROLL_SPEED_PX = "scrollSpeedPx"
        private const val KEY_SCROLL_SPEED_RATIO = "scrollSpeedRatio"
        private const val KEY_ZOOM_AMOUNT_PX = "zoomAmountPx"
        private const val KEY_ZOOM_AMOUNT_RATIO = "zoomAmountRatio"
        private const val KEY_ZOOM_DURATION_PX = "zoomDurationPx"
        private const val KEY_ZOOM_DURATION_RATIO = "zoomDurationRatio"
    }
}
