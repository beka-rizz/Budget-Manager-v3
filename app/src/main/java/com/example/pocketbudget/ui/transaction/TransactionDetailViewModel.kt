package com.example.pocketbudget.ui.transaction

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.pocketbudget.data.Transaction
import com.example.pocketbudget.data.TransactionGoalLink
import com.example.pocketbudget.data.repository.BudgetRepository
import com.example.pocketbudget.ui.MessageViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface TransactionDetailUiState {
    data object Loading : TransactionDetailUiState
    data object Missing : TransactionDetailUiState
    data class Ready(
        val transaction: Transaction,
        val goalLink: TransactionGoalLink?,
    ) : TransactionDetailUiState
}

class TransactionDetailViewModel(
    private val repository: BudgetRepository,
    private val messages: MessageViewModel,
    private val transactionId: Long,
) : ViewModel() {
    val uiState: StateFlow<TransactionDetailUiState> = combine(
        repository.observeTransaction(transactionId),
        repository.observeTransactionLink(transactionId),
    ) { transaction, link ->
        if (transaction == null) {
            TransactionDetailUiState.Missing
        } else {
            TransactionDetailUiState.Ready(transaction, link)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), TransactionDetailUiState.Loading)

    private val _deleted = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val deleted = _deleted.asSharedFlow()

    fun delete() {
        viewModelScope.launch {
            val deletion = repository.deleteTransaction(transactionId) ?: return@launch
            messages.show(
                text = "Removed ${deletion.transaction.title}",
                actionLabel = "Undo",
                deletion = deletion,
            )
            _deleted.emit(Unit)
        }
    }
}
