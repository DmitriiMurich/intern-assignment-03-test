package com.artem.korenyakin.internassignment03.model.repository

import com.artem.korenyakin.internassignment03.model.domain.Product
import com.artem.korenyakin.internassignment03.model.domain.ProductCategory

interface ProductRepository {
    suspend fun getProducts(
        page: Int,
        pageSize: Int,
    ): List<Product>

    suspend fun getCategories(): List<ProductCategory>
}
