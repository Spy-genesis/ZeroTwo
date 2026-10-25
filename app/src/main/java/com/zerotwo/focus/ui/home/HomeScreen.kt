package com.zerotwo.focus.ui.home

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.SystemClock
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.zerotwo.focus.blocker.BlockStatus
import com.zerotwo.focus.config.DemoConfig
import com.zerotwo.focus.screentime.formatDuration
import com.zerotwo.focus.timetable.ClassSession
import com.zerotwo.focus.timetable.format
import com.zerotwo.focus.ui.theme.HudCard
import com.zerotwo.focus.ui.theme.HudNumber
import com.zerotwo.focus.ui.theme.HudSectionLabel
import com.zerotwo.focus.ui.theme.HudTone
import com.zerotwo.focus.ui.theme.MonoFamily
import java.time.LocalTime
import com.zerotwo.focus.ui.theme.HudButtonShape
import com.zerotwo.focus.ui.theme.hudTextButtonColors
import com.zerotwo.focus.ui.theme.hudOutlinedButtonColors

@Composable
fun HomeScreen(
    onOpenBlockedApps: () -> Unit,
    onOpenScreenTime: () -> Unit,
    onOpenLeaderboard: () -> Unit,
    onReviewPermissions: () -> Unit,
    onSignedOut: () -> Unit,
    viewModel: HomeViewModel = viewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val profile by viewModel.profile.collectAsStateWithLifecycle()
    val todayMillis by viewModel.todayMillis.collectAsStateWithLifecycle()
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
        viewModel.syncScreenTime()
    }

    // Hidden demo toggle: tap the title quickly DEMO_UNLOCK_TAPS times.
    var taps by remember { mutableIntStateOf(0) }
    var lastTapAt by remember { mutableLongStateOf(0L) }
    fun onTitleTap() {
        val now = SystemClock.elapsedRealtime()
        taps = if (now - lastTapAt < 1_000) taps + 1 else 1
        lastTapAt = now
        if (taps >= DemoConfig.DEMO_UNLOCK_TAPS) {
            taps = 0
            val on = viewModel.toggleDemo()
            Toast.makeText(
                context,
                if (on) "Demo on: class in session + sample classmates" else "Demo mode off",
                Toast.LENGTH_SHORT,
            ).show()
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
                        "ZEROTWO",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Black,
                        modifier = Modifier.clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null, // stays hidden: no ripple
                            onClick = ::onTitleTap,
                        ),
                    )
                    if (isDemo) {
                        Spacer(Modifier.width(12.dp))
                        AssistChip(
                            onClick = { viewModel.toggleDemo() },
                            label = { Text("DEMO · TAP TO END") },
                            colors = AssistChipDefaults.assistChipColors(labelColor = MaterialTheme.colorScheme.secondary),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.secondary),
                        )
                    }
                }
            }

            if (!state.permissionsOk) {
                item {
                    HudCard(Modifier.fillMaxWidth(), tone = HudTone.Alert) {
                        Column(Modifier.padding(16.dp)) {
                            Text(
                                "Some permissions were turned off. Blocking may not work until you grant them again.",
                                color = MaterialTheme.colorScheme.onErrorContainer,
                            )
                            Button(shape = HudButtonShape, onClick = onReviewPermissions, modifier = Modifier.padding(top = 12.dp)) {
                                Text("Fix permissions")
                            }
                        }
                    }
                }
            }

            item { StatusCard(state.status, state.blocker.selectedApps.size) }

            item {
                HudCard(Modifier.fillMaxWidth(), onClick = onOpenScreenTime) {
                    Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text("SCREEN TIME TODAY", style = MaterialTheme.typography.titleMedium)
                            Text(
                                "Tap for the last 7 days",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Text(
                            todayMillis?.let(::formatDuration) ?: "—",
                            style = HudNumber.copy(fontSize = 24.sp),
                            color = MaterialTheme.colorScheme.secondary,
                        )
                    }
                }
            }

            item {
                HudCard(Modifier.fillMaxWidth()) {
                    Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text("BLOCK OUTSIDE CLASS HOURS", style = MaterialTheme.typography.titleMedium)
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
                HudSectionLabel("Today's schedule", Modifier.padding(top = 8.dp))
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
                    Button(shape = HudButtonShape, onClick = onOpenLeaderboard, modifier = Modifier.fillMaxWidth()) {
                        Text("Leaderboard")
                    }
                    OutlinedButton(shape = HudButtonShape, colors = hudOutlinedButtonColors(), onClick = onOpenBlockedApps, modifier = Modifier.fillMaxWidth()) {
                        Text("Choose blocked apps")
                    }
                    OutlinedButton(shape = HudButtonShape, colors = hudOutlinedButtonColors(), onClick = onReviewPermissions, modifier = Modifier.fillMaxWidth()) {
                        Text("Review permissions")
                    }
                }
            }

            profile?.let { p ->
                item {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 8.dp)) {
                        Text(
                            "Signed in as ${p.studentId} · ${p.department} · Batch ${p.batch}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.weight(1f),
                        )
                        TextButton(shape = HudButtonShape, colors = hudTextButtonColors(), onClick = {
                            viewModel.signOut()
                            onSignedOut()
                        }) { Text("Sign out") }
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
    HudCard(Modifier.fillMaxWidth(), tone = if (status.isBlocking) HudTone.Active else HudTone.Neutral) {
        Column(Modifier.padding(16.dp)) {
            Text(
                if (status.isBlocking) "● SHIELD ACTIVE" else "○ SHIELD IDLE",
                style = MaterialTheme.typography.labelSmall,
                fontFamily = MonoFamily,
                color = if (status.isBlocking) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                headline.uppercase(),
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(top = 6.dp),
            )
            Text(
                detail,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp),
            )
        }
    }
}

@Composable
private fun ScheduleRow(session: ClassSession, now: LocalTime) {
    val context = LocalContext.current
    val isNow = session.isInSession(now)
    val isPast = session.isOver(now)
    HudCard(
        modifier = Modifier.fillMaxWidth().alpha(if (isPast) 0.45f else 1f),
        tone = if (isNow) HudTone.Active else HudTone.Neutral,
    ) {
        Row(Modifier.padding(horizontal = 16.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(
                "${session.start.format(context)} – ${session.end.format(context)}",
                style = HudNumber.copy(fontSize = 12.sp, fontWeight = FontWeight.Normal),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.width(150.dp),
            )
            Text(session.course, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
            if (isNow) {
                Text(
                    "NOW",
                    style = HudNumber.copy(fontSize = 12.sp),
                    color = MaterialTheme.colorScheme.secondary,
                )
            }
        }
    }
}
