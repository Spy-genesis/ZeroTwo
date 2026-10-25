package com.zerotwo.focus.ui.home

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.SystemClock
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.zerotwo.focus.blocker.BlockStatus
import com.zerotwo.focus.config.TimetableConfig
import com.zerotwo.focus.timetable.ClassSession
import com.zerotwo.focus.timetable.format
import java.time.LocalTime

@Composable
fun HomeScreen(
    onOpenBlockedApps: () -> Unit,
    onOpenScreenTime: () -> Unit,
    onReviewPermissions: () -> Unit,
    viewModel: HomeViewModel = viewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { viewModel.refresh() }

    // Android 13+ hides the service notification unless this is granted. Blocking works either way.
    val notificationPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { }
    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
        viewModel.ensureServiceRunning()
    }

    // Hidden demo toggle: tap the title quickly DEMO_UNLOCK_TAPS times.
    var taps by remember { mutableIntStateOf(0) }
    var lastTapAt by remember { mutableLongStateOf(0L) }
    fun onTitleTap() {
        val now = SystemClock.elapsedRealtime()
        taps = if (now - lastTapAt < 1_000) taps + 1 else 1
        lastTapAt = now
        if (taps >= TimetableConfig.DEMO_UNLOCK_TAPS) {
            taps = 0
            val on = viewModel.toggleDemo()
            Toast.makeText(context, if (on) "Demo class started" else "Demo mode off", Toast.LENGTH_SHORT).show()
        }
    }

    val isDemo = (state.status as? BlockStatus.InClass)?.isDemo == true
    val inClass = state.status is BlockStatus.InClass

    Scaffold { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "ZeroTwo",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null, // stays hidden: no ripple
                            onClick = ::onTitleTap,
                        ),
                    )
                    if (isDemo) {
                        Spacer(Modifier.width(12.dp))
                        AssistChip(onClick = { viewModel.toggleDemo() }, label = { Text("Demo · tap to end") })
                    }
                }
            }

            if (!state.permissionsOk) {
                item {
                    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)) {
                        Column(Modifier.padding(16.dp)) {
                            Text(
                                "Some permissions were turned off. Blocking may not work until you grant them again.",
                                color = MaterialTheme.colorScheme.onErrorContainer,
                            )
                            Button(onClick = onReviewPermissions, modifier = Modifier.padding(top = 12.dp)) {
                                Text("Fix permissions")
                            }
                        }
                    }
                }
            }

            item { StatusCard(state.status, state.blocker.selectedApps.size) }

            item {
                Card(Modifier.fillMaxWidth()) {
                    Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text("Block outside class hours", style = MaterialTheme.typography.titleMedium)
                            Text(
                                if (inClass) {
                                    "Classes always block. This switch applies once class ends."
                                } else {
                                    "Classes always block. Use this for study time."
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Switch(
                            checked = state.blocker.manualBlockingEnabled,
                            onCheckedChange = viewModel::setManualBlockingEnabled,
                        )
                    }
                }
            }

            item {
                Text(
                    "Today's schedule",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(top = 8.dp),
                )
            }
            if (state.todaysClasses.isEmpty()) {
                item {
                    Text(
                        "No classes today.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            } else {
                items(state.todaysClasses, key = { "${it.start}-${it.course}" }) { session ->
                    ScheduleRow(session, state.now)
                }
            }

            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 8.dp)) {
                    Button(onClick = onOpenScreenTime, modifier = Modifier.fillMaxWidth()) {
                        Text("Screen time")
                    }
                    OutlinedButton(onClick = onOpenBlockedApps, modifier = Modifier.fillMaxWidth()) {
                        Text("Choose blocked apps")
                    }
                    OutlinedButton(onClick = onReviewPermissions, modifier = Modifier.fillMaxWidth()) {
                        Text("Review permissions")
                    }
                }
            }
        }
    }
}

@Composable
private fun StatusCard(status: BlockStatus, appCount: Int) {
    val context = LocalContext.current
    val apps = "$appCount app${if (appCount == 1) "" else "s"}"
    val (headline, detail) = when (status) {
        is BlockStatus.InClass ->
            "Blocking · in ${status.session.course}" to "$apps blocked until ${status.session.end.format(context)}"
        BlockStatus.Manual -> "Blocking · manual" to "$apps blocked until you switch it off"
        is BlockStatus.Off -> "Not blocking" to (
            status.nextClassToday?.let { "Next: ${it.course} at ${it.start.format(context)}" }
                ?: "No more classes today"
            )
    }
    val colors = if (status.isBlocking) {
        CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
        )
    } else {
        CardDefaults.cardColors()
    }
    Card(Modifier.fillMaxWidth(), colors = colors) {
        Column(Modifier.padding(16.dp)) {
            Text(headline, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
            Text(detail, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(top = 4.dp))
        }
    }
}

@Composable
private fun ScheduleRow(session: ClassSession, now: LocalTime) {
    val context = LocalContext.current
    val isNow = session.isInSession(now)
    val isPast = !now.isBefore(session.end)
    Card(
        modifier = Modifier.fillMaxWidth().alpha(if (isPast) 0.5f else 1f),
        colors = if (isNow) {
            CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
        } else {
            CardDefaults.cardColors()
        },
    ) {
        Row(Modifier.padding(horizontal = 16.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(
                "${session.start.format(context)} – ${session.end.format(context)}",
                style = MaterialTheme.typography.labelLarge,
                modifier = Modifier.width(132.dp),
            )
            Text(session.course, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
            if (isNow) {
                Text(
                    "Now",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }
    }
}
