package io.pocketbase.sample.viewmodel

import io.pocketbase.sample.client.PocketBaseSingleton
import io.pocketbase.sample.data.User
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

class AuthViewModel(private val scope: CoroutineScope) {
    private val client = PocketBaseSingleton.client

    // Auth state
    private val _isLoggedIn = MutableStateFlow(client.authStore.isValid)
    val isLoggedIn = _isLoggedIn.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error = _error.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading = _isLoading.asStateFlow()

    init {
        client.authStore.onChange.onEach {
            _isLoggedIn.value = client.authStore.isValid
        }.launchIn(scope)
    }

    fun login(email: String, password: String) {
        scope.launch(Dispatchers.IO) {
            _isLoading.value = true
            try {
                // authWithPassword returns RecordAuth<User>, saves to store automatically?
                // Yes, RecordService.authWithPassword saves to authStore.
                client.collection("users", User::class).authWithPassword(email, password)
                _error.value = null
            } catch (e: Exception) {
                e.printStackTrace()
                _error.value = "Login failed: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun signup(email: String, password: String, passwordConfirm: String) {
        scope.launch(Dispatchers.IO) {
            _isLoading.value = true
            try {
                val user = User(
                    email = email,
                    password = password,
                    passwordConfirm = passwordConfirm,
                    emailVisibility = true
                )
                client.collection("users", User::class).create(user)
                // Auto login
                login(email, password)
            } catch (e: Exception) {
                e.printStackTrace()
                _error.value = "Signup failed: ${e.message}"
                _isLoading.value = false
            }
        }
    }

    fun forgotPassword(email: String) {
        scope.launch(Dispatchers.IO) {
             _isLoading.value = true
             try {
                 client.collection("users", User::class).requestPasswordReset(email)
                 _error.value = "Password reset email sent!"
             } catch (e: Exception) {
                 e.printStackTrace()
                 _error.value = "Request failed: ${e.message}"
             } finally {
                 _isLoading.value = false
             }
        }
    }

    fun logout() {
        client.authStore.clear()
    }
}
