package com.solomondesign.myweather.data

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query

interface GeocodingService {
    @GET("geo/1.0/direct")
    suspend fun getCoordinates(
        @Query("q") cityName: String,
        @Query("limit") limit: Int = 1,
        @Query("appid") apiKey: String
    ): List<GeocodingResponse>
}

data class GeocodingResponse(
    val name: String,
    val lat: Double,
    val lon: Double,
    val country: String,
    val state: String?
)

object GeocodingApi {
    private val retrofit = Retrofit.Builder()
        .baseUrl("https://api.openweathermap.org/")
        .client(WeatherApi.client)
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    val service: GeocodingService = retrofit.create(GeocodingService::class.java)
} 