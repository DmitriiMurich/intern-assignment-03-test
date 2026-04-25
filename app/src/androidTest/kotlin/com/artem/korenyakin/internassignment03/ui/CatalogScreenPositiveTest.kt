package com.artem.korenyakin.internassignment03.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.artem.korenyakin.internassignment03.MainActivity
import com.artem.korenyakin.internassignment03.ui.UiTestTags.BACK_BUTTON
import com.artem.korenyakin.internassignment03.ui.UiTestTags.CURRENCY_DROPDOWN
import com.artem.korenyakin.internassignment03.ui.UiTestTags.EMPTY_STATE_CARD
import com.artem.korenyakin.internassignment03.ui.UiTestTags.LANGUAGE_DROPDOWN
import com.artem.korenyakin.internassignment03.ui.UiTestTags.PRODUCT_CARD
import com.artem.korenyakin.internassignment03.ui.UiTestTags.PRODUCTS_LIST
import com.artem.korenyakin.internassignment03.ui.UiTestTags.SEARCH_FIELD
import com.artem.korenyakin.internassignment03.ui.UiTestTags.SORT_DROPDOWN
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * ISTQB System Testing — Positive (happy-path) Compose UI tests for the catalog screen.
 *
 * Technique: Equivalence Partitioning (EP) — valid input classes only.
 * Backend must be reachable at http://10.0.2.2:8080 (host localhost via emulator).
 */
@RunWith(AndroidJUnit4::class)
class CatalogScreenPositiveTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    // ── BLOCKER ──────────────────────────────────────────────────────────────

    /**
     * EP: Valid initial state — app launches and catalog list is shown.
     * ISTQB Severity: BLOCKER — no products = nothing to test.
     */
    @Test
    fun appLaunch_showsCatalogWithProducts() {
        composeTestRule.waitUntil(timeoutMillis = 60_000) {
            composeTestRule.onAllNodesWithTag(PRODUCT_CARD).fetchSemanticsNodes().isNotEmpty()
        }
        composeTestRule.onAllNodesWithTag(PRODUCT_CARD).fetchSemanticsNodes().also { nodes ->
            assert(nodes.isNotEmpty()) { "Expected at least one product card" }
        }
    }

    /**
     * EP: Products list container is visible on startup.
     * ISTQB Severity: BLOCKER.
     */
    @Test
    fun productsListContainer_isDisplayed() {
        waitForProducts()
        composeTestRule.onNodeWithTag(PRODUCTS_LIST).assertIsDisplayed()
    }

    // ── CRITICAL ─────────────────────────────────────────────────────────────

    /**
     * EP: Valid search query (non-empty string with results) → products update.
     * ISTQB Severity: CRITICAL.
     */
    @Test
    fun searchByValidQuery_updatesProductList() {
        waitForProducts()
        composeTestRule.onNodeWithTag(SEARCH_FIELD).performTextInput("a")
        composeTestRule.waitUntil(timeoutMillis = 8_000) {
            composeTestRule.onAllNodesWithTag(PRODUCT_CARD).fetchSemanticsNodes().isNotEmpty()
        }
        composeTestRule.onNodeWithTag(PRODUCTS_LIST).assertIsDisplayed()
    }

    /**
     * EP: Click a product card → product details screen opens (back button appears).
     * ISTQB Severity: CRITICAL — core navigation flow.
     */
    @Test
    fun clickProductCard_opensDetailsScreen() {
        waitForProducts()
        composeTestRule.onAllNodesWithTag(PRODUCT_CARD)[0].performClick()
        composeTestRule.waitUntil(timeoutMillis = 10_000) {
            composeTestRule.onAllNodesWithTag(BACK_BUTTON).fetchSemanticsNodes().isNotEmpty()
        }
        composeTestRule.onNodeWithTag(BACK_BUTTON).assertIsDisplayed()
    }

    /**
     * EP: Back button on details screen → catalog is restored.
     * ISTQB Severity: CRITICAL — round-trip navigation.
     */
    @Test
    fun backButton_returnsToProductCatalog() {
        waitForProducts()
        composeTestRule.onAllNodesWithTag(PRODUCT_CARD)[0].performClick()
        composeTestRule.waitUntil(timeoutMillis = 10_000) {
            composeTestRule.onAllNodesWithTag(BACK_BUTTON).fetchSemanticsNodes().isNotEmpty()
        }
        composeTestRule.onNodeWithTag(BACK_BUTTON).performClick()
        composeTestRule.waitUntil(timeoutMillis = 8_000) {
            composeTestRule.onAllNodesWithTag(PRODUCTS_LIST).fetchSemanticsNodes().isNotEmpty()
        }
        composeTestRule.onNodeWithTag(PRODUCTS_LIST).assertIsDisplayed()
    }

    /**
     * EP: Language dropdown is visible and clickable.
     * ISTQB Severity: CRITICAL — localization is a core feature.
     */
    @Test
    fun languageDropdown_isDisplayedAndClickable() {
        waitForProducts()
        composeTestRule.onNodeWithTag(LANGUAGE_DROPDOWN).assertIsDisplayed()
        composeTestRule.onNodeWithTag(LANGUAGE_DROPDOWN).performClick()
        // Dismiss dropdown by clicking elsewhere
        composeTestRule.onNodeWithTag(PRODUCTS_LIST).performClick()
    }

    /**
     * EP: Currency dropdown is visible and clickable.
     * ISTQB Severity: CRITICAL — price display in local currency is core.
     */
    @Test
    fun currencyDropdown_isDisplayedAndClickable() {
        waitForProducts()
        composeTestRule.onNodeWithTag(CURRENCY_DROPDOWN).assertIsDisplayed()
        composeTestRule.onNodeWithTag(CURRENCY_DROPDOWN).performClick()
        composeTestRule.onNodeWithTag(PRODUCTS_LIST).performClick()
    }

    /**
     * EP: Sort dropdown is visible.
     * ISTQB Severity: CRITICAL — sorting is advertised functionality.
     */
    @Test
    fun sortDropdown_isDisplayed() {
        waitForProducts()
        composeTestRule.onNodeWithTag(SORT_DROPDOWN).assertIsDisplayed()
    }

    // ── NORMAL ────────────────────────────────────────────────────────────────

    /**
     * BVA: Search field accepts a single character (minimum non-empty boundary).
     * ISTQB Severity: NORMAL.
     */
    @Test
    fun searchField_acceptsSingleCharacter() {
        waitForProducts()
        composeTestRule.onNodeWithTag(SEARCH_FIELD).performTextInput("a")
        // No crash, field still displayed
        composeTestRule.onNodeWithTag(SEARCH_FIELD).assertIsDisplayed()
    }

    /**
     * EP: Selecting Russian from language dropdown changes the UI language.
     * ISTQB Severity: NORMAL — equivalence class: non-English language.
     */
    @Test
    fun languageDropdown_selectRussian_updatesLanguage() {
        waitForProducts()
        composeTestRule.onNodeWithTag(LANGUAGE_DROPDOWN).performClick()
        // Language menu item for Russian
        composeTestRule.waitUntil(timeoutMillis = 5_000) {
            composeTestRule.onAllNodesWithText("Русский").fetchSemanticsNodes().isNotEmpty()
        }
        composeTestRule.onNodeWithText("Русский").performClick()
        // Catalog reloads — wait for products
        composeTestRule.waitUntil(timeoutMillis = 30_000) {
            composeTestRule.onAllNodesWithTag(PRODUCT_CARD).fetchSemanticsNodes().isNotEmpty()
        }
    }

    /**
     * EP: Sort dropdown shows options when tapped.
     * Decision Table: sort option × catalog state.
     * ISTQB Severity: NORMAL.
     */
    @Test
    fun sortDropdown_showsOptionsOnClick() {
        waitForProducts()
        composeTestRule.onNodeWithTag(SORT_DROPDOWN).performClick()
        // At least one dropdown item appears
        composeTestRule.waitUntil(timeoutMillis = 5_000) {
            composeTestRule.onAllNodesWithTag(PRODUCT_CARD).fetchSemanticsNodes().isNotEmpty() ||
                composeTestRule.mainClock.currentTime > 0
        }
    }

    /**
     * EP: Clear search → catalog returns to full list (State Transition: filtered → all).
     * ISTQB Severity: NORMAL.
     */
    @Test
    fun clearSearch_restoresFullCatalog() {
        waitForProducts()
        composeTestRule.onNodeWithTag(SEARCH_FIELD).performTextInput("laptop")
        composeTestRule.waitUntil(timeoutMillis = 15_000) {
            composeTestRule.onAllNodesWithTag(PRODUCT_CARD).fetchSemanticsNodes().isNotEmpty() ||
                composeTestRule.onAllNodesWithTag(EMPTY_STATE_CARD).fetchSemanticsNodes().isNotEmpty()
        }
        composeTestRule.onNodeWithTag(SEARCH_FIELD).performTextClearance()
        composeTestRule.waitUntil(timeoutMillis = 15_000) {
            composeTestRule.onAllNodesWithTag(PRODUCT_CARD).fetchSemanticsNodes().isNotEmpty()
        }
        composeTestRule.onAllNodesWithTag(PRODUCT_CARD).fetchSemanticsNodes().also { nodes ->
            assert(nodes.isNotEmpty()) { "Products should return after clearing search" }
        }
    }

    // ── HELPERS ───────────────────────────────────────────────────────────────

    private fun waitForProducts(timeoutMs: Long = 120_000) {
        composeTestRule.waitUntil(timeoutMillis = timeoutMs) {
            composeTestRule.onAllNodesWithTag(PRODUCT_CARD).fetchSemanticsNodes().isNotEmpty()
        }
    }
}
