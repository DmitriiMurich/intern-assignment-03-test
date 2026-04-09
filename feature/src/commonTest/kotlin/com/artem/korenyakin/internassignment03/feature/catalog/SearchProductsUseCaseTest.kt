package com.artem.korenyakin.internassignment03.feature.catalog

import com.artem.korenyakin.internassignment03.feature.catalog.domain.SearchProductsUseCase
import com.artem.korenyakin.internassignment03.model.domain.CurrencyOption
import com.artem.korenyakin.internassignment03.model.domain.Money
import com.artem.korenyakin.internassignment03.model.domain.Product
import com.artem.korenyakin.internassignment03.model.domain.ProductCategory
import com.artem.korenyakin.internassignment03.model.domain.SortOption
import kotlin.test.Test
import kotlin.test.assertEquals

internal class SearchProductsUseCaseTest {
    private val useCase: SearchProductsUseCase = SearchProductsUseCase()

    @Test
    fun shouldDisplayProductsWithoutFilters() {
        val result = useCase(
            products = sampleProducts(),
            query = "",
            selectedCategory = ProductCategory.ALL,
            selectedSortOption = SortOption.PRICE_ASC,
        )

        assertEquals(
            listOf("Apple", "Body Wash", "Phone Case", "Tea", "Laptop Pro"),
            result.map(Product::title),
        )
    }

    @Test
    fun shouldFilterProductsBySearchQuery() {
        val result = useCase(
            products = sampleProducts(),
            query = " phone ",
            selectedCategory = ProductCategory.ALL,
            selectedSortOption = SortOption.PRICE_ASC,
        )

        assertEquals(
            listOf("Phone Case"),
            result.map(Product::title),
        )
    }

    @Test
    fun shouldFilterProductsByCategory() {
        val result = useCase(
            products = sampleProducts(),
            query = "",
            selectedCategory = accessoriesCategory(),
            selectedSortOption = SortOption.PRICE_ASC,
        )

        assertEquals(
            listOf("Phone Case"),
            result.map(Product::title),
        )
    }

    @Test
    fun shouldSortProductsByPriceAscending() {
        val result = useCase(
            products = sampleProducts(),
            query = "",
            selectedCategory = ProductCategory.ALL,
            selectedSortOption = SortOption.PRICE_ASC,
        )

        assertEquals(
            listOf(1.5, 7.0, 12.0, 19.0, 999.0),
            result.map { product -> product.price.amount },
        )
    }

    @Test
    fun shouldSortProductsByRatingDescending() {
        val result = useCase(
            products = sampleProducts(),
            query = "",
            selectedCategory = ProductCategory.ALL,
            selectedSortOption = SortOption.RATING_DESC,
        )

        assertEquals(
            listOf("Body Wash", "Phone Case", "Apple", "Tea", "Laptop Pro"),
            result.map(Product::title),
        )
    }

    @Test
    fun shouldCombineSearchAndCategoryFilters() {
        val result = useCase(
            products = sampleProducts(),
            query = "body",
            selectedCategory = beautyCategory(),
            selectedSortOption = SortOption.PRICE_ASC,
        )

        assertEquals(
            listOf("Body Wash"),
            result.map(Product::title),
        )
    }

    private fun sampleProducts(): List<Product> = listOf(
        product(
            id = "1",
            title = "Laptop Pro",
            price = 999.0,
            rating = 4.1,
            category = electronicsCategory(),
        ),
        product(
            id = "2",
            title = "Apple",
            price = 1.5,
            rating = 4.7,
            category = groceriesCategory(),
        ),
        product(
            id = "3",
            title = "Body Wash",
            price = 7.0,
            rating = 4.9,
            category = beautyCategory(),
        ),
        product(
            id = "4",
            title = "Phone Case",
            price = 12.0,
            rating = 4.8,
            category = accessoriesCategory(),
        ),
        product(
            id = "5",
            title = "Tea",
            price = 19.0,
            rating = 4.2,
            category = groceriesCategory(),
        ),
    )

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

    private fun electronicsCategory(): ProductCategory = ProductCategory(
        slug = "electronics",
        title = "Electronics",
    )

    private fun groceriesCategory(): ProductCategory = ProductCategory(
        slug = "groceries",
        title = "Groceries",
    )

    private fun beautyCategory(): ProductCategory = ProductCategory(
        slug = "beauty",
        title = "Beauty",
    )

    private fun accessoriesCategory(): ProductCategory = ProductCategory(
        slug = "mobile-accessories",
        title = "Mobile Accessories",
    )
}
