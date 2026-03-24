package com.artem.korenyakin.internassignment03.feature.catalog

import com.artem.korenyakin.internassignment03.model.domain.ProductCategory
import com.artem.korenyakin.internassignment03.model.domain.SortOption

data class CatalogStrings(
    val searchLabel: String,
    val searchPlaceholder: String,
    val heroLabel: String,
    val heroTitle: String,
    val heroLoadingSubtitle: String,
    val heroErrorSubtitle: String,
    val heroEmptyFilteredSubtitle: String,
    val heroEmptySubtitle: String,
    val heroResultsSubtitle: String,
    val loadingCatalogTitle: String,
    val loadingCatalogSubtitle: String,
    val requestFailedLabel: String,
    val requestFailedTitle: String,
    val genericLoadError: String,
    val retry: String,
    val noResultsLabel: String,
    val noResultsTitle: String,
    val noResultsFilteredSubtitle: String,
    val noResultsSubtitle: String,
    val resetFilters: String,
    val sortBy: String,
    val category: String,
    val results: String,
    val resultsSummary: String,
    val ratingValue: String,
    val dropdownIndicator: String,
    val sortPriceAsc: String,
    val sortPriceDesc: String,
    val sortRatingDesc: String,
    val categoryAll: String,
)

internal fun CatalogStrings.categoryTitle(category: ProductCategory): String = when (category) {
    ProductCategory.ALL -> categoryAll
    else -> category.title
}

internal fun CatalogStrings.sortOptionTitle(sortOption: SortOption): String = when (sortOption) {
    SortOption.PRICE_ASC -> sortPriceAsc
    SortOption.PRICE_DESC -> sortPriceDesc
    SortOption.RATING_DESC -> sortRatingDesc
}

internal fun CatalogStrings.formatHeroResultsSubtitle(
    visibleCount: Int,
    totalCount: Int,
): String = formatTemplate(
    template = heroResultsSubtitle,
    visibleCount,
    totalCount,
)

internal fun CatalogStrings.formatResultsSummary(
    visibleCount: Int,
    totalCount: Int,
): String = formatTemplate(
    template = resultsSummary,
    visibleCount,
    totalCount,
)

internal fun CatalogStrings.formatRatingValue(
    rating: String,
): String = formatTemplate(
    template = ratingValue,
    rating,
)

private fun formatTemplate(
    template: String,
    vararg args: Any,
): String {
    var formattedText: String = template

    args.forEachIndexed { index, value ->
        val placeholderIndex: Int = index + 1
        val replacement: String = value.toString()
        formattedText = formattedText
            .replace("%${placeholderIndex}\$d", replacement)
            .replace("%${placeholderIndex}\$s", replacement)
    }

    return formattedText
}
