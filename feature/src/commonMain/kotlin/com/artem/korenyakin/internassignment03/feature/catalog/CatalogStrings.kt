package com.artem.korenyakin.internassignment03.feature.catalog

import androidx.compose.runtime.Composable
import com.artem.korenyakin.internassignment03.feature.catalog.resources.Res
import com.artem.korenyakin.internassignment03.feature.catalog.resources.category_all
import com.artem.korenyakin.internassignment03.feature.catalog.resources.generic_load_error
import com.artem.korenyakin.internassignment03.feature.catalog.resources.hero_results_subtitle
import com.artem.korenyakin.internassignment03.feature.catalog.resources.language_name_de
import com.artem.korenyakin.internassignment03.feature.catalog.resources.language_name_en
import com.artem.korenyakin.internassignment03.feature.catalog.resources.language_name_es
import com.artem.korenyakin.internassignment03.feature.catalog.resources.language_name_fr
import com.artem.korenyakin.internassignment03.feature.catalog.resources.language_name_it
import com.artem.korenyakin.internassignment03.feature.catalog.resources.language_name_pt
import com.artem.korenyakin.internassignment03.feature.catalog.resources.language_name_ru
import com.artem.korenyakin.internassignment03.feature.catalog.resources.language_name_tr
import com.artem.korenyakin.internassignment03.feature.catalog.resources.language_name_uk
import com.artem.korenyakin.internassignment03.feature.catalog.resources.language_name_zh
import com.artem.korenyakin.internassignment03.feature.catalog.resources.rating_value
import com.artem.korenyakin.internassignment03.feature.catalog.resources.results_summary
import com.artem.korenyakin.internassignment03.feature.catalog.resources.server_connection_error
import com.artem.korenyakin.internassignment03.feature.catalog.resources.sort_price_asc
import com.artem.korenyakin.internassignment03.feature.catalog.resources.sort_price_desc
import com.artem.korenyakin.internassignment03.feature.catalog.resources.sort_rating_desc
import com.artem.korenyakin.internassignment03.model.domain.CatalogLanguage
import com.artem.korenyakin.internassignment03.model.domain.CurrencyOption
import com.artem.korenyakin.internassignment03.model.domain.Money
import com.artem.korenyakin.internassignment03.model.domain.ProductCategory
import com.artem.korenyakin.internassignment03.model.domain.SortOption
import kotlin.math.roundToInt
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
internal fun languageTitle(language: CatalogLanguage): String = when (language.code) {
    "en" -> stringResource(Res.string.language_name_en)
    "ru" -> stringResource(Res.string.language_name_ru)
    "de" -> stringResource(Res.string.language_name_de)
    "fr" -> stringResource(Res.string.language_name_fr)
    "es" -> stringResource(Res.string.language_name_es)
    "it" -> stringResource(Res.string.language_name_it)
    "pt" -> stringResource(Res.string.language_name_pt)
    "tr" -> stringResource(Res.string.language_name_tr)
    "uk" -> stringResource(Res.string.language_name_uk)
    "zh" -> stringResource(Res.string.language_name_zh)
    else -> language.name
}

internal fun currencyTitle(currency: CurrencyOption): String = "${currency.code} ${currency.symbol}"

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

@Composable
internal fun serverConnectionError(): String = stringResource(Res.string.server_connection_error)

@Composable
internal fun resolveErrorMessage(
    errorMessage: String?,
): String = when (errorMessage) {
    null,
    GenericLoadErrorToken,
    -> genericLoadError()

    ServerConnectionErrorToken -> serverConnectionError()
    else -> errorMessage
}

internal fun formatRatingNumber(
    rating: Double,
): String = ((rating * RatingFractionMultiplier).roundToInt() / RatingFractionMultiplier).toString()

internal fun formatReviewDate(
    rawDate: String,
): String = rawDate.substringBefore("T").takeIf { value ->
    value.isNotBlank()
} ?: rawDate

internal fun formatPrice(money: Money): String {
    val roundedValue = ((money.amount * PriceFractionMultiplier).roundToInt() / PriceFractionMultiplier)
    val rawText = roundedValue.toString()
    val normalizedText = if (rawText.endsWith(".0")) {
        rawText.dropLast(PriceTrimSuffixLength)
    } else {
        rawText
    }

    return when (money.currency.code) {
        "CHF" -> "${money.currency.symbol} $normalizedText"
        else -> "${money.currency.symbol}$normalizedText"
    }
}

private const val PriceFractionMultiplier: Double = 100.0
private const val PriceTrimSuffixLength: Int = 2
private const val RatingFractionMultiplier: Double = 10.0
