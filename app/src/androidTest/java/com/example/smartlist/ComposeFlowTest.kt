package com.example.smartlist

import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.BeforeClass
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import androidx.test.espresso.Espresso
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.SemanticsNodeInteractionCollection

@RunWith(AndroidJUnit4::class)
class ComposeFlowTest {
    @get:Rule
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    companion object {
        @JvmStatic
        @BeforeClass
        fun setupInstrumentationInit() {
            // Initialize a deterministic in-memory DB for instrumentation tests
            val appContext = InstrumentationRegistry.getInstrumentation().targetContext.applicationContext
            ServiceLocator.initForInstrumentation(appContext)
        }
    }

    @Test
    fun createList_addItem_backAndReopen_itemVisible() {
    // Click the New List button
        // wait until the new list button exists (avoids timing races on slow devices)
        composeTestRule.waitUntil(20_000) {
            composeTestRule.onAllNodesWithTag("new_list_button").fetchSemanticsNodes().isNotEmpty()
        }
        composeTestRule.onNodeWithTag("new_list_button").performClick()

        // Wait for UI to settle and ensure the new list row is present, then click it
        // Wait for the new list row to appear
        composeTestRule.waitUntil(20_000) {
            composeTestRule.onAllNodesWithTag("list_row_New list").fetchSemanticsNodes().isNotEmpty()
        }
        composeTestRule.onNodeWithTag("list_row_New list").performClick()

        // Enter item text and add
        composeTestRule.onNodeWithTag("item_input").performTextInput("Instrumented item")
    composeTestRule.onNodeWithTag("add_item_button").performClick()

    // Ensure Compose has settled and any logs/actions triggered by the click have completed
    composeTestRule.waitForIdle()

    // Press back
    Espresso.pressBack()

        // Reopen the list using its test tag
        composeTestRule.waitUntil(20_000) {
            composeTestRule.onAllNodesWithTag("list_row_New list").fetchSemanticsNodes().isNotEmpty()
        }
        composeTestRule.onNodeWithTag("list_row_New list").performClick()

        // Assert the item exists
        composeTestRule.onNodeWithText("Instrumented item").assertIsDisplayed()
    }
}
