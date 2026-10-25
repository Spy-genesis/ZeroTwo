package com.zerotwo.focus.config

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ErpMockConfigTest {

    @Test
    fun erpIsExactlyFiveDigits() {
        assertTrue(ErpMockConfig.STUDENT_ID_PATTERN.matches("24117"))
        assertTrue(ErpMockConfig.STUDENT_ID_PATTERN.matches("00001"))
        assertFalse(ErpMockConfig.STUDENT_ID_PATTERN.matches("2411"))
        assertFalse(ErpMockConfig.STUDENT_ID_PATTERN.matches("241170"))
        assertFalse(ErpMockConfig.STUDENT_ID_PATTERN.matches("2411a"))
        assertFalse(ErpMockConfig.STUDENT_ID_PATTERN.matches("21CS1045"))
    }

    @Test
    fun demoClassmatesUseValidErps() {
        DemoConfig.DEMO_CLASSMATES.forEach { assertTrue(it.studentId, ErpMockConfig.STUDENT_ID_PATTERN.matches(it.studentId)) }
    }
}
