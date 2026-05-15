package com.justspent.ui.analytics

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.justspent.data.local.entity.CategoryTotal
import com.justspent.data.repository.ExpenseRepository
import com.justspent.domain.model.MonthlyTotal
import com.justspent.domain.model.RecipientTotal
import kotlinx.coroutines.flow.*

class AnalyticsViewModel(private val repository: ExpenseRepository) : ViewModel() {

    val categoryTotals: StateFlow<List<CategoryTotal>> = repository.getTotalSpendingByCategory()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val totalSpending: StateFlow<Double> = repository.getTotalSpending()
        .map { it ?: 0.0 }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val totalIncome: StateFlow<Double> = repository.getTotalIncome()
        .map { it ?: 0.0 }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val expenseCount: StateFlow<Int> = repository.getExpenseCount()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val topRecipients: StateFlow<List<RecipientTotal>> = repository.getTopRecipients(5)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    /**
     * Computes monthly income vs expense totals from all transactions.
     * Groups by year-month and aggregates credits/debits separately.
     */
    val monthlyTotals: StateFlow<List<MonthlyTotal>> = repository.getAllExpenses()
        .map { expenses ->
            val sdf = java.text.SimpleDateFormat("yyyy-MM", java.util.Locale.getDefault())
            val grouped = expenses.groupBy { sdf.format(java.util.Date(it.timestamp)) }

            grouped.map { (yearMonth, items) ->
                MonthlyTotal(
                    yearMonth = yearMonth,
                    income = items.filter { it.isCredit }.sumOf { it.amount },
                    expense = items.filter { !it.isCredit }.sumOf { it.amount }
                )
            }.sortedBy { it.yearMonth }
                .takeLast(6) // Show last 6 months
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    /**
     * Daily average spending = Total Spending / number of distinct days with transactions.
     */
    val dailyAverage: StateFlow<Double> = repository.getAllExpenses()
        .map { expenses ->
            val debits = expenses.filter { !it.isCredit }
            if (debits.isEmpty()) return@map 0.0

            val sdf = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
            val distinctDays = debits.map { sdf.format(java.util.Date(it.timestamp)) }.distinct().size
            if (distinctDays == 0) 0.0 else debits.sumOf { it.amount } / distinctDays
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)
}

class AnalyticsViewModelFactory(
    private val repository: ExpenseRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        @Suppress("UNCHECKED_CAST")
        return AnalyticsViewModel(repository) as T
    }
}
