package com.justspent.ui.analytics

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.TrendingDown
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.justspent.domain.model.RecipientTotal
import com.justspent.ui.analytics.charts.DonutChart
import com.justspent.ui.analytics.charts.DonutLegend
import com.justspent.ui.analytics.charts.MonthlyBarChart
import com.justspent.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnalyticsScreen(
    viewModel: AnalyticsViewModel,
    onBack: () -> Unit
) {
    val categoryTotals by viewModel.categoryTotals.collectAsState()
    val totalSpending by viewModel.totalSpending.collectAsState()
    val totalIncome by viewModel.totalIncome.collectAsState()
    val expenseCount by viewModel.expenseCount.collectAsState()
    val topRecipients by viewModel.topRecipients.collectAsState()
    val monthlyTotals by viewModel.monthlyTotals.collectAsState()
    val dailyAverage by viewModel.dailyAverage.collectAsState()

    val isDark = isSystemInDarkTheme()
    val cardColor = if (isDark) DarkSurfaceContainerLowest else SurfaceContainerLowest

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Spend Analytics", fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(bottom = 32.dp)
        ) {
            // ─── Quick Stats Row ───
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    StatCard(
                        title = "Total Spent",
                        value = "₹${String.format("%,.0f", totalSpending)}",
                        icon = Icons.Default.TrendingDown,
                        iconTint = Color(0xFFE57373),
                        cardColor = cardColor,
                        modifier = Modifier.weight(1f)
                    )
                    StatCard(
                        title = "Daily Avg",
                        value = "₹${String.format("%,.0f", dailyAverage)}",
                        icon = Icons.Default.Analytics,
                        iconTint = DarkPrimary,
                        cardColor = cardColor,
                        modifier = Modifier.weight(1f)
                    )
                    StatCard(
                        title = "Payments",
                        value = "$expenseCount",
                        icon = Icons.Default.Receipt,
                        iconTint = Color(0xFF81C784),
                        cardColor = cardColor,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // ─── Category Breakdown ───
            item {
                SectionCard(title = "Where Your Money Goes", cardColor = cardColor) {
                    DonutChart(
                        data = categoryTotals,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(220.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    DonutLegend(
                        data = categoryTotals,
                        modifier = Modifier.padding(horizontal = 8.dp)
                    )
                }
            }

            // ─── Monthly Trends ───
            item {
                SectionCard(title = "Monthly Trends", cardColor = cardColor) {
                    MonthlyBarChart(
                        data = monthlyTotals,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp)
                    )
                }
            }

            // ─── Income vs Expense Summary ───
            item {
                SectionCard(title = "Balance Overview", cardColor = cardColor) {
                    val net = totalIncome - totalSpending
                    val netColor = if (net >= 0) Color(0xFF81C784) else Color(0xFFE57373)
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("Income", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("₹${String.format("%,.0f", totalIncome)}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Color(0xFF81C784))
                        }
                        Column {
                            Text("Expense", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("₹${String.format("%,.0f", totalSpending)}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Color(0xFFE57373))
                        }
                        Column {
                            Text("Net", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(
                                text = "${if (net >= 0) "+" else ""}₹${String.format("%,.0f", net)}",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = netColor
                            )
                        }
                    }

                    if (totalIncome + totalSpending > 0) {
                        Spacer(modifier = Modifier.height(12.dp))
                        val incomeRatio = (totalIncome / (totalIncome + totalSpending)).toFloat()
                        Row(modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp))) {
                            Box(modifier = Modifier.weight(incomeRatio.coerceAtLeast(0.01f)).fillMaxHeight().background(Color(0xFF81C784)))
                            Box(modifier = Modifier.weight((1f - incomeRatio).coerceAtLeast(0.01f)).fillMaxHeight().background(Color(0xFFE57373)))
                        }
                    }
                }
            }

            // ─── Top Recipients ───
            item {
                SectionCard(title = "Top Recipients", cardColor = cardColor) {
                    if (topRecipients.isEmpty()) {
                        Text(
                            "No recipient data yet",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(vertical = 16.dp)
                        )
                    } else {
                        val maxAmount = topRecipients.maxOf { it.total }
                        topRecipients.forEachIndexed { index, recipient ->
                            RecipientRow(
                                rank = index + 1,
                                recipient = recipient,
                                maxAmount = maxAmount
                            )
                            if (index < topRecipients.lastIndex) {
                                Spacer(modifier = Modifier.height(8.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════
// REUSABLE COMPONENTS
// ═══════════════════════════════════════════════════════════

@Composable
private fun StatCard(
    title: String,
    value: String,
    icon: ImageVector,
    iconTint: Color,
    cardColor: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = cardColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconTint,
                modifier = Modifier.size(20.dp)
            )
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1
            )
            Text(
                text = title,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun SectionCard(
    title: String,
    cardColor: Color,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = cardColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(12.dp))
            content()
        }
    }
}

@Composable
private fun RecipientRow(
    rank: Int,
    recipient: RecipientTotal,
    maxAmount: Double
) {
    val fraction = (recipient.total / maxAmount).toFloat()
    val animatedFraction = remember { Animatable(0f) }
    LaunchedEffect(fraction) {
        animatedFraction.animateTo(fraction, animationSpec = tween(600))
    }

    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "#$rank  ${recipient.recipient}",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f)
            )
            Text(
                text = "₹${String.format("%,.0f", recipient.total)} (${recipient.count}x)",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        @Suppress("DEPRECATION")
        LinearProgressIndicator(
            progress = animatedFraction.value,
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp)),
            color = Primary,
            trackColor = MaterialTheme.colorScheme.surfaceVariant
        )
    }
}
