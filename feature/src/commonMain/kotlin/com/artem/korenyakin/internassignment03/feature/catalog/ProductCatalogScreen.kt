package com.artem.korenyakin.internassignment03.feature.catalog

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.artem.korenyakin.internassignment03.feature.catalog.components.CatalogFeedbackCard
import com.artem.korenyakin.internassignment03.feature.catalog.components.ControlsSection
import com.artem.korenyakin.internassignment03.feature.catalog.components.HeroSection
import com.artem.korenyakin.internassignment03.feature.catalog.components.ProductCard
import com.artem.korenyakin.internassignment03.feature.catalog.components.ResultsSummary
import com.artem.korenyakin.internassignment03.feature.catalog.resources.Res
import com.artem.korenyakin.internassignment03.feature.catalog.resources.hero_empty_filtered_subtitle
import com.artem.korenyakin.internassignment03.feature.catalog.resources.hero_empty_subtitle
import com.artem.korenyakin.internassignment03.feature.catalog.resources.hero_error_subtitle
import com.artem.korenyakin.internassignment03.feature.catalog.resources.hero_label
import com.artem.korenyakin.internassignment03.feature.catalog.resources.hero_loading_subtitle
import com.artem.korenyakin.internassignment03.feature.catalog.resources.hero_title
import com.artem.korenyakin.internassignment03.feature.catalog.resources.loading_catalog_subtitle
import com.artem.korenyakin.internassignment03.feature.catalog.resources.loading_catalog_title
import com.artem.korenyakin.internassignment03.feature.catalog.resources.no_results_filtered_subtitle
import com.artem.korenyakin.internassignment03.feature.catalog.resources.no_results_label
import com.artem.korenyakin.internassignment03.feature.catalog.resources.no_results_subtitle
import com.artem.korenyakin.internassignment03.feature.catalog.resources.no_results_title
import com.artem.korenyakin.internassignment03.feature.catalog.resources.request_failed_label
import com.artem.korenyakin.internassignment03.feature.catalog.resources.request_failed_title
import com.artem.korenyakin.internassignment03.feature.catalog.resources.reset_filters
import com.artem.korenyakin.internassignment03.feature.catalog.resources.results
import com.artem.korenyakin.internassignment03.feature.catalog.resources.retry
import com.artem.korenyakin.internassignment03.model.domain.CurrencyOption
import com.artem.korenyakin.internassignment03.model.domain.ProductCategory
import com.artem.korenyakin.internassignment03.model.domain.SortOption
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map
import org.jetbrains.compose.resources.stringResource
import org.koin.core.context.GlobalContext

@Composable
fun ProductCatalogScreen(
    modifier: Modifier = Modifier,
) {
    val viewModel = remember {
        GlobalContext.get().get<ProductCatalogViewModel>()
    }
    val state by viewModel.state.collectAsState()
    val productDetailsState by viewModel.productDetailsState.collectAsState()
    val listState = rememberLazyListState()

    DisposableEffect(viewModel) {
        onDispose {
            viewModel.clear()
        }
    }

    when (productDetailsState) {
        ProductDetailsState.Hidden -> {
            ProductCatalogContent(
                state = state,
                listState = listState,
                onSearchQueryChanged = viewModel::onSearchQueryChanged,
                onLanguageSelected = viewModel::onLanguageSelected,
                onCurrencySelected = viewModel::onCurrencySelected,
                onCategorySelected = viewModel::onCategorySelected,
                onSortOptionSelected = viewModel::onSortOptionSelected,
                onLoadMore = viewModel::loadMore,
                onRetry = viewModel::retryLoad,
                onResetFilters = viewModel::resetFilters,
                onProductSelected = viewModel::openProductDetails,
                modifier = modifier,
            )
        }

        else -> {
            ProductDetailsContent(
                state = productDetailsState,
                onBack = viewModel::closeProductDetails,
                onRetry = viewModel::retryProductDetails,
                modifier = modifier,
            )
        }
    }
}

@Suppress("LongMethod", "CyclomaticComplexMethod")
@Composable
internal fun ProductCatalogContent(
    state: ProductCatalogState,
    listState: LazyListState,
    onSearchQueryChanged: (String) -> Unit,
    onLanguageSelected: (com.artem.korenyakin.internassignment03.model.domain.CatalogLanguage) -> Unit,
    onCurrencySelected: (CurrencyOption) -> Unit,
    onCategorySelected: (ProductCategory) -> Unit,
    onSortOptionSelected: (SortOption) -> Unit,
    onLoadMore: () -> Unit,
    onRetry: () -> Unit,
    onResetFilters: () -> Unit,
    onProductSelected: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val focusManager = LocalFocusManager.current
    val hasLoadedContent = state.products.isNotEmpty()
    val hasActiveFilters = state.searchQuery.isNotBlank() ||
        state.selectedCategory != ProductCategory.ALL ||
        state.selectedSortOption != SortOption.PRICE_ASC

    LaunchedEffect(
        listState,
        state.products.size,
        state.canLoadMore,
        state.isLoadingMore,
    ) {
        snapshotFlow {
            listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index
        }
            .map { lastVisibleIndex ->
                lastVisibleIndex?.let { index ->
                    index >= state.products.lastIndex - LoadMoreThreshold
                }
            }
            .filterNotNull()
            .distinctUntilChanged()
            .collect { shouldLoadMore ->
                if (shouldLoadMore && state.canLoadMore && !state.isLoadingMore) {
                    onLoadMore()
                }
            }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.42f),
                        MaterialTheme.colorScheme.background,
                        MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.18f),
                    ),
                ),
            )
            .pointerInput(focusManager) {
                awaitEachGesture {
                    val down = awaitFirstDown(pass = PointerEventPass.Final)
                    val up = waitForUpOrCancellation(pass = PointerEventPass.Final)

                    if (up != null && !down.isConsumed && !up.isConsumed) {
                        focusManager.clearFocus()
                    }
                }
            },
    ) {
        if (state.isLoading && !hasLoadedContent) {
            ScreenStatus(
                title = stringResource(Res.string.loading_catalog_title),
                subtitle = stringResource(Res.string.loading_catalog_subtitle),
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .padding(16.dp),
            ) {
                CircularProgressIndicator()
            }
        } else {
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding(),
                contentPadding = PaddingValues(
                    start = 16.dp,
                    top = 16.dp,
                    end = 16.dp,
                    bottom = 28.dp,
                ),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                item(key = "hero") {
                    HeroSection(
                        label = stringResource(Res.string.hero_label),
                        title = stringResource(Res.string.hero_title),
                        subtitle = heroSubtitle(state = state),
                    )
                }
                item(key = "controls") {
                    ControlsSection(
                        state = state,
                        onSearchQueryChanged = onSearchQueryChanged,
                        onLanguageSelected = onLanguageSelected,
                        onCurrencySelected = onCurrencySelected,
                        onCategorySelected = onCategorySelected,
                        onSortOptionSelected = onSortOptionSelected,
                    )
                }

                when {
                    state.errorMessage != null && !hasLoadedContent -> {
                        item(key = "error-state") {
                            CatalogFeedbackCard(
                                label = stringResource(Res.string.request_failed_label),
                                title = stringResource(Res.string.request_failed_title),
                                subtitle = resolveErrorMessage(errorMessage = state.errorMessage),
                                actionLabel = stringResource(Res.string.retry),
                                onAction = onRetry,
                            )
                        }
                    }

                    state.products.isEmpty() -> {
                        item(key = "empty-state") {
                            CatalogFeedbackCard(
                                label = stringResource(Res.string.no_results_label),
                                title = stringResource(Res.string.no_results_title),
                                subtitle = if (hasActiveFilters) {
                                    stringResource(Res.string.no_results_filtered_subtitle)
                                } else {
                                    stringResource(Res.string.no_results_subtitle)
                                },
                                actionLabel = if (hasActiveFilters) {
                                    stringResource(Res.string.reset_filters)
                                } else {
                                    null
                                },
                                onAction = if (hasActiveFilters) onResetFilters else null,
                            )
                        }
                    }

                    else -> {
                        item(key = "results-summary") {
                            ResultsSummary(
                                title = stringResource(Res.string.results),
                                summary = formatResultsSummary(
                                    visibleCount = state.products.size,
                                    totalCount = state.totalProducts,
                                ),
                            )
                        }
                        items(
                            items = state.products,
                            key = { product -> product.id },
                        ) { product ->
                            ProductCard(
                                product = product,
                                onClick = { onProductSelected(product.id) },
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }
                        if (state.isLoadingMore) {
                            item(key = "pagination-loader") {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 8.dp),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    CircularProgressIndicator()
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ScreenStatus(
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
    action: (@Composable () -> Unit)? = null,
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Surface(
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.surface,
            shadowElevation = 6.dp,
            border = BorderStroke(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.6f),
            ),
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center,
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 8.dp),
                )
                if (action != null) {
                    Box(
                        modifier = Modifier.padding(top = 16.dp),
                    ) {
                        action()
                    }
                }
            }
        }
    }
}

@Composable
private fun heroSubtitle(
    state: ProductCatalogState,
): String = when {
    state.isLoading -> stringResource(Res.string.hero_loading_subtitle)
    state.errorMessage != null && state.products.isEmpty() -> stringResource(Res.string.hero_error_subtitle)
    state.products.isEmpty() && state.totalProducts == 0 && hasActiveFilters(state) ->
        stringResource(Res.string.hero_empty_filtered_subtitle)
    state.products.isEmpty() -> stringResource(Res.string.hero_empty_subtitle)
    else -> formatHeroResultsSubtitle(
        visibleCount = state.products.size,
        totalCount = state.totalProducts,
    )
}

private const val LoadMoreThreshold: Int = 2

private fun hasActiveFilters(
    state: ProductCatalogState,
): Boolean = state.searchQuery.isNotBlank() ||
    state.selectedCategory != ProductCategory.ALL ||
    state.selectedSortOption != SortOption.PRICE_ASC
