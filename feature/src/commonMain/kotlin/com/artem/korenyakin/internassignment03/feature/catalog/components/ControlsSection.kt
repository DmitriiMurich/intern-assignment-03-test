package com.artem.korenyakin.internassignment03.feature.catalog.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import com.artem.korenyakin.internassignment03.feature.catalog.ProductCatalogState
import com.artem.korenyakin.internassignment03.feature.catalog.categoryTitle
import com.artem.korenyakin.internassignment03.feature.catalog.currencyTitle
import com.artem.korenyakin.internassignment03.feature.catalog.languageTitle
import com.artem.korenyakin.internassignment03.feature.catalog.resources.Res
import com.artem.korenyakin.internassignment03.feature.catalog.resources.category
import com.artem.korenyakin.internassignment03.feature.catalog.resources.currency
import com.artem.korenyakin.internassignment03.feature.catalog.resources.dropdown_indicator
import com.artem.korenyakin.internassignment03.feature.catalog.resources.language
import com.artem.korenyakin.internassignment03.feature.catalog.resources.search_label
import com.artem.korenyakin.internassignment03.feature.catalog.resources.search_placeholder
import com.artem.korenyakin.internassignment03.feature.catalog.resources.sort_by
import com.artem.korenyakin.internassignment03.model.domain.CatalogLanguage
import com.artem.korenyakin.internassignment03.model.domain.CurrencyOption
import com.artem.korenyakin.internassignment03.feature.catalog.sortOptionTitle
import com.artem.korenyakin.internassignment03.model.domain.ProductCategory
import com.artem.korenyakin.internassignment03.model.domain.SortOption
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun ControlsSection(
    state: ProductCatalogState,
    onSearchQueryChanged: (String) -> Unit,
    onLanguageSelected: (CatalogLanguage) -> Unit,
    onCurrencySelected: (CurrencyOption) -> Unit,
    onCategorySelected: (ProductCategory) -> Unit,
    onSortOptionSelected: (SortOption) -> Unit,
) {
    val focusManager = LocalFocusManager.current
    val languageOptions = state.languages.toLanguageOptions()
    val currencyOptions = state.currencies.toCurrencyOptions()
    val sortOptions = SortOption.entries.toSortOptions()

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
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            SearchBar(
                query = state.searchQuery,
                label = stringResource(Res.string.search_label),
                placeholder = stringResource(Res.string.search_placeholder),
                onQueryChanged = onSearchQueryChanged,
                modifier = Modifier.fillMaxWidth(),
            )
            CatalogDropdown(
                title = stringResource(Res.string.language),
                selectedTitle = languageTitle(state.selectedLanguage),
                options = languageOptions,
                indicatorText = stringResource(Res.string.dropdown_indicator),
                onSelected = onLanguageSelected,
                onExpanded = { focusManager.clearFocus() },
            )
            CatalogDropdown(
                title = stringResource(Res.string.currency),
                selectedTitle = currencyTitle(state.selectedCurrency),
                options = currencyOptions,
                indicatorText = stringResource(Res.string.dropdown_indicator),
                onSelected = onCurrencySelected,
                onExpanded = { focusManager.clearFocus() },
            )
            CategoryChipsSection(
                categories = state.categories,
                selectedCategory = state.selectedCategory,
                onCategorySelected = { category ->
                    focusManager.clearFocus()
                    onCategorySelected(category)
                },
            )
            CatalogDropdown(
                title = stringResource(Res.string.sort_by),
                selectedTitle = sortOptionTitle(state.selectedSortOption),
                options = sortOptions,
                indicatorText = stringResource(Res.string.dropdown_indicator),
                onSelected = onSortOptionSelected,
                onExpanded = { focusManager.clearFocus() },
            )
        }
    }
}

@Composable
private fun List<CatalogLanguage>.toLanguageOptions(): List<DropdownOption<CatalogLanguage>> = map { language ->
    DropdownOption(
        value = language,
        title = languageTitle(language),
    )
}

private fun List<CurrencyOption>.toCurrencyOptions(): List<DropdownOption<CurrencyOption>> = map { currency ->
    DropdownOption(
        value = currency,
        title = currencyTitle(currency),
    )
}

@Composable
private fun List<SortOption>.toSortOptions(): List<DropdownOption<SortOption>> = map { sortOption ->
    DropdownOption(
        value = sortOption,
        title = sortOptionTitle(sortOption),
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CategoryChipsSection(
    categories: List<ProductCategory>,
    selectedCategory: ProductCategory,
    onCategorySelected: (ProductCategory) -> Unit,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = stringResource(Res.string.category),
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            categories.forEach { category ->
                CategoryChip(
                    title = categoryTitle(category),
                    isSelected = category == selectedCategory,
                    onClick = { onCategorySelected(category) },
                )
            }
        }
    }
}

@Composable
private fun CategoryChip(
    title: String,
    isSelected: Boolean,
    onClick: () -> Unit,
) {
    Surface(
        shape = RoundedCornerShape(999.dp),
        color = if (isSelected) {
            MaterialTheme.colorScheme.primary
        } else {
            MaterialTheme.colorScheme.surfaceVariant
        },
        border = BorderStroke(
            width = 1.dp,
            color = if (isSelected) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.outline
            },
        ),
        modifier = Modifier.clickable(onClick = onClick),
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelLarge,
            color = if (isSelected) {
                MaterialTheme.colorScheme.onPrimary
            } else {
                MaterialTheme.colorScheme.onSurface
            },
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
        )
    }
}

@Suppress("LongMethod")
@Composable
private fun <T> CatalogDropdown(
    title: String,
    selectedTitle: String,
    options: List<DropdownOption<T>>,
    indicatorText: String,
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
                        text = indicatorText,
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
                            Text(text = option.title)
                        },
                        onClick = {
                            onSelected(option.value)
                            expanded = false
                        },
                    )
                }
            }
        }
    }
}

private data class DropdownOption<T>(
    val value: T,
    val title: String,
)
