package com.zerotwo.focus.ui.screentime

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import com.zerotwo.focus.screentime.DailyUsage
import com.zerotwo.focus.screentime.formatDuration
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale

/** Candidate gridline steps; the smallest that needs at most 4 lines is used. */
private val STEP_MINUTES = listOf(15L, 30L, 60L, 120L, 180L, 240L, 360L)

/**
 * Seven-day screen-time columns. Single series in the accent: the selected day is full strength,
 * the rest are de-emphasised. Tapping anywhere in a day's column selects it.
 */
@Composable
fun WeeklyBarChart(
    days: List<DailyUsage>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val textMeasurer = rememberTextMeasurer()
    val accent = MaterialTheme.colorScheme.primary
    val grid = MaterialTheme.colorScheme.outlineVariant
    val textPrimary = MaterialTheme.colorScheme.onSurface
    val textMuted = MaterialTheme.colorScheme.onSurfaceVariant
    val labelStyle = MaterialTheme.typography.labelSmall
    val today = LocalDate.now()

    val maxMinutes = (days.maxOfOrNull { it.totalMillis } ?: 0L) / 60_000
    val stepMinutes = STEP_MINUTES.firstOrNull { (maxMinutes + it - 1) / it <= 4 } ?: STEP_MINUTES.last()
    val gridLines = ((maxMinutes + stepMinutes - 1) / stepMinutes).coerceAtLeast(1)
    val scaleMinutes = gridLines * stepMinutes

    // Screen readers get the whole chart as one sentence; the per-app list below is the table view.
    val description = days.joinToString(prefix = "Screen time, last 7 days. ") {
        "${dayLabel(it.date, today)} ${formatDuration(it.totalMillis)}"
    }

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(220.dp)
            .semantics { contentDescription = description }
            .pointerInput(days.size) {
                detectTapGestures { tap ->
                    val plotLeft = 40.dp.toPx()
                    val slot = (size.width - plotLeft) / days.size
                    val index = ((tap.x - plotLeft) / slot).toInt()
                    if (tap.x >= plotLeft && index in days.indices) onSelect(index)
                }
            },
    ) {
        val plotLeft = 40.dp.toPx()
        val plotTop = 24.dp.toPx() // room for the selected bar's value label
        val plotBottom = size.height - 24.dp.toPx() // room for day labels
        val plotHeight = plotBottom - plotTop
        val slot = (size.width - plotLeft) / days.size
        val barWidth = minOf(24.dp.toPx(), slot * 0.6f)
        val corner = CornerRadius(4.dp.toPx())

        // Gridlines + y labels at clean steps. Hairline, recessive.
        for (i in 0..gridLines) {
            val y = plotBottom - plotHeight * i / gridLines
            drawLine(grid, Offset(plotLeft, y), Offset(size.width, y), strokeWidth = 1.dp.toPx())
            val label = textMeasurer.measure(formatTick(i * stepMinutes), labelStyle.copy(color = textMuted))
            drawText(label, topLeft = Offset(0f, y - label.size.height / 2f))
        }

        days.forEachIndexed { i, day ->
            val centerX = plotLeft + slot * (i + 0.5f)
            val selected = i == selectedIndex
            val minutes = day.totalMillis / 60_000f
            val barHeight = plotHeight * (minutes / scaleMinutes)

            if (barHeight > 0f) {
                // Rounded data end, square at the baseline.
                val left = centerX - barWidth / 2
                val top = plotBottom - barHeight
                val path = Path().apply {
                    addRoundRect(
                        RoundRect(
                            left = left, top = top, right = left + barWidth, bottom = plotBottom,
                            topLeftCornerRadius = corner, topRightCornerRadius = corner,
                        ),
                    )
                }
                drawPath(path, accent.copy(alpha = if (selected) 1f else 0.35f))
            }

            if (selected) {
                val value = textMeasurer.measure(
                    formatDuration(day.totalMillis),
                    labelStyle.copy(color = textPrimary, fontWeight = FontWeight.SemiBold),
                )
                val y = plotBottom - barHeight - value.size.height - 4.dp.toPx()
                drawText(value, topLeft = Offset(centerX - value.size.width / 2f, y.coerceAtLeast(0f)))
            }

            val dayText = textMeasurer.measure(
                dayLabel(day.date, today),
                labelStyle.copy(
                    color = if (selected) textPrimary else textMuted,
                    fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                ),
            )
            drawText(dayText, topLeft = Offset(centerX - dayText.size.width / 2f, plotBottom + 6.dp.toPx()))
        }
    }
}

private fun formatTick(minutes: Long): String = when {
    minutes == 0L -> "0"
    minutes % 60 == 0L -> "${minutes / 60}h"
    minutes < 60 -> "${minutes}m"
    else -> "${minutes / 60}h${minutes % 60}"
}

fun dayLabel(date: LocalDate, today: LocalDate): String =
    if (date == today) "Today" else date.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.getDefault())
