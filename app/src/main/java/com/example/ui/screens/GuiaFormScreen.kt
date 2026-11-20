package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowDropDown
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.Clear
import androidx.compose.material.icons.rounded.Eco
import androidx.compose.material.icons.rounded.Grass
import androidx.compose.material.icons.rounded.HomeWork
import androidx.compose.material.icons.rounded.LocationCity
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material.icons.rounded.MyLocation
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.ViewAgenda
import androidx.compose.material.icons.rounded.Yard
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.example.R
import com.example.data.BrazilCatalog
import com.example.data.BrazilianState
import com.example.data.IbgeMunicipiosRepository
import com.example.data.LocalCultivo
import com.example.data.SpotAnalysisResult
import com.example.data.UiState
import com.example.ui.GuiaFormState
import com.example.ui.components.MunicipioPickerDialog
import com.example.ui.components.SpotCameraAnalysisCard

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun GuiaFormScreen(
    formState: GuiaFormState,
    cameraAnalysisEnabled: Boolean,
    spotPhotoBitmap: Bitmap?,
    spotAnalysisState: UiState<SpotAnalysisResult>,
    onHortalicaChange: (String) -> Unit,
    onStateSelect: (BrazilianState) -> Unit,
    onCidadeChange: (String) -> Unit,
    onRetryLoadMunicipios: () -> Unit,
    onMesSelect: (String) -> Unit,
    onLocalCultivoSelect: (LocalCultivo) -> Unit,
    onDetectLocation: () -> Unit,
    onLocationPermissionDenied: () -> Unit,
    onSpotPhotoCaptured: (Uri) -> Unit,
    onClearSpotAnalysis: () -> Unit,
    onMissingHortalicaForPhoto: () -> Unit,
    onCameraPermissionDenied: () -> Unit,
    onSubmitGuide: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var showAutocomplete by remember { mutableStateOf(false) }
    var stateDropdownExpanded by remember { mutableStateOf(false) }
    var showMunicipioAutocomplete by remember { mutableStateOf(false) }
    var showMunicipioPickerDialog by remember { mutableStateOf(false) }

    val filteredSuggestions = remember(formState.hortalica) {
        BrazilCatalog.filterHortalicas(formState.hortalica)
    }

    val filteredMunicipioSuggestions = remember(formState.municipiosDoEstado, formState.cidade) {
        IbgeMunicipiosRepository.filterMunicipios(
            municipios = formState.municipiosDoEstado,
            query = formState.cidade,
            limit = 8
        )
    }

    val orderedMunicipioChips = remember(formState.municipiosDoEstado, formState.selectedState.capital) {
        val capital = formState.selectedState.capital
        val all = formState.municipiosDoEstado
        if (all.any { it.equals(capital, ignoreCase = true) }) {
            listOf(capital) + all.filterNot { it.equals(capital, ignoreCase = true) }
        } else {
            all
        }
    }

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            onDetectLocation()
        } else {
            onLocationPermissionDenied()
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 12.dp)
                .widthIn(max = 640.dp)
                .align(Alignment.TopCenter),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Hero Banner
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(156.dp)
                    .clip(RoundedCornerShape(24.dp))
            ) {
                Image(
                    painter = painterResource(id = R.drawable.img_hero_horta),
                    contentDescription = stringResource(id = R.string.cd_hero_banner),
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.horizontalGradient(
                                colors = listOf(
                                    Color(0xFF173A2B).copy(alpha = 0.90f),
                                    Color(0xFF173A2B).copy(alpha = 0.55f),
                                    Color.Transparent
                                )
                            )
                        )
                )
                Column(
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                        .padding(18.dp)
                        .fillMaxWidth(0.76f),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "O que vamos plantar hoje?",
                        style = MaterialTheme.typography.headlineSmall,
                        color = Color.White
                    )
                    Text(
                        text = "Recomendações adaptadas ao bioma, clima e mês da sua cidade no Brasil.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFFE6F3EA)
                    )
                }
            }

            // 1. Hortaliça com autocomplete
            FormSectionCard(
                stepLabel = "1",
                title = "Hortaliça",
                icon = Icons.Rounded.Eco
            ) {
                OutlinedTextField(
                    value = formState.hortalica,
                    onValueChange = {
                        onHortalicaChange(it)
                        showAutocomplete = true
                    },
                    label = { Text("Buscar entre ${BrazilCatalog.hortalicas.size}+ hortaliças do Brasil") },
                    placeholder = { Text("Ex.: Alface, quiabo, taioba, coentro, jiló...") },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Rounded.Search,
                            contentDescription = null
                        )
                    },
                    trailingIcon = {
                        if (formState.hortalica.isNotEmpty()) {
                            IconButton(
                                onClick = {
                                    onHortalicaChange("")
                                    showAutocomplete = false
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Clear,
                                    contentDescription = "Limpar hortaliça"
                                )
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("hortalica_input")
                )

                AnimatedVisibility(
                    visible = showAutocomplete && formState.hortalica.isNotBlank() && filteredSuggestions.isNotEmpty()
                ) {
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column {
                            filteredSuggestions.forEachIndexed { idx, item ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            onHortalicaChange(item.nome)
                                            showAutocomplete = false
                                        }
                                        .padding(horizontal = 14.dp, vertical = 12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = item.nome,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Medium,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = item.categoria,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                                if (idx < filteredSuggestions.lastIndex) {
                                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                                }
                            }
                        }
                    }
                }

                Text(
                    text = "Mais procuradas",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    BrazilCatalog.hortalicasPopulares.forEach { popular ->
                        val selected = formState.hortalica.equals(popular, ignoreCase = true)
                        FilterChip(
                            selected = selected,
                            onClick = {
                                onHortalicaChange(popular)
                                showAutocomplete = false
                            },
                            label = { Text(popular) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                            ),
                            modifier = Modifier.testTag("chip_veg_${BrazilCatalog.normalizeForSearch(popular)}")
                        )
                    }
                }
            }

            // 2. Localização (Estado, Cidade e GPS)
            FormSectionCard(
                stepLabel = "2",
                title = "Localização",
                icon = Icons.Rounded.LocationOn
            ) {
                FilledTonalButton(
                    onClick = {
                        val hasPermission = ContextCompat.checkSelfPermission(
                            context,
                            Manifest.permission.ACCESS_COARSE_LOCATION
                        ) == PackageManager.PERMISSION_GRANTED
                        if (hasPermission) {
                            onDetectLocation()
                        } else {
                            locationPermissionLauncher.launch(Manifest.permission.ACCESS_COARSE_LOCATION)
                        }
                    },
                    enabled = !formState.isDetectingLocation,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("use_my_location_button")
                ) {
                    if (formState.isDetectingLocation) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Detectando localização...")
                    } else {
                        Icon(
                            imageVector = Icons.Rounded.MyLocation,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(stringResource(id = R.string.btn_usar_localizacao))
                    }
                }

                // State Dropdown
                ExposedDropdownMenuBox(
                    expanded = stateDropdownExpanded,
                    onExpandedChange = { stateDropdownExpanded = !stateDropdownExpanded }
                ) {
                    OutlinedTextField(
                        value = "${formState.selectedState.nome} (${formState.selectedState.uf})",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Estado") },
                        supportingText = {
                            Text("Bioma predominante: ${formState.selectedState.biomaPrincipal}")
                        },
                        trailingIcon = {
                            ExposedDropdownMenuDefaults.TrailingIcon(expanded = stateDropdownExpanded)
                        },
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                            .fillMaxWidth()
                            .testTag("estado_dropdown")
                    )
                    ExposedDropdownMenu(
                        expanded = stateDropdownExpanded,
                        onDismissRequest = { stateDropdownExpanded = false }
                    ) {
                        BrazilCatalog.estados.forEach { st ->
                            DropdownMenuItem(
                                text = {
                                    Column {
                                        Text(
                                            text = "${st.nome} (${st.uf})",
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Medium
                                        )
                                        Text(
                                            text = "${st.regiao} • ${st.biomaPrincipal}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                },
                                onClick = {
                                    onStateSelect(st)
                                    stateDropdownExpanded = false
                                }
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = formState.cidade,
                    onValueChange = {
                        onCidadeChange(it)
                        showMunicipioAutocomplete = true
                    },
                    label = { Text("Município") },
                    placeholder = { Text("Buscar município de ${formState.selectedState.nome}...") },
                    supportingText = {
                        val helperText = when {
                            formState.isLoadingMunicipios ->
                                "Carregando todos os municípios de ${formState.selectedState.nome}..."
                            formState.municipiosError != null ->
                                "Lista reduzida offline. Toque abaixo para carregar todos os municípios."
                            formState.municipiosDoEstado.size == 1 ->
                                "1 município oficial em ${formState.selectedState.nome} (IBGE)"
                            else ->
                                "${formState.municipiosDoEstado.size} municípios oficiais em ${formState.selectedState.nome} (IBGE)"
                        }
                        Text(helperText)
                    },
                    trailingIcon = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (formState.isLoadingMunicipios) {
                                CircularProgressIndicator(
                                    modifier = Modifier
                                        .padding(end = 8.dp)
                                        .size(18.dp),
                                    strokeWidth = 2.dp
                                )
                            } else {
                                if (formState.cidade.isNotEmpty()) {
                                    IconButton(
                                        onClick = {
                                            onCidadeChange("")
                                            showMunicipioAutocomplete = true
                                        }
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.Clear,
                                            contentDescription = "Limpar município"
                                        )
                                    }
                                }
                                IconButton(
                                    onClick = { showMunicipioPickerDialog = true },
                                    modifier = Modifier.testTag("open_municipios_icon_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.ArrowDropDown,
                                        contentDescription = "Ver todos os municípios do estado"
                                    )
                                }
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("cidade_input")
                )

                AnimatedVisibility(
                    visible = showMunicipioAutocomplete &&
                        formState.cidade.isNotBlank() &&
                        filteredMunicipioSuggestions.isNotEmpty() &&
                        !(filteredMunicipioSuggestions.size == 1 &&
                            filteredMunicipioSuggestions.first().equals(formState.cidade.trim(), ignoreCase = true))
                ) {
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column {
                            filteredMunicipioSuggestions.forEachIndexed { idx, municipio ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            onCidadeChange(municipio)
                                            showMunicipioAutocomplete = false
                                        }
                                        .padding(horizontal = 14.dp, vertical = 12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = municipio,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Medium,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = formState.selectedState.uf,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                                if (idx < filteredMunicipioSuggestions.lastIndex) {
                                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                                }
                            }
                        }
                    }
                }

                if (formState.municipiosError != null && !formState.isLoadingMunicipios) {
                    OutlinedButton(
                        onClick = onRetryLoadMunicipios,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("retry_municipios_button")
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Refresh,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Carregar todos os municípios de ${formState.selectedState.uf}")
                    }
                } else {
                    OutlinedButton(
                        onClick = { showMunicipioPickerDialog = true },
                        enabled = !formState.isLoadingMunicipios && formState.municipiosDoEstado.isNotEmpty(),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("ver_todos_municipios_button")
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.LocationCity,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        val label = if (formState.municipiosDoEstado.size == 1) {
                            "Ver município de ${formState.selectedState.uf} (1)"
                        } else {
                            "Ver todos os ${formState.municipiosDoEstado.size} municípios de ${formState.selectedState.uf}"
                        }
                        Text(label, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }

                // All municipalities of the selected state as scrollable quick chips
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(vertical = 2.dp)
                ) {
                    items(
                        items = orderedMunicipioChips,
                        key = { "${formState.selectedState.uf}_$it" }
                    ) { cityOption ->
                        val isSelected = formState.cidade.trim().equals(cityOption, ignoreCase = true)
                        FilterChip(
                            selected = isSelected,
                            onClick = {
                                onCidadeChange(cityOption)
                                showMunicipioAutocomplete = false
                            },
                            label = { Text(cityOption) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        )
                    }
                }
            }

            // 3. Mês previsto do plantio
            FormSectionCard(
                stepLabel = "3",
                title = "Mês previsto do plantio",
                icon = Icons.Rounded.CalendarMonth
            ) {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(vertical = 2.dp)
                ) {
                    items(BrazilCatalog.meses) { mes ->
                        val selected = formState.mesPlantio == mes
                        FilterChip(
                            selected = selected,
                            onClick = { onMesSelect(mes) },
                            label = { Text(mes) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primary,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                            ),
                            modifier = Modifier.testTag("mes_${BrazilCatalog.normalizeForSearch(mes)}")
                        )
                    }
                }
            }

            // 4. Local de cultivo
            FormSectionCard(
                stepLabel = "4",
                title = "Local de cultivo",
                icon = Icons.Rounded.Yard
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(IntrinsicSize.Max),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        CultivoOptionCard(
                            cultivo = LocalCultivo.CANTEIRO,
                            icon = Icons.Rounded.Grass,
                            selected = formState.localCultivo == LocalCultivo.CANTEIRO,
                            onClick = { onLocalCultivoSelect(LocalCultivo.CANTEIRO) },
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                        )
                        CultivoOptionCard(
                            cultivo = LocalCultivo.VASO,
                            icon = Icons.Rounded.Yard,
                            selected = formState.localCultivo == LocalCultivo.VASO,
                            onClick = { onLocalCultivoSelect(LocalCultivo.VASO) },
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                        )
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(IntrinsicSize.Max),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        CultivoOptionCard(
                            cultivo = LocalCultivo.HORTA_VERTICAL,
                            icon = Icons.Rounded.ViewAgenda,
                            selected = formState.localCultivo == LocalCultivo.HORTA_VERTICAL,
                            onClick = { onLocalCultivoSelect(LocalCultivo.HORTA_VERTICAL) },
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                        )
                        CultivoOptionCard(
                            cultivo = LocalCultivo.ESTUFA,
                            icon = Icons.Rounded.HomeWork,
                            selected = formState.localCultivo == LocalCultivo.ESTUFA,
                            onClick = { onLocalCultivoSelect(LocalCultivo.ESTUFA) },
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                        )
                    }
                }
            }

            // 5. Análise do local pela câmera (IA semáforo: Verde / Amarelo / Vermelho)
            if (cameraAnalysisEnabled) {
                SpotCameraAnalysisCard(
                    hortalicaNome = formState.hortalica.trim(),
                    photoBitmap = spotPhotoBitmap,
                    analysisState = spotAnalysisState,
                    onPhotoCaptured = onSpotPhotoCaptured,
                    onClearAnalysis = onClearSpotAnalysis,
                    onMissingHortalica = onMissingHortalicaForPhoto,
                    onCameraPermissionDenied = onCameraPermissionDenied
                )
            }

            // Primary Submit CTA
            Button(
                onClick = onSubmitGuide,
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(58.dp)
                    .testTag("ver_guia_button")
            ) {
                Icon(
                    imageVector = Icons.Rounded.AutoAwesome,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = stringResource(id = R.string.btn_ver_guia),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }

    if (showMunicipioPickerDialog) {
        MunicipioPickerDialog(
            state = formState.selectedState,
            municipios = formState.municipiosDoEstado,
            selectedCidade = formState.cidade,
            onSelect = { selected ->
                onCidadeChange(selected)
                showMunicipioAutocomplete = false
            },
            onDismiss = { showMunicipioPickerDialog = false }
        )
    }
}

@Composable
private fun FormSectionCard(
    stepLabel: String,
    title: String,
    icon: ImageVector,
    content: @Composable () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.primaryContainer
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = stepLabel,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            content()
        }
    }
}

@Composable
private fun CultivoOptionCard(
    cultivo: LocalCultivo,
    icon: ImageVector,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val container = if (selected) {
        MaterialTheme.colorScheme.primaryContainer
    } else {
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
    }
    val border = if (selected) {
        BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
    } else {
        BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    }

    Surface(
        onClick = onClick,
        modifier = modifier
            .heightIn(min = 96.dp)
            .testTag("cultivo_${cultivo.name.lowercase()}"),
        shape = RoundedCornerShape(16.dp),
        color = container,
        border = border
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = cultivo.label,
                tint = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(22.dp)
            )
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = cultivo.label,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = cultivo.descricaoCurta,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}
