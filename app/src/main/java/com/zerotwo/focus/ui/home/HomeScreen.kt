package com.zerotwo.focus.ui.home

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel

@Composable
fun HomeScreen(
    onOpenBlockedApps: () -> Unit,
    onReviewPermissions: () -> Unit,
    viewModel: HomeViewModel = viewModel(),
) {
    val state by viewModel.blockerState.collectAsStateWithLifecycle()
    val permissionsOk by viewModel.permissionsOk.collectAsStateWithLifecycle()
    val context = LocalContext.current

    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { viewModel.refresh() }

    // Android 13+ hides the service notification unless this is granted. Blocking works either way.
    val notificationPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { }

    fun turnOn() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
        viewModel.setBlockingEnabled(true)
    }

    Scaffold { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text("ZeroTwo", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.SemiBold)

            if (!permissionsOk) {
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

            Card(Modifier.fillMaxWidth()) {
                Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("Blocking", style = MaterialTheme.typography.titleMedium)
                        Text(
                            if (state.blockingEnabled) {
                                "On · ${state.selectedApps.size} apps blocked"
                            } else {
                                "Off"
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Switch(
                        checked = state.blockingEnabled,
                        onCheckedChange = { if (it) turnOn() else viewModel.setBlockingEnabled(false) },
                    )
                }
            }

            Text(
                "Class-hours scheduling arrives in the next phase. For now, blocking applies all day while it's on.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Button(onClick = onOpenBlockedApps, modifier = Modifier.fillMaxWidth()) { Text("Choose blocked apps") }
            OutlinedButton(onClick = onReviewPermissions, modifier = Modifier.fillMaxWidth()) {
                Text("Review permissions")
            }
        }
    }
}
