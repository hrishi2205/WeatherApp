package com.example.weatherapp

import com.example.weatherapp.api.WeatherApiService
import com.example.weatherapp.datamodel.WeatherResponse

class WeatherRepository(private val apiService: WeatherApiService) {

    suspend fun getWeather(city: String, apiKey: String): WeatherResponse {
        return apiService.getWeather(city, apiKey, "metric")
    }
}
