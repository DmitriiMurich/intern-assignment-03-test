package com.artem.korenyakin.internassignment03.feature.catalog

import com.artem.korenyakin.internassignment03.model.domain.CatalogLanguage
import com.artem.korenyakin.internassignment03.model.domain.CurrencyOption
import com.artem.korenyakin.internassignment03.model.domain.Money
import com.artem.korenyakin.internassignment03.model.domain.Product
import com.artem.korenyakin.internassignment03.model.domain.ProductDetails
import com.artem.korenyakin.internassignment03.model.domain.ProductCatalogPage
import com.artem.korenyakin.internassignment03.model.domain.ProductCatalogQuery
import com.artem.korenyakin.internassignment03.model.domain.ProductCategory
import com.artem.korenyakin.internassignment03.model.domain.ProductReview
import com.artem.korenyakin.internassignment03.model.domain.SortOption
import com.artem.korenyakin.internassignment03.model.repository.AppLanguageManager
import com.artem.korenyakin.internassignment03.model.repository.ProductRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
internal class ProductCatalogViewModelTest {
    @Test
    fun shouldDisplayFirstBackendPageOnInit() = runTest {
        val viewModel = createViewModel(
            repository = FakeProductRepository(),
        )

        try {
            advanceUntilIdle()

            val state = viewModel.state.value
            assertFalse(state.isLoading)
            assertNull(state.errorMessage)
            assertEquals("en", state.selectedLanguage.code)
            assertEquals(20, state.products.size)
            assertEquals(45, state.totalProducts)
            assertEquals(1, state.currentPage)
            assertEquals(3, state.totalPages)
            assertTrue(state.canLoadMore)
        } finally {
            viewModel.clear()
        }
    }

    @Test
    fun shouldRequestServerSearchAfterDebounce() = runTest {
        val repository = FakeProductRepository()
        val viewModel = createViewModel(
            repository = repository,
        )

        try {
            advanceUntilIdle()

            viewModel.onSearchQueryChanged("mint")
            assertEquals(20, viewModel.state.value.products.size)

            advanceTimeBy(299)
            assertEquals(20, viewModel.state.value.products.size)

            advanceTimeBy(1)
            advanceUntilIdle()

            val state = viewModel.state.value
            assertEquals("mint", state.searchQuery)
            assertEquals(listOf("Mint Tea", "Mint Toothpaste"), state.products.map(Product::title))
            assertEquals("mint", repository.catalogRequests.last().searchQuery)
            assertFalse(state.canLoadMore)
        } finally {
            viewModel.clear()
        }
    }

    @Test
    fun shouldFilterByCategoryAndSortOnServer() = runTest {
        val repository = FakeProductRepository()
        val viewModel = createViewModel(
            repository = repository,
        )

        try {
            advanceUntilIdle()

            viewModel.onCategorySelected(beautyCategory())
            viewModel.onSortOptionSelected(SortOption.RATING_DESC)
            advanceUntilIdle()

            val state = viewModel.state.value
            assertEquals(beautyCategory(), state.selectedCategory)
            assertEquals(SortOption.RATING_DESC, state.selectedSortOption)
            assertTrue(state.products.all { product -> product.category == beautyCategory() })
            assertEquals("beauty", repository.catalogRequests.last().categorySlug)
            assertEquals(SortOption.RATING_DESC, repository.catalogRequests.last().sortOption)
        } finally {
            viewModel.clear()
        }
    }

    @Test
    fun shouldChangeLanguageAndReloadCatalog() = runTest {
        val repository = FakeProductRepository()
        val appLanguageManager = FakeAppLanguageManager()
        val viewModel = createViewModel(
            repository = repository,
            appLanguageManager = appLanguageManager,
        )

        try {
            advanceUntilIdle()

            viewModel.onLanguageSelected(russianLanguage())
            advanceUntilIdle()

            val state = viewModel.state.value
            assertEquals("ru", state.selectedLanguage.code)
            assertTrue(state.products.all { product -> product.title.startsWith("[RU]") })
            assertEquals("ru", repository.catalogRequests.last().languageCode)
            assertEquals("ru", appLanguageManager.selectedLanguageCode)
        } finally {
            viewModel.clear()
        }
    }

    @Test
    fun shouldChangeCurrencyAndReloadCatalog() = runTest {
        val repository = FakeProductRepository()
        val viewModel = createViewModel(
            repository = repository,
        )

        try {
            advanceUntilIdle()

            viewModel.onCurrencySelected(euroCurrency())
            advanceUntilIdle()

            val state = viewModel.state.value
            assertEquals("EUR", state.selectedCurrency.code)
            assertTrue(state.products.all { product -> product.price.currency.code == "EUR" })
            assertEquals("EUR", repository.catalogRequests.last().currencyCode)
        } finally {
            viewModel.clear()
        }
    }

    @Test
    fun shouldOpenAndCloseProductDetails() = runTest {
        val repository = FakeProductRepository()
        val viewModel = createViewModel(
            repository = repository,
        )

        try {
            advanceUntilIdle()

            viewModel.openProductDetails("b-3")
            advanceUntilIdle()

            val detailState = viewModel.productDetailsState.value as ProductDetailsState.Content
            assertEquals("b-3", detailState.details.product.id)
            assertEquals("Night Serum", detailState.details.product.title)
            assertTrue(detailState.details.reviews.isNotEmpty())
            assertEquals("b-3", repository.requestedProductDetails.last())

            viewModel.closeProductDetails()

            assertEquals(ProductDetailsState.Hidden, viewModel.productDetailsState.value)
        } finally {
            viewModel.clear()
        }
    }

    @Test
    fun shouldUsePreferredAppLanguageOnInit() = runTest {
        val viewModel = createViewModel(
            repository = FakeProductRepository(),
            appLanguageManager = FakeAppLanguageManager(selectedLanguageCode = "ru"),
        )

        try {
            advanceUntilIdle()

            val state = viewModel.state.value
            assertEquals("ru", state.selectedLanguage.code)
            assertTrue(state.products.all { product -> product.title.startsWith("[RU]") })
        } finally {
            viewModel.clear()
        }
    }

    @Test
    fun shouldKeepSupportedLanguagesWhenLanguageRequestFailsOffline() = runTest {
        val repository = FakeProductRepository().apply {
            languagesError = IllegalStateException("Network unavailable")
        }
        val viewModel = createViewModel(
            repository = repository,
            appLanguageManager = FakeAppLanguageManager(selectedLanguageCode = "ru"),
        )

        try {
            advanceUntilIdle()

            val state = viewModel.state.value
            assertEquals(10, state.languages.size)
            assertEquals("ru", state.selectedLanguage.code)
            assertTrue(state.languages.any { language -> language.code == "de" })
            assertTrue(state.products.all { product -> product.title.startsWith("[RU]") })
        } finally {
            viewModel.clear()
        }
    }

    @Test
    fun shouldLoadMoreUsingServerPagination() = runTest {
        val viewModel = createViewModel(
            repository = FakeProductRepository(),
        )

        try {
            advanceUntilIdle()
            assertEquals(20, viewModel.state.value.products.size)

            viewModel.loadMore()
            advanceUntilIdle()

            assertEquals(40, viewModel.state.value.products.size)
            assertEquals(2, viewModel.state.value.currentPage)
            assertTrue(viewModel.state.value.canLoadMore)

            viewModel.loadMore()
            advanceUntilIdle()

            assertEquals(45, viewModel.state.value.products.size)
            assertEquals(3, viewModel.state.value.currentPage)
            assertFalse(viewModel.state.value.canLoadMore)
        } finally {
            viewModel.clear()
        }
    }

    @Test
    fun shouldResetSearchCategoryAndSortWithoutResettingLanguage() = runTest {
        val viewModel = createViewModel(
            repository = FakeProductRepository(),
        )

        try {
            advanceUntilIdle()

            viewModel.onLanguageSelected(russianLanguage())
            advanceUntilIdle()
            viewModel.onCategorySelected(beautyCategory())
            viewModel.onSearchQueryChanged("serum")
            viewModel.onSortOptionSelected(SortOption.RATING_DESC)
            advanceTimeBy(300)
            advanceUntilIdle()

            viewModel.resetFilters()
            advanceTimeBy(300)
            advanceUntilIdle()

            val state = viewModel.state.value
            assertEquals("", state.searchQuery)
            assertEquals(ProductCategory.ALL, state.selectedCategory)
            assertEquals(SortOption.PRICE_ASC, state.selectedSortOption)
            assertEquals("ru", state.selectedLanguage.code)
            assertEquals(20, state.products.size)
        } finally {
            viewModel.clear()
        }
    }

    @Test
    fun shouldRetryAfterCatalogFailure() = runTest {
        val repository = FakeProductRepository()
        repository.catalogError = IllegalStateException("Backend unavailable")
        val viewModel = createViewModel(
            repository = repository,
        )

        try {
            advanceUntilIdle()

            assertEquals("Backend unavailable", viewModel.state.value.errorMessage)
            assertTrue(viewModel.state.value.products.isEmpty())

            repository.catalogError = null
            viewModel.retryLoad()
            advanceUntilIdle()

            val state = viewModel.state.value
            assertNull(state.errorMessage)
            assertEquals(20, state.products.size)
            assertEquals(45, state.totalProducts)
        } finally {
            viewModel.clear()
        }
    }

    private fun TestScope.createViewModel(
        repository: ProductRepository,
        appLanguageManager: AppLanguageManager = FakeAppLanguageManager(),
    ): ProductCatalogViewModel {
        val dispatcher = StandardTestDispatcher(testScheduler)

        return ProductCatalogViewModel(
            productRepository = repository,
            appLanguageManager = appLanguageManager,
            dispatcher = dispatcher,
        )
    }

    private class FakeProductRepository : ProductRepository {
        val catalogRequests: MutableList<ProductCatalogQuery> = mutableListOf()
        val requestedProductDetails: MutableList<String> = mutableListOf()
        var catalogError: Throwable? = null
        var productDetailsError: Throwable? = null
        var languagesError: Throwable? = null

        override suspend fun getLanguages(): List<CatalogLanguage> {
            languagesError?.let { throwable -> throw throwable }
            return listOf(
                CatalogLanguage.ENGLISH,
                russianLanguage(),
            )
        }

        override suspend fun getCurrencies(): List<CurrencyOption> = listOf(
            CurrencyOption.USD,
            euroCurrency(),
        )

        override suspend fun getCatalog(
            query: ProductCatalogQuery,
        ): ProductCatalogPage {
            catalogError?.let { throwable -> throw throwable }
            catalogRequests += query

            val normalizedQuery = query.searchQuery.trim().lowercase()
            val localizedProducts = sampleProducts().map { product ->
                localizeProduct(product, query.languageCode, query.currencyCode)
            }
            val filteredProducts = localizedProducts
                .filter { product ->
                    normalizedQuery.isBlank() ||
                        product.title.lowercase().contains(normalizedQuery) ||
                        product.description.lowercase().contains(normalizedQuery)
                }
                .filter { product ->
                    query.categorySlug == null || product.category.slug == query.categorySlug
                }
                .sortedWith(sortComparator(query.sortOption))

            val fromIndex = ((query.page - 1) * query.pageSize).coerceAtLeast(0)
            val pagedProducts = filteredProducts.drop(fromIndex).take(query.pageSize)
            val totalProducts = filteredProducts.size
            val totalPages = if (totalProducts == 0) {
                0
            } else {
                (totalProducts + query.pageSize - 1) / query.pageSize
            }

            return ProductCatalogPage(
                language = when (query.languageCode) {
                    "ru" -> russianLanguage()
                    else -> CatalogLanguage.ENGLISH
                },
                categories = sampleCategories(),
                products = pagedProducts,
                totalProducts = totalProducts,
                currentPage = query.page,
                pageSize = query.pageSize,
                totalPages = totalPages,
            )
        }

        override suspend fun getProductDetails(
            productId: String,
            languageCode: String,
            currencyCode: String,
        ): ProductDetails {
            productDetailsError?.let { throwable -> throw throwable }
            requestedProductDetails += productId

            val product = sampleProducts()
                .first { product -> product.id == productId }
                .let { sourceProduct -> localizeProduct(sourceProduct, languageCode, currencyCode) }

            return ProductDetails(
                product = product,
                reviews = sampleReviews(productId).map { review ->
                    if (languageCode == CatalogLanguage.ENGLISH.code) {
                        review
                    } else {
                        review.copy(
                            comment = "[${languageCode.uppercase()}] ${review.comment}",
                        )
                    }
                },
            )
        }

        private fun localizeProduct(
            product: Product,
            languageCode: String,
            currencyCode: String,
        ): Product {
            return product.copy(
                title = if (languageCode == CatalogLanguage.ENGLISH.code) {
                    product.title
                } else {
                    "[${languageCode.uppercase()}] ${product.title}"
                },
                description = if (languageCode == CatalogLanguage.ENGLISH.code) {
                    product.description
                } else {
                    "[${languageCode.uppercase()}] ${product.description}"
                },
                price = localizePrice(product.price, currencyCode),
            )
        }

        private fun sortComparator(sortOption: SortOption): Comparator<Product> = when (sortOption) {
            SortOption.PRICE_ASC -> compareBy<Product>({ product -> product.price.amount }, Product::title)
            SortOption.PRICE_DESC -> compareByDescending<Product> { product -> product.price.amount }
                .thenBy(Product::title)
            SortOption.RATING_DESC -> compareByDescending<Product> { product -> product.rating }
                .thenBy(Product::title)
        }

        private fun localizePrice(
            price: Money,
            currencyCode: String,
        ): Money = when (currencyCode) {
            "EUR" -> Money(
                amount = price.amount * EuroRate,
                currency = euroCurrency(),
            )

            else -> price.copy(
                currency = CurrencyOption.USD,
            )
        }
    }

    private class FakeAppLanguageManager(
        var selectedLanguageCode: String = CatalogLanguage.ENGLISH.code,
    ) : AppLanguageManager {
        override fun getCurrentLanguageCode(): String = selectedLanguageCode

        override fun updateLanguage(languageCode: String) {
            selectedLanguageCode = languageCode
        }

        override fun applyCurrentLanguage() = Unit
    }

    private companion object {
        private fun sampleCategories(): List<ProductCategory> = listOf(
            groceriesCategory(),
            beautyCategory(),
            electronicsCategory(),
        )

        private fun sampleProducts(): List<Product> = buildList {
            repeat(25) { index ->
                add(
                    product(
                        id = "g-$index",
                        title = if (index == 5) "Mint Tea" else "Groceries Item ${index + 1}",
                        price = index + 1.0,
                        rating = 4.0 + (index % 5) * 0.1,
                        category = groceriesCategory(),
                    ),
                )
            }
            repeat(10) { index ->
                add(
                    product(
                        id = "b-$index",
                        title = when (index) {
                            3 -> "Night Serum"
                            6 -> "Mint Toothpaste"
                            else -> "Beauty Item ${index + 1}"
                        },
                        price = 50.0 + index,
                        rating = 4.5 + (index % 4) * 0.1,
                        category = beautyCategory(),
                    ),
                )
            }
            repeat(10) { index ->
                add(
                    product(
                        id = "e-$index",
                        title = "Electronics Item ${index + 1}",
                        price = 100.0 + index,
                        rating = 4.2 + (index % 3) * 0.1,
                        category = electronicsCategory(),
                    ),
                )
            }
        }

        private fun product(
            id: String,
            title: String,
            price: Double,
            rating: Double,
            category: ProductCategory,
        ): Product = Product(
            id = id,
            title = title,
            description = "$title description",
            price = Money(
                amount = price,
                currency = CurrencyOption.USD,
            ),
            imageUrl = "https://example.com/$id.png",
            rating = rating,
            category = category,
        )

        private fun sampleReviews(productId: String): List<ProductReview> = listOf(
            ProductReview(
                id = "$productId-r1",
                rating = 4.9,
                comment = "Looks premium and feels durable.",
                date = "2026-04-10T10:15:00.000Z",
                reviewerName = "Emma Wilson",
            ),
            ProductReview(
                id = "$productId-r2",
                rating = 4.7,
                comment = "Exactly what I expected from the catalog.",
                date = "2026-04-11T08:45:00.000Z",
                reviewerName = "Lucas Brown",
            ),
        )

        private fun groceriesCategory(): ProductCategory = ProductCategory(
            slug = "groceries",
            title = "Groceries",
        )

        private fun beautyCategory(): ProductCategory = ProductCategory(
            slug = "beauty",
            title = "Beauty",
        )

        private fun electronicsCategory(): ProductCategory = ProductCategory(
            slug = "electronics",
            title = "Electronics",
        )

        private fun russianLanguage(): CatalogLanguage = CatalogLanguage(
            code = "ru",
            name = "Russian",
            isSourceLanguage = false,
        )

        private fun euroCurrency(): CurrencyOption = CurrencyOption(
            code = "EUR",
            name = "Euro",
            symbol = "\u20AC",
            isSourceCurrency = false,
        )

        private const val EuroRate: Double = 0.91
    }
}
