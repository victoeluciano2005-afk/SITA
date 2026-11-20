package com.example.ui

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.BrazilCatalog
import com.example.data.BrazilianState
import com.example.data.FavoriteGuide
import com.example.data.GardenPlant
import com.example.data.GeminiPlantingRepository
import com.example.data.GuideRequest
import com.example.data.HortaRepository
import com.example.data.IbgeMunicipiosRepository
import com.example.data.IrrigationRainfallComparison
import com.example.data.LocalCultivo
import com.example.data.LocationHelper
import com.example.data.PhotoHelper
import com.example.data.PlantingGuide
import com.example.data.RainfallClimateRepository
import com.example.data.SpotAnalysisResult
import com.example.data.UiState
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Calendar

data class GuiaFormState(
    val hortalica: String = "",
    val selectedState: BrazilianState = BrazilCatalog.estados.first { it.uf == "SP" },
    val cidade: String = "São Paulo",
    val municipiosDoEstado: List<String> = BrazilCatalog.estados.first { it.uf == "SP" }.cidadesComuns,
    val isLoadingMunicipios: Boolean = false,
    val municipiosError: String? = null,
    val mesPlantio: String = BrazilCatalog.meses[
        Calendar.getInstance().get(Calendar.MONTH).coerceIn(0, 11)
    ],
    val localCultivo: LocalCultivo = LocalCultivo.VASO,
    val isDetectingLocation: Boolean = false
)

class SiTaViewModel(
    private val hortaRepository: HortaRepository,
    private val geminiRepository: GeminiPlantingRepository,
    private val municipiosRepository: IbgeMunicipiosRepository,
    private val rainfallRepository: RainfallClimateRepository,
    private val currentUserId: String
) : ViewModel() {

    private val _formState = MutableStateFlow(GuiaFormState())
    val formState: StateFlow<GuiaFormState> = _formState.asStateFlow()

    private var municipiosJob: Job? = null
    private var climateChartJob: Job? = null

    init {
        loadMunicipiosForState(_formState.value.selectedState)
    }

    private val _guideState = MutableStateFlow<UiState<PlantingGuide>>(UiState.Idle)
    val guideState: StateFlow<UiState<PlantingGuide>> = _guideState.asStateFlow()

    private val _climateChartState =
        MutableStateFlow<UiState<IrrigationRainfallComparison>>(UiState.Idle)
    val climateChartState: StateFlow<UiState<IrrigationRainfallComparison>> =
        _climateChartState.asStateFlow()

    private val _showingGuideResult = MutableStateFlow(false)
    val showingGuideResult: StateFlow<Boolean> = _showingGuideResult.asStateFlow()

    private val _cameraAnalysisEnabled = MutableStateFlow(true)
    val cameraAnalysisEnabled: StateFlow<Boolean> = _cameraAnalysisEnabled.asStateFlow()

    private val _spotPhotoBitmap = MutableStateFlow<Bitmap?>(null)
    val spotPhotoBitmap: StateFlow<Bitmap?> = _spotPhotoBitmap.asStateFlow()

    private val _spotAnalysisState = MutableStateFlow<UiState<SpotAnalysisResult>>(UiState.Idle)
    val spotAnalysisState: StateFlow<UiState<SpotAnalysisResult>> = _spotAnalysisState.asStateFlow()

    private val _snackbarEvents = MutableSharedFlow<String>(extraBufferCapacity = 8)
    val snackbarEvents: SharedFlow<String> = _snackbarEvents.asSharedFlow()

    private val _isMutating = MutableStateFlow(false)
    val isMutating: StateFlow<Boolean> = _isMutating.asStateFlow()

    val favoritesState: StateFlow<UiState<List<FavoriteGuide>>> =
        hortaRepository.observeFavorites(currentUserId)
            .map<List<FavoriteGuide>, UiState<List<FavoriteGuide>>> { UiState.Success(it) }
            .catch { error ->
                val firestoreError = generateSequence(error) { it.cause }
                    .filterIsInstance<FirebaseFirestoreException>()
                    .firstOrNull()
                if (firestoreError == null && error is CancellationException) throw error
                val rootError = firestoreError ?: error
                Log.w(TAG, "Error observing favorites", rootError)
                emit(UiState.Error(rootError.message ?: "Erro ao carregar favoritos."))
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
                initialValue = UiState.Loading
            )

    val gardenState: StateFlow<UiState<List<GardenPlant>>> =
        hortaRepository.observeGardenPlants(currentUserId)
            .map<List<GardenPlant>, UiState<List<GardenPlant>>> { UiState.Success(it) }
            .catch { error ->
                val firestoreError = generateSequence(error) { it.cause }
                    .filterIsInstance<FirebaseFirestoreException>()
                    .firstOrNull()
                if (firestoreError == null && error is CancellationException) throw error
                val rootError = firestoreError ?: error
                Log.w(TAG, "Error observing garden_plants", rootError)
                emit(UiState.Error(rootError.message ?: "Erro ao carregar Minha Horta."))
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
                initialValue = UiState.Loading
            )

    fun updateHortalica(value: String) {
        _formState.update { it.copy(hortalica = value.take(80)) }
    }

    fun updateState(state: BrazilianState) {
        _formState.update { current ->
            current.copy(
                selectedState = state,
                cidade = state.capital,
                municipiosDoEstado = state.cidadesComuns,
                municipiosError = null
            )
        }
        loadMunicipiosForState(state)
    }

    fun retryLoadMunicipios() {
        loadMunicipiosForState(_formState.value.selectedState)
    }

    private fun loadMunicipiosForState(state: BrazilianState) {
        municipiosJob?.cancel()
        municipiosJob = viewModelScope.launch {
            _formState.update {
                it.copy(
                    isLoadingMunicipios = true,
                    municipiosError = null
                )
            }
            val result = municipiosRepository.getMunicipios(state)
            result.onSuccess { municipios ->
                _formState.update { current ->
                    if (current.selectedState.uf == state.uf) {
                        current.copy(
                            municipiosDoEstado = municipios,
                            isLoadingMunicipios = false,
                            municipiosError = null
                        )
                    } else {
                        current
                    }
                }
            }.onFailure { err ->
                _formState.update { current ->
                    if (current.selectedState.uf == state.uf) {
                        current.copy(
                            municipiosDoEstado = state.cidadesComuns,
                            isLoadingMunicipios = false,
                            municipiosError = err.message ?: "Erro ao carregar municípios."
                        )
                    } else {
                        current
                    }
                }
            }
        }
    }

    fun updateCidade(value: String) {
        _formState.update { it.copy(cidade = value.take(80)) }
    }

    fun updateMesPlantio(month: String) {
        _formState.update { it.copy(mesPlantio = month) }
    }

    fun updateLocalCultivo(cultivo: LocalCultivo) {
        _formState.update { it.copy(localCultivo = cultivo) }
    }

    fun setCameraAnalysisEnabled(enabled: Boolean) {
        _cameraAnalysisEnabled.value = enabled
        if (!enabled) {
            clearSpotAnalysis()
        }
        _snackbarEvents.tryEmit(
            if (enabled) "Análise de local por câmera ativada." else "Análise por câmera desativada."
        )
    }

    fun clearSpotAnalysis() {
        _spotPhotoBitmap.value = null
        _spotAnalysisState.value = UiState.Idle
    }

    fun emitNotice(message: String) {
        _snackbarEvents.tryEmit(message)
    }

    fun analyzeSpotPhoto(
        context: Context,
        uri: Uri,
        overrideGuide: PlantingGuide? = null
    ) {
        val form = _formState.value
        val hortalica = (overrideGuide?.hortalica ?: form.hortalica).trim()
        if (hortalica.isEmpty()) {
            _snackbarEvents.tryEmit("Escolha uma hortaliça antes de tirar a foto do local.")
            return
        }

        val stateObj = overrideGuide?.let { BrazilCatalog.findStateByNameOrUf(it.estado) }
            ?: form.selectedState
        val city = (overrideGuide?.cidade ?: form.cidade).trim().ifBlank { stateObj.capital }
        val month = overrideGuide?.mesPlantio ?: form.mesPlantio
        val cultivo = overrideGuide?.let { LocalCultivo.fromLabel(it.localCultivo) }
            ?: form.localCultivo

        val request = GuideRequest(
            hortalica = hortalica,
            estadoUf = stateObj.uf,
            estadoNome = stateObj.nome,
            cidade = city,
            mesPlantio = month,
            localCultivo = cultivo
        )

        viewModelScope.launch {
            _spotAnalysisState.value = UiState.Loading
            val bitmap = PhotoHelper.decodeDownsampledBitmap(context, uri, maxDimensionPx = 1024)
            if (bitmap == null) {
                _spotAnalysisState.value = UiState.Error(
                    "Não foi possível abrir a imagem selecionada. Tente tirar a foto novamente."
                )
                return@launch
            }
            _spotPhotoBitmap.value = bitmap

            val result = geminiRepository.analyzePlantingSpot(bitmap, request)
            result.onSuccess { analysis ->
                _spotAnalysisState.value = UiState.Success(analysis)
            }.onFailure { err ->
                _spotAnalysisState.value = UiState.Error(
                    err.message ?: "Não foi possível analisar a foto no momento."
                )
            }
        }
    }

    fun detectDeviceLocation(context: Context) {
        viewModelScope.launch {
            _formState.update { it.copy(isDetectingLocation = true) }
            val result = LocationHelper.detectCurrentLocation(context)
            _formState.update { it.copy(isDetectingLocation = false) }
            result.onSuccess { resolved ->
                _formState.update {
                    it.copy(
                        selectedState = resolved.state,
                        cidade = resolved.city
                    )
                }
                loadMunicipiosForState(resolved.state)
                _snackbarEvents.tryEmit("Localização definida: ${resolved.city} - ${resolved.state.uf}")
            }.onFailure { err ->
                _snackbarEvents.tryEmit(err.message ?: "Não foi possível obter sua localização.")
            }
        }
    }

    fun generatePlantingGuide() {
        val form = _formState.value
        val cleanVeg = form.hortalica.trim()
        val cleanCity = form.cidade.trim()
        if (cleanVeg.isEmpty()) {
            _snackbarEvents.tryEmit("Digite ou escolha uma hortaliça para consultar.")
            return
        }
        if (cleanCity.isEmpty()) {
            _snackbarEvents.tryEmit("Informe sua cidade para adaptar o guia ao clima local.")
            return
        }

        val request = GuideRequest(
            hortalica = cleanVeg,
            estadoUf = form.selectedState.uf,
            estadoNome = form.selectedState.nome,
            cidade = cleanCity,
            mesPlantio = form.mesPlantio,
            localCultivo = form.localCultivo
        )

        _showingGuideResult.value = true
        _guideState.value = UiState.Loading
        _climateChartState.value = UiState.Loading

        viewModelScope.launch {
            val result = geminiRepository.generatePlantingGuide(request)
            result.onSuccess { guide ->
                _guideState.value = UiState.Success(guide)
                loadClimateChartForGuide(guide)
            }.onFailure { err ->
                _guideState.value = UiState.Error(
                    err.message ?: "Erro ao gerar o guia de plantio."
                )
                _climateChartState.value = UiState.Idle
            }
        }
    }

    fun retryClimateChart() {
        val guide = (_guideState.value as? UiState.Success)?.data ?: return
        loadClimateChartForGuide(guide)
    }

    private fun loadClimateChartForGuide(guide: PlantingGuide) {
        climateChartJob?.cancel()
        climateChartJob = viewModelScope.launch {
            _climateChartState.value = UiState.Loading
            val result = rainfallRepository.getIrrigationVsRainfall(guide)
            result.onSuccess { comparison ->
                _climateChartState.value = UiState.Success(comparison)
            }.onFailure { err ->
                Log.w(TAG, "Error loading rainfall history chart", err)
                _climateChartState.value = UiState.Error(
                    err.message ?: "Não foi possível carregar o histórico de chuvas da região."
                )
            }
        }
    }

    fun openSavedFavorite(favorite: FavoriteGuide) {
        val guide = favorite.toPlantingGuide()
        _guideState.value = UiState.Success(guide)
        _showingGuideResult.value = true
        loadClimateChartForGuide(guide)
    }

    fun closeGuideResult() {
        _showingGuideResult.value = false
    }

    fun findMatchingFavorite(guide: PlantingGuide): FavoriteGuide? {
        val currentFavs = (favoritesState.value as? UiState.Success)?.data.orEmpty()
        return currentFavs.firstOrNull {
            it.hortalica.equals(guide.hortalica, ignoreCase = true) &&
                it.cidade.equals(guide.cidade, ignoreCase = true) &&
                it.estado.equals(guide.estado, ignoreCase = true) &&
                it.mesPlantio.equals(guide.mesPlantio, ignoreCase = true) &&
                it.localCultivo.equals(guide.localCultivo, ignoreCase = true)
        }
    }

    fun toggleFavorite(guide: PlantingGuide) {
        viewModelScope.launch {
            _isMutating.value = true
            val existing = findMatchingFavorite(guide)
            if (existing != null) {
                hortaRepository.removeFavorite(existing.id)
                    .onSuccess { _snackbarEvents.tryEmit("${guide.hortalica} removida dos favoritos.") }
                    .onFailure { _snackbarEvents.tryEmit("Não foi possível remover o favorito.") }
            } else {
                hortaRepository.addFavorite(guide)
                    .onSuccess { _snackbarEvents.tryEmit("${guide.hortalica} salva nos Favoritos!") }
                    .onFailure { _snackbarEvents.tryEmit("Não foi possível salvar nos favoritos.") }
            }
            _isMutating.value = false
        }
    }

    fun removeFavoriteById(favoriteId: String, name: String) {
        viewModelScope.launch {
            hortaRepository.removeFavorite(favoriteId)
                .onSuccess { _snackbarEvents.tryEmit("$name removida dos favoritos.") }
                .onFailure { _snackbarEvents.tryEmit("Erro ao remover favorito.") }
        }
    }

    fun plantGuideInGarden(
        guide: PlantingGuide,
        wateringIntervalDays: Int,
        reminderEnabled: Boolean,
        reminderTime: String,
        notes: String
    ) {
        viewModelScope.launch {
            _isMutating.value = true
            val result = hortaRepository.addGardenPlant(
                hortalica = guide.hortalica,
                localCultivo = LocalCultivo.fromLabel(guide.localCultivo),
                cidadeEstado = "${guide.cidade} - ${guide.estado}",
                adequacao = guide.adequacao,
                wateringIntervalDays = wateringIntervalDays,
                wateringScheduleLabel = guide.irrigacao.horario.ifBlank { guide.irrigacao.faseInicio },
                reminderEnabled = reminderEnabled,
                reminderTime = reminderTime,
                cicloInicioDias = guide.ciclo.inicio.dias,
                cicloMeioDias = guide.ciclo.meio.dias,
                totalDias = guide.ciclo.totalDias,
                notes = notes
            )
            _isMutating.value = false
            result.onSuccess {
                _snackbarEvents.tryEmit("${guide.hortalica} adicionada à Minha Horta!")
            }.onFailure {
                _snackbarEvents.tryEmit("Não foi possível salvar na Minha Horta.")
            }
        }
    }

    fun logWatering(plant: GardenPlant) {
        viewModelScope.launch {
            hortaRepository.logWatering(plant)
                .onSuccess { _snackbarEvents.tryEmit("Rega registrada para ${plant.hortalica}!") }
                .onFailure { _snackbarEvents.tryEmit("Não foi possível registrar a rega.") }
        }
    }

    fun updateCareSettings(
        plantId: String,
        wateringIntervalDays: Int,
        wateringScheduleLabel: String,
        reminderEnabled: Boolean,
        reminderTime: String,
        notes: String
    ) {
        viewModelScope.launch {
            hortaRepository.updateCareSettings(
                plantId = plantId,
                wateringIntervalDays = wateringIntervalDays,
                wateringScheduleLabel = wateringScheduleLabel,
                reminderEnabled = reminderEnabled,
                reminderTime = reminderTime,
                notes = notes
            ).onSuccess {
                _snackbarEvents.tryEmit("Lembrete e cuidados atualizados!")
            }.onFailure {
                _snackbarEvents.tryEmit("Não foi possível atualizar os cuidados.")
            }
        }
    }

    fun markHarvested(plant: GardenPlant) {
        viewModelScope.launch {
            hortaRepository.markHarvested(plant.id)
                .onSuccess { _snackbarEvents.tryEmit("Parabéns pela colheita de ${plant.hortalica}!") }
                .onFailure { _snackbarEvents.tryEmit("Não foi possível marcar como colhida.") }
        }
    }

    fun deletePlant(plant: GardenPlant) {
        viewModelScope.launch {
            hortaRepository.deleteGardenPlant(plant.id)
                .onSuccess { _snackbarEvents.tryEmit("${plant.hortalica} removida da Minha Horta.") }
                .onFailure { _snackbarEvents.tryEmit("Erro ao remover planta.") }
        }
    }

    private companion object {
        const val TAG = "SiTaVM"
        const val STOP_TIMEOUT_MILLIS = 5000L
    }
}
