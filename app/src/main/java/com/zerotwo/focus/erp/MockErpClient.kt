package com.zerotwo.focus.erp

import com.zerotwo.focus.config.ErpMockConfig
import kotlinx.coroutines.delay

/**
 * College ERP link — INTEGRATION-READY, MOCKED FOR DEMO.
 *
 * A real integration would call the ERP's auth API (e.g. OAuth/SSO), and get the student's
 * department and batch back from it. Here the student picks them, and any well-formed ID is
 * accepted. Swap the body of [signIn] for the real call; callers don't need to change.
 */
object MockErpClient {

    suspend fun signIn(rawStudentId: String, department: String?, batch: String?): Result<StudentProfile> {
        delay(600) // feels like a network round trip in the demo
        val studentId = rawStudentId.trim()
        return when {
            !ErpMockConfig.STUDENT_ID_PATTERN.matches(studentId) ->
                Result.failure(IllegalArgumentException("ERP must be exactly ${ErpMockConfig.ERP_ID_LENGTH} digits."))
            department == null || department !in ErpMockConfig.DEPARTMENTS ->
                Result.failure(IllegalArgumentException("Pick your department."))
            batch == null || batch !in ErpMockConfig.BATCHES ->
                Result.failure(IllegalArgumentException("Pick your batch."))
            else -> Result.success(StudentProfile(studentId, department, batch))
        }
    }
}
