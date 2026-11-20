package com.example.data

import android.graphics.Bitmap
import android.util.Log
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.Schema
import com.google.firebase.ai.type.ThinkingLevel
import com.google.firebase.ai.type.content
import com.google.firebase.ai.type.generationConfig
import com.google.firebase.ai.type.thinkingConfig
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import org.json.JSONObject

private const val TAG = "GeminiPlantingRepo"
private const val MODEL_NAME = "gemini-3.5-flash-lite"
private const val TIMEOUT_MS = 60_000L

class GeminiPlantingRepository {

    private val systemPrompt = """
        Você é um agrônomo especialista em horticultura brasileira (Embrapa, 5 regiões e 6 biomas do Brasil: Amazônia, Cerrado, Mata Atlântica, Caatinga, Pampa e Pantanal).
        Adapte todas as respostas à hortaliça, cidade, estado, bioma, mês de plantio (estações seca/chuvosa no N/NE/CO; 4 estações no Sul) e local de cultivo (canteiro, vaso, horta vertical ou estufa).
        Use linguagem simples, prática e objetiva para iniciantes (1 a 2 frases curtas por campo).
        Quando um dado local for aproximado, indique que é estimativa e recomende consultar a extensão rural local (EMATER).
        Responda SOMENTE em JSON válido no schema fornecido.
    """.trimIndent()

    private val visionSystemPrompt = """
        Você é um agrônomo especialista em horticultura brasileira analisando a foto do local onde o usuário pretende plantar uma hortaliça específica.
        Priorize rigorosamente as necessidades reais da hortaliça escolhida (quantidade de luz solar direta/meia-sombra, profundidade e espaço para raízes, tipo de solo/vaso/substrato, drenagem, ventilação e proteção) e também a realidade do usuário (cidade, estado, mês e local de cultivo).
        Classifique a foto obrigatoriamente em uma de três cores (semáforo agronômico):
        - "VERDE": O local na foto está certo e favorável para plantar essa hortaliça (boa iluminação, espaço/profundidade adequados e ambiente apropriado).
        - "AMARELO": O local pode melhorar, mas dá para plantar com ajustes simples (ex.: vaso um pouco raso, solo precisando de adubo/drenagem, sombra parcial ou necessidade de reposicionar para pegar mais sol).
        - "VERMELHO": Não é possível plantar essa hortaliça neste local (ex.: ambiente interno escuro sem sol, espaço sem recipiente/terra onde não cabe a raiz, superfície inadequada, excesso de sombra/umidade incompatível, ou foto que não mostra um local viável de cultivo).
        Use linguagem simples, direta e prática para iniciantes (1 a 2 frases curtas por campo).
        Responda SOMENTE em JSON válido no schema fornecido.
    """.trimIndent()

    private val plantingGuideSchema = Schema.obj(
        properties = mapOf(
            "hortalica" to Schema.string(description = "Nome da hortaliça"),
            "adequacao" to Schema.enumeration(
                values = listOf("Ideal", "Possível com cuidados", "Não recomendado"),
                description = "Selo de adequação"
            ),
            "solo" to Schema.obj(
                properties = mapOf(
                    "tipo" to Schema.string(description = "Solo ou substrato ideal"),
                    "ph" to Schema.string(description = "Faixa de pH ideal"),
                    "adubacao" to Schema.string(description = "Adubação recomendada")
                )
            ),
            "vegetacao_bioma" to Schema.string(description = "Bioma local e influência"),
            "alternativas" to Schema.string(description = "Hortaliças alternativas recomendadas"),
            "clima" to Schema.obj(
                properties = mapOf(
                    "temp_min" to Schema.string(description = "Temperatura mínima"),
                    "temp_max" to Schema.string(description = "Temperatura máxima"),
                    "luz" to Schema.string(description = "Luz solar diária"),
                    "tolerancia" to Schema.string(description = "Tolerância clima e avaliação do mês"),
                    "melhores_meses" to Schema.string(description = "Melhores meses na região")
                )
            ),
            "irrigacao" to Schema.obj(
                properties = mapOf(
                    "fase_inicio" to Schema.string(description = "Rega na fase inicial"),
                    "fase_meio" to Schema.string(description = "Rega na fase média"),
                    "fase_fim" to Schema.string(description = "Rega na fase final"),
                    "horario" to Schema.string(description = "Horário e ajuste climático"),
                    "intervalo_dias_sugerido" to Schema.integer(description = "Dias entre regas (1 a 7)")
                )
            ),
            "ciclo" to Schema.obj(
                properties = mapOf(
                    "inicio" to Schema.obj(
                        properties = mapOf(
                            "dias" to Schema.integer(description = "Dias fase inicial"),
                            "cuidados" to Schema.string(description = "Germinação e transplante")
                        )
                    ),
                    "meio" to Schema.obj(
                        properties = mapOf(
                            "dias" to Schema.integer(description = "Dias fase média"),
                            "cuidados" to Schema.string(description = "Crescimento e adubação")
                        )
                    ),
                    "fim" to Schema.obj(
                        properties = mapOf(
                            "dias" to Schema.integer(description = "Dias fase final"),
                            "cuidados" to Schema.string(description = "Formação e colheita")
                        )
                    ),
                    "total_dias" to Schema.integer(description = "Total de dias até colheita")
                )
            ),
            "espacamento" to Schema.string(description = "Espaçamento ou tamanho do vaso"),
            "consorcio" to Schema.string(description = "Plantas companheiras e incompatíveis"),
            "pragas" to Schema.string(description = "Pragas comuns e controle orgânico"),
            "dicas" to Schema.string(description = "Dicas práticas e nota EMATER")
        )
    )

    private val spotAnalysisSchema = Schema.obj(
        properties = mapOf(
            "cor" to Schema.enumeration(
                values = listOf("VERDE", "AMARELO", "VERMELHO"),
                description = "Cor do veredito: VERDE, AMARELO ou VERMELHO"
            ),
            "titulo_veredito" to Schema.string(description = "Título curto do veredito para a hortaliça"),
            "resumo" to Schema.string(description = "Avaliação geral da foto considerando a hortaliça escolhida"),
            "luz_avaliacao" to Schema.string(description = "Avaliação de luminosidade e sol visíveis na foto"),
            "espaco_solo_avaliacao" to Schema.string(description = "Avaliação do espaço, vaso, solo ou estrutura na foto"),
            "o_que_esta_bom" to Schema.string(description = "Pontos positivos observados na foto"),
            "o_que_melhorar" to Schema.string(description = "Ajustes práticos necessários ou motivo do impedimento"),
            "recomendacao_final" to Schema.string(description = "Próximo passo prático ou alternativa de local/hortaliça")
        )
    )

    private val model by lazy {
        Firebase.ai.generativeModel(
            modelName = MODEL_NAME,
            generationConfig = generationConfig {
                responseMimeType = "application/json"
                responseSchema = plantingGuideSchema
                temperature = 0.3f
                maxOutputTokens = 2048
                thinkingConfig = thinkingConfig {
                    thinkingLevel = ThinkingLevel.MINIMAL
                }
            },
            systemInstruction = content {
                text(systemPrompt)
            }
        )
    }

    private val visionModel by lazy {
        Firebase.ai.generativeModel(
            modelName = MODEL_NAME,
            generationConfig = generationConfig {
                responseMimeType = "application/json"
                responseSchema = spotAnalysisSchema
                temperature = 0.25f
                maxOutputTokens = 1024
                thinkingConfig = thinkingConfig {
                    thinkingLevel = ThinkingLevel.MINIMAL
                }
            },
            systemInstruction = content {
                text(visionSystemPrompt)
            }
        )
    }

    suspend fun generatePlantingGuide(request: GuideRequest): Result<PlantingGuide> {
        val prompt = buildString {
            appendLine("Gere o guia de plantio em português do Brasil (respostas curtas e práticas, 1 a 2 frases por campo):")
            appendLine("Hortaliça: ${request.hortalica.trim()}")
            appendLine("Local: ${request.cidade.trim()} - ${request.estadoNome} (${request.estadoUf})")
            appendLine("Mês de plantio: ${request.mesPlantio}")
            appendLine("Local de cultivo: ${request.localCultivo.label} (${request.localCultivo.descricaoCurta})")
        }

        return withContext(Dispatchers.IO) {
            try {
                val jsonBuilder = StringBuilder()
                withTimeout(TIMEOUT_MS) {
                    model.generateContentStream(prompt).collect { chunk ->
                        chunk.text?.let { jsonBuilder.append(it) }
                    }
                }
                val rawJson = jsonBuilder.toString().trim()
                if (rawJson.isEmpty()) {
                    return@withContext Result.failure(
                        IllegalStateException("A IA retornou uma resposta vazia. Tente novamente.")
                    )
                }
                val guide = parseGuideJson(rawJson, request)
                Result.success(guide)
            } catch (e: TimeoutCancellationException) {
                Log.e(TAG, "Gemini request timed out after ${TIMEOUT_MS}ms", e)
                Result.failure(
                    IllegalStateException("A geração do guia demorou mais de 60 segundos. Verifique sua conexão e tente novamente.")
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Failed to generate planting guide", e)
                Result.failure(
                    IllegalStateException(
                        e.localizedMessage ?: "Não foi possível gerar o guia de plantio no momento."
                    )
                )
            }
        }
    }

    suspend fun analyzePlantingSpot(
        photo: Bitmap,
        request: GuideRequest
    ): Result<SpotAnalysisResult> {
        val prompt = buildString {
            appendLine("Analise esta foto do local onde o usuário quer plantar a hortaliça escolhida:")
            appendLine("- Hortaliça escolhida: ${request.hortalica.trim()}")
            appendLine("- Localização do usuário: ${request.cidade.trim()} - ${request.estadoNome} (${request.estadoUf})")
            appendLine("- Mês previsto do plantio: ${request.mesPlantio}")
            appendLine("- Tipo de cultivo pretendido: ${request.localCultivo.label} (${request.localCultivo.descricaoCurta})")
            appendLine()
            appendLine("Avalie rigorosamente o que aparece na imagem em relação às necessidades de sol, espaço/profundidade, solo/recipiente, drenagem e ventilação dessa hortaliça. Classifique como VERDE, AMARELO ou VERMELHO.")
        }

        return withContext(Dispatchers.IO) {
            try {
                val jsonBuilder = StringBuilder()
                withTimeout(TIMEOUT_MS) {
                    val multimodalPrompt = content {
                        image(photo)
                        text(prompt)
                    }
                    visionModel.generateContentStream(multimodalPrompt).collect { chunk ->
                        chunk.text?.let { jsonBuilder.append(it) }
                    }
                }
                val rawJson = jsonBuilder.toString().trim()
                if (rawJson.isEmpty()) {
                    return@withContext Result.failure(
                        IllegalStateException("A IA não retornou análise para a foto. Tente novamente.")
                    )
                }
                val result = parseSpotAnalysisJson(rawJson, request.hortalica.trim())
                Result.success(result)
            } catch (e: TimeoutCancellationException) {
                Log.e(TAG, "Gemini vision analysis timed out after ${TIMEOUT_MS}ms", e)
                Result.failure(
                    IllegalStateException("A análise da foto demorou mais de 60 segundos. Tente novamente com boa conexão.")
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Failed to analyze planting spot photo", e)
                Result.failure(
                    IllegalStateException(
                        e.localizedMessage ?: "Não foi possível analisar a foto no momento."
                    )
                )
            }
        }
    }

    private fun parseSpotAnalysisJson(jsonString: String, hortalica: String): SpotAnalysisResult {
        val root = JSONObject(jsonString)
        val cor = SpotVerdictColor.fromRaw(root.optString("cor", "AMARELO"))
        return SpotAnalysisResult(
            hortalica = hortalica,
            cor = cor,
            tituloVeredito = root.optString("titulo_veredito", cor.tituloPadrao).ifBlank { cor.tituloPadrao },
            resumo = root.optString("resumo", "Análise concluída para $hortalica.").ifBlank { "Análise concluída." },
            luzAvaliacao = root.optString("luz_avaliacao", "Avalie a incidência de luz solar direta no período da manhã.").ifBlank { "Verifique a incidência solar." },
            espacoSoloAvaliacao = root.optString("espaco_solo_avaliacao", "Verifique profundidade, espaço para raízes e drenagem.").ifBlank { "Verifique espaço e solo." },
            oQueEstaBom = root.optString("o_que_esta_bom", "Espaço observado na imagem.").ifBlank { "—" },
            oQueMelhorar = root.optString("o_que_melhorar", "Ajuste substrato e luminosidade conforme o guia.").ifBlank { "Nenhum ajuste crítico identificado." },
            recomendacaoFinal = root.optString("recomendacao_final", "Consulte a EMATER local em caso de dúvidas sobre o solo.").ifBlank { "Siga as orientações do guia de plantio." }
        )
    }

    private fun parseGuideJson(jsonString: String, request: GuideRequest): PlantingGuide {
        val root = JSONObject(jsonString)
        val soloObj = root.getJSONObject("solo")
        val climaObj = root.getJSONObject("clima")
        val irrigacaoObj = root.getJSONObject("irrigacao")
        val cicloObj = root.getJSONObject("ciclo")
        val inicioObj = cicloObj.getJSONObject("inicio")
        val meioObj = cicloObj.getJSONObject("meio")
        val fimObj = cicloObj.getJSONObject("fim")

        val diasInicio = inicioObj.optInt("dias", 15).coerceIn(1, 365)
        val diasMeio = meioObj.optInt("dias", 25).coerceIn(1, 365)
        val diasFim = fimObj.optInt("dias", 15).coerceIn(1, 365)
        val totalDias = cicloObj.optInt("total_dias", diasInicio + diasMeio + diasFim)
            .coerceAtLeast(diasInicio + diasMeio + diasFim)
            .coerceIn(1, 730)

        return PlantingGuide(
            hortalica = root.optString("hortalica", request.hortalica).ifBlank { request.hortalica },
            estado = request.estadoUf,
            cidade = request.cidade.trim(),
            mesPlantio = request.mesPlantio,
            localCultivo = request.localCultivo.label,
            adequacao = AdequacaoBadge.fromRaw(root.optString("adequacao", "Ideal")),
            vegetacaoBioma = root.optString("vegetacao_bioma", "Bioma brasileiro").ifBlank { "Bioma regional" },
            alternativas = root.optString("alternativas", "").trim(),
            solo = SoloInfo(
                tipo = soloObj.optString("tipo", "Solo fértil e bem drenado").ifBlank { "Solo fértil e bem drenado" },
                ph = soloObj.optString("ph", "6,0 a 6,8").ifBlank { "6,0 a 6,8" },
                adubacao = soloObj.optString("adubacao", "Composto orgânico e húmus de minhoca").ifBlank { "Composto orgânico" }
            ),
            clima = ClimaInfo(
                tempMin = climaObj.optString("temp_min", "16°C").ifBlank { "16°C" },
                tempMax = climaObj.optString("temp_max", "28°C").ifBlank { "28°C" },
                luz = climaObj.optString("luz", "4 a 6 horas de sol por dia").ifBlank { "Sol pleno" },
                tolerancia = climaObj.optString("tolerancia", "").trim(),
                melhoresMeses = climaObj.optString("melhores_meses", "Ano todo com cuidados").ifBlank { "Ano todo" }
            ),
            irrigacao = IrrigacaoInfo(
                faseInicio = irrigacaoObj.optString("fase_inicio", "Manter solo levemente úmido todos os dias").ifBlank { "Diária leve" },
                faseMeio = irrigacaoObj.optString("fase_meio", "Regar 1 vez ao dia na base da planta").ifBlank { "1x ao dia" },
                faseFim = irrigacaoObj.optString("fase_fim", "Reduzir levemente sem deixar secar").ifBlank { "Moderada" },
                horario = irrigacaoObj.optString("horario", "Início da manhã ou fim da tarde").ifBlank { "Início da manhã" },
                intervaloDiasSugerido = irrigacaoObj.optInt("intervalo_dias_sugerido", 1).coerceIn(1, 14)
            ),
            ciclo = CicloInfo(
                inicio = FaseCiclo(
                    dias = diasInicio,
                    cuidados = inicioObj.optString("cuidados", "Germinação e cuidados iniciais com a muda.").ifBlank { "Cuidados iniciais" }
                ),
                meio = FaseCiclo(
                    dias = diasMeio,
                    cuidados = meioObj.optString("cuidados", "Adubação de cobertura e monitoramento de pragas.").ifBlank { "Crescimento vegetativo" }
                ),
                fim = FaseCiclo(
                    dias = diasFim,
                    cuidados = fimObj.optString("cuidados", "Acompanhar maturação e colher no ponto ideal.").ifBlank { "Colheita" }
                ),
                totalDias = totalDias
            ),
            espacamento = root.optString("espacamento", "").trim(),
            consorcio = root.optString("consorcio", "Consulte associações favoráveis para hortaliças.").ifBlank { "Consórcio diverso" },
            pragas = root.optString("pragas", "Monitorar pulgões e lagartas; usar controle orgânico.").ifBlank { "Monitoramento preventivo" },
            dicas = root.optString("dicas", "Estimativa regional. Consulte a EMATER local para análises específicas.").ifBlank { "Consulte a EMATER local." }
        )
    }
}
