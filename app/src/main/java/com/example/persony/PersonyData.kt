package com.example.persony

import androidx.room.*
import kotlinx.coroutines.flow.Flow

// --- ENTITIES ---

@androidx.room.Entity(tableName = "transactions")
data class TransactionEntity(
    @androidx.room.PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val date: String,
    val amount: String,
    val isExpense: Boolean
)

@androidx.room.Entity(tableName = "bills")
data class BillEntity(
    @androidx.room.PrimaryKey val id: String,
    val title: String,
    val price: String,
    val date: String,
    val recurrence: String
)

@androidx.room.Entity(tableName = "savings")
data class SavingEntity(
    @androidx.room.PrimaryKey val id: String,
    val name: String,
    val target: Long,
    val currentAmount: Long,
    val location: String,
    val iconName: String
)

@androidx.room.Entity(tableName = "user_prefs")
data class UserPrefs(
    @androidx.room.PrimaryKey val id: Int = 1,
    val userName: String = "User Baru",
    val dailyBudget: Long = 0L,
    val totalBalance: Long = 14570800L
)

// --- DAO ---

@androidx.room.Dao
interface PersonyDao {
    @androidx.room.Query("SELECT * FROM transactions ORDER BY id DESC")
    fun getAllTransactions(): Flow<List<TransactionEntity>>

    @androidx.room.Insert(onConflict = androidx.room.OnConflictStrategy.REPLACE)
    suspend fun insertTransaction(tx: TransactionEntity)

    @androidx.room.Delete
    suspend fun deleteTransaction(tx: TransactionEntity)

    @androidx.room.Query("SELECT * FROM bills ORDER BY date ASC")
    fun getAllBills(): Flow<List<BillEntity>>

    @androidx.room.Insert(onConflict = androidx.room.OnConflictStrategy.REPLACE)
    suspend fun upsertBill(bill: BillEntity)

    @androidx.room.Delete
    suspend fun deleteBill(bill: BillEntity)

    @androidx.room.Query("SELECT * FROM savings")
    fun getAllSavings(): Flow<List<SavingEntity>>

    @androidx.room.Insert(onConflict = androidx.room.OnConflictStrategy.REPLACE)
    suspend fun upsertSaving(saving: SavingEntity)

    @androidx.room.Query("DELETE FROM savings WHERE id = :id")
    suspend fun deleteSaving(id: String)

    @androidx.room.Query("SELECT * FROM user_prefs WHERE id = 1")
    fun getUserPrefs(): Flow<UserPrefs?>

    @androidx.room.Insert(onConflict = androidx.room.OnConflictStrategy.REPLACE)
    suspend fun updateUserPrefs(prefs: UserPrefs)

    @androidx.room.Query("DELETE FROM transactions")
    suspend fun clearTransactions()

    @androidx.room.Query("DELETE FROM bills")
    suspend fun clearBills()

    @androidx.room.Query("DELETE FROM savings")
    suspend fun clearSavings()
}

// --- DATABASE ---

@androidx.room.Database(entities = [TransactionEntity::class, BillEntity::class, SavingEntity::class, UserPrefs::class], version = 1)
abstract class AppDatabase : androidx.room.RoomDatabase() {
    abstract fun dao(): PersonyDao
}