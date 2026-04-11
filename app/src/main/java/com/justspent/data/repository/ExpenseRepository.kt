package com.justspent.data.repository

import com.justspent.data.local.dao.ExpenseDao
import com.justspent.data.local.entity.CategoryTotal
import com.justspent.data.local.entity.Expense
import kotlinx.coroutines.flow.Flow

/**
 * Repository layer that acts as the single source of truth for expense data.
 * Wraps [ExpenseDao] and exposes all operations to ViewModels.
 *
 * In this MVP we only have a local data source (Room).
 * If a remote API is added later, this class would coordinate between them.
 */
class ExpenseRepository(private val expenseDao: ExpenseDao) {

    /** Insert a new expense. Returns the auto-generated row ID. */
    suspend fun insertExpense(expense: Expense): Long {
        return expenseDao.insertExpense(expense)
    }

    /** Reactive stream of all expenses, ordered by most recent first. */
    fun getAllExpenses(): Flow<List<Expense>> {
        return expenseDao.getAllExpenses()
    }

    /** Reactive stream of expenses within a date range. */
    fun getExpensesByDateRange(startTime: Long, endTime: Long): Flow<List<Expense>> {
        return expenseDao.getExpensesByDateRange(startTime, endTime)
    }

    /** Reactive stream of total spending across all expenses. */
    fun getTotalSpending(): Flow<Double?> {
        return expenseDao.getTotalSpending()
    }

    /** Reactive stream of spending totals grouped by category. */
    fun getTotalSpendingByCategory(): Flow<List<CategoryTotal>> {
        return expenseDao.getTotalSpendingByCategory()
    }

    /** Delete a single expense. */
    suspend fun deleteExpense(expense: Expense) {
        expenseDao.deleteExpense(expense)
    }
}
