package com.artem.korenyakin.internassignment03.feature.catalog.domain

import com.artem.korenyakin.internassignment03.model.domain.Product

internal class SearchProductsUseCase {
    internal operator fun invoke(
        products: List<Product>,
        query: String,
    ): List<Product> {
        val normalizedQuery = query.trim().lowercase()
        if (normalizedQuery.isBlank()) {
            return products
        }

        return products.filter { product ->
            product.title.lowercase().contains(normalizedQuery)
        }
    }
}
