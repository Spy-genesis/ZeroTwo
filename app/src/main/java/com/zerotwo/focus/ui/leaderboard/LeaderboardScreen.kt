package com.zerotwo.focus.ui.leaderboard

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.zerotwo.focus.cloud.LeaderboardEntry
import com.zerotwo.focus.screentime.formatDuration

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LeaderboardScreen(
    onBack: () -> Unit,
    viewModel: LeaderboardViewModel = viewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val profile = state.profile

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Leaderboard") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = viewModel::refresh) {
                        Icon(Icons.Filled.Refresh, contentDescription = "Refresh")
                    }
                },
            )
        },
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            Column(Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                if (profile != null) {
                    Text(profile.department, style = MaterialTheme.typography.titleMedium)
                    Text(
                        "Batch ${profile.batch} · lowest screen time ranks first",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth().padding(top = 12.dp)) {
                    LeaderboardDay.entries.forEachIndexed { i, day ->
                        SegmentedButton(
                            selected = state.day == day,
                            onClick = { viewModel.selectDay(day) },
                            shape = SegmentedButtonDefaults.itemShape(i, LeaderboardDay.entries.size),
                        ) { Text(day.label) }
                    }
                }
            }

            Box(Modifier.fillMaxSize()) {
                when (val content = state.content) {
                    LeaderboardContent.Loading -> CircularProgressIndicator(Modifier.align(Alignment.Center))
                    LeaderboardContent.NotConfigured -> Message(
                        "Firebase isn't set up",
                        "Add app/google-services.json and rebuild. Steps are in PROGRESS.md.",
                    )
                    is LeaderboardContent.Error -> Message("Couldn't load the leaderboard", content.message) {
                        Button(onClick = viewModel::refresh) { Text("Try again") }
                    }
                    is LeaderboardContent.Loaded -> Ranking(content, myId = profile?.studentId)
                }
            }
        }
    }
}

@Composable
private fun Ranking(content: LeaderboardContent.Loaded, myId: String?) {
    val myRank = content.entries.indexOfFirst { it.studentId == myId }.takeIf { it >= 0 }?.plus(1)
    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        content.uploadWarning?.let { warning ->
            item {
                Text(
                    "Your latest numbers weren't uploaded: $warning",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                )
            }
        }
        item {
            Text(
                if (myRank != null) "You're #$myRank of ${content.entries.size}" else "${content.entries.size} students",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
            )
        }
        if (content.entries.isEmpty()) {
            item {
                Text(
                    "No one in your batch has synced yet.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        itemsIndexed(content.entries, key = { _, e -> e.studentId }) { index, entry ->
            LeaderboardRow(entry, rank = index + 1, isMe = entry.studentId == myId)
        }
    }
}

@Composable
private fun LeaderboardRow(entry: LeaderboardEntry, rank: Int, isMe: Boolean) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = if (isMe) {
            CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
        } else {
            CardDefaults.cardColors()
        },
    ) {
        Row(Modifier.padding(horizontal = 16.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(
                "#$rank",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.width(48.dp),
            )
            Column(Modifier.weight(1f)) {
                Text(
                    if (isMe) "You · ${entry.studentId}" else maskId(entry.studentId),
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = if (isMe) FontWeight.SemiBold else FontWeight.Normal,
                )
                Text(
                    "Social ${formatDuration(entry.socialMillis)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Text(formatDuration(entry.totalMillis), style = MaterialTheme.typography.titleMedium)
        }
    }
}

@Composable
private fun Message(title: String, body: String, action: @Composable () -> Unit = {}) {
    Column(
        Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(title, style = MaterialTheme.typography.titleLarge, textAlign = TextAlign.Center)
        Text(
            body,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        action()
    }
}

/** Classmates see "21•••45", not the full ID. */
private fun maskId(id: String): String = if (id.length <= 4) id else id.take(2) + "•••" + id.takeLast(2)
