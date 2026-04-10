package com.artem.korenyakin.internassignment03.feature.catalog.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
internal data class CatalogResponseDto(
    val language: String,
    val categories: List<CatalogCategoryDto>,
    val items: List<CatalogProductDto>,
    val meta: CatalogMetaDto,
)

@Serializable
internal data class CatalogProductDetailsDto(
    val language: String,
    val currency: String,
    val product: CatalogProductDto,
    val reviews: List<CatalogReviewDto>,
    val meta: CatalogProductDetailsMetaDto,
)

@Serializable
internal data class CatalogCategoryDto(
    val slug: String,
    val title: String,
)

@Serializable
internal data class CatalogProductDto(
    val id: String,
    val title: String,
    val description: String,
    val price: CatalogMoneyDto,
    val rating: Double,
    val imageUrl: String,
    val category: CatalogCategoryDto,
)

@Serializable
internal data class CatalogMoneyDto(
    val amount: Double,
    val currency: String,
)

@Serializable
internal data class CatalogReviewDto(
    val id: String,
    val rating: Double,
    val comment: String,
    val date: String,
    val reviewerName: String,
)

@Serializable
internal data class CatalogMetaDto(
    val totalProducts: Int,
    val totalCategories: Int,
    val currentPage: Int,
    val pageSize: Int,
    val totalPages: Int,
    val query: String,
    val category: String? = null,
    val sort: String,
    val sourceLanguage: String,
    val sourceCurrency: String,
    val exchangeRateProvider: String? = null,
)

@Serializable
internal data class CatalogProductDetailsMetaDto(
    val sourceLanguage: String,
    val sourceCurrency: String,
    val exchangeRateProvider: String? = null,
)
