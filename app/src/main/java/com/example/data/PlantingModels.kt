package com.example.data

import androidx.annotation.Keep
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties

sealed interface UiState<out T> {
    data object Idle : UiState<Nothing>
    data object Loading : UiState<Nothing>
    data class Success<T>(val data: T) : UiState<T>
    data class Error(val message: String) : UiState<Nothing>
}

data class GuideRequest(
    val hortalica: String,
    val estadoUf: String,
    val estadoNome: String,
    val cidade: String,
    val mesPlantio: String,
    val localCultivo: LocalCultivo
)

enum class SpotVerdictColor(val tituloPadrao: String, val descricaoCurta: String) {
    VERDE("Local certo para plantar!", "Verde • Condições favoráveis para esta hortaliça"),
    AMARELO("Pode melhorar, mas dá para plantar", "Amarelo • Viável com pequenos ajustes"),
    VERMELHO("Não é possível plantar neste local", "Vermelho • Local inadequado para esta hortaliça");

    companion object {
        fun fromRaw(raw: String): SpotVerdictColor {
            val clean = BrazilCatalog.normalizeForSearch(raw)
            return when {
                clean.contains("vermelh") || clean.contains("red") || clean.contains("nao") -> VERMELHO
                clean.contains("amarel") || clean.contains("yellow") || clean.contains("melhor") -> AMARELO
                else -> VERDE
            }
        }
    }
}

data class SpotAnalysisResult(
    val hortalica: String,
    val cor: SpotVerdictColor,
    val tituloVeredito: String,
    val resumo: String,
    val luzAvaliacao: String,
    val espacoSoloAvaliacao: String,
    val oQueEstaBom: String,
    val oQueMelhorar: String,
    val recomendacaoFinal: String
)

enum class AdequacaoBadge(val label: String) {
    IDEAL("Ideal"),
    POSSIVEL_COM_CUIDADOS("Possível com cuidados"),
    NAO_RECOMENDADO("Não recomendado");

    companion object {
        fun fromRaw(raw: String): AdequacaoBadge {
            val clean = BrazilCatalog.normalizeForSearch(raw)
            return when {
                clean.contains("nao recomend") -> NAO_RECOMENDADO
                clean.contains("cuidado") || clean.contains("possivel") -> POSSIVEL_COM_CUIDADOS
                else -> IDEAL
            }
        }
    }
}

data class SoloInfo(
    val tipo: String,
    val ph: String,
    val adubacao: String
)

data class ClimaInfo(
    val tempMin: String,
    val tempMax: String,
    val luz: String,
    val tolerancia: String,
    val melhoresMeses: String
)

data class IrrigacaoInfo(
    val faseInicio: String,
    val faseMeio: String,
    val faseFim: String,
    val horario: String,
    val intervaloDiasSugerido: Int
)

data class FaseCiclo(
    val dias: Int,
    val cuidados: String
)

data class CicloInfo(
    val inicio: FaseCiclo,
    val meio: FaseCiclo,
    val fim: FaseCiclo,
    val totalDias: Int
)

data class PlantingGuide(
    val hortalica: String,
    val estado: String,
    val cidade: String,
    val mesPlantio: String,
    val localCultivo: String,
    val adequacao: AdequacaoBadge,
    val vegetacaoBioma: String,
    val alternativas: String,
    val solo: SoloInfo,
    val clima: ClimaInfo,
    val irrigacao: IrrigacaoInfo,
    val ciclo: CicloInfo,
    val espacamento: String,
    val consorcio: String,
    val pragas: String,
    val dicas: String
)

@Keep
@IgnoreExtraProperties
data class FavoriteGuide(
    @DocumentId val id: String = "",
    val userId: String = "",
    val hortalica: String = "",
    val estado: String = "",
    val cidade: String = "",
    val mesPlantio: String = "",
    val localCultivo: String = "Canteiro",
    val adequacao: String = "Ideal",
    val vegetacaoBioma: String = "",
    val soloTipo: String = "",
    val soloPh: String = "",
    val soloAdubacao: String = "",
    val tempMin: String = "",
    val tempMax: String = "",
    val luz: String = "",
    val melhoresMeses: String = "",
    val toleranciaClima: String = "",
    val alternativas: String = "",
    val irrigacaoInicio: String = "",
    val irrigacaoMeio: String = "",
    val irrigacaoFim: String = "",
    val irrigacaoHorario: String = "",
    val cicloInicioDias: Long = 15L,
    val cicloInicioCuidados: String = "",
    val cicloMeioDias: Long = 30L,
    val cicloMeioCuidados: String = "",
    val cicloFimDias: Long = 15L,
    val cicloFimCuidados: String = "",
    val totalDias: Long = 60L,
    val espacamento: String = "",
    val consorcio: String = "",
    val pragas: String = "",
    val dicas: String = "",
    val createdAt: Timestamp? = null,
    val updatedAt: Timestamp? = null
) {
    fun toPlantingGuide(): PlantingGuide = PlantingGuide(
        hortalica = hortalica,
        estado = estado,
        cidade = cidade,
        mesPlantio = mesPlantio,
        localCultivo = localCultivo,
        adequacao = AdequacaoBadge.fromRaw(adequacao),
        vegetacaoBioma = vegetacaoBioma,
        alternativas = alternativas,
        solo = SoloInfo(tipo = soloTipo, ph = soloPh, adubacao = soloAdubacao),
        clima = ClimaInfo(
            tempMin = tempMin,
            tempMax = tempMax,
            luz = luz,
            tolerancia = toleranciaClima,
            melhoresMeses = melhoresMeses
        ),
        irrigacao = IrrigacaoInfo(
            faseInicio = irrigacaoInicio,
            faseMeio = irrigacaoMeio,
            faseFim = irrigacaoFim,
            horario = irrigacaoHorario,
            intervaloDiasSugerido = 1
        ),
        ciclo = CicloInfo(
            inicio = FaseCiclo(cicloInicioDias.toInt().coerceIn(1, 365), cicloInicioCuidados),
            meio = FaseCiclo(cicloMeioDias.toInt().coerceIn(1, 365), cicloMeioCuidados),
            fim = FaseCiclo(cicloFimDias.toInt().coerceIn(1, 365), cicloFimCuidados),
            totalDias = totalDias.toInt().coerceIn(1, 730)
        ),
        espacamento = espacamento,
        consorcio = consorcio,
        pragas = pragas,
        dicas = dicas
    )
}

enum class PlantStatus {
    ACTIVE,
    HARVESTED;

    companion object {
        fun safeValueOf(raw: String): PlantStatus =
            entries.firstOrNull { it.name == raw } ?: ACTIVE
    }
}

@Keep
@IgnoreExtraProperties
data class GardenPlant(
    @DocumentId val id: String = "",
    val userId: String = "",
    val hortalica: String = "",
    val localCultivo: String = "Canteiro",
    val cidadeEstado: String = "",
    val adequacao: String = "Ideal",
    val wateringIntervalDays: Long = 1L,
    val wateringScheduleLabel: String = "",
    val reminderEnabled: Boolean = true,
    val reminderTime: String = "07:30",
    val plantedAtEpochMillis: Long = 0L,
    val lastWateredEpochMillis: Long = 0L,
    val wateredCount: Long = 0L,
    val cicloInicioDias: Long = 15L,
    val cicloMeioDias: Long = 30L,
    val totalDias: Long = 60L,
    val status: String = "ACTIVE",
    val notes: String = "",
    val createdAt: Timestamp? = null,
    val updatedAt: Timestamp? = null
) {
    fun parsedStatus(): PlantStatus = PlantStatus.safeValueOf(status)

    fun daysSincePlanted(nowMillis: Long = System.currentTimeMillis()): Int {
        if (plantedAtEpochMillis <= 0L) return 0
        val diff = (nowMillis - plantedAtEpochMillis).coerceAtLeast(0L)
        return (diff / 86_400_000L).toInt()
    }

    fun currentStageLabel(nowMillis: Long = System.currentTimeMillis()): String {
        if (parsedStatus() == PlantStatus.HARVESTED) return "Colhida"
        val day = daysSincePlanted(nowMillis)
        return when {
            day < cicloInicioDias -> "Início (Germinação/Muda)"
            day < cicloInicioDias + cicloMeioDias -> "Meio (Crescimento)"
            day < totalDias -> "Fim (Pré-colheita)"
            else -> "Pronta para colheita!"
        }
    }

    fun progressFraction(nowMillis: Long = System.currentTimeMillis()): Float {
        if (parsedStatus() == PlantStatus.HARVESTED) return 1f
        val total = totalDias.coerceAtLeast(1L).toFloat()
        return (daysSincePlanted(nowMillis) / total).coerceIn(0f, 1f)
    }

    fun needsWaterToday(nowMillis: Long = System.currentTimeMillis()): Boolean {
        if (parsedStatus() != PlantStatus.ACTIVE) return false
        if (lastWateredEpochMillis <= 0L) return true
        val intervalMillis = wateringIntervalDays.coerceIn(1L, 14L) * 86_400_000L
        return (nowMillis - lastWateredEpochMillis) >= (intervalMillis - 14_400_000L) // 4h grace window
    }

    fun daysUntilNextWatering(nowMillis: Long = System.currentTimeMillis()): Int {
        if (parsedStatus() != PlantStatus.ACTIVE) return -1
        if (lastWateredEpochMillis <= 0L) return 0
        val nextMillis = lastWateredEpochMillis + wateringIntervalDays.coerceIn(1L, 14L) * 86_400_000L
        val remaining = nextMillis - nowMillis
        if (remaining <= 14_400_000L) return 0
        return ((remaining + 43_200_000L) / 86_400_000L).toInt().coerceAtLeast(1)
    }
}
