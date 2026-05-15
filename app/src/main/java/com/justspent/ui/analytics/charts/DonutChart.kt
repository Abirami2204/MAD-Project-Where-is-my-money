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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.justspent.data.local.entity.CategoryTotal
import com.justspent.ui.theme.*

/**
 * A custom donut chart drawn entirely with Compose Canvas.
 * Animates each arc segment on first composition.
 */
@Composable
fun DonutChart(
    data: List<CategoryTotal>,
    modifier: Modifier = Modifier
) {
    if (data.isEmpty()) return

    val total = data.sumOf { it.total }
    if (total == 0.0) return

    val colors = listOf(
        CategoryFood,
        CategoryTransport,
        CategoryShopping,
        CategoryBills,
        CategoryOther,
        Color(0xFF26A69A),
        Color(0xFFAB47BC),
        Color(0xFFFF7043)
    )

    val animationProgress = remember { Animatable(0f) }
    LaunchedEffect(data) {
        animationProgress.snapTo(0f)
        animationProgress.animateTo(1f, animationSpec = tween(durationMillis = 1000))
    }

    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Canvas(
            modifier = Modifier
                .size(200.dp)
                .padding(16.dp)
        ) {
            val strokeWidth = 36.dp.toPx()
            val diameter = size.minDimension - strokeWidth
            val topLeft = Offset(
                (size.width - diameter) / 2,
                (size.height - diameter) / 2
            )
            val arcSize = Size(diameter, diameter)

            var startAngle = -90f
            data.forEachIndexed { index, item ->
                val sweep = (item.total / total * 360f * animationProgress.value).toFloat()
                drawArc(
                    color = colors[index % colors.size],
                    startAngle = startAngle,
                    sweepAngle = sweep,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                )
                startAngle += (item.total / total * 360f).toFloat()
            }
        }

        // Center label
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "₹${String.format("%,.0f", total)}",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "Total Spent",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/**
 * Legend items displayed below the donut chart.
 */
@Composable
fun DonutLegend(
    data: List<CategoryTotal>,
    modifier: Modifier = Modifier
) {
    val colors = listOf(
        CategoryFood,
        CategoryTransport,
        CategoryShopping,
        CategoryBills,
        CategoryOther,
        Color(0xFF26A69A),
        Color(0xFFAB47BC),
        Color(0xFFFF7043)
    )
    val total = data.sumOf { it.total }
    if (total == 0.0) return

    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        data.forEachIndexed { index, item ->
            val pct = (item.total / total * 100).toInt()
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Canvas(modifier = Modifier.size(10.dp)) {
                        drawCircle(color = colors[index % colors.size])
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = item.category,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                Text(
                    text = "₹${String.format("%,.0f", item.total)} ($pct%)",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
