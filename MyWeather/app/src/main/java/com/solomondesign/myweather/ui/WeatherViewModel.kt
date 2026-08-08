package com.solomondesign.myweather.ui

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.solomondesign.myweather.data.GeocodingApi
import com.solomondesign.myweather.data.WeatherApi
import com.solomondesign.myweather.data.WeatherResponse
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class WeatherUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val weatherData: WeatherResponse? = null,
    val searchQuery: String = "",
    val currentLocation: String? = null
)

class WeatherViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(WeatherUiState())
    val uiState = _uiState.asStateFlow()

    init {
        searchLocation("New York")
    }

    fun onSearchQueryChange(query: String) {
        _uiState.value = _uiState.value.copy(
            searchQuery = query,
            error = null
        )
    }

    fun searchLocation(location: String) {
        if (location.isBlank()) {
            _uiState.value = _uiState.value.copy(
                error = "Please enter a city name"
            )
            return
        }

        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(
                    isLoading = true,
                    error = null,
                    weatherData = null,
                    currentLocation = location
                )

                // Get coordinates from geocoding API
                val geocodingResponse = GeocodingApi.service.getCoordinates(
                    cityName = location,
                    apiKey = WeatherApi.getApiKey()
                )

                if (geocodingResponse.isEmpty()) {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = "City not found"
                    )
                    return@launch
                }

                val cityData = geocodingResponse.first()
                Log.d("WeatherViewModel", "Found coordinates for $location: ${cityData.lat}, ${cityData.lon}")

                // Get weather data using coordinates
                val weatherResponse = WeatherApi.service.getWeatherForecast(
                    lat = cityData.lat,
                    lon = cityData.lon,
                    apiKey = WeatherApi.getApiKey()
                )

                // Update location name to include state/country if available
                val formattedLocation = buildString {
                    append(cityData.name)
                    if (!cityData.state.isNullOrBlank()) {
                        append(", ${cityData.state}")
                    }
                    append(", ${cityData.country}")
                }

                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    weatherData = weatherResponse,
                    currentLocation = formattedLocation,
                    searchQuery = ""
                )
            } catch (e: Exception) {
                Log.e("WeatherViewModel", "Error fetching weather", e)
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = when (e) {
                        is retrofit2.HttpException -> "Error: Unable to find city"
                        is java.net.UnknownHostException -> "Error: No internet connection"
                        else -> "Error: ${e.localizedMessage ?: "Unknown error"}"
                    }
                )
            }
        }
    }
} 