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
    private lateinit var disableKeyCodeButton: Button
    private lateinit var scrollupKeyCodeButton: Button
    private lateinit var scrolldownKeyCodeButton: Button
    private lateinit var scrollrightKeyCodeButton: Button
    private lateinit var scrollleftKeyCodeButton: Button

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
        const val KEY_DISABLE_CODE = "KEY_DISABLE_CODE"
        const val KEY_SCROLLUP_CODE = "KEY_SCROLLUP_CODE"
        const val KEY_SCROLLDOWN_CODE = "KEY_SCROLLDOWN_CODE"
        const val KEY_SCROLLRIGHT_CODE = "KEY_SCROLLRIGHT_CODE"
        const val KEY_SCROLLLEFT_CODE = "KEY_SCROLLLEFT_CODE"
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
        disableKeyCodeButton = findViewById(R.id.disableKeyCodeButton)
        scrollupKeyCodeButton = findViewById(R.id.scrollupKeyCodeButton)
        scrolldownKeyCodeButton = findViewById(R.id.scrolldownKeyCodeButton)
        scrollrightKeyCodeButton = findViewById(R.id.scrollrightKeyCodeButton)
        scrollleftKeyCodeButton = findViewById(R.id.scrollleftKeyCodeButton)

        loadPreferences()

        upKeyCodeButton.setOnClickListener { startKeyInputMode(upKeyCodeButton, KEY_UP_CODE) }
        downKeyCodeButton.setOnClickListener { startKeyInputMode(downKeyCodeButton, KEY_DOWN_CODE) }
        leftKeyCodeButton.setOnClickListener { startKeyInputMode(leftKeyCodeButton, KEY_LEFT_CODE) }
        rightKeyCodeButton.setOnClickListener { startKeyInputMode(rightKeyCodeButton, KEY_RIGHT_CODE) }
        tapKeyCodeButton.setOnClickListener { startKeyInputMode(tapKeyCodeButton, KEY_TAP_CODE) }
        disableKeyCodeButton.setOnClickListener { startKeyInputMode(disableKeyCodeButton, KEY_DISABLE_CODE) }
        scrollupKeyCodeButton.setOnClickListener { startKeyInputMode(scrollupKeyCodeButton, KEY_SCROLLUP_CODE) }
        scrolldownKeyCodeButton.setOnClickListener { startKeyInputMode(scrolldownKeyCodeButton, KEY_SCROLLDOWN_CODE) }
        scrollrightKeyCodeButton.setOnClickListener { startKeyInputMode(scrollrightKeyCodeButton, KEY_SCROLLRIGHT_CODE) }
        scrollleftKeyCodeButton.setOnClickListener { startKeyInputMode(scrollleftKeyCodeButton, KEY_SCROLLLEFT_CODE) }

        setLongPressListener(upKeyCodeButton, KEY_UP_CODE)
        setLongPressListener(downKeyCodeButton, KEY_DOWN_CODE)
        setLongPressListener(leftKeyCodeButton, KEY_LEFT_CODE)
        setLongPressListener(rightKeyCodeButton, KEY_RIGHT_CODE)
        setLongPressListener(tapKeyCodeButton, KEY_TAP_CODE)
        setLongPressListener(disableKeyCodeButton, KEY_DISABLE_CODE)
        setLongPressListener(scrollupKeyCodeButton, KEY_SCROLLUP_CODE)
        setLongPressListener(scrolldownKeyCodeButton, KEY_SCROLLDOWN_CODE)
        setLongPressListener(scrollrightKeyCodeButton, KEY_SCROLLRIGHT_CODE)
        setLongPressListener(scrollleftKeyCodeButton, KEY_SCROLLLEFT_CODE)

        moveSpeedEditText.setOnEditorActionListener { _, _, _ ->
            val speed = moveSpeedEditText.text.toString().toIntOrNull()
            if (speed != null) {
                moveSpeedEditText.clearFocus()
                saveKeyCodeToPreferences(KEY_MOVE_SPEED, speed)
                Toast.makeText(this@MainActivity, getString(R.string.move_speed_saved), Toast.LENGTH_SHORT).show()            }
            true
        }

        val licenseButton: Button = findViewById(R.id.license_button)
        licenseButton.setOnClickListener {
            LicenseUtils.showLicenseDialog(this)
        }
    }

    private fun setLongPressListener(button: Button, key: String) {
        button.setOnLongClickListener {
            button.text = "0"
            saveKeyCodeToPreferences(key, 3000)
            Toast.makeText(this, getString(R.string.not_set), Toast.LENGTH_SHORT).show()
            true
        }
    }

    @SuppressLint("SetTextI18n")
    private fun loadPreferences() {
        upKeyCodeButton.text = sharedPreferences.getInt(KEY_UP_CODE, KeyEvent.KEYCODE_DPAD_UP).toString()
        downKeyCodeButton.text = sharedPreferences.getInt(KEY_DOWN_CODE, KeyEvent.KEYCODE_DPAD_DOWN).toString()
        leftKeyCodeButton.text = sharedPreferences.getInt(KEY_LEFT_CODE, KeyEvent.KEYCODE_DPAD_LEFT).toString()
        rightKeyCodeButton.text = sharedPreferences.getInt(KEY_RIGHT_CODE,  KeyEvent.KEYCODE_DPAD_RIGHT).toString()
        tapKeyCodeButton.text = sharedPreferences.getInt(KEY_TAP_CODE, KeyEvent.KEYCODE_ENTER).toString()
        moveSpeedEditText.setText(sharedPreferences.getInt(KEY_MOVE_SPEED, 10).toString())
        disableKeyCodeButton.text = sharedPreferences.getInt(KEY_DISABLE_CODE, KeyEvent.KEYCODE_VOLUME_DOWN).toString()
        scrollupKeyCodeButton.text = sharedPreferences.getInt(KEY_SCROLLUP_CODE, KeyEvent.KEYCODE_2).toString()
        scrolldownKeyCodeButton.text = sharedPreferences.getInt(KEY_SCROLLDOWN_CODE, KeyEvent.KEYCODE_5).toString()
        scrollleftKeyCodeButton.text = sharedPreferences.getInt(KEY_SCROLLLEFT_CODE, KeyEvent.KEYCODE_4).toString()
        scrollrightKeyCodeButton.text = sharedPreferences.getInt(KEY_SCROLLRIGHT_CODE, KeyEvent.KEYCODE_6).toString()
    }

    @SuppressLint("SetTextI18n")
    private fun startKeyInputMode(button: Button, key: String) {
        currentButton = button
        button.text = getString(R.string.waiting_for_key_input)

        button.setOnKeyListener { _, keyCode, event ->
            if (event.action == KeyEvent.ACTION_DOWN) {
                button.text = keyCode.toString()
                Toast.makeText(this, getString(R.string.key_code_set, keyCode), Toast.LENGTH_SHORT).show()
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
                KEY_DISABLE_CODE -> putInt(KEY_DISABLE_CODE, keyCode as Int)
                KEY_SCROLLUP_CODE -> putInt(KEY_SCROLLUP_CODE, keyCode as Int)
                KEY_SCROLLDOWN_CODE -> putInt(KEY_SCROLLDOWN_CODE, keyCode as Int)
                KEY_SCROLLLEFT_CODE -> putInt(KEY_SCROLLLEFT_CODE, keyCode as Int)
                KEY_SCROLLRIGHT_CODE -> putInt(KEY_SCROLLRIGHT_CODE, keyCode as Int)
            }
            apply()
        }
    }
}
