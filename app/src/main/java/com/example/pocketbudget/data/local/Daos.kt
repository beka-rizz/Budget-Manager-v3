package com.example.pocketbudget.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface TransactionDao {
    @Query("SELECT * FROM transactions")
    fun observeAll(): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions WHERE id = :id")
    fun observeById(id: Long): Flow<TransactionEntity?>

    @Query("SELECT * FROM transactions WHERE id = :id")
    suspend fun getById(id: Long): TransactionEntity?

    @Insert
    suspend fun insert(entity: TransactionEntity): Long

    @Update
    suspend fun update(entity: TransactionEntity)

    @Query("DELETE FROM transactions WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("SELECT COUNT(*) FROM transactions")
    suspend fun count(): Int
}

@Dao
interface SavingsGoalDao {
    @Query("SELECT * FROM savings_goals")
    fun observeAll(): Flow<List<SavingsGoalEntity>>

    @Query("SELECT * FROM savings_goals WHERE id = :id")
    fun observeById(id: Long): Flow<SavingsGoalEntity?>

    @Query("SELECT * FROM savings_goals WHERE id = :id")
    suspend fun getById(id: Long): SavingsGoalEntity?

    @Insert
    suspend fun insert(entity: SavingsGoalEntity): Long

    @Update
    suspend fun update(entity: SavingsGoalEntity)

    @Query("DELETE FROM savings_goals WHERE id = :id")
    suspend fun deleteById(id: Long)
}

@Dao
interface ContributionDao {
    @Query("SELECT * FROM contributions")
    fun observeAll(): Flow<List<ContributionEntity>>

    @Query("SELECT * FROM contributions WHERE goalId = :goalId")
    fun observeByGoal(goalId: Long): Flow<List<ContributionEntity>>

    @Query("SELECT * FROM contributions WHERE goalId = :goalId")
    suspend fun listByGoal(goalId: Long): List<ContributionEntity>

    @Query("SELECT * FROM contributions WHERE transactionId = :transactionId LIMIT 1")
    fun observeByTransaction(transactionId: Long): Flow<ContributionEntity?>

    @Query("SELECT * FROM contributions WHERE transactionId = :transactionId LIMIT 1")
    suspend fun getByTransaction(transactionId: Long): ContributionEntity?

    @Insert
    suspend fun insert(entity: ContributionEntity): Long
}

@Dao
interface MetaDao {
    @Query("SELECT * FROM meta WHERE id = 1")
    suspend fun get(): MetaEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: MetaEntity)
}
