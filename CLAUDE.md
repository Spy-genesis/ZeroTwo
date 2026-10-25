Project: Class-aware screen time blocker (Android, hackathon, 48h)
Stack: Kotlin, Jetpack Compose, Material 3, min SDK 26, MVVM, single module, no Hilt.
Firebase Auth (anonymous) + Firestore for leaderboard only. No custom backend.
Test target: real device.

Concept: blocks social apps, auto-blocks during class hours from a mock timetable, batch/department screen-time leaderboard.

Rules:
Config (blocked package names, mock timetable JSON) lives in clearly named files.
Handle permission-denied gracefully, no crashes.
ERP link is MOCKED (student ID + department picker). Comment it as "integration-ready, mocked for demo".
Out of scope: real ERP, iOS, push notifications, custom backend, analytics.
After finishing each phase: make sure it builds, git commit, update PROGRESS.md (done / next / known issues), then STOP and tell me what to test on device.
At the start of any session: read PROGRESS.md and continue from the next phase.
