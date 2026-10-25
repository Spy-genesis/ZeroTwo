# PROGRESS

## Phases
1. **Scaffold + permission onboarding** ✅
2. **Blocking social apps** ✅
3. **Mock timetable + class-hours blocking** ✅
4. **Screen time stats + 7-day chart** ✅
5. **Mock ERP login + Firebase leaderboard** ✅
6. **Polish + demo prep** ✅

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

### Phase 3 — Timetable
- **Timetable config:** `app/src/main/assets/mock_timetable.json` (`day`, `start`, `end`, `course`; Mon–Sat, 15 classes, none on Sunday). Settings in `config/TimetableConfig.kt`. Parsed with built-in `org.json` by `timetable/TimetableRepository.kt` — integration-ready, mocked for demo. Missing/malformed file → empty timetable; bad entries skipped and logged.
- **Policy:** `blocker/BlockPolicy.kt` is the single rule used by the service, accessibility service and UI:
  1. Demo class active → block (`InClass`, demo)
  2. Timetable class now → block (`InClass`), manual switch ignored
  3. Manual switch on → block (`Manual`)
  4. Otherwise `Off` (with next class today)
- **Block screen:** in class it says **"You're in <course>"** + "<App> is blocked until <end>". Outside class it keeps the Phase 2 wording.
- **Service:** `BlockerService` now always runs once Home is reached (a class can start any time). Every second it re-evaluates the policy, updates the notification ("In <course> · N apps blocked until 10:00" / "Blocking is on" / "Not blocking · Next class …"), and runs the UsageStats check. If you're already in Instagram when class starts, it gets blocked within ~1 s.
- **Home:** status card (blocking/why/until), "Block outside class hours" switch (the old manual toggle; same pref key, so its value survives), today's schedule with the current class highlighted ("Now") and past ones dimmed.
- **Blocked apps screen:** during class you can add apps but not unblock them.
- **Hidden demo mode:** tap the **"ZeroTwo" title 5 times quickly** → fake class "Operating Systems (Demo)" starting now for 60 min (auto-expires). A "Demo · tap to end" chip appears next to the title; tapping it (or 5 more taps) ends it.

### Phase 4 — Screen time
- **Data:** `screentime/ScreenTimeRepository.kt` builds per-app, per-day foreground time for the last 7 days from raw `UsageStatsManager.queryEvents` (not `INTERVAL_DAILY` buckets, which don't align to local midnight on many phones). Foreground → background = a session; switching apps, screen off and shutdown close open sessions; sessions crossing midnight are split. Launchers and System UI are excluded; apps under 1 min per day are hidden. Apps in `BlockedAppsConfig` are tagged "Social".
- **Screen:** Home → "Screen time". Shows the selected day's total (big number), social-app time and 7-day average, a 7-day bar chart, and the per-app list (icon, name, time, thin share bar) for the selected day. Refreshes on resume. Without Usage Access, shows a message + "Open settings" instead of crashing.
- **Chart:** `ui/screentime/WeeklyBarChart.kt`, plain Compose `Canvas`. Single series in the accent; selected day full strength, others faded; bars ≤ 24 dp with rounded tops; hairline gridlines at clean steps (15m/30m/1h/2h…, ≤ 4 lines); value label only on the selected bar. Tap a column to select that day. TalkBack reads all 7 totals.
- **Manifest:** `<queries>` for launcher/home intents so other apps' names and icons are visible on Android 11+.

### Phase 5 — Mock ERP login + leaderboard
- **ERP login (MOCKED, integration-ready):** `erp/MockErpClient.kt` accepts any **5-digit ERP** (`[0-9]{5}`) plus a department + batch picked from `config/ErpMockConfig.kt` (departments: Computer Science, Mathematics, Social Sciences, Economics (Scam), BBA, ACF). The ERP field only accepts digits, max 5, with a number keypad. Profiles saved under older rules are treated as signed out. Comments mark where a real ERP call goes. Profile saved locally in `erp/StudentProfile.kt` (`StudentProfileStore`).
- **Flow:** onboarding → ERP sign-in → Home. App launch skips sign-in when a profile exists. Home footer shows "Signed in as …" + **Sign out** (back to sign-in; screen-time data in Firestore stays).
- **Firebase:** `cloud/FirebaseGateway.kt` — anonymous auth underneath (just to satisfy rules); Firestore collection `dailyScreenTime`, doc ID `{studentId}_{yyyy-MM-dd}`, fields `studentId, department, batch, date, totalMillis, socialMillis, uid, updatedAt`. One flat collection + equality-only filters → **no composite index needed**; sorting is done on the phone.
- **Upload:** `cloud/ScreenTimeSync.kt` uploads today + yesterday. Triggered on sign-in, on Home, every 15 min from `BlockerService`, and forced when the leaderboard opens/refreshes. Never throws; 15 s timeout; Firestore's offline cache queues writes.
- **Leaderboard:** Home → **Leaderboard**. Same department + batch, Today / Yesterday, ranked by **total** screen time, lowest first (ties by ID). Shows "You're #N of M", your row highlighted with full ID, classmates' ERPs masked to the last 2 digits (`•••17`), social time under each. Refresh button. Clear messages for: Firebase not configured, no connection, rules rejecting, anonymous auth disabled.
- **Rules:** `firestore.rules` in repo root (signed-in read; writes must have a 5-digit ERP, a known department, matching doc ID, own uid, sane millis). **Re-publish after changing them.**
- **Build:** the Google Services plugin is only applied if `app/google-services.json` exists (gitignored). Verified both ways.

### Firebase setup (one time)
1. console.firebase.google.com → Add project (Analytics off is fine).
2. Add Android app, package **`com.zerotwo.focus`** → download `google-services.json` → put it in `app/`.
3. Build → Authentication → Sign-in method → enable **Anonymous**.
4. Build → Firestore Database → Create database (any region, production mode) → Rules tab → paste `firestore.rules` → Publish.
5. Rebuild + reinstall.

### Phase 6 — Polish + demo
- **Demo mode end to end:** moved to `config/DemoConfig.kt`. 5 taps on the Home title → fake class (block screen says "You're in Operating Systems (Demo)") **and** the leaderboard adds 6 sample classmates (marked "sample", shown only on this phone, never uploaded; "Today" values scale with time of day). If Firebase is missing or unreachable, demo mode falls back to an **offline leaderboard** (your on-device time + sample classmates) instead of an error.
- **Home:** new "Screen time today" tile (tap → Screen time screen) replaces the plain button; demo toast explains what turned on.
- **Leaderboard states:** loading spinner, not-configured, error + Try again, empty-day message, upload-failed notice above the list, demo banner.
- **Onboarding:** on Xiaomi/Redmi/POCO/Oppo/realme/vivo/OnePlus/Huawei/Honor, an "Extra step" card explains the OEM Autostart / background pop-up switches with an App info button.
- **Fixed known issues:**
  - Reboot / app update: `blocker/BootReceiver.kt` (BOOT_COMPLETED + MY_PACKAGE_REPLACED) restarts `BlockerService` when onboarding is done.
  - Late demo class crossing midnight: `ClassSession.isInSession`/`isOver` handle wrap-around; schedule row shows "Now" correctly.
  - Screen time missed a session already open at the window's first midnight: events are now read from 24 h earlier and clipped.
  - Launcher icon has a monochrome layer (Android 13 themed icons).
- **Tests:** first JVM unit tests (`./gradlew testDebugUnitTest`): `ClassSessionTest` (incl. midnight wrap) and `FormatDurationTest`. 3/3 pass.
- **DEMO.md:** pre-stage checklist, 60-second script, recovery steps, Q&A talking points.

### UI re-theme — "mecha-cockpit HUD" (v1.0.2)
UI only; no logic or behavior changes.
- **Palette:** every color lives in `ui/theme/Color.kt`: background `#0B0508`, cards `#1A0A10` / elevated `#26101A`, primary crimson `#E0143C`, secondary hot pink `#FF4D8D`, highlight `#FFE3EC`, blocked/error `#FF2D2D`, success mint `#5CF2B0` (only the GRANTED badge), text `#FFE3EC` / `#B98A99`. Always dark (no light variant); dynamic color off.
- **Type:** `ui/theme/Type.kt`. Headings Orbitron (bold, wide tracking, uppercased at call sites since Compose has no text-transform), body Inter, numbers JetBrains Mono (`HudNumber`, `HudNumberLarge`, chart labels, schedule times, ranks, durations, ERP field). Fonts bundled in `res/font`; OFL licenses in `licenses/`.
- **Shapes:** `ui/theme/Shape.kt`: chamfered (cut-corner) cards, fields, chips and buttons.
- **Components:** `ui/theme/HudComponents.kt`: `HudCard` (1dp frame; tones Neutral / Active = crimson / Alert = red / Pink), `HudSectionLabel` (crimson tick + uppercase heading), pink outlined/text button colors (crimson text on near-black failed contrast).
- **Screens:** status card with "● SHIELD ACTIVE" readout; current class / your leaderboard row / un-granted permissions framed in crimson; podium ranks in pink; block screen with a red framed lock and "// ACCESS DENIED". Dark window background + light system-bar icons (no white flash, readable bars in system light mode); launcher icon background crimson.
- Checked by rendering onboarding, sign-in, Home (demo), leaderboard (demo) and the block screen with Robolectric + Roborazzi (temporary, not committed).

## Next
All planned phases done. Before demo day: run the full DEMO.md flow on the real phone once, with and without Wi-Fi.

## Known issues
- **Not verified on a device/emulator by Claude.** The Android emulator (37.x, API 35 image) segfaults on this host (kernel 7.2) even outside the sandbox, so all on-device checks are on you. It (≈4.4 GB in `~/Android/Sdk/emulator` + `system-images`) can be deleted.
- Android 13+ sideloaded APK (installed from file manager/browser): accessibility switch is greyed out ("restricted setting"). Fix: App info → ⋮ → Allow restricted settings (explained on the onboarding card). Installing with `adb install` avoids it.
- OEM ROMs (MIUI/HyperOS etc.) may block the background-launched block screen until "Autostart" / "Display pop-up windows while running in background" is allowed. The app can't detect these switches; the onboarding card explains them.
- The status notification is always shown, by design: the service has to be alive to notice a class starting.
- Timetable classes can't cross midnight (such JSON entries are skipped). Only the demo class can.
- Timetable uses device local time; changing the phone clock gets around blocking.
- Screen time may differ by a few minutes from Digital Wellbeing (different session rules).
- ERP is mocked: student IDs are self-declared, so anyone can post under any ID, and a reinstall can overwrite the same student's docs. Real ERP auth fixes both.
- Leaderboard fetches the whole batch for a day (no paging); fine at class size.
- Lint: only "newer dependency version available" warnings; versions are pinned on purpose.
- Usage access deep link with package URI only lands on our app on some ROMs; others show the full list.

## Build
Toolchain lives in `~/Android` (JDK 17, SDK 35, Gradle 8.9). Not on PATH. `~/.gradle/gradle.properties` forces IPv4 (Java hung on IPv6 on this machine).
```
export JAVA_HOME=~/Android/jdk ANDROID_HOME=~/Android/Sdk
./gradlew assembleDebug        # APK: app/build/outputs/apk/debug/app-debug.apk
./gradlew testDebugUnitTest    # JVM unit tests
~/Android/Sdk/platform-tools/adb install -r app/build/outputs/apk/debug/app-debug.apk
```
