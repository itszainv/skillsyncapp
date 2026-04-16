package com.example.skillsync

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.LaunchedEffect
import com.example.skillsync.admin.AdminDashboard
import com.example.skillsync.auth.AuthViewModel
import com.example.skillsync.screens.LoginScreen
import com.example.skillsync.screens.RegisterScreen
import com.example.skillsync.student.StudentDashboardScreen
import com.example.skillsync.ui.theme.SkillSyncTheme
import com.example.skillsync.data.FirestoreRepository
import com.google.firebase.auth.FirebaseAuth

class MainActivity : ComponentActivity() {
    private val authViewModel: AuthViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            val repo = remember { FirestoreRepository() }
            val user = FirebaseAuth.getInstance().currentUser
            var themeKey by remember { mutableStateOf("dark_red") }
            var screen by remember {
                mutableStateOf(
                    when {
                        user == null -> "login"
                        authViewModel.isAdmin() -> "admin"
                        else -> "student"
                    }
                )
            }

            LaunchedEffect(screen, user?.uid) {
                if (screen == "student" && FirebaseAuth.getInstance().currentUser != null) {
                    themeKey = repo.getUserProfileModel().selectedTheme
                } else {
                    themeKey = "dark_red"
                }
            }

            SkillSyncTheme(themeKey = themeKey) {
                when (screen) {
                    "login" -> LoginScreen(
                        onLoginSuccess = {
                            screen = if (authViewModel.isAdmin()) "admin" else "student"
                        },
                        onGoToRegister = { screen = "register" }
                    )

                    "register" -> RegisterScreen(
                        onRegisterSuccess = { screen = "login" },
                        onBackToLogin = { screen = "login" }
                    )

                    "student" -> StudentDashboardScreen(
                        onLogout = {
                            authViewModel.logout()
                            screen = "login"
                            themeKey = "dark_red"
                        },
                        onThemeChanged = { newTheme ->
                            themeKey = newTheme
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
}