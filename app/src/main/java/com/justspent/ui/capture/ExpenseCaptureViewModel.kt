package com.justspent.ui.capture

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.justspent.data.local.entity.Expense
import com.justspent.data.repository.ExpenseRepository
import kotlinx.coroutines.launch

class ExpenseCaptureViewModel(
    private val repository: ExpenseRepository,
    val sourceApp: String
) : ViewModel() {

    var amount by mutableStateOf("")
        private set

    var category by mutableStateOf("Food")
        private set

    var note by mutableStateOf("")
        private set

    var isSaving by mutableStateOf(false)
        private set

    val categories = listOf("Food", "Transport", "Shopping", "Bills", "Other")

    fun updateAmount(value: String) {
        // Only allow valid decimal input
        if (value.isEmpty() || value.matches(Regex("^\\d*\\.?\\d{0,2}$"))) {
            amount = value
        }
    }

    fun updateCategory(value: String) {
        category = value
    }

    fun updateNote(value: String) {
        note = value
    }

    fun saveExpense(onComplete: () -> Unit) {
        val amountValue = amount.toDoubleOrNull()
        if (amountValue == null || amountValue <= 0.0) return

        isSaving = true
        viewModelScope.launch {
            repository.insertExpense(
                Expense(
                    amount = amountValue,
                    category = category,
                    note = note.ifBlank { category },
                    sourceApp = sourceApp
                )
            )
            isSaving = false
            onComplete()
        }
    }

    val isValid: Boolean
        get() = amount.toDoubleOrNull()?.let { it > 0.0 } ?: false
}

class ExpenseCaptureViewModelFactory(
    private val repository: ExpenseRepository,
    private val sourceApp: String
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        @Suppress("UNCHECKED_CAST")
        return ExpenseCaptureViewModel(repository, sourceApp) as T
    }
}
