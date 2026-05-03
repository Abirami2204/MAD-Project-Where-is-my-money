package com.justspent.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.justspent.data.local.PreferencesRepository
import com.justspent.data.local.entity.CategoryTotal
import com.justspent.data.local.entity.Expense
import com.justspent.data.repository.ExpenseRepository
import com.justspent.domain.model.SmsTransaction
import com.justspent.service.SmsParserService
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

class DashboardViewModel(
    private val repository: ExpenseRepository,
    private val prefs: PreferencesRepository,
    private val smsService: SmsParserService
) : ViewModel() {

    val allExpenses: Flow<List<Expense>> = repository.getAllExpenses()
    val totalSpending: Flow<Double> = repository.getTotalSpending().map { it ?: 0.0 }
    val categoryTotals: Flow<List<CategoryTotal>> = repository.getTotalSpendingByCategory()

    // SMS Sync states
    val lastSyncDate: Flow<Long> = prefs.lastSmsSyncTimestamp
    
    private val _isSyncing = MutableStateFlow(false)
    val isSyncing: StateFlow<Boolean> = _isSyncing

    private val _scrapedTransactions = MutableStateFlow<List<SmsTransaction>>(emptyList())
    val scrapedTransactions: StateFlow<List<SmsTransaction>> = _scrapedTransactions

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

    fun syncSms() {
        viewModelScope.launch {
            _isSyncing.value = true
            val startAfter = prefs.lastSmsSyncTimestamp.first()
            val parsed = smsService.readAndParseSms(startAfter)
            _scrapedTransactions.value = parsed
            _isSyncing.value = false
        }
    }

    fun clearScrapedTransactions() {
        _scrapedTransactions.value = emptyList()
    }

    fun saveScrapedTransactions(transactions: List<SmsTransaction>) {
        viewModelScope.launch {
            transactions.forEach { t ->
                val expense = Expense(
                    amount = t.amount,
                    category = "Income", // Or Bank Transfer, since it's credited
                    note = t.senderName.takeIf { it.isNotBlank() } ?: "SMS Sync",
                    sourceApp = "SBI (SMS)",
                    isCredit = true,
                    timestamp = t.timestampMs
                )
                repository.insertExpense(expense)
            }
            
            // Advance the last sync timestamp to the highest one among the saved
            if (transactions.isNotEmpty()) {
                val latest = transactions.maxOf { it.timestampMs }
                prefs.updateLastSmsSyncTimestamp(latest)
            }
            clearScrapedTransactions()
        }
    }
}

class DashboardViewModelFactory(
    private val repository: ExpenseRepository,
    private val preferencesRepository: PreferencesRepository,
    private val smsParserService: SmsParserService
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        @Suppress("UNCHECKED_CAST")
        return DashboardViewModel(repository, preferencesRepository, smsParserService) as T
    }
}
