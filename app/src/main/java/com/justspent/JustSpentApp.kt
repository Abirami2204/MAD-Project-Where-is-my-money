package com.justspent

import android.app.Application
import com.justspent.data.local.ExpenseDatabase
import com.justspent.data.repository.ExpenseRepository

/**
 * Application class for JustSpent.
 * Provides manual dependency injection by lazily initializing
 * the database and repository as singletons.
 *
 * ViewModels access these via: (application as JustSpentApp).repository
 */
class JustSpentApp : Application() {

    /** Lazily initialized Room database instance. */
    val database: ExpenseDatabase by lazy {
        ExpenseDatabase.getDatabase(this)
    }

    /** Lazily initialized repository — the single source of truth for data access. */
    val repository: ExpenseRepository by lazy {
        ExpenseRepository(database.expenseDao())
    }
}
