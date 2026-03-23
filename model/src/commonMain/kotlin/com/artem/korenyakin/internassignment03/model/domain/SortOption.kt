package com.artem.korenyakin.internassignment03.model.domain

public enum class SortOption(
    val title: String,
) {
    PRICE_ASC("Цена: по возрастанию"),
    PRICE_DESC("Цена: по убыванию"),
    RATING_DESC("Рейтинг"),
}
