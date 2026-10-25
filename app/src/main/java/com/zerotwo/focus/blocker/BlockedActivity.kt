package com.zerotwo.focus.blocker

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.zerotwo.focus.ui.theme.ZeroTwoTheme

/** Full-screen block page shown on top of a blocked app. Back and "Go Home" both send the user home. */
class BlockedActivity : ComponentActivity() {

    private var appLabel by mutableStateOf("This app")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        readLabel(intent)
        setContent {
            ZeroTwoTheme {
                BackHandler { goHome() }
                Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    Column(
                        modifier = Modifier.fillMaxSize().padding(32.dp),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Icon(
                            Icons.Filled.Lock,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(72.dp),
                        )
                        Spacer(Modifier.size(24.dp))
                        Text(
                            "$appLabel is blocked",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.SemiBold,
                            textAlign = TextAlign.Center,
                        )
                        Spacer(Modifier.size(12.dp))
                        Text(
                            "ZeroTwo is keeping social apps closed so you can focus. You've got this.",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                        )
                        Spacer(Modifier.size(32.dp))
                        Button(onClick = ::goHome, modifier = Modifier.fillMaxWidth()) { Text("Go Home") }
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        readLabel(intent)
    }

    private fun readLabel(intent: Intent?) {
        intent?.getStringExtra(EXTRA_APP_LABEL)?.let { appLabel = it }
    }

    private fun goHome() {
        val home = Intent(Intent.ACTION_MAIN)
            .addCategory(Intent.CATEGORY_HOME)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        try {
            startActivity(home)
        } catch (e: Exception) {
            Log.w("BlockedActivity", "No home screen to go to", e)
        }
        finish()
    }

    companion object {
        private const val EXTRA_APP_LABEL = "app_label"

        fun launch(context: Context, appLabel: String) {
            val intent = Intent(context, BlockedActivity::class.java)
                .putExtra(EXTRA_APP_LABEL, appLabel)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            try {
                context.startActivity(intent)
            } catch (e: Exception) {
                // Background-start blocked by the OS; the next detector tick will retry.
                Log.w("BlockedActivity", "Could not show block screen", e)
            }
        }
    }
}
