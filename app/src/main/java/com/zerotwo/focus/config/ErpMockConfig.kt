package com.zerotwo.focus.config

/**
 * MOCK ERP CONFIG. Integration-ready, mocked for demo: the real ERP would supply
 * departments and batches from its own API; for the hackathon they're hardcoded here.
 */
object ErpMockConfig {

    val DEPARTMENTS: List<String> = listOf(
        "Computer Science (CSE)",
        "Information Technology (IT)",
        "Electronics & Communication (ECE)",
        "Electrical (EEE)",
        "Mechanical (ME)",
        "Civil (CE)",
    )

    val BATCHES: List<String> = listOf("2022", "2023", "2024", "2025")

    /** Accepted student ID shape: 4-16 letters/digits, e.g. 21CS1045. */
    val STUDENT_ID_PATTERN = Regex("^[A-Z0-9]{4,16}$")
}
