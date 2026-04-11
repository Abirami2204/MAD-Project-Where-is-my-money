package com.justspent.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.justspent.data.local.entity.CategoryTotal
import com.justspent.data.local.entity.Expense
import com.justspent.data.repository.ExpenseRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class DashboardViewModel(private val repository: ExpenseRepository) : ViewModel() {

    val allExpenses: Flow<List<Expense>> = repository.getAllExpenses()
    val totalSpending: Flow<Double> = repository.getTotalSpending().map { it ?: 0.0 }
    val categoryTotals: Flow<List<CategoryTotal>> = repository.getTotalSpendingByCategory()

    /**
     * Groups expenses by relative day labels: "Today", "Yesterday", or formatted date.
     */
    val groupedExpenses: Flow<Map<String, List<Expense>>> = allExpenses.map { expenses ->
        val now = System.currentTimeMillis()
        val todayStart = now - (now % 86400000)
        val yesterdayStart = todayStart - 86400000

        expenses.groupBy { expense ->
            when {
                expense.timestamp >= todayStart -> "Today"
                expense.timestamp >= yesterdayStart -> "Yesterday"
                else -> {
                    val sdf = java.text.SimpleDateFormat("dd MMM yyyy", java.util.Locale.getDefault())
                    sdf.format(java.util.Date(expense.timestamp))
                }
            }
        }
    }
}

class DashboardViewModelFactory(
    private val repository: ExpenseRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        @Suppress("UNCHECKED_CAST")
        return DashboardViewModel(repository) as T
    }
}
