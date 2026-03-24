package com.artem.korenyakin.internassignment03.feature.catalog.data.mapper

import com.artem.korenyakin.internassignment03.feature.catalog.data.remote.dto.DummyJsonProductDto
import com.artem.korenyakin.internassignment03.model.domain.Product
import com.artem.korenyakin.internassignment03.model.domain.ProductCategory

internal fun DummyJsonProductDto.toDomain(): Product = Product(
    id = id.toString(),
    title = title,
    description = description,
    price = price,
    imageUrl = thumbnailUrl ?: images.firstOrNull().orEmpty(),
    rating = rating,
    category = category.toProductCategory(),
)

internal fun String.toProductCategory(): ProductCategory = ProductCategory(
    slug = this,
    title = split("-", "_")
        .joinToString(separator = " ") { chunk ->
            chunk.replaceFirstChar { character -> character.uppercase() }
        },
)