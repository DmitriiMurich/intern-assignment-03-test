package com.artem.korenyakin.internassignment03

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.artem.korenyakin.internassignment03.feature.catalog.CatalogStrings
import com.artem.korenyakin.internassignment03.feature.catalog.ProductCatalogScreen
import com.artem.korenyakin.internassignment03.ui.theme.Internassignment03Theme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            val strings: CatalogStrings = rememberCatalogStrings()

            Internassignment03Theme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                ) {
                    ProductCatalogScreen(strings = strings)
                }
            }
        }
    }
}

@Composable
private fun rememberCatalogStrings(): CatalogStrings {
    val context = LocalContext.current

    return remember(context) {
        CatalogStrings(
            searchLabel = context.getString(R.string.search_label),
            searchPlaceholder = context.getString(R.string.search_placeholder),
            heroLabel = context.getString(R.string.hero_label),
            heroTitle = context.getString(R.string.hero_title),
            heroLoadingSubtitle = context.getString(R.string.hero_loading_subtitle),
            heroErrorSubtitle = context.getString(R.string.hero_error_subtitle),
            heroEmptyFilteredSubtitle = context.getString(R.string.hero_empty_filtered_subtitle),
            heroEmptySubtitle = context.getString(R.string.hero_empty_subtitle),
            heroResultsSubtitle = context.getString(R.string.hero_results_subtitle),
            loadingCatalogTitle = context.getString(R.string.loading_catalog_title),
            loadingCatalogSubtitle = context.getString(R.string.loading_catalog_subtitle),
            requestFailedLabel = context.getString(R.string.request_failed_label),
            requestFailedTitle = context.getString(R.string.request_failed_title),
            genericLoadError = context.getString(R.string.generic_load_error),
            retry = context.getString(R.string.retry),
            noResultsLabel = context.getString(R.string.no_results_label),
            noResultsTitle = context.getString(R.string.no_results_title),
            noResultsFilteredSubtitle = context.getString(R.string.no_results_filtered_subtitle),
            noResultsSubtitle = context.getString(R.string.no_results_subtitle),
            resetFilters = context.getString(R.string.reset_filters),
            sortBy = context.getString(R.string.sort_by),
            category = context.getString(R.string.category),
            results = context.getString(R.string.results),
            resultsSummary = context.getString(R.string.results_summary),
            ratingValue = context.getString(R.string.rating_value),
            dropdownIndicator = context.getString(R.string.dropdown_indicator),
            sortPriceAsc = context.getString(R.string.sort_price_asc),
            sortPriceDesc = context.getString(R.string.sort_price_desc),
            sortRatingDesc = context.getString(R.string.sort_rating_desc),
            categoryAll = context.getString(R.string.category_all),
        )
    }
}
