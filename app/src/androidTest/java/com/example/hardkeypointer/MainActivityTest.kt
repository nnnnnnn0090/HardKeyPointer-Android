/*
 * HardKeyPointer-Android
 * Copyright (c) 2024-2026 nnnnnnn0090
 * 作者: nnnnnnn0090
 * ライセンス: リポジトリの LICENSE を参照してください。
 */
package com.nnnnnnn0090.hardkeypointer

import android.widget.Button
import android.widget.TextView
import android.view.KeyEvent
import androidx.test.core.app.ActivityScenario
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.android.material.slider.Slider
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MainActivityTest {

    /** 実機テスト間で設定とキー入力待ち状態を初期化します。 */
    @Before
    fun resetPreferences() {
        KeyCaptureState.finish()
        InstrumentationRegistry.getInstrumentation().targetContext
            .getSharedPreferences("com.nnnnnnn0090.hardkeypointer.PREFS", 0)
            .edit()
            .clear()
            .commit()
    }

    /** 設定画面が起動できることを確認します。 */
    @Test
    fun testActivityLaunches() {
        val scenario = ActivityScenario.launch(MainActivity::class.java)
        scenario.onActivity { activity ->
            assertNotNull(activity)
            assertEquals("com.nnnnnnn0090.hardkeypointer", activity.packageName)
        }
    }

    /** 初期スライダー値が既定値で表示されることを確認します。 */
    @Test
    fun testLoadPreferences_setsDefaultValues() {
        val scenario = ActivityScenario.launch(MainActivity::class.java)
        scenario.onActivity { activity ->
            val moveSpeedSlider = activity.findViewById<Slider>(R.id.moveSpeedSlider)
            val moveAccelSlider = activity.findViewById<Slider>(R.id.moveAccelSlider)
            val scrollDistanceSlider = activity.findViewById<Slider>(R.id.scrollDistanceSlider)

            assertNotNull(moveSpeedSlider)
            assertNotNull(moveAccelSlider)
            assertNotNull(scrollDistanceSlider)

            assertEquals(30f, moveSpeedSlider.value)
            assertEquals(100f, moveAccelSlider.value)
            assertEquals(200f, scrollDistanceSlider.value)
        }
    }

    /** サービス状態を表示するビューが存在することを確認します。 */
    @Test
    fun testServiceStatusViewsExist() {
        val scenario = ActivityScenario.launch(MainActivity::class.java)
        scenario.onActivity { activity ->
            val statusLabel = activity.findViewById<TextView>(R.id.serviceStatusLabel)
            val detail = activity.findViewById<TextView>(R.id.serviceStatusDetail)
            val button = activity.findViewById<Button>(R.id.openAccessibilitySettingsButton)

            assertNotNull(statusLabel)
            assertNotNull(detail)
            assertNotNull(button)
        }
    }

    /** 全キー設定ボタンがレイアウトに存在することを確認します。 */
    @Test
    fun testKeyCodeButtonsExist() {
        val scenario = ActivityScenario.launch(MainActivity::class.java)
        scenario.onActivity { activity ->
            val buttonIds = listOf(
                R.id.upKeyCodeButton,
                R.id.downKeyCodeButton,
                R.id.leftKeyCodeButton,
                R.id.rightKeyCodeButton,
                R.id.tapKeyCodeButton,
                R.id.disableKeyCodeButton,
                R.id.scrollupKeyCodeButton,
                R.id.scrolldownKeyCodeButton,
                R.id.scrollleftKeyCodeButton,
                R.id.scrollrightKeyCodeButton
            )

            buttonIds.forEach { id ->
                val button = activity.findViewById<Button>(id)
                assertNotNull("Button with id $id should exist", button)
            }
        }
    }

    /** ライセンス表示ボタンが存在することを確認します。 */
    @Test
    fun testLicenseButtonExists() {
        val scenario = ActivityScenario.launch(MainActivity::class.java)
        scenario.onActivity { activity ->
            val licenseButton = activity.findViewById<Button>(R.id.license_button)
            assertNotNull(licenseButton)
        }
    }

    /** ボタンクリック後に次の物理キーを直接割り当てられることを確認します。 */
    @Test
    fun testDirectButtonClickCapturesNextKey() {
        val scenario = ActivityScenario.launch(MainActivity::class.java)
        scenario.onActivity { activity ->
            val button = activity.findViewById<Button>(R.id.upKeyCodeButton)
            button.performClick()

            assertTrue(KeyCaptureState.isActive)
            assertTrue(activity.dispatchKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_F1)))
            assertEquals(
                KeyEvent.KEYCODE_F1,
                SettingsRepository(activity).getKeyCode(PointerAction.UP)
            )
            assertTrue(activity.dispatchKeyEvent(KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_F1)))
            assertFalse(KeyCaptureState.isActive)
        }
    }
}
