package io.pocketbase.sample.ui

import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.*
import io.pocketbase.sample.viewmodel.AuthViewModel
import io.pocketbase.sample.viewmodel.TodoViewModel

enum class AuthScreen {
    Login,
    Signup,
    ForgotPassword
}

@Composable
fun App() {
    MaterialTheme {
        val scope = rememberCoroutineScope()
        val authViewModel = remember { AuthViewModel(scope) }
        val isLoggedIn by authViewModel.isLoggedIn.collectAsState()

        if (isLoggedIn) {
            val todoViewModel = remember { TodoViewModel(scope) }
            var selectedTodoId by remember { mutableStateOf<String?>(null) }

            if (selectedTodoId != null) {
                TodoDetailScreen(
                    todoId = selectedTodoId!!,
                    viewModel = todoViewModel,
                    onBack = { selectedTodoId = null }
                )
            } else {
                HomeScreen(
                    authViewModel = authViewModel,
                    todoViewModel = todoViewModel, // Pass shared VM
                    onLogout = { authViewModel.logout() },
                    onTodoClick = { todo -> selectedTodoId = todo.id }
                )
            }
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
