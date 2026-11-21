package com.example.pocketbudget.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.pocketbudget.data.Category
import com.example.pocketbudget.data.Transaction
import com.example.pocketbudget.data.repository.BudgetRepository
import com.example.pocketbudget.domain.BudgetCalculator
import com.example.pocketbudget.domain.CategorySum
import com.example.pocketbudget.domain.sumMoney
import com.example.pocketbudget.ui.MessageViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class HomeUiState(
    val isLoading: Boolean = true,
    val searchQuery: String = "",
    val selectedCategory: Category? = null,
    val transactions: List<Transaction> = emptyList(),
    val sourceCount: Int = 0,
    val balance: Double = 0.0,
    val totalIncome: Double = 0.0,
    val totalExpense: Double = 0.0,
    val categorySums: List<CategorySum> = emptyList(),
    val setAside: Double = 0.0,
    val goalNamesByTransactionId: Map<Long, String> = emptyMap(),
) {
    val hasActiveFilter: Boolean
        get() = selectedCategory != null || searchQuery.isNotBlank()
}

class HomeViewModel(
    private val repository: BudgetRepository,
    private val messages: MessageViewModel,
) : ViewModel() {
    private val searchQuery = MutableStateFlow("")
    private val selectedCategory = MutableStateFlow<Category?>(null)

    val uiState: StateFlow<HomeUiState> = combine(
        repository.observeTransactions(),
        repository.observeGoalDirectory(),
        searchQuery,
        selectedCategory,
    ) { transactions, directory, query, category ->
        val budget = BudgetCalculator.build(transactions, query, category)
        HomeUiState(
            isLoading = false,
            searchQuery = budget.searchQuery,
            selectedCategory = budget.selectedCategory,
            transactions = budget.transactions,
            sourceCount = budget.sourceCount,
            balance = budget.balance,
            totalIncome = budget.totalIncome,
            totalExpense = budget.totalExpense,
            categorySums = budget.categorySums,
            setAside = sumMoney(directory.goals.map { it.saved }),
            goalNamesByTransactionId = directory.goalNameByTransactionId,
        )
    }.stateIn(viewModelScope, SharingStarted.Eagerly, HomeUiState())

    fun onSearchQueryChange(query: String) {
        searchQuery.value = query
    }

    fun onCategorySelected(category: Category?) {
        selectedCategory.value = category
    }

    fun clearFilters() {
        searchQuery.value = ""
        selectedCategory.value = null
    }

    fun onTransactionSaved(transaction: Transaction) {
        val query = searchQuery.value.trim()
        if (query.isNotEmpty() && !transaction.title.contains(query, ignoreCase = true)) {
            searchQuery.value = ""
        }
        val category = selectedCategory.value
        if (category != null && category != transaction.category) {
            selectedCategory.value = null
        }
    }

    fun deleteTransaction(id: Long) {
        viewModelScope.launch {
            val deletion = repository.deleteTransaction(id) ?: return@launch
            messages.show(
                text = "Removed ${deletion.transaction.title}",
                actionLabel = "Undo",
                deletion = deletion,
            )
        }
    }
}
