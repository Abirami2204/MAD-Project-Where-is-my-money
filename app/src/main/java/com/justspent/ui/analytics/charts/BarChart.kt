package com.justspent.ui.analytics.charts

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.justspent.domain.model.MonthlyTotal

private val IncomeColor = Color(0xFF81C784)
private val ExpenseColor = Color(0xFFE57373)

/**
 * Custom bar chart showing income (green) vs expense (red) per month.
 * Bars animate upward on first composition.
 */
@Composable
fun MonthlyBarChart(
    data: List<MonthlyTotal>,
    modifier: Modifier = Modifier
) {
    if (data.isEmpty()) {
        Box(modifier = modifier.fillMaxWidth().height(180.dp), contentAlignment = Alignment.Center) {
            Text(
                text = "No monthly data yet",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        return
    }

    val maxValue = data.maxOf { maxOf(it.income, it.expense) }.coerceAtLeast(1.0)

    val animationProgress = remember { Animatable(0f) }
    LaunchedEffect(data) {
        animationProgress.snapTo(0f)
        animationProgress.animateTo(1f, animationSpec = tween(durationMillis = 800))
    }

    Column(modifier = modifier.fillMaxWidth()) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(180.dp)
                .padding(horizontal = 8.dp)
        ) {
            val barGroupWidth = size.width / data.size
            val barWidth = barGroupWidth * 0.3f
            val gapBetweenBars = 4.dp.toPx()
            val cornerRadius = CornerRadius(6f, 6f)

            data.forEachIndexed { index, item ->
                val groupX = index * barGroupWidth + barGroupWidth * 0.15f

                // Income bar (left)
                val incomeHeight = (item.income / maxValue * size.height * animationProgress.value).toFloat()
                drawRoundRect(
                    color = IncomeColor,
                    topLeft = Offset(groupX, size.height - incomeHeight),
                    size = Size(barWidth, incomeHeight),
                    cornerRadius = cornerRadius
                )

                // Expense bar (right)
                val expenseHeight = (item.expense / maxValue * size.height * animationProgress.value).toFloat()
                drawRoundRect(
                    color = ExpenseColor,
                    topLeft = Offset(groupX + barWidth + gapBetweenBars, size.height - expenseHeight),
                    size = Size(barWidth, expenseHeight),
                    cornerRadius = cornerRadius
                )
            }
        }

        // Month labels
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            data.forEach { item ->
                val label = try {
                    val parts = item.yearMonth.split("-")
                    val monthNames = listOf("", "Jan", "Feb", "Mar", "Apr", "May", "Jun",
                        "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")
                    monthNames[parts[1].toInt()]
                } catch (e: Exception) {
                    item.yearMonth
                }
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // Legend row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Canvas(modifier = Modifier.size(8.dp)) { drawCircle(color = IncomeColor) }
            Spacer(modifier = Modifier.width(4.dp))
            Text("Income", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(modifier = Modifier.width(16.dp))
            Canvas(modifier = Modifier.size(8.dp)) { drawCircle(color = ExpenseColor) }
            Spacer(modifier = Modifier.width(4.dp))
            Text("Expense", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
