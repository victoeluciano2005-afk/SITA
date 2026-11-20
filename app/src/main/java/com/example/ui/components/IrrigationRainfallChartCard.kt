package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.BarChart
import androidx.compose.material.icons.rounded.CloudQueue
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.WaterDrop
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.IrrigationRainfallComparison
import com.example.data.LocalCultivo
import com.example.data.UiState
import com.example.ui.theme.LocalBotanicalColors
import java.util.Locale
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.roundToInt

private enum class ChartRangeMode {
    SELECTED_MONTH,
    FULL_YEAR
}

private data class ChartPoint(
    val xLabel: String,
    val title: String,
    val rainMm: Float,
    val rainyDays: Int,
    val idealRegasSemana: Float,
    val isHighlightedPeriod: Boolean
)

@Composable
fun IrrigationRainfallChartCard(
    climateState: UiState<IrrigationRainfallComparison>,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    val botanical = LocalBotanicalColors.current

    BotanicalSectionCard(
        title = "Irrigação vs. Chuvas",
        subtitle = "Frequência ideal comparada ao histórico de chuvas da região",
        icon = Icons.Rounded.BarChart,
        iconContainerColor = botanical.waterContainer,
        iconTint = botanical.waterAccent,
        modifier = modifier.testTag("irrigation_rainfall_chart_card")
    ) {
        when (climateState) {
            is UiState.Idle,
            is UiState.Loading -> {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 20.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(22.dp),
                        strokeWidth = 2.5.dp,
                        color = botanical.waterAccent
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "Consultando histórico real de chuvas da região...",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            is UiState.Error -> {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.7f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.ErrorOutline,
                                contentDescription = "Erro ao carregar histórico de chuvas",
                                tint = MaterialTheme.colorScheme.onErrorContainer,
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = climateState.message,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                        }
                        OutlinedButton(
                            onClick = onRetry,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.testTag("retry_rainfall_chart_button")
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Refresh,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Tentar novamente")
                        }
                    }
                }
            }

            is UiState.Success -> {
                val data = climateState.data
                var rangeMode by remember(data) { mutableStateOf(ChartRangeMode.SELECTED_MONTH) }

                val points = remember(data, rangeMode) {
                    when (rangeMode) {
                        ChartRangeMode.SELECTED_MONTH -> data.weeklyPoints.map { pt ->
                            ChartPoint(
                                xLabel = pt.label,
                                title = pt.periodLabel,
                                rainMm = pt.rainMm,
                                rainyDays = pt.rainyDays,
                                idealRegasSemana = pt.idealRegasSemana,
                                isHighlightedPeriod = false
                            )
                        }
                        ChartRangeMode.FULL_YEAR -> data.monthlyPoints.map { pt ->
                            ChartPoint(
                                xLabel = pt.shortLabel,
                                title = if (pt.isSelectedMonth) {
                                    "${pt.fullLabel} (mês escolhido)"
                                } else {
                                    pt.fullLabel
                                },
                                rainMm = pt.rainMm,
                                rainyDays = pt.rainyDays,
                                idealRegasSemana = pt.idealRegasSemana,
                                isHighlightedPeriod = pt.isSelectedMonth
                            )
                        }
                    }
                }

                var selectedIndex by remember(data, rangeMode) {
                    val defaultIdx = if (rangeMode == ChartRangeMode.FULL_YEAR) {
                        points.indexOfFirst { it.isHighlightedPeriod }.coerceAtLeast(0)
                    } else {
                        0
                    }
                    mutableIntStateOf(defaultIdx)
                }

                LaunchedEffect(points) {
                    if (selectedIndex !in points.indices) {
                        selectedIndex = 0
                    }
                }

                // Period Toggle (Mês escolhido por semana vs 12 meses da região)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = rangeMode == ChartRangeMode.SELECTED_MONTH,
                        onClick = { rangeMode = ChartRangeMode.SELECTED_MONTH },
                        label = { Text("${data.mesPlantio} (semanas)") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                        ),
                        modifier = Modifier.testTag("chart_mode_month")
                    )
                    FilterChip(
                        selected = rangeMode == ChartRangeMode.FULL_YEAR,
                        onClick = { rangeMode = ChartRangeMode.FULL_YEAR },
                        label = { Text("Histórico anual (12m)") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                        ),
                        modifier = Modifier.testTag("chart_mode_year")
                    )
                }

                // KPI Summary Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ChartMetricTile(
                        label = "Chuva em ${data.mesPlantio}",
                        value = "${data.totalChuvaMesMm.roundToInt()} mm",
                        footnote = "${data.diasComChuvaMes} dias c/ chuva",
                        accentColor = botanical.waterAccent,
                        modifier = Modifier.weight(1f)
                    )
                    ChartMetricTile(
                        label = "Rega base ideal",
                        value = "${formatOneDecimal(data.baseRegasPorSemana)}x / sem",
                        footnote = data.localCultivo,
                        accentColor = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.weight(1f)
                    )
                }

                // Interactive Tooltip Card for the selected Bar/Point
                val activePoint = points.getOrNull(selectedIndex)
                if (activePoint != null) {
                    val advice = buildPointAdvice(
                        point = activePoint,
                        isMonthlyAggregate = rangeMode == ChartRangeMode.FULL_YEAR,
                        localCultivo = LocalCultivo.fromLabel(data.localCultivo)
                    )
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("chart_active_tooltip")
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = activePoint.title,
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "Toque nas barras",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(14.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.CloudQueue,
                                        contentDescription = null,
                                        tint = botanical.waterAccent,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Text(
                                        text = "${activePoint.rainMm.roundToInt()} mm (${activePoint.rainyDays}d chuva)",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.WaterDrop,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Text(
                                        text = "${formatOneDecimal(activePoint.idealRegasSemana)} regas/sem",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                            Text(
                                text = advice,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                // Recharts-style Composed Bar + Line Chart
                ComposedRainIrrigationCanvas(
                    points = points,
                    selectedIndex = selectedIndex,
                    onSelectIndex = { selectedIndex = it },
                    rainBarColor = botanical.waterAccent,
                    irrigationLineColor = MaterialTheme.colorScheme.primary,
                    gridColor = MaterialTheme.colorScheme.outlineVariant,
                    axisTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    surfaceColor = MaterialTheme.colorScheme.surface,
                    cursorBgColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.09f),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(232.dp)
                        .testTag("irrigation_rainfall_canvas")
                )

                // Legend
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(width = 12.dp, height = 10.dp)
                                .clip(RoundedCornerShape(3.dp))
                                .background(botanical.waterAccent)
                        )
                        Text(
                            text = if (rangeMode == ChartRangeMode.SELECTED_MONTH) {
                                "Chuva histórica (mm/sem)"
                            } else {
                                "Chuva histórica (mm/mês)"
                            },
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary)
                        )
                        Text(
                            text = "Irrigação ideal (regas/sem)",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Data source footnote
                Text(
                    text = "Base climática real (Open-Meteo ERA5 • ${data.anoReferencia}) — ${data.fonteDados}.",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun ComposedRainIrrigationCanvas(
    points: List<ChartPoint>,
    selectedIndex: Int,
    onSelectIndex: (Int) -> Unit,
    rainBarColor: Color,
    irrigationLineColor: Color,
    gridColor: Color,
    axisTextColor: Color,
    surfaceColor: Color,
    cursorBgColor: Color,
    modifier: Modifier = Modifier
) {
    if (points.isEmpty()) return

    val textMeasurer = rememberTextMeasurer()
    val axisLabelStyle = TextStyle(
        color = axisTextColor,
        fontSize = 10.sp,
        fontWeight = FontWeight.Medium
    )
    val leftAxisTitleStyle = TextStyle(
        color = rainBarColor,
        fontSize = 10.sp,
        fontWeight = FontWeight.SemiBold
    )
    val rightAxisTitleStyle = TextStyle(
        color = irrigationLineColor,
        fontSize = 10.sp,
        fontWeight = FontWeight.SemiBold
    )

    val maxRainRaw = points.maxOfOrNull { it.rainMm } ?: 0f
    val maxRainAxis = computeNiceAxisMax(max(maxRainRaw, 20f))

    val maxRegasRaw = points.maxOfOrNull { it.idealRegasSemana } ?: 7f
    val maxRegasAxis = max(7f, ceil((maxRegasRaw + 1f) / 2f) * 2f)

    Canvas(
        modifier = modifier.pointerInput(points) {
            detectTapGestures { offset ->
                val leftPadPx = 38.dp.toPx()
                val rightPadPx = 34.dp.toPx()
                val plotWidth = (size.width - leftPadPx - rightPadPx).coerceAtLeast(1f)
                val relX = (offset.x - leftPadPx).coerceIn(0f, plotWidth - 0.01f)
                val step = plotWidth / points.size
                val tappedIndex = (relX / step).toInt().coerceIn(0, points.lastIndex)
                onSelectIndex(tappedIndex)
            }
        }
    ) {
        val leftPad = 38.dp.toPx()
        val rightPad = 34.dp.toPx()
        val topPad = 24.dp.toPx()
        val bottomPad = 28.dp.toPx()

        val chartWidth = (size.width - leftPad - rightPad).coerceAtLeast(1f)
        val chartHeight = (size.height - topPad - bottomPad).coerceAtLeast(1f)

        // Axis unit titles at top corners
        drawText(
            textMeasurer = textMeasurer,
            text = "mm",
            topLeft = Offset(2.dp.toPx(), 2.dp.toPx()),
            style = leftAxisTitleStyle
        )
        val rightTitleLayout = textMeasurer.measure("x/sem", style = rightAxisTitleStyle)
        drawText(
            textLayoutResult = rightTitleLayout,
            topLeft = Offset(size.width - rightTitleLayout.size.width - 2.dp.toPx(), 2.dp.toPx())
        )

        // Dashed horizontal gridlines (4 levels: 0%, 33%, 66%, 100%)
        val dashEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 6f), 0f)
        val gridSteps = 3
        for (i in 0..gridSteps) {
            val fraction = i.toFloat() / gridSteps.toFloat()
            val y = topPad + chartHeight * (1f - fraction)

            drawLine(
                color = gridColor.copy(alpha = if (i == 0) 0.9f else 0.55f),
                start = Offset(leftPad, y),
                end = Offset(leftPad + chartWidth, y),
                strokeWidth = 1.dp.toPx(),
                pathEffect = if (i == 0) null else dashEffect
            )

            // Left Y-axis ticks (Rainfall mm)
            val rainTick = (maxRainAxis * fraction).roundToInt().toString()
            val rainLayout = textMeasurer.measure(rainTick, style = axisLabelStyle)
            drawText(
                textLayoutResult = rainLayout,
                topLeft = Offset(
                    x = (leftPad - rainLayout.size.width - 6.dp.toPx()).coerceAtLeast(0f),
                    y = y - rainLayout.size.height / 2f
                )
            )

            // Right Y-axis ticks (Ideal regas/sem)
            val regasTick = formatOneDecimal(maxRegasAxis * fraction)
            val regasLayout = textMeasurer.measure(regasTick, style = axisLabelStyle)
            drawText(
                textLayoutResult = regasLayout,
                topLeft = Offset(
                    x = leftPad + chartWidth + 6.dp.toPx(),
                    y = y - regasLayout.size.height / 2f
                )
            )
        }

        val slotWidth = chartWidth / points.size
        val barWidth = (slotWidth * (if (points.size <= 4) 0.44f else 0.58f))
            .coerceIn(8.dp.toPx(), 38.dp.toPx())

        val lineCenters = ArrayList<Offset>(points.size)

        // Draw cursor highlight, rainfall bars, and X-axis labels
        points.forEachIndexed { idx, pt ->
            val slotStartX = leftPad + idx * slotWidth
            val centerX = slotStartX + slotWidth / 2f

            if (idx == selectedIndex) {
                drawRoundRect(
                    color = cursorBgColor,
                    topLeft = Offset(slotStartX + 2.dp.toPx(), topPad),
                    size = Size((slotWidth - 4.dp.toPx()).coerceAtLeast(2f), chartHeight),
                    cornerRadius = CornerRadius(8.dp.toPx(), 8.dp.toPx())
                )
            }

            // Bar height for historical rainfall (mm)
            val rainRatio = (pt.rainMm / maxRainAxis).coerceIn(0f, 1f)
            val barHeight = max(if (pt.rainMm > 0f) 4.dp.toPx() else 0f, chartHeight * rainRatio)
            val barTop = topPad + chartHeight - barHeight

            val barAlpha = when {
                idx == selectedIndex -> 0.96f
                pt.isHighlightedPeriod -> 0.80f
                else -> 0.52f
            }

            if (barHeight > 0f) {
                drawRoundRect(
                    color = rainBarColor.copy(alpha = barAlpha),
                    topLeft = Offset(centerX - barWidth / 2f, barTop),
                    size = Size(barWidth, barHeight),
                    cornerRadius = CornerRadius(6.dp.toPx(), 6.dp.toPx())
                )
            }

            // Compute irrigation line point
            val regasRatio = (pt.idealRegasSemana / maxRegasAxis).coerceIn(0f, 1f)
            val lineY = topPad + chartHeight * (1f - regasRatio)
            lineCenters.add(Offset(centerX, lineY))

            // X-axis label (for 12 months, show every label cleanly with smaller font if tight)
            val showXLabel = points.size <= 6 || idx % 2 == 0 || idx == selectedIndex || pt.isHighlightedPeriod
            if (showXLabel) {
                val xStyle = if (idx == selectedIndex || pt.isHighlightedPeriod) {
                    axisLabelStyle.copy(
                        color = irrigationLineColor,
                        fontWeight = FontWeight.Bold
                    )
                } else {
                    axisLabelStyle
                }
                val xLayout = textMeasurer.measure(pt.xLabel, style = xStyle)
                drawText(
                    textLayoutResult = xLayout,
                    topLeft = Offset(
                        x = (centerX - xLayout.size.width / 2f)
                            .coerceIn(0f, size.width - xLayout.size.width),
                        y = topPad + chartHeight + 8.dp.toPx()
                    )
                )
            }
        }

        // Draw smooth cubic Bézier line for ideal irrigation frequency
        if (lineCenters.size >= 2) {
            val path = Path().apply {
                moveTo(lineCenters.first().x, lineCenters.first().y)
                for (i in 0 until lineCenters.lastIndex) {
                    val p0 = lineCenters[i]
                    val p1 = lineCenters[i + 1]
                    val midX = (p0.x + p1.x) / 2f
                    cubicTo(midX, p0.y, midX, p1.y, p1.x, p1.y)
                }
            }
            drawPath(
                path = path,
                color = irrigationLineColor,
                style = Stroke(
                    width = 2.6.dp.toPx(),
                    cap = StrokeCap.Round,
                    join = StrokeJoin.Round
                )
            )
        }

        // Draw Recharts-style line nodes (activeDot with outer halo)
        lineCenters.forEachIndexed { idx, center ->
            val isSelected = idx == selectedIndex
            if (isSelected) {
                drawCircle(
                    color = irrigationLineColor.copy(alpha = 0.24f),
                    radius = 10.dp.toPx(),
                    center = center
                )
            }
            drawCircle(
                color = irrigationLineColor,
                radius = if (isSelected) 5.5.dp.toPx() else 3.8.dp.toPx(),
                center = center
            )
            drawCircle(
                color = surfaceColor,
                radius = if (isSelected) 2.5.dp.toPx() else 1.6.dp.toPx(),
                center = center
            )
        }
    }
}

@Composable
private fun ChartMetricTile(
    label: String,
    value: String,
    footnote: String,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1
            )
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = accentColor
            )
            Text(
                text = footnote,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1
            )
        }
    }
}

private fun computeNiceAxisMax(rawMax: Float): Float {
    val step = when {
        rawMax <= 30f -> 10f
        rawMax <= 100f -> 20f
        rawMax <= 250f -> 50f
        else -> 100f
    }
    return ceil((rawMax * 1.15f) / step) * step
}

private fun formatOneDecimal(value: Float): String {
    val rounded = (value * 10f).roundToInt() / 10f
    return if (rounded % 1f == 0f) {
        rounded.toInt().toString()
    } else {
        String.format(Locale.forLanguageTag("pt-BR"), "%.1f", rounded)
    }
}

private fun buildPointAdvice(
    point: ChartPoint,
    isMonthlyAggregate: Boolean,
    localCultivo: LocalCultivo
): String {
    if (localCultivo == LocalCultivo.ESTUFA) {
        return "Cultivo em estufa: a cobertura bloqueia a chuva direta, portanto mantenha a frequência ideal de irrigação."
    }
    val heavyThreshold = if (isMonthlyAggregate) 150f else 35f
    val moderateThreshold = if (isMonthlyAggregate) 60f else 12f
    return when {
        point.rainMm >= heavyThreshold ->
            "Período chuvoso: reduza as regas manuais e garanta boa drenagem para evitar encharcamento."
        point.rainMm >= moderateThreshold ->
            "Chuva moderada: ajuste as regas verificando a umidade do solo com o dedo antes de molhar."
        else ->
            "Período mais seco: siga a frequência ideal de irrigação e reforce a cobertura morta (palhada)."
    }
}
