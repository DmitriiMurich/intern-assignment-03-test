package com.artem.korenyakin.internassignment03.model.domain

data class ProductCatalogQuery(
    val page: Int,
    val pageSize: Int,
    val searchQuery: String,
    val categorySlug: String?,
    val sortOption: SortOption,
    val languageCode: String,
    val currencyCode: String,
)
