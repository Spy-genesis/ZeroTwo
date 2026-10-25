package com.zerotwo.focus.ui.onboarding

import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.zerotwo.focus.permissions.AppPermission
import com.zerotwo.focus.permissions.SettingsLauncher
import com.zerotwo.focus.ui.calendar.rememberCalendarAccessRequest
import com.zerotwo.focus.ui.theme.HudBlocked
import com.zerotwo.focus.ui.theme.HudCard
import com.zerotwo.focus.ui.theme.HudMint
import com.zerotwo.focus.ui.theme.HudTone
import com.zerotwo.focus.ui.theme.MonoFamily
import kotlinx.coroutines.launch
import com.zerotwo.focus.ui.theme.HudButtonShape
import com.zerotwo.focus.ui.theme.hudTextButtonColors
import com.zerotwo.focus.ui.theme.hudOutlinedButtonColors

@Composable
fun OnboardingScreen(
    onContinue: () -> Unit,
    viewModel: OnboardingViewModel = viewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    // Re-check every time we come back from a Settings screen.
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { viewModel.refresh() }

    // Calendar is a runtime permission: show Android's dialog rather than a Settings page.
    val requestCalendar = rememberCalendarAccessRequest { viewModel.refresh() }

    fun showCouldNotOpen() = scope.launch {
        snackbar.showSnackbar("Couldn't open Settings. Open Settings > Apps > ZeroTwo manually.")
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbar) },
        bottomBar = {
            Column(Modifier.fillMaxWidth().padding(16.dp)) {
                if (!state.requiredGranted) {
                    Text(
                        "Grant the required permissions to continue.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 8.dp),
                    )
                }
                Button(
                    shape = HudButtonShape,
                    onClick = onContinue,
                    enabled = state.requiredGranted,
                    modifier = Modifier.fillMaxWidth(),
                ) { Text("Continue") }
            }
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                Text("SET UP ZEROTWO", style = MaterialTheme.typography.headlineMedium)
                Spacer(Modifier.size(4.dp))
                Text(
                    "ZeroTwo needs a few Android settings switched on to block social apps during class. " +
                        "Tap each one, flip the switch, then come back.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.size(16.dp))
                val total = state.statuses.size.coerceAtLeast(1)
                LinearProgressIndicator(
                    progress = { state.grantedCount / total.toFloat() },
                    modifier = Modifier.fillMaxWidth(),
                )
                Text(
                    "${state.grantedCount}/${state.statuses.size} GRANTED",
                    style = MaterialTheme.typography.labelMedium,
                    fontFamily = MonoFamily,
                    color = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
            items(state.statuses, key = { it.permission.name }) { status ->
                PermissionCard(
                    status = status,
                    onOpenSettings = {
                        when {
                            status.permission == AppPermission.CALENDAR -> requestCalendar()
                            !SettingsLauncher.open(context, status.permission) -> showCouldNotOpen()
                        }
                    },
                    onOpenAppInfo = {
                        if (!SettingsLauncher.openAppDetails(context)) showCouldNotOpen()
                    },
                )
            }
            oemBrand()?.let { brand ->
                item {
                    OemHintCard(brand) { if (!SettingsLauncher.openAppDetails(context)) showCouldNotOpen() }
                }
            }
        }
    }
}

@Composable
private fun PermissionCard(
    status: PermissionStatus,
    onOpenSettings: () -> Unit,
    onOpenAppInfo: () -> Unit,
) {
    val permission = status.permission
    HudCard(Modifier.fillMaxWidth(), tone = if (status.granted) HudTone.Neutral else HudTone.Active) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    permission.title.uppercase(),
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f),
                )
                StatusBadge(status.granted)
            }
            Text(
                (if (permission.required) "" else "Optional · ") + permission.why,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp),
            )
            if (!status.granted) {
                if (permission == AppPermission.ACCESSIBILITY) {
                    Text(
                        "Find \"ZeroTwo app blocker\" under Installed/Downloaded apps. " +
                            "If the switch is greyed out, open App info, tap ⋮ and choose \"Allow restricted settings\".",
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(top = 8.dp),
                    )
                }
                Row(Modifier.padding(top = 12.dp)) {
                    Button(shape = HudButtonShape, onClick = onOpenSettings) {
                        Text(if (permission == AppPermission.CALENDAR) "Allow access" else "Open settings")
                    }
                    if (permission == AppPermission.ACCESSIBILITY) {
                        Spacer(Modifier.width(8.dp))
                        TextButton(shape = HudButtonShape, colors = hudTextButtonColors(), onClick = onOpenAppInfo) { Text("App info") }
                    }
                }
            } else {
                OutlinedButton(shape = HudButtonShape, colors = hudOutlinedButtonColors(), onClick = onOpenSettings, modifier = Modifier.padding(top = 12.dp)) {
                    Text("Manage")
                }
            }
        }
    }
}

@Composable
private fun StatusBadge(granted: Boolean) {
    val color = if (granted) HudMint else HudBlocked
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            imageVector = if (granted) Icons.Filled.CheckCircle else Icons.Filled.Warning,
            contentDescription = null,
            tint = color,
            modifier = Modifier.size(18.dp),
        )
        Spacer(Modifier.width(4.dp))
        Text(
            if (granted) "GRANTED" else "NOT GRANTED",
            style = MaterialTheme.typography.labelLarge,
            color = color,
        )
    }
}

/**
 * Some OEM skins add their own switches that Android's APIs can't read, so this is advice, not a check.
 * Returns a display name for those brands, or null.
 */
private fun oemBrand(): String? {
    val maker = android.os.Build.MANUFACTURER.lowercase()
    return listOf("xiaomi", "redmi", "poco", "oppo", "realme", "vivo", "oneplus", "huawei", "honor")
        .firstOrNull { maker.contains(it) }
        ?.replaceFirstChar { it.uppercase() }
}

@Composable
private fun OemHintCard(brand: String, onOpenAppInfo: () -> Unit) {
    HudCard(Modifier.fillMaxWidth(), tone = HudTone.Pink) {
        Column(Modifier.padding(16.dp)) {
            Text("Extra step on $brand phones".uppercase(), style = MaterialTheme.typography.titleMedium)
            Text(
                "In App info, allow \"Autostart\" and, if listed, \"Display pop-up windows while running in " +
                    "background\". Without them the block screen may not appear. ZeroTwo can't check these for you.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp),
            )
            OutlinedButton(shape = HudButtonShape, colors = hudOutlinedButtonColors(), onClick = onOpenAppInfo, modifier = Modifier.padding(top = 12.dp)) { Text("Open app info") }
        }
    }
}
