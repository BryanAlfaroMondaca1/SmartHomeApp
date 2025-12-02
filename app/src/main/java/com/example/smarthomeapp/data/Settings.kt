package com.example.smarthomeapp.data

data class Settings(
    val homeName: String = "",
    val tempMin: Double = 0.0,
    val tempMax: Double = 0.0,
    val humidityMin: Double = 0.0,
    val humidityMax: Double = 0.0,
    val lightMin: Long = 0L,
    val lightMax: Long = 0L
)
