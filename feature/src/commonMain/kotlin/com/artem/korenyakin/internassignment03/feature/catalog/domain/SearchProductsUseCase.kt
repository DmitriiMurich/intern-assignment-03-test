package com.artem.korenyakin.internassignment03.feature.catalog.domain

import com.artem.korenyakin.internassignment03.model.domain.Product
import com.artem.korenyakin.internassignment03.model.domain.ProductCategory
import com.artem.korenyakin.internassignment03.model.domain.SortOption

internal class SearchProductsUseCase {
    internal operator fun invoke(
        products: List<Product>,
        query: String,
        selectedCategory: ProductCategory,
        selectedSortOption: SortOption,
    ): List<Product> {
        val normalizedQuery = query.trim().lowercase()

        return products
            .asSequence()
            .filter { product ->
                normalizedQuery.isBlank() || product.title.lowercase().contains(normalizedQuery)
            }
            .filter { product ->
                selectedCategory == ProductCategory.ALL || product.category.slug == selectedCategory.slug
            }
            .sortedWith(sortComparator(selectedSortOption))
            .toList()
    }

    private fun sortComparator(sortOption: SortOption): Comparator<Product> = when (sortOption) {
        SortOption.PRICE_ASC -> compareBy(Product::price, Product::title)
        SortOption.PRICE_DESC -> compareByDescending<Product> { product -> product.price }
            .thenBy(Product::title)
        SortOption.RATING_DESC -> compareByDescending<Product> { product -> product.rating }
            .thenBy(Product::title)
    }
}
