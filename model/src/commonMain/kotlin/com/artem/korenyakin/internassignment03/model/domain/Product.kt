package com.artem.korenyakin.internassignment03.model.domain

data class Product(
    val id: String,
    val title: String,
    val description: String,
    val price: Double,
    val imageUrl: String,
    val rating: Double,
    val category: ProductCategory,
)
