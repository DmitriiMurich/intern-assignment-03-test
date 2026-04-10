package com.artem.korenyakin.internassignment03.feature.catalog

import com.artem.korenyakin.internassignment03.feature.catalog.data.repository.CatalogConnectionException
import com.artem.korenyakin.internassignment03.model.domain.CatalogLanguage
import com.artem.korenyakin.internassignment03.model.domain.CurrencyOption
import com.artem.korenyakin.internassignment03.model.domain.ProductCatalogPage
import com.artem.korenyakin.internassignment03.model.domain.ProductCatalogQuery
import com.artem.korenyakin.internassignment03.model.domain.ProductCategory
import com.artem.korenyakin.internassignment03.model.domain.SortOption
import com.artem.korenyakin.internassignment03.model.repository.AppLanguageManager
import com.artem.korenyakin.internassignment03.model.repository.ProductRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

internal class ProductCatalogViewModel(
    private val productRepository: ProductRepository,
    private val appLanguageManager: AppLanguageManager,
    dispatcher: CoroutineDispatcher = Dispatchers.Main.immediate,
) {
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + dispatcher)
    private val bootstrapCompletedFlow: MutableStateFlow<Boolean> = MutableStateFlow(false)
    private val searchQueryFlow: MutableStateFlow<String> = MutableStateFlow("")
    private val selectedCategoryFlow: MutableStateFlow<ProductCategory> = MutableStateFlow(ProductCategory.ALL)
    private val selectedSortOptionFlow: MutableStateFlow<SortOption> = MutableStateFlow(SortOption.PRICE_ASC)
    private val selectedLanguageFlow: MutableStateFlow<CatalogLanguage> = MutableStateFlow(CatalogLanguage.ENGLISH)
    private val selectedCurrencyFlow: MutableStateFlow<CurrencyOption> = MutableStateFlow(CurrencyOption.USD)

    private var refreshJob: Job? = null
    private var loadMoreJob: Job? = null
    private var productDetailsJob: Job? = null
    private var latestRequest: CatalogRequest? = null

    private val _state: MutableStateFlow<ProductCatalogState> = MutableStateFlow(ProductCatalogState())
    internal val state: StateFlow<ProductCatalogState> = _state.asStateFlow()
    private val _productDetailsState: MutableStateFlow<ProductDetailsState> =
        MutableStateFlow(ProductDetailsState.Hidden)
    internal val productDetailsState: StateFlow<ProductDetailsState> = _productDetailsState.asStateFlow()

    init {
        observeCatalogRequests()
        bootstrap()
    }

    internal fun onSearchQueryChanged(query: String) {
        searchQueryFlow.value = query
        _state.update { currentState ->
            currentState.copy(
                searchQuery = query,
            )
        }
    }

    internal fun onLanguageSelected(language: CatalogLanguage) {
        if (selectedLanguageFlow.value.code == language.code) {
            return
        }

        selectedLanguageFlow.value = language
        _state.update { currentState ->
            currentState.copy(
                selectedLanguage = language,
            )
        }
        appLanguageManager.updateLanguage(language.code)
    }

    internal fun onCurrencySelected(currency: CurrencyOption) {
        if (selectedCurrencyFlow.value.code == currency.code) {
            return
        }

        selectedCurrencyFlow.value = currency
        _state.update { currentState ->
            currentState.copy(
                selectedCurrency = currency,
            )
        }
    }

    internal fun onCategorySelected(category: ProductCategory) {
        selectedCategoryFlow.value = category
        _state.update { currentState ->
            currentState.copy(
                selectedCategory = category,
            )
        }
    }

    internal fun onSortOptionSelected(sortOption: SortOption) {
        selectedSortOptionFlow.value = sortOption
        _state.update { currentState ->
            currentState.copy(
                selectedSortOption = sortOption,
            )
        }
    }

    internal fun resetFilters() {
        searchQueryFlow.value = ""
        selectedCategoryFlow.value = ProductCategory.ALL
        selectedSortOptionFlow.value = SortOption.PRICE_ASC

        _state.update { currentState ->
            currentState.copy(
                searchQuery = "",
                selectedCategory = ProductCategory.ALL,
                selectedSortOption = SortOption.PRICE_ASC,
            )
        }
    }

    internal fun loadMore() {
        val currentState = state.value
        if (
            currentState.isLoading ||
            currentState.isLoadingMore ||
            !currentState.canLoadMore
        ) {
            return
        }

        val nextPage = currentState.currentPage + 1
        val nextQuery = buildCatalogQuery(page = nextPage)

        loadMoreJob?.cancel()
        loadMoreJob = scope.launch {
            _state.update { updatedState ->
                updatedState.copy(
                    isLoadingMore = true,
                    errorMessage = null,
                )
            }

            runCatching {
                productRepository.getCatalog(nextQuery)
            }.onSuccess { loadedPage ->
                handleLoadMoreSuccess(loadedPage)
            }.onFailure { throwable ->
                handleLoadFailure(throwable)
            }
        }
    }

    internal fun retryLoad() {
        refreshCatalog(latestRequest ?: currentRequest())
    }

    internal fun openProductDetails(productId: String) {
        val request = currentRequest()

        productDetailsJob?.cancel()
        _productDetailsState.value = ProductDetailsState.Loading(productId)
        productDetailsJob = scope.launch {
            runCatching {
                productRepository.getProductDetails(
                    productId = productId,
                    languageCode = request.language.code,
                    currencyCode = request.currency.code,
                )
            }.onSuccess { productDetails ->
                _productDetailsState.value = ProductDetailsState.Content(productDetails)
            }.onFailure { throwable ->
                _productDetailsState.value = ProductDetailsState.Error(
                    productId = productId,
                    errorMessage = errorMessageFor(throwable),
                )
            }
        }
    }

    internal fun retryProductDetails() {
        val currentProductDetailsState = productDetailsState.value
        val productId = when (currentProductDetailsState) {
            is ProductDetailsState.Loading -> currentProductDetailsState.productId
            is ProductDetailsState.Content -> currentProductDetailsState.details.product.id
            is ProductDetailsState.Error -> currentProductDetailsState.productId
            ProductDetailsState.Hidden -> null
        } ?: return

        openProductDetails(productId)
    }

    internal fun closeProductDetails() {
        productDetailsJob?.cancel()
        _productDetailsState.value = ProductDetailsState.Hidden
    }

    internal fun clear() {
        scope.cancel()
    }

    @OptIn(FlowPreview::class)
    private fun observeCatalogRequests() {
        scope.launch {
            combine(
                bootstrapCompletedFlow,
                combine(
                    searchQueryFlow.debounce(SEARCH_DEBOUNCE_MS),
                    selectedCategoryFlow,
                    selectedSortOptionFlow,
                    selectedLanguageFlow,
                    selectedCurrencyFlow,
                ) { query, category, sortOption, language, currency ->
                    CatalogRequest(
                        query = query,
                        category = category,
                        sortOption = sortOption,
                        language = language,
                        currency = currency,
                    )
                },
            ) { isBootstrapCompleted, request ->
                request.takeIf { isBootstrapCompleted }
            }.filterNotNull()
                .collect { request ->
                    latestRequest = request
                    refreshCatalog(request)
                }
        }
    }

    private fun bootstrap() {
        scope.launch {
            val languages = runCatching {
                productRepository.getLanguages()
            }.getOrDefault(CatalogLanguage.SupportedLanguages)
                .ifEmpty { CatalogLanguage.SupportedLanguages }
            val currencies = runCatching {
                productRepository.getCurrencies()
            }.getOrDefault(CurrencyOption.SupportedCurrencies)
                .ifEmpty { CurrencyOption.SupportedCurrencies }
            val preferredLanguageCode = appLanguageManager.getCurrentLanguageCode()

            val initialLanguage = languages.firstOrNull { language ->
                language.code == preferredLanguageCode
            } ?: languages.firstOrNull { language ->
                language.isSourceLanguage
            } ?: languages.first()
            val initialCurrency = currencies.firstOrNull { currency ->
                currency.isSourceCurrency
            } ?: currencies.first()

            selectedLanguageFlow.value = initialLanguage
            selectedCurrencyFlow.value = initialCurrency
            _state.update { currentState ->
                currentState.copy(
                    languages = languages,
                    selectedLanguage = initialLanguage,
                    currencies = currencies,
                    selectedCurrency = initialCurrency,
                )
            }
            bootstrapCompletedFlow.value = true
        }
    }

    private fun refreshCatalog() {
        refreshCatalog(currentRequest())
    }

    private fun refreshCatalog(
        request: CatalogRequest,
    ) {
        refreshJob?.cancel()
        loadMoreJob?.cancel()
        refreshJob = scope.launch {
            showLoadingState()

            runCatching {
                productRepository.getCatalog(
                    buildCatalogQuery(
                        page = ProductCatalogState.InitialPage,
                        request = request,
                    ),
                )
            }.onSuccess { loadedPage ->
                handleRefreshSuccess(loadedPage)
            }.onFailure { throwable ->
                handleLoadFailure(throwable)
            }
        }
    }

    private fun buildCatalogQuery(
        page: Int,
        request: CatalogRequest = currentRequest(),
    ): ProductCatalogQuery = ProductCatalogQuery(
        page = page,
        pageSize = state.value.pageSize,
        searchQuery = request.query,
        categorySlug = request.category
            .takeUnless { category -> category == ProductCategory.ALL }
            ?.slug,
        sortOption = request.sortOption,
        languageCode = request.language.code,
        currencyCode = request.currency.code,
    )

    private fun currentRequest(): CatalogRequest = CatalogRequest(
        query = searchQueryFlow.value,
        category = selectedCategoryFlow.value,
        sortOption = selectedSortOptionFlow.value,
        language = selectedLanguageFlow.value,
        currency = selectedCurrencyFlow.value,
    )

    private fun handleRefreshSuccess(
        loadedPage: ProductCatalogPage,
    ) {
        val categories = loadedPage.categories.withAllCategory()

        _state.update { currentState ->
            currentState.copy(
                categories = categories,
                products = loadedPage.products,
                totalProducts = loadedPage.totalProducts,
                currentPage = loadedPage.currentPage,
                pageSize = loadedPage.pageSize,
                totalPages = loadedPage.totalPages,
                canLoadMore = loadedPage.currentPage < loadedPage.totalPages,
                isLoading = false,
                isLoadingMore = false,
                errorMessage = null,
            )
        }
    }

    private fun handleLoadMoreSuccess(
        loadedPage: ProductCatalogPage,
    ) {
        _state.update { currentState ->
            currentState.copy(
                products = (currentState.products + loadedPage.products).distinctBy { product -> product.id },
                totalProducts = loadedPage.totalProducts,
                currentPage = loadedPage.currentPage,
                pageSize = loadedPage.pageSize,
                totalPages = loadedPage.totalPages,
                canLoadMore = loadedPage.currentPage < loadedPage.totalPages,
                isLoadingMore = false,
                errorMessage = null,
            )
        }
    }

    private fun handleLoadFailure(
        throwable: Throwable,
    ) {
        _state.update { currentState ->
            currentState.copy(
                isLoading = false,
                isLoadingMore = false,
                canLoadMore = currentState.currentPage < currentState.totalPages,
                errorMessage = errorMessageFor(throwable),
            )
        }
    }

    private fun showLoadingState() {
        _state.update { currentState ->
            currentState.copy(
                isLoading = true,
                isLoadingMore = false,
                products = emptyList(),
                totalProducts = 0,
                currentPage = ProductCatalogState.InitialPage,
                totalPages = 0,
                canLoadMore = false,
                errorMessage = null,
            )
        }
    }

    private fun List<ProductCategory>.withAllCategory(): List<ProductCategory> {
        return listOf(ProductCategory.ALL) + filterNot { category ->
            category == ProductCategory.ALL
        }
    }

    private fun errorMessageFor(
        throwable: Throwable,
    ): String = when (throwable) {
        is CatalogConnectionException -> ServerConnectionErrorToken
        else -> throwable.message ?: GenericLoadErrorToken
    }

    private data class CatalogRequest(
        val query: String,
        val category: ProductCategory,
        val sortOption: SortOption,
        val language: CatalogLanguage,
        val currency: CurrencyOption,
    )

    private companion object {
        private const val SEARCH_DEBOUNCE_MS: Long = 300L
    }
}

internal const val GenericLoadErrorToken: String = "__catalog_load_error__"
internal const val ServerConnectionErrorToken: String = "__catalog_server_connection_error__"
