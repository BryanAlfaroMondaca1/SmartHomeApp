package com.example.smarthomeapp

import android.os.Bundle
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.example.smarthomeapp.data.Settings
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class SettingsActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var userSettingsRef: DatabaseReference

    private var settings by mutableStateOf(Settings())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        auth = FirebaseAuth.getInstance()
        val userId = auth.currentUser?.uid
        if (userId == null) {
            finish() // Should not happen if called from MainActivity
            return
        }

        userSettingsRef = FirebaseDatabase.getInstance().getReference("users/$userId/settings")

        loadSettings()

        setContent {
            val onSettingsChange = remember<(Settings) -> Unit> { { newSettings -> settings = newSettings } }
            val onSaveClick = remember<() -> Unit> { { saveSettings() } }
            val onBackClick = remember<() -> Unit> { { finish() } }

            SettingsScreen(
                settings = settings,
                onSettingsChange = onSettingsChange,
                onSaveClick = onSaveClick,
                onBackClick = onBackClick
            )
        }
    }

    private fun loadSettings() {
        userSettingsRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val loadedSettings = snapshot.getValue(Settings::class.java)
                if (loadedSettings != null) {
                    settings = loadedSettings
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@SettingsActivity, "Failed to load settings: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun saveSettings() {
        if (settings.tempMin >= settings.tempMax || settings.humidityMin >= settings.humidityMax || settings.lightMin >= settings.lightMax) {
            Toast.makeText(this, "Invalid range: min value must be less than max value", Toast.LENGTH_LONG).show()
            return
        }

        userSettingsRef.setValue(settings)
            .addOnSuccessListener {
                Toast.makeText(this, "Settings saved!", Toast.LENGTH_SHORT).show()
                finish()
            }
            .addOnFailureListener {
                Toast.makeText(this, "Failed to save settings: ${it.message}", Toast.LENGTH_SHORT).show()
            }
    }
}
