package com.example.weatherapp.datamodel

data class City(
    val id: Long,
    val name: String,
    val state: String?,
    val country: String,
    val coord: Coord
)

data class Coord(
    val lon: Double,
    val lat: Double
)

