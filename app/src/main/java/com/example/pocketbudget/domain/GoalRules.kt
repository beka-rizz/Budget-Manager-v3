package com.example.pocketbudget.domain

import com.example.pocketbudget.data.Category
import com.example.pocketbudget.data.Contribution
import com.example.pocketbudget.data.ContributionKind
import com.example.pocketbudget.data.GoalStatus
import com.example.pocketbudget.data.SavingsGoal
import com.example.pocketbudget.data.Transaction
import com.example.pocketbudget.data.TransactionType

data class GoalWithProgress(
    val goal: SavingsGoal,
    val saved: Double,
    val deposited: Double,
    val withdrawn: Double,
) {
    val remaining: Double
        get() = centsToAmount((toCents(goal.targetAmount) - toCents(saved)).coerceAtLeast(0))

    val isFunded: Boolean
        get() = toCents(goal.targetAmount) > 0 && toCents(saved) >= toCents(goal.targetAmount)

    val fraction: Float
        get() {
            val target = toCents(goal.targetAmount)
            if (target <= 0L) return 0f
            return (toCents(saved).toFloat() / target.toFloat()).coerceIn(0f, 1f)
        }
}

data class GoalDirectory(
    val goals: List<GoalWithProgress>,
    val goalNameByTransactionId: Map<Long, String>,
)

data class GoalActions(
    val canDeposit: Boolean,
    val canWithdraw: Boolean,
    val canPause: Boolean,
    val canResume: Boolean,
    val canComplete: Boolean,
    val canReopen: Boolean,
    val hint: String,
)

data class GoalDetails(
    val progress: GoalWithProgress,
    val contributions: List<Contribution>,
    val actions: GoalActions,
)

data class GoalDraftValidation(
    val name: String,
    val note: String,
    val target: Double?,
    val nameError: String?,
    val noteError: String?,
    val targetError: String?,
) {
    val isValid: Boolean
        get() = nameError == null && noteError == null && targetError == null && target != null
}

sealed interface MovePlan {
    data class Ready(
        val title: String,
        val amount: Double,
        val type: TransactionType,
        val category: Category,
        val kind: ContributionKind,
        val note: String,
        val fundedAfter: Boolean,
    ) : MovePlan

    data class Rejected(
        val amountError: String?,
        val noteError: String?,
    ) : MovePlan
}

data class PlannedRelease(
    val title: String,
    val amount: Double,
    val type: TransactionType,
    val category: Category,
)

fun savedCents(contributions: List<Contribution>): Long {
    return contributions.sumOf { contribution ->
        val cents = toCents(contribution.amount)
        if (contribution.kind == ContributionKind.DEPOSIT) cents else -cents
    }
}

fun progressOf(goal: SavingsGoal, contributions: List<Contribution>): GoalWithProgress {
    val saved = centsToAmount(savedCents(contributions).coerceAtLeast(0))
    val deposited = centsToAmount(
        contributions.filter { it.kind == ContributionKind.DEPOSIT }.sumOf { toCents(it.amount) },
    )
    val withdrawn = centsToAmount(
        contributions.filter { it.kind == ContributionKind.WITHDRAWAL }.sumOf { toCents(it.amount) },
    )
    return GoalWithProgress(
        goal = goal,
        saved = saved,
        deposited = deposited,
        withdrawn = withdrawn,
    )
}

fun directoryFrom(goals: List<SavingsGoal>, contributions: List<Contribution>): GoalDirectory {
    val progress = goals.map { goal ->
        progressOf(goal, contributions.filter { it.goalId == goal.id })
    }
    val names = contributions.mapNotNull { contribution ->
        val name = goals.firstOrNull { it.id == contribution.goalId }?.name ?: return@mapNotNull null
        contribution.transactionId to name
    }.toMap()
    return GoalDirectory(goals = progress, goalNameByTransactionId = names)
}

fun detailsFrom(
    goal: SavingsGoal,
    contributions: List<Contribution>,
    transactions: List<Transaction>,
): GoalDetails {
    val relevant = contributions
        .filter { it.goalId == goal.id }
        .sortedWith(compareByDescending<Contribution> { it.createdAtMillis }.thenByDescending { it.id })
        .map { contribution ->
            val title = transactions.firstOrNull { it.id == contribution.transactionId }?.title
                ?: contribution.transactionTitle
            contribution.copy(transactionTitle = title)
        }
    val progress = progressOf(goal, relevant)
    return GoalDetails(
        progress = progress,
        contributions = relevant,
        actions = availableActions(goal, progress.saved),
    )
}

fun validateGoalDraft(
    name: String,
    targetText: String,
    note: String,
    saved: Double,
): GoalDraftValidation {
    val cleanName = name.trim()
    val cleanNote = note.trim()
    val nameError = when {
        cleanName.isEmpty() -> "Enter a name"
        cleanName.length > 40 -> "Keep the name under 40 characters"
        else -> null
    }
    val noteError = if (cleanNote.length > 120) "Keep the note under 120 characters" else null
    val target = parseAmount(targetText)
    val targetError = when {
        target == null -> "Enter a target greater than zero"
        toCents(target) < toCents(saved) -> "Target must cover the amount already saved"
        else -> null
    }
    return GoalDraftValidation(
        name = cleanName,
        note = cleanNote,
        target = target,
        nameError = nameError,
        noteError = noteError,
        targetError = targetError,
    )
}

fun statusAfterTargetChange(status: GoalStatus, newTarget: Double, saved: Double): GoalStatus {
    return if (status == GoalStatus.COMPLETED && toCents(newTarget) > toCents(saved)) {
        GoalStatus.ACTIVE
    } else {
        status
    }
}

fun availableActions(goal: SavingsGoal, saved: Double): GoalActions {
    val funded = toCents(goal.targetAmount) > 0 && toCents(saved) >= toCents(goal.targetAmount)
    val hasMoney = toCents(saved) > 0
    return when (goal.status) {
        GoalStatus.COMPLETED -> GoalActions(
            canDeposit = false,
            canWithdraw = false,
            canPause = false,
            canResume = false,
            canComplete = false,
            canReopen = true,
            hint = "This goal is complete. Reopen it if you want to change the money set aside.",
        )
        GoalStatus.PAUSED -> GoalActions(
            canDeposit = false,
            canWithdraw = hasMoney,
            canPause = false,
            canResume = true,
            canComplete = false,
            canReopen = false,
            hint = if (funded) {
                "This goal is funded, but it is paused. Resume it to mark it complete."
            } else {
                "Paused. New money is not accepted until you resume. You can still take money out."
            },
        )
        GoalStatus.ACTIVE -> GoalActions(
            canDeposit = !funded,
            canWithdraw = hasMoney,
            canPause = !funded,
            canResume = false,
            canComplete = funded,
            canReopen = false,
            hint = if (funded) {
                "The target is reached. Mark the goal complete, or take money out if you set too much aside."
            } else {
                "Add money until you reach the target. Pause the goal if you want to freeze it."
            },
        )
    }
}

fun pauseError(goal: SavingsGoal, saved: Double): String? = when {
    goal.status != GoalStatus.ACTIVE -> "Only an active goal can be paused."
    toCents(goal.targetAmount) > 0 && toCents(saved) >= toCents(goal.targetAmount) ->
        "This goal is funded. Mark it complete instead of pausing it."
    else -> null
}

fun resumeError(goal: SavingsGoal): String? {
    return if (goal.status != GoalStatus.PAUSED) "Only a paused goal can be resumed." else null
}

fun completeError(goal: SavingsGoal, saved: Double): String? = when {
    goal.status != GoalStatus.ACTIVE -> "Only an active goal can be marked complete."
    toCents(saved) < toCents(goal.targetAmount) -> "Add the remaining amount before marking this goal complete."
    else -> null
}

fun reopenError(goal: SavingsGoal): String? {
    return if (goal.status != GoalStatus.COMPLETED) "Only a completed goal can be reopened." else null
}

fun moveBlockedReason(goal: SavingsGoal, saved: Double, kind: ContributionKind): String? {
    return when (kind) {
        ContributionKind.DEPOSIT -> when {
            goal.status == GoalStatus.COMPLETED -> "Reopen this goal before adding money."
            goal.status == GoalStatus.PAUSED -> "Resume this goal before adding money."
            toCents(goal.targetAmount) > 0 && toCents(saved) >= toCents(goal.targetAmount) ->
                "This goal is already funded."
            else -> null
        }
        ContributionKind.WITHDRAWAL -> when {
            goal.status == GoalStatus.COMPLETED -> "Reopen this goal before taking money out."
            toCents(saved) <= 0 -> "This goal has no money to take out."
            else -> null
        }
    }
}

fun planDeposit(goal: SavingsGoal, saved: Double, amount: Double, note: String): MovePlan {
    val cleanNote = note.trim()
    val noteError = noteError(cleanNote)
    val remaining = toCents(goal.targetAmount) - toCents(saved)
    val amountError = when {
        goal.status != GoalStatus.ACTIVE -> "Resume this goal before adding money."
        remaining <= 0L -> "This goal is already funded. Mark it complete or take money out."
        toCents(amount) <= 0L -> "Enter an amount greater than zero."
        toCents(amount) > remaining -> "That amount is more than the remaining target."
        else -> null
    }
    if (noteError != null || amountError != null) {
        return MovePlan.Rejected(amountError = amountError, noteError = noteError)
    }
    return MovePlan.Ready(
        title = moveTitle(goal.name, ContributionKind.DEPOSIT),
        amount = amount,
        type = TransactionType.EXPENSE,
        category = goal.category,
        kind = ContributionKind.DEPOSIT,
        note = cleanNote,
        fundedAfter = toCents(saved) + toCents(amount) >= toCents(goal.targetAmount),
    )
}

fun planWithdraw(goal: SavingsGoal, saved: Double, amount: Double, note: String): MovePlan {
    val cleanNote = note.trim()
    val noteError = noteError(cleanNote)
    val amountError = when {
        goal.status == GoalStatus.COMPLETED -> "Reopen this goal before taking money out."
        toCents(saved) <= 0L -> "This goal has no money to take out."
        toCents(amount) <= 0L -> "Enter an amount greater than zero."
        toCents(amount) > toCents(saved) -> "That amount is more than the money set aside."
        else -> null
    }
    if (noteError != null || amountError != null) {
        return MovePlan.Rejected(amountError = amountError, noteError = noteError)
    }
    return MovePlan.Ready(
        title = moveTitle(goal.name, ContributionKind.WITHDRAWAL),
        amount = amount,
        type = TransactionType.INCOME,
        category = goal.category,
        kind = ContributionKind.WITHDRAWAL,
        note = cleanNote,
        fundedAfter = false,
    )
}

fun planRelease(goal: SavingsGoal, saved: Double): PlannedRelease {
    return PlannedRelease(
        title = "Released from ${goal.name}".take(80),
        amount = saved,
        type = TransactionType.INCOME,
        category = goal.category,
    )
}

fun percentLabel(saved: Double, target: Double): String {
    val targetCents = toCents(target)
    if (targetCents <= 0L) return "0%"
    val percent = (toCents(saved) * 100 / targetCents).toInt().coerceIn(0, 100)
    return "$percent%"
}

private fun noteError(note: String): String? {
    return if (note.length > 120) "Keep the note under 120 characters" else null
}

private fun moveTitle(name: String, kind: ContributionKind): String {
    val prefix = if (kind == ContributionKind.DEPOSIT) "Saved for " else "Returned from "
    return (prefix + name).take(80)
}
