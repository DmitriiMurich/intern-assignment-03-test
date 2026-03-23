package com.artem.korenyakin.internassignment03.feature.catalog.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.artem.korenyakin.internassignment03.model.domain.Product

@Composable
internal fun ProductCard(
    product: Product,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
        ) {
            Text(
                text = product.title,
                style = MaterialTheme.typography.titleMedium,
            )
            Text(
                text = product.description,
                style = MaterialTheme.typography.bodyMedium,
            )
            Text(
                text = "Категория: ${product.category.title}",
                style = MaterialTheme.typography.bodySmall,
            )
            Text(
                text = "Цена: ${product.price}",
                style = MaterialTheme.typography.bodySmall,
            )
            Text(
                text = "Рейтинг: ${product.rating}",
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
}
