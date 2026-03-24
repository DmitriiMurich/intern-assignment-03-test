package com.artem.korenyakin.internassignment03.model.domain

data class ProductCategory(
    val slug: String,
    val title: String,
) {
    companion object {
        val ALL: ProductCategory = ProductCategory(
            slug = "all",
            title = "All",
        )
    }
}
