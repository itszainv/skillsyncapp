package com.example.skillsync.auth

import androidx.lifecycle.ViewModel
import com.google.firebase.Firebase
import com.google.firebase.auth.auth

class AuthViewModel : ViewModel() {
    private val auth = Firebase.auth

    private val adminEmails = setOf(
        "ashoeb@umich.edu",
        "zainv@umich.edu",
        // kage
        // james
    )

    fun login(email: String, password: String, onResult: (Boolean, String?) -> Unit) {
        auth.signInWithEmailAndPassword(email, password)
            .addOnSuccessListener { onResult(true, null) }
            .addOnFailureListener { onResult(false, it.message) }
    }

    fun isAdmin(): Boolean {
        return auth.currentUser?.email in adminEmails
    }

    fun logout() { auth.signOut() }
}