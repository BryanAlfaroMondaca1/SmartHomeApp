package com.example.smarthomeapp

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.example.smarthomeapp.data.Settings
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

    // --- Variables de la UI ---
    private var homeName by mutableStateOf("Control de Casa Inteligente")
    private var isAutoMode by mutableStateOf(false)
    private var temperature by mutableStateOf(0.0)
    private var humidity by mutableStateOf(0.0)
    private var light by mutableStateOf(0.0)
    private var fanOn by mutableStateOf(false)
    private var lightsOn by mutableStateOf(false)
    private var alarmOn by mutableStateOf(false)

    // <--- MEJORA: Variable para guardar los rangos de notificación
    private var notificationSettings by mutableStateOf(Settings())

    // <--- MEJORA: Launcher para pedir permiso de notificaciones
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (!isGranted) {
            Toast.makeText(this, "El permiso para notificaciones es necesario para recibir alertas.", Toast.LENGTH_LONG).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // <--- MEJORA: Pedir permiso y crear canal de notificación
        askNotificationPermission()
        createNotificationChannel()

        FirebaseDatabase.getInstance().setPersistenceEnabled(true)
        auth = FirebaseAuth.getInstance()
        db = FirebaseDatabase.getInstance()

        val userId = auth.currentUser?.uid
        if (userId == null) {
            goToLoginActivity()
            return
        }

        userSettingsRef = db.getReference("users/$userId/settings")
        sensorsRef = db.getReference("sensores")
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
        // Listener para el nombre de la casa
        userSettingsRef.child("home_name").addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val name = snapshot.getValue(String::class.java)
                homeName = if (!name.isNullOrEmpty()) name else "Control de Casa Inteligente"
            }
            override fun onCancelled(error: DatabaseError) { /* Handle error */ }
        })

        // Listener para los controles (ventilador, luces, etc.)
        controlsRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                fanOn = snapshot.child("fan").getValue(Boolean::class.java) ?: false
                lightsOn = snapshot.child("lights").getValue(Boolean::class.java) ?: false
                alarmOn = snapshot.child("alarm").getValue(Boolean::class.java) ?: false
            }
            override fun onCancelled(error: DatabaseError) {
                showToast("Error al leer los controles: ${error.message}")
            }
        })

        // Listener para los sensores (temperatura, humedad, etc.)
        sensorsRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                temperature = snapshot.child("temperatura").getValue(Double::class.java) ?: 0.0
                humidity = snapshot.child("humedad").getValue(Double::class.java) ?: 0.0
                light = snapshot.child("luz").getValue(Long::class.java)?.toDouble() ?: 0.0

                // <--- MEJORA: Comprobar si hay que enviar una notificación
                checkSensorValuesForNotifications(temperature, humidity, light)
            }
            override fun onCancelled(error: DatabaseError) {
                showToast("Error al leer los sensores: ${error.message}")
            }
        })

        // Listener para el modo de operación (auto/manual)
        deviceSettingsRef.child("mode").addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                isAutoMode = snapshot.getValue(String::class.java) == "auto"
            }
            override fun onCancelled(error: DatabaseError) { /* Handle error */ }
        })

        // <--- MEJORA: Listener para los rangos de notificación
        deviceSettingsRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val settings = snapshot.getValue(Settings::class.java)
                if (settings != null) {
                    notificationSettings = settings
                }
            }
            override fun onCancelled(error: DatabaseError) {
                showToast("Error al cargar la configuración de notificaciones.")
            }
        })
    }

    private fun toggleControl(control: String) {
        controlsRef.child(control).get().addOnSuccessListener { snapshot ->
            val currentState = snapshot.getValue(Boolean::class.java) ?: false
            controlsRef.child(control).setValue(!currentState)
        }
    }

    // <--- MEJORA: Nuevas funciones para manejar notificaciones ---
    private fun checkSensorValuesForNotifications(temp: Double, hum: Double, lgt: Double) {
        if (temp > notificationSettings.tempMax) {
            sendNotification(1, "Alerta de Temperatura", "La temperatura (%.1f°C) ha superado el máximo permitido (%.1f°C).".format(temp, notificationSettings.tempMax))
        } else if (temp < notificationSettings.tempMin) {
            sendNotification(1, "Alerta de Temperatura", "La temperatura (%.1f°C) está por debajo del mínimo permitido (%.1f°C).".format(temp, notificationSettings.tempMin))
        }

        if (hum > notificationSettings.humidityMax) {
            sendNotification(2, "Alerta de Humedad", "La humedad (%.1f%%) ha superado el máximo permitido (%.1f%%).".format(hum, notificationSettings.humidityMax))
        } else if (hum < notificationSettings.humidityMin) {
            sendNotification(2, "Alerta de Humedad", "La humedad (%.1f%%) está por debajo del mínimo permitido (%.1f%%).".format(hum, notificationSettings.humidityMin))
        }

        // La luz se puede manejar de forma similar si se desea
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Alertas de Sensores"
            val descriptionText = "Canal para notificaciones de alertas de temperatura y humedad."
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val channel = NotificationChannel("sensor_alerts", name, importance).apply {
                description = descriptionText
            }
            val notificationManager: NotificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun sendNotification(notificationId: Int, title: String, content: String) {
        val builder = NotificationCompat.Builder(this, "sensor_alerts")
            .setSmallIcon(R.drawable.ic_thermostat) // Reemplazar con un ícono adecuado
            .setContentTitle(title)
            .setContentText(content)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)

        val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) {
             notificationManager.notify(notificationId, builder.build())
        }
    }

    private fun askNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }
    // --- Fin de funciones de notificación ---

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    private fun goToLoginActivity() {
        val intent = Intent(this, LoginActivity::class.java)
        startActivity(intent)
        finish()
    }
}
