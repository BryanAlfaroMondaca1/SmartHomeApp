package com.example.smarthomeapp

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.smarthomeapp.data.Settings

@Composable
fun SettingsScreen(
    settings: Settings,
    onSettingsChange: (Settings) -> Unit,
    onSaveClick: () -> Unit,
    onBackClick: () -> Unit
) {
    // <--- CORRECCIÓN DEFINITIVA: Tema claro con texto negro forzado
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = Color.White
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Text(
                text = "CONFIGURACIÓN",
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.padding(bottom = 24.dp),
                color = Color.Black
            )

            OutlinedTextField(
                value = settings.homeName,
                onValueChange = { onSettingsChange(settings.copy(homeName = it)) },
                label = { Text("Nombre de la Casa") },
                modifier = Modifier.fillMaxWidth(),
                colors = getTextFieldColors()
            )

            Spacer(modifier = Modifier.height(24.dp))

            Text("Temperatura (°C)", style = MaterialTheme.typography.titleLarge, color = Color.Black)
            Row(Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = settings.tempMin.toString(),
                    onValueChange = { onSettingsChange(settings.copy(tempMin = it.toDoubleOrNull() ?: 0.0)) },
                    label = { Text("Mín") },
                    modifier = Modifier.weight(1f),
                    colors = getTextFieldColors()
                )
                Spacer(modifier = Modifier.width(8.dp))
                OutlinedTextField(
                    value = settings.tempMax.toString(),
                    onValueChange = { onSettingsChange(settings.copy(tempMax = it.toDoubleOrNull() ?: 0.0)) },
                    label = { Text("Máx") },
                    modifier = Modifier.weight(1f),
                    colors = getTextFieldColors()
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text("Humedad (%)", style = MaterialTheme.typography.titleLarge, color = Color.Black)
            Row(Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = settings.humidityMin.toString(),
                    onValueChange = { onSettingsChange(settings.copy(humidityMin = it.toDoubleOrNull() ?: 0.0)) },
                    label = { Text("Mín") },
                    modifier = Modifier.weight(1f),
                    colors = getTextFieldColors()
                )
                Spacer(modifier = Modifier.width(8.dp))
                OutlinedTextField(
                    value = settings.humidityMax.toString(),
                    onValueChange = { onSettingsChange(settings.copy(humidityMax = it.toDoubleOrNull() ?: 0.0)) },
                    label = { Text("Máx") },
                    modifier = Modifier.weight(1f),
                    colors = getTextFieldColors()
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text("Luz (lux)", style = MaterialTheme.typography.titleLarge, color = Color.Black)
            Row(Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = settings.lightMin.toString(),
                    onValueChange = { onSettingsChange(settings.copy(lightMin = it.toLongOrNull() ?: 0L)) },
                    label = { Text("Mín") },
                    modifier = Modifier.weight(1f),
                    colors = getTextFieldColors()
                )
                Spacer(modifier = Modifier.width(8.dp))
                OutlinedTextField(
                    value = settings.lightMax.toString(),
                    onValueChange = { onSettingsChange(settings.copy(lightMax = it.toLongOrNull() ?: 0L)) },
                    label = { Text("Máx") },
                    modifier = Modifier.weight(1f),
                    colors = getTextFieldColors()
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            Button(
                onClick = onSaveClick,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Guardar Ajustes", color = Color.White)
            }

            TextButton(
                onClick = onBackClick,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Volver")
            }
        }
    }
}

// Función de ayuda para no repetir los colores del TextField
@Composable
private fun getTextFieldColors(): TextFieldColors {
    return TextFieldDefaults.outlinedTextFieldColors(
        textColor = Color.Black,
        cursorColor = Color.Black,
        focusedBorderColor = MaterialTheme.colorScheme.primary,
        unfocusedBorderColor = Color.Gray
    )
}


@Preview(showBackground = true)
@Composable
fun SettingsScreenPreview() {
    SettingsScreen(
        settings = Settings(),
        onSettingsChange = {},
        onSaveClick = {},
        onBackClick = {}
    )
}
