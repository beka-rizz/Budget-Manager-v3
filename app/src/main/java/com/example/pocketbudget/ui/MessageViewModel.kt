package com.example.pocketbudget.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.pocketbudget.data.repository.BudgetRepository
import com.example.pocketbudget.data.repository.GoalActionResult
import com.example.pocketbudget.data.repository.GoalEvent
import com.example.pocketbudget.data.repository.TransactionDeletion
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch

data class AppMessage(
    val id: Long,
    val text: String,
    val actionLabel: String? = null,
    val deletion: TransactionDeletion? = null,
)

class MessageViewModel(
    private val repository: BudgetRepository,
) : ViewModel() {
    private val _messages = MutableSharedFlow<AppMessage>(extraBufferCapacity = 8)
    val messages = _messages.asSharedFlow()
    private var pendingUndo: AppMessage? = null
    var lastUndoMessageId: Long? = null
        private set

    fun show(
        text: String,
        actionLabel: String? = null,
        deletion: TransactionDeletion? = null,
    ): Long {
        val message = AppMessage(
            id = System.nanoTime(),
            text = text,
            actionLabel = actionLabel,
            deletion = deletion,
        )
        if (deletion != null) {
            pendingUndo = message
            lastUndoMessageId = message.id
        }
        _messages.tryEmit(message)
        return message.id
    }

    fun undo(messageId: Long) {
        val message = pendingUndo ?: return
        if (message.id != messageId) return
        val deletion = message.deletion ?: return
        pendingUndo = null
        viewModelScope.launch {
            repository.restoreTransaction(deletion)
        }
    }
}

fun goalEventMessage(result: GoalActionResult.Success): String {
    val name = result.goalName
    val base = when (result.event) {
        GoalEvent.DEPOSITED -> "Added money to $name"
        GoalEvent.WITHDRAWN -> "Took money out of $name"
        GoalEvent.PAUSED -> "Paused $name"
        GoalEvent.RESUMED -> "Resumed $name"
        GoalEvent.COMPLETED -> "Completed $name"
        GoalEvent.REOPENED -> "Reopened $name"
    }
    return if (result.nowFunded && result.event == GoalEvent.DEPOSITED) {
        "$base. The target is reached, so you can mark it complete."
    } else {
        base
    }
}
