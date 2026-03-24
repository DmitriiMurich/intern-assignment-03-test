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
import kotlinx.coroutines.async
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
        loadedPagesFlow.value = 1
        _state.update { currentState ->
            currentState.copy(
                searchQuery = query,
            )
        }
    }

    internal fun onCategorySelected(category: ProductCategory) {
        selectedCategoryFlow.value = category
        loadedPagesFlow.value = 1
        _state.update { currentState ->
            currentState.copy(
                selectedCategory = category,
            )
        }
    }

    internal fun onSortOptionSelected(sortOption: SortOption) {
        selectedSortOptionFlow.value = sortOption
        loadedPagesFlow.value = 1
        _state.update { currentState ->
            currentState.copy(
                selectedSortOption = sortOption,
            )
        }
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
        loadedPagesFlow.value = 1
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
            }.collect { derivedCatalog ->
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
        }
    }

    private fun loadCatalog() {
        scope.launch {
            _state.update { currentState ->
                currentState.copy(
                    isLoading = true,
                    isLoadingMore = false,
                    errorMessage = null,
                )
            }

            runCatching {
                val categoriesDeferred = async {
                    productRepository.getCategories()
                }
                val products = productRepository.getProducts(
                    page = 0,
                    pageSize = REMOTE_PAGE_SIZE,
                )

                LoadedCatalog(
                    products = products,
                    categories = categoriesDeferred.await(),
                )
            }.onSuccess { loadedCatalog ->
                val categories = loadedCatalog.categories.ifEmpty {
                    listOf(ProductCategory.ALL)
                }
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
            }.onFailure { throwable ->
                categoriesFlow.value = listOf(ProductCategory.ALL)
                allProductsFlow.value = emptyList()
                _state.update { currentState ->
                    currentState.copy(
                        categories = listOf(ProductCategory.ALL),
                        products = emptyList(),
                        filteredProducts = emptyList(),
                        visibleProducts = emptyList(),
                        isLoading = false,
                        isLoadingMore = false,
                        canLoadMore = false,
                        errorMessage = throwable.message ?: DEFAULT_ERROR_MESSAGE,
                    )
                }
            }
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
        private const val DEFAULT_ERROR_MESSAGE: String = "Could not load the catalog"
    }
}