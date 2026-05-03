package com.justspent.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.justspent.data.local.entity.CategoryTotal
import com.justspent.data.local.entity.Expense
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for the expenses table.
 * All read operations return [Flow] for reactive UI updates via Compose.
 */
@Dao
interface ExpenseDao {

    /**
     * Insert a new expense. Returns the auto-generated row ID.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExpense(expense: Expense): Long

    /**
     * Get all expenses ordered by most recent first.
     */
    @Query("SELECT * FROM expenses ORDER BY timestamp DESC")
    fun getAllExpenses(): Flow<List<Expense>>

    /**
     * Get expenses within a specific date range (inclusive).
     *
     * @param startTime Start of range in epoch milliseconds.
     * @param endTime End of range in epoch milliseconds.
     */
    @Query("SELECT * FROM expenses WHERE timestamp BETWEEN :startTime AND :endTime ORDER BY timestamp DESC")
    fun getExpensesByDateRange(startTime: Long, endTime: Long): Flow<List<Expense>>

    /**
     * Get the sum of all expenses (debits). Returns null if no expenses exist.
     */
    @Query("SELECT SUM(amount) FROM expenses WHERE isCredit = 0")
    fun getTotalSpending(): Flow<Double?>

    /**
     * Get the sum of all income (credits). Returns null if no income exist.
     */
    @Query("SELECT SUM(amount) FROM expenses WHERE isCredit = 1")
    fun getTotalIncome(): Flow<Double?>

    /**
     * Get total spending grouped by category (expenses only), ordered by highest spending first.
     */
    @Query("SELECT category, SUM(amount) as total FROM expenses WHERE isCredit = 0 GROUP BY category ORDER BY total DESC")
    fun getTotalSpendingByCategory(): Flow<List<CategoryTotal>>

    /**
     * Advanced filtering for transaction history.
     */
    @Query("""
        SELECT * FROM expenses 
        WHERE (:category IS NULL OR category = :category)
        AND (:isCredit IS NULL OR isCredit = :isCredit)
        AND (note LIKE '%' || :query || '%' OR category LIKE '%' || :query || '%' OR recipient LIKE '%' || :query || '%')
        AND (timestamp BETWEEN :startTime AND :endTime)
        ORDER BY timestamp DESC
    """)
    fun getFilteredExpenses(
        query: String = "",
        category: String? = null,
        isCredit: Boolean? = null,
        startTime: Long = 0L,
        endTime: Long = Long.MAX_VALUE
    ): Flow<List<Expense>>

    /**
     * Delete a single expense.
     */
    @Delete
    suspend fun deleteExpense(expense: Expense)

    /**
     * Get all synced SMS IDs to avoid duplicates.
     */
    @Query("SELECT smsId FROM expenses WHERE smsId IS NOT NULL")
    suspend fun getAllSyncedSmsIds(): List<String>
}
