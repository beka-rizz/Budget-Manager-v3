package com.example.pocketbudget.data.repository

import com.example.pocketbudget.data.Category
import com.example.pocketbudget.data.Contribution
import com.example.pocketbudget.data.Transaction
import com.example.pocketbudget.data.TransactionGoalLink
import com.example.pocketbudget.domain.GoalDetails
import com.example.pocketbudget.domain.GoalDirectory
import com.example.pocketbudget.domain.GoalWithProgress
import kotlinx.coroutines.flow.Flow

enum class GoalEvent {
    DEPOSITED,
    WITHDRAWN,
    PAUSED,
    RESUMED,
    COMPLETED,
    REOPENED,
}

sealed interface TransactionWriteResult {
    data class Saved(val transaction: Transaction, val moneyLocked: Boolean) : TransactionWriteResult
    data class Invalid(val titleError: String?, val amountError: String?) : TransactionWriteResult
    data object Missing : TransactionWriteResult
}

data class TransactionDeletion(
    val transaction: Transaction,
    val goalName: String?,
    val contribution: Contribution?,
)

sealed interface GoalWriteResult {
    data class Saved(val name: String, val reopened: Boolean) : GoalWriteResult
    data class Invalid(
        val nameError: String?,
        val targetError: String?,
        val noteError: String?,
    ) : GoalWriteResult
    data object Missing : GoalWriteResult
}

sealed interface GoalActionResult {
    data class Success(
        val goalName: String,
        val event: GoalEvent,
        val nowFunded: Boolean,
    ) : GoalActionResult

    data class Failure(val reason: String) : GoalActionResult
}

sealed interface GoalDeleteResult {
    data class Deleted(val name: String, val released: Double) : GoalDeleteResult
    data class NeedsRelease(val name: String, val saved: Double) : GoalDeleteResult
    data object NotFound : GoalDeleteResult
}

interface BudgetRepository {
    fun observeTransactions(): Flow<List<Transaction>>
    fun observeTransaction(id: Long): Flow<Transaction?>
    fun observeTransactionLink(transactionId: Long): Flow<TransactionGoalLink?>
    fun observeGoalDirectory(): Flow<GoalDirectory>
    fun observeGoal(id: Long): Flow<GoalDetails?>

    suspend fun getTransaction(id: Long): Transaction?
    suspend fun getTransactionLink(transactionId: Long): TransactionGoalLink?
    suspend fun getGoalProgress(id: Long): GoalWithProgress?

    suspend fun saveTransaction(
        id: Long?,
        title: String,
        amountText: String,
        type: com.example.pocketbudget.data.TransactionType,
        category: Category,
    ): TransactionWriteResult

    suspend fun deleteTransaction(id: Long): TransactionDeletion?
    suspend fun restoreTransaction(deletion: TransactionDeletion)

    suspend fun saveGoal(
        id: Long?,
        name: String,
        targetText: String,
        category: Category,
        note: String,
    ): GoalWriteResult

    suspend fun deleteGoal(id: Long, releaseSavings: Boolean): GoalDeleteResult
    suspend fun deposit(goalId: Long, amount: Double, note: String): GoalActionResult
    suspend fun withdraw(goalId: Long, amount: Double, note: String): GoalActionResult
    suspend fun pauseGoal(id: Long): GoalActionResult
    suspend fun resumeGoal(id: Long): GoalActionResult
    suspend fun completeGoal(id: Long): GoalActionResult
    suspend fun reopenGoal(id: Long): GoalActionResult

    suspend fun seedIfNeeded() {}
}
