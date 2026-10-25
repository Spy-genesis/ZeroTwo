package com.zerotwo.focus.blocker

import android.accessibilityservice.AccessibilityService
import android.view.accessibility.AccessibilityEvent

/**
 * Foreground-app detector. Phase 1 only declares it so onboarding can ask the user to enable it;
 * the actual blocking logic arrives in a later phase.
 */
class BlockerAccessibilityService : AccessibilityService() {

    override fun onAccessibilityEvent(event: AccessibilityEvent?) = Unit

    override fun onInterrupt() = Unit
}
