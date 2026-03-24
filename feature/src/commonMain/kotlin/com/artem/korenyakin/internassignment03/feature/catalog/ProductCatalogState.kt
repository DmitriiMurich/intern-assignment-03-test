package com.artem.korenyakin.internassignment03.feature.catalog

import com.artem.korenyakin.internassignment03.model.domain.Product
import com.artem.korenyakin.internassignment03.model.domain.ProductCategory
import com.artem.korenyakin.internassignment03.model.domain.SortOption

internal data class ProductCatalogState(
    val searchQuery: String = "",
    val selectedCategory: ProductCategory = ProductCategory.ALL,
    val selectedSortOption: SortOption = SortOption.PRICE_ASC,
    val categories: List<ProductCategory> = listOf(ProductCategory.ALL),
    val products: List<Product> = emptyList(),
    val filteredProducts: List<Product> = emptyList(),
    val visibleProducts: List<Product> = emptyList(),
    val isLoading: Boolean = true,
    val isLoadingMore: Boolean = false,
    val canLoadMore: Boolean = false,
    val errorMessage: String? = null,
)
