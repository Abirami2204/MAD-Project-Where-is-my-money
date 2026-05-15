package com.justspent.domain.model

/**
 * Holds monthly income and expense totals for bar chart visualization.
 * @property yearMonth Format: "2024-01", "2024-02", etc.
 */
data class MonthlyTotal(
    val yearMonth: String,
    val income: Double,
    val expense: Double
)

/**
 * Holds a recipient's total transaction amount and count.
 */
data class RecipientTotal(
    val recipient: String,
    val total: Double,
    val count: Int
)
