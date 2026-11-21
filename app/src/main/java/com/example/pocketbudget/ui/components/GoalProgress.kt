package com.example.pocketbudget.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.example.pocketbudget.data.GoalStatus
import com.example.pocketbudget.ui.theme.LocalBudgetColors

@Composable
fun GoalProgressBar(
    fraction: Float,
    modifier: Modifier = Modifier,
) {
    LinearProgressIndicator(
        progress = { fraction },
        modifier = modifier.clip(RoundedCornerShape(99.dp)),
    )
}

@Composable
fun GoalStatusBadge(
    status: GoalStatus,
    modifier: Modifier = Modifier,
) {
    val budgetColors = LocalBudgetColors.current
    val container = when (status) {
        GoalStatus.ACTIVE -> budgetColors.incomeContainer
        GoalStatus.PAUSED -> MaterialTheme.colorScheme.tertiaryContainer
        GoalStatus.COMPLETED -> MaterialTheme.colorScheme.surfaceVariant
    }
    val content = when (status) {
        GoalStatus.ACTIVE -> budgetColors.income
        GoalStatus.PAUSED -> MaterialTheme.colorScheme.onTertiaryContainer
        GoalStatus.COMPLETED -> MaterialTheme.colorScheme.onSurfaceVariant
    }
    Text(
        text = status.label,
        modifier = modifier
            .clip(RoundedCornerShape(50))
            .background(container)
            .padding(horizontal = 10.dp, vertical = 4.dp),
        color = content,
        style = MaterialTheme.typography.labelLarge,
    )
}
