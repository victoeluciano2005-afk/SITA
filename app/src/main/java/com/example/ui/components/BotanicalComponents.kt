package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Eco
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.ReportProblem
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material.icons.rounded.WaterDrop
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.AdequacaoBadge
import com.example.data.CicloInfo
import com.example.ui.theme.LocalBotanicalColors

@Composable
fun AdequacaoBadgePill(
    adequacao: AdequacaoBadge,
    modifier: Modifier = Modifier
) {
    val botanical = LocalBotanicalColors.current
    val (bg, fg, icon) = when (adequacao) {
        AdequacaoBadge.IDEAL -> Triple(
            botanical.idealContainer,
            botanical.onIdealContainer,
            Icons.Rounded.CheckCircle
        )
        AdequacaoBadge.POSSIVEL_COM_CUIDADOS -> Triple(
            botanical.cautionContainer,
            botanical.onCautionContainer,
            Icons.Rounded.WarningAmber
        )
        AdequacaoBadge.NAO_RECOMENDADO -> Triple(
            botanical.avoidContainer,
            botanical.onAvoidContainer,
            Icons.Rounded.ReportProblem
        )
    }

    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(50),
        color = bg
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = adequacao.label,
                tint = fg,
                modifier = Modifier.size(16.dp)
            )
            Text(
                text = adequacao.label,
                style = MaterialTheme.typography.labelLarge,
                color = fg,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
fun BotanicalSectionCard(
    title: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    iconContainerColor: Color = MaterialTheme.colorScheme.primaryContainer,
    iconTint: Color = MaterialTheme.colorScheme.onPrimaryContainer,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(iconContainerColor),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = title,
                        tint = iconTint,
                        modifier = Modifier.size(22.dp)
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    if (!subtitle.isNullOrBlank()) {
                        Text(
                            text = subtitle,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            content()
        }
    }
}

@Composable
fun KeyDetailRow(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.SemiBold
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
fun GrowthTimelineView(
    ciclo: CicloInfo,
    modifier: Modifier = Modifier
) {
    val totalSafe = (ciclo.inicio.dias + ciclo.meio.dias + ciclo.fim.dias).coerceAtLeast(1)
    val w1 = (ciclo.inicio.dias.toFloat() / totalSafe).coerceAtLeast(0.16f)
    val w2 = (ciclo.meio.dias.toFloat() / totalSafe).coerceAtLeast(0.16f)
    val w3 = (ciclo.fim.dias.toFloat() / totalSafe).coerceAtLeast(0.16f)

    val phase1Color = Color(0xFF6BA368)
    val phase2Color = MaterialTheme.colorScheme.primary
    val phase3Color = MaterialTheme.colorScheme.secondary

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Total harvest summary strip
        Surface(
            shape = RoundedCornerShape(14.dp),
            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.65f),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Schedule,
                        contentDescription = "Tempo total até a colheita",
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = "Tempo total até a colheita",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
                Text(
                    text = "~${ciclo.totalDias} dias",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }

        // Proportional visual bar (Início -> Meio -> Fim)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(14.dp)
                .clip(RoundedCornerShape(8.dp)),
            horizontalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            Box(
                modifier = Modifier
                    .weight(w1)
                    .height(14.dp)
                    .background(phase1Color)
            )
            Box(
                modifier = Modifier
                    .weight(w2)
                    .height(14.dp)
                    .background(phase2Color)
            )
            Box(
                modifier = Modifier
                    .weight(w3)
                    .height(14.dp)
                    .background(phase3Color)
            )
        }

        // 3 connected timeline phase items
        TimelinePhaseItem(
            stepNumber = "1",
            title = "Início • Germinação e Muda",
            daysLabel = "${ciclo.inicio.dias} dias",
            description = ciclo.inicio.cuidados,
            dotColor = phase1Color,
            showConnector = true
        )
        TimelinePhaseItem(
            stepNumber = "2",
            title = "Meio • Crescimento Vegetativo",
            daysLabel = "${ciclo.meio.dias} dias",
            description = ciclo.meio.cuidados,
            dotColor = phase2Color,
            showConnector = true
        )
        TimelinePhaseItem(
            stepNumber = "3",
            title = "Fim • Formação e Colheita",
            daysLabel = "${ciclo.fim.dias} dias",
            description = ciclo.fim.cuidados,
            dotColor = phase3Color,
            showConnector = false
        )
    }
}

@Composable
private fun TimelinePhaseItem(
    stepNumber: String,
    title: String,
    daysLabel: String,
    description: String,
    dotColor: Color,
    showConnector: Boolean
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(dotColor),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = stepNumber,
                    style = MaterialTheme.typography.labelMedium,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
            }
            if (showConnector) {
                Box(
                    modifier = Modifier
                        .width(2.dp)
                        .height(52.dp)
                        .background(MaterialTheme.colorScheme.outlineVariant)
                )
            }
        }
        Surface(
            modifier = Modifier.weight(1f),
            shape = RoundedCornerShape(14.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
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
                        text = title,
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f)
                    )
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = dotColor.copy(alpha = 0.18f)
                    ) {
                        Text(
                            text = daysLabel,
                            style = MaterialTheme.typography.labelMedium,
                            color = dotColor,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp)
                        )
                    }
                }
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PlantToGardenDialog(
    hortalica: String,
    initialIntervalDays: Int,
    initialReminderEnabled: Boolean = true,
    initialReminderTime: String = "07:30",
    initialNotes: String = "",
    confirmLabel: String = "Plantar agora",
    onDismiss: () -> Unit,
    onConfirm: (intervalDays: Int, reminderEnabled: Boolean, reminderTime: String, notes: String) -> Unit
) {
    var intervalDays by remember { mutableIntStateOf(initialIntervalDays.coerceIn(1, 7)) }
    var reminderEnabled by remember { mutableStateOf(initialReminderEnabled) }
    var reminderTime by remember { mutableStateOf(initialReminderTime) }
    var notes by remember { mutableStateOf(initialNotes) }

    val intervalOptions = listOf(1 to "Diária", 2 to "A cada 2 dias", 3 to "A cada 3 dias", 5 to "A cada 5 dias", 7 to "Semanal")
    val timeOptions = listOf("06:30", "07:30", "08:00", "17:30", "18:30")

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = Icons.Rounded.Eco,
                contentDescription = "Configurar plantio",
                tint = MaterialTheme.colorScheme.primary
            )
        },
        title = {
            Text(
                text = "Minha Horta: $hortalica",
                style = MaterialTheme.typography.titleLarge
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "Frequência de rega",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    intervalOptions.forEach { (days, label) ->
                        FilterChip(
                            selected = intervalDays == days,
                            onClick = { intervalDays = days },
                            label = { Text(label) },
                            leadingIcon = if (intervalDays == days) {
                                {
                                    Icon(
                                        imageVector = Icons.Rounded.WaterDrop,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            } else null
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.NotificationsActive,
                            contentDescription = "Lembrete de rega",
                            tint = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = "Lembrete de rega",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium
                        )
                    }
                    Switch(
                        checked = reminderEnabled,
                        onCheckedChange = { reminderEnabled = it },
                        modifier = Modifier.testTag("reminder_switch")
                    )
                }

                if (reminderEnabled) {
                    Text(
                        text = "Horário preferido",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        timeOptions.forEach { time ->
                            FilterChip(
                                selected = reminderTime == time,
                                onClick = { reminderTime = time },
                                label = { Text(time) }
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it.take(500) },
                    label = { Text("Anotações (opcional)") },
                    placeholder = { Text("Ex.: Vaso na varanda com sol da manhã") },
                    singleLine = false,
                    maxLines = 3,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("plant_notes_input")
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(intervalDays, reminderEnabled, reminderTime, notes) },
                modifier = Modifier.testTag("confirm_plant_button")
            ) {
                Text(confirmLabel)
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.testTag("cancel_plant_button")
            ) {
                Text("Cancelar")
            }
        }
    )
}
