package com.justspent.service

import android.content.ContentResolver
import android.net.Uri
import com.justspent.domain.model.SmsTransaction
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class SmsParserService(private val contentResolver: ContentResolver) {

    /**
     * Reads SMS inbox mapping rows after `startAfterTimestamp` explicitly filtering for
     * messages containing "SBI" and "credited".
     */
    suspend fun readAndParseSms(startAfterTimestamp: Long): List<SmsTransaction> {
        return withContext(Dispatchers.IO) {
            val transactions = mutableListOf<SmsTransaction>()
            val uri = Uri.parse("content://sms/inbox")
            val projection = arrayOf("_id", "body", "date")
            val selection = "date > ?"
            val selectionArgs = arrayOf(startAfterTimestamp.toString())

            val cursor = contentResolver.query(
                uri,
                projection,
                selection,
                selectionArgs,
                "date ASC"
            )

            cursor?.use {
                val idIndex = it.getColumnIndexOrThrow("_id")
                val bodyIndex = it.getColumnIndexOrThrow("body")
                val dateIndex = it.getColumnIndexOrThrow("date")

                while (it.moveToNext()) {
                    val id = it.getString(idIndex) ?: ""
                    val body = it.getString(bodyIndex) ?: ""
                    val timestamp = it.getLong(dateIndex)

                    if (body.contains("SBI", ignoreCase = true) && body.contains("credited", ignoreCase = true)) {
                        val parsed = parseSms(id, body, timestamp)
                        if (parsed != null) {
                            transactions.add(parsed)
                        }
                    }
                }
            }
            transactions
        }
    }

    private fun parseSms(id: String, message: String, timestampMs: Long): SmsTransaction? {
        val amountRegex = "(?:Rs\\.?|INR)\\s*([0-9.]+)".toRegex()
        val dateRegex = "\\son\\s+([^\\s]+)\\s+transfer".toRegex()
        val nameRegex = "transfer from\\s+(.*?)\\s+Ref No".toRegex()

        val amountMatch = amountRegex.find(message)
        val dateMatch = dateRegex.find(message)
        val nameMatch = nameRegex.find(message)

        if (amountMatch != null && dateMatch != null && nameMatch != null) {
            val amountStr = amountMatch.groupValues[1]
            val amount = amountStr.toDoubleOrNull() ?: 0.0
            val dateStr = dateMatch.groupValues[1]
            val name = nameMatch.groupValues[1].trim()

            return SmsTransaction(
                id = id,
                amount = amount,
                dateString = dateStr,
                senderName = name,
                rawMessage = message,
                timestampMs = timestampMs
            )
        }
        return null
    }

    /**
     * Parses the date from the SMS body (e.g., "17Dec24") and combines it with 
     * the time-of-day from the arrival timestamp to create a final Long epoch.
     */
    fun parseTransactionDate(dateStr: String, arrivalTimestamp: Long): Long {
        return try {
            // SBI date format: ddMMMyy (e.g. 17Dec24)
            val sdf = java.text.SimpleDateFormat("ddMMMyy", java.util.Locale.US)
            val parsedDate = sdf.parse(dateStr)
            
            if (parsedDate != null) {
                val calendar = java.util.Calendar.getInstance().apply {
                    time = parsedDate
                    // Preserve the time-of-day from arrival stamp
                    val arrivalCal = java.util.Calendar.getInstance().apply { timeInMillis = arrivalTimestamp }
                    set(java.util.Calendar.HOUR_OF_DAY, arrivalCal.get(java.util.Calendar.HOUR_OF_DAY))
                    set(java.util.Calendar.MINUTE, arrivalCal.get(java.util.Calendar.MINUTE))
                    set(java.util.Calendar.SECOND, arrivalCal.get(java.util.Calendar.SECOND))
                }
                calendar.timeInMillis
            } else {
                arrivalTimestamp
            }
        } catch (e: Exception) {
            arrivalTimestamp
        }
    }
}
