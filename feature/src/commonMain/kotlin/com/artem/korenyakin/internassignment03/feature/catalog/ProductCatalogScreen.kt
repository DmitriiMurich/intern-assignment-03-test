package com.artem.korenyakin.internassignment03.feature.catalog

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.artem.korenyakin.internassignment03.feature.catalog.components.ProductCard
import com.artem.korenyakin.internassignment03.feature.catalog.components.SearchBar
import com.artem.korenyakin.internassignment03.model.domain.Product
import com.artem.korenyakin.internassignment03.model.domain.ProductCategory

@Composable
public fun ProductCatalogScreen(
    modifier: Modifier = Modifier,
) {
    val previewProduct = Product(
        id = "preview-1",
        title = "Инициализация проекта завершена",
        description = "Следующим шагом будет реализация каталога, фильтров и пагинации.",
        price = 0.0,
        imageUrl = "",
        rating = 5.0,
        category = ProductCategory.ELECTRONICS,
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        SearchBar(
            query = "",
            onQueryChanged = {},
            modifier = Modifier.fillMaxWidth(),
        )
        Text(
            text = "Базовая KMP-структура готова. Экран пока отображает placeholder UI.",
            style = MaterialTheme.typography.bodyMedium,
        )
        ProductCard(
            product = previewProduct,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}
