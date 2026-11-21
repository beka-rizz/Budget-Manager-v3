package com.example.pocketbudget.ui.home

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import com.example.pocketbudget.R
import com.example.pocketbudget.data.Category
import com.example.pocketbudget.data.Transaction
import com.example.pocketbudget.ui.MessageViewModel
import com.example.pocketbudget.ui.activityViewModel
import com.example.pocketbudget.ui.components.BalanceSummaryCard
import com.example.pocketbudget.ui.components.BudgetScaffold
import com.example.pocketbudget.ui.components.CategoryChipRow
import com.example.pocketbudget.ui.components.ConfirmDeleteDialog
import com.example.pocketbudget.ui.components.EmptyState
import com.example.pocketbudget.ui.components.LoadingState
import com.example.pocketbudget.ui.components.TransactionCard
import com.example.pocketbudget.ui.navigation.Routes

@Composable
fun HomeRoute(
    navController: NavHostController,
    messages: MessageViewModel,
    bottomBar: @Composable () -> Unit,
) {
    val viewModel = activityViewModel { container ->
        HomeViewModel(container.repository, messages)
    }
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var pendingDeleteId by rememberSaveable { mutableLongStateOf(-1L) }
    val pending = uiState.transactions.firstOrNull { it.id == pendingDeleteId }

    HomeScreen(
        uiState = uiState,
        onSearchQueryChange = viewModel::onSearchQueryChange,
        onCategorySelected = viewModel::onCategorySelected,
        onClearFilters = viewModel::clearFilters,
        onTransactionClick = { transaction ->
            navController.navigate(Routes.transactionDetail(transaction.id))
        },
        onDeleteTransaction = { transaction -> pendingDeleteId = transaction.id },
        onAddClick = { navController.navigate(Routes.TRANSACTION_NEW) },
        bottomBar = bottomBar,
    )

    if (pending != null) {
        val goalName = uiState.goalNamesByTransactionId[pending.id]
        ConfirmDeleteDialog(
            title = "Remove this transaction?",
            body = if (goalName == null) {
                "Remove ${pending.title}? You can undo it from the message that appears next."
            } else {
                "${pending.title} is part of the $goalName goal. Removing it also updates that goal. You can undo it next."
            },
            confirmLabel = "Remove",
            onConfirm = {
                val id = pending.id
                pendingDeleteId = -1L
                viewModel.deleteTransaction(id)
            },
            onDismiss = { pendingDeleteId = -1L },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    uiState: HomeUiState,
    onSearchQueryChange: (String) -> Unit,
    onCategorySelected: (Category?) -> Unit,
    onClearFilters: () -> Unit,
    onTransactionClick: (Transaction) -> Unit,
    onDeleteTransaction: (Transaction) -> Unit,
    onAddClick: () -> Unit,
    modifier: Modifier = Modifier,
    bottomBar: @Composable () -> Unit = {},
) {
    val listState = rememberSaveable(saver = LazyListState.Saver) { LazyListState() }
    val focusManager = LocalFocusManager.current
    val layoutDirection = LocalLayoutDirection.current

    BudgetScaffold(
        title = "Pocket Budget",
        navigationIcon = {
            Image(
                painter = painterResource(R.drawable.ic_mark),
                contentDescription = null,
                modifier = Modifier
                    .padding(start = 12.dp)
                    .size(40.dp),
            )
        },
        bottomBar = bottomBar,
        floatingActionButton = {
            ExtendedFloatingActionButton(onClick = onAddClick) {
                Icon(Icons.Filled.Add, contentDescription = null)
                Text(text = "Add", modifier = Modifier.padding(start = 8.dp))
            }
        },
    ) { innerPadding ->
        if (uiState.isLoading) {
            LoadingState(Modifier.padding(innerPadding))
        } else {
        Box(
            modifier = modifier.fillMaxSize(),
            contentAlignment = Alignment.TopCenter,
        ) {
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .widthIn(max = 720.dp)
                    .fillMaxHeight(),
                contentPadding = PaddingValues(
                    start = 16.dp + innerPadding.calculateStartPadding(layoutDirection),
                    end = 16.dp + innerPadding.calculateEndPadding(layoutDirection),
                    top = innerPadding.calculateTopPadding() + 8.dp,
                    bottom = innerPadding.calculateBottomPadding() + 88.dp,
                ),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                item(key = "summary") {
                    BalanceSummaryCard(
                        balance = uiState.balance,
                        income = uiState.totalIncome,
                        expense = uiState.totalExpense,
                        categorySums = uiState.categorySums,
                        filtered = uiState.hasActiveFilter,
                        setAside = uiState.setAside,
                    )
                }
                item(key = "search") {
                    OutlinedTextField(
                        value = uiState.searchQuery,
                        onValueChange = onSearchQueryChange,
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        label = { Text("Search by title") },
                        leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                        trailingIcon = {
                            if (uiState.searchQuery.isNotEmpty()) {
                                IconButton(onClick = { onSearchQueryChange("") }) {
                                    Icon(Icons.Filled.Close, contentDescription = "Clear search")
                                }
                            }
                        },
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                        keyboardActions = KeyboardActions(onSearch = { focusManager.clearFocus() }),
                        shape = RoundedCornerShape(16.dp),
                    )
                }
                item(key = "categories") {
                    CategoryChipRow(
                        selected = uiState.selectedCategory,
                        onCategorySelected = onCategorySelected,
                    )
                }
                item(key = "heading") {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(text = "Transactions", style = MaterialTheme.typography.titleMedium)
                            Text(
                                text = transactionCountLabel(uiState),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        if (uiState.hasActiveFilter) {
                            TextButton(onClick = onClearFilters) { Text("Clear") }
                        }
                    }
                }
                if (uiState.transactions.isEmpty()) {
                    item(key = "empty") {
                        EmptyState(
                            title = if (uiState.sourceCount > 0) "Nothing matches" else "No transactions yet",
                            body = if (uiState.sourceCount > 0) {
                                "No transactions match this search or category."
                            } else {
                                "Add an income or an expense to start the ledger."
                            },
                            actionLabel = if (uiState.sourceCount > 0) "Clear filters" else null,
                            onAction = if (uiState.sourceCount > 0) onClearFilters else null,
                        )
                    }
                } else {
                    items(
                        items = uiState.transactions,
                        key = { it.id },
                    ) { transaction ->
                        TransactionCard(
                            transaction = transaction,
                            goalName = uiState.goalNamesByTransactionId[transaction.id],
                            onClick = { onTransactionClick(transaction) },
                            onDelete = { onDeleteTransaction(transaction) },
                        )
                    }
                }
            }
        }
        }
    }
}

private fun transactionCountLabel(uiState: HomeUiState): String {
    val visible = uiState.transactions.size
    return if (uiState.hasActiveFilter) {
        "$visible of ${uiState.sourceCount}"
    } else if (visible == 1) {
        "1 transaction"
    } else {
        "$visible transactions"
    }
}
