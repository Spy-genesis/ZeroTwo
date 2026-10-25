# ZeroTwo — 60-second demo

One phone, works offline. Demo mode fakes "a class is in session right now" and adds sample classmates to the leaderboard, so nothing depends on the real timetable, the time of day or venue Wi-Fi.

## Before you go on stage (5 min)
- [ ] Latest build installed (`adb install -r app/build/outputs/apk/debug/app-debug.apk`).
- [ ] Onboarding done: all 3 required cards **Granted** (battery one too, ideally).
- [ ] Signed in, e.g. `21CS1050` · Computer Science (CSE) · 2024.
- [ ] Instagram (or YouTube) installed and **on the home screen dock**, so you can open it in one tap.
- [ ] Phone used a bit today, so screen time isn't 0m.
- [ ] Blocking **off** on Home and demo mode **off** (no "Demo" chip next to the title).
- [ ] Do Not Disturb on, brightness up, screen mirroring tested.
- [ ] Open ZeroTwo Home and leave it there.

## Script

| Time | Do | Say |
|---|---|---|
| 0:00–0:08 | Show Home. | "Students spend hours a day on social apps, a lot of it during lectures. ZeroTwo is a screen-time blocker that knows your class timetable." |
| 0:08–0:18 | Point at **Today's schedule**. Tap the **ZeroTwo** title 5× fast; the status card turns indigo: *Blocking · in Operating Systems (Demo)*. | "The timetable comes from the college ERP. When a class starts, blocking switches on by itself. Let's pretend a lecture just started." |
| 0:18–0:30 | Press Home, tap **Instagram**. The block screen appears: *You're in Operating Systems (Demo)*. Tap **Go Home**. | "Instagram is blocked until class ends. No willpower needed, and no off switch mid-lecture: the blocked-apps list is locked during class." |
| 0:30–0:42 | Back in ZeroTwo, tap **Screen time today**. Tap a couple of bars. | "Outside class you decide. It tracks per-app screen time for the week, and social apps are tagged." |
| 0:42–0:55 | Back, tap **Leaderboard**. | "And it's social in a good way: you're ranked against your own batch and department, lowest screen time wins. Classmates' IDs are masked." |
| 0:55–1:00 | Back to Home. | "ZeroTwo: focus that follows your timetable." |

## If something goes wrong
- **Block screen doesn't appear:** open Instagram again (the 1-second backup check catches it). On Xiaomi/Oppo/Vivo, the "Extra step" card in Review permissions explains the missing OEM switch.
- **Leaderboard shows an error:** demo mode falls back to an offline leaderboard (your on-device time + sample classmates), so make sure the Demo chip is showing.
- **Demo mode ran out** (it lasts 60 minutes): tap the title 5× again.
- **Reset after the demo:** tap the "Demo · tap to end" chip.

## Talking points for questions
- **How does it detect apps?** An accessibility service (package name only, never screen content), with a UsageStats check every second as backup.
- **Why won't Android kill it?** A foreground service with a persistent notification, the battery optimization exemption, and a restart after reboot.
- **ERP?** Integration-ready, mocked for the demo: student ID + department/batch picker, and the timetable is a JSON file the ERP would supply.
- **Backend?** None of our own: Firebase anonymous auth + Firestore, one document per student per day.
