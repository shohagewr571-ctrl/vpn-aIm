package com.aim.vpn.presentation.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aim.vpn.data.auth.AuthRepository
import com.aim.vpn.domain.AccessKeyResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class LoginUiState(
    val keyInput: String = "",
    val isLoading: Boolean = false,
    val error: String? = null,
    val success: Boolean = false
)

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    private val _sessionValid = MutableStateFlow(false)
    val sessionValid: StateFlow<Boolean> = _sessionValid.asStateFlow()

    private val _sessionChecked = MutableStateFlow(false)
    val sessionChecked: StateFlow<Boolean> = _sessionChecked.asStateFlow()

    init {
        viewModelScope.launch {
            _sessionValid.value = runCatching { authRepository.hasValidSession() }.getOrDefault(false)
            _sessionChecked.value = true
        }
    }

    fun onKeyChanged(value: String) {
        _uiState.value = _uiState.value.copy(keyInput = value, error = null)
    }

    fun login() {
        val current = _uiState.value
        if (current.isLoading) return
        if (current.keyInput.isBlank()) {
            _uiState.value = current.copy(error = "Access key cannot be empty")
            return
        }
        viewModelScope.launch {
            _uiState.value = current.copy(isLoading = true, error = null)
            val result: AccessKeyResult = authRepository.validateAccessKey(current.keyInput)
            if (result.valid) {
                _sessionValid.value = true
                _uiState.value = _uiState.value.copy(isLoading = false, success = true, error = null)
            } else {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    success = false,
                    error = result.message.ifBlank { "Invalid Access Key" }
                )
            }
        }
    }
}
