package com.artem.korenyakin.internassignment03.feature.catalog

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import com.artem.korenyakin.internassignment03.feature.catalog.components.ProductCard
import com.artem.korenyakin.internassignment03.feature.catalog.components.SearchBar
import com.artem.korenyakin.internassignment03.model.domain.ProductCategory
import com.artem.korenyakin.internassignment03.model.domain.SortOption
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map
import org.koin.core.context.GlobalContext

@Composable
fun ProductCatalogScreen(
    modifier: Modifier = Modifier,
) {
    val viewModel = remember {
        GlobalContext.get().get<ProductCatalogViewModel>()
    }
    val state by viewModel.state.collectAsState()

    DisposableEffect(viewModel) {
        onDispose {
            viewModel.clear()
        }
    }

    ProductCatalogContent(
        state = state,
        onSearchQueryChanged = viewModel::onSearchQueryChanged,
        onCategorySelected = viewModel::onCategorySelected,
        onSortOptionSelected = viewModel::onSortOptionSelected,
        onLoadMore = viewModel::loadMore,
        onRetry = viewModel::retryLoad,
        modifier = modifier,
    )
}

@Composable
internal fun ProductCatalogContent(
    state: ProductCatalogState,
    onSearchQueryChanged: (String) -> Unit,
    onCategorySelected: (ProductCategory) -> Unit,
    onSortOptionSelected: (SortOption) -> Unit,
    onLoadMore: () -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val listState = rememberLazyListState()
    val focusManager = LocalFocusManager.current

    LaunchedEffect(
        listState,
        state.visibleProducts.size,
        state.canLoadMore,
        state.isLoadingMore,
    ) {
        snapshotFlow {
            listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index
        }
            .map { lastVisibleIndex ->
                lastVisibleIndex?.let { index ->
                    index >= state.visibleProducts.lastIndex - LOAD_MORE_THRESHOLD
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
        when {
            state.isLoading -> {
                ScreenStatus(
                    title = "Loading catalog",
                    subtitle = "Fetching products and categories from the API",
                    modifier = Modifier
                        .fillMaxSize()
                        .statusBarsPadding()
                        .padding(16.dp),
                ) {
                    CircularProgressIndicator()
                }
            }

            state.errorMessage != null && state.visibleProducts.isEmpty() -> {
                ScreenStatus(
                    title = "Could not load the catalog",
                    subtitle = state.errorMessage,
                    modifier = Modifier
                        .fillMaxSize()
                        .statusBarsPadding()
                        .padding(16.dp),
                ) {
                    Button(onClick = onRetry) {
                        Text(text = "Try again")
                    }
                }
            }

            state.visibleProducts.isEmpty() -> {
                ScreenStatus(
                    title = "Nothing found",
                    subtitle = "Try a different search query or choose another category",
                    modifier = Modifier
                        .fillMaxSize()
                        .statusBarsPadding()
                        .padding(16.dp),
                )
            }

            else -> {
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
                        HeroSection(state = state)
                    }
                    item(key = "controls") {
                        ControlsSection(
                            state = state,
                            onSearchQueryChanged = onSearchQueryChanged,
                            onCategorySelected = onCategorySelected,
                            onSortOptionSelected = onSortOptionSelected,
                        )
                    }
                    item(key = "results-summary") {
                        ResultsSummary(
                            visibleCount = state.visibleProducts.size,
                            totalCount = state.filteredProducts.size,
                        )
                    }
                    items(
                        items = state.visibleProducts,
                        key = { product -> product.id },
                    ) { product ->
                        ProductCard(
                            product = product,
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

@Composable
private fun HeroSection(
    state: ProductCatalogState,
) {
    Surface(
        shape = RoundedCornerShape(30.dp),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 10.dp,
        border = BorderStroke(
            width = 1.dp,
            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.7f),
        ),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    brush = Brush.linearGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.primaryContainer,
                            MaterialTheme.colorScheme.secondaryContainer,
                        ),
                    ),
                )
                .padding(20.dp),
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    text = "PRODUCT CATALOG",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = "Deals, picks and fast filters",
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = heroSubtitle(state = state),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun ControlsSection(
    state: ProductCatalogState,
    onSearchQueryChanged: (String) -> Unit,
    onCategorySelected: (ProductCategory) -> Unit,
    onSortOptionSelected: (SortOption) -> Unit,
) {
    val focusManager = LocalFocusManager.current

    Surface(
        shape = RoundedCornerShape(28.dp),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 8.dp,
        border = BorderStroke(
            width = 1.dp,
            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.7f),
        ),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            SearchBar(
                query = state.searchQuery,
                onQueryChanged = onSearchQueryChanged,
                modifier = Modifier.fillMaxWidth(),
            )
            CatalogDropdown(
                title = "Category",
                selectedTitle = state.selectedCategory.title,
                options = state.categories,
                optionTitle = { category -> category.title },
                onSelected = onCategorySelected,
                onExpanded = { focusManager.clearFocus() },
            )
            CatalogDropdown(
                title = "Sort by",
                selectedTitle = state.selectedSortOption.title,
                options = SortOption.entries.toList(),
                optionTitle = { sortOption -> sortOption.title },
                onSelected = onSortOptionSelected,
                onExpanded = { focusManager.clearFocus() },
            )
        }
    }
}

@Composable
private fun <T> CatalogDropdown(
    title: String,
    selectedTitle: String,
    options: List<T>,
    optionTitle: (T) -> String,
    onSelected: (T) -> Unit,
    onExpanded: () -> Unit,
) {
    var expanded by remember {
        mutableStateOf(false)
    }

    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Box(
            modifier = Modifier.fillMaxWidth(),
        ) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(22.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
                border = BorderStroke(
                    width = 1.dp,
                    color = MaterialTheme.colorScheme.outline,
                ),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            onExpanded()
                            expanded = true
                        }
                        .padding(horizontal = 16.dp, vertical = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(2.dp),
                    ) {
                        Text(
                            text = title.uppercase(),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Text(
                            text = selectedTitle,
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                    Text(
                        text = "v",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
            DropdownMenu(
                expanded = expanded,
                onDismissRequest = {
                    expanded = false
                },
                modifier = Modifier.heightIn(max = 320.dp),
                offset = DpOffset(x = 0.dp, y = 8.dp),
            ) {
                options.forEach { option ->
                    DropdownMenuItem(
                        text = {
                            Text(text = optionTitle(option))
                        },
                        onClick = {
                            onSelected(option)
                            expanded = false
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun ResultsSummary(
    visibleCount: Int,
    totalCount: Int,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = "Results",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.SemiBold,
        )
        Surface(
            shape = RoundedCornerShape(999.dp),
            color = MaterialTheme.colorScheme.surfaceVariant,
            border = BorderStroke(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outline,
            ),
        ) {
            Text(
                text = "$visibleCount of $totalCount",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            )
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

private fun heroSubtitle(
    state: ProductCatalogState,
): String = when {
    state.isLoading -> "Loading fresh items."
    state.errorMessage != null && state.visibleProducts.isEmpty() -> "The page is ready, but the API did not respond as expected."
    else -> "Showing ${state.visibleProducts.size} of ${state.filteredProducts.size} items."
}

private const val LOAD_MORE_THRESHOLD: Int = 2
