package com.example.weatherapp.datamodel

import com.google.gson.annotations.SerializedName

data class WeatherResponse(
    val weather: List<Weather>, // Weather condition list
    val main: Main,
    val wind: Wind,
    val clouds: Clouds,
    val rain: Rain? = null, // Nullable, because some responses don't have rain
    val name: String // City name
)

data class Weather(
    val id: Int,
    val main: String, // Example: "Rain", "Sunny"
    val description: String,
    val icon: String // Example: "10d" (for weather icons)
)

data class Main(
    val temp: Double,
    @SerializedName("feels_like") val feelsLike: Double,
    val pressure: Int,
    val humidity: Int
)

data class Wind(
    val speed: Double,
    val deg: Int
)

data class Clouds(
    val all: Int // Cloud percentage
)

data class Rain(
    @SerializedName("1h") val lastHour: Double
)
