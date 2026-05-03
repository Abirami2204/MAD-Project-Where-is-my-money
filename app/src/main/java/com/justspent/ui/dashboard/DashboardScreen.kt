package com.justspent.ui.dashboard

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.justspent.data.local.entity.CategoryTotal
import com.justspent.data.local.entity.Expense
import com.justspent.ui.theme.*
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val categoryColors = mapOf(
    "Food" to CategoryFood,
    "Transport" to CategoryTransport,
    "Shopping" to CategoryShopping,
    "Bills" to CategoryBills,
    "Other" to CategoryOther
)

private val categoryEmojis = mapOf(
    "Food" to "🍕",
    "Transport" to "🚕",
    "Shopping" to "🛒",
    "Bills" to "📄",
    "Other" to "📦"
)

@Composable
fun DashboardScreen(
    viewModel: DashboardViewModel,
    onManualEntry: () -> Unit = {}
) {
    val totalSpending by viewModel.totalSpending.collectAsState(initial = 0.0)
    val categoryTotals by viewModel.categoryTotals.collectAsState(initial = emptyList())
    val groupedExpenses by viewModel.groupedExpenses.collectAsState(initial = emptyMap())
    val allExpenses by viewModel.allExpenses.collectAsState(initial = emptyList())

    val scrapedTransactions by viewModel.scrapedTransactions.collectAsState()
    val isSyncing by viewModel.isSyncing.collectAsState()
    val lastSyncDate by viewModel.lastSyncDate.collectAsState(initial = 0L)

    val context = LocalContext.current
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            viewModel.syncSms()
        }
    }

    val isDark = isSystemInDarkTheme()

    if (scrapedTransactions.isNotEmpty()) {
        SmsVerificationModal(
            transactions = scrapedTransactions,
            onDismiss = { viewModel.clearScrapedTransactions() },
            onSave = { selected ->
                viewModel.saveScrapedTransactions(selected)
            }
        )
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        floatingActionButton = {
            FloatingActionButton(
                onClick = onManualEntry,
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                shape = RoundedCornerShape(16.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add expense")
            }
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // ─── Top Spacing ───
            item { Spacer(modifier = Modifier.height(16.dp)) }

            // ─── Header ───
            item { 
                HeaderSection(
                    lastSyncDate = lastSyncDate,
                    isSyncing = isSyncing,
                    onSyncClicked = {
                        val hasPerm = ContextCompat.checkSelfPermission(
                            context,
                            Manifest.permission.READ_SMS
                        ) == PackageManager.PERMISSION_GRANTED
                        if (hasPerm) {
                            viewModel.syncSms()
                        } else {
                            permissionLauncher.launch(Manifest.permission.READ_SMS)
                        }
                    }
                ) 
            }

            // ─── Hero Card: Total Spent ───
            item { TotalSpentCard(totalSpending, allExpenses.size, isDark) }

            // ─── Category Breakdown ───
            if (categoryTotals.isNotEmpty()) {
                item { CategoryBreakdown(categoryTotals, totalSpending, isDark) }
            }

            // ─── Recent Transactions ───
            if (groupedExpenses.isNotEmpty()) {
                item {
                    Text(
                        text = "Recent Transactions",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }

                groupedExpenses.forEach { (dayLabel, expenses) ->
                    item {
                        Text(
                            text = dayLabel,
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }

                    items(expenses, key = { it.id }) { expense ->
                        TransactionItem(expense, isDark)
                    }
                }
            } else {
                item { EmptyState(isDark) }
            }

            // ─── Bottom padding for FAB ───
            item { Spacer(modifier = Modifier.height(80.dp)) }
        }
    }
}

// ═══════════════════════════════════════════════════════════
// HEADER
// ═══════════════════════════════════════════════════════════
@Composable
private fun HeaderSection(
    lastSyncDate: Long,
    isSyncing: Boolean,
    onSyncClicked: () -> Unit
) {
    val syncText = if (lastSyncDate == 0L) "Never Synced" else {
        val sdf = SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault())
        "Synced: ${sdf.format(Date(lastSyncDate))}"
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(
                text = "JustSpent",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
                letterSpacing = (-0.5).sp
            )
            Text(
                text = "Your financial sanctuary",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        
        // Sync Button Area
        Column(horizontalAlignment = Alignment.End) {
            IconButton(
                onClick = onSyncClicked,
                modifier = Modifier
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer)
            ) {
                if (isSyncing) {
                    androidx.compose.material3.CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.Sync,
                        contentDescription = "Sync SMS",
                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = syncText,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

// ═══════════════════════════════════════════════════════════
// HERO CARD
// ═══════════════════════════════════════════════════════════
@Composable
private fun TotalSpentCard(totalSpending: Double, transactionCount: Int, isDark: Boolean) {
    val gradientStart = if (isDark) DarkGradientStart else GradientStart
    val gradientEnd = if (isDark) DarkGradientEnd else GradientEnd

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    brush = Brush.linearGradient(
                        colors = listOf(gradientStart, gradientEnd)
                    ),
                    shape = RoundedCornerShape(20.dp)
                )
                .padding(28.dp)
        ) {
            Column {
                Text(
                    text = "Current Total Spent",
                    style = MaterialTheme.typography.labelLarge,
                    color = Color.White.copy(alpha = 0.75f),
                    letterSpacing = 0.5.sp
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "₹${String.format("%,.2f", totalSpending)}",
                    fontSize = 36.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    letterSpacing = (-1).sp
                )
                Spacer(modifier = Modifier.height(12.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF81C784))
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "$transactionCount transactions recorded",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.85f)
                    )
                }
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════
// CATEGORY BREAKDOWN
// ═══════════════════════════════════════════════════════════
@Composable
private fun CategoryBreakdown(categoryTotals: List<CategoryTotal>, totalSpending: Double, isDark: Boolean) {
    val cardColor = if (isDark) DarkSurfaceContainerLowest else SurfaceContainerLowest
    val trackColor = if (isDark) DarkSurfaceContainerHigh else SurfaceContainerHigh

    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            text = "Spending by Category",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(modifier = Modifier.height(4.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = cardColor),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                categoryTotals.forEach { catTotal ->
                    CategoryRow(catTotal, totalSpending, trackColor)
                }
            }
        }
    }
}

@Composable
private fun CategoryRow(catTotal: CategoryTotal, totalSpending: Double, trackColor: Color) {
    val fraction = if (totalSpending > 0) (catTotal.total / totalSpending).toFloat() else 0f
    val animatedFraction by animateFloatAsState(
        targetValue = fraction,
        animationSpec = tween(durationMillis = 800),
        label = "categoryProgress"
    )
    val color = categoryColors[catTotal.category] ?: CategoryOther
    val emoji = categoryEmojis[catTotal.category] ?: "📦"

    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(text = emoji, fontSize = 18.sp)
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = catTotal.category,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            Text(
                text = "₹${String.format("%,.0f", catTotal.total)}",
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }

        @Suppress("DEPRECATION")
        LinearProgressIndicator(
            progress = animatedFraction,
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp)),
            color = color,
            trackColor = trackColor,
            strokeCap = StrokeCap.Round
        )

        Text(
            text = "${String.format("%.0f", fraction * 100)}% of total",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

// ═══════════════════════════════════════════════════════════
// TRANSACTION ITEM
// ═══════════════════════════════════════════════════════════
@Composable
private fun TransactionItem(expense: Expense, isDark: Boolean) {
    val cardColor = if (isDark) DarkSurfaceContainerLowest else SurfaceContainerLowest
    val dateFormat = SimpleDateFormat("hh:mm a", Locale.getDefault())
    val emoji = categoryEmojis[expense.category] ?: "📦"
    val color = categoryColors[expense.category] ?: CategoryOther

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = cardColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Category icon circle
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(color.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Text(text = emoji, fontSize = 20.sp)
            }

            Spacer(modifier = Modifier.width(14.dp))

            // Description
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = expense.note.ifBlank { expense.category },
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1
                )
                Text(
                    text = "${dateFormat.format(Date(expense.timestamp))} • ${expense.sourceApp}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Amount
            val amountText = if (expense.isCredit) "+ ₹${String.format("%,.0f", expense.amount)}" else "- ₹${String.format("%,.0f", expense.amount)}"
            val amountColor = if (expense.isCredit) Color(0xFF81C784) else MaterialTheme.colorScheme.onSurface
            Text(
                text = amountText,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = amountColor
            )
        }
    }
}

// ═══════════════════════════════════════════════════════════
// EMPTY STATE
// ═══════════════════════════════════════════════════════════
@Composable
private fun EmptyState(isDark: Boolean) {
    val cardColor = if (isDark) DarkSurfaceContainerLowest else SurfaceContainerLowest

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = cardColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(text = "💫", fontSize = 40.sp)
            Text(
                text = "No expenses yet",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "Open a UPI app and close it — we'll catch it automatically.\nOr tap + to add one manually.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 16.dp),
                lineHeight = 20.sp
            )
        }
        }
    }
}

// ═══════════════════════════════════════════════════════════
// SMS VERIFICATION MODAL
// ═══════════════════════════════════════════════════════════
@Composable
fun SmsVerificationModal(
    transactions: List<com.justspent.domain.model.SmsTransaction>,
    onDismiss: () -> Unit,
    onSave: (List<com.justspent.domain.model.SmsTransaction>) -> Unit
) {
    var selectedIds by remember { mutableStateOf(transactions.map { it.id }.toSet()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "${transactions.size} Sync Records",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().clickable {
                        selectedIds = if (selectedIds.size == transactions.size) emptySet() else transactions.map { it.id }.toSet()
                    },
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = selectedIds.size == transactions.size && transactions.isNotEmpty(),
                        onCheckedChange = null
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = "Select All", fontWeight = FontWeight.SemiBold)
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                LazyColumn(modifier = Modifier.weight(1f, fill = false)) {
                    items(transactions, key = { it.id }) { t ->
                        val isSelected = selectedIds.contains(t.id)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    val newSet = selectedIds.toMutableSet()
                                    if (isSelected) newSet.remove(t.id) else newSet.add(t.id)
                                    selectedIds = newSet
                                }
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(checked = isSelected, onCheckedChange = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = "₹${t.amount} from ${t.senderName}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    text = t.dateString,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { 
                    val selectedTransactions = transactions.filter { it.id in selectedIds }
                    onSave(selectedTransactions) 
                }
            ) {
                Text("Save Selected")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
