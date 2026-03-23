package com.artem.korenyakin.internassignment03.feature.catalog

import com.artem.korenyakin.internassignment03.model.domain.Product
import com.artem.korenyakin.internassignment03.model.domain.ProductCategory
import com.artem.korenyakin.internassignment03.model.domain.SortOption

internal data class ProductCatalogState(
    val searchQuery: String = "",
    val selectedCategory: ProductCategory = ProductCategory.ALL,
    val selectedSortOption: SortOption = SortOption.PRICE_ASC,
    val products: List<Product> = emptyList(),
    val visibleProducts: List<Product> = emptyList(),
    val isLoading: Boolean = false,
    val canLoadMore: Boolean = true,
)
