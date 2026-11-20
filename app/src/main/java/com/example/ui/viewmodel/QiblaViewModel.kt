package com.example.ui.viewmodel

import android.Manifest
import android.annotation.SuppressLint
import android.app.Application
import android.content.Context
import android.content.pm.PackageManager
import android.location.Geocoder
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Build
import android.os.Bundle
import android.os.CancellationSignal
import android.os.Looper
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.qibla.CityPreset
import com.example.qibla.CompassSensorReading
import com.example.qibla.QiblaCalculator
import com.example.qibla.QiblaSensorController
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale

enum class QiblaLocationSource {
    GPS,
    MANUAL_CITY,
    UNSET
}

data class QiblaUiState(
    val locationSource: QiblaLocationSource = QiblaLocationSource.UNSET,
    val locationLabel: String = "يرجى تحديد الموقع أو تفعيل GPS",
    val latitude: Double? = null,
    val longitude: Double? = null,
    val qiblaBearingDeg: Float = 0f,
    val distanceKm: Double = 0.0,
    val hasLocationPermission: Boolean = false,
    val isFetchingLocation: Boolean = false,
    val locationError: String? = null,
    val sensorReading: CompassSensorReading = CompassSensorReading()
) {
    val hasLocation: Boolean get() = latitude != null && longitude != null
    val isAligned: Boolean
        get() = hasLocation &&
            sensorReading.hasAccelerometer &&
            sensorReading.hasMagnetometer &&
            QiblaCalculator.isAlignedWithQibla(sensorReading.trueAzimuthDeg, qiblaBearingDeg)
}

class QiblaViewModel(application: Application) : AndroidViewModel(application) {
    private val appContext = application.applicationContext
    private val sensorController = QiblaSensorController(appContext)
    private val locationManager = appContext.getSystemService(Context.LOCATION_SERVICE) as LocationManager

    private val _uiState = MutableStateFlow(
        QiblaUiState(hasLocationPermission = checkLocationPermission())
    )
    val uiState: StateFlow<QiblaUiState> = _uiState.asStateFlow()

    private var activeCancellationSignal: CancellationSignal? = null

    init {
        viewModelScope.launch {
            sensorController.reading.collect { reading ->
                _uiState.update { it.copy(sensorReading = reading) }
            }
        }
        if (checkLocationPermission()) {
            fetchCurrentGpsLocation()
        }
    }

    fun startSensors() {
        sensorController.start()
    }

    fun stopSensors() {
        sensorController.stop()
    }

    fun onPermissionResult(granted: Boolean) {
        _uiState.update { it.copy(hasLocationPermission = granted, locationError = null) }
        if (granted) {
            fetchCurrentGpsLocation()
        }
    }

    fun selectCityPreset(city: CityPreset) {
        applyCoordinates(
            lat = city.latitude,
            lon = city.longitude,
            alt = 0.0,
            label = "${city.nameArabic}، ${city.countryArabic}",
            source = QiblaLocationSource.MANUAL_CITY
        )
    }

    @SuppressLint("MissingPermission")
    fun fetchCurrentGpsLocation() {
        if (!checkLocationPermission()) {
            _uiState.update {
                it.copy(
                    hasLocationPermission = false,
                    isFetchingLocation = false,
                    locationError = "لم يتم منح صلاحية الموقع"
                )
            }
            return
        }

        _uiState.update {
            it.copy(
                hasLocationPermission = true,
                isFetchingLocation = true,
                locationError = null
            )
        }

        // First check best available last known location for instant display
        val lastKnown = listOf(
            LocationManager.GPS_PROVIDER,
            LocationManager.NETWORK_PROVIDER,
            LocationManager.PASSIVE_PROVIDER
        ).mapNotNull { provider ->
            try {
                if (locationManager.isProviderEnabled(provider)) {
                    locationManager.getLastKnownLocation(provider)
                } else null
            } catch (_: Exception) {
                null
            }
        }.maxByOrNull { it.time }

        if (lastKnown != null) {
            applyGpsLocation(lastKnown, stillLoadingFresh = false)
        }

        val activeProvider = when {
            isProviderEnabled(LocationManager.NETWORK_PROVIDER) -> LocationManager.NETWORK_PROVIDER
            isProviderEnabled(LocationManager.GPS_PROVIDER) -> LocationManager.GPS_PROVIDER
            else -> null
        }

        if (activeProvider == null) {
            if (lastKnown == null) {
                _uiState.update {
                    it.copy(
                        isFetchingLocation = false,
                        locationError = "خدمة تحديد الموقع (GPS) متوقفة. فعّلها أو اختر مدينتك يدوياً."
                    )
                }
            } else {
                _uiState.update { it.copy(isFetchingLocation = false) }
            }
            return
        }

        activeCancellationSignal?.cancel()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val signal = CancellationSignal()
            activeCancellationSignal = signal
            try {
                locationManager.getCurrentLocation(
                    activeProvider,
                    signal,
                    appContext.mainExecutor
                ) { location ->
                    if (location != null) {
                        applyGpsLocation(location, stillLoadingFresh = false)
                    } else if (_uiState.value.latitude == null) {
                        _uiState.update {
                            it.copy(
                                isFetchingLocation = false,
                                locationError = "تعذر الحصول على إشارة الموقع الحالية. يمكنك اختيار مدينتك يدوياً."
                            )
                        }
                    } else {
                        _uiState.update { it.copy(isFetchingLocation = false) }
                    }
                }
            } catch (_: Exception) {
                _uiState.update { it.copy(isFetchingLocation = false) }
            }
        } else {
            @Suppress("DEPRECATION")
            try {
                locationManager.requestSingleUpdate(
                    activeProvider,
                    object : LocationListener {
                        override fun onLocationChanged(location: Location) {
                            applyGpsLocation(location, stillLoadingFresh = false)
                        }
                        @Deprecated("Deprecated in Java")
                        override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}
                        override fun onProviderEnabled(provider: String) {}
                        override fun onProviderDisabled(provider: String) {}
                    },
                    Looper.getMainLooper()
                )
            } catch (_: Exception) {
                _uiState.update { it.copy(isFetchingLocation = false) }
            }
        }
    }

    private fun applyGpsLocation(location: Location, stillLoadingFresh: Boolean) {
        applyCoordinates(
            lat = location.latitude,
            lon = location.longitude,
            alt = location.altitude,
            label = "الموقع الحالي (GPS)",
            source = QiblaLocationSource.GPS,
            stillLoading = stillLoadingFresh
        )
        viewModelScope.launch {
            val cityName = resolveCityName(location.latitude, location.longitude)
            if (!cityName.isNullOrBlank()) {
                _uiState.update { current ->
                    if (current.locationSource == QiblaLocationSource.GPS) {
                        current.copy(locationLabel = cityName)
                    } else current
                }
            }
        }
    }

    private fun applyCoordinates(
        lat: Double,
        lon: Double,
        alt: Double,
        label: String,
        source: QiblaLocationSource,
        stillLoading: Boolean = false
    ) {
        sensorController.updateLocationForDeclination(lat, lon, alt)
        val bearing = QiblaCalculator.calculateQiblaBearing(lat, lon)
        val dist = QiblaCalculator.calculateDistanceKm(lat, lon)
        _uiState.update {
            it.copy(
                locationSource = source,
                locationLabel = label,
                latitude = lat,
                longitude = lon,
                qiblaBearingDeg = bearing,
                distanceKm = dist,
                isFetchingLocation = stillLoading,
                locationError = null
            )
        }
    }

    private suspend fun resolveCityName(lat: Double, lon: Double): String? = withContext(Dispatchers.IO) {
        try {
            if (!Geocoder.isPresent()) return@withContext null
            val geocoder = Geocoder(appContext, Locale("ar"))
            @Suppress("DEPRECATION")
            val addresses = geocoder.getFromLocation(lat, lon, 1)
            val addr = addresses?.firstOrNull() ?: return@withContext null
            listOfNotNull(addr.locality ?: addr.subAdminArea ?: addr.adminArea, addr.countryName)
                .distinct()
                .joinToString("، ")
                .ifBlank { null }
        } catch (_: Exception) {
            null
        }
    }

    private fun checkLocationPermission(): Boolean {
        val coarse = ContextCompat.checkSelfPermission(
            appContext,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        val fine = ContextCompat.checkSelfPermission(
            appContext,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        return coarse || fine
    }

    private fun isProviderEnabled(provider: String): Boolean {
        return try {
            locationManager.isProviderEnabled(provider)
        } catch (_: Exception) {
            false
        }
    }

    override fun onCleared() {
        super.onCleared()
        activeCancellationSignal?.cancel()
        sensorController.stop()
    }
}
