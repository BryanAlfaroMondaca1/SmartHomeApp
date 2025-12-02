package com.example.smarthomeapp

import android.content.Intent
import android.os.Bundle
import android.util.Patterns
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.remember
import com.google.firebase.auth.FirebaseAuth

class LoginActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        auth = FirebaseAuth.getInstance()

        // Check if user is already logged in
        if (auth.currentUser != null) {
            goToMainActivity()
            return
        }

        setContent {
            val onLoginClick = remember<(String, String) -> Unit> { { email, password -> handleLogin(email, password) } }
            val onRegisterClick = remember<(String, String) -> Unit> { { email, password -> handleRegister(email, password) } }

            LoginScreen(
                onLoginClick = onLoginClick,
                onRegisterClick = onRegisterClick
            )
        }
    }

    private fun handleLogin(email: String, password: String) {
        if (!validateInput(email, password)) return

        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    Toast.makeText(this, "Login successful!", Toast.LENGTH_SHORT).show()
                    goToMainActivity()
                } else {
                    Toast.makeText(this, "Login failed: ${task.exception?.message}", Toast.LENGTH_LONG).show()
                }
            }
    }

    private fun handleRegister(email: String, password: String) {
        if (!validateInput(email, password)) return

        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    Toast.makeText(this, "Registration successful!", Toast.LENGTH_SHORT).show()
                    // You might want to automatically log in the user or ask them to log in.
                } else {
                    Toast.makeText(this, "Registration failed: ${task.exception?.message}", Toast.LENGTH_LONG).show()
                }
            }
    }

    private fun validateInput(email: String, password: String): Boolean {
        if (email.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Email and password cannot be empty", Toast.LENGTH_SHORT).show()
            return false
        }

        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            Toast.makeText(this, "Please enter a valid email address", Toast.LENGTH_SHORT).show()
            return false
        }

        if (password.length < 6) {
            Toast.makeText(this, "Password must be at least 6 characters long", Toast.LENGTH_SHORT).show()
            return false
        }

        return true
    }

    private fun goToMainActivity() {
        val intent = Intent(this, MainActivity::class.java)
        startActivity(intent)
        finish() // Prevents user from coming back to the login screen
    }
}
