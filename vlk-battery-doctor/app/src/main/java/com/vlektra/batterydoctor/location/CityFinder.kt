package com.vlektra.batterydoctor.location

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Geocoder
import android.location.Location
import android.location.LocationManager
import android.os.Build
import androidx.core.content.ContextCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Locale

/**
 * Works out which city the technician is in, so the report fills itself.
 *
 * Deliberately undemanding: it reads the last known fix rather than waking the
 * GPS, and falls back quietly whenever anything is missing. A service centre
 * basement with no signal should not stop a report being written, so every
 * failure path returns null and the technician types the city instead.
 */
object CityFinder {

    sealed interface Result {
        data class Found(val city: String) : Result
        data object NoPermission : Result
        data object NoFix : Result
        data object Unavailable : Result
    }

    fun hasPermission(context: Context): Boolean =
        ContextCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(
                context, Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED

    suspend fun detect(context: Context): Result = withContext(Dispatchers.IO) {
        if (!hasPermission(context)) return@withContext Result.NoPermission

        val location = lastKnownLocation(context) ?: return@withContext Result.NoFix

        if (!Geocoder.isPresent()) return@withContext Result.Unavailable

        val name = try {
            @Suppress("DEPRECATION")
            Geocoder(context, Locale.UK)
                .getFromLocation(location.latitude, location.longitude, 1)
                ?.firstOrNull()
                ?.let { it.locality ?: it.subAdminArea ?: it.adminArea }
        } catch (e: Exception) {
            null
        }

        if (name.isNullOrBlank()) Result.Unavailable else Result.Found(name)
    }

    private fun lastKnownLocation(context: Context): Location? {
        val manager = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
            ?: return null

        val providers = buildList {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) add(LocationManager.FUSED_PROVIDER)
            add(LocationManager.NETWORK_PROVIDER)
            add(LocationManager.GPS_PROVIDER)
        }

        var best: Location? = null
        for (p in providers) {
            val fix = try {
                @Suppress("MissingPermission")
                manager.getLastKnownLocation(p)
            } catch (e: SecurityException) {
                null
            } catch (e: IllegalArgumentException) {
                null
            }
            if (fix != null && (best == null || fix.time > best!!.time)) best = fix
        }
        return best
    }
}
