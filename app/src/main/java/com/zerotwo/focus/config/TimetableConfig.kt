package com.zerotwo.focus.config

/** TIMETABLE CONFIG: where the mock timetable lives and how demo mode behaves. */
object TimetableConfig {

    /** Mock timetable in app/src/main/assets. Integration-ready, mocked for demo: the ERP would replace this file. */
    const val TIMETABLE_ASSET = "mock_timetable.json"

    /** Demo mode fakes this class starting the moment it's switched on. */
    const val DEMO_COURSE_NAME = "Operating Systems (Demo)"
    const val DEMO_DURATION_MINUTES = 60L

    /** Taps on the Home title needed to toggle the hidden demo mode. */
    const val DEMO_UNLOCK_TAPS = 5
}
