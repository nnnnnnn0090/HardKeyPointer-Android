package com.nnnnnnn0090.hardkeypointer

import android.content.Context
import android.view.KeyEvent
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.hamcrest.CoreMatchers.allOf
import org.hamcrest.CoreMatchers.instanceOf
import org.hamcrest.CoreMatchers.is
import org.hamcrest.CoreMatchers.notNullValue
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MainActivityTest {

    @Test
    fun testActivityLaunches() {
        val scenario = ActivityScenario.launch(MainActivity::class.java)
        scenario.onActivity { activity ->
            assertNotNull(activity)
            assertEquals("com.nnnnnnn0090.hardkeypointer", activity.packageName)
        }
    }

    @Test
    fun testLoadPreferences_setsDefaultValues() {
        val scenario = ActivityScenario.launch(MainActivity::class.java)
        scenario.onActivity { activity ->
            // Test that default values are loaded
            val moveSpeedEditText = activity.findViewById<android.widget.EditText>(R.id.moveSpeedEditText)
            val moveAccelEditText = activity.findViewById<android.widget.EditText>(R.id.moveAccelEditText)
            val scrollDistanceEditText = activity.findViewById<android.widget.EditText>(R.id.scrollDistanceEditText)
            
            assertNotNull(moveSpeedEditText)
            assertNotNull(moveAccelEditText)
            assertNotNull(scrollDistanceEditText)
            
            assertEquals("30", moveSpeedEditText.text.toString())
            assertEquals("100", moveAccelEditText.text.toString())
            assertEquals("200", scrollDistanceEditText.text.toString())
        }
    }

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
                val button = activity.findViewById<android.widget.Button>(id)
                assertNotNull("Button with id $id should exist", button)
            }
        }
    }

    @Test
    fun testLicenseButtonExists() {
        val scenario = ActivityScenario.launch(MainActivity::class.java)
        scenario.onActivity { activity ->
            val licenseButton = activity.findViewById<android.widget.Button>(R.id.license_button)
            assertNotNull(licenseButton)
            assertEquals("License", licenseButton.text.toString())
        }
    }
}