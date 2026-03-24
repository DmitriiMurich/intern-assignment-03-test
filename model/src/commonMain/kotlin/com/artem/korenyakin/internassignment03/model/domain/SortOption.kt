package com.artem.korenyakin.internassignment03.model.domain

public enum class SortOption(
    val title: String,
) {
    PRICE_ASC("Price: low to high"),
    PRICE_DESC("Price: high to low"),
    RATING_DESC("Rating: high to low"),
}