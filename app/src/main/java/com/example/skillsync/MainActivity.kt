package com.example.skillsync

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.runtime.*
import com.example.skillsync.auth.AuthViewModel
import com.example.skillsync.screens.DashboardScreen
import com.example.skillsync.screens.LoginScreen
import com.example.skillsync.screens.RegisterScreen
import com.example.skillsync.admin.AdminDashboard
import com.google.firebase.auth.FirebaseAuth

class MainActivity : ComponentActivity() {
    private val authViewModel: AuthViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            val user = FirebaseAuth.getInstance().currentUser
            var screen by remember {
                mutableStateOf(
                    when {
                        user == null -> "login"
                        authViewModel.isAdmin() -> "admin"
                        else -> "dashboard"
                    }
                )
            }

            when (screen) {
                "login" -> LoginScreen(
                    onLoginSuccess = {
                        screen = if (authViewModel.isAdmin()) "admin" else "dashboard"
                    },
                    onGoToRegister = { screen = "register" }
                )

                "register" -> RegisterScreen(
                    onRegisterSuccess = { screen = "login" }
                )

                "dashboard" -> DashboardScreen(
                    onLogout = {
                        authViewModel.logout()
                        screen = "login"
                    }
                )

                "admin" -> AdminDashboard(
                    onLogout = {
                        authViewModel.logout()
                        screen = "login"
                    }
                )
            }
        }
    }
}