package com.example.pocketbudget.data

import java.util.Calendar

fun sampleTransactions(now: Long = System.currentTimeMillis()): List<Transaction> {
    return sampleSnapshot(now).transactions
}

fun sampleSnapshot(now: Long = System.currentTimeMillis()): SampleSnapshot {
    fun at(daysAgo: Int, hour: Int): Long {
        return Calendar.getInstance().apply {
            timeInMillis = now
            add(Calendar.DAY_OF_YEAR, -daysAgo)
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    }

    val transactions = listOf(
        Transaction(1, "Scholarship", 45_000.0, TransactionType.INCOME, Category.SALARY, at(0, 9)),
        Transaction(2, "Magnum groceries", 8_640.0, TransactionType.EXPENSE, Category.FOOD, at(0, 18)),
        Transaction(3, "Coffee with Aida", 1_800.0, TransactionType.EXPENSE, Category.FOOD, at(1, 16)),
        Transaction(4, "Metro card", 3_200.0, TransactionType.EXPENSE, Category.TRANSPORT, at(1, 8)),
        Transaction(5, "Room rent", 80_000.0, TransactionType.EXPENSE, Category.BILLS, at(2, 12)),
        Transaction(6, "Part-time shift", 60_000.0, TransactionType.INCOME, Category.SALARY, at(3, 19)),
        Transaction(7, "Cinema", 3_500.0, TransactionType.EXPENSE, Category.FUN, at(4, 20)),
        Transaction(8, "Winter jacket", 24_900.0, TransactionType.EXPENSE, Category.SHOPPING, at(5, 15)),
        Transaction(9, "Phone bill", 2_990.0, TransactionType.EXPENSE, Category.BILLS, at(6, 11)),
        Transaction(10, "Pharmacy", 1_750.0, TransactionType.EXPENSE, Category.OTHER, at(6, 13)),
        Transaction(11, "Family transfer", 150_000.0, TransactionType.INCOME, Category.OTHER, at(7, 10)),
        Transaction(12, "Saved for New laptop", 40_000.0, TransactionType.EXPENSE, Category.SHOPPING, at(1, 14)),
        Transaction(13, "Saved for Summer trip", 20_000.0, TransactionType.EXPENSE, Category.FUN, at(4, 11)),
        Transaction(14, "Saved for Emergency fund", 50_000.0, TransactionType.EXPENSE, Category.OTHER, at(6, 9)),
        Transaction(15, "Returned from Summer trip", 5_000.0, TransactionType.INCOME, Category.FUN, at(3, 17)),
    )
    val goals = listOf(
        SavingsGoal(
            id = 1,
            name = "New laptop",
            targetAmount = 180_000.0,
            category = Category.SHOPPING,
            note = "For study and work",
            status = GoalStatus.ACTIVE,
            createdAtMillis = at(8, 10),
            completedAtMillis = null,
        ),
        SavingsGoal(
            id = 2,
            name = "Summer trip",
            targetAmount = 90_000.0,
            category = Category.FUN,
            note = "Paused until exams are over",
            status = GoalStatus.PAUSED,
            createdAtMillis = at(8, 11),
            completedAtMillis = null,
        ),
        SavingsGoal(
            id = 3,
            name = "Emergency fund",
            targetAmount = 50_000.0,
            category = Category.OTHER,
            note = "Three months of small expenses",
            status = GoalStatus.COMPLETED,
            createdAtMillis = at(8, 12),
            completedAtMillis = at(6, 10),
        ),
    )
    val contributions = listOf(
        Contribution(
            id = 1,
            goalId = 1,
            transactionId = 12,
            amount = 40_000.0,
            kind = ContributionKind.DEPOSIT,
            note = "First deposit",
            createdAtMillis = at(1, 14),
            transactionTitle = "Saved for New laptop",
        ),
        Contribution(
            id = 2,
            goalId = 2,
            transactionId = 13,
            amount = 20_000.0,
            kind = ContributionKind.DEPOSIT,
            note = "Set aside after the cinema week",
            createdAtMillis = at(4, 11),
            transactionTitle = "Saved for Summer trip",
        ),
        Contribution(
            id = 3,
            goalId = 2,
            transactionId = 15,
            amount = 5_000.0,
            kind = ContributionKind.WITHDRAWAL,
            note = "Train ticket came up early",
            createdAtMillis = at(3, 17),
            transactionTitle = "Returned from Summer trip",
        ),
        Contribution(
            id = 4,
            goalId = 3,
            transactionId = 14,
            amount = 50_000.0,
            kind = ContributionKind.DEPOSIT,
            note = "Moved part of the family transfer aside",
            createdAtMillis = at(6, 9),
            transactionTitle = "Saved for Emergency fund",
        ),
    )
    return SampleSnapshot(
        transactions = transactions,
        goals = goals,
        contributions = contributions,
    )
}
