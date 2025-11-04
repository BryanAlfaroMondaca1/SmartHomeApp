package com.example.smarthomeapp

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.smarthomeapp.databinding.ActivitySettingsBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class SettingsActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySettingsBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var userSettingsRef: DatabaseReference

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()
        val userId = auth.currentUser?.uid
        if (userId == null) {
            finish() // Should not happen if called from MainActivity
            return
        }

        userSettingsRef = FirebaseDatabase.getInstance().getReference("users/$userId/settings")

        loadSettings()

        binding.btnSaveSettings.setOnClickListener {
            saveSettings()
        }

        binding.btnBack.setOnClickListener {
            finish()
        }
    }

    private fun loadSettings() {
        userSettingsRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                binding.etHomeName.setText(snapshot.child("home_name").getValue(String::class.java))
                binding.etTempMin.setText(snapshot.child("temp_min").getValue(Double::class.java)?.toString())
                binding.etTempMax.setText(snapshot.child("temp_max").getValue(Double::class.java)?.toString())
                binding.etHumidityMin.setText(snapshot.child("humidity_min").getValue(Double::class.java)?.toString())
                binding.etHumidityMax.setText(snapshot.child("humidity_max").getValue(Double::class.java)?.toString())
                binding.etLightMin.setText(snapshot.child("light_min").getValue(Long::class.java)?.toString())
                binding.etLightMax.setText(snapshot.child("light_max").getValue(Long::class.java)?.toString())
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@SettingsActivity, "Failed to load settings: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun saveSettings() {
        try {
            val homeName = binding.etHomeName.text.toString()
            val tempMin = binding.etTempMin.text.toString().toDouble()
            val tempMax = binding.etTempMax.text.toString().toDouble()
            val humidityMin = binding.etHumidityMin.text.toString().toDouble()
            val humidityMax = binding.etHumidityMax.text.toString().toDouble()
            val lightMin = binding.etLightMin.text.toString().toLong()
            val lightMax = binding.etLightMax.text.toString().toLong()

            if (tempMin >= tempMax || humidityMin >= humidityMax || lightMin >= lightMax) {
                Toast.makeText(this, "Invalid range: min value must be less than max value", Toast.LENGTH_LONG).show()
                return
            }

            val settings = mapOf(
                "home_name" to homeName,
                "temp_min" to tempMin,
                "temp_max" to tempMax,
                "humidity_min" to humidityMin,
                "humidity_max" to humidityMax,
                "light_min" to lightMin,
                "light_max" to lightMax
            )

            userSettingsRef.updateChildren(settings)
                .addOnSuccessListener {
                    Toast.makeText(this, "Settings saved!", Toast.LENGTH_SHORT).show()
                    finish()
                }
                .addOnFailureListener {
                    Toast.makeText(this, "Failed to save settings: ${it.message}", Toast.LENGTH_SHORT).show()
                }

        } catch (e: NumberFormatException) {
            Toast.makeText(this, "Please fill all fields with valid numbers", Toast.LENGTH_SHORT).show()
        }
    }
}
