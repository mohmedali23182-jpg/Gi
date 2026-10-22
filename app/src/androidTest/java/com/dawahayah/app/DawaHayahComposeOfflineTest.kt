package com.dawahayah.app

import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class DawaHayahComposeOfflineTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun firstLaunchOffline_displaysArabicIdentityAndBottomNavigation() {
        composeTestRule.onNodeWithText("دواء وحياة", substring = true).assertExists()
        composeTestRule.onNodeWithText("الرئيسية").assertExists()
        composeTestRule.onNodeWithText("جسم الإنسان").assertExists()
        composeTestRule.onNodeWithText("الأمراض").assertExists()
        composeTestRule.onNodeWithText("الأدوية").assertExists()
        composeTestRule.onNodeWithText("مكتبتي").assertExists()
    }
}
