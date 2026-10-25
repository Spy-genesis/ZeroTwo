package com.zerotwo.focus.ui.theme

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.ButtonColors
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/** What a card's 1dp frame signals. */
enum class HudTone { Neutral, Active, Alert, Pink }

/**
 * A HUD panel: chamfered card with a thin frame. [HudTone.Active] (crimson) marks the
 * live/selected thing, [HudTone.Alert] a problem, [HudTone.Pink] a soft notice.
 */
@Composable
fun HudCard(
    modifier: Modifier = Modifier,
    tone: HudTone = HudTone.Neutral,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val (container, frame) = when (tone) {
        HudTone.Neutral -> HudSurface to HudOutlineVariant
        HudTone.Active -> HudCrimsonContainer to HudCrimson
        HudTone.Alert -> HudBlockedContainer to HudBlocked
        HudTone.Pink -> HudPinkContainer to HudHotPink.copy(alpha = 0.6f)
    }
    val colors = CardDefaults.cardColors(containerColor = container, contentColor = HudTextPrimary)
    val border = BorderStroke(1.dp, frame)
    if (onClick != null) {
        Card(onClick = onClick, modifier = modifier, colors = colors, border = border, content = content)
    } else {
        Card(modifier = modifier, colors = colors, border = border, content = content)
    }
}

/** Section heading with a crimson tick, e.g. "▌TODAY'S SCHEDULE". */
@Composable
fun HudSectionLabel(text: String, modifier: Modifier = Modifier, color: Color = HudTextPrimary) {
    Row(modifier, verticalAlignment = Alignment.CenterVertically) {
        Spacer(Modifier.width(3.dp).height(14.dp).background(HudCrimson))
        Text(
            text.uppercase(),
            style = MaterialTheme.typography.titleMedium,
            color = color,
            modifier = Modifier.padding(start = 8.dp),
        )
    }
}

/** Outlined/text buttons use hot pink text: crimson on near-black is too dim to read comfortably. */
@Composable
fun hudOutlinedButtonColors(): ButtonColors =
    ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.secondary)

@Composable
fun hudTextButtonColors(): ButtonColors =
    ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.secondary)
