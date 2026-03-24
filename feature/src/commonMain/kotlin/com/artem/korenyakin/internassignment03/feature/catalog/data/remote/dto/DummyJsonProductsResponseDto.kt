package com.artem.korenyakin.internassignment03.feature.catalog.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
internal data class DummyJsonProductsResponseDto(
    val products: List<DummyJsonProductDto>,
    val total: Int,
    val skip: Int,
    val limit: Int,
)
