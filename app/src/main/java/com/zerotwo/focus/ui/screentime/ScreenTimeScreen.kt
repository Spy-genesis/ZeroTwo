package com.zerotwo.focus.ui.screentime

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toBitmap
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.zerotwo.focus.permissions.AppPermission
import com.zerotwo.focus.permissions.SettingsLauncher
import com.zerotwo.focus.screentime.AppUsage
import com.zerotwo.focus.screentime.formatDuration
import com.zerotwo.focus.ui.theme.HudCard
import com.zerotwo.focus.ui.theme.HudNumber
import com.zerotwo.focus.ui.theme.HudNumberLarge
import com.zerotwo.focus.ui.theme.HudSectionLabel
import com.zerotwo.focus.ui.theme.HudShapes
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import com.zerotwo.focus.ui.theme.HudButtonShape

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScreenTimeScreen(
    onBack: () -> Unit,
    viewModel: ScreenTimeViewModel = viewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { viewModel.refresh() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("SCREEN TIME") },
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
            when (val s = state) {
                ScreenTimeUiState.Loading ->
                    CircularProgressIndicator(Modifier.align(Alignment.Center))
                ScreenTimeUiState.NoPermission -> NoPermission()
                is ScreenTimeUiState.Loaded -> Loaded(s, viewModel::select)
            }
        }
    }
}

@Composable
private fun NoPermission() {
    val context = LocalContext.current
    Column(
        Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("USAGE ACCESS IS OFF", style = MaterialTheme.typography.titleLarge)
        Text(
            "ZeroTwo needs usage access to measure screen time.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Button(shape = HudButtonShape, onClick = { SettingsLauncher.open(context, AppPermission.USAGE_ACCESS) }) { Text("Open settings") }
    }
}

@Composable
private fun Loaded(state: ScreenTimeUiState.Loaded, onSelect: (Int) -> Unit) {
    val selected = state.selected
    val today = LocalDate.now()
    val topMillis = selected.apps.firstOrNull()?.millis ?: 1L
    val socialMillis = selected.apps.filter { it.isSocial }.sumOf { it.millis }

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Text(
                when (selected.date) {
                    today -> "Today"
                    today.minusDays(1) -> "Yesterday"
                    else -> selected.date.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM))
                }.uppercase(),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.secondary,
            )
            // Hero figure: the day's total.
            Text(
                formatDuration(selected.totalMillis),
                style = HudNumberLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                "Social apps ${formatDuration(socialMillis)} · 7-day average ${formatDuration(state.averageMillis)}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        item {
            HudCard(Modifier.fillMaxWidth()) {
                WeeklyBarChart(
                    days = state.days,
                    selectedIndex = state.selectedIndex,
                    onSelect = onSelect,
                    modifier = Modifier.padding(16.dp),
                )
            }
            Text(
                "Tap a day to see its apps.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp),
            )
        }
        item {
            HudSectionLabel("Apps", Modifier.padding(top = 8.dp))
        }
        if (selected.apps.isEmpty()) {
            item {
                Text(
                    "No app used for a minute or more.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        } else {
            items(selected.apps, key = { it.packageName }) { app -> AppRow(app, topMillis) }
        }
    }
}

@Composable
private fun AppRow(app: AppUsage, topMillis: Long) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        AppIcon(app.packageName)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(app.label, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f, fill = false))
                if (app.isSocial) {
                    Spacer(Modifier.width(8.dp))
                    Text(
                        "SOCIAL",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier
                            .background(MaterialTheme.colorScheme.secondaryContainer, HudShapes.extraSmall)
                            .padding(horizontal = 6.dp, vertical = 2.dp),
                    )
                }
                Spacer(Modifier.weight(1f))
                Text(formatDuration(app.millis), style = HudNumber.copy(fontSize = 14.sp))
            }
            // Share of the top app's time: one hue on a lighter track.
            Box(
                Modifier
                    .padding(top = 6.dp)
                    .fillMaxWidth()
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant),
            ) {
                Box(
                    Modifier
                        .fillMaxWidth((app.millis.toFloat() / topMillis).coerceIn(0.02f, 1f))
                        .fillMaxHeight()
                        .background(MaterialTheme.colorScheme.primary),
                )
            }
        }
    }
}

@Composable
private fun AppIcon(packageName: String) {
    val context = LocalContext.current
    val icon: ImageBitmap? = remember(packageName) {
        runCatching {
            context.packageManager.getApplicationIcon(packageName).toBitmap(96, 96).asImageBitmap()
        }.getOrNull()
    }
    if (icon != null) {
        Image(icon, contentDescription = null, modifier = Modifier.size(32.dp))
    } else {
        Box(Modifier.size(32.dp).background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(8.dp)))
    }
}
