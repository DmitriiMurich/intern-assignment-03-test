package com.artem.korenyakin.internassignment03.feature.catalog

import com.artem.korenyakin.internassignment03.feature.catalog.domain.SearchProductsUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

internal class ProductCatalogViewModel(
    private val searchProductsUseCase: SearchProductsUseCase,
) {
    private val _state: MutableStateFlow<ProductCatalogState> = MutableStateFlow(ProductCatalogState())
    internal val state: StateFlow<ProductCatalogState> = _state.asStateFlow()

    internal fun onSearchQueryChanged(query: String) {
        _state.update { currentState ->
            currentState.copy(
                searchQuery = query,
                visibleProducts = searchProductsUseCase(
                    products = currentState.products,
                    query = query,
                ),
            )
        }
    }
}
