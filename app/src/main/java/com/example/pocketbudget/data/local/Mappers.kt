package com.example.pocketbudget.data.local

import com.example.pocketbudget.data.Contribution
import com.example.pocketbudget.data.SavingsGoal
import com.example.pocketbudget.data.Transaction

fun TransactionEntity.toDomain(): Transaction {
    return Transaction(
        id = id,
        title = title,
        amount = amount,
        type = type,
        category = category,
        createdAtMillis = createdAtMillis,
    )
}

fun Transaction.toEntity(): TransactionEntity {
    return TransactionEntity(
        id = id,
        title = title,
        amount = amount,
        type = type,
        category = category,
        createdAtMillis = createdAtMillis,
    )
}

fun SavingsGoalEntity.toDomain(): SavingsGoal {
    return SavingsGoal(
        id = id,
        name = name,
        targetAmount = targetAmount,
        category = category,
        note = note,
        status = status,
        createdAtMillis = createdAtMillis,
        completedAtMillis = completedAtMillis,
    )
}

fun SavingsGoal.toEntity(): SavingsGoalEntity {
    return SavingsGoalEntity(
        id = id,
        name = name,
        targetAmount = targetAmount,
        category = category,
        note = note,
        status = status,
        createdAtMillis = createdAtMillis,
        completedAtMillis = completedAtMillis,
    )
}

fun ContributionEntity.toDomain(transactionTitle: String): Contribution {
    return Contribution(
        id = id,
        goalId = goalId,
        transactionId = transactionId,
        amount = amount,
        kind = kind,
        note = note,
        createdAtMillis = createdAtMillis,
        transactionTitle = transactionTitle,
    )
}

fun Contribution.toEntity(): ContributionEntity {
    return ContributionEntity(
        id = id,
        goalId = goalId,
        transactionId = transactionId,
        amount = amount,
        kind = kind,
        note = note,
        createdAtMillis = createdAtMillis,
    )
}
