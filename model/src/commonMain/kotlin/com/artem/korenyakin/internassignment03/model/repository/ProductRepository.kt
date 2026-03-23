package com.artem.korenyakin.internassignment03.model.repository

import com.artem.korenyakin.internassignment03.model.domain.Product
import com.artem.korenyakin.internassignment03.model.domain.ProductCategory

public interface ProductRepository {
    public suspend fun getProducts(
        page: Int,
        pageSize: Int,
    ): List<Product>

    public suspend fun getCategories(): List<ProductCategory>
}
