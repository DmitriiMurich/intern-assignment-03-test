package com.artem.korenyakin.internassignment03.feature.catalog.components

import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
internal fun SearchBar(
    query: String,
    onQueryChanged: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChanged,
        modifier = modifier,
        label = {
            Text(text = "Поиск")
        },
        placeholder = {
            Text(text = "Поиск будет реализован следующим этапом")
        },
        singleLine = true,
    )
}
