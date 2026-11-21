package com.example.pocketbudget.data.local

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.pocketbudget.data.Category
import com.example.pocketbudget.data.ContributionKind
import com.example.pocketbudget.data.GoalStatus
import com.example.pocketbudget.data.TransactionType

@Entity(tableName = "transactions")
data class TransactionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val amount: Double,
    val type: TransactionType,
    val category: Category,
    val createdAtMillis: Long,
)

@Entity(tableName = "savings_goals")
data class SavingsGoalEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val targetAmount: Double,
    val category: Category,
    val note: String,
    val status: GoalStatus,
    val createdAtMillis: Long,
    val completedAtMillis: Long?,
)

@Entity(
    tableName = "contributions",
    foreignKeys = [
        ForeignKey(
            entity = SavingsGoalEntity::class,
            parentColumns = ["id"],
            childColumns = ["goalId"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = TransactionEntity::class,
            parentColumns = ["id"],
            childColumns = ["transactionId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [
        Index("goalId"),
        Index(value = ["transactionId"], unique = true),
    ],
)
data class ContributionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val goalId: Long,
    val transactionId: Long,
    val amount: Double,
    val kind: ContributionKind,
    val note: String,
    val createdAtMillis: Long,
)

@Entity(tableName = "meta")
data class MetaEntity(
    @PrimaryKey val id: Int = 1,
    val seeded: Boolean,
)
