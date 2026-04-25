package com.artem.korenyakin.internassignment03.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.artem.korenyakin.internassignment03.MainActivity
import com.artem.korenyakin.internassignment03.ui.UiTestTags.BACK_BUTTON
import com.artem.korenyakin.internassignment03.ui.UiTestTags.PRODUCT_CARD
import com.artem.korenyakin.internassignment03.ui.UiTestTags.PRODUCTS_LIST
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * ISTQB System Testing — Compose UI tests for the Product Details screen.
 *
 * Techniques:
 *  - EP: valid product ID → details loaded
 *  - State Transition: catalog → loading → content → catalog
 *  - BVA: first product in list (min index boundary)
 *
 * Backend must be reachable at http://10.0.2.2:8080.
 */
@RunWith(AndroidJUnit4::class)
class ProductDetailsUiTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    // ── BLOCKER ──────────────────────────────────────────────────────────────

    /**
     * EP: Click valid product → details screen appears (back button visible).
     * ISTQB Severity: BLOCKER — detail view is the primary product discovery flow.
     */
    @Test
    fun clickProduct_detailsScreenOpens() {
        waitForProducts()
        composeTestRule.onAllNodesWithTag(PRODUCT_CARD)[0].performClick()
        composeTestRule.waitUntil(timeoutMillis = 12_000) {
            composeTestRule.onAllNodesWithTag(BACK_BUTTON).fetchSemanticsNodes().isNotEmpty()
        }
        composeTestRule.onNodeWithTag(BACK_BUTTON).assertIsDisplayed()
    }

    // ── CRITICAL ─────────────────────────────────────────────────────────────

    /**
     * State Transition: details screen → back → catalog is restored.
     * ISTQB Severity: CRITICAL — navigation must be reversible.
     */
    @Test
    fun detailsScreen_backButton_restoresCatalog() {
        waitForProducts()
        composeTestRule.onAllNodesWithTag(PRODUCT_CARD)[0].performClick()
        composeTestRule.waitUntil(timeoutMillis = 12_000) {
            composeTestRule.onAllNodesWithTag(BACK_BUTTON).fetchSemanticsNodes().isNotEmpty()
        }
        composeTestRule.onNodeWithTag(BACK_BUTTON).performClick()
        composeTestRule.waitUntil(timeoutMillis = 8_000) {
            composeTestRule.onAllNodesWithTag(PRODUCTS_LIST).fetchSemanticsNodes().isNotEmpty()
        }
        composeTestRule.onNodeWithTag(PRODUCTS_LIST).assertIsDisplayed()
    }

    /**
     * EP: Second product also opens details correctly.
     * BVA: index 1 (second element, next boundary after first).
     * ISTQB Severity: CRITICAL — ensures not just the first item works.
     */
    @Test
    fun clickSecondProduct_detailsScreenOpens() {
        waitForProducts()
        val cards = composeTestRule.onAllNodesWithTag(PRODUCT_CARD).fetchSemanticsNodes()
        if (cards.size < 2) return // skip if only one product
        composeTestRule.onAllNodesWithTag(PRODUCT_CARD)[1].performClick()
        composeTestRule.waitUntil(timeoutMillis = 12_000) {
            composeTestRule.onAllNodesWithTag(BACK_BUTTON).fetchSemanticsNodes().isNotEmpty()
        }
        composeTestRule.onNodeWithTag(BACK_BUTTON).assertIsDisplayed()
    }

    /**
     * State Transition: open details → back → open different product → details again.
     * Decision Table: product A → back → product B → details.
     * ISTQB Severity: CRITICAL — verifies no stale state after navigation round-trip.
     */
    @Test
    fun openTwoDifferentProducts_bothShowDetails() {
        waitForProducts()
        val cards = composeTestRule.onAllNodesWithTag(PRODUCT_CARD).fetchSemanticsNodes()
        if (cards.size < 2) return

        // Open first product
        composeTestRule.onAllNodesWithTag(PRODUCT_CARD)[0].performClick()
        composeTestRule.waitUntil(timeoutMillis = 12_000) {
            composeTestRule.onAllNodesWithTag(BACK_BUTTON).fetchSemanticsNodes().isNotEmpty()
        }
        // Go back
        composeTestRule.onNodeWithTag(BACK_BUTTON).performClick()
        composeTestRule.waitUntil(timeoutMillis = 8_000) {
            composeTestRule.onAllNodesWithTag(PRODUCTS_LIST).fetchSemanticsNodes().isNotEmpty()
        }
        // Open second product
        composeTestRule.onAllNodesWithTag(PRODUCT_CARD)[1].performClick()
        composeTestRule.waitUntil(timeoutMillis = 12_000) {
            composeTestRule.onAllNodesWithTag(BACK_BUTTON).fetchSemanticsNodes().isNotEmpty()
        }
        composeTestRule.onNodeWithTag(BACK_BUTTON).assertIsDisplayed()
    }

    // ── NORMAL ────────────────────────────────────────────────────────────────

    /**
     * EP: After opening details and going back, catalog count is same (no data loss).
     * ISTQB Severity: NORMAL.
     */
    @Test
    fun afterBackNavigation_productCountUnchanged() {
        waitForProducts()
        val initialCount = composeTestRule.onAllNodesWithTag(PRODUCT_CARD).fetchSemanticsNodes().size

        composeTestRule.onAllNodesWithTag(PRODUCT_CARD)[0].performClick()
        composeTestRule.waitUntil(timeoutMillis = 12_000) {
            composeTestRule.onAllNodesWithTag(BACK_BUTTON).fetchSemanticsNodes().isNotEmpty()
        }
        composeTestRule.onNodeWithTag(BACK_BUTTON).performClick()
        composeTestRule.waitUntil(timeoutMillis = 8_000) {
            composeTestRule.onAllNodesWithTag(PRODUCTS_LIST).fetchSemanticsNodes().isNotEmpty()
        }

        val finalCount = composeTestRule.onAllNodesWithTag(PRODUCT_CARD).fetchSemanticsNodes().size
        assert(finalCount == initialCount) {
            "Product count changed after navigation: was $initialCount, now $finalCount"
        }
    }

    // ── HELPERS ───────────────────────────────────────────────────────────────

    private fun waitForProducts(timeoutMs: Long = 30_000) {
        composeTestRule.waitUntil(timeoutMillis = timeoutMs) {
            composeTestRule.onAllNodesWithTag(PRODUCT_CARD).fetchSemanticsNodes().isNotEmpty()
        }
    }
}
