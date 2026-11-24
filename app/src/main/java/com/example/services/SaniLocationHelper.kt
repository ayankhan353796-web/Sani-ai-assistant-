package com.example.services

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.location.Geocoder
import android.net.Uri
import android.os.Build
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.util.Locale

data class SaniLocationResult(
    val latitude: Double,
    val longitude: Double,
    val addressLabel: String
)

class SaniLocationHelper(private val context: Context) {

    @SuppressLint("MissingPermission")
    suspend fun fetchCurrentLocation(): Result<SaniLocationResult> {
        return try {
            val client = LocationServices.getFusedLocationProviderClient(context)
            val tokenSource = CancellationTokenSource()
            val location = client.getCurrentLocation(
                Priority.PRIORITY_BALANCED_POWER_ACCURACY,
                tokenSource.token
            ).await() ?: client.lastLocation.await()

            if (location == null) {
                return Result.failure(IllegalStateException("Location unavailable. Enable GPS and try again."))
            }

            val addressLabel = reverseGeocode(location.latitude, location.longitude)
            Result.success(SaniLocationResult(location.latitude, location.longitude, addressLabel))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private suspend fun reverseGeocode(lat: Double, lon: Double): String = withContext(Dispatchers.IO) {
        try {
            val geocoder = Geocoder(context, Locale.getDefault())
            @Suppress("DEPRECATION")
            val addresses = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                geocoder.getFromLocation(lat, lon, 1)
            } else {
                geocoder.getFromLocation(lat, lon, 1)
            }
            val first = addresses?.firstOrNull()
            if (first != null) {
                listOfNotNull(
                    first.subLocality,
                    first.locality,
                    first.adminArea,
                    first.countryName
                ).distinct().joinToString(", ").ifBlank {
                    String.format(Locale.US, "%.5f, %.5f", lat, lon)
                }
            } else {
                String.format(Locale.US, "%.5f, %.5f", lat, lon)
            }
        } catch (_: Exception) {
            String.format(Locale.US, "%.5f, %.5f", lat, lon)
        }
    }

    fun openInMaps(lat: Double, lon: Double, label: String) {
        val uri = Uri.parse("geo:$lat,$lon?q=$lat,$lon(${Uri.encode(label)})")
        val intent = Intent(Intent.ACTION_VIEW, uri).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }
}
