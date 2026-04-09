package com.artem.korenyakin.internassignment03.feature.catalog

import com.artem.korenyakin.internassignment03.model.domain.CatalogLanguage
import com.artem.korenyakin.internassignment03.model.domain.CurrencyOption
import com.artem.korenyakin.internassignment03.model.domain.Product
import com.artem.korenyakin.internassignment03.model.domain.ProductCategory
import com.artem.korenyakin.internassignment03.model.domain.SortOption

internal data class ProductCatalogState(
    val searchQuery: String = "",
    val selectedLanguage: CatalogLanguage = CatalogLanguage.ENGLISH,
    val languages: List<CatalogLanguage> = CatalogLanguage.SupportedLanguages,
    val selectedCurrency: CurrencyOption = CurrencyOption.USD,
    val currencies: List<CurrencyOption> = CurrencyOption.SupportedCurrencies,
    val selectedCategory: ProductCategory = ProductCategory.ALL,
    val selectedSortOption: SortOption = SortOption.PRICE_ASC,
    val categories: List<ProductCategory> = listOf(ProductCategory.ALL),
    val products: List<Product> = emptyList(),
    val totalProducts: Int = 0,
    val currentPage: Int = InitialPage,
    val pageSize: Int = DefaultPageSize,
    val totalPages: Int = 0,
    val isLoading: Boolean = true,
    val isLoadingMore: Boolean = false,
    val canLoadMore: Boolean = false,
    val errorMessage: String? = null,
) {
    companion object {
        const val InitialPage: Int = 1
        const val DefaultPageSize: Int = 20
    }
}
