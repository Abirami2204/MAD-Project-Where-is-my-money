package com.justspent

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.ViewModelProvider
import com.justspent.service.ServiceController
import com.justspent.ui.capture.ExpenseCaptureActivity
import com.justspent.ui.dashboard.DashboardScreen
import com.justspent.ui.dashboard.DashboardViewModel
import com.justspent.ui.dashboard.DashboardViewModelFactory
import com.justspent.ui.setup.UsageAccessScreen
import com.justspent.ui.theme.JustSpentTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val app = application as JustSpentApp
        val preferencesRepository = com.justspent.data.local.PreferencesRepository(this)
        val smsParserService = com.justspent.service.SmsParserService(contentResolver)
        
        val dashboardViewModel = ViewModelProvider(
            this,
            DashboardViewModelFactory(app.repository, preferencesRepository, smsParserService)
        )[DashboardViewModel::class.java]

        setContent {
            JustSpentTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    var hasUsageAccess by remember {
                        mutableStateOf(ServiceController.hasUsageAccessPermission(this))
                    }

                    if (!hasUsageAccess) {
                        UsageAccessScreen(
                            onPermissionGranted = { hasUsageAccess = true }
                        )
                    } else {
                        LaunchedEffect(Unit) {
                            ServiceController.startMonitoring(this@MainActivity)

                            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                                if (checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                                    requestPermissions(arrayOf(android.Manifest.permission.POST_NOTIFICATIONS), 101)
                                }
                            }
                        }
                        DashboardScreen(
                            viewModel = dashboardViewModel,
                            onManualEntry = {
                                val intent = Intent(this@MainActivity, ExpenseCaptureActivity::class.java)
                                intent.putExtra("SOURCE_APP", "Manual")
                                startActivity(intent)
                            }
                        )
                    }
                }
            }
        }
    }
}
