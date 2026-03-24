package com.artem.korenyakin.internassignment03.feature.catalog

import com.artem.korenyakin.internassignment03.feature.catalog.domain.SearchProductsUseCase
import com.artem.korenyakin.internassignment03.model.domain.Product
import com.artem.korenyakin.internassignment03.model.domain.ProductCategory
import com.artem.korenyakin.internassignment03.model.domain.SortOption
import com.artem.korenyakin.internassignment03.model.repository.ProductRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

internal class ProductCatalogViewModel(
    private val productRepository: ProductRepository,
    private val searchProductsUseCase: SearchProductsUseCase,
    dispatcher: CoroutineDispatcher = Dispatchers.Main.immediate,
) {
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + dispatcher)
    private val allProductsFlow: MutableStateFlow<List<Product>> = MutableStateFlow(emptyList())
    private val categoriesFlow: MutableStateFlow<List<ProductCategory>> = MutableStateFlow(listOf(ProductCategory.ALL))
    private val searchQueryFlow: MutableStateFlow<String> = MutableStateFlow("")
    private val selectedCategoryFlow: MutableStateFlow<ProductCategory> = MutableStateFlow(ProductCategory.ALL)
    private val selectedSortOptionFlow: MutableStateFlow<SortOption> = MutableStateFlow(SortOption.PRICE_ASC)
    private val loadedPagesFlow: MutableStateFlow<Int> = MutableStateFlow(1)

    private val _state: MutableStateFlow<ProductCatalogState> = MutableStateFlow(ProductCatalogState())
    internal val state: StateFlow<ProductCatalogState> = _state.asStateFlow()

    init {
        observeCatalog()
        loadCatalog()
    }

    internal fun onSearchQueryChanged(query: String) {
        searchQueryFlow.value = query
        resetPagination()
        updateFilterState(
            searchQuery = query,
        )
    }

    internal fun onCategorySelected(category: ProductCategory) {
        selectedCategoryFlow.value = category
        resetPagination()
        updateFilterState(
            selectedCategory = category,
        )
    }

    internal fun onSortOptionSelected(sortOption: SortOption) {
        selectedSortOptionFlow.value = sortOption
        resetPagination()
        updateFilterState(
            selectedSortOption = sortOption,
        )
    }

    internal fun resetFilters() {
        searchQueryFlow.value = ""
        selectedCategoryFlow.value = ProductCategory.ALL
        selectedSortOptionFlow.value = SortOption.PRICE_ASC
        resetPagination()
        updateFilterState(
            searchQuery = "",
            selectedCategory = ProductCategory.ALL,
            selectedSortOption = SortOption.PRICE_ASC,
        )
    }

    internal fun loadMore() {
        val currentState = state.value
        if (currentState.isLoading || currentState.isLoadingMore || !currentState.canLoadMore) {
            return
        }

        _state.update { updatedState ->
            updatedState.copy(
                isLoadingMore = true,
            )
        }
        loadedPagesFlow.value = loadedPagesFlow.value + 1
    }

    internal fun retryLoad() {
        resetPagination()
        loadCatalog()
    }

    internal fun clear() {
        scope.cancel()
    }

    @OptIn(FlowPreview::class)
    private fun observeCatalog() {
        val catalogSourceFlow = combine(
            allProductsFlow,
            categoriesFlow,
        ) { products, categories ->
            CatalogSource(
                products = products,
                categories = categories,
            )
        }

        scope.launch {
            combine(
                catalogSourceFlow,
                searchQueryFlow.debounce(SEARCH_DEBOUNCE_MS),
                selectedCategoryFlow,
                selectedSortOptionFlow,
                loadedPagesFlow,
            ) { catalogSource, debouncedQuery, selectedCategory, selectedSortOption, loadedPages ->
                deriveCatalog(
                    products = catalogSource.products,
                    categories = catalogSource.categories,
                    query = debouncedQuery,
                    selectedCategory = selectedCategory,
                    selectedSortOption = selectedSortOption,
                    loadedPages = loadedPages,
                )
            }.collect(::applyDerivedCatalog)
        }
    }

    private fun loadCatalog() {
        scope.launch {
            showLoadingState()

            runCatching {
                requestCatalog()
            }.onSuccess(::handleLoadSuccess)
                .onFailure(::handleLoadFailure)
        }
    }

    private suspend fun requestCatalog(): LoadedCatalog {
        val products = productRepository.getProducts(
            page = 0,
            pageSize = REMOTE_PAGE_SIZE,
        )
        val categories = productRepository.getCategories()

        return LoadedCatalog(
            products = products,
            categories = categories,
        )
    }

    private fun handleLoadSuccess(loadedCatalog: LoadedCatalog) {
        val categories = loadedCatalog.categories.orDefault()
        val initialCatalog = deriveCatalog(
            products = loadedCatalog.products,
            categories = categories,
            query = searchQueryFlow.value,
            selectedCategory = selectedCategoryFlow.value,
            selectedSortOption = selectedSortOptionFlow.value,
            loadedPages = loadedPagesFlow.value,
        )

        categoriesFlow.value = categories
        allProductsFlow.value = loadedCatalog.products
        _state.update { currentState ->
            currentState.copy(
                categories = initialCatalog.categories,
                products = initialCatalog.products,
                filteredProducts = initialCatalog.filteredProducts,
                visibleProducts = initialCatalog.visibleProducts,
                canLoadMore = initialCatalog.canLoadMore,
                isLoading = false,
                isLoadingMore = false,
                errorMessage = null,
            )
        }
    }

    private fun handleLoadFailure(throwable: Throwable) {
        val categories = listOf(ProductCategory.ALL)
        categoriesFlow.value = categories
        allProductsFlow.value = emptyList()
        _state.update { currentState ->
            currentState.copy(
                categories = categories,
                products = emptyList(),
                filteredProducts = emptyList(),
                visibleProducts = emptyList(),
                isLoading = false,
                isLoadingMore = false,
                canLoadMore = false,
                errorMessage = throwable.message ?: GenericLoadErrorToken,
            )
        }
    }

    private fun deriveCatalog(
        products: List<Product>,
        categories: List<ProductCategory>,
        query: String,
        selectedCategory: ProductCategory,
        selectedSortOption: SortOption,
        loadedPages: Int,
    ): DerivedCatalog {
        val filteredProducts = searchProductsUseCase(
            products = products,
            query = query,
            selectedCategory = selectedCategory,
            selectedSortOption = selectedSortOption,
        )
        val visibleCount = (loadedPages * VISIBLE_PAGE_SIZE).coerceAtMost(filteredProducts.size)

        return DerivedCatalog(
            categories = categories,
            products = products,
            filteredProducts = filteredProducts,
            visibleProducts = filteredProducts.take(visibleCount),
            canLoadMore = visibleCount < filteredProducts.size,
        )
    }

    private fun resetPagination() {
        loadedPagesFlow.value = 1
    }

    private fun updateFilterState(
        searchQuery: String = searchQueryFlow.value,
        selectedCategory: ProductCategory = selectedCategoryFlow.value,
        selectedSortOption: SortOption = selectedSortOptionFlow.value,
    ) {
        _state.update { currentState ->
            currentState.copy(
                searchQuery = searchQuery,
                selectedCategory = selectedCategory,
                selectedSortOption = selectedSortOption,
            )
        }
    }

    private fun applyDerivedCatalog(derivedCatalog: DerivedCatalog) {
        _state.update { currentState ->
            currentState.copy(
                categories = derivedCatalog.categories,
                products = derivedCatalog.products,
                filteredProducts = derivedCatalog.filteredProducts,
                visibleProducts = derivedCatalog.visibleProducts,
                canLoadMore = derivedCatalog.canLoadMore,
                isLoadingMore = false,
            )
        }
    }

    private fun showLoadingState() {
        _state.update { currentState ->
            currentState.copy(
                isLoading = true,
                isLoadingMore = false,
                errorMessage = null,
            )
        }
    }

    private fun List<ProductCategory>.orDefault(): List<ProductCategory> {
        return ifEmpty {
            listOf(ProductCategory.ALL)
        }
    }

    private data class CatalogSource(
        val products: List<Product>,
        val categories: List<ProductCategory>,
    )

    private data class DerivedCatalog(
        val categories: List<ProductCategory>,
        val products: List<Product>,
        val filteredProducts: List<Product>,
        val visibleProducts: List<Product>,
        val canLoadMore: Boolean,
    )

    private data class LoadedCatalog(
        val products: List<Product>,
        val categories: List<ProductCategory>,
    )

    private companion object {
        private const val SEARCH_DEBOUNCE_MS: Long = 300L
        private const val REMOTE_PAGE_SIZE: Int = 200
        private const val VISIBLE_PAGE_SIZE: Int = 20
    }
}

internal const val GenericLoadErrorToken: String = "__catalog_load_error__"
