package com.nnnnnnn0090.hardkeypointer

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.view.KeyEvent
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {
    private val buttons = mutableMapOf<String, Button>()
    private lateinit var moveSpeedEditText: EditText
    private lateinit var moveAccelEditText: EditText
    private var currentButton: Button? = null
    private var currentAction: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        if (!AccessibilityUtils.isAccessibilityServiceEnabled(this)) {
            AccessibilityUtils.redirectToAccessibilitySettings(this)
        } else {
            val serviceIntent = Intent(this, TapService::class.java)
            startService(serviceIntent)
        }

        initializeButtons()
        
        moveSpeedEditText = findViewById(R.id.moveSpeedEditText)
        moveAccelEditText = findViewById(R.id.moveAccelEditText)

        loadPreferences()
        setupListeners()

        val licenseButton: Button = findViewById(R.id.license_button)
        licenseButton.setOnClickListener {
            LicenseUtils.showLicenseDialog(this)
        }
    }

    private fun initializeButtons() {
        val buttonIds = mapOf(
            "up" to R.id.upKeyCodeButton, "down" to R.id.downKeyCodeButton,
            "left" to R.id.leftKeyCodeButton, "right" to R.id.rightKeyCodeButton,
            "tap" to R.id.tapKeyCodeButton, "disable" to R.id.disableKeyCodeButton,
            "scrollup" to R.id.scrollupKeyCodeButton, "scrolldown" to R.id.scrolldownKeyCodeButton,
            "scrollright" to R.id.scrollrightKeyCodeButton, "scrollleft" to R.id.scrollleftKeyCodeButton
        )
        buttonIds.forEach { (action, id) -> buttons[action] = findViewById(id) }
    }

    private fun setupListeners() {
        buttons.forEach { (action, button) ->
            button.setOnClickListener { startKeyInputMode(button, action) }
            button.setOnLongClickListener {
                button.text = "0"
                SettingsManager.setKeyCode(this, action, 3000)
                Toast.makeText(this, getString(R.string.not_set), Toast.LENGTH_SHORT).show()
                true
            }
        }

        moveSpeedEditText.setOnEditorActionListener { _, _, _ ->
            moveSpeedEditText.text.toString().toIntOrNull()?.let {
                moveSpeedEditText.clearFocus()
                SettingsManager.setMoveSpeed(this, it)
                Toast.makeText(this, getString(R.string.move_speed_saved), Toast.LENGTH_SHORT).show()
            }
            true
        }

        moveAccelEditText.setOnEditorActionListener { _, _, _ ->
            moveAccelEditText.text.toString().toIntOrNull()?.let {
                moveAccelEditText.clearFocus()
                SettingsManager.setMoveAccel(this, it)
                Toast.makeText(this, getString(R.string.move_accel_saved), Toast.LENGTH_SHORT).show()
            }
            true
        }
    }

    @SuppressLint("SetTextI18n")
    private fun loadPreferences() {
        buttons.forEach { (action, button) ->
            button.text = SettingsManager.getKeyCode(this, action).toString()
        }
        moveSpeedEditText.setText(SettingsManager.getMoveSpeed(this).toString())
        moveAccelEditText.setText(SettingsManager.getMoveAccel(this).toString())
    }

    @SuppressLint("SetTextI18n")
    private fun startKeyInputMode(button: Button, action: String) {
        currentButton = button
        currentAction = action
        button.text = getString(R.string.waiting_for_key_input)
    }

    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        if (event.action == KeyEvent.ACTION_DOWN && currentButton != null && currentAction != null) {
            val keyCode = event.keyCode
            currentButton?.text = keyCode.toString()
            Toast.makeText(this, getString(R.string.key_code_set, keyCode), Toast.LENGTH_SHORT).show()
            SettingsManager.setKeyCode(this, currentAction!!, keyCode)
            currentButton = null
            currentAction = null
            return true
        }
        return super.dispatchKeyEvent(event)
    }
}
