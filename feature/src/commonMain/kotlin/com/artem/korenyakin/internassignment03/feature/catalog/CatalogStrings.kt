package com.artem.korenyakin.internassignment03.feature.catalog

import androidx.compose.runtime.Composable
import com.artem.korenyakin.internassignment03.feature.catalog.resources.Res
import com.artem.korenyakin.internassignment03.feature.catalog.resources.category_all
import com.artem.korenyakin.internassignment03.feature.catalog.resources.generic_load_error
import com.artem.korenyakin.internassignment03.feature.catalog.resources.hero_results_subtitle
import com.artem.korenyakin.internassignment03.feature.catalog.resources.rating_value
import com.artem.korenyakin.internassignment03.feature.catalog.resources.results_summary
import com.artem.korenyakin.internassignment03.feature.catalog.resources.sort_price_asc
import com.artem.korenyakin.internassignment03.feature.catalog.resources.sort_price_desc
import com.artem.korenyakin.internassignment03.feature.catalog.resources.sort_rating_desc
import com.artem.korenyakin.internassignment03.model.domain.ProductCategory
import com.artem.korenyakin.internassignment03.model.domain.SortOption
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun categoryTitle(category: ProductCategory): String = when (category) {
    ProductCategory.ALL -> stringResource(Res.string.category_all)
    else -> category.title
}

@Composable
internal fun sortOptionTitle(sortOption: SortOption): String = when (sortOption) {
    SortOption.PRICE_ASC -> stringResource(Res.string.sort_price_asc)
    SortOption.PRICE_DESC -> stringResource(Res.string.sort_price_desc)
    SortOption.RATING_DESC -> stringResource(Res.string.sort_rating_desc)
}

@Composable
internal fun formatHeroResultsSubtitle(
    visibleCount: Int,
    totalCount: Int,
): String = stringResource(
    resource = Res.string.hero_results_subtitle,
    visibleCount,
    totalCount,
)

@Composable
internal fun formatResultsSummary(
    visibleCount: Int,
    totalCount: Int,
): String = stringResource(
    resource = Res.string.results_summary,
    visibleCount,
    totalCount,
)

@Composable
internal fun formatRatingValue(
    rating: String,
): String = stringResource(
    resource = Res.string.rating_value,
    rating,
)

@Composable
internal fun genericLoadError(): String = stringResource(Res.string.generic_load_error)
