package com.artem.korenyakin.internassignment03.feature.catalog.data.mapper

import com.artem.korenyakin.internassignment03.feature.catalog.data.remote.dto.CatalogCategoryDto
import com.artem.korenyakin.internassignment03.feature.catalog.data.remote.dto.CatalogMoneyDto
import com.artem.korenyakin.internassignment03.feature.catalog.data.remote.dto.CatalogProductDetailsDto
import com.artem.korenyakin.internassignment03.feature.catalog.data.remote.dto.CatalogProductDetailsMetaDto
import com.artem.korenyakin.internassignment03.feature.catalog.data.remote.dto.CatalogProductDto
import com.artem.korenyakin.internassignment03.feature.catalog.data.remote.dto.CatalogReviewDto
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse

internal class CatalogBackendMapperTest {
    @Test
    fun shouldFallbackToSyntheticCurrencyWhenBackendReturnsUnsupportedCode() {
        val money = CatalogMoneyDto(
            amount = 74.5,
            currency = "AED",
        ).toDomain()

        assertEquals(74.5, money.amount)
        assertEquals("AED", money.currency.code)
        assertEquals("AED", money.currency.name)
        assertEquals("AED", money.currency.symbol)
        assertFalse(money.currency.isSourceCurrency)
    }

    @Test
    fun shouldMapProductDetailsAndLocalizedReviewsToDomain() {
        val details = CatalogProductDetailsDto(
            language = "ru",
            currency = "EUR",
            product = CatalogProductDto(
                id = "10",
                title = "Night Serum",
                description = "Localized description",
                price = CatalogMoneyDto(
                    amount = 88.5,
                    currency = "EUR",
                ),
                rating = 4.9,
                imageUrl = "https://example.com/product.png",
                category = CatalogCategoryDto(
                    slug = "beauty",
                    title = "Beauty",
                ),
            ),
            reviews = listOf(
                CatalogReviewDto(
                    id = "review-1",
                    rating = 5.0,
                    comment = "Localized review comment",
                    date = "2026-04-10T10:15:00.000Z",
                    reviewerName = "Emma Wilson",
                ),
            ),
            meta = CatalogProductDetailsMetaDto(
                sourceLanguage = "en",
                sourceCurrency = "USD",
                exchangeRateProvider = "frankfurter",
            ),
        ).toDomain()

        assertEquals("10", details.product.id)
        assertEquals("Night Serum", details.product.title)
        assertEquals("EUR", details.product.price.currency.code)
        assertEquals(1, details.reviews.size)
        assertEquals("Localized review comment", details.reviews.first().comment)
        assertEquals("Emma Wilson", details.reviews.first().reviewerName)
    }
}
