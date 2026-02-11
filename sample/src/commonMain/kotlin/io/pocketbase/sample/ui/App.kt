package io.pocketbase.sample.ui

import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.*
import io.pocketbase.sample.viewmodel.AuthViewModel

enum class AuthScreen {
    Login,
    Signup,
    ForgotPassword
}

@Composable
fun App() {
    MaterialTheme {
        val scope = rememberCoroutineScope()
        // Ideally ViewModel should survive config changes but for sample remember is ok
        val authViewModel = remember { AuthViewModel(scope) }
        val isLoggedIn by authViewModel.isLoggedIn.collectAsState()

        if (isLoggedIn) {
            HomeScreen(
                authViewModel = authViewModel,
                onLogout = { authViewModel.logout() }
            )
        } else {
            var screen by remember { mutableStateOf(AuthScreen.Login) }

            when (screen) {
                AuthScreen.Login -> LoginScreen(
                    viewModel = authViewModel,
                    onSignupClick = { screen = AuthScreen.Signup },
                    onForgotPasswordClick = { screen = AuthScreen.ForgotPassword }
                )
                AuthScreen.Signup -> SignupScreen(
                    viewModel = authViewModel,
                    onBack = { screen = AuthScreen.Login }
                )
                AuthScreen.ForgotPassword -> ForgotPasswordScreen(
                    viewModel = authViewModel,
                    onBack = { screen = AuthScreen.Login }
                )
            }
        }
    }
}
