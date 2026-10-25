# PROGRESS

## Phases
1. **Scaffold + permission onboarding** ✅
2. **Blocking social apps** ✅
3. Mock timetable JSON + auto-block during class hours
4. Mock ERP link: student ID + department picker (integration-ready, mocked for demo)
5. Firebase anonymous auth + screen-time upload + batch/department leaderboard
6. Polish + demo prep

## Done
### Phase 1 — Scaffold + onboarding
- Single-module Compose app, `com.zerotwo.focus`, min SDK 26 / target 35, Material 3 theme with one accent (indigo `#4F46E5`, dynamic color off).
- Navigation: `onboarding` → `home`. App starts on `home` if all required permissions are already granted.
- Onboarding checks 4 permissions, shows Granted / Not granted, deep-links to the matching Settings screen, re-checks on every `ON_RESUME`:
  - Usage access (required), Display over other apps (required), Accessibility service (required), Ignore battery optimization (optional).
- Every deep link has fallbacks (generic screen → App info → Settings root); if all fail, a snackbar explains what to do.

### Phase 2 — Blocking
- **Config:** `config/BlockedAppsConfig.kt` — hardcoded default list: Instagram, TikTok (2 packages), Facebook (+ Lite), Snapchat, YouTube, X.
- **Settings store:** `blocker/BlockerSettings.kt` (SharedPreferences + StateFlow): master on/off + per-app opt-outs. All config apps start selected.
- **Detection (two paths, both feed `blocker/AppBlocker.kt`, which debounces 1.5 s):**
  - `BlockerAccessibilityService` — instant, on `TYPE_WINDOW_STATE_CHANGED` (package name only, no window content).
  - `BlockerService` polls `UsageStatsManager` every 1 s as a backup (`ForegroundAppDetector`).
- **Foreground service:** `BlockerService`, type `specialUse`, persistent low-priority notification ("N apps blocked"), `START_STICKY`, re-started on app launch if blocking is on. Asks for `POST_NOTIFICATIONS` on Android 13+ when blocking is switched on; works even if denied.
- **Block screen:** `BlockedActivity` — full-screen "<App> is blocked" + **Go Home**; Back also goes home. Own task, excluded from recents.
- **UI:** Home has a Blocking switch, a warning card if permissions were revoked, and "Choose blocked apps" → per-app switches.

## Next
Phase 3: mock timetable JSON config, "in class now?" logic, auto-enable blocking only during class hours (manual switch becomes an override / always-on mode).

## Known issues
- Android 13+ sideloaded APK (installed from file manager/browser): accessibility switch is greyed out ("restricted setting"). Fix: App info → ⋮ → Allow restricted settings. Installing with `adb install` avoids it.
- Block screen is an Activity launched from the background. Allowed because we hold the overlay permission / are an accessibility service; on some aggressive OEM ROMs (MIUI/HyperOS) you may also need to allow "Display pop-up windows while running in background" in App info → Other permissions.
- With accessibility off, the UsageStats fallback can take up to ~1 s to react.
- Blocking does not survive a reboot until the app is opened once (no BOOT_COMPLETED receiver yet). The accessibility service does restart on boot by itself.
- Lint: only "newer dependency version available" warnings; versions are pinned on purpose.
- Usage access deep link with package URI only lands on our app on some ROMs; others show the full list.

## Build
Toolchain lives in `~/Android` (JDK 17, SDK 35, Gradle 8.9). Not on PATH. `~/.gradle/gradle.properties` forces IPv4 (Java hung on IPv6 on this machine).
```
export JAVA_HOME=~/Android/jdk ANDROID_HOME=~/Android/Sdk
./gradlew assembleDebug        # APK: app/build/outputs/apk/debug/app-debug.apk
~/Android/Sdk/platform-tools/adb install -r app/build/outputs/apk/debug/app-debug.apk
```
