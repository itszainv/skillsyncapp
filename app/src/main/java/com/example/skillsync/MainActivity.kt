package com.example.skillsync

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.*
import com.example.skillsync.screens.DashboardScreen
import com.example.skillsync.screens.LoginScreen
import com.example.skillsync.screens.RegisterScreen
import com.google.firebase.auth.FirebaseAuth

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            val user = FirebaseAuth.getInstance().currentUser
            var screen by remember { mutableStateOf(if (user != null) "dashboard" else "login") }

            when (screen) {

                "login" -> LoginScreen(
                    onLoginSuccess = { screen = "dashboard" },
                    onGoToRegister = { screen = "register" }
                )

                "register" -> RegisterScreen(
                    onRegisterSuccess = { screen = "login" }
                )

                "dashboard" -> DashboardScreen(
                    onLogout = { screen = "login" }
                )
            }
        }
    }
}