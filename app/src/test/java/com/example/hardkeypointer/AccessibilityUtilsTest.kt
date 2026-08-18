package com.nnnnnnn0090.hardkeypointer

import android.accessibilityservice.AccessibilityServiceInfo
import android.content.Context
import android.content.pm.ResolveInfo
import android.content.pm.ServiceInfo
import android.view.accessibility.AccessibilityManager
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.*

class AccessibilityUtilsTest {

    private lateinit var mockContext: Context
    private lateinit var mockAccessibilityManager: AccessibilityManager

    @Before
    fun setup() {
        mockContext = mock(Context::class.java)
        mockAccessibilityManager = mock(AccessibilityManager::class.java)
        
        `when`(mockContext.getSystemService(Context.ACCESSIBILITY_SERVICE)).thenReturn(mockAccessibilityManager)
        `when`(mockContext.packageName).thenReturn("com.nnnnnnn0090.hardkeypointer")
    }

    private fun createAccessibilityServiceInfo(packageName: String, name: String): AccessibilityServiceInfo {
        val serviceInfo = mock(AccessibilityServiceInfo::class.java)
        val resolveInfo = ResolveInfo()
        resolveInfo.serviceInfo = ServiceInfo().apply {
            this.packageName = packageName
            this.name = name
        }
        `when`(serviceInfo.resolveInfo).thenReturn(resolveInfo)
        return serviceInfo
    }

    @Test
    fun testIsAccessibilityServiceEnabled_returnsTrueWhenServiceEnabled() {
        val accessibilityServiceInfo = createAccessibilityServiceInfo(
            "com.nnnnnnn0090.hardkeypointer",
            "com.nnnnnnn0090.hardkeypointer.TapService"
        )
        
        `when`(mockAccessibilityManager.getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_ALL_MASK))
            .thenReturn(listOf(accessibilityServiceInfo))
        
        val result = AccessibilityUtils.isAccessibilityServiceEnabled(mockContext)
        
        assertTrue(result)
    }

    @Test
    fun testIsAccessibilityServiceEnabled_returnsFalseWhenServiceNotEnabled() {
        val accessibilityServiceInfo = createAccessibilityServiceInfo(
            "com.other.package",
            "com.other.package.OtherService"
        )
        
        `when`(mockAccessibilityManager.getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_ALL_MASK))
            .thenReturn(listOf(accessibilityServiceInfo))
        
        val result = AccessibilityUtils.isAccessibilityServiceEnabled(mockContext)
        
        assertFalse(result)
    }

    @Test
    fun testIsAccessibilityServiceEnabled_returnsFalseWhenListEmpty() {
        `when`(mockAccessibilityManager.getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_ALL_MASK))
            .thenReturn(emptyList())
        
        val result = AccessibilityUtils.isAccessibilityServiceEnabled(mockContext)
        
        assertFalse(result)
    }
}