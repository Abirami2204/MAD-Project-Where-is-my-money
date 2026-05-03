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
    val totalIncome: Flow<Double> = repository.getTotalIncome().map { it ?: 0.0 }
    
    val netBalance: Flow<Double> = totalIncome.map { income ->
        val spent = repository.getTotalSpending().first() ?: 0.0
        income - spent
    }
    
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
            
            // Filter out any that are already in our database
            val existingIds = repository.getAllSyncedSmsIds()
            val filtered = parsed.filterNot { it.id in existingIds }
            
            _scrapedTransactions.value = filtered
            _isSyncing.value = false
        }
    }

    fun clearScrapedTransactions() {
        _scrapedTransactions.value = emptyList()
    }

    fun saveScrapedTransactions(selected: List<SmsTransaction>) {
        val allCurrent = _scrapedTransactions.value
        
        viewModelScope.launch {
            // 1. Save selected transactions
            selected.forEach { t ->
                val finalTimestamp = smsService.parseTransactionDate(t.dateString, t.timestampMs)
                
                val expense = Expense(
                    amount = t.amount,
                    category = "Income", 
                    note = t.senderName.trim(), // Removed [date] tag
                    recipient = t.senderName.trim(), // Populate new recipient field
                    sourceApp = "SBI (SMS)",
                    isCredit = true,
                    smsId = t.id, // Track SMS ID to prevent duplicates
                    timestamp = finalTimestamp
                )
                repository.insertExpense(expense)
            }
            
            // 2. Smart Pointer Logic: Advance lastSyncDate but preserve gaps
            // If the user skipped transactions, we advance the pointer ONLY to the 
            // oldest skipped transaction so we find it again in the next sync.
            val selectedIds = selected.map { it.id }.toSet()
            val skipped = allCurrent.filterNot { it.id in selectedIds }
            
            if (skipped.isNotEmpty()) {
                val oldestSkipped = skipped.minOf { it.timestampMs }
                // Use oldest skipped - 1 to ensure it's captured by "date > ?" query next time
                prefs.updateLastSmsSyncTimestamp(oldestSkipped - 1)
            } else if (allCurrent.isNotEmpty()) {
                // Everything saved, advance to the latest arrival time
                val latest = allCurrent.maxOf { it.timestampMs }
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
