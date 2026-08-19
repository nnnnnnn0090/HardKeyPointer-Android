/*
 * HardKeyPointer-Android
 * Copyright (c) 2024-2026 nnnnnnn0090
 * 作者: nnnnnnn0090
 * ライセンス: リポジトリの LICENSE を参照してください。
 */
package com.nnnnnnn0090.hardkeypointer

import android.os.Bundle
import android.view.KeyEvent
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.appcompat.widget.PopupMenu
import com.google.android.material.button.MaterialButtonToggleGroup
import com.google.android.material.slider.Slider
import kotlin.math.roundToInt

/** キー割り当て、移動設定、サービス状態を管理する設定画面です。 */
class MainActivity : AppCompatActivity() {
    private lateinit var settings: SettingsRepository
    private lateinit var serviceStatusLabel: TextView
    private lateinit var serviceStatusDetail: TextView
    private val bindingButtons = mutableMapOf<PointerAction, Button>()
    private val triggerModeButtons = mutableMapOf<PointerAction, Button>()
    private var captureButton: Button? = null
    private var captureAction: PointerAction? = null
    private var pendingCapturedKeyCode: Int? = null

    /** 画面を生成し、設定ボタン・スライダー・ライセンス表示を初期化します。 */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        settings = SettingsRepository(applicationContext)
        setContentView(R.layout.activity_main)

        serviceStatusLabel = findViewById(R.id.serviceStatusLabel)
        serviceStatusDetail = findViewById(R.id.serviceStatusDetail)
        findViewById<Button>(R.id.openAccessibilitySettingsButton).setOnClickListener {
            AccessibilityUtils.openAccessibilitySettings(this)
        }

        initializeBindingButtons()
        initializeTriggerModeControls()
        initializeCoordinateMode()
        initializeSliders()
        findViewById<Button>(R.id.license_button).setOnClickListener {
            LicenseUtils.showLicenseDialog(this)
        }
        findViewById<Button>(R.id.resetSettingsButton).setOnClickListener {
            showResetSettingsConfirmation()
        }
    }

    /** pxモードと割合モードの切り替えを初期化します。 */
    private fun initializeCoordinateMode() {
        val group = findViewById<MaterialButtonToggleGroup>(R.id.coordinateModeGroup)
        group.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (!isChecked) return@addOnButtonCheckedListener
            val mode = when (checkedId) {
                R.id.pixelModeButton -> CoordinateMode.PIXELS
                R.id.ratioModeButton -> CoordinateMode.RATIO
                else -> return@addOnButtonCheckedListener
            }
            if (mode != settings.getCoordinateMode()) {
                settings.setCoordinateMode(mode)
                renderSettings()
            }
        }
    }

    /** 画面へ戻るたびにサービス状態と保存設定を再描画します。 */
    override fun onResume() {
        super.onResume()
        updateServiceStatus()
        if (!AccessibilityUtils.isAccessibilityServiceEnabled(this)) {
            AccessibilityUtils.clearReturnToAppRequest(this)
        }
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
            PointerAction.SCROLL_RIGHT to R.id.scrollrightKeyCodeButton,
            PointerAction.ZOOM_IN to R.id.zoomInKeyCodeButton,
            PointerAction.ZOOM_OUT to R.id.zoomOutKeyCodeButton
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

    /** 各キー設定へ即押し・長押しの発動方式セレクターを追加します。 */
    private fun initializeTriggerModeControls() {
        bindingButtons.forEach { (action, button) ->
            val row = button.parent as? ViewGroup ?: return@forEach
            val rowParent = row.parent as? ViewGroup ?: return@forEach
            val rowIndex = rowParent.indexOfChild(row)
            if (rowIndex < 0) return@forEach

            val originalParams = row.layoutParams as? LinearLayout.LayoutParams
            val containerParams = originalParams?.let { LinearLayout.LayoutParams(it) }
                ?: LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
            containerParams.topMargin = 0
            val container = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                layoutParams = containerParams
                val verticalPadding = resources.getDimensionPixelSize(
                    R.dimen.action_setting_vertical_padding
                )
                setPadding(0, verticalPadding, 0, verticalPadding)
            }
            rowParent.removeViewAt(rowIndex)
            row.layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            container.addView(row)

            val control = layoutInflater.inflate(
                R.layout.trigger_mode_button,
                container,
                false
            )
            container.addView(control)

            val modeButton = control.findViewById<Button>(R.id.triggerModeButton)
            triggerModeButtons[action] = modeButton
            updateTriggerModeButton(action)
            modeButton.setOnClickListener {
                showTriggerModeMenu(action, modeButton)
            }

            if (rowIndex > 1) {
                rowParent.addView(createActionDivider(), rowIndex)
                rowParent.addView(container, rowIndex + 1)
            } else {
                rowParent.addView(container, rowIndex)
            }
        }
    }

    /** 操作設定の境界を示す、控えめな区切り線を生成します。 */
    private fun createActionDivider(): View {
        val height = resources.getDimensionPixelSize(R.dimen.action_setting_divider_height)
        return View(this).apply {
            setBackgroundColor(ContextCompat.getColor(this@MainActivity, R.color.outline_variant))
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                height
            )
        }
    }

    /** 現在の発動方式をボタンの表示とアクセシビリティ説明へ反映します。 */
    private fun updateTriggerModeButton(action: PointerAction) {
        val modeButton = triggerModeButtons[action] ?: return
        val mode = settings.getTriggerMode(action)
        val label = getString(
            if (mode == TriggerMode.IMMEDIATE) {
                R.string.trigger_mode_immediate
            } else {
                R.string.trigger_mode_long_press
            }
        )
        modeButton.text = getString(R.string.trigger_mode_button_text, label)
        modeButton.contentDescription = getString(
            R.string.trigger_mode_content_description,
            getString(R.string.trigger_mode_label),
            label
        )
    }

    /** 操作ごとの発動方式をポップアップメニューから変更します。 */
    private fun showTriggerModeMenu(action: PointerAction, anchor: Button) {
        PopupMenu(this, anchor).apply {
            menu.add(0, TRIGGER_MENU_IMMEDIATE, 0, R.string.trigger_mode_immediate)
            menu.add(0, TRIGGER_MENU_LONG_PRESS, 1, R.string.trigger_mode_long_press)
            setOnMenuItemClickListener { item ->
                val mode = when (item.itemId) {
                    TRIGGER_MENU_IMMEDIATE -> TriggerMode.IMMEDIATE
                    TRIGGER_MENU_LONG_PRESS -> TriggerMode.LONG_PRESS
                    else -> return@setOnMenuItemClickListener false
                }
                settings.setTriggerMode(action, mode)
                updateTriggerModeButton(action)
                true
            }
        }.show()
    }

    /** 移動、加速度、スクロール、ズームの各スライダーを初期化します。 */
    private fun initializeSliders() {
        configureSpatialSliders()
        configureModeSpecificSliders()
    }

    /** 座標モードごとに独立した速度・加速度系スライダーを初期化します。 */
    private fun configureModeSpecificSliders() {
        val mode = settings.getCoordinateMode()
        configureSlider(
            slider = findViewById(R.id.moveAccelSlider),
            value = findViewById(R.id.moveAccelValue),
            initialValue = settings.getMoveAcceleration(mode).toFloat(),
            onChange = { settings.setMoveAcceleration(mode, it) },
            formatter = { getString(R.string.value_percent, it) }
        )
        configureSlider(
            slider = findViewById(R.id.scrollSpeedSlider),
            value = findViewById(R.id.scrollSpeedValue),
            initialValue = settings.getScrollSpeed(mode).toFloat(),
            onChange = { settings.setScrollSpeed(mode, it) },
            formatter = { getString(R.string.value_speed, it) }
        )
        configureSlider(
            slider = findViewById(R.id.zoomDurationSlider),
            value = findViewById(R.id.zoomDurationValue),
            initialValue = settings.getZoomDuration(mode).toFloat(),
            onChange = { settings.setZoomDuration(mode, it) },
            formatter = { getString(R.string.value_ms, it) }
        )
    }

    /** 現在の座標モードに合わせて距離系スライダーを再構成します。 */
    private fun configureSpatialSliders() {
        val mode = settings.getCoordinateMode()
        val isPixels = mode == CoordinateMode.PIXELS
        configureSlider(
            slider = findViewById(R.id.moveSpeedSlider),
            value = findViewById(R.id.moveSpeedValue),
            initialValue = settings.getMoveSpeed(mode).toFloat(),
            min = if (isPixels) SettingsRepository.MIN_MOVE_SPEED_PX.toFloat()
            else SettingsRepository.MIN_MOVE_SPEED_RATIO.toFloat(),
            max = if (isPixels) SettingsRepository.MAX_MOVE_SPEED_PX.toFloat()
            else SettingsRepository.MAX_MOVE_SPEED_RATIO.toFloat(),
            step = 1f,
            onChange = { settings.setMoveSpeed(mode, it) },
            formatter = {
                getString(if (isPixels) R.string.value_px_per_frame else R.string.value_percent_per_second, it)
            }
        )
        configureSlider(
            slider = findViewById(R.id.scrollDistanceSlider),
            value = findViewById(R.id.scrollDistanceValue),
            initialValue = settings.getScrollDistance(mode).toFloat(),
            min = if (isPixels) SettingsRepository.MIN_SCROLL_DISTANCE_PX.toFloat()
            else SettingsRepository.MIN_SCROLL_DISTANCE_RATIO.toFloat(),
            max = if (isPixels) SettingsRepository.MAX_SCROLL_DISTANCE_PX.toFloat()
            else SettingsRepository.MAX_SCROLL_DISTANCE_RATIO.toFloat(),
            step = if (isPixels) 10f else 1f,
            onChange = { settings.setScrollDistance(mode, it) },
            formatter = { getString(if (isPixels) R.string.value_px else R.string.value_percent, it) }
        )
        configureSlider(
            slider = findViewById(R.id.zoomAmountSlider),
            value = findViewById(R.id.zoomAmountValue),
            initialValue = settings.getZoomAmount(mode).toFloat(),
            min = if (isPixels) SettingsRepository.MIN_ZOOM_AMOUNT_PX.toFloat()
            else SettingsRepository.MIN_ZOOM_AMOUNT_RATIO.toFloat(),
            max = if (isPixels) SettingsRepository.MAX_ZOOM_AMOUNT_PX.toFloat()
            else SettingsRepository.MAX_ZOOM_AMOUNT_RATIO.toFloat(),
            step = if (isPixels) 12f else 1f,
            onChange = { settings.setZoomAmount(mode, it) },
            formatter = { getString(if (isPixels) R.string.value_px else R.string.value_percent, it) }
        )
    }

    /** スライダーの初期値、表示文字列、保存処理を設定します。 */
    private fun configureSlider(
        slider: Slider,
        value: TextView,
        initialValue: Float,
        min: Float = slider.valueFrom,
        max: Float = slider.valueTo,
        step: Float = slider.stepSize,
        onChange: (Int) -> Unit,
        formatter: (Int) -> String
    ) {
        slider.valueFrom = min
        slider.valueTo = max
        slider.stepSize = step
        slider.clearOnChangeListeners()
        slider.value = normalizeSliderValue(slider, initialValue)
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
        serviceStatusLabel.text = getString(
            if (enabled) R.string.service_enabled else R.string.service_disabled
        )
        serviceStatusDetail.setText(
            if (enabled) R.string.service_status_enabled_detail
            else R.string.service_status_disabled_detail
        )
        val backgroundColor = if (enabled) R.color.primary_container else R.color.error_container
        val textColor = if (enabled) R.color.on_primary_container else R.color.on_error_container
        serviceStatusLabel.backgroundTintList =
            ContextCompat.getColorStateList(this, backgroundColor)
        serviceStatusLabel.setTextColor(ContextCompat.getColor(this, textColor))
        findViewById<Button>(R.id.openAccessibilitySettingsButton).visibility =
            if (enabled) Button.GONE else Button.VISIBLE
    }

    /** 全設定を初期値へ戻す確認ダイアログを表示します。 */
    private fun showResetSettingsConfirmation() {
        AlertDialog.Builder(this)
            .setTitle(R.string.reset_settings_title)
            .setMessage(R.string.reset_settings_message)
            .setNegativeButton(R.string.cancel_button, null)
            .setPositiveButton(R.string.reset_settings_confirm) { _, _ ->
                resetSettings()
            }
            .show()
    }

    /** 全設定を消去して、初期状態を画面へ再描画します。 */
    private fun resetSettings() {
        cancelKeyCapture()
        settings.resetAll()
        renderSettings()
        Toast.makeText(this, R.string.reset_settings_completed, Toast.LENGTH_SHORT).show()
    }

    /** 保存済みのキー名と数値設定を画面へ描画します。 */
    private fun renderSettings() {
        bindingButtons.forEach { (action, button) ->
            button.text = KeyNameFormatter.format(this, settings.getKeyCode(action))
        }
        val mode = settings.getCoordinateMode()
        findViewById<MaterialButtonToggleGroup>(R.id.coordinateModeGroup).check(
            if (mode == CoordinateMode.PIXELS) R.id.pixelModeButton else R.id.ratioModeButton
        )
        findViewById<TextView>(R.id.moveSpeedLabel).setText(
            if (mode == CoordinateMode.PIXELS) R.string.move_speed_px
            else R.string.move_speed_ratio
        )
        findViewById<TextView>(R.id.scrollDistanceLabel).setText(
            if (mode == CoordinateMode.PIXELS) R.string.scroll_distance_px
            else R.string.scroll_distance_ratio
        )
        findViewById<TextView>(R.id.zoomAmountLabel).setText(
            if (mode == CoordinateMode.PIXELS) R.string.zoom_amount_px
            else R.string.zoom_amount_ratio
        )
        configureSpatialSliders()
        configureModeSpecificSliders()
        setSliderValue(R.id.moveSpeedSlider, settings.getMoveSpeed())
        setSliderValue(R.id.moveAccelSlider, settings.getMoveAcceleration(mode))
        setSliderValue(R.id.scrollDistanceSlider, settings.getScrollDistance())
        setSliderValue(R.id.scrollSpeedSlider, settings.getScrollSpeed(mode))
        setSliderValue(R.id.zoomAmountSlider, settings.getZoomAmount())
        setSliderValue(R.id.zoomDurationSlider, settings.getZoomDuration(mode))
        findViewById<TextView>(R.id.moveSpeedValue).text =
            getString(
                if (mode == CoordinateMode.PIXELS) R.string.value_px_per_frame
                else R.string.value_percent_per_second,
                settings.getMoveSpeed()
            )
        findViewById<TextView>(R.id.moveAccelValue).text =
            getString(R.string.value_percent, settings.getMoveAcceleration(mode))
        findViewById<TextView>(R.id.scrollDistanceValue).text =
            getString(
                if (mode == CoordinateMode.PIXELS) R.string.value_px
                else R.string.value_percent,
                settings.getScrollDistance()
            )
        findViewById<TextView>(R.id.scrollSpeedValue).text =
            getString(R.string.value_speed, settings.getScrollSpeed(mode))
        findViewById<TextView>(R.id.zoomAmountValue).text =
            getString(
                if (mode == CoordinateMode.PIXELS) R.string.value_px
                else R.string.value_percent,
                settings.getZoomAmount()
            )
        findViewById<TextView>(R.id.zoomDurationValue).text =
            getString(R.string.value_ms, settings.getZoomDuration(mode))
        triggerModeButtons.keys.forEach(::updateTriggerModeButton)
    }

    /** 指定IDのスライダーへ範囲内の値を設定します。 */
    private fun setSliderValue(id: Int, value: Int) {
        val slider = findViewById<Slider>(id)
        slider.value = normalizeSliderValue(slider, value.toFloat())
    }

    /** 保存値をスライダーの範囲と刻みに合わせて補正します。 */
    private fun normalizeSliderValue(slider: Slider, value: Float): Float {
        val bounded = value.coerceIn(slider.valueFrom, slider.valueTo)
        if (slider.stepSize <= 0f) return bounded
        val steps = ((bounded - slider.valueFrom) / slider.stepSize).roundToInt()
        return (slider.valueFrom + steps * slider.stepSize)
            .coerceIn(slider.valueFrom, slider.valueTo)
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

    private companion object {
        const val TRIGGER_MENU_IMMEDIATE = 1
        const val TRIGGER_MENU_LONG_PRESS = 2
    }
}
