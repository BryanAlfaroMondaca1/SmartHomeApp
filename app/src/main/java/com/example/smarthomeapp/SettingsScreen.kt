package com.example.smarthomeapp

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
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
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Text(
            text = "CONFIGURACIÓN",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(bottom = 24.dp)
        )

        OutlinedTextField(
            value = settings.homeName,
            onValueChange = { onSettingsChange(settings.copy(homeName = it)) },
            label = { Text("Nombre de la Casa") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(24.dp))

        Text("Temperatura (°C)", style = MaterialTheme.typography.titleLarge)
        Row(Modifier.fillMaxWidth()) {
            OutlinedTextField(
                value = settings.tempMin.toString(),
                onValueChange = { onSettingsChange(settings.copy(tempMin = it.toDoubleOrNull() ?: 0.0)) },
                label = { Text("Mín") },
                modifier = Modifier.weight(1f)
            )
            Spacer(modifier = Modifier.width(8.dp))
            OutlinedTextField(
                value = settings.tempMax.toString(),
                onValueChange = { onSettingsChange(settings.copy(tempMax = it.toDoubleOrNull() ?: 0.0)) },
                label = { Text("Máx") },
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text("Humedad (%)", style = MaterialTheme.typography.titleLarge)
        Row(Modifier.fillMaxWidth()) {
            OutlinedTextField(
                value = settings.humidityMin.toString(),
                onValueChange = { onSettingsChange(settings.copy(humidityMin = it.toDoubleOrNull() ?: 0.0)) },
                label = { Text("Mín") },
                modifier = Modifier.weight(1f)
            )
            Spacer(modifier = Modifier.width(8.dp))
            OutlinedTextField(
                value = settings.humidityMax.toString(),
                onValueChange = { onSettingsChange(settings.copy(humidityMax = it.toDoubleOrNull() ?: 0.0)) },
                label = { Text("Máx") },
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text("Luz (lux)", style = MaterialTheme.typography.titleLarge)
        Row(Modifier.fillMaxWidth()) {
            OutlinedTextField(
                value = settings.lightMin.toString(),
                onValueChange = { onSettingsChange(settings.copy(lightMin = it.toLongOrNull() ?: 0L)) },
                label = { Text("Mín") },
                modifier = Modifier.weight(1f)
            )
            Spacer(modifier = Modifier.width(8.dp))
            OutlinedTextField(
                value = settings.lightMax.toString(),
                onValueChange = { onSettingsChange(settings.copy(lightMax = it.toLongOrNull() ?: 0L)) },
                label = { Text("Máx") },
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = onSaveClick,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Guardar Ajustes")
        }

        TextButton(
            onClick = onBackClick,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Volver")
        }
    }
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
