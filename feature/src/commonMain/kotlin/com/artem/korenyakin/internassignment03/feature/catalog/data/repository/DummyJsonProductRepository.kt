package com.artem.korenyakin.internassignment03.feature.catalog.data.repository

import com.artem.korenyakin.internassignment03.feature.catalog.data.mapper.toDomain
import com.artem.korenyakin.internassignment03.feature.catalog.data.mapper.toProductCategory
import com.artem.korenyakin.internassignment03.feature.catalog.data.remote.dto.DummyJsonProductsResponseDto
import com.artem.korenyakin.internassignment03.model.domain.Product
import com.artem.korenyakin.internassignment03.model.domain.ProductCategory
import com.artem.korenyakin.internassignment03.model.repository.ProductRepository
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.parameter

internal class DummyJsonProductRepository(
    private val httpClient: HttpClient,
) : ProductRepository {
    override suspend fun getProducts(
        page: Int,
        pageSize: Int,
    ): List<Product> {
        if (pageSize <= 0) {
            return emptyList()
        }

        val safePage = page.coerceAtLeast(0)

        return httpClient.get("products") {
            parameter("limit", pageSize)
            parameter("skip", safePage * pageSize)
        }.body<DummyJsonProductsResponseDto>()
            .products
            .map { productDto -> productDto.toDomain() }
    }

    override suspend fun getCategories(): List<ProductCategory> {
        val categories: List<String> = httpClient.get("products/category-list").body()

        return buildList {
            add(ProductCategory.ALL)
            categories
                .map { categorySlug -> categorySlug.toProductCategory() }
                .forEach { category -> add(category) }
        }
    }
}
