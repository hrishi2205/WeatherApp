package com.example.weatherapp.fragment

import android.Manifest
import android.content.pm.PackageManager
import android.location.Geocoder
import android.location.Location
import android.os.Bundle
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.example.weatherapp.CityItem
import com.example.weatherapp.R
import com.example.weatherapp.WeatherRepository
import com.example.weatherapp.api.WeatherApiService
import com.example.weatherapp.databinding.FragmentWeatherBinding
import com.example.weatherapp.datamodel.City
import com.example.weatherapp.datamodel.WeatherResponse
import com.example.weatherapp.viewmodel.WeatherViewModel
import com.example.weatherapp.viewmodel.WeatherViewModelFactory
import com.google.android.gms.location.LocationServices
import com.google.gson.Gson
import org.json.JSONArray
import java.io.IOException
import java.util.Locale

class WeatherFragment : Fragment() {

    private var _binding: FragmentWeatherBinding? = null
    private val binding get() = _binding!!
    private lateinit var viewModel: WeatherViewModel

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentWeatherBinding.inflate(inflater, container, false)

        val apiService = WeatherApiService.instance
        val repository = WeatherRepository(apiService)
        val factory = WeatherViewModelFactory(repository)
        viewModel = ViewModelProvider(this, factory)[WeatherViewModel::class.java]

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel.weatherData.observe(viewLifecycleOwner) { weather ->
            updateUI(weather)
        }
        val cities = loadCityList()
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, cityDisplayList)
        binding.etCity.setAdapter(adapter)

        binding.etCity.setOnEditorActionListener { _, actionId, event ->
            if (actionId == EditorInfo.IME_ACTION_DONE ||
                (event?.keyCode == KeyEvent.KEYCODE_ENTER && event.action == KeyEvent.ACTION_DOWN)) {

                val input = binding.etCity.text.toString().trim()
                val matchedCity = cityDisplayList.find { it.equals(input, ignoreCase = true) }

                if (matchedCity != null) {
                    val cityName = matchedCity.substringBefore(",")
                    binding.textView2.text = cityName.replaceFirstChar { it.uppercase() }
                    viewModel.fetchWeather(cityName, "YOUR_API_KEY")
                } else {
                    Toast.makeText(requireContext(), "Please select a valid city from suggestions", Toast.LENGTH_SHORT).show()
                }

                true
            } else {
                false
            }
        }

        binding.etCity.setOnEditorActionListener { _, actionId, event ->
            if (actionId == EditorInfo.IME_ACTION_DONE ||
                (event?.keyCode == KeyEvent.KEYCODE_ENTER && event.action == KeyEvent.ACTION_DOWN)) {
                val city = binding.etCity.text.toString().trim()
                binding.textView2.text = city.replaceFirstChar { it.uppercase() }
                if (city.isNotEmpty()) {
                    viewModel.fetchWeather(city, "3eb23da9b2857529504306091487cb06")
                } else {
                    Toast.makeText(requireContext(), "Please enter a city name", Toast.LENGTH_SHORT).show()
                }
                true
            } else {
                false
            }
        }

        requestLocationPermission()
    }

    private fun updateUI(weather: WeatherResponse) {
        binding.tvTemperature.text = "${weather.main.temp.toInt()}°C"
        binding.tvWeatherDesc.text = weather.weather[0].description.replaceFirstChar { it.uppercase() }
        binding.tvFeelsLike.text = "Feels like: ${weather.main.feelsLike.toInt()}°C"
        binding.tvHumidity.text = "Humidity: ${weather.main.humidity}%"
        binding.tvPressure.text = "Pressure: ${weather.main.pressure} hPa"
        binding.tvWindSpeed.text = "Wind : ${weather.wind.speed} m/s"

        when (weather.weather[0].main) {
            "Clear" -> {
                binding.root.setBackgroundResource(R.drawable.gradiant_sunny)
                binding.imageView.setImageResource(R.drawable.sunny)
            }
            "Rain" -> {
                binding.imageView.setImageResource(R.drawable.rainyday)
                binding.root.setBackgroundResource(R.drawable.gradient_rainy)
            }
            "Clouds" -> {
                binding.imageView.setImageResource(R.drawable.clouds)
                binding.root.setBackgroundResource(R.drawable.gradient_cloudy)
            }
            "Snow" -> {
                binding.root.setBackgroundResource(R.drawable.gradient_snowy)
                binding.imageView.setImageResource(R.drawable.snow)
            }
            else -> binding.root.setBackgroundResource(R.drawable.gradient_default)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun requestLocationPermission() {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(
                requireActivity(),
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                LOCATION_PERMISSION_REQUEST_CODE
            )
        } else {
            getCurrentLocation()
        }
    }

    private fun getCurrentLocation() {
        val fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireContext())

        if (ActivityCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }

        fusedLocationClient.lastLocation.addOnSuccessListener { location: Location? ->
            location?.let {
                val latitude = it.latitude
                val longitude = it.longitude
                fetchCityNameFromCoordinates(latitude, longitude)
            }
        }
    }

    private fun fetchCityNameFromCoordinates(lat: Double, lon: Double) {
        val geocoder = Geocoder(requireContext(), Locale.getDefault())
        try {
            val addresses = geocoder.getFromLocation(lat, lon, 1)
            if (!addresses.isNullOrEmpty()) {
                val city = addresses[0].locality ?: ""
                binding.etCity.setText(city)
                viewModel.fetchWeather(city, "3eb23da9b2857529504306091487cb06")
            }
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    companion object {
        private const val LOCATION_PERMISSION_REQUEST_CODE = 1001
    }

    private lateinit var cityList: List<CityItem>
    private lateinit var cityDisplayList: List<String>

    private fun loadCityList() {
        val inputStream = requireContext().assets.open("city.list.json")
        val json = inputStream.bufferedReader().use { it.readText() }
        val jsonArray = JSONArray(json)
        val tempList = mutableListOf<CityItem>()

        for (i in 0 until jsonArray.length()) {
            val cityObj = jsonArray.getJSONObject(i)
            val name = cityObj.getString("name")
            val country = cityObj.getString("country")
            tempList.add(CityItem(name, country))
        }

        // Remove duplicates, sort alphabetically
        cityList = tempList.distinctBy { "${it.name},${it.country}" }.sortedBy { it.name }

        // Create a display list for the dropdown (e.g., "London, GB")
        cityDisplayList = cityList.map { "${it.name}, ${it.country}" }
    }
}
