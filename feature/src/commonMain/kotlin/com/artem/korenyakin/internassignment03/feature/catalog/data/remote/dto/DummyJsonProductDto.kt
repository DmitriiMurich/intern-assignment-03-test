package com.artem.korenyakin.internassignment03.feature.catalog.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal data class DummyJsonProductDto(
    val id: Long,
    val title: String,
    val description: String,
    val category: String,
    val price: Double,
    val rating: Double = 0.0,
    val images: List<String> = emptyList(),
    @SerialName("thumbnail")
    val thumbnailUrl: String? = null,
)