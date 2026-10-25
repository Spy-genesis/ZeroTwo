package com.zerotwo.focus.erp

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class StudentProfile(
    val studentId: String,
    val department: String,
    val batch: String,
)

/** The signed-in student, kept on the device. Null when signed out. */
class StudentProfileStore private constructor(context: Context) {

    private val prefs = context.getSharedPreferences("student", Context.MODE_PRIVATE)

    private val _profile = MutableStateFlow(read())
    val profile: StateFlow<StudentProfile?> = _profile.asStateFlow()

    fun save(profile: StudentProfile) {
        prefs.edit()
            .putString(KEY_ID, profile.studentId)
            .putString(KEY_DEPARTMENT, profile.department)
            .putString(KEY_BATCH, profile.batch)
            .apply()
        _profile.value = profile
    }

    fun clear() {
        prefs.edit().clear().apply()
        _profile.value = null
    }

    private fun read(): StudentProfile? {
        val id = prefs.getString(KEY_ID, null) ?: return null
        val department = prefs.getString(KEY_DEPARTMENT, null) ?: return null
        val batch = prefs.getString(KEY_BATCH, null) ?: return null
        return StudentProfile(id, department, batch)
    }

    companion object {
        private const val KEY_ID = "student_id"
        private const val KEY_DEPARTMENT = "department"
        private const val KEY_BATCH = "batch"

        @Volatile private var instance: StudentProfileStore? = null

        fun get(context: Context): StudentProfileStore =
            instance ?: synchronized(this) {
                instance ?: StudentProfileStore(context.applicationContext).also { instance = it }
            }
    }
}
