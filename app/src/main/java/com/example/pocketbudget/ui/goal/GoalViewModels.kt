package com.example.pocketbudget.ui.goal

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.pocketbudget.data.Category
import com.example.pocketbudget.data.ContributionKind
import com.example.pocketbudget.data.repository.BudgetRepository
import com.example.pocketbudget.data.repository.GoalActionResult
import com.example.pocketbudget.data.repository.GoalDeleteResult
import com.example.pocketbudget.data.repository.GoalWriteResult
import com.example.pocketbudget.data.GoalStatus
import com.example.pocketbudget.domain.GoalDetails
import com.example.pocketbudget.domain.GoalWithProgress
import com.example.pocketbudget.domain.moveBlockedReason
import com.example.pocketbudget.domain.sumMoney
import com.example.pocketbudget.ui.MessageViewModel
import com.example.pocketbudget.ui.format.formatAmountInput
import com.example.pocketbudget.ui.format.formatMoney
import com.example.pocketbudget.ui.goalEventMessage
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class GoalsUiState(
    val isLoading: Boolean = true,
    val goals: List<GoalWithProgress> = emptyList(),
    val setAside: Double = 0.0,
    val activeCount: Int = 0,
    val pausedCount: Int = 0,
    val completedCount: Int = 0,
    val closest: GoalWithProgress? = null,
)

class GoalsViewModel(
    repository: BudgetRepository,
) : ViewModel() {
    val uiState: StateFlow<GoalsUiState> = repository.observeGoalDirectory()
        .map { directory ->
            val goals = directory.goals.sortedWith(
                compareBy<GoalWithProgress> { statusRank(it) }
                    .thenByDescending { it.fraction }
                    .thenBy { it.goal.name.lowercase() },
            )
            goalsUiState(goals)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), GoalsUiState())
}

private fun goalsUiState(goals: List<GoalWithProgress>): GoalsUiState {
    return GoalsUiState(
        isLoading = false,
        goals = goals,
        setAside = sumMoney(goals.map { it.saved }),
        activeCount = goals.count { it.goal.status == GoalStatus.ACTIVE },
        pausedCount = goals.count { it.goal.status == GoalStatus.PAUSED },
        completedCount = goals.count { it.goal.status == GoalStatus.COMPLETED },
        closest = goals
            .filter { it.goal.status != GoalStatus.COMPLETED }
            .maxByOrNull { it.fraction },
    )
}

private fun statusRank(progress: GoalWithProgress): Int {
    return when (progress.goal.status) {
        GoalStatus.ACTIVE -> if (progress.isFunded) 0 else 1
        GoalStatus.PAUSED -> 2
        GoalStatus.COMPLETED -> 3
    }
}

sealed interface GoalDetailUiState {
    data object Loading : GoalDetailUiState
    data object Missing : GoalDetailUiState
    data class Ready(val details: GoalDetails) : GoalDetailUiState
}

class GoalDetailViewModel(
    private val repository: BudgetRepository,
    private val messages: MessageViewModel,
    private val goalId: Long,
) : ViewModel() {
    val uiState: StateFlow<GoalDetailUiState> = repository.observeGoal(goalId)
        .map { details ->
            if (details == null) GoalDetailUiState.Missing else GoalDetailUiState.Ready(details)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), GoalDetailUiState.Loading)

    private val _closed = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val closed = _closed.asSharedFlow()

    fun pause() = runAction { repository.pauseGoal(goalId) }
    fun resume() = runAction { repository.resumeGoal(goalId) }
    fun complete() = runAction { repository.completeGoal(goalId) }
    fun reopen() = runAction { repository.reopenGoal(goalId) }

    fun delete(releaseSavings: Boolean) {
        viewModelScope.launch {
            when (val result = repository.deleteGoal(goalId, releaseSavings)) {
                is GoalDeleteResult.Deleted -> {
                    val text = if (result.released > 0.0) {
                        "Released ${formatMoney(result.released)} from ${result.name} and deleted the goal"
                    } else {
                        "Deleted ${result.name}"
                    }
                    messages.show(text)
                    _closed.emit(Unit)
                }
                is GoalDeleteResult.NeedsRelease -> messages.show(
                    "${result.name} still holds ${formatMoney(result.saved)}. Release it before deleting.",
                )
                GoalDeleteResult.NotFound -> messages.show("That goal no longer exists.")
            }
        }
    }

    private fun runAction(block: suspend () -> GoalActionResult) {
        viewModelScope.launch {
            when (val result = block()) {
                is GoalActionResult.Success -> messages.show(goalEventMessage(result))
                is GoalActionResult.Failure -> messages.show(result.reason)
            }
        }
    }
}

data class GoalFormState(
    val isEditing: Boolean = false,
    val ready: Boolean = false,
    val missing: Boolean = false,
    val name: String = "",
    val targetText: String = "",
    val note: String = "",
    val category: Category = Category.SHOPPING,
    val savedAmount: Double = 0.0,
    val nameError: String? = null,
    val targetError: String? = null,
    val noteError: String? = null,
)

class GoalFormViewModel(
    private val repository: BudgetRepository,
    private val messages: MessageViewModel,
    private val savedStateHandle: SavedStateHandle,
    private val goalId: Long?,
) : ViewModel() {
    private val _state = MutableStateFlow(readState())
    val uiState: StateFlow<GoalFormState> = _state
    private val _saved = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val saved = _saved.asSharedFlow()

    init {
        if (goalId == null) {
            if (savedStateHandle.get<Boolean>(KEY_READY) != true) {
                savedStateHandle[KEY_READY] = true
                savedStateHandle[KEY_CATEGORY] = Category.SHOPPING.name
            }
            _state.value = readState()
        } else if (savedStateHandle.get<Boolean>(KEY_READY) != true) {
            viewModelScope.launch { load() }
        }
    }

    fun onNameChange(value: String) = edit { savedStateHandle[KEY_NAME] = value; copy(name = value, nameError = null) }
    fun onTargetChange(value: String) = edit { savedStateHandle[KEY_TARGET] = value; copy(targetText = value, targetError = null) }
    fun onNoteChange(value: String) = edit { savedStateHandle[KEY_NOTE] = value; copy(note = value, noteError = null) }

    fun onCategoryChange(value: Category) {
        savedStateHandle[KEY_CATEGORY] = value.name
        _state.update { it.copy(category = value) }
    }

    fun save() {
        val current = _state.value
        if (!current.ready || current.missing) return
        viewModelScope.launch {
            when (
                val result = repository.saveGoal(
                    id = goalId,
                    name = current.name,
                    targetText = current.targetText,
                    category = current.category,
                    note = current.note,
                )
            ) {
                is GoalWriteResult.Invalid -> _state.value = current.copy(
                    nameError = result.nameError,
                    targetError = result.targetError,
                    noteError = result.noteError,
                )
                GoalWriteResult.Missing -> {
                    savedStateHandle[KEY_MISSING] = true
                    _state.value = current.copy(missing = true)
                }
                is GoalWriteResult.Saved -> {
                    val text = if (result.reopened) {
                        "Saved ${result.name}. The target is above the saved amount, so the goal is active again."
                    } else if (goalId == null) {
                        "Created ${result.name}"
                    } else {
                        "Saved ${result.name}"
                    }
                    messages.show(text)
                    _saved.emit(Unit)
                }
            }
        }
    }

    private suspend fun load() {
        val progress = repository.getGoalProgress(goalId ?: return)
        if (progress == null) {
            savedStateHandle[KEY_MISSING] = true
            savedStateHandle[KEY_READY] = true
            _state.value = readState()
            return
        }
        val goal = progress.goal
        savedStateHandle[KEY_NAME] = goal.name
        savedStateHandle[KEY_TARGET] = formatAmountInput(goal.targetAmount)
        savedStateHandle[KEY_NOTE] = goal.note
        savedStateHandle[KEY_CATEGORY] = goal.category.name
        savedStateHandle[KEY_SAVED] = progress.saved.toString()
        savedStateHandle[KEY_READY] = true
        _state.value = readState()
    }

    private fun edit(block: GoalFormState.() -> GoalFormState) {
        _state.value = _state.value.block()
    }

    private fun readState(): GoalFormState {
        return GoalFormState(
            isEditing = goalId != null,
            ready = savedStateHandle.get<Boolean>(KEY_READY) == true,
            missing = savedStateHandle.get<Boolean>(KEY_MISSING) == true,
            name = savedStateHandle.get<String>(KEY_NAME).orEmpty(),
            targetText = savedStateHandle.get<String>(KEY_TARGET).orEmpty(),
            note = savedStateHandle.get<String>(KEY_NOTE).orEmpty(),
            category = savedStateHandle.get<String>(KEY_CATEGORY)
                ?.let { runCatching { Category.valueOf(it) }.getOrNull() }
                ?: Category.SHOPPING,
            savedAmount = savedStateHandle.get<String>(KEY_SAVED)?.toDoubleOrNull() ?: 0.0,
        )
    }

    private companion object {
        const val KEY_READY = "goal_ready"
        const val KEY_MISSING = "goal_missing"
        const val KEY_NAME = "goal_name"
        const val KEY_TARGET = "goal_target"
        const val KEY_NOTE = "goal_note"
        const val KEY_CATEGORY = "goal_category"
        const val KEY_SAVED = "goal_saved"
    }
}

data class MoveDraft(
    val kind: ContributionKind = ContributionKind.DEPOSIT,
    val amountText: String = "",
    val note: String = "",
    val amountError: String? = null,
    val noteError: String? = null,
)

sealed interface GoalMoveUiState {
    data object Loading : GoalMoveUiState
    data object Missing : GoalMoveUiState
    data class Ready(
        val goalName: String,
        val saved: Double,
        val remaining: Double,
        val draft: MoveDraft,
        val blockedReason: String?,
    ) : GoalMoveUiState
}

class GoalMoveViewModel(
    private val repository: BudgetRepository,
    private val messages: MessageViewModel,
    private val savedStateHandle: SavedStateHandle,
    private val goalId: Long,
    initialMode: String,
) : ViewModel() {
    private val draft = MutableStateFlow(readDraft(initialMode))
    val uiState: StateFlow<GoalMoveUiState> = combine(
        repository.observeGoal(goalId),
        draft,
    ) { details, current ->
        if (details == null) {
            GoalMoveUiState.Missing
        } else {
            val progress = details.progress
            GoalMoveUiState.Ready(
                goalName = progress.goal.name,
                saved = progress.saved,
                remaining = progress.remaining,
                draft = current,
                blockedReason = moveBlockedReason(progress.goal, progress.saved, current.kind),
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), GoalMoveUiState.Loading)

    private val _saved = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val saved = _saved.asSharedFlow()

    fun onKindChange(kind: ContributionKind) {
        savedStateHandle[KEY_KIND] = kind.name
        draft.update { it.copy(kind = kind, amountError = null) }
    }

    fun onAmountChange(value: String) {
        savedStateHandle[KEY_AMOUNT] = value
        draft.update { it.copy(amountText = value, amountError = null) }
    }

    fun onNoteChange(value: String) {
        savedStateHandle[KEY_NOTE] = value
        draft.update { it.copy(note = value, noteError = null) }
    }

    fun save() {
        val current = draft.value
        val state = uiState.value as? GoalMoveUiState.Ready ?: return
        if (state.blockedReason != null) return
        val amount = com.example.pocketbudget.domain.parseAmount(current.amountText)
        if (amount == null) {
            draft.update { it.copy(amountError = "Enter an amount greater than zero") }
            return
        }
        if (current.note.trim().length > 120) {
            draft.update { it.copy(noteError = "Keep the note under 120 characters") }
            return
        }
        viewModelScope.launch {
            val result = if (current.kind == ContributionKind.DEPOSIT) {
                repository.deposit(goalId, amount, current.note)
            } else {
                repository.withdraw(goalId, amount, current.note)
            }
            when (result) {
                is GoalActionResult.Success -> {
                    messages.show(goalEventMessage(result))
                    _saved.emit(Unit)
                }
                is GoalActionResult.Failure -> draft.update { it.copy(amountError = result.reason) }
            }
        }
    }

    private fun readDraft(initialMode: String): MoveDraft {
        val stored = savedStateHandle.get<String>(KEY_KIND)
        val kind = stored?.let { runCatching { ContributionKind.valueOf(it) }.getOrNull() }
            ?: if (initialMode == "withdraw") ContributionKind.WITHDRAWAL else ContributionKind.DEPOSIT
        if (stored == null) savedStateHandle[KEY_KIND] = kind.name
        return MoveDraft(
            kind = kind,
            amountText = savedStateHandle.get<String>(KEY_AMOUNT).orEmpty(),
            note = savedStateHandle.get<String>(KEY_NOTE).orEmpty(),
        )
    }

    private companion object {
        const val KEY_KIND = "move_kind"
        const val KEY_AMOUNT = "move_amount"
        const val KEY_NOTE = "move_note"
    }
}
