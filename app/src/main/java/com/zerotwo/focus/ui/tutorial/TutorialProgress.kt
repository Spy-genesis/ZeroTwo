package com.zerotwo.focus.ui.tutorial

import android.content.Context

/** Whether the first-run tutorial has been completed or skipped. */
object TutorialProgress {

    private fun prefs(context: Context) = context.getSharedPreferences("tutorial", Context.MODE_PRIVATE)

    fun isSeen(context: Context): Boolean = prefs(context).getBoolean("seen", false)

    fun markSeen(context: Context) = prefs(context).edit().putBoolean("seen", true).apply()
}
