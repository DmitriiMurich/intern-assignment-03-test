package com.artem.korenyakin.internassignment03.model.domain

data class ProductReview(
    val id: String,
    val rating: Double,
    val comment: String,
    val date: String,
    val reviewerName: String,
)
