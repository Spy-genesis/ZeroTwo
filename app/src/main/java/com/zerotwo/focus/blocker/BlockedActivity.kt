package com.zerotwo.focus.blocker

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
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
import com.zerotwo.focus.timetable.format
import com.zerotwo.focus.ui.theme.HudShapes
import com.zerotwo.focus.ui.theme.MonoFamily
import com.zerotwo.focus.ui.theme.ZeroTwoTheme
import com.zerotwo.focus.ui.theme.HudButtonShape

/** Full-screen block page shown on top of a blocked app. Back and "Go Home" both send the user home. */
class BlockedActivity : ComponentActivity() {

    private var title by mutableStateOf("This app is blocked")
    private var message by mutableStateOf(DEFAULT_MESSAGE)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
        )
        readExtras(intent)
        setContent {
            ZeroTwoTheme {
                BackHandler { goHome() }
                Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    Column(
                        modifier = Modifier.fillMaxSize().padding(32.dp),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        // Lock inside a red chamfered frame: the one "blocked" red moment in the app.
                        Box(
                            Modifier
                                .size(112.dp)
                                .border(2.dp, MaterialTheme.colorScheme.error, HudShapes.large)
                                .background(MaterialTheme.colorScheme.errorContainer, HudShapes.large),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                Icons.Filled.Lock,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(56.dp),
                            )
                        }
                        Spacer(Modifier.size(24.dp))
                        Text(
                            "// ACCESS DENIED",
                            style = MaterialTheme.typography.labelLarge,
                            fontFamily = MonoFamily,
                            color = MaterialTheme.colorScheme.error,
                        )
                        Spacer(Modifier.size(8.dp))
                        Text(
                            title.uppercase(),
                            style = MaterialTheme.typography.headlineMedium,
                            textAlign = TextAlign.Center,
                        )
                        Spacer(Modifier.size(12.dp))
                        Text(
                            message,
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                        )
                        Spacer(Modifier.size(32.dp))
                        Button(shape = HudButtonShape, onClick = ::goHome, modifier = Modifier.fillMaxWidth()) { Text("GO HOME") }
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        readExtras(intent)
    }

    private fun readExtras(intent: Intent?) {
        intent?.getStringExtra(EXTRA_TITLE)?.let { title = it }
        intent?.getStringExtra(EXTRA_MESSAGE)?.let { message = it }
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
        private const val EXTRA_TITLE = "title"
        private const val EXTRA_MESSAGE = "message"
        private const val DEFAULT_MESSAGE = "ZeroTwo is keeping social apps closed so you can focus. You've got this."

        fun launch(context: Context, appLabel: String, status: BlockStatus) {
            val (title, message) = when (status) {
                is BlockStatus.InClass ->
                    "You're in ${status.session.course}" to
                        "$appLabel is blocked until ${status.session.end.format(context)}, when class ends."
                else -> "$appLabel is blocked" to DEFAULT_MESSAGE
            }
            val intent = Intent(context, BlockedActivity::class.java)
                .putExtra(EXTRA_TITLE, title)
                .putExtra(EXTRA_MESSAGE, message)
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
