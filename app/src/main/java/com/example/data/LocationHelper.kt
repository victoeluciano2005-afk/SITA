package com.example.data

import android.annotation.SuppressLint
import android.content.Context
import android.location.Address
import android.location.Geocoder
import android.os.Build
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.util.Locale
import kotlin.coroutines.resume

data class ResolvedBrazilLocation(
    val state: BrazilianState,
    val city: String
)

object LocationHelper {

    @SuppressLint("MissingPermission")
    suspend fun detectCurrentLocation(context: Context): Result<ResolvedBrazilLocation> {
        return try {
            val fusedClient = LocationServices.getFusedLocationProviderClient(context)
            val cts = CancellationTokenSource()
            val location = fusedClient
                .getCurrentLocation(Priority.PRIORITY_BALANCED_POWER_ACCURACY, cts.token)
                .await()
                ?: fusedClient.lastLocation.await()
                ?: return Result.failure(
                    IllegalStateException("Não foi possível obter a localização do aparelho. Verifique se o GPS está ativado ou selecione o estado e cidade manualmente.")
                )

            val address = reverseGeocode(context, location.latitude, location.longitude)
                ?: return Result.failure(
                    IllegalStateException("Localização obtida, mas não foi possível identificar a cidade. Selecione manualmente.")
                )

            val matchedState = BrazilCatalog.findStateByNameOrUf(address.adminArea)
                ?: return Result.failure(
                    IllegalStateException("Localização detectada fora dos estados brasileiros suportados (${address.adminArea ?: "desconhecido"}). Selecione seu estado no Brasil manualmente.")
                )

            val city = address.locality
                ?: address.subAdminArea
                ?: matchedState.capital

            Result.success(ResolvedBrazilLocation(state = matchedState, city = city))
        } catch (e: Exception) {
            Result.failure(
                IllegalStateException(
                    e.localizedMessage ?: "Erro ao detectar localização."
                )
            )
        }
    }

    @Suppress("DEPRECATION")
    private suspend fun reverseGeocode(
        context: Context,
        latitude: Double,
        longitude: Double
    ): Address? = withContext(Dispatchers.IO) {
        if (!Geocoder.isPresent()) return@withContext null
        val geocoder = Geocoder(context, Locale.forLanguageTag("pt-BR"))
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            suspendCancellableCoroutine { cont ->
                geocoder.getFromLocation(latitude, longitude, 1) { addresses ->
                    if (cont.isActive) cont.resume(addresses.firstOrNull())
                }
            }
        } else {
            runCatching { geocoder.getFromLocation(latitude, longitude, 1)?.firstOrNull() }.getOrNull()
        }
    }
}
