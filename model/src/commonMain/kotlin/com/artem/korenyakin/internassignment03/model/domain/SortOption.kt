package com.artem.korenyakin.internassignment03.model.domain

enum class SortOption(
    val apiValue: String,
) {
    PRICE_ASC(apiValue = "price_asc"),
    PRICE_DESC(apiValue = "price_desc"),
    RATING_DESC(apiValue = "rating_desc"),
    ;

    companion object {
        fun fromApiValue(value: String): SortOption = entries.firstOrNull { sortOption ->
            sortOption.apiValue == value
        } ?: PRICE_ASC
    }
}
