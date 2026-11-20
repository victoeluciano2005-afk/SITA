package com.example.data

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.text.Collator
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap

class IbgeMunicipiosRepository(
    private val cacheDir: File
) {
    private val memoryCache = ConcurrentHashMap<String, List<String>>()

    suspend fun getMunicipios(state: BrazilianState): Result<List<String>> =
        withContext(Dispatchers.IO) {
            val uf = state.uf.uppercase(Locale.ROOT)

            memoryCache[uf]?.let { cached ->
                if (cached.isNotEmpty()) return@withContext Result.success(cached)
            }

            readDiskCache(uf)?.let { diskCached ->
                if (diskCached.isNotEmpty()) {
                    memoryCache[uf] = diskCached
                    return@withContext Result.success(diskCached)
                }
            }

            val ibgeResult = fetchFromIbge(uf)
            if (ibgeResult.isSuccess) {
                val list = ibgeResult.getOrThrow()
                if (list.isNotEmpty()) {
                    memoryCache[uf] = list
                    writeDiskCache(uf, list)
                    return@withContext Result.success(list)
                }
            } else {
                Log.w(TAG, "IBGE primary fetch failed for $uf", ibgeResult.exceptionOrNull())
            }

            val fallbackResult = fetchFromBrasilApi(uf)
            if (fallbackResult.isSuccess) {
                val list = fallbackResult.getOrThrow()
                if (list.isNotEmpty()) {
                    memoryCache[uf] = list
                    writeDiskCache(uf, list)
                    return@withContext Result.success(list)
                }
            } else {
                Log.w(TAG, "BrasilAPI fallback fetch failed for $uf", fallbackResult.exceptionOrNull())
            }

            Result.failure(
                IllegalStateException(
                    "Não foi possível baixar a lista completa de municípios de ${state.nome}. Verifique sua conexão."
                )
            )
        }

    private fun fetchFromIbge(uf: String): Result<List<String>> = runCatching {
        val url = "https://servicodados.ibge.gov.br/api/v1/localidades/estados/$uf/municipios?orderBy=nome"
        val body = httpGet(url)
        val array = JSONArray(body)
        val names = LinkedHashSet<String>(array.length())
        for (i in 0 until array.length()) {
            val item = array.optJSONObject(i) ?: continue
            val nome = item.optString("nome", "").trim()
            if (nome.isNotEmpty()) {
                names.add(nome)
            }
        }
        sortPortuguese(names.toList())
    }

    private fun fetchFromBrasilApi(uf: String): Result<List<String>> = runCatching {
        val url = "https://brasilapi.com.br/api/ibge/municipios/v1/$uf?providers=dados-abertos-br,gov,wikipedia"
        val body = httpGet(url)
        val array = JSONArray(body)
        val names = LinkedHashSet<String>(array.length())
        for (i in 0 until array.length()) {
            val item = array.optJSONObject(i) ?: continue
            val rawName = item.optString("nome", "").trim()
            if (rawName.isNotEmpty()) {
                names.add(formatPortugueseCityName(rawName))
            }
        }
        sortPortuguese(names.toList())
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
                throw IllegalStateException("HTTP $code ao consultar municípios")
            }
            connection.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
        } finally {
            connection.disconnect()
        }
    }

    private fun cacheFile(uf: String): File {
        val dir = File(cacheDir, CACHE_SUBDIR).apply { if (!exists()) mkdirs() }
        return File(dir, "municipios_${uf.lowercase(Locale.ROOT)}.json")
    }

    private fun readDiskCache(uf: String): List<String>? = runCatching {
        val file = cacheFile(uf)
        if (!file.exists()) return null
        val json = JSONArray(file.readText(Charsets.UTF_8))
        val list = ArrayList<String>(json.length())
        for (i in 0 until json.length()) {
            val item = json.optString(i, "").trim()
            if (item.isNotEmpty()) list.add(item)
        }
        list.takeIf { it.isNotEmpty() }
    }.getOrNull()

    private fun writeDiskCache(uf: String, municipios: List<String>) {
        runCatching {
            val json = JSONArray()
            municipios.forEach { json.put(it) }
            cacheFile(uf).writeText(json.toString(), Charsets.UTF_8)
        }.onFailure { err ->
            Log.w(TAG, "Failed writing municipality cache for $uf", err)
        }
    }

    private fun sortPortuguese(items: List<String>): List<String> {
        val collator = Collator.getInstance(Locale.forLanguageTag("pt-BR")).apply {
            strength = Collator.PRIMARY
        }
        return items.sortedWith(collator)
    }

    private fun formatPortugueseCityName(raw: String): String {
        val locale = Locale.forLanguageTag("pt-BR")
        // Keep original casing if already mixed-case (e.g., IBGE-formatted)
        val isAllUpper = raw == raw.uppercase(locale) && raw != raw.lowercase(locale)
        if (!isAllUpper) return raw

        val lowerWords = setOf("da", "de", "do", "das", "dos", "e")
        return raw.lowercase(locale)
            .split(" ")
            .filter { it.isNotBlank() }
            .mapIndexed { index, word ->
                when {
                    index > 0 && word in lowerWords -> word
                    word.startsWith("d'") && word.length > 2 -> {
                        "d'" + word.substring(2).replaceFirstChar { it.titlecase(locale) }
                    }
                    word.contains("-") -> {
                        word.split("-").joinToString("-") { part ->
                            if (part in lowerWords) part
                            else part.replaceFirstChar { it.titlecase(locale) }
                        }
                    }
                    else -> word.replaceFirstChar { it.titlecase(locale) }
                }
            }
            .joinToString(" ")
    }

    companion object {
        private const val TAG = "IbgeMunicipiosRepo"
        private const val CACHE_SUBDIR = "ibge_municipios"
        private const val CONNECT_TIMEOUT_MS = 10_000
        private const val READ_TIMEOUT_MS = 12_000

        fun filterMunicipios(municipios: List<String>, query: String, limit: Int = Int.MAX_VALUE): List<String> {
            val clean = BrazilCatalog.normalizeForSearch(query)
            if (clean.isEmpty()) return municipios.take(limit)
            val startsWithMatches = municipios.filter {
                BrazilCatalog.normalizeForSearch(it).startsWith(clean)
            }
            val containsMatches = municipios.filter {
                val normalized = BrazilCatalog.normalizeForSearch(it)
                !normalized.startsWith(clean) && normalized.contains(clean)
            }
            return (startsWithMatches + containsMatches).take(limit)
        }
    }
}
