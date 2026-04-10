package com.artem.korenyakin.internassignment03.feature.catalog

import com.artem.korenyakin.internassignment03.model.domain.ProductDetails

internal sealed interface ProductDetailsState {
    data object Hidden : ProductDetailsState

    data class Loading(
        val productId: String,
    ) : ProductDetailsState

    data class Content(
        val details: ProductDetails,
    ) : ProductDetailsState

    data class Error(
        val productId: String,
        val errorMessage: String,
    ) : ProductDetailsState
}
