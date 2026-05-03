package com.justspent.domain.model

data class SmsTransaction(
    val id: String,
    val amount: Double,
    val dateString: String,
    val senderName: String,
    val rawMessage: String,
    val timestampMs: Long
)
