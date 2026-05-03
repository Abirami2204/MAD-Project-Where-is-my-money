package com.justspent.ui.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.justspent.data.local.entity.Expense
import com.justspent.data.repository.ExpenseRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn

@OptIn(ExperimentalCoroutinesApi::class)
class TransactionHistoryViewModel(private val repository: ExpenseRepository) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery

    private val _selectedCategory = MutableStateFlow<String?>(null)
    val selectedCategory: StateFlow<String?> = _selectedCategory

    private val _selectedType = MutableStateFlow<Boolean?>(null) // null = All, true = Income, false = Expense
    val selectedType: StateFlow<Boolean?> = _selectedType

    private val _dateRange = MutableStateFlow(0L to Long.MAX_VALUE)
    val dateRange: StateFlow<Pair<Long, Long>> = _dateRange

    /**
     * The filtered list of transactions based on all active filters.
     */
    val filteredTransactions: StateFlow<List<Expense>> = combine(
        _searchQuery,
        _selectedCategory,
        _selectedType,
        _dateRange
    ) { query, category, isCredit, range ->
        repository.getFilteredExpenses(
            query = query,
            category = category,
            isCredit = isCredit,
            startTime = range.first,
            endTime = range.second
        )
    }.flatMapLatest { it }
    .stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    fun onSearchQueryChanged(newQuery: String) {
        _searchQuery.value = newQuery
    }

    fun onCategorySelected(category: String?) {
        _selectedCategory.value = category
    }

    fun onTypeSelected(isCredit: Boolean?) {
        _selectedType.value = isCredit
    }

    fun onDateRangeSelected(start: Long, end: Long) {
        _dateRange.value = start to end
    }
}

class TransactionHistoryViewModelFactory(
    private val repository: ExpenseRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        @Suppress("UNCHECKED_CAST")
        return TransactionHistoryViewModel(repository) as T
    }
}
