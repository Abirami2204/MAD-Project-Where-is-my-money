package com.justspent.ui.capture

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import android.app.Application
import android.provider.ContactsContract
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.justspent.data.local.entity.Expense
import com.justspent.data.repository.ExpenseRepository
import com.justspent.domain.model.RecipientTotal
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ExpenseCaptureViewModel(
    application: Application,
    private val repository: ExpenseRepository,
    val sourceApp: String
) : AndroidViewModel(application) {

    var amount by mutableStateOf("")
        private set

    var category by mutableStateOf("Food")
        private set

    var note by mutableStateOf("")
        private set

    var recipient by mutableStateOf("")
        private set

    var isSaving by mutableStateOf(false)
        private set

    private val _frequentRecipients = MutableStateFlow<List<RecipientTotal>>(emptyList())
    val frequentRecipients: StateFlow<List<RecipientTotal>> = _frequentRecipients.asStateFlow()

    init {
        // Load top 5 frequent recipients for quick selection
        viewModelScope.launch {
            repository.getTopRecipients(5).collect { list ->
                _frequentRecipients.value = list
            }
        }
    }

    private val _contactSuggestions = MutableStateFlow<List<String>>(emptyList())
    val contactSuggestions: StateFlow<List<String>> = _contactSuggestions.asStateFlow()

    fun loadContacts(query: String) {
        if (query.isBlank()) {
            _contactSuggestions.value = emptyList()
            return
        }

        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            val contacts = mutableListOf<String>()
            val contentResolver = getApplication<Application>().contentResolver
            
            val projection = arrayOf(ContactsContract.Contacts.DISPLAY_NAME_PRIMARY)
            val selection = "${ContactsContract.Contacts.DISPLAY_NAME_PRIMARY} LIKE ?"
            val selectionArgs = arrayOf("%$query%")
            
            try {
                contentResolver.query(
                    ContactsContract.Contacts.CONTENT_URI,
                    projection,
                    selection,
                    selectionArgs,
                    "${ContactsContract.Contacts.DISPLAY_NAME_PRIMARY} ASC"
                )?.use { cursor ->
                    val nameIndex = cursor.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME_PRIMARY)
                    while (cursor.moveToNext() && contacts.size < 5) {
                        if (nameIndex != -1) {
                            cursor.getString(nameIndex)?.let { name ->
                                if (!contacts.contains(name)) {
                                    contacts.add(name)
                                }
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                // Permission might be denied
                e.printStackTrace()
            }
            
            _contactSuggestions.value = contacts
        }
    }

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

    fun updateRecipient(value: String) {
        recipient = value
        loadContacts(value)
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
                    sourceApp = sourceApp,
                    recipient = recipient.trim()
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
    private val application: Application,
    private val repository: ExpenseRepository,
    private val sourceApp: String
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        @Suppress("UNCHECKED_CAST")
        return ExpenseCaptureViewModel(application, repository, sourceApp) as T
    }
}
