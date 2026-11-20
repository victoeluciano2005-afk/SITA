package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CalendarToday
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Eco
import androidx.compose.material.icons.rounded.EditNotifications
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.NotificationsOff
import androidx.compose.material.icons.rounded.Spa
import androidx.compose.material.icons.rounded.WaterDrop
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.AdequacaoBadge
import com.example.data.GardenPlant
import com.example.data.PlantStatus
import com.example.data.UiState
import com.example.ui.components.AdequacaoBadgePill
import com.example.ui.components.PlantToGardenDialog
import com.example.ui.theme.LocalBotanicalColors
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

private enum class HortaFilter(val label: String) {
    ATIVAS("Em cultivo"),
    REGAR_HOJE("Regar hoje"),
    COLHIDAS("Colhidas")
}

@Composable
fun MinhaHortaScreen(
    gardenState: UiState<List<GardenPlant>>,
    onLogWatering: (GardenPlant) -> Unit,
    onUpdateCare: (plantId: String, interval: Int, label: String, enabled: Boolean, time: String, notes: String) -> Unit,
    onMarkHarvested: (GardenPlant) -> Unit,
    onDeletePlant: (GardenPlant) -> Unit,
    onGoToGuide: () -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedFilter by remember { mutableStateOf(HortaFilter.ATIVAS) }
    var selectedDayOffset by remember { mutableIntStateOf(0) }
    var editingPlant by remember { mutableStateOf<GardenPlant?>(null) }
    var confirmingHarvest by remember { mutableStateOf<GardenPlant?>(null) }
    var confirmingDelete by remember { mutableStateOf<GardenPlant?>(null) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        when (gardenState) {
            is UiState.Idle,
            is UiState.Loading -> {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            }

            is UiState.Error -> {
                Column(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Erro ao carregar Minha Horta",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = gardenState.message,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error,
                        textAlign = TextAlign.Center
                    )
                }
            }

            is UiState.Success -> {
                val allPlants = gardenState.data
                val activePlants = remember(allPlants) {
                    allPlants.filter { it.parsedStatus() == PlantStatus.ACTIVE }
                }
                val harvestedPlants = remember(allPlants) {
                    allPlants.filter { it.parsedStatus() == PlantStatus.HARVESTED }
                }
                val dueTodayPlants = remember(activePlants) {
                    activePlants.filter { it.needsWaterToday() }
                }

                if (allPlants.isEmpty()) {
                    EmptyHortaView(
                        onGoToGuide = onGoToGuide,
                        modifier = Modifier.align(Alignment.Center)
                    )
                } else {
                    val displayedPlants = when (selectedFilter) {
                        HortaFilter.ATIVAS -> activePlants
                        HortaFilter.REGAR_HOJE -> dueTodayPlants
                        HortaFilter.COLHIDAS -> harvestedPlants
                    }

                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .widthIn(max = 680.dp)
                            .align(Alignment.TopCenter),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        // Dashboard summary metrics
                        item {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                HortaMetricTile(
                                    value = activePlants.size.toString(),
                                    label = "Em cultivo",
                                    highlighted = false,
                                    modifier = Modifier.weight(1f)
                                )
                                HortaMetricTile(
                                    value = dueTodayPlants.size.toString(),
                                    label = "Regar hoje",
                                    highlighted = dueTodayPlants.isNotEmpty(),
                                    modifier = Modifier.weight(1f)
                                )
                                HortaMetricTile(
                                    value = harvestedPlants.size.toString(),
                                    label = "Colhidas",
                                    highlighted = false,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }

                        // 7-day Watering Calendar Strip
                        if (activePlants.isNotEmpty()) {
                            item {
                                WateringCalendarCard(
                                    activePlants = activePlants,
                                    selectedDayOffset = selectedDayOffset,
                                    onSelectDay = { selectedDayOffset = it }
                                )
                            }
                        }

                        // Filter chips
                        item {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                HortaFilter.entries.forEach { filter ->
                                    val count = when (filter) {
                                        HortaFilter.ATIVAS -> activePlants.size
                                        HortaFilter.REGAR_HOJE -> dueTodayPlants.size
                                        HortaFilter.COLHIDAS -> harvestedPlants.size
                                    }
                                    FilterChip(
                                        selected = selectedFilter == filter,
                                        onClick = { selectedFilter = filter },
                                        label = { Text("${filter.label} ($count)") },
                                        modifier = Modifier.testTag("horta_filter_${filter.name.lowercase()}")
                                    )
                                }
                            }
                        }

                        if (displayedPlants.isEmpty()) {
                            item {
                                Surface(
                                    shape = RoundedCornerShape(18.dp),
                                    color = MaterialTheme.colorScheme.surface,
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = when (selectedFilter) {
                                            HortaFilter.REGAR_HOJE -> "Todas as plantas ativas estão regadas! Bom trabalho."
                                            HortaFilter.COLHIDAS -> "Nenhuma colheita registrada ainda."
                                            HortaFilter.ATIVAS -> "Nenhuma planta em cultivo no momento."
                                        },
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier.padding(24.dp)
                                    )
                                }
                            }
                        } else {
                            items(displayedPlants, key = { it.id }) { plant ->
                                GardenPlantCard(
                                    plant = plant,
                                    onWaterNow = { onLogWatering(plant) },
                                    onEditCare = { editingPlant = plant },
                                    onMarkHarvested = { confirmingHarvest = plant },
                                    onDelete = { confirmingDelete = plant }
                                )
                            }
                        }

                        item { Spacer(modifier = Modifier.height(16.dp)) }
                    }
                }
            }
        }
    }

    editingPlant?.let { plant ->
        PlantToGardenDialog(
            hortalica = plant.hortalica,
            initialIntervalDays = plant.wateringIntervalDays.toInt(),
            initialReminderEnabled = plant.reminderEnabled,
            initialReminderTime = plant.reminderTime,
            initialNotes = plant.notes,
            confirmLabel = "Salvar alterações",
            onDismiss = { editingPlant = null },
            onConfirm = { interval, enabled, time, notes ->
                onUpdateCare(
                    plant.id,
                    interval,
                    plant.wateringScheduleLabel,
                    enabled,
                    time,
                    notes
                )
                editingPlant = null
            }
        )
    }

    confirmingHarvest?.let { plant ->
        AlertDialog(
            onDismissRequest = { confirmingHarvest = null },
            title = { Text("Registrar colheita?") },
            text = {
                Text("Marcar ${plant.hortalica} como colhida encerra o ciclo ativo e salva o histórico na aba Colhidas.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        onMarkHarvested(plant)
                        confirmingHarvest = null
                    },
                    modifier = Modifier.testTag("confirm_harvest_button")
                ) {
                    Text("Marcar colhida")
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmingHarvest = null }) {
                    Text("Cancelar")
                }
            }
        )
    }

    confirmingDelete?.let { plant ->
        AlertDialog(
            onDismissRequest = { confirmingDelete = null },
            title = { Text("Remover da Minha Horta?") },
            text = { Text("Deseja excluir ${plant.hortalica} da sua lista de cultivo?") },
            confirmButton = {
                Button(
                    onClick = {
                        onDeletePlant(plant)
                        confirmingDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Remover")
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmingDelete = null }) {
                    Text("Cancelar")
                }
            }
        )
    }
}

@Composable
private fun HortaMetricTile(
    value: String,
    label: String,
    highlighted: Boolean,
    modifier: Modifier = Modifier
) {
    val botanical = LocalBotanicalColors.current
    val bg = if (highlighted) botanical.waterContainer else MaterialTheme.colorScheme.surface
    val fg = if (highlighted) botanical.waterAccent else MaterialTheme.colorScheme.onSurface

    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(18.dp),
        color = bg,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(
            modifier = Modifier.padding(vertical = 14.dp, horizontal = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = fg
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = if (highlighted) fg else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun WateringCalendarCard(
    activePlants: List<GardenPlant>,
    selectedDayOffset: Int,
    onSelectDay: (Int) -> Unit
) {
    val botanical = LocalBotanicalColors.current
    val localeBr = remember { Locale.forLanguageTag("pt-BR") }
    val weekdayFmt = remember { SimpleDateFormat("EEE", localeBr) }
    val dayFmt = remember { SimpleDateFormat("dd", localeBr) }

    val days = remember(activePlants) {
        val now = System.currentTimeMillis()
        (0..6).map { offset ->
            val cal = Calendar.getInstance().apply {
                timeInMillis = now
                add(Calendar.DAY_OF_YEAR, offset)
            }
            val duePlants = activePlants.filter { plant ->
                val daysUntil = plant.daysUntilNextWatering(now).coerceAtLeast(0)
                val interval = plant.wateringIntervalDays.toInt().coerceIn(1, 14)
                if (offset < daysUntil) {
                    false
                } else {
                    (offset - daysUntil) % interval == 0
                }
            }
            Triple(
                offset,
                weekdayFmt.format(cal.time).replaceFirstChar { it.uppercase() }.take(3) to dayFmt.format(cal.time),
                duePlants
            )
        }
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.CalendarToday,
                    contentDescription = null,
                    tint = botanical.waterAccent,
                    modifier = Modifier.size(18.dp)
                )
                Text(
                    text = "Calendário de Rega (7 dias)",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }

            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(days) { (offset, labels, dueList) ->
                    val selected = offset == selectedDayOffset
                    Surface(
                        onClick = { onSelectDay(offset) },
                        shape = RoundedCornerShape(14.dp),
                        color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                        modifier = Modifier.width(62.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(vertical = 10.dp, horizontal = 6.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = if (offset == 0) "Hoje" else labels.first,
                                style = MaterialTheme.typography.labelSmall,
                                color = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = labels.second,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                            )
                            Box(
                                modifier = Modifier
                                    .size(7.dp)
                                    .clip(CircleShape)
                                    .background(
                                        when {
                                            dueList.isEmpty() -> MaterialTheme.colorScheme.outlineVariant
                                            selected -> MaterialTheme.colorScheme.onPrimary
                                            else -> botanical.waterAccent
                                        }
                                    )
                            )
                        }
                    }
                }
            }

            val selectedDue = days.getOrNull(selectedDayOffset)?.third.orEmpty()
            Text(
                text = if (selectedDue.isEmpty()) {
                    "Nenhuma rega prevista para este dia."
                } else {
                    "Regar (${selectedDue.size}): ${selectedDue.joinToString(", ") { "${it.hortalica} (${it.reminderTime})" }}"
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun GardenPlantCard(
    plant: GardenPlant,
    onWaterNow: () -> Unit,
    onEditCare: () -> Unit,
    onMarkHarvested: () -> Unit,
    onDelete: () -> Unit
) {
    val botanical = LocalBotanicalColors.current
    val isActive = plant.parsedStatus() == PlantStatus.ACTIVE
    val needsWater = plant.needsWaterToday()
    val daysPlanted = plant.daysSincePlanted()
    val dateFmt = remember { SimpleDateFormat("dd/MM 'às' HH:mm", Locale.forLanguageTag("pt-BR")) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("garden_plant_card_${plant.id}"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = plant.hortalica,
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "${plant.localCultivo} • ${plant.cidadeEstado}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (isActive) {
                        IconButton(
                            onClick = onEditCare,
                            modifier = Modifier.testTag("edit_care_button_${plant.id}")
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.EditNotifications,
                                contentDescription = "Editar lembrete e cuidados",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier.testTag("delete_plant_button_${plant.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.DeleteOutline,
                            contentDescription = "Remover planta",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Status + Watering + Reminder badges
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                AdequacaoBadgePill(adequacao = AdequacaoBadge.fromRaw(plant.adequacao))

                Surface(
                    shape = RoundedCornerShape(50),
                    color = if (plant.reminderEnabled) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surfaceVariant
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = if (plant.reminderEnabled) Icons.Rounded.NotificationsActive else Icons.Rounded.NotificationsOff,
                            contentDescription = null,
                            tint = if (plant.reminderEnabled) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = if (plant.reminderEnabled) {
                                "A cada ${plant.wateringIntervalDays}d • ${plant.reminderTime}"
                            } else {
                                "Lembrete pausado"
                            },
                            style = MaterialTheme.typography.labelSmall,
                            color = if (plant.reminderEnabled) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Growth cycle progress
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = plant.currentStageLabel(),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "Dia $daysPlanted de ${plant.totalDias}",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                LinearProgressIndicator(
                    progress = { plant.progressFraction() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(8.dp)),
                    color = if (isActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                )
            }

            // Watering status info
            if (isActive) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = if (needsWater) botanical.waterContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.WaterDrop,
                            contentDescription = null,
                            tint = if (needsWater) botanical.waterAccent else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (needsWater) {
                                    "Precisa de água hoje"
                                } else {
                                    val daysNext = plant.daysUntilNextWatering()
                                    "Próxima rega em $daysNext dia(s)"
                                },
                                style = MaterialTheme.typography.labelLarge,
                                color = if (needsWater) botanical.waterAccent else MaterialTheme.colorScheme.onSurface,
                                fontWeight = FontWeight.SemiBold
                            )
                            val lastWateredText = if (plant.lastWateredEpochMillis > 0L) {
                                "Última rega: ${dateFmt.format(Date(plant.lastWateredEpochMillis))} (${plant.wateredCount}x)"
                            } else {
                                plant.wateringScheduleLabel
                            }
                            Text(
                                text = lastWateredText,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            if (plant.notes.isNotBlank()) {
                Text(
                    text = "Nota: ${plant.notes}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Action Buttons for Active Plants
            if (isActive) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = onWaterNow,
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = botanical.waterAccent,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("water_now_button_${plant.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.WaterDrop,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(stringResource(id = R.string.btn_regar_agora))
                    }
                    OutlinedButton(
                        onClick = onMarkHarvested,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .height(48.dp)
                            .testTag("harvest_button_${plant.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.CheckCircle,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Colher")
                    }
                }
            } else {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Eco,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = "Colhida com sucesso • ${plant.wateredCount} regas registradas",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyHortaView(
    onGoToGuide: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .padding(28.dp)
            .widthIn(max = 380.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Image(
            painter = painterResource(id = R.drawable.img_empty_garden),
            contentDescription = stringResource(id = R.string.cd_empty_garden),
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxWidth()
                .height(190.dp)
                .clip(RoundedCornerShape(24.dp))
        )
        Text(
            text = "Sua horta ainda está vazia",
            style = MaterialTheme.typography.headlineSmall,
            textAlign = TextAlign.Center
        )
        Text(
            text = "Consulte qualquer hortaliça na aba Guia e toque em \"Plantar na Minha Horta\" para acompanhar as fases e o calendário de rega.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        FilledTonalButton(
            onClick = onGoToGuide,
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier
                .height(48.dp)
                .testTag("empty_horta_go_guide_button")
        ) {
            Icon(
                imageVector = Icons.Rounded.Spa,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text("Abrir guia de plantio")
        }
    }
}
