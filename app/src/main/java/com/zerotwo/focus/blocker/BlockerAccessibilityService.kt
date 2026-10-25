package com.zerotwo.focus.blocker

import android.accessibilityservice.AccessibilityService
import android.view.accessibility.AccessibilityEvent

/**
 * Instant foreground-app detector: Android tells us whenever a new window comes up.
 * Only the package name is read; window content is never accessed.
 */
class BlockerAccessibilityService : AccessibilityService() {

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event?.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return
        val packageName = event.packageName?.toString() ?: return
        AppBlocker.onForegroundApp(this, packageName)
    }

    override fun onInterrupt() = Unit
}
