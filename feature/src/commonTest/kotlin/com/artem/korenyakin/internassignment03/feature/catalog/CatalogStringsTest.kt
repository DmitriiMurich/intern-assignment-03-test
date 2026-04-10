package com.artem.korenyakin.internassignment03.feature.catalog

import com.artem.korenyakin.internassignment03.model.domain.CurrencyOption
import com.artem.korenyakin.internassignment03.model.domain.Money
import kotlin.test.Test
import kotlin.test.assertEquals

internal class CatalogStringsTest {
    @Test
    fun shouldFormatPriceWithoutTrailingZeroForDefaultCurrencies() {
        val formattedPrice = formatPrice(
            Money(
                amount = 19.0,
                currency = CurrencyOption.USD,
            ),
        )

        assertEquals("\$19", formattedPrice)
    }

    @Test
    fun shouldFormatPriceWithSpaceForSwissFrancAndRoundToTwoDigits() {
        val formattedPrice = formatPrice(
            Money(
                amount = 19.345,
                currency = CurrencyOption(
                    code = "CHF",
                    name = "Swiss Franc",
                    symbol = "CHF",
                    isSourceCurrency = false,
                ),
            ),
        )

        assertEquals("CHF 19.35", formattedPrice)
    }

    @Test
    fun shouldFormatRatingWithSingleFractionDigit() {
        assertEquals("4.9", formatRatingNumber(4.94))
        assertEquals("5.0", formatRatingNumber(4.96))
    }

    @Test
    fun shouldExtractDatePartFromIsoReviewTimestamp() {
        assertEquals("2026-04-10", formatReviewDate("2026-04-10T10:15:00.000Z"))
    }

    @Test
    fun shouldKeepOriginalReviewDateWhenTimestampSeparatorIsMissing() {
        assertEquals("2026/04/10", formatReviewDate("2026/04/10"))
    }
}
