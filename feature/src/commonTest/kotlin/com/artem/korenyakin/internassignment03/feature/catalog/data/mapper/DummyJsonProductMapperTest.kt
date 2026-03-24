package com.artem.korenyakin.internassignment03.feature.catalog.data.mapper

import com.artem.korenyakin.internassignment03.feature.catalog.data.remote.dto.DummyJsonProductDto
import kotlin.test.Test
import kotlin.test.assertEquals

internal class DummyJsonProductMapperTest {
    @Test
    fun shouldMapProductDtoToDomainUsingThumbnail() {
        val product = DummyJsonProductDto(
            id = 101,
            title = "Phone Case",
            description = "Protective case",
            category = "mobile-accessories",
            price = 12.5,
            rating = 4.7,
            images = listOf("https://example.com/image-1.png"),
            thumbnailUrl = "https://example.com/thumbnail.png",
        ).toDomain()

        assertEquals("101", product.id)
        assertEquals("Phone Case", product.title)
        assertEquals(12.5, product.price)
        assertEquals(4.7, product.rating)
        assertEquals("https://example.com/thumbnail.png", product.imageUrl)
        assertEquals("mobile-accessories", product.category.slug)
        assertEquals("Mobile Accessories", product.category.title)
    }

    @Test
    fun shouldMapProductDtoToDomainUsingFirstImageWhenThumbnailMissing() {
        val product = DummyJsonProductDto(
            id = 202,
            title = "Body Lotion",
            description = "Hydrating lotion",
            category = "skin-care",
            price = 18.0,
            rating = 4.4,
            images = listOf(
                "https://example.com/image-a.png",
                "https://example.com/image-b.png",
            ),
            thumbnailUrl = null,
        ).toDomain()

        assertEquals("https://example.com/image-a.png", product.imageUrl)
        assertEquals("Skin Care", product.category.title)
    }

    @Test
    fun shouldMapCategorySlugToReadableTitle() {
        val category = "home-decoration".toProductCategory()

        assertEquals("home-decoration", category.slug)
        assertEquals("Home Decoration", category.title)
    }
}
