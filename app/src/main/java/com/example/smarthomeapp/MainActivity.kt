package com.example.smarthomeapp

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.smarthomeapp.databinding.ActivityMainBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import java.util.Locale

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseDatabase
    private lateinit var userSettingsRef: DatabaseReference
    private lateinit var sensorsRef: DatabaseReference
    private lateinit var controlsRef: DatabaseReference

    private val sensorUpdateHandler = Handler(Looper.getMainLooper())
    private lateinit var sensorUpdateRunnable: Runnable

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

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

        setupUI()
        setupListeners()
        startSensorSimulation()
    }

    private fun setupUI() {
        binding.btnLogout.setOnClickListener {
            auth.signOut()
            goToLoginActivity()
        }

        binding.btnSettings.setOnClickListener {
            val intent = Intent(this, SettingsActivity::class.java)
            startActivity(intent)
        }
    }

    private fun setupListeners() {
        // Listen for home name changes
        userSettingsRef.child("home_name").addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val homeName = snapshot.getValue(String::class.java)
                if (!homeName.isNullOrEmpty()) {
                    binding.tvTitle.text = homeName
                } else {
                    binding.tvTitle.text = "Smart Home Control"
                }
            }
            override fun onCancelled(error: DatabaseError) { /* Handle error */ }
        })

        // Listen for control state changes
        controlsRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                updateControlButtons(snapshot)
            }
            override fun onCancelled(error: DatabaseError) {
                showToast("Failed to read control states: ${error.message}")
            }
        })

        // Listen for sensor data changes
        sensorsRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                updateSensorReadings(snapshot)
            }
            override fun onCancelled(error: DatabaseError) {
                showToast("Failed to read sensor data: ${error.message}")
            }
        })

        // Listen for mode changes
        userSettingsRef.child("mode").addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val isAutoMode = snapshot.getValue(String::class.java) == "auto"
                binding.switchMode.isChecked = isAutoMode
                binding.tvModeLabel.text = if (isAutoMode) "Automatic Mode" else "Manual Mode"
            }
            override fun onCancelled(error: DatabaseError) { /* Handle error */ }
        })

        // Control button clicks
        binding.btnFan.setOnClickListener { toggleControl("fan") }
        binding.btnLights.setOnClickListener { toggleControl("lights") }
        binding.btnAlarm.setOnClickListener { toggleControl("alarm") }

        // Mode switch change
        binding.switchMode.setOnCheckedChangeListener { _, isChecked ->
            val newMode = if (isChecked) "auto" else "manual"
            userSettingsRef.child("mode").setValue(newMode)
        }
    }

    private fun updateControlButtons(snapshot: DataSnapshot) {
        val fanOn = snapshot.child("fan").getValue(Boolean::class.java) ?: false
        val lightsOn = snapshot.child("lights").getValue(Boolean::class.java) ?: false
        val alarmOn = snapshot.child("alarm").getValue(Boolean::class.java) ?: false

        updateButtonUI(binding.btnFan, "Fan", fanOn)
        updateButtonUI(binding.btnLights, "Lights", lightsOn)
        updateButtonUI(binding.btnAlarm, "Alarm", alarmOn)
    }

    private fun updateSensorReadings(snapshot: DataSnapshot) {
        val temp = snapshot.child("temperature").getValue(Double::class.java) ?: 0.0
        val humidity = snapshot.child("humidity").getValue(Double::class.java) ?: 0.0
        val light = snapshot.child("light").getValue(Double::class.java) ?: 0.0

        binding.tvTemperature.text = String.format(Locale.US, "Temperature: %.1f °C", temp)
        binding.tvHumidity.text = String.format(Locale.US, "Humidity: %.1f %%", humidity)
        binding.tvLight.text = String.format(Locale.US, "Light: %.0f lux", light)
    }

    private fun toggleControl(control: String) {
        controlsRef.child(control).get().addOnSuccessListener { snapshot ->
            val currentState = snapshot.getValue(Boolean::class.java) ?: false
            controlsRef.child(control).setValue(!currentState)
        }
    }

    private fun updateButtonUI(button: Button, name: String, isOn: Boolean) {
        if (isOn) {
            button.text = "$name: ON"
            button.setBackgroundColor(Color.GREEN)
        } else {
            button.text = "$name: OFF"
            button.setBackgroundColor(Color.RED)
        }
    }

    private fun startSensorSimulation() {
        sensorUpdateRunnable = object : Runnable {
            override fun run() {
                // Simulate sensor data updates
                val temp = 20.0 + Math.random() * 15.0 // 20-35°C
                val humidity = 40.0 + Math.random() * 40.0 // 40-80%
                val light = 100.0 + Math.random() * 900.0 // 100-1000 lux

                sensorsRef.child("temperature").setValue(temp)
                sensorsRef.child("humidity").setValue(humidity)
                sensorsRef.child("light").setValue(light)

                sensorUpdateHandler.postDelayed(this, 5000)
            }
        }
        sensorUpdateHandler.post(sensorUpdateRunnable)
    }

    override fun onDestroy() {
        super.onDestroy()
        sensorUpdateHandler.removeCallbacks(sensorUpdateRunnable)
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
