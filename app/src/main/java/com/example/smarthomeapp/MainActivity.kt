package com.example.smarthomeapp

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class MainActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseDatabase
    private lateinit var userSettingsRef: DatabaseReference
    private lateinit var sensorsRef: DatabaseReference
    private lateinit var controlsRef: DatabaseReference
    private lateinit var deviceSettingsRef: DatabaseReference

    private var homeName by mutableStateOf("Smart Home Control")
    private var isAutoMode by mutableStateOf(false)
    private var temperature by mutableStateOf(0.0)
    private var humidity by mutableStateOf(0.0)
    private var light by mutableStateOf(0.0)
    private var fanOn by mutableStateOf(false)
    private var lightsOn by mutableStateOf(false)
    private var alarmOn by mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        auth = FirebaseAuth.getInstance()
        db = FirebaseDatabase.getInstance()

        val userId = auth.currentUser?.uid
        if (userId == null) {
            goToLoginActivity()
            return
        }

        userSettingsRef = db.getReference("users/$userId/settings")
        sensorsRef = db.getReference("sensors")
        controlsRef = db.getReference("controls")
        deviceSettingsRef = db.getReference("device/settings")

        setupListeners()

        setContent {
            val onModeChange = remember<(Boolean) -> Unit> { { newMode ->
                deviceSettingsRef.child("mode").setValue(if (newMode) "auto" else "manual")
            } }
            val onFanClick = remember<() -> Unit> { { toggleControl("fan") } }
            val onLightsClick = remember<() -> Unit> { { toggleControl("lights") } }
            val onAlarmClick = remember<() -> Unit> { { toggleControl("alarm") } }
            val onSettingsClick = remember<() -> Unit> { { startActivity(Intent(this, SettingsActivity::class.java)) } }
            val onLogoutClick = remember<() -> Unit> { {
                auth.signOut()
                goToLoginActivity()
            } }

            MainScreen(
                homeName = homeName,
                isAutoMode = isAutoMode,
                onModeChange = onModeChange,
                temperature = temperature,
                humidity = humidity,
                light = light,
                fanOn = fanOn,
                lightsOn = lightsOn,
                alarmOn = alarmOn,
                onFanClick = onFanClick,
                onLightsClick = onLightsClick,
                onAlarmClick = onAlarmClick,
                onSettingsClick = onSettingsClick,
                onLogoutClick = onLogoutClick
            )
        }
    }

    private fun setupListeners() {
        userSettingsRef.child("home_name").addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val name = snapshot.getValue(String::class.java)
                homeName = if (!name.isNullOrEmpty()) name else "Smart Home Control"
            }
            override fun onCancelled(error: DatabaseError) { /* Handle error */ }
        })

        controlsRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                fanOn = snapshot.child("fan").getValue(Boolean::class.java) ?: false
                lightsOn = snapshot.child("lights").getValue(Boolean::class.java) ?: false
                alarmOn = snapshot.child("alarm").getValue(Boolean::class.java) ?: false
            }
            override fun onCancelled(error: DatabaseError) {
                showToast("Failed to read control states: ${error.message}")
            }
        })

        sensorsRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                temperature = snapshot.child("temperature").getValue(Double::class.java) ?: 0.0
                humidity = snapshot.child("humidity").getValue(Double::class.java) ?: 0.0
                val lightValue = snapshot.child("light").getValue(Any::class.java)
                light = when(lightValue) {
                    is Double -> lightValue
                    is Long -> lightValue.toDouble()
                    else -> 0.0
                }
            }
            override fun onCancelled(error: DatabaseError) {
                showToast("Failed to read sensor data: ${error.message}")
            }
        })

        deviceSettingsRef.child("mode").addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                isAutoMode = snapshot.getValue(String::class.java) == "auto"
            }
            override fun onCancelled(error: DatabaseError) { /* Handle error */ }
        })
    }

    private fun toggleControl(control: String) {
        controlsRef.child(control).get().addOnSuccessListener { snapshot ->
            val currentState = snapshot.getValue(Boolean::class.java) ?: false
            controlsRef.child(control).setValue(!currentState)
        }
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    private fun goToLoginActivity() {
        val intent = Intent(this, LoginActivity::class.java)
        startActivity(intent)
        finish()
    }
}
