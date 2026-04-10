package com.artem.korenyakin.internassignment03.model.domain

data class ProductDetails(
    val product: Product,
    val reviews: List<ProductReview>,
)
