package com.example.ui.screens

import android.graphics.Bitmap
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Bookmark
import androidx.compose.material.icons.rounded.BookmarkBorder
import androidx.compose.material.icons.rounded.BugReport
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.Eco
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.Forest
import androidx.compose.material.icons.rounded.Lightbulb
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Thermostat
import androidx.compose.material.icons.rounded.Timeline
import androidx.compose.material.icons.rounded.WaterDrop
import androidx.compose.material.icons.rounded.WbSunny
import androidx.compose.material.icons.rounded.Yard
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.AdequacaoBadge
import com.example.data.IrrigationRainfallComparison
import com.example.data.PlantingGuide
import com.example.data.SpotAnalysisResult
import com.example.data.UiState
import com.example.ui.components.AdequacaoBadgePill
import com.example.ui.components.BotanicalSectionCard
import com.example.ui.components.GrowthTimelineView
import com.example.ui.components.IrrigationRainfallChartCard
import com.example.ui.components.KeyDetailRow
import com.example.ui.components.PlantToGardenDialog
import com.example.ui.components.SpotCameraAnalysisCard
import com.example.ui.theme.LocalBotanicalColors

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun GuiaResultScreen(
    guideState: UiState<PlantingGuide>,
    climateChartState: UiState<IrrigationRainfallComparison>,
    isFavorited: Boolean,
    isMutating: Boolean,
    cameraAnalysisEnabled: Boolean,
    spotPhotoBitmap: Bitmap?,
    spotAnalysisState: UiState<SpotAnalysisResult>,
    onBack: () -> Unit,
    onRetry: () -> Unit,
    onRetryClimateChart: () -> Unit,
    onToggleFavorite: (PlantingGuide) -> Unit,
    onPlantInGarden: (PlantingGuide, Int, Boolean, String, String) -> Unit,
    onSpotPhotoCaptured: (Uri, PlantingGuide) -> Unit,
    onClearSpotAnalysis: () -> Unit,
    onCameraPermissionDenied: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler { onBack() }

    var showPlantDialog by remember { mutableStateOf(false) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        when (guideState) {
            is UiState.Idle,
            is UiState.Loading -> {
                Column(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(28.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(44.dp),
                        color = MaterialTheme.colorScheme.primary,
                        strokeWidth = 3.5.dp
                    )
                    Text(
                        text = "Preparando seu guia agronômico...",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onBackground,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = "Analisando bioma, clima regional, solo, irrigação e ciclo de colheita para a sua cidade.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }
            }

            is UiState.Error -> {
                Column(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(28.dp)
                        .widthIn(max = 420.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.ErrorOutline,
                        contentDescription = "Erro",
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(48.dp)
                    )
                    Text(
                        text = "Não foi possível gerar o guia",
                        style = MaterialTheme.typography.titleLarge,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = guideState.message,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedButton(
                            onClick = onBack,
                            modifier = Modifier.testTag("error_back_button")
                        ) {
                            Text("Voltar")
                        }
                        Button(
                            onClick = onRetry,
                            modifier = Modifier.testTag("retry_guide_button")
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Refresh,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Tentar novamente")
                        }
                    }
                }
            }

            is UiState.Success -> {
                val guide = guideState.data
                val botanical = LocalBotanicalColors.current

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                        .widthIn(max = 680.dp)
                        .align(Alignment.TopCenter),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Header back bar
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            IconButton(
                                onClick = onBack,
                                modifier = Modifier.testTag("result_back_button")
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                                    contentDescription = stringResource(id = R.string.cd_back)
                                )
                            }
                            Text(
                                text = "Guia de Plantio",
                                style = MaterialTheme.typography.titleLarge
                            )
                        }
                        FilledTonalButton(
                            onClick = { onToggleFavorite(guide) },
                            enabled = !isMutating,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.testTag("favorite_guide_button")
                        ) {
                            Icon(
                                imageVector = if (isFavorited) Icons.Rounded.Bookmark else Icons.Rounded.BookmarkBorder,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (isFavorited) {
                                    stringResource(id = R.string.btn_favoritado)
                                } else {
                                    stringResource(id = R.string.btn_favoritar)
                                }
                            )
                        }
                    }

                    // CARD 1: RESUMO
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(20.dp),
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.Top
                            ) {
                                Column(
                                    modifier = Modifier.weight(1f),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text(
                                        text = guide.hortalica,
                                        style = MaterialTheme.typography.headlineMedium,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    AdequacaoBadgePill(
                                        adequacao = guide.adequacao,
                                        modifier = Modifier.testTag("adequacao_badge")
                                    )
                                }
                                Surface(
                                    shape = RoundedCornerShape(16.dp),
                                    color = MaterialTheme.colorScheme.primaryContainer
                                ) {
                                    Column(
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Text(
                                            text = "${guide.ciclo.totalDias}d",
                                            style = MaterialTheme.typography.titleLarge,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onPrimaryContainer
                                        )
                                        Text(
                                            text = "colheita",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onPrimaryContainer
                                        )
                                    }
                                }
                            }

                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                SummaryTag(Icons.Rounded.LocationOn, "${guide.cidade} - ${guide.estado}")
                                SummaryTag(Icons.Rounded.CalendarMonth, guide.mesPlantio)
                                SummaryTag(Icons.Rounded.Yard, guide.localCultivo)
                            }

                            if (guide.alternativas.isNotBlank()) {
                                val highlightBg = if (guide.adequacao == AdequacaoBadge.NAO_RECOMENDADO) {
                                    botanical.avoidContainer
                                } else {
                                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                                }
                                val highlightFg = if (guide.adequacao == AdequacaoBadge.NAO_RECOMENDADO) {
                                    botanical.onAvoidContainer
                                } else {
                                    MaterialTheme.colorScheme.onSurface
                                }
                                Surface(
                                    shape = RoundedCornerShape(14.dp),
                                    color = highlightBg,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(
                                        modifier = Modifier.padding(12.dp),
                                        verticalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Text(
                                            text = if (guide.adequacao == AdequacaoBadge.NAO_RECOMENDADO) {
                                                "Alternativas recomendadas para sua região"
                                            } else {
                                                "Outras boas opções para este período"
                                            },
                                            style = MaterialTheme.typography.labelLarge,
                                            color = highlightFg,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                        Text(
                                            text = guide.alternativas,
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = highlightFg
                                        )
                                    }
                                }
                            }

                            Button(
                                onClick = { showPlantDialog = true },
                                enabled = !isMutating,
                                shape = RoundedCornerShape(14.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.primary,
                                    contentColor = MaterialTheme.colorScheme.onPrimary
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(50.dp)
                                    .testTag("plant_in_garden_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Eco,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = stringResource(id = R.string.btn_plantar_horta),
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    // CARD 2: SOLO E VEGETAÇÃO
                    BotanicalSectionCard(
                        title = "Solo e Vegetação",
                        subtitle = "Adequação ao bioma da região",
                        icon = Icons.Rounded.Forest,
                        iconContainerColor = MaterialTheme.colorScheme.secondaryContainer,
                        iconTint = MaterialTheme.colorScheme.onSecondaryContainer
                    ) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    text = "Bioma e Vegetação Local",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.secondary,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = guide.vegetacaoBioma,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f),
                                modifier = Modifier.weight(1f)
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Text(
                                        text = "pH Ideal",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                    Text(
                                        text = guide.solo.ph,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                }
                            }
                        }
                        KeyDetailRow(label = "Tipo de solo / substrato", value = guide.solo.tipo)
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                        KeyDetailRow(label = "Adubação recomendada", value = guide.solo.adubacao)
                    }

                    // CARD 3: CLIMA
                    BotanicalSectionCard(
                        title = "Clima",
                        subtitle = "Temperatura, luz solar e calendário",
                        icon = Icons.Rounded.WbSunny,
                        iconContainerColor = MaterialTheme.colorScheme.tertiaryContainer,
                        iconTint = MaterialTheme.colorScheme.onTertiaryContainer
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            MetricMiniCard(
                                icon = Icons.Rounded.Thermostat,
                                label = "Temp. mínima",
                                value = guide.clima.tempMin,
                                modifier = Modifier.weight(1f)
                            )
                            MetricMiniCard(
                                icon = Icons.Rounded.Thermostat,
                                label = "Temp. máxima",
                                value = guide.clima.tempMax,
                                modifier = Modifier.weight(1f)
                            )
                        }
                        KeyDetailRow(label = "Exigência de luz solar", value = guide.clima.luz)
                        if (guide.clima.tolerancia.isNotBlank()) {
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                            KeyDetailRow(
                                label = "Tolerância (chuva, calor, frio) e análise do mês",
                                value = guide.clima.tolerancia
                            )
                        }
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                        KeyDetailRow(label = "Melhores meses na região", value = guide.clima.melhoresMeses)
                    }

                    // CARD 4: IRRIGAÇÃO
                    BotanicalSectionCard(
                        title = "Irrigação",
                        subtitle = "Rega por fase e ajuste climático",
                        icon = Icons.Rounded.WaterDrop,
                        iconContainerColor = botanical.waterContainer,
                        iconTint = botanical.waterAccent
                    ) {
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = botanical.waterContainer.copy(alpha = 0.65f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    text = "Melhor horário e clima local",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = botanical.waterAccent,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = guide.irrigacao.horario,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                        KeyDetailRow(label = "Fase inicial (germinação / muda)", value = guide.irrigacao.faseInicio)
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                        KeyDetailRow(label = "Fase intermediária (crescimento)", value = guide.irrigacao.faseMeio)
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                        KeyDetailRow(label = "Fase final (colheita)", value = guide.irrigacao.faseFim)
                    }

                    // GRÁFICO INTERATIVO: FREQUÊNCIA DE IRRIGAÇÃO IDEAL VS. HISTÓRICO DE CHUVAS
                    IrrigationRainfallChartCard(
                        climateState = climateChartState,
                        onRetry = onRetryClimateChart
                    )

                    // CARD 5: LINHA DO TEMPO (INÍCIO -> MEIO -> FIM)
                    BotanicalSectionCard(
                        title = "Linha do Tempo",
                        subtitle = "Do plantio à colheita em 3 fases",
                        icon = Icons.Rounded.Timeline,
                        iconContainerColor = MaterialTheme.colorScheme.primaryContainer,
                        iconTint = MaterialTheme.colorScheme.onPrimaryContainer
                    ) {
                        GrowthTimelineView(ciclo = guide.ciclo)
                    }

                    // CARD 6: DICAS E EXTRAS
                    BotanicalSectionCard(
                        title = "Dicas, Consórcio e Pragas",
                        subtitle = "Boas práticas e controle orgânico",
                        icon = Icons.Rounded.Lightbulb,
                        iconContainerColor = MaterialTheme.colorScheme.secondaryContainer,
                        iconTint = MaterialTheme.colorScheme.onSecondaryContainer
                    ) {
                        if (guide.espacamento.isNotBlank()) {
                            KeyDetailRow(label = "Espaçamento recomendado", value = guide.espacamento)
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                        }
                        KeyDetailRow(label = "Consórcio (companheiras e incompatíveis)", value = guide.consorcio)
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.BugReport,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.secondary,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "Pragas, doenças e controle orgânico",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.secondary,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                        Text(
                            text = guide.pragas,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                        KeyDetailRow(label = "Dicas para iniciantes e extensão rural", value = guide.dicas)
                    }

                    // CARD 7: ANÁLISE DO LOCAL PELA CÂMERA (VERDE / AMARELO / VERMELHO)
                    if (cameraAnalysisEnabled) {
                        SpotCameraAnalysisCard(
                            hortalicaNome = guide.hortalica,
                            photoBitmap = spotPhotoBitmap,
                            analysisState = spotAnalysisState,
                            onPhotoCaptured = { uri -> onSpotPhotoCaptured(uri, guide) },
                            onClearAnalysis = onClearSpotAnalysis,
                            onMissingHortalica = {},
                            onCameraPermissionDenied = onCameraPermissionDenied
                        )
                    }

                    OutlinedButton(
                        onClick = onBack,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("nova_consulta_button")
                    ) {
                        Text(stringResource(id = R.string.btn_nova_consulta))
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                }

                if (showPlantDialog) {
                    PlantToGardenDialog(
                        hortalica = guide.hortalica,
                        initialIntervalDays = guide.irrigacao.intervaloDiasSugerido,
                        onDismiss = { showPlantDialog = false },
                        onConfirm = { interval, enabled, time, notes ->
                            showPlantDialog = false
                            onPlantInGarden(guide, interval, enabled, time, notes)
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun SummaryTag(
    icon: ImageVector,
    text: String
) {
    Surface(
        shape = RoundedCornerShape(50),
        color = MaterialTheme.colorScheme.surfaceVariant
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(15.dp)
            )
            Text(
                text = text,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun MetricMiniCard(
    icon: ImageVector,
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.tertiary,
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}
