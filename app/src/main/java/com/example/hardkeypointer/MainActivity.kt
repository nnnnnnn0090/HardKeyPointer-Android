package com.nnnnnnn0090.hardkeypointer

import android.accessibilityservice.AccessibilityServiceInfo
import android.annotation.SuppressLint
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.view.KeyEvent
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    private lateinit var upKeyCodeButton: Button
    private lateinit var downKeyCodeButton: Button
    private lateinit var leftKeyCodeButton: Button
    private lateinit var rightKeyCodeButton: Button
    private lateinit var tapKeyCodeButton: Button
    private lateinit var moveSpeedEditText: EditText

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
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
//
//        if (!isAccessibilityServiceEnabled()) {
//            Toast.makeText(this, "アクセシビリティサービスが有効ではありません", Toast.LENGTH_SHORT).show()
//            return
//        }

        sharedPreferences = getSharedPreferences(PREF_NAME, MODE_PRIVATE)

        upKeyCodeButton = findViewById(R.id.upKeyCodeButton)
        downKeyCodeButton = findViewById(R.id.downKeyCodeButton)
        leftKeyCodeButton = findViewById(R.id.leftKeyCodeButton)
        rightKeyCodeButton = findViewById(R.id.rightKeyCodeButton)
        tapKeyCodeButton = findViewById(R.id.tapKeyCodeButton)
        moveSpeedEditText = findViewById(R.id.moveSpeedEditText)

        loadPreferences()

        upKeyCodeButton.setOnClickListener { startKeyInputMode(upKeyCodeButton, KEY_UP_CODE) }
        downKeyCodeButton.setOnClickListener { startKeyInputMode(downKeyCodeButton, KEY_DOWN_CODE) }
        leftKeyCodeButton.setOnClickListener { startKeyInputMode(leftKeyCodeButton, KEY_LEFT_CODE) }
        rightKeyCodeButton.setOnClickListener { startKeyInputMode(rightKeyCodeButton, KEY_RIGHT_CODE) }
        tapKeyCodeButton.setOnClickListener { startKeyInputMode(tapKeyCodeButton, KEY_TAP_CODE) }

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
            showLicenseDialog()
        }
    }

    @SuppressLint("SetTextI18n")
    private fun loadPreferences() {
        upKeyCodeButton.text = sharedPreferences.getInt(KEY_UP_CODE, 19).toString()
        downKeyCodeButton.text = sharedPreferences.getInt(KEY_DOWN_CODE, 20).toString()
        leftKeyCodeButton.text = sharedPreferences.getInt(KEY_LEFT_CODE, 21).toString()
        rightKeyCodeButton.text = sharedPreferences.getInt(KEY_RIGHT_CODE, 22).toString()
        tapKeyCodeButton.text = sharedPreferences.getInt(KEY_TAP_CODE, 66).toString()
        moveSpeedEditText.setText(sharedPreferences.getInt(KEY_MOVE_SPEED, 1).toString())
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
            }
            apply()
        }
    }

    private fun showLicenseDialog() {
        val licenseText = """
MIT License

Copyright (c) 2024 nnnnnnn0090

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.
        """

        val builder = AlertDialog.Builder(this)
        builder.setTitle("License Information")
            .setMessage(licenseText)
            .setPositiveButton("OK", null)

        val dialog = builder.create()
        dialog.show()
    }
//    private fun isAccessibilityServiceEnabled(): Boolean {

//    }
}
