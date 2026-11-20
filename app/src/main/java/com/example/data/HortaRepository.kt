package com.example.data

import android.content.Context
import com.example.R
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

class HortaRepository(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth = Firebase.auth
) {
    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    private fun requireUserId(): String =
        auth.currentUser?.uid
            ?: throw IllegalStateException("Faça login com o Google antes de acessar seus dados.")

    private fun String.clampText(maxLen: Int, fallback: String): String {
        val trimmed = this.trim()
        val safe = if (trimmed.isEmpty()) fallback else trimmed
        return if (safe.length > maxLen) safe.take(maxLen) else safe
    }

    // --- FAVORITES ---

    fun observeFavorites(userId: String): Flow<List<FavoriteGuide>> = flow {
        require(userId.isNotBlank()) { "UID inválido." }
        val path = "users/$userId/favorites"
        emitAll(
            db.collection("users").document(userId).collection("favorites")
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        FavoriteGuide::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    ).sortedByDescending { it.createdAt?.seconds ?: 0L }
                }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val toLog = firestoreError ?: (error as? Exception)
                    if (toLog != null && auth.currentUser?.uid == userId) {
                        handleFirestoreError(toLog, OperationType.LIST, path)
                    }
                    throw error
                }
        )
    }

    suspend fun addFavorite(guide: PlantingGuide): Result<String> {
        var docPath = "users/${auth.currentUser?.uid ?: "unauthenticated"}/favorites"
        return try {
            val uid = requireUserId()
            val docRef = db.collection("users").document(uid).collection("favorites").document()
            docPath = docRef.path

            val cultivo = LocalCultivo.fromLabel(guide.localCultivo).label
            val adequacao = guide.adequacao.label

            val payload = mutableMapOf<String, Any?>(
                "userId" to uid,
                "hortalica" to guide.hortalica.clampText(80, "Hortaliça"),
                "estado" to guide.estado.clampText(40, "BR"),
                "cidade" to guide.cidade.clampText(80, "Brasil"),
                "mesPlantio" to guide.mesPlantio.clampText(30, "Janeiro"),
                "localCultivo" to cultivo,
                "adequacao" to adequacao,
                "vegetacaoBioma" to guide.vegetacaoBioma.clampText(300, "Bioma brasileiro"),
                "soloTipo" to guide.solo.tipo.clampText(400, "Solo fértil e drenado"),
                "soloPh" to guide.solo.ph.clampText(100, "6,0 a 6,8"),
                "soloAdubacao" to guide.solo.adubacao.clampText(700, "Adubação orgânica"),
                "tempMin" to guide.clima.tempMin.clampText(50, "15°C"),
                "tempMax" to guide.clima.tempMax.clampText(50, "28°C"),
                "luz" to guide.clima.luz.clampText(400, "Sol pleno"),
                "melhoresMeses" to guide.clima.melhoresMeses.clampText(300, "Ano todo"),
                "toleranciaClima" to guide.clima.tolerancia.trim().take(600).ifEmpty { null },
                "alternativas" to guide.alternativas.trim().take(400).ifEmpty { null },
                "irrigacaoInicio" to guide.irrigacao.faseInicio.clampText(500, "Rega diária leve"),
                "irrigacaoMeio" to guide.irrigacao.faseMeio.clampText(500, "Rega diária"),
                "irrigacaoFim" to guide.irrigacao.faseFim.clampText(500, "Rega moderada"),
                "irrigacaoHorario" to guide.irrigacao.horario.clampText(400, "Início da manhã"),
                "cicloInicioDias" to guide.ciclo.inicio.dias.coerceIn(1, 365).toLong(),
                "cicloInicioCuidados" to guide.ciclo.inicio.cuidados.clampText(800, "Germinação e cuidados iniciais"),
                "cicloMeioDias" to guide.ciclo.meio.dias.coerceIn(1, 365).toLong(),
                "cicloMeioCuidados" to guide.ciclo.meio.cuidados.clampText(800, "Crescimento vegetativo"),
                "cicloFimDias" to guide.ciclo.fim.dias.coerceIn(1, 365).toLong(),
                "cicloFimCuidados" to guide.ciclo.fim.cuidados.clampText(800, "Colheita"),
                "totalDias" to guide.ciclo.totalDias.coerceIn(1, 730).toLong(),
                "espacamento" to guide.espacamento.trim().take(300).ifEmpty { null },
                "consorcio" to guide.consorcio.clampText(800, "Plantas companheiras variadas"),
                "pragas" to guide.pragas.clampText(900, "Controle orgânico preventivo"),
                "dicas" to guide.dicas.clampText(1000, "Consulte a EMATER local."),
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            ).filterValues { it != null }

            docRef.set(payload).await()
            Result.success(docRef.id)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun removeFavorite(favoriteId: String): Result<Unit> {
        var docPath = "users/${auth.currentUser?.uid ?: "unauthenticated"}/favorites/$favoriteId"
        return try {
            val uid = requireUserId()
            require(favoriteId.isNotBlank()) { "ID de favorito inválido." }
            val docRef = db.collection("users").document(uid)
                .collection("favorites").document(favoriteId)
            docPath = docRef.path
            docRef.delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docPath)
            Result.failure(e)
        }
    }

    // --- MINHA HORTA (GARDEN PLANTS) ---

    fun observeGardenPlants(userId: String): Flow<List<GardenPlant>> = flow {
        require(userId.isNotBlank()) { "UID inválido." }
        val path = "users/$userId/garden_plants"
        emitAll(
            db.collection("users").document(userId).collection("garden_plants")
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        GardenPlant::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    ).sortedByDescending { it.createdAt?.seconds ?: 0L }
                }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val toLog = firestoreError ?: (error as? Exception)
                    if (toLog != null && auth.currentUser?.uid == userId) {
                        handleFirestoreError(toLog, OperationType.LIST, path)
                    }
                    throw error
                }
        )
    }

    suspend fun addGardenPlant(
        hortalica: String,
        localCultivo: LocalCultivo,
        cidadeEstado: String,
        adequacao: AdequacaoBadge,
        wateringIntervalDays: Int,
        wateringScheduleLabel: String,
        reminderEnabled: Boolean,
        reminderTime: String,
        cicloInicioDias: Int,
        cicloMeioDias: Int,
        totalDias: Int,
        notes: String
    ): Result<String> {
        var docPath = "users/${auth.currentUser?.uid ?: "unauthenticated"}/garden_plants"
        return try {
            val uid = requireUserId()
            val docRef = db.collection("users").document(uid).collection("garden_plants").document()
            docPath = docRef.path

            val nowMillis = System.currentTimeMillis().coerceIn(1_577_836_800_000L, 4_102_444_800_000L)
            val safeTime = reminderTime.trim().ifBlank { "07:30" }.take(10).let {
                if (it.length < 4) "07:30" else it
            }

            val payload = mutableMapOf<String, Any?>(
                "userId" to uid,
                "hortalica" to hortalica.clampText(80, "Hortaliça"),
                "localCultivo" to localCultivo.label,
                "cidadeEstado" to cidadeEstado.clampText(120, "Brasil"),
                "adequacao" to adequacao.label,
                "wateringIntervalDays" to wateringIntervalDays.coerceIn(1, 14).toLong(),
                "wateringScheduleLabel" to wateringScheduleLabel.clampText(240, "Regar conforme umidade do solo"),
                "reminderEnabled" to reminderEnabled,
                "reminderTime" to safeTime,
                "plantedAtEpochMillis" to nowMillis,
                "lastWateredEpochMillis" to 0L,
                "wateredCount" to 0L,
                "cicloInicioDias" to cicloInicioDias.coerceIn(1, 365).toLong(),
                "cicloMeioDias" to cicloMeioDias.coerceIn(1, 365).toLong(),
                "totalDias" to totalDias.coerceIn(1, 730).toLong(),
                "status" to PlantStatus.ACTIVE.name,
                "notes" to notes.trim().take(500).ifEmpty { null },
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            ).filterValues { it != null }

            docRef.set(payload).await()
            Result.success(docRef.id)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun logWatering(plant: GardenPlant): Result<Unit> {
        var docPath = "users/${auth.currentUser?.uid ?: "unauthenticated"}/garden_plants/${plant.id}"
        return try {
            val uid = requireUserId()
            require(plant.id.isNotBlank()) { "ID da planta inválido." }
            require(plant.parsedStatus() == PlantStatus.ACTIVE) { "Planta já colhida." }

            val docRef = db.collection("users").document(uid)
                .collection("garden_plants").document(plant.id)
            docPath = docRef.path

            val nextCount = (plant.wateredCount + 1L).coerceAtMost(10_000L)
            val nowMillis = System.currentTimeMillis().coerceIn(0L, 4_102_444_800_000L)

            docRef.update(
                mapOf(
                    "lastWateredEpochMillis" to nowMillis,
                    "wateredCount" to nextCount,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun updateCareSettings(
        plantId: String,
        wateringIntervalDays: Int,
        wateringScheduleLabel: String,
        reminderEnabled: Boolean,
        reminderTime: String,
        notes: String
    ): Result<Unit> {
        var docPath = "users/${auth.currentUser?.uid ?: "unauthenticated"}/garden_plants/$plantId"
        return try {
            val uid = requireUserId()
            require(plantId.isNotBlank()) { "ID da planta inválido." }

            val docRef = db.collection("users").document(uid)
                .collection("garden_plants").document(plantId)
            docPath = docRef.path

            val safeTime = reminderTime.trim().ifBlank { "07:30" }.take(10).let {
                if (it.length < 4) "07:30" else it
            }

            docRef.update(
                mapOf(
                    "wateringIntervalDays" to wateringIntervalDays.coerceIn(1, 14).toLong(),
                    "wateringScheduleLabel" to wateringScheduleLabel.clampText(240, "Regar conforme necessidade"),
                    "reminderEnabled" to reminderEnabled,
                    "reminderTime" to safeTime,
                    "notes" to notes.trim().take(500),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun markHarvested(plantId: String): Result<Unit> {
        var docPath = "users/${auth.currentUser?.uid ?: "unauthenticated"}/garden_plants/$plantId"
        return try {
            val uid = requireUserId()
            require(plantId.isNotBlank()) { "ID da planta inválido." }
            val docRef = db.collection("users").document(uid)
                .collection("garden_plants").document(plantId)
            docPath = docRef.path

            docRef.update(
                mapOf(
                    "status" to PlantStatus.HARVESTED.name,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun deleteGardenPlant(plantId: String): Result<Unit> {
        var docPath = "users/${auth.currentUser?.uid ?: "unauthenticated"}/garden_plants/$plantId"
        return try {
            val uid = requireUserId()
            require(plantId.isNotBlank()) { "ID da planta inválido." }
            val docRef = db.collection("users").document(uid)
                .collection("garden_plants").document(plantId)
            docPath = docRef.path
            docRef.delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docPath)
            Result.failure(e)
        }
    }
}
