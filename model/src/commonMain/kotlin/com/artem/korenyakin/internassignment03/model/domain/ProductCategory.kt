package com.artem.korenyakin.internassignment03.model.domain

public data class ProductCategory(
    val slug: String,
    val title: String,
) {
    public companion object {
        public val ALL: ProductCategory = ProductCategory(
            slug = "all",
            title = "All",
        )
    }
}