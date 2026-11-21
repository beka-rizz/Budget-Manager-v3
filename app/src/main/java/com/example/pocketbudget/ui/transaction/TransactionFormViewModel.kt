package com.example.pocketbudget.ui.transaction

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.pocketbudget.data.Category
import com.example.pocketbudget.data.Transaction
import com.example.pocketbudget.data.TransactionType
import com.example.pocketbudget.data.repository.BudgetRepository
import com.example.pocketbudget.data.repository.TransactionWriteResult
import com.example.pocketbudget.ui.MessageViewModel
import com.example.pocketbudget.ui.format.formatAmountInput
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class TransactionFormState(
    val isEditing: Boolean = false,
    val ready: Boolean = false,
    val missing: Boolean = false,
    val title: String = "",
    val amountText: String = "",
    val type: TransactionType = TransactionType.EXPENSE,
    val category: Category = Category.FOOD,
    val titleError: String? = null,
    val amountError: String? = null,
    val moneyLocked: Boolean = false,
    val goalName: String? = null,
)

class TransactionFormViewModel(
    private val repository: BudgetRepository,
    private val messages: MessageViewModel,
    private val savedStateHandle: SavedStateHandle,
    private val transactionId: Long?,
) : ViewModel() {
    private val _state = MutableStateFlow(readState())
    val uiState: StateFlow<TransactionFormState> = _state.asStateFlow()
    private val _saved = MutableSharedFlow<Transaction>(extraBufferCapacity = 1)
    val saved = _saved.asSharedFlow()

    init {
        if (transactionId == null) {
            if (savedStateHandle.get<Boolean>(KEY_READY) != true) {
                savedStateHandle[KEY_READY] = true
                savedStateHandle[KEY_TYPE] = TransactionType.EXPENSE.name
                savedStateHandle[KEY_CATEGORY] = Category.FOOD.name
            }
            _state.value = readState()
        } else if (savedStateHandle.get<Boolean>(KEY_READY) != true) {
            viewModelScope.launch { load() }
        }
    }

    fun onTitleChange(value: String) {
        savedStateHandle[KEY_TITLE] = value
        _state.value = _state.value.copy(title = value, titleError = null)
    }

    fun onAmountChange(value: String) {
        savedStateHandle[KEY_AMOUNT] = value
        _state.value = _state.value.copy(amountText = value, amountError = null)
    }

    fun onTypeChange(value: TransactionType) {
        if (_state.value.moneyLocked) return
        savedStateHandle[KEY_TYPE] = value.name
        _state.value = _state.value.copy(type = value)
    }

    fun onCategoryChange(value: Category) {
        savedStateHandle[KEY_CATEGORY] = value.name
        _state.value = _state.value.copy(category = value)
    }

    fun save() {
        val current = _state.value
        if (!current.ready || current.missing) return
        viewModelScope.launch {
            when (
                val result = repository.saveTransaction(
                    id = transactionId,
                    title = current.title,
                    amountText = current.amountText,
                    type = current.type,
                    category = current.category,
                )
            ) {
                is TransactionWriteResult.Invalid -> {
                    _state.value = current.copy(
                        titleError = result.titleError,
                        amountError = result.amountError,
                    )
                }
                TransactionWriteResult.Missing -> {
                    savedStateHandle[KEY_MISSING] = true
                    _state.value = current.copy(missing = true)
                }
                is TransactionWriteResult.Saved -> {
                    val verb = if (transactionId == null) "Added" else "Updated"
                    messages.show("$verb ${result.transaction.title}")
                    _saved.emit(result.transaction)
                }
            }
        }
    }

    private suspend fun load() {
        val transaction = repository.getTransaction(transactionId ?: return)
        if (transaction == null) {
            savedStateHandle[KEY_MISSING] = true
            savedStateHandle[KEY_READY] = true
            _state.value = readState()
            return
        }
        val link = repository.getTransactionLink(transaction.id)
        savedStateHandle[KEY_TITLE] = transaction.title
        savedStateHandle[KEY_AMOUNT] = formatAmountInput(transaction.amount)
        savedStateHandle[KEY_TYPE] = transaction.type.name
        savedStateHandle[KEY_CATEGORY] = transaction.category.name
        savedStateHandle[KEY_LOCKED] = link != null
        if (link != null) savedStateHandle[KEY_GOAL_NAME] = link.goalName
        savedStateHandle[KEY_READY] = true
        _state.value = readState()
    }

    private fun readState(): TransactionFormState {
        val ready = savedStateHandle.get<Boolean>(KEY_READY) == true
        return TransactionFormState(
            isEditing = transactionId != null,
            ready = ready,
            missing = savedStateHandle.get<Boolean>(KEY_MISSING) == true,
            title = savedStateHandle.get<String>(KEY_TITLE).orEmpty(),
            amountText = savedStateHandle.get<String>(KEY_AMOUNT).orEmpty(),
            type = savedStateHandle.get<String>(KEY_TYPE)
                ?.let { runCatching { TransactionType.valueOf(it) }.getOrNull() }
                ?: TransactionType.EXPENSE,
            category = savedStateHandle.get<String>(KEY_CATEGORY)
                ?.let { runCatching { Category.valueOf(it) }.getOrNull() }
                ?: Category.FOOD,
            moneyLocked = savedStateHandle.get<Boolean>(KEY_LOCKED) == true,
            goalName = savedStateHandle.get<String>(KEY_GOAL_NAME),
        )
    }

    private companion object {
        const val KEY_READY = "tx_ready"
        const val KEY_MISSING = "tx_missing"
        const val KEY_TITLE = "tx_title"
        const val KEY_AMOUNT = "tx_amount"
        const val KEY_TYPE = "tx_type"
        const val KEY_CATEGORY = "tx_category"
        const val KEY_LOCKED = "tx_locked"
        const val KEY_GOAL_NAME = "tx_goal_name"
    }
}
