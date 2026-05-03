package com.justspent.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Represents a single expense transaction recorded by the user.
 *
 * @property id Auto-generated primary key.
 * @property amount The monetary amount of the expense.
 * @property category The expense category (e.g., "Food", "Transport").
 * @property note An optional user-provided note.
 * @property sourceApp The UPI app that triggered the capture (e.g., "Google Pay").
 * @property isCredit True if this is an incoming transaction (credit), false if outgoing (debit/expense).
 * @property timestamp The time the expense was recorded, in epoch milliseconds.
 */
@Entity(tableName = "expenses")
data class Expense(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val amount: Double,
    val category: String,
    val note: String = "",
    val sourceApp: String = "",
    val isCredit: Boolean = false,
    val timestamp: Long = System.currentTimeMillis()
)

/**
 * Data class used as a return type for Room's GROUP BY queries.
 * Holds the total spending for a single category.
 */
data class CategoryTotal(
    val category: String,
    val total: Double
)
