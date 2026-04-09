package com.artem.korenyakin.internassignment03.model.domain

data class ProductCatalogPage(
    val language: CatalogLanguage,
    val categories: List<ProductCategory>,
    val products: List<Product>,
    val totalProducts: Int,
    val currentPage: Int,
    val pageSize: Int,
    val totalPages: Int,
)
