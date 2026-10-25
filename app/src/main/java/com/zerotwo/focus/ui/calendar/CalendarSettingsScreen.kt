package com.zerotwo.focus.ui.calendar

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.zerotwo.focus.calendar.CalendarEvent
import com.zerotwo.focus.calendar.DeviceCalendar
import com.zerotwo.focus.timetable.format
import com.zerotwo.focus.ui.theme.HudButtonShape
import com.zerotwo.focus.ui.theme.HudCard
import com.zerotwo.focus.ui.theme.HudNumber
import com.zerotwo.focus.ui.theme.HudSectionLabel
import com.zerotwo.focus.ui.theme.HudShapes
import com.zerotwo.focus.ui.theme.HudTone
import java.time.Instant
import java.time.ZoneId

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarSettingsScreen(
    onBack: () -> Unit,
    viewModel: CalendarSettingsViewModel = viewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val requestAccess = rememberCalendarAccessRequest { viewModel.refresh() }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { viewModel.refresh() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("CALENDAR") },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            when {
                !state.loaded -> CircularProgressIndicator(Modifier.align(Alignment.Center))
                !state.hasPermission -> NoAccess(requestAccess)
                else -> Settings(state, viewModel)
            }
        }
    }
}

@Composable
private fun NoAccess(onAllow: () -> Unit) {
    Column(Modifier.fillMaxWidth().padding(16.dp)) {
        HudCard(Modifier.fillMaxWidth(), tone = HudTone.Active) {
            Column(Modifier.padding(16.dp)) {
                Text("CALENDAR ACCESS IS OFF", style = MaterialTheme.typography.titleMedium)
                Text(
                    "Let ZeroTwo read your calendar and it blocks social apps during your real lectures, " +
                        "from Google Calendar or any calendar on this phone. It only reads event times and titles. " +
                        "Until then, it uses the built-in timetable.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 6.dp),
                )
                Button(shape = HudButtonShape, onClick = onAllow, modifier = Modifier.padding(top = 12.dp)) {
                    Text("Allow calendar access")
                }
            }
        }
    }
}

@Composable
private fun Settings(state: CalendarSettingsUiState, viewModel: CalendarSettingsViewModel) {
    val noneSelected = state.calendars.none { it.id in state.filter.selectedCalendarIds }
    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            HudSectionLabel("Calendars")
            Text(
                "Events in checked calendars block social apps while they're happening.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp),
            )
        }
        if (state.calendars.isEmpty()) {
            item {
                HudCard(Modifier.fillMaxWidth(), tone = HudTone.Pink) {
                    Text(
                        "No calendars on this phone. Add a Google account or create a calendar in your calendar app, " +
                            "then come back. Until then, ZeroTwo uses the built-in timetable.",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(16.dp),
                    )
                }
            }
        } else {
            item {
                HudCard(Modifier.fillMaxWidth()) {
                    state.calendars.forEach { calendar ->
                        CalendarRow(
                            calendar = calendar,
                            checked = calendar.id in state.filter.selectedCalendarIds,
                            onCheckedChange = { viewModel.setCalendarSelected(calendar.id, it) },
                        )
                    }
                }
            }
        }

        item {
            HudSectionLabel("Filter", Modifier.padding(top = 8.dp))
            OutlinedTextField(
                value = state.filter.keyword,
                onValueChange = viewModel::setKeyword,
                label = { Text("Only events whose title contains") },
                placeholder = { Text("e.g. lecture") },
                supportingText = { Text("Leave empty to count every event in the checked calendars.") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
            )
        }
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Count all-day events", style = MaterialTheme.typography.bodyLarge)
                    Text(
                        "Off by default, so holidays and birthdays don't block the whole day.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Switch(checked = state.filter.includeAllDay, onCheckedChange = viewModel::setIncludeAllDay)
            }
        }

        item { HudSectionLabel("Blocks today", Modifier.padding(top = 8.dp)) }
        when {
            noneSelected -> item {
                Text(
                    "Check a calendar above to start. Until then, ZeroTwo uses the built-in timetable.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            state.matchingToday.isEmpty() -> item {
                Text(
                    "No matching events today.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            else -> items(state.matchingToday, key = { "${it.eventId}-${it.beginMillis}" }) { EventRow(it) }
        }
    }
}

@Composable
private fun CalendarRow(calendar: DeviceCalendar, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Checkbox(checked = checked, onCheckedChange = onCheckedChange)
        // The calendar's own color, as a small swatch beside its name.
        Box(
            Modifier
                .size(10.dp)
                .background(calendar.color?.let { Color(it) } ?: MaterialTheme.colorScheme.outline, HudShapes.extraSmall),
        )
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Text(calendar.displayName, style = MaterialTheme.typography.bodyLarge)
            if (calendar.accountName.isNotBlank() && calendar.accountName != calendar.displayName) {
                Text(
                    calendar.accountName,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
fun EventRow(event: CalendarEvent, isNow: Boolean = false, isPast: Boolean = false) {
    val context = LocalContext.current
    val zone = ZoneId.systemDefault()
    fun time(millis: Long) = Instant.ofEpochMilli(millis).atZone(zone).toLocalTime().format(context)
    HudCard(
        modifier = Modifier.fillMaxWidth().alpha(if (isPast) 0.45f else 1f),
        tone = if (isNow) HudTone.Active else HudTone.Neutral,
    ) {
        Row(Modifier.padding(horizontal = 16.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(
                if (event.allDay) "ALL DAY" else "${time(event.beginMillis)} – ${time(event.endMillis)}",
                style = HudNumber.copy(fontSize = 12.sp),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.width(150.dp),
            )
            Text(event.title, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
            if (isNow) {
                Text("NOW", style = HudNumber.copy(fontSize = 12.sp), color = MaterialTheme.colorScheme.secondary)
            }
        }
    }
}
