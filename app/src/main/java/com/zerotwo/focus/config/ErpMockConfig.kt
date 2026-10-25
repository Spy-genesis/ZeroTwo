package com.zerotwo.focus.config

/**
 * MOCK ERP CONFIG. Integration-ready, mocked for demo: the real ERP would supply
 * departments and batches from its own API; for the hackathon they're hardcoded here.
 */
object ErpMockConfig {

    val DEPARTMENTS: List<String> = listOf(
        "Computer Science",
        "Mathematics",
        "Social Sciences",
        "Economics (Scam)",
        "BBA",
        "ACF",
    )

    val BATCHES: List<String> = listOf("2022", "2023", "2024", "2025")

    /** ERP IDs are exactly 5 digits, e.g. 24117. */
    const val ERP_ID_LENGTH = 5
    val STUDENT_ID_PATTERN = Regex("^[0-9]{$ERP_ID_LENGTH}$")
}
