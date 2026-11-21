package com.example.pocketbudget.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.FilterChip
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.example.pocketbudget.data.Category
import com.example.pocketbudget.ui.theme.iconRes

@Composable
fun CategoryChipRow(
    selected: Category?,
    onCategorySelected: (Category?) -> Unit,
    modifier: Modifier = Modifier,
    includeAll: Boolean = true,
) {
    Row(
        modifier = modifier.horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        if (includeAll) {
            FilterChip(
                selected = selected == null,
                onClick = { onCategorySelected(null) },
                label = { Text("All") },
            )
        }
        Category.entries.forEach { category ->
            FilterChip(
                selected = selected == category,
                onClick = {
                    val next = if (includeAll && selected == category) null else category
                    onCategorySelected(next)
                },
                label = { Text(category.label) },
                leadingIcon = {
                    Image(
                        painter = painterResource(category.iconRes()),
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                        colorFilter = ColorFilter.tint(LocalContentColor.current),
                    )
                },
            )
        }
    }
}
