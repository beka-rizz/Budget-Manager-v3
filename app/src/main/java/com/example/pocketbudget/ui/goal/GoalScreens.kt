package com.example.pocketbudget.ui.goal

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation.NavHostController
import com.example.pocketbudget.data.Category
import com.example.pocketbudget.data.Contribution
import com.example.pocketbudget.data.ContributionKind
import com.example.pocketbudget.di.LocalAppContainer
import com.example.pocketbudget.domain.GoalWithProgress
import com.example.pocketbudget.domain.percentLabel
import com.example.pocketbudget.domain.toCents
import com.example.pocketbudget.ui.MessageViewModel
import com.example.pocketbudget.ui.components.AmountField
import com.example.pocketbudget.ui.components.BudgetScaffold
import com.example.pocketbudget.ui.components.CategoryChipRow
import com.example.pocketbudget.ui.components.ConfirmDeleteDialog
import com.example.pocketbudget.ui.components.EmptyState
import com.example.pocketbudget.ui.components.GoalProgressBar
import com.example.pocketbudget.ui.components.GoalStatusBadge
import com.example.pocketbudget.ui.components.LoadingState
import com.example.pocketbudget.ui.format.formatExactDateTime
import com.example.pocketbudget.ui.format.formatMoney
import com.example.pocketbudget.ui.format.formatTransactionDate
import com.example.pocketbudget.ui.navigation.Routes
import com.example.pocketbudget.ui.theme.LocalBudgetColors
import com.example.pocketbudget.ui.theme.accent
import com.example.pocketbudget.ui.theme.iconRes

@Composable
fun GoalsRoute(
    navController: NavHostController,
    bottomBar: @Composable () -> Unit,
) {
    val container = LocalAppContainer.current
    val viewModel: GoalsViewModel = viewModel(
        factory = viewModelFactory {
            initializer { GoalsViewModel(container.repository) }
        },
    )
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    GoalsScreen(
        uiState = uiState,
        onGoalClick = { navController.navigate(Routes.goalDetail(it)) },
        onAddClick = { navController.navigate(Routes.GOAL_NEW) },
        bottomBar = bottomBar,
    )
}

@Composable
fun GoalsScreen(
    uiState: GoalsUiState,
    onGoalClick: (Long) -> Unit,
    onAddClick: () -> Unit,
    bottomBar: @Composable () -> Unit = {},
) {
    val layoutDirection = LocalLayoutDirection.current
    BudgetScaffold(
        title = "Savings goals",
        bottomBar = bottomBar,
        floatingActionButton = {
            ExtendedFloatingActionButton(onClick = onAddClick) {
                Icon(Icons.Filled.Add, contentDescription = null)
                Text("New goal", modifier = Modifier.padding(start = 8.dp))
            }
        },
    ) { innerPadding ->
        if (uiState.isLoading) {
            LoadingState(Modifier.padding(innerPadding))
        } else {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 720.dp),
            contentPadding = PaddingValues(
                start = 16.dp + innerPadding.calculateStartPadding(layoutDirection),
                end = 16.dp + innerPadding.calculateEndPadding(layoutDirection),
                top = innerPadding.calculateTopPadding() + 8.dp,
                bottom = innerPadding.calculateBottomPadding() + 88.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (uiState.goals.isEmpty()) {
                item(key = "empty") {
                    EmptyState(
                        title = "No savings goals yet",
                        body = "Create a goal, then move money into it. Every deposit and withdrawal is also written to the ledger.",
                        actionLabel = "New goal",
                        onAction = onAddClick,
                    )
                }
            } else {
                item(key = "stats") { GoalStatsCard(uiState) }
                items(uiState.goals, key = { it.goal.id }) { progress ->
                    GoalCard(progress = progress, onClick = { onGoalClick(progress.goal.id) })
                }
            }
        }
        }
    }
}

@Composable
private fun GoalStatsCard(uiState: GoalsUiState) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest),
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = "Set aside",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(text = formatMoney(uiState.setAside), style = MaterialTheme.typography.headlineMedium)
            Text(
                text = "${uiState.activeCount} active · ${uiState.pausedCount} paused · ${uiState.completedCount} complete",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            val closest = uiState.closest
            if (closest != null) {
                Text(
                    text = "Closest to done: ${closest.goal.name} · ${percentLabel(closest.saved, closest.goal.targetAmount)}",
                    style = MaterialTheme.typography.bodyLarge,
                )
            }
        }
    }
}

@Composable
private fun GoalCard(
    progress: GoalWithProgress,
    onClick: () -> Unit,
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(progress.goal.category.accent().copy(alpha = 0.16f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Image(
                        painter = painterResource(progress.goal.category.iconRes()),
                        contentDescription = null,
                        modifier = Modifier.size(22.dp),
                        colorFilter = ColorFilter.tint(progress.goal.category.accent()),
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = progress.goal.name,
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = progress.goal.category.label,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                GoalStatusBadge(progress.goal.status)
            }
            Text(
                text = "${formatMoney(progress.saved)} of ${formatMoney(progress.goal.targetAmount)}",
                style = MaterialTheme.typography.bodyLarge,
            )
            GoalProgressBar(fraction = progress.fraction, modifier = Modifier.fillMaxWidth().height(8.dp))
            Text(
                text = "${percentLabel(progress.saved, progress.goal.targetAmount)} · ${formatMoney(progress.remaining)} to go",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
fun GoalDetailRoute(
    goalId: Long,
    navController: NavHostController,
    messages: MessageViewModel,
) {
    val container = LocalAppContainer.current
    val viewModel: GoalDetailViewModel = viewModel(
        factory = viewModelFactory {
            initializer { GoalDetailViewModel(container.repository, messages, goalId) }
        },
    )
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var showDelete by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(viewModel) {
        viewModel.closed.collect { navController.popBackStack() }
    }
    val ready = uiState as? GoalDetailUiState.Ready
    BudgetScaffold(
        title = ready?.details?.progress?.goal?.name ?: "Goal",
        onBack = { navController.popBackStack() },
        actions = {
            if (ready != null) {
                IconButton(onClick = { navController.navigate(Routes.goalEdit(goalId)) }) {
                    Icon(Icons.Filled.Edit, contentDescription = "Edit goal")
                }
                IconButton(onClick = { showDelete = true }) {
                    Icon(Icons.Filled.Delete, contentDescription = "Delete goal")
                }
            }
        },
    ) { padding ->
        when (val state = uiState) {
            GoalDetailUiState.Loading -> LoadingState(Modifier.padding(padding))
            GoalDetailUiState.Missing -> EmptyState(
                title = "This goal is gone",
                body = "It may have been deleted from another screen.",
                modifier = Modifier.padding(padding).fillMaxSize(),
                actionLabel = "Back",
                onAction = { navController.popBackStack() },
            )
            is GoalDetailUiState.Ready -> GoalDetailContent(
                state = state,
                contentPadding = padding,
                onDeposit = { navController.navigate(Routes.goalMove(goalId, "deposit")) },
                onWithdraw = { navController.navigate(Routes.goalMove(goalId, "withdraw")) },
                onPause = viewModel::pause,
                onResume = viewModel::resume,
                onComplete = viewModel::complete,
                onReopen = viewModel::reopen,
                onContributionClick = { navController.navigate(Routes.transactionDetail(it)) },
            )
        }
    }
    val progress = ready?.details?.progress
    if (showDelete && progress != null) {
        val holdsMoney = toCents(progress.saved) > 0
        ConfirmDeleteDialog(
            title = if (holdsMoney) "Release and delete?" else "Delete this goal?",
            body = if (holdsMoney) {
                "${progress.goal.name} still holds ${formatMoney(progress.saved)}. That money will return to your balance as income, and the goal history will be removed."
            } else {
                "Delete ${progress.goal.name}? Its history is removed. Ledger entries that already net to zero stay in the ledger."
            },
            confirmLabel = if (holdsMoney) "Release and delete" else "Delete",
            onConfirm = {
                showDelete = false
                viewModel.delete(releaseSavings = holdsMoney)
            },
            onDismiss = { showDelete = false },
        )
    }
}

@Composable
private fun GoalDetailContent(
    state: GoalDetailUiState.Ready,
    contentPadding: PaddingValues,
    onDeposit: () -> Unit,
    onWithdraw: () -> Unit,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onComplete: () -> Unit,
    onReopen: () -> Unit,
    onContributionClick: (Long) -> Unit,
) {
    val progress = state.details.progress
    val actions = state.details.actions
    val layoutDirection = LocalLayoutDirection.current
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = 16.dp + contentPadding.calculateStartPadding(layoutDirection),
            end = 16.dp + contentPadding.calculateEndPadding(layoutDirection),
            top = contentPadding.calculateTopPadding() + 8.dp,
            bottom = contentPadding.calculateBottomPadding() + 24.dp,
        ),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item(key = "summary") {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    GoalStatusBadge(progress.goal.status)
                    Text(progress.goal.category.label, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Text(
                    text = "${formatMoney(progress.saved)} of ${formatMoney(progress.goal.targetAmount)}",
                    style = MaterialTheme.typography.headlineMedium,
                )
                GoalProgressBar(progress.fraction, Modifier.fillMaxWidth().height(8.dp))
                Text(
                    text = "${percentLabel(progress.saved, progress.goal.targetAmount)} · ${formatMoney(progress.remaining)} still needed",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (progress.goal.note.isNotBlank()) {
                    Text(progress.goal.note, style = MaterialTheme.typography.bodyLarge)
                }
                progress.goal.completedAtMillis?.let { completedAt ->
                    Text(
                        text = "Completed ${formatExactDateTime(completedAt)}",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Text(
                    text = "Added ${formatMoney(progress.deposited)} · Taken out ${formatMoney(progress.withdrawn)}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(text = actions.hint, style = MaterialTheme.typography.bodyLarge)
            }
        }
        item(key = "actions") {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (actions.canDeposit) {
                    Button(onClick = onDeposit, modifier = Modifier.fillMaxWidth().height(52.dp)) {
                        Text("Add money")
                    }
                }
                if (actions.canWithdraw) {
                    OutlinedButton(onClick = onWithdraw, modifier = Modifier.fillMaxWidth().height(52.dp)) {
                        Text("Take money out")
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (actions.canPause) TextButton(onClick = onPause) { Text("Pause") }
                    if (actions.canResume) TextButton(onClick = onResume) { Text("Resume") }
                    if (actions.canComplete) TextButton(onClick = onComplete) { Text("Mark complete") }
                    if (actions.canReopen) TextButton(onClick = onReopen) { Text("Reopen") }
                }
            }
        }
        item(key = "history-title") {
            Text("History", style = MaterialTheme.typography.titleMedium)
        }
        if (state.details.contributions.isEmpty()) {
            item(key = "history-empty") {
                EmptyState(
                    title = "No money has moved yet",
                    body = "Add the first amount. It will show up here and on the ledger.",
                )
            }
        } else {
            items(state.details.contributions, key = { it.id }) { contribution ->
                ContributionRow(contribution, onClick = { onContributionClick(contribution.transactionId) })
            }
        }
    }
}

@Composable
private fun ContributionRow(
    contribution: Contribution,
    onClick: () -> Unit,
) {
    val budgetColors = LocalBudgetColors.current
    val added = contribution.kind == ContributionKind.DEPOSIT
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest),
    ) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(if (added) "Added" else "Taken out", style = MaterialTheme.typography.titleMedium)
                Text(
                    text = formatMoney(if (added) contribution.amount else -contribution.amount, withSign = true),
                    style = MaterialTheme.typography.titleMedium,
                    color = if (added) budgetColors.income else budgetColors.expense,
                )
            }
            Text(
                text = contribution.note.ifBlank { contribution.transactionTitle },
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = "${formatTransactionDate(contribution.createdAtMillis)} · ${contribution.transactionTitle}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
fun GoalFormRoute(
    goalId: Long?,
    navController: NavHostController,
    messages: MessageViewModel,
) {
    val container = LocalAppContainer.current
    val viewModel: GoalFormViewModel = viewModel(
        factory = viewModelFactory {
            initializer {
                GoalFormViewModel(
                    repository = container.repository,
                    messages = messages,
                    savedStateHandle = createSavedStateHandle(),
                    goalId = goalId,
                )
            }
        },
    )
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    LaunchedEffect(viewModel) {
        viewModel.saved.collect { navController.popBackStack() }
    }
    GoalFormScreen(
        state = state,
        onBack = { navController.popBackStack() },
        onNameChange = viewModel::onNameChange,
        onTargetChange = viewModel::onTargetChange,
        onNoteChange = viewModel::onNoteChange,
        onCategoryChange = viewModel::onCategoryChange,
        onSave = viewModel::save,
    )
}

@Composable
fun GoalFormScreen(
    state: GoalFormState,
    onBack: () -> Unit,
    onNameChange: (String) -> Unit,
    onTargetChange: (String) -> Unit,
    onNoteChange: (String) -> Unit,
    onCategoryChange: (Category) -> Unit,
    onSave: () -> Unit,
) {
    BudgetScaffold(
        title = if (state.isEditing) "Edit goal" else "New goal",
        onBack = onBack,
    ) { padding ->
        when {
            state.missing -> EmptyState(
                title = "This goal is gone",
                body = "It was deleted before the edit could be saved.",
                modifier = Modifier.padding(padding).fillMaxSize(),
                actionLabel = "Back",
                onAction = onBack,
            )
            !state.ready -> LoadingState(Modifier.padding(padding))
            else -> Column(
                modifier = Modifier
                    .fillMaxSize()
                    .imePadding()
                    .verticalScroll(rememberScrollState())
                    .padding(padding)
                    .padding(horizontal = 20.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                OutlinedTextField(
                    value = state.name,
                    onValueChange = onNameChange,
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    isError = !state.nameError.isNullOrBlank(),
                    label = { Text("Name") },
                    supportingText = state.nameError?.let { error -> { Text(error) } },
                    shape = RoundedCornerShape(16.dp),
                )
                AmountField(
                    value = state.targetText,
                    onValueChange = onTargetChange,
                    label = "Target",
                    isError = !state.targetError.isNullOrBlank(),
                    supportingText = state.targetError
                        ?: if (toCents(state.savedAmount) > 0) {
                            "Already saved: ${formatMoney(state.savedAmount)}"
                        } else {
                            "How much you want to set aside"
                        },
                )
                OutlinedTextField(
                    value = state.note,
                    onValueChange = onNoteChange,
                    modifier = Modifier.fillMaxWidth(),
                    isError = !state.noteError.isNullOrBlank(),
                    label = { Text("Note") },
                    supportingText = state.noteError?.let { error -> { Text(error) } },
                    minLines = 2,
                    shape = RoundedCornerShape(16.dp),
                )
                Text("Category", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                CategoryChipRow(
                    selected = state.category,
                    onCategorySelected = { selected -> if (selected != null) onCategoryChange(selected) },
                    includeAll = false,
                )
                Button(
                    onClick = onSave,
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    shape = RoundedCornerShape(16.dp),
                ) {
                    Text(if (state.isEditing) "Save changes" else "Create goal")
                }
            }
        }
    }
}

@Composable
fun GoalMoveRoute(
    goalId: Long,
    mode: String,
    navController: NavHostController,
    messages: MessageViewModel,
) {
    val container = LocalAppContainer.current
    val viewModel: GoalMoveViewModel = viewModel(
        factory = viewModelFactory {
            initializer {
                GoalMoveViewModel(
                    repository = container.repository,
                    messages = messages,
                    savedStateHandle = createSavedStateHandle(),
                    goalId = goalId,
                    initialMode = mode,
                )
            }
        },
    )
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    LaunchedEffect(viewModel) {
        viewModel.saved.collect { navController.popBackStack() }
    }
    GoalMoveScreen(
        state = state,
        onBack = { navController.popBackStack() },
        onKindChange = viewModel::onKindChange,
        onAmountChange = viewModel::onAmountChange,
        onNoteChange = viewModel::onNoteChange,
        onSave = viewModel::save,
    )
}

@Composable
fun GoalMoveScreen(
    state: GoalMoveUiState,
    onBack: () -> Unit,
    onKindChange: (ContributionKind) -> Unit,
    onAmountChange: (String) -> Unit,
    onNoteChange: (String) -> Unit,
    onSave: () -> Unit,
) {
    val title = (state as? GoalMoveUiState.Ready)?.goalName?.let { "Move money · $it" } ?: "Move money"
    BudgetScaffold(title = title, onBack = onBack) { padding ->
        when (state) {
            GoalMoveUiState.Loading -> LoadingState(Modifier.padding(padding))
            GoalMoveUiState.Missing -> EmptyState(
                title = "This goal is gone",
                body = "It was deleted before the money could be moved.",
                modifier = Modifier.padding(padding).fillMaxSize(),
                actionLabel = "Back",
                onAction = onBack,
            )
            is GoalMoveUiState.Ready -> Column(
                modifier = Modifier
                    .fillMaxSize()
                    .imePadding()
                    .verticalScroll(rememberScrollState())
                    .padding(padding)
                    .padding(horizontal = 20.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                    SegmentedButton(
                        selected = state.draft.kind == ContributionKind.DEPOSIT,
                        onClick = { onKindChange(ContributionKind.DEPOSIT) },
                        shape = SegmentedButtonDefaults.itemShape(0, 2),
                    ) { Text("Add") }
                    SegmentedButton(
                        selected = state.draft.kind == ContributionKind.WITHDRAWAL,
                        onClick = { onKindChange(ContributionKind.WITHDRAWAL) },
                        shape = SegmentedButtonDefaults.itemShape(1, 2),
                    ) { Text("Take out") }
                }
                Text(
                    text = if (state.draft.kind == ContributionKind.DEPOSIT) {
                        "${formatMoney(state.remaining)} still needed"
                    } else {
                        "${formatMoney(state.saved)} available to take out"
                    },
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                state.blockedReason?.let { reason ->
                    Text(reason, color = MaterialTheme.colorScheme.error)
                }
                AmountField(
                    value = state.draft.amountText,
                    onValueChange = onAmountChange,
                    enabled = state.blockedReason == null,
                    isError = !state.draft.amountError.isNullOrBlank(),
                    supportingText = state.draft.amountError,
                )
                OutlinedTextField(
                    value = state.draft.note,
                    onValueChange = onNoteChange,
                    modifier = Modifier.fillMaxWidth(),
                    isError = !state.draft.noteError.isNullOrBlank(),
                    label = { Text("Note") },
                    supportingText = {
                        Text(state.draft.noteError ?: "Optional. The ledger keeps its own title.")
                    },
                    shape = RoundedCornerShape(16.dp),
                )
                Button(
                    onClick = onSave,
                    enabled = state.blockedReason == null,
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    shape = RoundedCornerShape(16.dp),
                ) {
                    Text(if (state.draft.kind == ContributionKind.DEPOSIT) "Add to goal" else "Take out")
                }
            }
        }
    }
}
