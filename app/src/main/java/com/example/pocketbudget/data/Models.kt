package com.example.pocketbudget.data

enum class TransactionType {
    INCOME,
    EXPENSE,
}

enum class Category(val label: String) {
    FOOD("Food"),
    TRANSPORT("Transport"),
    SHOPPING("Shopping"),
    BILLS("Bills"),
    FUN("Fun"),
    SALARY("Salary"),
    OTHER("Other"),
}

enum class GoalStatus(val label: String) {
    ACTIVE("Active"),
    PAUSED("Paused"),
    COMPLETED("Complete"),
}

enum class ContributionKind {
    DEPOSIT,
    WITHDRAWAL,
}

data class Transaction(
    val id: Long,
    val title: String,
    val amount: Double,
    val type: TransactionType,
    val category: Category,
    val createdAtMillis: Long,
)

data class SavingsGoal(
    val id: Long,
    val name: String,
    val targetAmount: Double,
    val category: Category,
    val note: String,
    val status: GoalStatus,
    val createdAtMillis: Long,
    val completedAtMillis: Long?,
)

data class Contribution(
    val id: Long,
    val goalId: Long,
    val transactionId: Long,
    val amount: Double,
    val kind: ContributionKind,
    val note: String,
    val createdAtMillis: Long,
    val transactionTitle: String,
)

data class TransactionGoalLink(
    val goalId: Long,
    val goalName: String,
)

data class SampleSnapshot(
    val transactions: List<Transaction>,
    val goals: List<SavingsGoal>,
    val contributions: List<Contribution>,
)
