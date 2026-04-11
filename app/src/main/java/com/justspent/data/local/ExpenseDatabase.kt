package com.justspent.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.justspent.data.local.dao.ExpenseDao
import com.justspent.data.local.entity.Expense

/**
 * Room database for JustSpent.
 * Uses a singleton pattern to ensure only one instance exists app-wide.
 */
@Database(
    entities = [Expense::class],
    version = 1,
    exportSchema = false
)
abstract class ExpenseDatabase : RoomDatabase() {

    abstract fun expenseDao(): ExpenseDao

    companion object {
        @Volatile
        private var INSTANCE: ExpenseDatabase? = null

        /**
         * Returns the singleton database instance, creating it if necessary.
         * Thread-safe via double-checked locking.
         */
        fun getDatabase(context: Context): ExpenseDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    ExpenseDatabase::class.java,
                    "justspent_database"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
