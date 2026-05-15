package com.justspent.ui.capture

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.lifecycle.ViewModelProvider
import com.justspent.JustSpentApp
import com.justspent.ui.theme.JustSpentTheme

class ExpenseCaptureActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val sourceApp = intent.getStringExtra("SOURCE_APP") ?: "Unknown App"
        val app = application as JustSpentApp
        
        val viewModel = ViewModelProvider(
            this,
            ExpenseCaptureViewModelFactory(app, app.repository, sourceApp)
        )[ExpenseCaptureViewModel::class.java]

        setContent {
            JustSpentTheme {
                ExpenseCaptureScreen(
                    viewModel = viewModel,
                    onDismiss = { finish() }
                )
            }
        }
    }
}
