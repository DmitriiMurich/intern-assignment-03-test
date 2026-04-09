package com.artem.korenyakin.internassignment03.feature.catalog.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
internal data class CatalogLanguageDto(
    val code: String,
    val name: String,
    val isSourceLanguage: Boolean,
)

@Serializable
internal data class CatalogLanguagesResponseDto(
    val items: List<CatalogLanguageDto>,
)
