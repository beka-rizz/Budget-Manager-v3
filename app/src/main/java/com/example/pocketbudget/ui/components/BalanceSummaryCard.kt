package com.example.pocketbudget.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.pocketbudget.data.Category
import com.example.pocketbudget.domain.CategorySum
import com.example.pocketbudget.ui.format.formatMoney
import com.example.pocketbudget.ui.theme.LocalBudgetColors
import com.example.pocketbudget.ui.theme.accent
import com.example.pocketbudget.ui.theme.iconRes

@Composable
fun BalanceSummaryCard(
    balance: Double,
    income: Double,
    expense: Double,
    categorySums: List<CategorySum>,
    filtered: Boolean,
    setAside: Double,
    modifier: Modifier = Modifier,
) {
    val budgetColors = LocalBudgetColors.current
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = if (filtered) "Filtered balance" else "Balance",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = formatMoney(balance),
                    style = MaterialTheme.typography.headlineMedium,
                    color = if (balance < 0) budgetColors.expense else MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = "Income minus expenses",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                MetricTile(
                    label = "Income",
                    value = formatMoney(income, withSign = true),
                    container = budgetColors.incomeContainer,
                    content = budgetColors.income,
                    modifier = Modifier.weight(1f),
                )
                MetricTile(
                    label = "Expenses",
                    value = formatMoney(-expense),
                    container = budgetColors.expenseContainer,
                    content = budgetColors.expense,
                    modifier = Modifier.weight(1f),
                )
            }
            if (!filtered && setAside > 0.0) {
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = "Set aside in goals",
                            style = MaterialTheme.typography.bodyLarge,
                        )
                        Text(
                            text = formatMoney(setAside),
                            style = MaterialTheme.typography.titleMedium,
                        )
                    }
                    Text(
                        text = "This money is already included in expenses.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            if (categorySums.isNotEmpty()) {
                HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "By category",
                        style = MaterialTheme.typography.titleMedium,
                    )
                    categorySums.forEach { sum ->
                        CategorySumRow(sum = sum)
                    }
                }
            }
        }
    }
}

@Composable
private fun MetricTile(
    label: String,
    value: String,
    container: Color,
    content: Color,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(container)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(text = label, style = MaterialTheme.typography.labelLarge, color = content)
        Text(text = value, style = MaterialTheme.typography.titleMedium, color = content)
    }
}

@Composable
private fun CategorySumRow(sum: CategorySum) {
    val budgetColors = LocalBudgetColors.current
    val net = sum.income - sum.expense
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(sum.category.accent().copy(alpha = 0.16f)),
            contentAlignment = Alignment.Center,
        ) {
            Image(
                painter = painterResource(sum.category.iconRes()),
                contentDescription = null,
                modifier = Modifier.size(18.dp),
                colorFilter = ColorFilter.tint(sum.category.accent()),
            )
        }
        Text(
            text = sum.category.label,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.bodyLarge,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            text = formatMoney(net, withSign = true),
            style = MaterialTheme.typography.titleMedium,
            color = if (net < 0) budgetColors.expense else budgetColors.income,
        )
    }
}
