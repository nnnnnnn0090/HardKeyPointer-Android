package com.nnnnnnn0090.hardkeypointer

import android.annotation.SuppressLint
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.view.KeyEvent
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    private lateinit var upKeyCodeButton: Button
    private lateinit var downKeyCodeButton: Button
    private lateinit var leftKeyCodeButton: Button
    private lateinit var rightKeyCodeButton: Button
    private lateinit var tapKeyCodeButton: Button
    private lateinit var moveSpeedEditText: EditText
    private lateinit var enableKeyCodeButton: Button
    private lateinit var disableKeyCodeButton: Button

    private var currentButton: Button? = null

    private lateinit var sharedPreferences: SharedPreferences

    companion object {
        const val PREF_NAME = "com.nnnnnnn0090.hardkeypointer.PREFS"
        const val KEY_UP_CODE = "KEY_UP_CODE"
        const val KEY_DOWN_CODE = "KEY_DOWN_CODE"
        const val KEY_LEFT_CODE = "KEY_LEFT_CODE"
        const val KEY_RIGHT_CODE = "KEY_RIGHT_CODE"
        const val KEY_TAP_CODE = "KEY_TAP_CODE"
        const val KEY_MOVE_SPEED = "KEY_MOVE_SPEED"
        const val KEY_ENABLE_CODE = "KEY_ENABLE_CODE"
        const val KEY_DISABLE_CODE = "KEY_DISABLE_CODE"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        if (!AccessibilityUtils.isAccessibilityServiceEnabled(this)) {
            AccessibilityUtils.redirectToAccessibilitySettings(this)
        } else {
            val serviceIntent = Intent(this, TapService::class.java)
            startService(serviceIntent)
        }

        sharedPreferences = getSharedPreferences(PREF_NAME, MODE_PRIVATE)

        upKeyCodeButton = findViewById(R.id.upKeyCodeButton)
        downKeyCodeButton = findViewById(R.id.downKeyCodeButton)
        leftKeyCodeButton = findViewById(R.id.leftKeyCodeButton)
        rightKeyCodeButton = findViewById(R.id.rightKeyCodeButton)
        tapKeyCodeButton = findViewById(R.id.tapKeyCodeButton)
        moveSpeedEditText = findViewById(R.id.moveSpeedEditText)
        enableKeyCodeButton = findViewById(R.id.enableKeyCodeButton)
        disableKeyCodeButton = findViewById(R.id.disableKeyCodeButton)

        loadPreferences()

        upKeyCodeButton.setOnClickListener { startKeyInputMode(upKeyCodeButton, KEY_UP_CODE) }
        downKeyCodeButton.setOnClickListener { startKeyInputMode(downKeyCodeButton, KEY_DOWN_CODE) }
        leftKeyCodeButton.setOnClickListener { startKeyInputMode(leftKeyCodeButton, KEY_LEFT_CODE) }
        rightKeyCodeButton.setOnClickListener { startKeyInputMode(rightKeyCodeButton, KEY_RIGHT_CODE) }
        tapKeyCodeButton.setOnClickListener { startKeyInputMode(tapKeyCodeButton, KEY_TAP_CODE) }
        enableKeyCodeButton.setOnClickListener { startKeyInputMode(enableKeyCodeButton, KEY_ENABLE_CODE) }
        disableKeyCodeButton.setOnClickListener { startKeyInputMode(disableKeyCodeButton, KEY_DISABLE_CODE) }

        moveSpeedEditText.setOnEditorActionListener { _, _, _ ->
            val speed = moveSpeedEditText.text.toString().toIntOrNull()
            if (speed != null) {
                moveSpeedEditText.clearFocus()
                saveKeyCodeToPreferences(KEY_MOVE_SPEED, speed)
                Toast.makeText(this@MainActivity, "移動速度を保存しました", Toast.LENGTH_SHORT).show()
            }
            true
        }

        val licenseButton: Button = findViewById(R.id.license_button)
        licenseButton.setOnClickListener {
            LicenseUtils.showLicenseDialog(this)
        }
    }

    @SuppressLint("SetTextI18n")
    private fun loadPreferences() {
        upKeyCodeButton.text = sharedPreferences.getInt(KEY_UP_CODE, 19).toString()
        downKeyCodeButton.text = sharedPreferences.getInt(KEY_DOWN_CODE, 20).toString()
        leftKeyCodeButton.text = sharedPreferences.getInt(KEY_LEFT_CODE, 21).toString()
        rightKeyCodeButton.text = sharedPreferences.getInt(KEY_RIGHT_CODE, 22).toString()
        tapKeyCodeButton.text = sharedPreferences.getInt(KEY_TAP_CODE, 66).toString()
        moveSpeedEditText.setText(sharedPreferences.getInt(KEY_MOVE_SPEED, 10).toString())
        enableKeyCodeButton.text = sharedPreferences.getInt(KEY_ENABLE_CODE, 24).toString()
        disableKeyCodeButton.text = sharedPreferences.getInt(KEY_DISABLE_CODE, 25).toString()
    }

    @SuppressLint("SetTextI18n")
    private fun startKeyInputMode(button: Button, key: String) {
        currentButton = button
        button.text = "キーを入力待機中..."

        button.setOnKeyListener { _, keyCode, event ->
            if (event.action == KeyEvent.ACTION_DOWN) {
                button.text = keyCode.toString()
                Toast.makeText(this, "設定しました: $keyCode", Toast.LENGTH_SHORT).show()
                saveKeyCodeToPreferences(key, keyCode)
                currentButton = null
                button.setOnKeyListener(null)
                true
            } else {
                false
            }
        }
    }

    private fun saveKeyCodeToPreferences(key: String, keyCode: Any) {
        sharedPreferences.edit().apply {
            when (key) {
                KEY_UP_CODE -> putInt(KEY_UP_CODE, keyCode as Int)
                KEY_DOWN_CODE -> putInt(KEY_DOWN_CODE, keyCode as Int)
                KEY_LEFT_CODE -> putInt(KEY_LEFT_CODE, keyCode as Int)
                KEY_RIGHT_CODE -> putInt(KEY_RIGHT_CODE, keyCode as Int)
                KEY_TAP_CODE -> putInt(KEY_TAP_CODE, keyCode as Int)
                KEY_MOVE_SPEED -> putInt(KEY_MOVE_SPEED, keyCode as Int)
                KEY_ENABLE_CODE -> putInt(KEY_ENABLE_CODE, keyCode as Int)
                KEY_DISABLE_CODE -> putInt(KEY_DISABLE_CODE, keyCode as Int)
            }
            apply()
        }
    }
}
