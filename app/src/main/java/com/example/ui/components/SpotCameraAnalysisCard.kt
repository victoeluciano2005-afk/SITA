package com.example.ui.components

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Block
import androidx.compose.material.icons.rounded.CameraAlt
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.PhotoLibrary
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.example.data.PhotoHelper
import com.example.data.SpotAnalysisResult
import com.example.data.SpotVerdictColor
import com.example.data.UiState

private val VerdictGreenAccent = Color(0xFF1E8449)
private val VerdictGreenSoft = Color(0xFFD8F3DC)
private val VerdictGreenOnSoft = Color(0xFF114B29)

private val VerdictYellowAccent = Color(0xFFD68910)
private val VerdictYellowSoft = Color(0xFFFEF3C7)
private val VerdictYellowOnSoft = Color(0xFF6B4204)

private val VerdictRedAccent = Color(0xFFC0392B)
private val VerdictRedSoft = Color(0xFFFADBD8)
private val VerdictRedOnSoft = Color(0xFF6E170F)

@Composable
fun SpotCameraAnalysisCard(
    hortalicaNome: String,
    photoBitmap: Bitmap?,
    analysisState: UiState<SpotAnalysisResult>,
    onPhotoCaptured: (Uri) -> Unit,
    onClearAnalysis: () -> Unit,
    onMissingHortalica: () -> Unit,
    onCameraPermissionDenied: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var pendingCameraUri by remember { mutableStateOf<Uri?>(null) }

    val takePictureLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        val uri = pendingCameraUri
        if (success && uri != null) {
            onPhotoCaptured(uri)
        }
    }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            val uri = PhotoHelper.createCameraPhotoUri(context)
            pendingCameraUri = uri
            takePictureLauncher.launch(uri)
        } else {
            onCameraPermissionDenied()
        }
    }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            onPhotoCaptured(uri)
        }
    }

    fun launchCamera() {
        if (hortalicaNome.isBlank()) {
            onMissingHortalica()
            return
        }
        val hasCameraPerm = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED
        if (hasCameraPerm) {
            val uri = PhotoHelper.createCameraPhotoUri(context)
            pendingCameraUri = uri
            takePictureLauncher.launch(uri)
        } else {
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    fun launchGallery() {
        if (hortalicaNome.isBlank()) {
            onMissingHortalica()
            return
        }
        photoPickerLauncher.launch(
            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
        )
    }

    val activeResult = (analysisState as? UiState.Success)?.data
    val frameColor = when (activeResult?.cor) {
        SpotVerdictColor.VERDE -> VerdictGreenAccent
        SpotVerdictColor.AMARELO -> VerdictYellowAccent
        SpotVerdictColor.VERMELHO -> VerdictRedAccent
        null -> MaterialTheme.colorScheme.outlineVariant
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("camera_analysis_card"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(
            width = if (activeResult != null) 2.dp else 1.dp,
            color = frameColor
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Card Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.secondaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.CameraAlt,
                            contentDescription = "Análise de local pela câmera",
                            tint = MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Column {
                        Text(
                            text = "Analisar local pela câmera",
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = if (hortalicaNome.isNotBlank()) {
                                "Verifique se seu espaço é favorável para $hortalicaNome"
                            } else {
                                "Escolha uma hortaliça acima e fotografe o local"
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                if (photoBitmap != null || analysisState !is UiState.Idle) {
                    IconButton(
                        onClick = onClearAnalysis,
                        modifier = Modifier.testTag("clear_spot_analysis_button")
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Close,
                            contentDescription = "Limpar análise da foto"
                        )
                    }
                }
            }

            // Captured Photo Preview with Colored Verdict Border & Overlay
            if (photoBitmap != null) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(210.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .border(
                            width = if (activeResult != null) 4.dp else 1.dp,
                            color = frameColor,
                            shape = RoundedCornerShape(16.dp)
                        )
                ) {
                    Image(
                        bitmap = photoBitmap.asImageBitmap(),
                        contentDescription = "Foto do local de plantio",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )

                    if (analysisState is UiState.Loading) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color.Black.copy(alpha = 0.55f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(10.dp),
                                modifier = Modifier.padding(16.dp)
                            ) {
                                CircularProgressIndicator(
                                    color = Color.White,
                                    strokeWidth = 3.dp,
                                    modifier = Modifier.size(36.dp)
                                )
                                Text(
                                    text = "IA analisando luz, solo e espaço para ${hortalicaNome.ifBlank { "sua hortaliça" }}...",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = Color.White
                                )
                            }
                        }
                    }

                    if (activeResult != null) {
                        val (pillBg, pillText, pillIcon) = when (activeResult.cor) {
                            SpotVerdictColor.VERDE -> Triple(
                                VerdictGreenAccent,
                                "VERDE • Está certo",
                                Icons.Rounded.CheckCircle
                            )
                            SpotVerdictColor.AMARELO -> Triple(
                                VerdictYellowAccent,
                                "AMARELO • Pode melhorar",
                                Icons.Rounded.WarningAmber
                            )
                            SpotVerdictColor.VERMELHO -> Triple(
                                VerdictRedAccent,
                                "VERMELHO • Não é possível plantar",
                                Icons.Rounded.Block
                            )
                        }
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = pillBg,
                            modifier = Modifier
                                .align(Alignment.TopStart)
                                .padding(10.dp)
                                .testTag("spot_verdict_badge_${activeResult.cor.name.lowercase()}")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = pillIcon,
                                    contentDescription = pillText,
                                    tint = Color.White,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = pillText,
                                    style = MaterialTheme.typography.labelLarge,
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }

            // Analysis Result Details
            when (analysisState) {
                is UiState.Success -> {
                    val result = analysisState.data
                    val (bannerBg, bannerFg, accent) = when (result.cor) {
                        SpotVerdictColor.VERDE -> Triple(VerdictGreenSoft, VerdictGreenOnSoft, VerdictGreenAccent)
                        SpotVerdictColor.AMARELO -> Triple(VerdictYellowSoft, VerdictYellowOnSoft, VerdictYellowAccent)
                        SpotVerdictColor.VERMELHO -> Triple(VerdictRedSoft, VerdictRedOnSoft, VerdictRedAccent)
                    }

                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = bannerBg,
                        border = BorderStroke(1.dp, accent.copy(alpha = 0.45f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = result.tituloVeredito,
                                style = MaterialTheme.typography.titleMedium,
                                color = bannerFg,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = result.cor.descricaoCurta,
                                style = MaterialTheme.typography.labelMedium,
                                color = accent,
                                fontWeight = FontWeight.SemiBold
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = result.resumo,
                                style = MaterialTheme.typography.bodyMedium,
                                color = bannerFg
                            )
                        }
                    }

                    KeyDetailRow(label = "Luminosidade e sol", value = result.luzAvaliacao)
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    KeyDetailRow(label = "Espaço, solo e recipiente", value = result.espacoSoloAvaliacao)
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    KeyDetailRow(label = "Pontos favoráveis", value = result.oQueEstaBom)
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    KeyDetailRow(
                        label = if (result.cor == SpotVerdictColor.VERMELHO) {
                            "Por que não é recomendado neste local"
                        } else {
                            "O que pode melhorar"
                        },
                        value = result.oQueMelhorar
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    KeyDetailRow(label = "Recomendação prática", value = result.recomendacaoFinal)
                }

                is UiState.Error -> {
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = MaterialTheme.colorScheme.errorContainer,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.ErrorOutline,
                                contentDescription = "Erro na análise",
                                tint = MaterialTheme.colorScheme.onErrorContainer
                            )
                            Text(
                                text = analysisState.message,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                        }
                    }
                }

                else -> Unit
            }

            // Capture / Pick Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = { launchCamera() },
                    enabled = analysisState !is UiState.Loading,
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.secondary,
                        contentColor = MaterialTheme.colorScheme.onSecondary
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("take_spot_photo_button")
                ) {
                    Icon(
                        imageVector = Icons.Rounded.CameraAlt,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (photoBitmap != null) "Nova foto" else "Usar câmera",
                        maxLines = 1
                    )
                }

                OutlinedButton(
                    onClick = { launchGallery() },
                    enabled = analysisState !is UiState.Loading,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .height(48.dp)
                        .testTag("pick_spot_photo_button")
                ) {
                    Icon(
                        imageVector = Icons.Rounded.PhotoLibrary,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Galeria")
                }
            }
        }
    }
}
