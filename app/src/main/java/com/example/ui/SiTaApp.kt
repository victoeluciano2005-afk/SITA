package com.example.ui

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Logout
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.Spa
import androidx.compose.material.icons.outlined.Yard
import androidx.compose.material.icons.rounded.AccountCircle
import androidx.compose.material.icons.rounded.Bookmark
import androidx.compose.material.icons.rounded.CameraAlt
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Spa
import androidx.compose.material.icons.rounded.Yard
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.credentials.CredentialManager
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.R
import com.example.auth.authStateFlow
import com.example.auth.signOutUser
import com.example.data.GeminiPlantingRepository
import com.example.data.HortaRepository
import com.example.data.IbgeMunicipiosRepository
import com.example.data.PlantStatus
import com.example.data.RainfallClimateRepository
import com.example.data.UiState
import com.example.ui.screens.AuthScreen
import com.example.ui.screens.FavoritosScreen
import com.example.ui.screens.GuiaFormScreen
import com.example.ui.screens.GuiaResultScreen
import com.example.ui.screens.MinhaHortaScreen
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch

private enum class SiTaTab(
    val labelRes: Int,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val testTag: String
) {
    GUIA(R.string.nav_guia, Icons.Rounded.Spa, Icons.Outlined.Spa, "nav_tab_guia"),
    MINHA_HORTA(R.string.nav_minha_horta, Icons.Rounded.Yard, Icons.Outlined.Yard, "nav_tab_minha_horta"),
    FAVORITOS(R.string.nav_favoritos, Icons.Rounded.Bookmark, Icons.Outlined.BookmarkBorder, "nav_tab_favoritos")
}

@Composable
fun AppNavigation(auth: FirebaseAuth = Firebase.auth) {
    val currentUser by auth.authStateFlow()
        .collectAsStateWithLifecycle(initialValue = auth.currentUser)

    val user = currentUser
    if (user == null) {
        AuthScreen(onAuthSuccess = { /* Reactive authStateFlow transitions automatically */ })
    } else {
        key(user.uid) {
            SiTaAuthenticatedApp(user = user)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SiTaAuthenticatedApp(user: FirebaseUser) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember(context) { CredentialManager.create(context) }
    val snackbarHostState = remember { SnackbarHostState() }

    val viewModel: SiTaViewModel = viewModel(
        key = user.uid,
        factory = viewModelFactory {
            initializer {
                val app = checkNotNull(this[APPLICATION_KEY]) {
                    "APPLICATION_KEY missing from CreationExtras"
                }
                val databaseId = app.getString(R.string.firestore_database_id)
                val db = FirebaseFirestore.getInstance(databaseId)
                SiTaViewModel(
                    hortaRepository = HortaRepository(db),
                    geminiRepository = GeminiPlantingRepository(),
                    municipiosRepository = IbgeMunicipiosRepository(app.cacheDir),
                    rainfallRepository = RainfallClimateRepository(app.cacheDir),
                    currentUserId = user.uid
                )
            }
        }
    )

    val formState by viewModel.formState.collectAsStateWithLifecycle()
    val guideState by viewModel.guideState.collectAsStateWithLifecycle()
    val climateChartState by viewModel.climateChartState.collectAsStateWithLifecycle()
    val showingGuideResult by viewModel.showingGuideResult.collectAsStateWithLifecycle()
    val favoritesState by viewModel.favoritesState.collectAsStateWithLifecycle()
    val gardenState by viewModel.gardenState.collectAsStateWithLifecycle()
    val isMutating by viewModel.isMutating.collectAsStateWithLifecycle()
    val cameraAnalysisEnabled by viewModel.cameraAnalysisEnabled.collectAsStateWithLifecycle()
    val spotPhotoBitmap by viewModel.spotPhotoBitmap.collectAsStateWithLifecycle()
    val spotAnalysisState by viewModel.spotAnalysisState.collectAsStateWithLifecycle()

    var currentTab by rememberSaveable { mutableStateOf(SiTaTab.GUIA) }
    var showSettingsDialog by remember { mutableStateOf(false) }
    var showAccountDialog by remember { mutableStateOf(false) }
    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    val settingsCameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasCameraPermission = granted
        viewModel.emitNotice(
            if (granted) {
                "Acesso à câmera liberado! Você já pode fotografar locais de plantio."
            } else {
                "Permissão de câmera negada. Você ainda pode usar fotos da galeria."
            }
        )
    }

    LaunchedEffect(viewModel) {
        viewModel.snackbarEvents.collect { msg ->
            snackbarHostState.showSnackbar(msg)
        }
    }

    val waterDueCount = remember(gardenState) {
        (gardenState as? UiState.Success)?.data
            ?.count { it.parsedStatus() == PlantStatus.ACTIVE && it.needsWaterToday() }
            ?: 0
    }

    val currentGuideIsFavorited = remember(guideState, favoritesState) {
        val guide = (guideState as? UiState.Success)?.data ?: return@remember false
        viewModel.findMatchingFavorite(guide) != null
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val useNavigationRail = maxWidth >= 600.dp

        Scaffold(
            contentWindowInsets = WindowInsets.safeDrawing,
            snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
            topBar = {
                TopAppBar(
                    title = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(34.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primary),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Spa,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onPrimary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Column {
                                Text(
                                    text = stringResource(id = R.string.app_name),
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = stringResource(id = R.string.app_subtitle),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    },
                    actions = {
                        IconButton(
                            onClick = {
                                hasCameraPermission = ContextCompat.checkSelfPermission(
                                    context,
                                    Manifest.permission.CAMERA
                                ) == PackageManager.PERMISSION_GRANTED
                                showSettingsDialog = true
                            },
                            modifier = Modifier.testTag("settings_button")
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Settings,
                                contentDescription = "Configurações da câmera",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        IconButton(
                            onClick = { showAccountDialog = true },
                            modifier = Modifier.testTag("account_menu_button")
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.AccountCircle,
                                contentDescription = stringResource(id = R.string.cd_account),
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.background,
                        titleContentColor = MaterialTheme.colorScheme.onBackground
                    )
                )
            },
            bottomBar = {
                if (!useNavigationRail) {
                    NavigationBar(
                        containerColor = MaterialTheme.colorScheme.surface,
                        tonalElevation = 2.dp
                    ) {
                        SiTaTab.entries.forEach { tab ->
                            val selected = currentTab == tab
                            NavigationBarItem(
                                selected = selected,
                                onClick = {
                                    currentTab = tab
                                    if (tab != SiTaTab.GUIA) {
                                        viewModel.closeGuideResult()
                                    }
                                },
                                icon = {
                                    TabIconWithBadge(
                                        tab = tab,
                                        selected = selected,
                                        waterDueCount = waterDueCount
                                    )
                                },
                                label = { Text(stringResource(id = tab.labelRes)) },
                                modifier = Modifier.testTag(tab.testTag)
                            )
                        }
                    }
                }
            }
        ) { innerPadding ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                if (useNavigationRail) {
                    NavigationRail(
                        containerColor = MaterialTheme.colorScheme.surface,
                        modifier = Modifier.fillMaxHeight()
                    ) {
                        SiTaTab.entries.forEach { tab ->
                            val selected = currentTab == tab
                            NavigationRailItem(
                                selected = selected,
                                onClick = {
                                    currentTab = tab
                                    if (tab != SiTaTab.GUIA) {
                                        viewModel.closeGuideResult()
                                    }
                                },
                                icon = {
                                    TabIconWithBadge(
                                        tab = tab,
                                        selected = selected,
                                        waterDueCount = waterDueCount
                                    )
                                },
                                label = { Text(stringResource(id = tab.labelRes)) },
                                modifier = Modifier.testTag(tab.testTag)
                            )
                        }
                    }
                }

                Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
                    when (currentTab) {
                        SiTaTab.GUIA -> {
                            if (showingGuideResult) {
                                GuiaResultScreen(
                                    guideState = guideState,
                                    climateChartState = climateChartState,
                                    isFavorited = currentGuideIsFavorited,
                                    isMutating = isMutating,
                                    cameraAnalysisEnabled = cameraAnalysisEnabled,
                                    spotPhotoBitmap = spotPhotoBitmap,
                                    spotAnalysisState = spotAnalysisState,
                                    onBack = { viewModel.closeGuideResult() },
                                    onRetry = { viewModel.generatePlantingGuide() },
                                    onRetryClimateChart = { viewModel.retryClimateChart() },
                                    onToggleFavorite = { viewModel.toggleFavorite(it) },
                                    onPlantInGarden = { guide, interval, enabled, time, notes ->
                                        viewModel.plantGuideInGarden(
                                            guide = guide,
                                            wateringIntervalDays = interval,
                                            reminderEnabled = enabled,
                                            reminderTime = time,
                                            notes = notes
                                        )
                                    },
                                    onSpotPhotoCaptured = { uri, guide ->
                                        viewModel.analyzeSpotPhoto(context, uri, overrideGuide = guide)
                                    },
                                    onClearSpotAnalysis = viewModel::clearSpotAnalysis,
                                    onCameraPermissionDenied = {
                                        viewModel.emitNotice(
                                            "Permissão de câmera negada. Você pode autorizar nas Configurações ou escolher uma foto da galeria."
                                        )
                                    }
                                )
                            } else {
                                GuiaFormScreen(
                                    formState = formState,
                                    cameraAnalysisEnabled = cameraAnalysisEnabled,
                                    spotPhotoBitmap = spotPhotoBitmap,
                                    spotAnalysisState = spotAnalysisState,
                                    onHortalicaChange = viewModel::updateHortalica,
                                    onStateSelect = viewModel::updateState,
                                    onCidadeChange = viewModel::updateCidade,
                                    onRetryLoadMunicipios = viewModel::retryLoadMunicipios,
                                    onMesSelect = viewModel::updateMesPlantio,
                                    onLocalCultivoSelect = viewModel::updateLocalCultivo,
                                    onDetectLocation = { viewModel.detectDeviceLocation(context) },
                                    onLocationPermissionDenied = {
                                        scope.launch {
                                            snackbarHostState.showSnackbar(
                                                "Permissão de localização negada. Você pode escolher seu estado e cidade manualmente."
                                            )
                                        }
                                    },
                                    onSpotPhotoCaptured = { uri ->
                                        viewModel.analyzeSpotPhoto(context, uri)
                                    },
                                    onClearSpotAnalysis = viewModel::clearSpotAnalysis,
                                    onMissingHortalicaForPhoto = {
                                        viewModel.emitNotice(
                                            "Primeiro escolha ou digite a hortaliça no passo 1 para a IA avaliar o local."
                                        )
                                    },
                                    onCameraPermissionDenied = {
                                        viewModel.emitNotice(
                                            "Permissão de câmera negada. Você pode liberar nas Configurações ou usar a galeria."
                                        )
                                    },
                                    onSubmitGuide = { viewModel.generatePlantingGuide() }
                                )
                            }
                        }

                        SiTaTab.MINHA_HORTA -> {
                            MinhaHortaScreen(
                                gardenState = gardenState,
                                onLogWatering = viewModel::logWatering,
                                onUpdateCare = viewModel::updateCareSettings,
                                onMarkHarvested = viewModel::markHarvested,
                                onDeletePlant = viewModel::deletePlant,
                                onGoToGuide = {
                                    viewModel.closeGuideResult()
                                    currentTab = SiTaTab.GUIA
                                }
                            )
                        }

                        SiTaTab.FAVORITOS -> {
                            FavoritosScreen(
                                favoritesState = favoritesState,
                                onOpenFavorite = { fav ->
                                    viewModel.openSavedFavorite(fav)
                                    currentTab = SiTaTab.GUIA
                                },
                                onRemoveFavorite = viewModel::removeFavoriteById,
                                onPlantFavorite = { guide, interval, enabled, time, notes ->
                                    viewModel.plantGuideInGarden(
                                        guide = guide,
                                        wateringIntervalDays = interval,
                                        reminderEnabled = enabled,
                                        reminderTime = time,
                                        notes = notes
                                    )
                                },
                                onGoToGuide = {
                                    viewModel.closeGuideResult()
                                    currentTab = SiTaTab.GUIA
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    if (showSettingsDialog) {
        AlertDialog(
            onDismissRequest = { showSettingsDialog = false },
            icon = {
                Icon(
                    imageVector = Icons.Rounded.CameraAlt,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
            },
            title = { Text("Configurações da Câmera e IA") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Análise do local por foto",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "Permite fotografar canteiros e vasos para a IA avaliar com cores (verde, amarelo ou vermelho).",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Switch(
                            checked = cameraAnalysisEnabled,
                            onCheckedChange = { viewModel.setCameraAnalysisEnabled(it) },
                            modifier = Modifier.testTag("settings_camera_toggle")
                        )
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = if (hasCameraPermission) Icons.Rounded.CheckCircle else Icons.Rounded.CameraAlt,
                                    contentDescription = null,
                                    tint = if (hasCameraPermission) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary,
                                    modifier = Modifier.size(18.dp)
                                )
                                Text(
                                    text = if (hasCameraPermission) {
                                        "Acesso à câmera autorizado"
                                    } else {
                                        "Acesso à câmera pendente"
                                    },
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                            if (!hasCameraPermission) {
                                FilledTonalButton(
                                    onClick = {
                                        settingsCameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                                    },
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("settings_grant_camera_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.CameraAlt,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Permitir acesso à câmera")
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showSettingsDialog = false }) {
                    Text("Concluir")
                }
            }
        )
    }

    if (showAccountDialog) {
        AlertDialog(
            onDismissRequest = { showAccountDialog = false },
            icon = {
                Icon(
                    imageVector = Icons.Rounded.AccountCircle,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
            },
            title = { Text(text = user.displayName ?: "Conta Google") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    if (!user.email.isNullOrBlank()) {
                        Text(
                            text = user.email.orEmpty(),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium
                        )
                    }
                    Text(
                        text = "Seus favoritos e a Minha Horta estão salvos com segurança na sua conta Google.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showAccountDialog = false
                        signOutUser(
                            credentialManager = credentialManager,
                            scope = scope
                        )
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError
                    ),
                    modifier = Modifier.testTag("sign_out_confirm_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.Logout,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = " ${stringResource(id = R.string.btn_sair)}"
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { showAccountDialog = false }) {
                    Text("Fechar")
                }
            }
        )
    }
}

@Composable
private fun TabIconWithBadge(
    tab: SiTaTab,
    selected: Boolean,
    waterDueCount: Int
) {
    val icon = if (selected) tab.selectedIcon else tab.unselectedIcon
    if (tab == SiTaTab.MINHA_HORTA && waterDueCount > 0) {
        BadgedBox(
            badge = {
                Badge { Text(waterDueCount.coerceAtMost(99).toString()) }
            }
        ) {
            Icon(
                imageVector = icon,
                contentDescription = stringResource(id = tab.labelRes)
            )
        }
    } else {
        Icon(
            imageVector = icon,
            contentDescription = stringResource(id = tab.labelRes)
        )
    }
}
