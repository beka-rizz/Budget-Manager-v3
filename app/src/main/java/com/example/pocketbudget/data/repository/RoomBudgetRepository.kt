package com.example.pocketbudget.data.repository

import androidx.room.withTransaction
import com.example.pocketbudget.data.Category
import com.example.pocketbudget.data.ContributionKind
import com.example.pocketbudget.data.GoalStatus
import com.example.pocketbudget.data.Transaction
import com.example.pocketbudget.data.TransactionGoalLink
import com.example.pocketbudget.data.TransactionType
import com.example.pocketbudget.data.local.ContributionEntity
import com.example.pocketbudget.data.local.MetaEntity
import com.example.pocketbudget.data.local.PocketBudgetDatabase
import com.example.pocketbudget.data.local.SavingsGoalEntity
import com.example.pocketbudget.data.local.TransactionEntity
import com.example.pocketbudget.data.local.toDomain
import com.example.pocketbudget.data.local.toEntity
import com.example.pocketbudget.data.sampleSnapshot
import com.example.pocketbudget.domain.GoalDetails
import com.example.pocketbudget.domain.GoalDirectory
import com.example.pocketbudget.domain.GoalWithProgress
import com.example.pocketbudget.domain.MovePlan
import com.example.pocketbudget.domain.completeError
import com.example.pocketbudget.domain.detailsFrom
import com.example.pocketbudget.domain.directoryFrom
import com.example.pocketbudget.domain.pauseError
import com.example.pocketbudget.domain.planDeposit
import com.example.pocketbudget.domain.planRelease
import com.example.pocketbudget.domain.planWithdraw
import com.example.pocketbudget.domain.progressOf
import com.example.pocketbudget.domain.reopenError
import com.example.pocketbudget.domain.resumeError
import com.example.pocketbudget.domain.statusAfterTargetChange
import com.example.pocketbudget.domain.toCents
import com.example.pocketbudget.domain.validateDraft
import com.example.pocketbudget.domain.validateGoalDraft
import com.example.pocketbudget.domain.validateTitle
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map

class RoomBudgetRepository(
    private val database: PocketBudgetDatabase,
) : BudgetRepository {
    private val transactionDao = database.transactionDao()
    private val goalDao = database.savingsGoalDao()
    private val contributionDao = database.contributionDao()
    private val metaDao = database.metaDao()

    override fun observeTransactions(): Flow<List<Transaction>> {
        return transactionDao.observeAll().map { entities -> entities.map { it.toDomain() } }
    }

    override fun observeTransaction(id: Long): Flow<Transaction?> {
        return transactionDao.observeById(id).map { entity -> entity?.toDomain() }
    }

    override fun observeTransactionLink(transactionId: Long): Flow<TransactionGoalLink?> {
        return combine(contributionDao.observeByTransaction(transactionId), goalDao.observeAll()) { contribution, goals ->
            if (contribution == null) {
                null
            } else {
                goals.firstOrNull { it.id == contribution.goalId }?.let { goal ->
                    TransactionGoalLink(goalId = goal.id, goalName = goal.name)
                }
            }
        }
    }

    override fun observeGoalDirectory(): Flow<GoalDirectory> {
        return combine(goalDao.observeAll(), contributionDao.observeAll()) { goals, contributions ->
            directoryFrom(
                goals = goals.map { it.toDomain() },
                contributions = contributions.map { entity ->
                    entity.toDomain(transactionTitle = "")
                },
            )
        }
    }

    override fun observeGoal(id: Long): Flow<GoalDetails?> {
        return combine(
            goalDao.observeById(id),
            contributionDao.observeByGoal(id),
            transactionDao.observeAll(),
        ) { goal, contributions, transactions ->
            if (goal == null) {
                null
            } else {
                detailsFrom(
                    goal = goal.toDomain(),
                    contributions = contributions.map { entity ->
                        val title = transactions.firstOrNull { it.id == entity.transactionId }?.title.orEmpty()
                        entity.toDomain(title)
                    },
                    transactions = transactions.map { it.toDomain() },
                )
            }
        }
    }

    override suspend fun getTransaction(id: Long): Transaction? {
        return transactionDao.getById(id)?.toDomain()
    }

    override suspend fun getTransactionLink(transactionId: Long): TransactionGoalLink? {
        val contribution = contributionDao.getByTransaction(transactionId) ?: return null
        val goal = goalDao.getById(contribution.goalId) ?: return null
        return TransactionGoalLink(goalId = goal.id, goalName = goal.name)
    }

    override suspend fun getGoalProgress(id: Long): GoalWithProgress? {
        val goal = goalDao.getById(id) ?: return null
        val contributions = contributionDao.listByGoal(id).map { it.toDomain("") }
        return progressOf(goal.toDomain(), contributions)
    }

    override suspend fun saveTransaction(
        id: Long?,
        title: String,
        amountText: String,
        type: TransactionType,
        category: Category,
    ): TransactionWriteResult {
        val existing = id?.let { transactionDao.getById(it) }
        if (id != null && existing == null) return TransactionWriteResult.Missing
        val link = id?.let { contributionDao.getByTransaction(it) }
        val locked = link != null && existing != null
        val (cleanTitle, titleError) = validateTitle(title)
        if (titleError != null) {
            return TransactionWriteResult.Invalid(titleError = titleError, amountError = null)
        }
        val amount: Double
        val savedType: TransactionType
        if (locked) {
            amount = existing.amount
            savedType = existing.type
        } else {
            val validation = validateDraft(cleanTitle, amountText)
            if (!validation.isValid) {
                return TransactionWriteResult.Invalid(
                    titleError = validation.titleError,
                    amountError = validation.amountError,
                )
            }
            amount = validation.amount ?: return TransactionWriteResult.Invalid(
                titleError = validation.titleError,
                amountError = validation.amountError,
            )
            savedType = type
        }
        val saved = if (existing == null) {
            val newId = transactionDao.insert(
                TransactionEntity(
                    title = cleanTitle,
                    amount = amount,
                    type = savedType,
                    category = category,
                    createdAtMillis = System.currentTimeMillis(),
                ),
            )
            transactionDao.getById(newId)?.toDomain()
        } else {
            val updated = existing.copy(
                title = cleanTitle,
                amount = amount,
                type = savedType,
                category = category,
            )
            transactionDao.update(updated)
            updated.toDomain()
        }
        return if (saved == null) {
            TransactionWriteResult.Missing
        } else {
            TransactionWriteResult.Saved(transaction = saved, moneyLocked = locked)
        }
    }

    override suspend fun deleteTransaction(id: Long): TransactionDeletion? {
        return database.withTransaction {
            val entity = transactionDao.getById(id) ?: return@withTransaction null
            val contribution = contributionDao.getByTransaction(id)
            val goalName = contribution?.let { goalDao.getById(it.goalId)?.name }
            transactionDao.deleteById(id)
            TransactionDeletion(
                transaction = entity.toDomain(),
                goalName = goalName,
                contribution = contribution?.toDomain(entity.title),
            )
        }
    }

    override suspend fun restoreTransaction(deletion: TransactionDeletion) {
        database.withTransaction {
            transactionDao.insert(deletion.transaction.toEntity())
            val contribution = deletion.contribution ?: return@withTransaction
            if (goalDao.getById(contribution.goalId) != null) {
                contributionDao.insert(contribution.toEntity())
            }
        }
    }

    override suspend fun saveGoal(
        id: Long?,
        name: String,
        targetText: String,
        category: Category,
        note: String,
    ): GoalWriteResult {
        val existing = id?.let { goalDao.getById(it) }
        if (id != null && existing == null) return GoalWriteResult.Missing
        val savedAmount = if (existing == null) {
            0.0
        } else {
            progressOf(
                existing.toDomain(),
                contributionDao.listByGoal(existing.id).map { it.toDomain("") },
            ).saved
        }
        val validation = validateGoalDraft(name, targetText, note, savedAmount)
        if (!validation.isValid) {
            return GoalWriteResult.Invalid(
                nameError = validation.nameError,
                targetError = validation.targetError,
                noteError = validation.noteError,
            )
        }
        val target = validation.target ?: return GoalWriteResult.Invalid(
            nameError = validation.nameError,
            targetError = validation.targetError,
            noteError = validation.noteError,
        )
        if (existing == null) {
            goalDao.insert(
                SavingsGoalEntity(
                    name = validation.name,
                    targetAmount = target,
                    category = category,
                    note = validation.note,
                    status = GoalStatus.ACTIVE,
                    createdAtMillis = System.currentTimeMillis(),
                    completedAtMillis = null,
                ),
            )
            return GoalWriteResult.Saved(name = validation.name, reopened = false)
        }
        val newStatus = statusAfterTargetChange(existing.status, target, savedAmount)
        val reopened = existing.status == GoalStatus.COMPLETED && newStatus == GoalStatus.ACTIVE
        goalDao.update(
            existing.copy(
                name = validation.name,
                targetAmount = target,
                category = category,
                note = validation.note,
                status = newStatus,
                completedAtMillis = if (newStatus == GoalStatus.COMPLETED) existing.completedAtMillis else null,
            ),
        )
        return GoalWriteResult.Saved(name = validation.name, reopened = reopened)
    }

    override suspend fun deleteGoal(id: Long, releaseSavings: Boolean): GoalDeleteResult {
        return database.withTransaction {
            val goal = goalDao.getById(id) ?: return@withTransaction GoalDeleteResult.NotFound
            val saved = progressOf(
                goal.toDomain(),
                contributionDao.listByGoal(id).map { it.toDomain("") },
            ).saved
            if (toCents(saved) > 0 && !releaseSavings) {
                return@withTransaction GoalDeleteResult.NeedsRelease(name = goal.name, saved = saved)
            }
            if (toCents(saved) > 0) {
                val release = planRelease(goal.toDomain(), saved)
                transactionDao.insert(
                    TransactionEntity(
                        title = release.title,
                        amount = release.amount,
                        type = release.type,
                        category = release.category,
                        createdAtMillis = System.currentTimeMillis(),
                    ),
                )
            }
            goalDao.deleteById(id)
            GoalDeleteResult.Deleted(
                name = goal.name,
                released = if (toCents(saved) > 0) saved else 0.0,
            )
        }
    }

    override suspend fun deposit(goalId: Long, amount: Double, note: String): GoalActionResult {
        return move(goalId, amount, note, deposit = true)
    }

    override suspend fun withdraw(goalId: Long, amount: Double, note: String): GoalActionResult {
        return move(goalId, amount, note, deposit = false)
    }

    override suspend fun pauseGoal(id: Long): GoalActionResult = updateStatus(id) { goal, saved ->
        val error = pauseError(goal, saved)
        if (error != null) {
            GoalActionResult.Failure(error)
        } else {
            goalDao.update(goal.toEntity().copy(status = GoalStatus.PAUSED))
            GoalActionResult.Success(goal.name, GoalEvent.PAUSED, nowFunded = false)
        }
    }

    override suspend fun resumeGoal(id: Long): GoalActionResult = updateStatus(id) { goal, _ ->
        val error = resumeError(goal)
        if (error != null) {
            GoalActionResult.Failure(error)
        } else {
            goalDao.update(goal.toEntity().copy(status = GoalStatus.ACTIVE))
            GoalActionResult.Success(goal.name, GoalEvent.RESUMED, nowFunded = false)
        }
    }

    override suspend fun completeGoal(id: Long): GoalActionResult = updateStatus(id) { goal, saved ->
        val error = completeError(goal, saved)
        if (error != null) {
            GoalActionResult.Failure(error)
        } else {
            goalDao.update(
                goal.toEntity().copy(
                    status = GoalStatus.COMPLETED,
                    completedAtMillis = System.currentTimeMillis(),
                ),
            )
            GoalActionResult.Success(goal.name, GoalEvent.COMPLETED, nowFunded = true)
        }
    }

    override suspend fun reopenGoal(id: Long): GoalActionResult = updateStatus(id) { goal, _ ->
        val error = reopenError(goal)
        if (error != null) {
            GoalActionResult.Failure(error)
        } else {
            goalDao.update(
                goal.toEntity().copy(
                    status = GoalStatus.ACTIVE,
                    completedAtMillis = null,
                ),
            )
            GoalActionResult.Success(goal.name, GoalEvent.REOPENED, nowFunded = false)
        }
    }

    override suspend fun seedIfNeeded() {
        if (metaDao.get()?.seeded == true) return
        database.withTransaction {
            if (metaDao.get()?.seeded == true) return@withTransaction
            if (transactionDao.count() == 0) {
                val sample = sampleSnapshot()
                sample.transactions.forEach { transactionDao.insert(it.toEntity()) }
                sample.goals.forEach { goalDao.insert(it.toEntity()) }
                sample.contributions.forEach { contributionDao.insert(it.toEntity()) }
            }
            metaDao.insert(MetaEntity(seeded = true))
        }
    }

    private suspend fun move(
        goalId: Long,
        amount: Double,
        note: String,
        deposit: Boolean,
    ): GoalActionResult {
        return database.withTransaction {
            val goalEntity = goalDao.getById(goalId)
                ?: return@withTransaction GoalActionResult.Failure("That goal no longer exists.")
            val goal = goalEntity.toDomain()
            val contributions = contributionDao.listByGoal(goalId).map { it.toDomain("") }
            val saved = progressOf(goal, contributions).saved
            val plan = if (deposit) {
                planDeposit(goal, saved, amount, note)
            } else {
                planWithdraw(goal, saved, amount, note)
            }
            when (plan) {
                is MovePlan.Rejected -> GoalActionResult.Failure(
                    plan.amountError ?: plan.noteError ?: "Check the amount and try again.",
                )
                is MovePlan.Ready -> {
                    val now = System.currentTimeMillis()
                    val transactionId = transactionDao.insert(
                        TransactionEntity(
                            title = plan.title,
                            amount = plan.amount,
                            type = plan.type,
                            category = plan.category,
                            createdAtMillis = now,
                        ),
                    )
                    contributionDao.insert(
                        ContributionEntity(
                            goalId = goalId,
                            transactionId = transactionId,
                            amount = plan.amount,
                            kind = plan.kind,
                            note = plan.note,
                            createdAtMillis = now,
                        ),
                    )
                    GoalActionResult.Success(
                        goalName = goal.name,
                        event = if (plan.kind == ContributionKind.DEPOSIT) {
                            GoalEvent.DEPOSITED
                        } else {
                            GoalEvent.WITHDRAWN
                        },
                        nowFunded = plan.fundedAfter,
                    )
                }
            }
        }
    }

    private suspend fun updateStatus(
        id: Long,
        block: suspend (goal: com.example.pocketbudget.data.SavingsGoal, saved: Double) -> GoalActionResult,
    ): GoalActionResult {
        return database.withTransaction {
            val entity = goalDao.getById(id)
                ?: return@withTransaction GoalActionResult.Failure("That goal no longer exists.")
            val goal = entity.toDomain()
            val saved = progressOf(
                goal,
                contributionDao.listByGoal(id).map { it.toDomain("") },
            ).saved
            block(goal, saved)
        }
    }
}
