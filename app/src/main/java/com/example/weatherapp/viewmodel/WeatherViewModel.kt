package com.example.weatherapp.viewmodel

import android.util.Log
import androidx.lifecycle.*
import com.example.weatherapp.WeatherRepository
import com.example.weatherapp.datamodel.WeatherResponse
import kotlinx.coroutines.launch

class WeatherViewModel(private val repository: WeatherRepository) : ViewModel() {

    private val _weatherData = MutableLiveData<WeatherResponse>()
    val weatherData: LiveData<WeatherResponse> get() = _weatherData

    private val _weatherCondition = MutableLiveData<String>()
    val weatherCondition: LiveData<String> get() = _weatherCondition

    fun fetchWeather(city: String, apiKey: String) {
        viewModelScope.launch {
            try {
                val response = repository.getWeather(city, apiKey)
                _weatherData.postValue(response)

                val condition = response.weather.firstOrNull()?.main ?: "Unknown"
                _weatherCondition.postValue(condition)

                Log.d("WeatherViewModel", "Weather Condition: $condition")
            } catch (e: Exception) {
                Log.e("WeatherViewModel", "Error fetching weather: ${e.message}")
                _weatherCondition.postValue("Error")
            }
        }
    }
}
