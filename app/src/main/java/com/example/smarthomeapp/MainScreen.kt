package com.example.smarthomeapp

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun MainScreen(
    homeName: String,
    isAutoMode: Boolean,
    onModeChange: (Boolean) -> Unit,
    temperature: Double,
    humidity: Double,
    light: Double,
    fanOn: Boolean,
    lightsOn: Boolean,
    alarmOn: Boolean,
    onFanClick: () -> Unit,
    onLightsClick: () -> Unit,
    onAlarmClick: () -> Unit,
    onSettingsClick: () -> Unit,
    onLogoutClick: () -> Unit
) {
    // <--- CORRECCIÓN DEFINITIVA: Tema claro con texto negro forzado
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = Color(0xFFF0F2F5) // Fondo gris claro para que resalten las tarjetas
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            Header(homeName = homeName, onSettingsClick = onSettingsClick, onLogoutClick = onLogoutClick)
            Spacer(modifier = Modifier.height(24.dp))
            ModeSwitch(isAutoMode = isAutoMode, onModeChange = onModeChange)
            Spacer(modifier = Modifier.height(16.dp))
            SensorDataCard(temperature = temperature, humidity = humidity, light = light)
            Spacer(modifier = Modifier.height(16.dp))
            ControlsCard(
                fanOn = fanOn,
                lightsOn = lightsOn,
                alarmOn = alarmOn,
                onFanClick = onFanClick,
                onLightsClick = onLightsClick,
                onAlarmClick = onAlarmClick
            )
        }
    }
}

@Composable
fun Header(
    homeName: String,
    onSettingsClick: () -> Unit,
    onLogoutClick: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = homeName,
            fontSize = 26.sp,
            fontWeight = FontWeight.Bold,
            color = Color.Black // Forzado a negro
        )
        Spacer(modifier = Modifier.weight(1f))
        IconButton(onClick = onSettingsClick) {
            Icon(painter = painterResource(id = R.drawable.ic_settings), contentDescription = "Ajustes", tint = Color.DarkGray)
        }
        IconButton(onClick = onLogoutClick) {
            Icon(painter = painterResource(id = R.drawable.ic_logout), contentDescription = "Cerrar Sesión", tint = Color.DarkGray)
        }
    }
}

@Composable
fun ModeSwitch(
    isAutoMode: Boolean,
    onModeChange: (Boolean) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = Color.White) // Fondo de tarjeta blanco
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = if (isAutoMode) "Modo Automático" else "Modo Manual",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color.Black // Forzado a negro
            )
            Switch(checked = isAutoMode, onCheckedChange = onModeChange)
        }
    }
}

@Composable
fun SensorDataCard(
    temperature: Double,
    humidity: Double,
    light: Double
) {
    val umbralOscuridad = 500
    val lightText = if (light > umbralOscuridad) {
        "Iluminación baja, luz encendida"
    } else {
        "Iluminación suficiente, luz apagada"
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = Color.White) // Fondo de tarjeta blanco
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            SensorRow(iconRes = R.drawable.ic_thermostat, value = "Temperatura: %.1f °C".format(temperature))
            Spacer(modifier = Modifier.height(16.dp))
            SensorRow(iconRes = R.drawable.ic_water_drop, value = "Humedad: %.1f %%".format(humidity))
            Spacer(modifier = Modifier.height(16.dp))
            SensorRow(iconRes = R.drawable.ic_lightbulb, value = lightText)
        }
    }
}

@Composable
fun SensorRow(iconRes: Int, value: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Image(painter = painterResource(id = iconRes), contentDescription = null, modifier = Modifier.size(24.dp))
        Spacer(modifier = Modifier.width(16.dp))
        Text(text = value, fontSize = 18.sp, color = Color.Black, modifier = Modifier.weight(1f)) // Forzado a negro
    }
}

@Composable
fun ControlsCard(
    fanOn: Boolean,
    lightsOn: Boolean,
    alarmOn: Boolean,
    onFanClick: () -> Unit,
    onLightsClick: () -> Unit,
    onAlarmClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = Color.White) // Fondo de tarjeta blanco
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            ControlButton(text = "Ventilador", isOn = fanOn, onClick = onFanClick)
            Spacer(modifier = Modifier.height(12.dp))
            ControlButton(text = "Luces", isOn = lightsOn, onClick = onLightsClick)
            Spacer(modifier = Modifier.height(12.dp))
            ControlButton(text = "Alarma", isOn = alarmOn, onClick = onAlarmClick)
        }
    }
}

@Composable
fun ControlButton(
    text: String,
    isOn: Boolean,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(60.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = if (isOn) Color(0xFF4CAF50) else Color(0xFFF44336)
        )
    ) {
        Text(text = "$text: ${if (isOn) "ENCENDIDO" else "APAGADO"}", fontSize = 16.sp, color = Color.White)
    }
}

@Preview(showBackground = true)
@Composable
fun DefaultPreview() {
    MainScreen(
        homeName = "Control de Auto Inteligente",
        isAutoMode = true,
        onModeChange = {},
        temperature = 25.5,
        humidity = 60.2,
        light = 300.0,
        fanOn = false,
        lightsOn = true,
        alarmOn = false,
        onFanClick = {},
        onLightsClick = {},
        onAlarmClick = {},
        onSettingsClick = {},
        onLogoutClick = {}
    )
}
