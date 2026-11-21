package com.example.pocketbudget.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(
    entities = [
        TransactionEntity::class,
        SavingsGoalEntity::class,
        ContributionEntity::class,
        MetaEntity::class,
    ],
    version = 1,
    exportSchema = false,
)
@TypeConverters(Converters::class)
abstract class PocketBudgetDatabase : RoomDatabase() {
    abstract fun transactionDao(): TransactionDao
    abstract fun savingsGoalDao(): SavingsGoalDao
    abstract fun contributionDao(): ContributionDao
    abstract fun metaDao(): MetaDao

    companion object {
        @Volatile
        private var instance: PocketBudgetDatabase? = null

        fun get(context: Context): PocketBudgetDatabase {
            return instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    PocketBudgetDatabase::class.java,
                    "pocket_budget.db",
                ).fallbackToDestructiveMigration(dropAllTables = true)
                    .build()
                    .also { instance = it }
            }
        }
    }
}
