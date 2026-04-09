package com.artem.korenyakin.internassignment03.feature.catalog.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
internal data class CatalogCurrencyDto(
    val code: String,
    val name: String,
    val symbol: String,
    val isSourceCurrency: Boolean,
)

@Serializable
internal data class CatalogCurrenciesResponseDto(
    val items: List<CatalogCurrencyDto>,
)
