package com.zerotwo.focus.config

/**
 * A social app the blocker knows about. Some apps ship under more than one package
 * name (e.g. regional TikTok builds), so [packages] is a set.
 */
data class BlockableApp(
    val id: String,
    val label: String,
    val packages: Set<String>,
)

/**
 * DEFAULT BLOCKED APPS — edit this list to change what ZeroTwo can block.
 * Every app here is blocked by default; the user can switch individual apps off
 * on the "Blocked apps" screen.
 */
object BlockedAppsConfig {

    val DEFAULT_BLOCKED_APPS: List<BlockableApp> = listOf(
        BlockableApp("instagram", "Instagram", setOf("com.instagram.android")),
        BlockableApp("tiktok", "TikTok", setOf("com.zhiliaoapp.musically", "com.ss.android.ugc.trill")),
        BlockableApp("facebook", "Facebook", setOf("com.facebook.katana", "com.facebook.lite")),
        BlockableApp("snapchat", "Snapchat", setOf("com.snapchat.android")),
        BlockableApp("youtube", "YouTube", setOf("com.google.android.youtube")),
        BlockableApp("x", "X", setOf("com.twitter.android")),
    )
}
