package com.zerotwo.focus.ui.tutorial

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.zerotwo.focus.config.DemoConfig
import com.zerotwo.focus.ui.calendar.rememberCalendarAccessRequest
import com.zerotwo.focus.ui.theme.HudButtonShape
import com.zerotwo.focus.ui.theme.HudCard
import com.zerotwo.focus.ui.theme.HudCrimson
import com.zerotwo.focus.ui.theme.HudMint
import com.zerotwo.focus.ui.theme.HudOutlineVariant
import com.zerotwo.focus.ui.theme.HudShapes
import com.zerotwo.focus.ui.theme.HudTone
import com.zerotwo.focus.ui.theme.MonoFamily
import com.zerotwo.focus.ui.theme.hudOutlinedButtonColors
import com.zerotwo.focus.ui.theme.hudTextButtonColors

/**
 * First-run walkthrough: see ZeroTwo work with a demo class, then connect a real calendar,
 * then a quick tour of the controls. Replayable from Home.
 */
@Composable
fun TutorialScreen(
    onFinished: () -> Unit,
    onOpenCalendar: () -> Unit,
    viewModel: TutorialViewModel = viewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val requestCalendar = rememberCalendarAccessRequest { viewModel.refresh() }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { viewModel.refresh() }
    BackHandler(enabled = state.step != TutorialStep.WELCOME) { viewModel.back() }

    fun finish() {
        viewModel.finish()
        onFinished()
    }

    val stepCount = TutorialStep.entries.size
    Scaffold(
        bottomBar = {
            Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                if (state.step != TutorialStep.WELCOME) {
                    TextButton(shape = HudButtonShape, colors = hudTextButtonColors(), onClick = viewModel::back) {
                        Text("Back")
                    }
                }
                Spacer(Modifier.weight(1f))
                Button(
                    shape = HudButtonShape,
                    onClick = { if (state.step == TutorialStep.DONE) finish() else viewModel.next() },
                ) {
                    Text(
                        when {
                            state.step == TutorialStep.WELCOME -> "Show me"
                            state.step == TutorialStep.CALENDAR && state.calendarsSelected == 0 -> "Skip for now"
                            state.step == TutorialStep.DONE -> "Start using ZeroTwo"
                            else -> "Next"
                        },
                    )
                }
            }
        },
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 16.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "STEP ${state.step.ordinal + 1}/$stepCount",
                    style = MaterialTheme.typography.labelMedium,
                    fontFamily = MonoFamily,
                    color = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.weight(1f),
                )
                if (state.step != TutorialStep.DONE) {
                    TextButton(shape = HudButtonShape, colors = hudTextButtonColors(), onClick = ::finish) {
                        Text("Skip tutorial")
                    }
                }
            }
            // Segmented progress: one bar per step.
            Row(Modifier.fillMaxWidth().padding(top = 4.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                TutorialStep.entries.forEach {
                    Box(
                        Modifier
                            .weight(1f)
                            .height(3.dp)
                            .background(if (it.ordinal <= state.step.ordinal) HudCrimson else HudOutlineVariant),
                    )
                }
            }
            Spacer(Modifier.height(32.dp))

            when (state.step) {
                TutorialStep.WELCOME -> Welcome()
                TutorialStep.DEMO -> Demo(state, viewModel::startDemo)
                TutorialStep.CALENDAR -> Calendar(state, requestCalendar, onOpenCalendar)
                TutorialStep.CONTROLS -> Controls()
                TutorialStep.PROGRESS -> Progress()
                TutorialStep.DONE -> Done()
            }
        }
    }
}

@Composable
private fun Header(icon: ImageVector, title: String, body: String) {
    Box(
        Modifier
            .size(88.dp)
            .border(2.dp, MaterialTheme.colorScheme.primary, HudShapes.large)
            .background(MaterialTheme.colorScheme.primaryContainer, HudShapes.large),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimaryContainer, modifier = Modifier.size(40.dp))
    }
    Text(title.uppercase(), style = MaterialTheme.typography.headlineMedium, modifier = Modifier.padding(top = 24.dp))
    Text(
        body,
        style = MaterialTheme.typography.bodyLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(top = 12.dp),
    )
}

/** A numbered or titled point inside a step. */
@Composable
private fun Point(label: String, title: String, body: String) {
    Row(Modifier.padding(top = 16.dp)) {
        Text(
            label,
            style = MaterialTheme.typography.labelLarge,
            fontFamily = MonoFamily,
            color = MaterialTheme.colorScheme.secondary,
            modifier = Modifier.width(36.dp),
        )
        Column {
            Text(title, style = MaterialTheme.typography.titleSmall)
            Text(body, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun Check(done: Boolean, doneText: String, waitingText: String) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 16.dp)) {
        if (done) {
            Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = HudMint, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(8.dp))
        }
        Text(
            if (done) doneText else waitingText,
            style = MaterialTheme.typography.bodyMedium,
            color = if (done) HudMint else MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun Welcome() {
    Header(
        Icons.Filled.Lock,
        "Welcome to ZeroTwo",
        "ZeroTwo locks social apps while you're in class, automatically, and unlocks them when class ends.",
    )
    Point("01", "It follows your schedule", "Your calendar (or the built-in timetable) decides when you're in class.")
    Point("02", "It blocks the distractions", "Instagram, TikTok, Facebook, Snapchat, YouTube and X get a block screen.")
    Point("03", "It keeps score", "See your screen time and how you rank in your batch.")
    Text(
        "This takes about a minute. First, let's see it work.",
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.secondary,
        modifier = Modifier.padding(top = 24.dp),
    )
}

@Composable
private fun Demo(state: TutorialUiState, onStartDemo: () -> Unit) {
    Header(
        Icons.Filled.PlayArrow,
        "Try it with a demo class",
        "Start a pretend lecture. For the next ${DemoConfig.DEMO_DURATION_MINUTES} minutes ZeroTwo acts as if you're in " +
            "\"${DemoConfig.DEMO_COURSE_NAME}\".",
    )
    if (!state.demoRunning) {
        Button(shape = HudButtonShape, onClick = onStartDemo, modifier = Modifier.fillMaxWidth().padding(top = 24.dp)) {
            Text("Start demo class")
        }
    } else {
        HudCard(Modifier.fillMaxWidth().padding(top = 24.dp), tone = HudTone.Active) {
            Column(Modifier.padding(16.dp)) {
                Text("DEMO CLASS IS RUNNING", style = MaterialTheme.typography.titleMedium)
                Point("1", "Press your phone's Home button", "Leave ZeroTwo open in the background.")
                Point("2", "Open Instagram, YouTube or TikTok", "Any app from the blocked list.")
                Point("3", "Meet the block screen", "Tap GO HOME, then come back to ZeroTwo.")
            }
        }
        Check(
            done = state.sawBlockScreen,
            doneText = "You hit the block screen. That's ZeroTwo working.",
            waitingText = "Waiting for you to try a blocked app…",
        )
    }
    Text(
        "Tip: tap the ZEROTWO title on Home ${DemoConfig.DEMO_UNLOCK_TAPS} times to start a demo any time. " +
            "Pressing Next ends this demo.",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(top = 24.dp),
    )
}

@Composable
private fun Calendar(state: TutorialUiState, onAllow: () -> Unit, onOpenCalendar: () -> Unit) {
    Header(
        Icons.Filled.DateRange,
        "Now make it real",
        "Connect your calendar and ZeroTwo blocks during your actual lectures, from Google Calendar or any " +
            "calendar on this phone. It only reads event times and titles.",
    )
    when {
        !state.calendarAccess -> Button(
            shape = HudButtonShape,
            onClick = onAllow,
            modifier = Modifier.fillMaxWidth().padding(top = 24.dp),
        ) { Text("Allow calendar access") }

        state.calendarsSelected == 0 -> {
            Check(done = true, doneText = "Calendar access is on.", waitingText = "")
            Button(shape = HudButtonShape, onClick = onOpenCalendar, modifier = Modifier.fillMaxWidth().padding(top = 16.dp)) {
                Text("Choose your class calendar")
            }
            Text(
                "Tick the calendar that holds your lectures. Tip: add a keyword like \"lecture\" so other events don't block.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 8.dp),
            )
        }

        else -> {
            Check(
                done = true,
                doneText = "${state.calendarsSelected} calendar${if (state.calendarsSelected == 1) "" else "s"} connected.",
                waitingText = "",
            )
            OutlinedButton(
                shape = HudButtonShape,
                colors = hudOutlinedButtonColors(),
                onClick = onOpenCalendar,
                modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
            ) { Text("Change calendars") }
        }
    }
    Text(
        "No calendar? Skip this step: ZeroTwo uses the built-in timetable instead. You can connect one later from " +
            "Home → Calendar settings.",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(top = 24.dp),
    )
}

@Composable
private fun Controls() {
    Header(Icons.Filled.Settings, "Your controls", "Everything lives on the Home screen.")
    Point("◆", "Status card", "Shows whether the shield is active, why, and until when.")
    Point("◆", "Block outside class hours", "Switch it on for study time. Classes always block, whatever this says.")
    Point(
        "◆", "Choose blocked apps",
        "All six social apps are on by default. During class you can add apps but not remove them.",
    )
    Point("◆", "Today's schedule", "Your classes for today, with the current one marked NOW.")
}

@Composable
private fun Progress() {
    Header(Icons.Filled.Star, "Track and compete", "Less scrolling is the goal, and now you can see it.")
    Point("◆", "Screen time", "Your daily total and time per app, with a 7-day chart. Social apps are tagged.")
    Point(
        "◆", "Leaderboard",
        "Your department and batch, lowest screen time first. Classmates only see the last 2 digits of your ERP.",
    )
}

@Composable
private fun Done() {
    Header(
        Icons.Filled.CheckCircle,
        "You're set",
        "ZeroTwo now runs in the background with a small notification, and blocks whenever class is on.",
    )
    Point("◆", "Replay this tutorial", "Home → How ZeroTwo works.")
    Point("◆", "Something not blocking?", "Home → Review permissions shows what's missing.")
}
