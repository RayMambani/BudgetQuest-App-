package com.budgetquest.app.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.budgetquest.app.data.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel backing the login and registration screens.
 *
 * It holds the form state (username, password, loading flag) as [MutableStateFlow]s
 * that the Compose UI observes, and exposes one-shot outcomes (success / error) via
 * a [MutableSharedFlow] so transient events such as a snackbar fire exactly once and
 * are not re-emitted on configuration changes. All credential work is delegated to
 * the [AuthRepository]; this class only validates input and coordinates UI state.
 */
@HiltViewModel
class AuthViewModel @Inject constructor(
    private val repository: AuthRepository
) : ViewModel() {

    private val _username = MutableStateFlow("")
    val username = _username.asStateFlow()

    private val _password = MutableStateFlow("")
    val password = _password.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading = _isLoading.asStateFlow()

    private val _authEvent = MutableSharedFlow<AuthResult>()
    val authEvent = _authEvent.asSharedFlow()

    fun onUsernameChange(value: String) { _username.value = value }
    fun onPasswordChange(value: String) { _password.value = value }

    /** Trims input, guards against empty fields, then asks the repository to authenticate. */
    fun login() {
        viewModelScope.launch {
            val cleanUsername = _username.value.trim()
            val cleanPassword = _password.value.trim()

            if (cleanUsername.isEmpty() || cleanPassword.isEmpty()) {
                Log.w(TAG, "login() blocked: empty username or password")
                _authEvent.emit(AuthResult.Error("Please enter both username and password"))
                return@launch
            }

            Log.d(TAG, "login() submitting for '$cleanUsername'")
            _isLoading.value = true
            val success = repository.login(cleanUsername, cleanPassword)
            _isLoading.value = false

            if (success) {
                Log.i(TAG, "login() success for '$cleanUsername'")
                _authEvent.emit(AuthResult.Success)
            } else {
                Log.w(TAG, "login() rejected for '$cleanUsername'")
                _authEvent.emit(AuthResult.Error("Invalid credentials"))
            }
        }
    }

    /** Enforces the password policy before delegating account creation to the repository. */
    fun register() {
        viewModelScope.launch {
            val cleanUsername = _username.value.trim()
            val cleanPassword = _password.value.trim()

            val passwordError = validatePassword(cleanPassword)
            if (passwordError != null) {
                Log.w(TAG, "register() blocked by password policy: $passwordError")
                _authEvent.emit(AuthResult.Error(passwordError))
                return@launch
            }

            Log.d(TAG, "register() submitting for '$cleanUsername'")
            _isLoading.value = true
            val success = repository.register(cleanUsername, cleanPassword)
            _isLoading.value = false

            if (success) {
                Log.i(TAG, "register() success for '$cleanUsername'")
                _authEvent.emit(AuthResult.Success)
            } else {
                Log.w(TAG, "register() failed: username '$cleanUsername' already exists")
                _authEvent.emit(AuthResult.Error("Username already exists"))
            }
        }
    }

    private fun validatePassword(password: String): String? {
        if (password.length < 6) return "Password must be at least 6 characters"
        if (!password.any { it.isUpperCase() }) return "Password must contain an uppercase letter"
        if (!password.any { it.isLowerCase() }) return "Password must contain a lowercase letter"
        if (!password.any { it.isDigit() }) return "Password must contain a number"
        if (!password.any { !it.isLetterOrDigit() }) return "Password must contain a symbol"
        return null
    }

    /** Represents the outcome of an auth attempt, emitted as a one-shot UI event. */
    sealed class AuthResult {
        object Success : AuthResult()
        data class Error(val message: String) : AuthResult()
    }

    companion object {
        private const val TAG = "AuthViewModel"
    }
}
