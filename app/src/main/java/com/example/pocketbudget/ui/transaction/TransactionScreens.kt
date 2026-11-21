package com.example.pocketbudget.ui.transaction

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation.NavHostController
import com.example.pocketbudget.data.TransactionType
import com.example.pocketbudget.di.LocalAppContainer
import com.example.pocketbudget.ui.MessageViewModel
import com.example.pocketbudget.ui.activityViewModel
import com.example.pocketbudget.ui.components.AmountField
import com.example.pocketbudget.ui.components.BudgetScaffold
import com.example.pocketbudget.ui.components.CategoryChipRow
import com.example.pocketbudget.ui.components.ConfirmDeleteDialog
import com.example.pocketbudget.ui.components.EmptyState
import com.example.pocketbudget.ui.components.LoadingState
import com.example.pocketbudget.ui.format.formatExactDateTime
import com.example.pocketbudget.ui.format.formatMoney
import com.example.pocketbudget.ui.format.formatTransactionDate
import com.example.pocketbudget.ui.home.HomeViewModel
import com.example.pocketbudget.ui.navigation.Routes
import com.example.pocketbudget.ui.theme.LocalBudgetColors
import com.example.pocketbudget.ui.theme.accent
import com.example.pocketbudget.ui.theme.iconRes

@Composable
fun TransactionDetailRoute(
    transactionId: Long,
    navController: NavHostController,
    messages: MessageViewModel,
) {
    val container = LocalAppContainer.current
    val viewModel: TransactionDetailViewModel = viewModel(
        factory = viewModelFactory {
            initializer {
                TransactionDetailViewModel(container.repository, messages, transactionId)
            }
        },
    )
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var showDelete by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(viewModel) {
        viewModel.deleted.collect { navController.popBackStack() }
    }

    val ready = uiState as? TransactionDetailUiState.Ready
    BudgetScaffold(
        title = "Transaction",
        onBack = { navController.popBackStack() },
        actions = {
            if (ready != null) {
                IconButton(onClick = { navController.navigate(Routes.transactionEdit(transactionId)) }) {
                    Icon(Icons.Filled.Edit, contentDescription = "Edit transaction")
                }
                IconButton(onClick = { showDelete = true }) {
                    Icon(Icons.Filled.Delete, contentDescription = "Delete transaction")
                }
            }
        },
    ) { padding ->
        when (val state = uiState) {
            TransactionDetailUiState.Loading -> LoadingState(Modifier.padding(padding))
            TransactionDetailUiState.Missing -> EmptyState(
                title = "This transaction is gone",
                body = "It may have been removed from another screen.",
                modifier = Modifier
                    .padding(padding)
                    .fillMaxSize(),
                actionLabel = "Back",
                onAction = { navController.popBackStack() },
            )
            is TransactionDetailUiState.Ready -> TransactionDetailContent(
                state = state,
                contentPadding = padding,
                onOpenGoal = { goalId -> navController.navigate(Routes.goalDetail(goalId)) },
            )
        }
    }

    val transaction = ready?.transaction
    if (showDelete && transaction != null) {
        val goalName = ready.goalLink?.goalName
        ConfirmDeleteDialog(
            title = "Remove this transaction?",
            body = if (goalName == null) {
                "Remove ${transaction.title}? You can undo it from the message that appears next."
            } else {
                "${transaction.title} is part of the $goalName goal. Removing it also updates that goal. You can undo it next."
            },
            confirmLabel = "Remove",
            onConfirm = {
                showDelete = false
                viewModel.delete()
            },
            onDismiss = { showDelete = false },
        )
    }
}

@Composable
private fun TransactionDetailContent(
    state: TransactionDetailUiState.Ready,
    contentPadding: PaddingValues,
    onOpenGoal: (Long) -> Unit,
) {
    val transaction = state.transaction
    val budgetColors = LocalBudgetColors.current
    val isIncome = transaction.type == TransactionType.INCOME
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(contentPadding)
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Box(
            modifier = Modifier
                .size(72.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(transaction.category.accent().copy(alpha = 0.16f)),
            contentAlignment = Alignment.Center,
        ) {
            Image(
                painter = painterResource(transaction.category.iconRes()),
                contentDescription = null,
                modifier = Modifier.size(36.dp),
                colorFilter = ColorFilter.tint(transaction.category.accent()),
            )
        }
        Text(text = transaction.title, style = MaterialTheme.typography.headlineMedium)
        Text(
            text = if (isIncome) {
                formatMoney(transaction.amount, withSign = true)
            } else {
                formatMoney(-transaction.amount)
            },
            style = MaterialTheme.typography.headlineMedium,
            color = if (isIncome) budgetColors.income else budgetColors.expense,
        )
        Text(
            text = "${transaction.type.name.lowercase().replaceFirstChar { it.uppercase() }} · ${transaction.category.label}",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = "${formatTransactionDate(transaction.createdAtMillis)} · ${formatExactDateTime(transaction.createdAtMillis)}",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        val link = state.goalLink
        if (link != null) {
            TextButton(onClick = { onOpenGoal(link.goalId) }) {
                Text("Part of the ${link.goalName} goal")
            }
        }
    }
}

@Composable
fun TransactionFormRoute(
    transactionId: Long?,
    navController: NavHostController,
    messages: MessageViewModel,
) {
    val container = LocalAppContainer.current
    val home = activityViewModel { HomeViewModel(it.repository, messages) }
    val viewModel: TransactionFormViewModel = viewModel(
        factory = viewModelFactory {
            initializer {
                TransactionFormViewModel(
                    repository = container.repository,
                    messages = messages,
                    savedStateHandle = createSavedStateHandle(),
                    transactionId = transactionId,
                )
            }
        },
    )
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(viewModel) {
        viewModel.saved.collect { transaction ->
            home.onTransactionSaved(transaction)
            navController.popBackStack()
        }
    }

    TransactionFormScreen(
        state = uiState,
        onBack = { navController.popBackStack() },
        onTitleChange = viewModel::onTitleChange,
        onAmountChange = viewModel::onAmountChange,
        onTypeChange = viewModel::onTypeChange,
        onCategoryChange = viewModel::onCategoryChange,
        onSave = viewModel::save,
    )
}

@Composable
fun TransactionFormScreen(
    state: TransactionFormState,
    onBack: () -> Unit,
    onTitleChange: (String) -> Unit,
    onAmountChange: (String) -> Unit,
    onTypeChange: (TransactionType) -> Unit,
    onCategoryChange: (com.example.pocketbudget.data.Category) -> Unit,
    onSave: () -> Unit,
) {
    val budgetColors = LocalBudgetColors.current
    BudgetScaffold(
        title = if (state.isEditing) "Edit transaction" else "New transaction",
        onBack = onBack,
    ) { padding ->
        when {
            state.missing -> EmptyState(
                title = "This transaction is gone",
                body = "It was removed before the edit could be saved.",
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
                if (state.moneyLocked) {
                    Text(
                        text = "Amount and type come from the ${state.goalName ?: "linked"} goal. Change them from that goal.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Start,
                    )
                }
                Text(
                    text = "Type",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    SegmentedButton(
                        selected = state.type == TransactionType.INCOME,
                        onClick = { onTypeChange(TransactionType.INCOME) },
                        enabled = !state.moneyLocked,
                        shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
                        colors = SegmentedButtonDefaults.colors(
                            activeContainerColor = budgetColors.incomeContainer,
                            activeContentColor = budgetColors.income,
                        ),
                    ) { Text("Income") }
                    SegmentedButton(
                        selected = state.type == TransactionType.EXPENSE,
                        onClick = { onTypeChange(TransactionType.EXPENSE) },
                        enabled = !state.moneyLocked,
                        shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
                        colors = SegmentedButtonDefaults.colors(
                            activeContainerColor = budgetColors.expenseContainer,
                            activeContentColor = budgetColors.expense,
                        ),
                    ) { Text("Expense") }
                }
                AmountField(
                    value = state.amountText,
                    onValueChange = onAmountChange,
                    enabled = !state.moneyLocked,
                    isError = !state.amountError.isNullOrBlank(),
                    supportingText = state.amountError,
                )
                OutlinedTextField(
                    value = state.title,
                    onValueChange = onTitleChange,
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    isError = !state.titleError.isNullOrBlank(),
                    label = { Text("Title") },
                    supportingText = state.titleError?.let { error -> { Text(error) } },
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { onSave() }),
                    shape = RoundedCornerShape(16.dp),
                )
                Text(
                    text = "Category",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                CategoryChipRow(
                    selected = state.category,
                    onCategorySelected = { selected ->
                        if (selected != null) onCategoryChange(selected)
                    },
                    includeAll = false,
                )
                Button(
                    onClick = onSave,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = RoundedCornerShape(16.dp),
                ) {
                    Text(if (state.isEditing) "Save changes" else "Add transaction")
                }
            }
        }
    }
}
