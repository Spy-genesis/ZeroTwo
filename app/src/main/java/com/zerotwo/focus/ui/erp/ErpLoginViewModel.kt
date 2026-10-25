package com.zerotwo.focus.ui.erp

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.zerotwo.focus.cloud.ScreenTimeSync
import com.zerotwo.focus.erp.MockErpClient
import com.zerotwo.focus.erp.StudentProfileStore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ErpLoginUiState(
    val studentId: String = "",
    val department: String? = null,
    val batch: String? = null,
    val signingIn: Boolean = false,
    val error: String? = null,
    val signedIn: Boolean = false,
)

class ErpLoginViewModel(application: Application) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(ErpLoginUiState())
    val uiState: StateFlow<ErpLoginUiState> = _uiState.asStateFlow()

    fun setStudentId(value: String) = _uiState.update { it.copy(studentId = value, error = null) }
    fun setDepartment(value: String) = _uiState.update { it.copy(department = value, error = null) }
    fun setBatch(value: String) = _uiState.update { it.copy(batch = value, error = null) }

    fun signIn() {
        val s = _uiState.value
        if (s.signingIn) return
        _uiState.update { it.copy(signingIn = true, error = null) }
        viewModelScope.launch {
            MockErpClient.signIn(s.studentId, s.department, s.batch)
                .onSuccess { profile ->
                    StudentProfileStore.get(getApplication()).save(profile)
                    // Fire-and-forget: the app works offline; the upload retries later if this fails.
                    launch { ScreenTimeSync.sync(getApplication(), force = true) }
                    _uiState.update { it.copy(signingIn = false, signedIn = true) }
                }
                .onFailure { e -> _uiState.update { it.copy(signingIn = false, error = e.message) } }
        }
    }
}
