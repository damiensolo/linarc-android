package com.solomondesign.myweather.data

import com.google.gson.annotations.SerializedName

data class WeatherResponse(
    @SerializedName("list")
    val forecasts: List<Forecast> = emptyList()
)

data class Forecast(
    @SerializedName("dt")
    val timestamp: Long,
    @SerializedName("main")
    val measurements: Measurements,
    val weather: List<Weather>,
)

data class Measurements(
    @SerializedName("temp")
    val temperature: Float,
    @SerializedName("humidity")
    val humidity: Int,
    @SerializedName("temp_min")
    val minTemperature: Float,
    @SerializedName("temp_max")
    val maxTemperature: Float
)

data class Weather(
    val main: String,
    val description: String,
    val icon: String
) 