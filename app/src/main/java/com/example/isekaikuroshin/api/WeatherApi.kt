package com.example.isekaikuroshin.api

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query

/**
 * GÖREV D3: OpenWeatherMap API Integration
 * Free tier API: api.openweathermap.org/data/2.5/weather
 */

data class WeatherResponse(
    val weather: List<WeatherCondition>,
    val wind: Wind,
    val rain: Rain?
)

data class WeatherCondition(
    val main: String,
    val description: String
)

data class Wind(
    val speed: Double // m/s
)

data class Rain(
    val `1h`: Double? // mm in last 1 hour
)

interface WeatherApiService {
    @GET("data/2.5/weather")
    suspend fun getCurrentWeather(
        @Query("lat") latitude: Double,
        @Query("lon") longitude: Double,
        @Query("appid") apiKey: String,
        @Query("units") units: String = "metric"
    ): WeatherResponse
}

object WeatherApi {
    private const val BASE_URL = "https://api.openweathermap.org/"

    // TODO-D3: Replace with your OpenWeatherMap API key
    // Get free API key from: https://openweathermap.org/api
    const val API_KEY = "YOUR_API_KEY_HERE"

    private val retrofit = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    val service: WeatherApiService = retrofit.create(WeatherApiService::class.java)
}
