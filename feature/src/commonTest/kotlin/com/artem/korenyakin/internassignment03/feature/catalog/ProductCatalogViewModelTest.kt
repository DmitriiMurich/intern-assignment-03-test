package com.artem.korenyakin.internassignment03.feature.catalog

import com.artem.korenyakin.internassignment03.feature.catalog.domain.SearchProductsUseCase
import com.artem.korenyakin.internassignment03.model.domain.Product
import com.artem.korenyakin.internassignment03.model.domain.ProductCategory
import com.artem.korenyakin.internassignment03.model.domain.SortOption
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
    fun shouldDisplayProductsOnInit() = runTest {
        val viewModel = createViewModel(
            repository = FakeProductRepository(),
        )

        try {
            advanceUntilIdle()

            val state = viewModel.state.value
            assertFalse(state.isLoading)
            assertNull(state.errorMessage)
            assertEquals(45, state.products.size)
            assertEquals(20, state.visibleProducts.size)
            assertTrue(state.canLoadMore)
            assertEquals(ProductCategory.ALL, state.selectedCategory)
            assertEquals(SortOption.PRICE_ASC, state.selectedSortOption)
        } finally {
            viewModel.clear()
        }
    }

    @Test
    fun shouldFilterProductsBySearchQueryAfterDebounce() = runTest {
        val viewModel = createViewModel(
            repository = FakeProductRepository(),
        )

        try {
            advanceUntilIdle()

            viewModel.onSearchQueryChanged("mint")
            assertEquals(20, viewModel.state.value.visibleProducts.size)

            advanceTimeBy(299)
            assertEquals(20, viewModel.state.value.visibleProducts.size)

            advanceTimeBy(1)
            advanceUntilIdle()

            val state = viewModel.state.value
            assertEquals("mint", state.searchQuery)
            assertEquals(
                listOf("Mint Tea", "Mint Toothpaste"),
                state.visibleProducts.map(Product::title),
            )
            assertFalse(state.canLoadMore)
        } finally {
            viewModel.clear()
        }
    }

    @Test
    fun shouldFilterProductsByCategory() = runTest {
        val viewModel = createViewModel(
            repository = FakeProductRepository(),
        )

        try {
            advanceUntilIdle()

            viewModel.onCategorySelected(groceriesCategory())
            advanceUntilIdle()

            val state = viewModel.state.value
            assertEquals(groceriesCategory(), state.selectedCategory)
            assertTrue(state.visibleProducts.all { product -> product.category == groceriesCategory() })
            assertEquals(20, state.visibleProducts.size)
            assertTrue(state.canLoadMore)
        } finally {
            viewModel.clear()
        }
    }

    @Test
    fun shouldSortProductsByPriceAscending() = runTest {
        val viewModel = createViewModel(
            repository = FakeProductRepository(),
        )

        try {
            advanceUntilIdle()

            val prices = viewModel.state.value.visibleProducts.map(Product::price)
            val sortedPrices = prices.sorted()

            assertEquals(sortedPrices, prices)
        } finally {
            viewModel.clear()
        }
    }

    @Test
    fun shouldCombineSearchAndCategoryFilters() = runTest {
        val viewModel = createViewModel(
            repository = FakeProductRepository(),
        )

        try {
            advanceUntilIdle()

            viewModel.onCategorySelected(beautyCategory())
            viewModel.onSearchQueryChanged("serum")
            advanceTimeBy(300)
            advanceUntilIdle()

            val state = viewModel.state.value
            assertEquals(beautyCategory(), state.selectedCategory)
            assertEquals(listOf("Night Serum"), state.visibleProducts.map(Product::title))
        } finally {
            viewModel.clear()
        }
    }

    @Test
    fun shouldExposeEmptyResultsWithoutError() = runTest {
        val viewModel = createViewModel(
            repository = FakeProductRepository(),
        )

        try {
            advanceUntilIdle()

            viewModel.onSearchQueryChanged("missing-product")
            advanceTimeBy(300)
            advanceUntilIdle()

            val state = viewModel.state.value
            assertTrue(state.visibleProducts.isEmpty())
            assertTrue(state.filteredProducts.isEmpty())
            assertNull(state.errorMessage)
        } finally {
            viewModel.clear()
        }
    }

    @Test
    fun shouldLoadMoreProducts() = runTest {
        val viewModel = createViewModel(
            repository = FakeProductRepository(),
        )

        try {
            advanceUntilIdle()

            assertEquals(20, viewModel.state.value.visibleProducts.size)

            viewModel.loadMore()
            advanceUntilIdle()

            val state = viewModel.state.value
            assertEquals(40, state.visibleProducts.size)
            assertTrue(state.canLoadMore)

            viewModel.loadMore()
            advanceUntilIdle()

            assertEquals(45, viewModel.state.value.visibleProducts.size)
            assertFalse(viewModel.state.value.canLoadMore)
        } finally {
            viewModel.clear()
        }
    }

    @Test
    fun shouldResetFilters() = runTest {
        val viewModel = createViewModel(
            repository = FakeProductRepository(),
        )

        try {
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
            assertEquals(20, state.visibleProducts.size)
            assertTrue(state.canLoadMore)
        } finally {
            viewModel.clear()
        }
    }

    @Test
    fun shouldExposeErrorStateWhenRepositoryFails() = runTest {
        val viewModel = createViewModel(
            repository = FakeProductRepository(
                productsError = IllegalStateException("No internet connection"),
            ),
        )

        try {
            advanceUntilIdle()

            val state = viewModel.state.value
            assertFalse(state.isLoading)
            assertEquals("No internet connection", state.errorMessage)
            assertTrue(state.visibleProducts.isEmpty())
            assertFalse(state.canLoadMore)
        } finally {
            viewModel.clear()
        }
    }

    @Test
    fun shouldRetryLoadAfterFailure() = runTest {
        val repository = RetryableFakeProductRepository(
            initialProductsError = IllegalStateException("Request failed"),
        )
        val viewModel = createViewModel(
            repository = repository,
        )

        try {
            advanceUntilIdle()
            assertEquals("Request failed", viewModel.state.value.errorMessage)

            repository.productsError = null
            viewModel.retryLoad()
            advanceUntilIdle()

            val state = viewModel.state.value
            assertNull(state.errorMessage)
            assertEquals(45, state.products.size)
            assertEquals(20, state.visibleProducts.size)
        } finally {
            viewModel.clear()
        }
    }

    private fun TestScope.createViewModel(
        repository: ProductRepository,
    ): ProductCatalogViewModel {
        val dispatcher = StandardTestDispatcher(testScheduler)

        return ProductCatalogViewModel(
            productRepository = repository,
            searchProductsUseCase = SearchProductsUseCase(),
            dispatcher = dispatcher,
        )
    }

    private open class FakeProductRepository(
        private val products: List<Product> = sampleProducts(),
        private val categories: List<ProductCategory> = sampleCategories(),
        private val categoriesError: Throwable? = null,
        protected open var productsError: Throwable? = null,
    ) : ProductRepository {
        override suspend fun getProducts(
            page: Int,
            pageSize: Int,
        ): List<Product> {
            productsError?.let { throwable -> throw throwable }

            return products
                .drop(page * pageSize)
                .take(pageSize)
        }

        override suspend fun getCategories(): List<ProductCategory> {
            categoriesError?.let { throwable -> throw throwable }
            return categories
        }
    }

    private class RetryableFakeProductRepository(
        initialProductsError: Throwable? = null,
    ) : FakeProductRepository(
        productsError = initialProductsError,
    ) {
        public override var productsError: Throwable? = initialProductsError
    }

    private companion object {
        private fun sampleCategories(): List<ProductCategory> = listOf(
            ProductCategory.ALL,
            groceriesCategory(),
            beautyCategory(),
            electronicsCategory(),
        )

        private fun sampleProducts(): List<Product> = buildList {
            repeat(25) { index ->
                add(
                    product(
                        id = "g-$index",
                        title = if (index == 5) {
                            "Mint Tea"
                        } else {
                            "Groceries Item ${index + 1}"
                        },
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
            price = price,
            imageUrl = "https://example.com/$id.png",
            rating = rating,
            category = category,
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
    }
}
