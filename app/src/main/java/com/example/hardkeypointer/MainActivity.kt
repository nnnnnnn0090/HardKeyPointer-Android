/*
 * HardKeyPointer-Android
 * Copyright (c) 2024-2026 nnnnnnn0090
 * 作者: nnnnnnn0090
 * ライセンス: リポジトリの LICENSE を参照してください。
 */
package com.nnnnnnn0090.hardkeypointer

import android.os.Bundle
import android.view.KeyEvent
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.google.android.material.chip.Chip
import com.google.android.material.slider.Slider

/** キー割り当て、移動設定、サービス状態を管理する設定画面です。 */
class MainActivity : AppCompatActivity() {
    private lateinit var settings: SettingsRepository
    private lateinit var serviceStatusChip: Chip
    private lateinit var serviceStatusDetail: TextView
    private val bindingButtons = mutableMapOf<PointerAction, Button>()
    private var captureButton: Button? = null
    private var captureAction: PointerAction? = null
    private var pendingCapturedKeyCode: Int? = null

    /** 画面を生成し、設定ボタン・スライダー・ライセンス表示を初期化します。 */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        settings = SettingsRepository(applicationContext)
        setContentView(R.layout.activity_main)

        serviceStatusChip = findViewById(R.id.serviceStatusChip)
        serviceStatusDetail = findViewById(R.id.serviceStatusDetail)
        findViewById<Button>(R.id.openAccessibilitySettingsButton).setOnClickListener {
            AccessibilityUtils.openAccessibilitySettings(this)
        }

        initializeBindingButtons()
        initializeSliders()
        findViewById<Button>(R.id.license_button).setOnClickListener {
            LicenseUtils.showLicenseDialog(this)
        }
    }

    /** 画面へ戻るたびにサービス状態と保存設定を再描画します。 */
    override fun onResume() {
        super.onResume()
        updateServiceStatus()
        renderSettings()
    }

    /** 画面を離れる際にキー入力待ちを安全に解除します。 */
    override fun onPause() {
        cancelKeyCapture()
        super.onPause()
    }

    /** 各操作ボタンへキー設定と長押し解除のリスナーを登録します。 */
    private fun initializeBindingButtons() {
        val buttonIds = mapOf(
            PointerAction.UP to R.id.upKeyCodeButton,
            PointerAction.DOWN to R.id.downKeyCodeButton,
            PointerAction.LEFT to R.id.leftKeyCodeButton,
            PointerAction.RIGHT to R.id.rightKeyCodeButton,
            PointerAction.TAP to R.id.tapKeyCodeButton,
            PointerAction.TOGGLE to R.id.disableKeyCodeButton,
            PointerAction.SCROLL_UP to R.id.scrollupKeyCodeButton,
            PointerAction.SCROLL_DOWN to R.id.scrolldownKeyCodeButton,
            PointerAction.SCROLL_LEFT to R.id.scrollleftKeyCodeButton,
            PointerAction.SCROLL_RIGHT to R.id.scrollrightKeyCodeButton
        )
        buttonIds.forEach { (action, id) ->
            val button = findViewById<Button>(id)
            bindingButtons[action] = button
            button.setOnClickListener { beginKeyCapture(action, button) }
            button.setOnLongClickListener {
                cancelKeyCapture()
                settings.setKeyCode(action, SettingsRepository.NOT_SET)
                button.text = getString(R.string.not_set)
                Toast.makeText(this, R.string.not_set, Toast.LENGTH_SHORT).show()
                true
            }
        }
    }

    /** 移動速度・加速度・スクロール距離のスライダーを初期化します。 */
    private fun initializeSliders() {
        configureSlider(
            slider = findViewById(R.id.moveSpeedSlider),
            value = findViewById(R.id.moveSpeedValue),
            initialValue = settings.getMoveSpeed().toFloat(),
            onChange = settings::setMoveSpeed,
            formatter = { getString(R.string.value_px, it) }
        )
        configureSlider(
            slider = findViewById(R.id.moveAccelSlider),
            value = findViewById(R.id.moveAccelValue),
            initialValue = settings.getMoveAcceleration().toFloat(),
            onChange = settings::setMoveAcceleration,
            formatter = { getString(R.string.value_percent, it) }
        )
        configureSlider(
            slider = findViewById(R.id.scrollDistanceSlider),
            value = findViewById(R.id.scrollDistanceValue),
            initialValue = settings.getScrollDistance().toFloat(),
            onChange = settings::setScrollDistance,
            formatter = { getString(R.string.value_px, it) }
        )
    }

    /** スライダーの初期値、表示文字列、保存処理を設定します。 */
    private fun configureSlider(
        slider: Slider,
        value: TextView,
        initialValue: Float,
        onChange: (Int) -> Unit,
        formatter: (Int) -> String
    ) {
        slider.value = initialValue.coerceIn(slider.valueFrom, slider.valueTo)
        value.text = formatter(slider.value.toInt())
        slider.addOnChangeListener { _, newValue, _ ->
            val intValue = newValue.toInt()
            value.text = formatter(intValue)
            onChange(intValue)
        }
    }

    /** ユーザー補助サービスの有効状態を画面へ反映します。 */
    private fun updateServiceStatus() {
        val enabled = AccessibilityUtils.isAccessibilityServiceEnabled(this)
        serviceStatusChip.text = getString(
            if (enabled) R.string.service_enabled else R.string.service_disabled
        )
        serviceStatusDetail.setText(
            if (enabled) R.string.service_status_enabled_detail
            else R.string.service_status_disabled_detail
        )
        val backgroundColor = if (enabled) R.color.primary_container else R.color.error_container
        val textColor = if (enabled) R.color.on_primary_container else R.color.on_error_container
        serviceStatusChip.setChipBackgroundColorResource(backgroundColor)
        serviceStatusChip.setTextColor(ContextCompat.getColor(this, textColor))
        findViewById<Button>(R.id.openAccessibilitySettingsButton).visibility =
            if (enabled) Button.GONE else Button.VISIBLE
    }

    /** 保存済みのキー名と数値設定を画面へ描画します。 */
    private fun renderSettings() {
        bindingButtons.forEach { (action, button) ->
            button.text = KeyNameFormatter.format(this, settings.getKeyCode(action))
        }
        setSliderValue(R.id.moveSpeedSlider, settings.getMoveSpeed())
        setSliderValue(R.id.moveAccelSlider, settings.getMoveAcceleration())
        setSliderValue(R.id.scrollDistanceSlider, settings.getScrollDistance())
        findViewById<TextView>(R.id.moveSpeedValue).text =
            getString(R.string.value_px, settings.getMoveSpeed())
        findViewById<TextView>(R.id.moveAccelValue).text =
            getString(R.string.value_percent, settings.getMoveAcceleration())
        findViewById<TextView>(R.id.scrollDistanceValue).text =
            getString(R.string.value_px, settings.getScrollDistance())
    }

    /** 指定IDのスライダーへ範囲内の値を設定します。 */
    private fun setSliderValue(id: Int, value: Int) {
        findViewById<Slider>(id).value = value.toFloat()
    }

    /** 指定ボタンをフォーカスし、次の物理キーを割り当てる状態にします。 */
    private fun beginKeyCapture(action: PointerAction, button: Button) {
        button.isFocusableInTouchMode = true
        button.requestFocus()
        captureAction = action
        captureButton = button
        pendingCapturedKeyCode = null
        KeyCaptureState.begin()
        button.text = getString(R.string.waiting_for_key_input)
    }

    /** キー設定中の入力を保存し、KEY_UPまでイベントを消費します。 */
    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        // KEY_UP まで選択キーを消費します。押しっぱなしやリピート入力が、
        // 設定直後の新しい操作として実行されることを防ぎます。
        val pendingKey = pendingCapturedKeyCode
        if (KeyCaptureState.isActive && pendingKey != null && pendingKey == event.keyCode) {
            if (event.action == KeyEvent.ACTION_UP) finishKeyCapture()
            return true
        }

        val action = captureAction
        val button = captureButton
        if (event.action == KeyEvent.ACTION_DOWN && action != null && button != null) {
            pendingCapturedKeyCode = event.keyCode
            if (isAlreadyAssigned(action, event.keyCode)) {
                Toast.makeText(this, R.string.key_already_assigned, Toast.LENGTH_SHORT).show()
                button.text = KeyNameFormatter.format(this, settings.getKeyCode(action))
                captureAction = null
                captureButton = null
                return true
            }
            settings.setKeyCode(action, event.keyCode)
            button.text = KeyNameFormatter.format(this, event.keyCode)
            Toast.makeText(
                this,
                getString(R.string.key_code_set, event.keyCode),
                Toast.LENGTH_SHORT
            ).show()
            captureAction = null
            captureButton = null
            return true
        }
        return super.dispatchKeyEvent(event)
    }

    /** 指定キーが別の操作へ重複割り当てされているか確認します。 */
    private fun isAlreadyAssigned(action: PointerAction, keyCode: Int): Boolean =
        keyCode != KeyEvent.KEYCODE_UNKNOWN &&
            PointerAction.entries.any { other ->
                other != action && settings.getKeyCode(other) == keyCode
            }

    /** キー設定に必要な一時状態をすべて破棄します。 */
    private fun finishKeyCapture() {
        pendingCapturedKeyCode = null
        captureAction = null
        captureButton = null
        KeyCaptureState.finish()
    }

    /** キー設定をキャンセルし、表示を保存済みの値へ戻します。 */
    private fun cancelKeyCapture() {
        val action = captureAction
        captureButton?.let { button ->
            if (action != null) button.text = KeyNameFormatter.format(this, settings.getKeyCode(action))
        }
        finishKeyCapture()
    }
}
