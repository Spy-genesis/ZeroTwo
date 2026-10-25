package com.zerotwo.focus.ui.theme

import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

// Chamfered corners instead of rounded ones: armor plating, not bubbles.
val HudShapes = Shapes(
    extraSmall = CutCornerShape(2.dp),
    small = CutCornerShape(4.dp),
    medium = CutCornerShape(topStart = 14.dp, bottomEnd = 14.dp),
    large = CutCornerShape(topStart = 20.dp, bottomEnd = 20.dp),
    extraLarge = CutCornerShape(topStart = 28.dp, bottomEnd = 28.dp),
)

/** Material buttons default to pill shapes that the theme can't override. */
val HudButtonShape = CutCornerShape(topStart = 10.dp, bottomEnd = 10.dp)
