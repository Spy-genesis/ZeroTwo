# PROGRESS

## Phases
1. **Scaffold + permission onboarding** ✅
2. Blocking social apps (accessibility service + overlay block screen, blocked-packages config file)
3. Mock timetable JSON + auto-block during class hours
4. Mock ERP link: student ID + department picker (integration-ready, mocked for demo)
5. Firebase anonymous auth + screen-time upload + batch/department leaderboard
6. Polish + demo prep

## Done
### Phase 1 — Scaffold + onboarding
- Single-module Compose app, `com.zerotwo.focus`, min SDK 26 / target 35, Material 3 theme with one accent (indigo `#4F46E5`, dynamic color off).
- Navigation: `onboarding` → `home` (placeholder). App starts on `home` if all required permissions are already granted.
- Onboarding checks 4 permissions, shows Granted / Not granted, deep-links to the matching Settings screen, re-checks on every `ON_RESUME`:
  - Usage access (required) — AppOps `GET_USAGE_STATS`
  - Display over other apps (required) — `Settings.canDrawOverlays`
  - Accessibility service (required) — `ENABLED_ACCESSIBILITY_SERVICES` contains `BlockerAccessibilityService`
  - Ignore battery optimization (optional) — `PowerManager.isIgnoringBatteryOptimizations`
- Every deep link has fallbacks (generic screen → App info → Settings root); if all fail, a snackbar explains what to do. No crashes on denial.
- `BlockerAccessibilityService` is declared but does nothing yet.

Key files: `permissions/PermissionChecker.kt`, `permissions/SettingsLauncher.kt`, `ui/onboarding/`.

## Next
Phase 2: blocked package names config file, foreground-app detection in `BlockerAccessibilityService`, overlay block screen.

## Known issues
- Android 13+: when the APK is sideloaded from a file manager/browser, the accessibility switch is greyed out ("restricted setting"). The fix is App info → ⋮ → Allow restricted settings; the onboarding card explains this and has an App info button. Installing via `adb install` avoids it.
- Usage access deep link with package URI only lands on our app on some ROMs; others show the full list.
- Toolchain lives in `~/Android` (JDK 17, SDK, Gradle 8.9) — not on PATH; see build command below.

## Build
```
export JAVA_HOME=~/Android/jdk ANDROID_HOME=~/Android/Sdk
./gradlew assembleDebug        # APK: app/build/outputs/apk/debug/app-debug.apk
~/Android/Sdk/platform-tools/adb install -r app/build/outputs/apk/debug/app-debug.apk
```
