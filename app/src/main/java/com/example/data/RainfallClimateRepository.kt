package com.example.data

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.util.Calendar
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.max
import kotlin.math.roundToInt

data class WeeklyRainIrrigationPoint(
    val label: String,
    val periodLabel: String,
    val rainMm: Float,
    val rainyDays: Int,
    val idealRegasSemana: Float,
    val baseRegasSemana: Float
)

data class MonthlyRainIrrigationPoint(
    val monthNumber: Int, // 1..12
    val shortLabel: String,
    val fullLabel: String,
    val rainMm: Float,
    val rainyDays: Int,
    val idealRegasSemana: Float,
    val isSelectedMonth: Boolean
)

data class IrrigationRainfallComparison(
    val cidade: String,
    val estadoUf: String,
    val mesPlantio: String,
    val anoReferencia: Int,
    val localCultivo: String,
    val baseRegasPorSemana: Float,
    val totalChuvaMesMm: Float,
    val diasComChuvaMes: Int,
    val weeklyPoints: List<WeeklyRainIrrigationPoint>,
    val monthlyPoints: List<MonthlyRainIrrigationPoint>,
    val fonteDados: String
)

private data class DailyRainRecord(
    val year: Int,
    val month: Int, // 1..12
    val day: Int,
    val rainMm: Float
)

private data class CachedClimateArchive(
    val resolvedLocationLabel: String,
    val records: List<DailyRainRecord>
)

class RainfallClimateRepository(
    private val cacheDir: File
) {
    private val archiveMemoryCache = ConcurrentHashMap<String, CachedClimateArchive>()

    suspend fun getIrrigationVsRainfall(guide: PlantingGuide): Result<IrrigationRainfallComparison> =
        withContext(Dispatchers.IO) {
            runCatching {
                val state = BrazilCatalog.findStateByNameOrUf(guide.estado)
                val uf = state?.uf ?: guide.estado.trim().take(2).uppercase(Locale.ROOT)
                val stateName = state?.nome ?: guide.estado.trim()
                val city = guide.cidade.trim().ifBlank { state?.capital ?: "São Paulo" }

                val archive = loadOrFetchArchive(city = city, uf = uf, stateName = stateName)
                val selectedMonthIndex = resolveMonthNumber(guide.mesPlantio)
                val baseRegasSemana = computeBaseRegasPerWeek(guide.irrigacao)
                val localCultivo = LocalCultivo.fromLabel(guide.localCultivo)

                buildComparison(
                    cidade = city,
                    estadoUf = uf,
                    mesPlantio = BrazilCatalog.meses[(selectedMonthIndex - 1).coerceIn(0, 11)],
                    selectedMonthNumber = selectedMonthIndex,
                    localCultivo = localCultivo,
                    baseRegasSemana = baseRegasSemana,
                    archive = archive
                )
            }
        }

    private fun loadOrFetchArchive(
        city: String,
        uf: String,
        stateName: String
    ): CachedClimateArchive {
        val cacheKey = "${BrazilCatalog.normalizeForSearch(city)}_${uf.lowercase(Locale.ROOT)}"
        archiveMemoryCache[cacheKey]?.let { return it }

        readDiskCache(cacheKey)?.let {
            archiveMemoryCache[cacheKey] = it
            return it
        }

        val (lat, lon, resolvedLabel) = resolveCoordinates(city, uf, stateName)
        val (startDate, endDate) = computeLastTwelveCompletedMonthsRange()

        val archiveUrl = buildString {
            append("https://archive-api.open-meteo.com/v1/archive?")
            append("latitude=").append(String.format(Locale.US, "%.4f", lat))
            append("&longitude=").append(String.format(Locale.US, "%.4f", lon))
            append("&start_date=").append(startDate)
            append("&end_date=").append(endDate)
            append("&daily=precipitation_sum")
            append("&timezone=America%2FSao_Paulo")
        }

        val jsonText = httpGet(archiveUrl)
        val parsedRecords = parseDailyPrecipitation(jsonText)
        if (parsedRecords.isEmpty()) {
            throw IllegalStateException("Histórico de chuvas indisponível para $city - $uf.")
        }

        val result = CachedClimateArchive(
            resolvedLocationLabel = resolvedLabel,
            records = parsedRecords
        )
        archiveMemoryCache[cacheKey] = result
        writeDiskCache(cacheKey, resolvedLabel, jsonText)
        return result
    }

    private fun resolveCoordinates(
        city: String,
        uf: String,
        stateName: String
    ): Triple<Double, Double, String> {
        val geocoded = runCatching {
            val encoded = URLEncoder.encode(city, "UTF-8")
            val url = "https://geocoding-api.open-meteo.com/v1/search?name=$encoded&count=10&language=pt&format=json"
            val body = httpGet(url)
            val root = JSONObject(body)
            val results = root.optJSONArray("results") ?: return@runCatching null
            val targetStateNorm = BrazilCatalog.normalizeForSearch(stateName)

            var bestBrMatch: JSONObject? = null
            for (i in 0 until results.length()) {
                val item = results.optJSONObject(i) ?: continue
                if (!item.optString("country_code").equals("BR", ignoreCase = true)) continue
                val admin1Norm = BrazilCatalog.normalizeForSearch(item.optString("admin1", ""))
                if (admin1Norm.isNotEmpty() &&
                    (admin1Norm == targetStateNorm || admin1Norm.contains(targetStateNorm) || targetStateNorm.contains(admin1Norm))
                ) {
                    return@runCatching Triple(
                        item.getDouble("latitude"),
                        item.getDouble("longitude"),
                        "Estação ERA5 • $city ($uf)"
                    )
                }
                if (bestBrMatch == null) bestBrMatch = item
            }
            bestBrMatch?.let { item ->
                Triple(
                    item.getDouble("latitude"),
                    item.getDouble("longitude"),
                    "Estação ERA5 • ${item.optString("name", city)} ($uf)"
                )
            }
        }.onFailure { err ->
            Log.w(TAG, "Geocoding fallback to state capital for $city - $uf", err)
        }.getOrNull()

        if (geocoded != null) return geocoded

        val capitalCoords = CAPITAL_COORDINATES[uf] ?: (-15.7939 to -47.8828)
        return Triple(
            capitalCoords.first,
            capitalCoords.second,
            "Referência regional ($stateName - $uf)"
        )
    }

    private fun computeLastTwelveCompletedMonthsRange(): Pair<String, String> {
        val cal = Calendar.getInstance()
        // ERA5 reanalysis has a ~5-day lag; step back 1 month (or 2 if early in the month)
        val monthsBack = if (cal.get(Calendar.DAY_OF_MONTH) <= 6) 2 else 1
        cal.add(Calendar.MONTH, -monthsBack)
        val endYear = cal.get(Calendar.YEAR)
        val endMonth = cal.get(Calendar.MONTH) + 1
        val endDay = cal.getActualMaximum(Calendar.DAY_OF_MONTH)

        cal.add(Calendar.MONTH, -11)
        val startYear = cal.get(Calendar.YEAR)
        val startMonth = cal.get(Calendar.MONTH) + 1

        val startStr = String.format(Locale.US, "%04d-%02d-01", startYear, startMonth)
        val endStr = String.format(Locale.US, "%04d-%02d-%02d", endYear, endMonth, endDay)
        return startStr to endStr
    }

    private fun parseDailyPrecipitation(rawJson: String): List<DailyRainRecord> {
        val root = JSONObject(rawJson)
        val daily = root.optJSONObject("daily") ?: return emptyList()
        val dates = daily.optJSONArray("time") ?: return emptyList()
        val precips = daily.optJSONArray("precipitation_sum") ?: return emptyList()

        val count = minOf(dates.length(), precips.length())
        val records = ArrayList<DailyRainRecord>(count)
        for (i in 0 until count) {
            val dateStr = dates.optString(i, "")
            val parts = dateStr.split("-")
            if (parts.size != 3) continue
            val y = parts[0].toIntOrNull() ?: continue
            val m = parts[1].toIntOrNull() ?: continue
            val d = parts[2].toIntOrNull() ?: continue
            val rain = if (precips.isNull(i)) 0f else precips.optDouble(i, 0.0).toFloat().coerceAtLeast(0f)
            records.add(DailyRainRecord(year = y, month = m, day = d, rainMm = rain))
        }
        return records
    }

    private fun buildComparison(
        cidade: String,
        estadoUf: String,
        mesPlantio: String,
        selectedMonthNumber: Int,
        localCultivo: LocalCultivo,
        baseRegasSemana: Float,
        archive: CachedClimateArchive
    ): IrrigationRainfallComparison {
        val byMonth = archive.records.groupBy { it.month }
        val selectedMonthRecords = byMonth[selectedMonthNumber].orEmpty()
        val referenceYear = selectedMonthRecords.firstOrNull()?.year
            ?: archive.records.lastOrNull()?.year
            ?: Calendar.getInstance().get(Calendar.YEAR)

        val totalChuvaMesMm = selectedMonthRecords.sumOf { it.rainMm.toDouble() }.toFloat()
        val diasComChuvaMes = selectedMonthRecords.count { it.rainMm >= RAINY_DAY_THRESHOLD_MM }

        val maxDayInMonth = selectedMonthRecords.maxOfOrNull { it.day } ?: 30
        val weekBuckets = listOf(
            Triple("Sem 1", "Dias 1–7", 1..7),
            Triple("Sem 2", "Dias 8–14", 8..14),
            Triple("Sem 3", "Dias 15–21", 15..21),
            Triple("Sem 4", "Dias 22–$maxDayInMonth", 22..31)
        )

        val weeklyPoints = weekBuckets.map { (shortLabel, periodLabel, dayRange) ->
            val bucket = selectedMonthRecords.filter { it.day in dayRange }
            val rainSum = bucket.sumOf { it.rainMm.toDouble() }.toFloat()
            val rainyDays = bucket.count { it.rainMm >= RAINY_DAY_THRESHOLD_MM }
            val adjustedRegas = adjustWeeklyIrrigationForRain(
                baseRegasSemana = baseRegasSemana,
                weeklyRainMm = rainSum,
                weeklyRainyDays = rainyDays,
                localCultivo = localCultivo
            )
            WeeklyRainIrrigationPoint(
                label = shortLabel,
                periodLabel = "$periodLabel de $mesPlantio",
                rainMm = (rainSum * 10f).roundToInt() / 10f,
                rainyDays = rainyDays,
                idealRegasSemana = adjustedRegas,
                baseRegasSemana = baseRegasSemana
            )
        }

        val monthlyPoints = (1..12).map { m ->
            val records = byMonth[m].orEmpty()
            val rainSum = records.sumOf { it.rainMm.toDouble() }.toFloat()
            val rainyDays = records.count { it.rainMm >= RAINY_DAY_THRESHOLD_MM }
            val avgWeeklyRain = rainSum / 4.3f
            val avgWeeklyRainyDays = (rainyDays / 4.3f).roundToInt()
            val adjustedRegas = adjustWeeklyIrrigationForRain(
                baseRegasSemana = baseRegasSemana,
                weeklyRainMm = avgWeeklyRain,
                weeklyRainyDays = avgWeeklyRainyDays,
                localCultivo = localCultivo
            )
            MonthlyRainIrrigationPoint(
                monthNumber = m,
                shortLabel = MONTH_SHORT_LABELS[m - 1],
                fullLabel = BrazilCatalog.meses[m - 1],
                rainMm = (rainSum * 10f).roundToInt() / 10f,
                rainyDays = rainyDays,
                idealRegasSemana = adjustedRegas,
                isSelectedMonth = m == selectedMonthNumber
            )
        }

        return IrrigationRainfallComparison(
            cidade = cidade,
            estadoUf = estadoUf,
            mesPlantio = mesPlantio,
            anoReferencia = referenceYear,
            localCultivo = localCultivo.label,
            baseRegasPorSemana = baseRegasSemana,
            totalChuvaMesMm = (totalChuvaMesMm * 10f).roundToInt() / 10f,
            diasComChuvaMes = diasComChuvaMes,
            weeklyPoints = weeklyPoints,
            monthlyPoints = monthlyPoints,
            fonteDados = archive.resolvedLocationLabel
        )
    }

    private fun computeBaseRegasPerWeek(irrigacao: IrrigacaoInfo): Float {
        val text = BrazilCatalog.normalizeForSearch(
            "${irrigacao.faseMeio} ${irrigacao.faseInicio}"
        )
        return when {
            text.contains("2 vezes ao dia") ||
                text.contains("duas vezes ao dia") ||
                text.contains("2x ao dia") ||
                text.contains("2x por dia") -> 10.5f
            irrigacao.intervaloDiasSugerido <= 1 -> 7f
            else -> ((7f / irrigacao.intervaloDiasSugerido.coerceIn(1, 7)) * 2f).roundToInt() / 2f
        }
    }

    private fun adjustWeeklyIrrigationForRain(
        baseRegasSemana: Float,
        weeklyRainMm: Float,
        weeklyRainyDays: Int,
        localCultivo: LocalCultivo
    ): Float {
        // Estufa é coberta: a chuva externa não irriga diretamente o substrato
        if (localCultivo == LocalCultivo.ESTUFA) {
            return baseRegasSemana
        }

        // Em vaso/horta vertical o substrato seca mais rápido e muitas vezes fica sob beiral
        val rainAbsorptionFactor = when (localCultivo) {
            LocalCultivo.CANTEIRO -> 1.0f
            LocalCultivo.VASO -> 0.45f
            LocalCultivo.HORTA_VERTICAL -> 0.35f
            LocalCultivo.ESTUFA -> 0f
        }

        val rainReduction = when {
            weeklyRainMm >= 65f -> 0.65f
            weeklyRainMm >= 35f -> 0.45f
            weeklyRainMm >= 18f -> 0.28f
            weeklyRainMm >= 8f || weeklyRainyDays >= 2 -> 0.14f
            else -> 0f
        } * rainAbsorptionFactor

        val minSafeFloor = max(1f, baseRegasSemana * 0.25f)
        val raw = (baseRegasSemana * (1f - rainReduction)).coerceAtLeast(minSafeFloor)
        return (raw * 2f).roundToInt() / 2f
    }

    private fun resolveMonthNumber(monthName: String): Int {
        val clean = BrazilCatalog.normalizeForSearch(monthName)
        val idx = BrazilCatalog.meses.indexOfFirst {
            BrazilCatalog.normalizeForSearch(it) == clean
        }
        return if (idx >= 0) idx + 1 else (Calendar.getInstance().get(Calendar.MONTH) + 1)
    }

    private fun httpGet(urlString: String): String {
        val connection = (URL(urlString).openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = CONNECT_TIMEOUT_MS
            readTimeout = READ_TIMEOUT_MS
            setRequestProperty("Accept", "application/json")
            setRequestProperty("User-Agent", "SiTa-HortaBrasil-Android/1.0")
        }
        return try {
            val code = connection.responseCode
            if (code !in 200..299) {
                throw IllegalStateException("HTTP $code ao consultar dados climáticos")
            }
            connection.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
        } finally {
            connection.disconnect()
        }
    }

    private fun cacheFile(key: String): File {
        val dir = File(cacheDir, CACHE_SUBDIR).apply { if (!exists()) mkdirs() }
        return File(dir, "$key.json")
    }

    private fun readDiskCache(key: String): CachedClimateArchive? = runCatching {
        val file = cacheFile(key)
        if (!file.exists()) return null
        val wrapper = JSONObject(file.readText(Charsets.UTF_8))
        val label = wrapper.optString("label", "ERA5")
        val payload = wrapper.optString("payload", "")
        val records = parseDailyPrecipitation(payload)
        if (records.isEmpty()) null else CachedClimateArchive(label, records)
    }.getOrNull()

    private fun writeDiskCache(key: String, label: String, rawPayload: String) {
        runCatching {
            val wrapper = JSONObject().apply {
                put("label", label)
                put("payload", rawPayload)
            }
            cacheFile(key).writeText(wrapper.toString(), Charsets.UTF_8)
        }.onFailure { err ->
            Log.w(TAG, "Failed caching climate data for $key", err)
        }
    }

    companion object {
        private const val TAG = "RainfallClimateRepo"
        private const val CACHE_SUBDIR = "rain_history_v1"
        private const val CONNECT_TIMEOUT_MS = 10_000
        private const val READ_TIMEOUT_MS = 15_000
        private const val RAINY_DAY_THRESHOLD_MM = 2.0f

        private val MONTH_SHORT_LABELS = listOf(
            "Jan", "Fev", "Mar", "Abr", "Mai", "Jun",
            "Jul", "Ago", "Set", "Out", "Nov", "Dez"
        )

        private val CAPITAL_COORDINATES: Map<String, Pair<Double, Double>> = mapOf(
            "AC" to (-9.9754 to -67.8249),
            "AL" to (-9.6658 to -35.7350),
            "AP" to (0.0349 to -51.0694),
            "AM" to (-3.1190 to -60.0217),
            "BA" to (-12.9714 to -38.5014),
            "CE" to (-3.7319 to -38.5267),
            "DF" to (-15.7939 to -47.8828),
            "ES" to (-20.3155 to -40.3128),
            "GO" to (-16.6869 to -49.2648),
            "MA" to (-2.5307 to -44.3068),
            "MT" to (-15.6010 to -56.0974),
            "MS" to (-20.4697 to -54.6201),
            "MG" to (-19.9167 to -43.9345),
            "PA" to (-1.4558 to -48.4902),
            "PB" to (-7.1195 to -34.8450),
            "PR" to (-25.4284 to -49.2733),
            "PE" to (-8.0476 to -34.8770),
            "PI" to (-5.0919 to -42.8034),
            "RJ" to (-22.9068 to -43.1729),
            "RN" to (-5.7945 to -35.2110),
            "RS" to (-30.0346 to -51.2177),
            "RO" to (-8.7612 to -63.9004),
            "RR" to (2.8235 to -60.6758),
            "SC" to (-27.5954 to -48.5480),
            "SP" to (-23.5505 to -46.6333),
            "SE" to (-10.9472 to -37.0731),
            "TO" to (-10.1840 to -48.3336)
        )
    }
}
