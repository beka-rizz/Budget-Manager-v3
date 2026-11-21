package com.example.pocketbudget.data.local

import androidx.room.TypeConverter
import com.example.pocketbudget.data.Category
import com.example.pocketbudget.data.ContributionKind
import com.example.pocketbudget.data.GoalStatus
import com.example.pocketbudget.data.TransactionType

class Converters {
    @TypeConverter
    fun fromTransactionType(value: TransactionType): String = value.name

    @TypeConverter
    fun toTransactionType(value: String): TransactionType = TransactionType.valueOf(value)

    @TypeConverter
    fun fromCategory(value: Category): String = value.name

    @TypeConverter
    fun toCategory(value: String): Category = Category.valueOf(value)

    @TypeConverter
    fun fromGoalStatus(value: GoalStatus): String = value.name

    @TypeConverter
    fun toGoalStatus(value: String): GoalStatus = GoalStatus.valueOf(value)

    @TypeConverter
    fun fromContributionKind(value: ContributionKind): String = value.name

    @TypeConverter
    fun toContributionKind(value: String): ContributionKind = ContributionKind.valueOf(value)
}
