package com.maprandroid.ui.config

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.maprandroid.data.local.ConfigProfileEntity
import com.maprandroid.data.local.toggleQuickReloadButton
import com.maprandroid.data.local.toggleSaveStateButton
import com.maprandroid.data.local.toggleLoadStateButton
import com.maprandroid.data.local.ALL_SNES_BUTTONS
import com.maprandroid.data.local.GameEntity
import com.maprandroid.data.local.SettingsRepository
import com.maprandroid.data.repository.ConfigProfileRepository
import com.maprandroid.data.repository.GameRepository
import com.maprandroid.data.repository.MapRandoRepository
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import com.maprandroid.util.BaseRomValidator
import com.maprandroid.util.RomPatcher
import java.io.File
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class GenerationStep(val stepNumber: Int, val description: String) {
    GENERATING_SEED(1, "Generating seed on MapRando server"),
    READING_BASE_ROM(2, "Reading Base ROM file"),
    PATCHING_ROM(3, "Customizing & patching ROM"),
    SAVING_ROM(4, "Saving ROM to library & device"),
    COMPLETE(5, "Generation Complete")
}

data class ConfigUiState(
    val isCustomMode: Boolean = false,
    val presets: List<String> = emptyList(),
    val selectedPreset: String? = null,
    val savedProfiles: List<ConfigProfileEntity> = emptyList(),
    val selectedProfileId: Int? = null,
    val currentCustomProfile: ConfigProfileEntity = ConfigProfileEntity(name = "Custom Preset"),
    val activeTab: Int = 0, // 0: Skill & Techs, 1: Progression & Pool, 2: Map & Environment, 3: QoL & Visuals, 4: Difficulty & Tuning
    val showSaveDialog: Boolean = false,
    val isLoading: Boolean = false,
    val isError: Boolean = false,
    val errorMessage: String? = null,
    val generationSuccess: Boolean = false,
    val pendingSeedId: String? = null,
    val baseRomUri: String? = null,
    val baseRomName: String? = null,
    val baseRomStatus: String = "Not Configured",
    val isBaseRomValid: Boolean = false,
    val showGenerationModal: Boolean = false,
    val generationStep: GenerationStep? = null,
    val isGenerating: Boolean = false,
    val generationError: String? = null,
    val generatedSeedId: String? = null,
    val generatedRomPath: String? = null,
    val emulatorPackageName: String? = null,
    val isCustomizingSeed: Boolean = false,
    val pendingCustomizeUrl: String? = null,
    val pendingUnheaderedBaseRomBytes: ByteArray? = null,
    val favoriteSprite: String = "samus_vanilla",
    val defaultControls: com.maprandroid.data.local.DefaultControls = com.maprandroid.data.local.DefaultControls(),
    val seedCustomizationProfile: ConfigProfileEntity = ConfigProfileEntity(name = "Customization Profile")
)

class ConfigViewModel(
    private val mapRandoRepository: MapRandoRepository,
    private val gameRepository: GameRepository,
    private val configProfileRepository: ConfigProfileRepository? = null,
    private val settingsRepository: SettingsRepository? = null
) : ViewModel() {

    private val _uiState = MutableStateFlow(ConfigUiState())
    val uiState: StateFlow<ConfigUiState> = _uiState.asStateFlow()

    init {
        fetchPresets()
        observeProfiles()
        observeSettings()
    }

    private fun observeSettings() {
        val settingsRepo = settingsRepository ?: return
        viewModelScope.launch {
            launch {
                settingsRepo.baseRomUri.collect { uri ->
                    _uiState.update { it.copy(baseRomUri = uri) }
                }
            }
            launch {
                settingsRepo.baseRomName.collect { name ->
                    _uiState.update { it.copy(baseRomName = name) }
                }
            }
            launch {
                settingsRepo.baseRomStatus.collect { status ->
                    _uiState.update { it.copy(baseRomStatus = status ?: "Not Configured") }
                }
            }
            launch {
                settingsRepo.isBaseRomValid.collect { isValid ->
                    _uiState.update { it.copy(isBaseRomValid = isValid) }
                }
            }
            launch {
                settingsRepo.emulatorPackageName.collect { pkg ->
                    _uiState.update { it.copy(emulatorPackageName = pkg) }
                }
            }
            launch {
                settingsRepo.savedSprites.collect { sprites ->
                    if (sprites.isNotEmpty()) {
                        com.maprandroid.data.model.SpriteCatalog.updateSprites(sprites)
                    }
                }
            }
            launch {
                settingsRepo.favoriteSprite.collect { favorite ->
                    _uiState.update { state ->
                        val updatedState = state.copy(favoriteSprite = favorite)
                        if (state.seedCustomizationProfile.samusSprite == "samus_vanilla") {
                            updatedState.copy(
                                seedCustomizationProfile = state.seedCustomizationProfile.copy(samusSprite = favorite)
                            )
                        } else {
                            updatedState
                        }
                    }
                }
            }
            launch {
                settingsRepo.defaultControls.collect { controls ->
                    _uiState.update { state ->
                        state.copy(defaultControls = controls)
                    }
                }
            }
        }
    }

    fun selectBaseRomFile(context: Context, uri: Uri) {
        viewModelScope.launch {
            try {
                try {
                    val takeFlags = android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
                    context.contentResolver.takePersistableUriPermission(uri, takeFlags)
                } catch (_: Exception) {
                }

                var fileName = "SuperMetroid.sfc"
                context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                    val nameIndex = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                    if (cursor.moveToFirst() && nameIndex >= 0) {
                        fileName = cursor.getString(nameIndex) ?: fileName
                    }
                }

                val romBytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                    ?: throw IllegalArgumentException("Unable to read selected Base ROM file")

                val validation = BaseRomValidator.validate(romBytes)

                _uiState.update {
                    it.copy(
                        baseRomUri = uri.toString(),
                        baseRomName = fileName,
                        baseRomStatus = validation.statusMessage,
                        isBaseRomValid = validation.isValid,
                        isError = !validation.isValid,
                        errorMessage = if (!validation.isValid) validation.statusMessage else null
                    )
                }

                settingsRepository?.saveBaseRomInfo(
                    uri = uri.toString(),
                    name = fileName,
                    status = validation.statusMessage,
                    isValid = validation.isValid
                )
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        baseRomStatus = "Failed to load Base ROM",
                        isBaseRomValid = false,
                        isError = true,
                        errorMessage = "Error reading Base ROM: ${e.localizedMessage}"
                    )
                }
            }
        }
    }

    fun fetchPresets() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, isError = false, errorMessage = null) }
            mapRandoRepository.getPresets().fold(
                onSuccess = { response ->
                    val initialSelection = _uiState.value.selectedPreset ?: response.presets.firstOrNull()
                    _uiState.update {
                        it.copy(
                            presets = response.presets,
                            selectedPreset = initialSelection,
                            isLoading = false,
                            isError = false
                        )
                    }
                },
                onFailure = { error ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            isError = true,
                            errorMessage = error.localizedMessage ?: "Failed to load presets"
                        )
                    }
                }
            )
        }
    }

    private fun observeProfiles() {
        if (configProfileRepository == null) return
        viewModelScope.launch {
            val initial = configProfileRepository.allProfiles.first()
            if (initial.isEmpty()) {
                configProfileRepository.seedDefaultProfilesIfEmpty(initial)
            }
            configProfileRepository.allProfiles.collect { profiles ->
                _uiState.update { state ->
                    val profileExists = profiles.any { it.id == state.selectedProfileId }
                    state.copy(
                        savedProfiles = profiles,
                        selectedProfileId = if (profileExists) state.selectedProfileId else null
                    )
                }
            }
        }
    }

    fun setCustomMode(isCustom: Boolean) {
        _uiState.update { it.copy(isCustomMode = isCustom, isError = false, errorMessage = null) }
    }

    fun selectPreset(preset: String) {
        _uiState.update { it.copy(selectedPreset = preset, isError = false, errorMessage = null) }
    }

    fun setActiveTab(tabIndex: Int) {
        _uiState.update { it.copy(activeTab = tabIndex) }
    }

    fun loadProfile(profile: ConfigProfileEntity) {
        _uiState.update {
            it.copy(
                selectedProfileId = profile.id,
                currentCustomProfile = profile,
                isError = false,
                errorMessage = null
            )
        }
    }

    // --- Skill & Tech Updaters ---
    private fun parseOverrides(jsonStr: String): MutableMap<String, Boolean> {
        if (jsonStr.isBlank() || jsonStr == "{}") return mutableMapOf()
        return try {
            val jsonObject = org.json.JSONObject(jsonStr)
            val map = mutableMapOf<String, Boolean>()
            val keys = jsonObject.keys()
            while (keys.hasNext()) {
                val key = keys.next()
                map[key] = jsonObject.getBoolean(key)
            }
            map
        } catch (_: Exception) {
            mutableMapOf()
        }
    }

    private fun serializeOverrides(map: Map<String, Boolean>): String {
        if (map.isEmpty()) return "{}"
        return try {
            val jsonObject = org.json.JSONObject()
            map.forEach { (k, v) -> jsonObject.put(k, v) }
            jsonObject.toString()
        } catch (_: Exception) {
            "{}"
        }
    }

    fun updateTechOverride(techName: String, enabled: Boolean) {
        _uiState.update { state ->
            val overrides = parseOverrides(state.currentCustomProfile.techOverridesJson)
            overrides[techName] = enabled
            val newJson = serializeOverrides(overrides)
            val updated = state.currentCustomProfile.copy(techOverridesJson = newJson)
            state.copy(currentCustomProfile = updated)
        }
    }

    fun bulkUpdateTechCategory(category: com.maprandroid.data.model.TechCategory, enabled: Boolean) {
        _uiState.update { state ->
            val overrides = parseOverrides(state.currentCustomProfile.techOverridesJson)
            com.maprandroid.data.model.TechCatalog.ALL_TECHS
                .filter { it.category == category }
                .forEach { tech -> overrides[tech.name] = enabled }
            val newJson = serializeOverrides(overrides)
            val updated = state.currentCustomProfile.copy(techOverridesJson = newJson)
            state.copy(currentCustomProfile = updated)
        }
    }

    fun resetTechOverrides() {
        _uiState.update { state ->
            val updated = state.currentCustomProfile.copy(techOverridesJson = "{}")
            state.copy(currentCustomProfile = updated)
        }
    }

    fun updateLogicSkill(skill: String) {
        _uiState.update { state ->
            val updated = when (skill) {
                "Casual" -> state.currentCustomProfile.copy(
                    logicSkill = skill,
                    techWallJump = true, techIBJ = false, techShinespark = false, techShortCharge = false,
                    techMockball = false, techSpringBall = false, techMidAirMorph = false, techDamageBoosts = false
                )
                "Medium" -> state.currentCustomProfile.copy(
                    logicSkill = skill,
                    techWallJump = true, techIBJ = true, techShinespark = true, techShortCharge = false,
                    techMockball = true, techSpringBall = false, techMidAirMorph = false, techDamageBoosts = false
                )
                "Hard" -> state.currentCustomProfile.copy(
                    logicSkill = skill,
                    techWallJump = true, techIBJ = true, techShinespark = true, techShortCharge = true,
                    techMockball = true, techSpringBall = true, techMidAirMorph = false, techDamageBoosts = false
                )
                "Very Hard", "Expert", "Master" -> state.currentCustomProfile.copy(
                    logicSkill = skill,
                    techWallJump = true, techIBJ = true, techShinespark = true, techShortCharge = true,
                    techMockball = true, techSpringBall = true, techMidAirMorph = true, techDamageBoosts = true
                )
                else -> state.currentCustomProfile.copy(logicSkill = skill)
            }
            state.copy(currentCustomProfile = updated)
        }
    }

    fun updateTechWallJump(enabled: Boolean) {
        _uiState.update { state ->
            val updated = state.currentCustomProfile.copy(techWallJump = enabled)
            state.copy(currentCustomProfile = updated)
        }
    }

    fun updateTechIBJ(enabled: Boolean) {
        _uiState.update { state ->
            val updated = state.currentCustomProfile.copy(techIBJ = enabled)
            state.copy(currentCustomProfile = updated)
        }
    }

    fun updateTechShinespark(enabled: Boolean) {
        _uiState.update { state ->
            val updated = state.currentCustomProfile.copy(techShinespark = enabled)
            state.copy(currentCustomProfile = updated)
        }
    }

    fun updateTechShortCharge(enabled: Boolean) {
        _uiState.update { state ->
            val updated = state.currentCustomProfile.copy(techShortCharge = enabled)
            state.copy(currentCustomProfile = updated)
        }
    }

    fun updateTechMockball(enabled: Boolean) {
        _uiState.update { state ->
            val updated = state.currentCustomProfile.copy(techMockball = enabled)
            state.copy(currentCustomProfile = updated)
        }
    }

    fun updateTechSpringBall(enabled: Boolean) {
        _uiState.update { state ->
            val updated = state.currentCustomProfile.copy(techSpringBall = enabled)
            state.copy(currentCustomProfile = updated)
        }
    }

    fun updateTechMidAirMorph(enabled: Boolean) {
        _uiState.update { state ->
            val updated = state.currentCustomProfile.copy(techMidAirMorph = enabled)
            state.copy(currentCustomProfile = updated)
        }
    }

    fun updateTechDamageBoosts(enabled: Boolean) {
        _uiState.update { state ->
            val updated = state.currentCustomProfile.copy(techDamageBoosts = enabled)
            state.copy(currentCustomProfile = updated)
        }
    }

    fun updateLogicOptions(options: String) {
        _uiState.update { state ->
            val updated = state.currentCustomProfile.copy(logicOptions = options)
            state.copy(currentCustomProfile = updated)
        }
    }

    // --- Progression & Pool Updaters ---
    fun updateItemProgression(progression: String) {
        _uiState.update { state ->
            val updated = state.currentCustomProfile.copy(itemProgression = progression)
            state.copy(currentCustomProfile = updated)
        }
    }

    fun updateItemPool(pool: String) {
        _uiState.update { state ->
            val updated = state.currentCustomProfile.copy(itemPool = pool)
            state.copy(currentCustomProfile = updated)
        }
    }

    fun updateMajorMinorDistribution(distribution: String) {
        _uiState.update { state ->
            val updated = state.currentCustomProfile.copy(majorMinorDistribution = distribution)
            state.copy(currentCustomProfile = updated)
        }
    }

    fun updateScavengerHunt(enabled: Boolean) {
        _uiState.update { state ->
            val updated = state.currentCustomProfile.copy(scavengerHunt = enabled)
            state.copy(currentCustomProfile = updated)
        }
    }

    fun updateChozoHunt(enabled: Boolean) {
        _uiState.update { state ->
            val updated = state.currentCustomProfile.copy(chozoHunt = enabled)
            state.copy(currentCustomProfile = updated)
        }
    }

    fun updateScavengerRequiredCount(count: Int) {
        _uiState.update { state ->
            val updated = state.currentCustomProfile.copy(scavengerRequiredCount = count)
            state.copy(currentCustomProfile = updated)
        }
    }

    fun updateSuperMissileCount(count: Int) {
        _uiState.update { state ->
            val updated = state.currentCustomProfile.copy(superMissileCount = count)
            state.copy(currentCustomProfile = updated)
        }
    }

    fun updatePowerBombCount(count: Int) {
        _uiState.update { state ->
            val updated = state.currentCustomProfile.copy(powerBombCount = count)
            state.copy(currentCustomProfile = updated)
        }
    }

    fun updateStartMorph(enabled: Boolean) {
        _uiState.update { state ->
            val updated = state.currentCustomProfile.copy(startMorph = enabled)
            state.copy(currentCustomProfile = updated)
        }
    }

    fun updateStartCharge(enabled: Boolean) {
        _uiState.update { state ->
            val updated = state.currentCustomProfile.copy(startCharge = enabled)
            state.copy(currentCustomProfile = updated)
        }
    }

    fun updateStartVaria(enabled: Boolean) {
        _uiState.update { state ->
            val updated = state.currentCustomProfile.copy(startVaria = enabled)
            state.copy(currentCustomProfile = updated)
        }
    }

    // --- Map & Environment Updaters ---
    fun updateMapRandomization(mapRandomization: String) {
        _uiState.update { state ->
            val updated = state.currentCustomProfile.copy(mapRandomization = mapRandomization)
            state.copy(currentCustomProfile = updated)
        }
    }

    fun updateBossRandomization(bossRandomization: String) {
        _uiState.update { state ->
            val updated = state.currentCustomProfile.copy(bossRandomization = bossRandomization)
            state.copy(currentCustomProfile = updated)
        }
    }

    fun updateDoorLockRandomization(doorLock: String) {
        _uiState.update { state ->
            val updated = state.currentCustomProfile.copy(doorLockRandomization = doorLock)
            state.copy(currentCustomProfile = updated)
        }
    }

    fun updateElevatorRando(enabled: Boolean) {
        _uiState.update { state ->
            val updated = state.currentCustomProfile.copy(elevatorRando = enabled)
            state.copy(currentCustomProfile = updated)
        }
    }

    fun updateEscapeRando(enabled: Boolean) {
        _uiState.update { state ->
            val updated = state.currentCustomProfile.copy(escapeRando = enabled)
            state.copy(currentCustomProfile = updated)
        }
    }

    // --- QoL & Visuals Updaters ---
    fun setQolPreset(preset: String) {
        _uiState.update { state ->
            val updated = when (preset) {
                "Max QoL" -> state.currentCustomProfile.copy(
                    qolFastElevators = true,
                    qolFastDoors = true,
                    qolFastText = true,
                    qolCutsceneSkip = true,
                    qolQuickMorph = true,
                    qolSpinJumpRestart = true,
                    qolAutoChargeBeam = true
                )
                "Minimal QoL" -> state.currentCustomProfile.copy(
                    qolFastElevators = false,
                    qolFastDoors = false,
                    qolFastText = false,
                    qolCutsceneSkip = false,
                    qolQuickMorph = false,
                    qolSpinJumpRestart = false,
                    qolAutoChargeBeam = false
                )
                else -> state.currentCustomProfile.copy(
                    qolFastElevators = true,
                    qolFastDoors = true,
                    qolFastText = true,
                    qolCutsceneSkip = true,
                    qolQuickMorph = true,
                    qolSpinJumpRestart = true,
                    qolAutoChargeBeam = false
                )
            }
            state.copy(currentCustomProfile = updated)
        }
    }

    fun updateQolFastElevators(enabled: Boolean) {
        _uiState.update { state ->
            val updated = state.currentCustomProfile.copy(qolFastElevators = enabled)
            state.copy(currentCustomProfile = updated)
        }
    }

    fun updateQolFastDoors(enabled: Boolean) {
        _uiState.update { state ->
            val updated = state.currentCustomProfile.copy(qolFastDoors = enabled)
            state.copy(currentCustomProfile = updated)
        }
    }

    fun updateQolFastText(enabled: Boolean) {
        _uiState.update { state ->
            val updated = state.currentCustomProfile.copy(qolFastText = enabled)
            state.copy(currentCustomProfile = updated)
        }
    }

    fun updateQolCutsceneSkip(enabled: Boolean) {
        _uiState.update { state ->
            val updated = state.currentCustomProfile.copy(qolCutsceneSkip = enabled)
            state.copy(currentCustomProfile = updated)
        }
    }

    fun updateQolQuickMorph(enabled: Boolean) {
        _uiState.update { state ->
            val updated = state.currentCustomProfile.copy(qolQuickMorph = enabled)
            state.copy(currentCustomProfile = updated)
        }
    }

    fun updateQolSpinJumpRestart(enabled: Boolean) {
        _uiState.update { state ->
            val updated = state.currentCustomProfile.copy(qolSpinJumpRestart = enabled)
            state.copy(currentCustomProfile = updated)
        }
    }

    fun updateQolAutoChargeBeam(enabled: Boolean) {
        _uiState.update { state ->
            val updated = state.currentCustomProfile.copy(qolAutoChargeBeam = enabled)
            state.copy(currentCustomProfile = updated)
        }
    }

    fun updateAimButtonSetting(setting: String) {
        _uiState.update { state ->
            val updated = state.currentCustomProfile.copy(aimButtonSetting = setting)
            state.copy(currentCustomProfile = updated)
        }
    }

    fun updateMapIcons(setting: String) {
        _uiState.update { state ->
            val updated = state.currentCustomProfile.copy(mapIcons = setting)
            state.copy(currentCustomProfile = updated)
        }
    }

    fun updateItemMarkers(setting: String) {
        _uiState.update { state ->
            val updated = state.currentCustomProfile.copy(itemMarkers = setting)
            state.copy(currentCustomProfile = updated)
        }
    }

    fun updateMusicShuffle(enabled: Boolean) {
        _uiState.update { state ->
            val updated = state.currentCustomProfile.copy(musicShuffle = enabled)
            state.copy(currentCustomProfile = updated)
        }
    }

    fun updatePaletteShuffle(enabled: Boolean) {
        _uiState.update { state ->
            val updated = state.currentCustomProfile.copy(paletteShuffle = enabled)
            state.copy(currentCustomProfile = updated)
        }
    }

    // --- Difficulty Updaters ---
    fun updateDifficultyResource(difficulty: String) {
        _uiState.update { state ->
            val updated = state.currentCustomProfile.copy(difficultyResource = difficulty)
            state.copy(currentCustomProfile = updated)
        }
    }

    fun updateDamageMultiplier(multiplier: String) {
        _uiState.update { state ->
            val updated = state.currentCustomProfile.copy(damageMultiplier = multiplier)
            state.copy(currentCustomProfile = updated)
        }
    }

    // --- Objectives Updaters ---
    fun updateObjectivePreset(preset: String) {
        _uiState.update { state ->
            val updated = state.currentCustomProfile.copy(objectivePreset = preset)
            state.copy(currentCustomProfile = updated)
        }
    }

    fun updateKraidObjective(enabled: Boolean) {
        _uiState.update { state ->
            val updated = state.currentCustomProfile.copy(kraidObjective = enabled)
            state.copy(currentCustomProfile = updated)
        }
    }

    fun updatePhantoonObjective(enabled: Boolean) {
        _uiState.update { state ->
            val updated = state.currentCustomProfile.copy(phantoonObjective = enabled)
            state.copy(currentCustomProfile = updated)
        }
    }

    fun updateDraygonObjective(enabled: Boolean) {
        _uiState.update { state ->
            val updated = state.currentCustomProfile.copy(draygonObjective = enabled)
            state.copy(currentCustomProfile = updated)
        }
    }

    fun updateRidleyObjective(enabled: Boolean) {
        _uiState.update { state ->
            val updated = state.currentCustomProfile.copy(ridleyObjective = enabled)
            state.copy(currentCustomProfile = updated)
        }
    }

    fun updateMinObjectives(count: Int) {
        _uiState.update { state ->
            val updated = state.currentCustomProfile.copy(minObjectives = count)
            state.copy(currentCustomProfile = updated)
        }
    }

    fun updateMaxObjectives(count: Int) {
        _uiState.update { state ->
            val updated = state.currentCustomProfile.copy(maxObjectives = count)
            state.copy(currentCustomProfile = updated)
        }
    }

    // --- Doors & Location Updaters ---
    fun updateDoorsPreset(preset: String) {
        _uiState.update { state ->
            val updated = state.currentCustomProfile.copy(doorsPreset = preset)
            state.copy(currentCustomProfile = updated)
        }
    }

    fun updateRedDoorsCount(count: Int) {
        _uiState.update { state ->
            val updated = state.currentCustomProfile.copy(redDoorsCount = count)
            state.copy(currentCustomProfile = updated)
        }
    }

    fun updateGreenDoorsCount(count: Int) {
        _uiState.update { state ->
            val updated = state.currentCustomProfile.copy(greenDoorsCount = count)
            state.copy(currentCustomProfile = updated)
        }
    }

    fun updateYellowDoorsCount(count: Int) {
        _uiState.update { state ->
            val updated = state.currentCustomProfile.copy(yellowDoorsCount = count)
            state.copy(currentCustomProfile = updated)
        }
    }

    fun updateStartLocationMode(mode: String) {
        _uiState.update { state ->
            val updated = state.currentCustomProfile.copy(startLocationMode = mode)
            state.copy(currentCustomProfile = updated)
        }
    }

    fun updateSaveAnimals(setting: String) {
        _uiState.update { state ->
            val updated = state.currentCustomProfile.copy(saveAnimals = setting)
            state.copy(currentCustomProfile = updated)
        }
    }

    // --- Variations & Other Updaters ---
    fun updateWallJumpSetting(setting: String) {
        _uiState.update { state ->
            val updated = state.currentCustomProfile.copy(wallJumpSetting = setting)
            state.copy(currentCustomProfile = updated)
        }
    }

    fun updateSpeedBoosterSetting(setting: String) {
        _uiState.update { state ->
            val updated = state.currentCustomProfile.copy(speedBoosterSetting = setting)
            state.copy(currentCustomProfile = updated)
        }
    }

    fun updateAreaAssignmentBaseOrder(order: String) {
        _uiState.update { state ->
            val updated = state.currentCustomProfile.copy(areaAssignmentBaseOrder = order)
            state.copy(currentCustomProfile = updated)
        }
    }

    fun updateShipInCrateria(enabled: Boolean) {
        _uiState.update { state ->
            val updated = state.currentCustomProfile.copy(shipInCrateria = enabled)
            state.copy(currentCustomProfile = updated)
        }
    }

    fun updateMotherBrainInTourian(enabled: Boolean) {
        _uiState.update { state ->
            val updated = state.currentCustomProfile.copy(motherBrainInTourian = enabled)
            state.copy(currentCustomProfile = updated)
        }
    }

    fun updateRaceMode(enabled: Boolean) {
        _uiState.update { state ->
            val updated = state.currentCustomProfile.copy(raceMode = enabled)
            state.copy(currentCustomProfile = updated)
        }
    }

    fun updateSavestate(setting: String) {
        _uiState.update { state ->
            val updated = state.currentCustomProfile.copy(savestate = setting)
            state.copy(currentCustomProfile = updated)
        }
    }

    fun updateEnableMajorGlitches(enabled: Boolean) {
        _uiState.update { state ->
            val updated = state.currentCustomProfile.copy(enableMajorGlitches = enabled)
            state.copy(currentCustomProfile = updated)
        }
    }

    fun updateBeamDoorsCount(count: Int) {
        _uiState.update { state ->
            val updated = state.currentCustomProfile.copy(beamDoorsCount = count)
            state.copy(currentCustomProfile = updated)
        }
    }

    fun updateStartLocationRoom(room: String) {
        _uiState.update { state ->
            val updated = state.currentCustomProfile.copy(startLocationRoom = room)
            state.copy(currentCustomProfile = updated)
        }
    }

    fun updateSporeSpawnObjective(enabled: Boolean) {
        _uiState.update { state ->
            val updated = state.currentCustomProfile.copy(sporeSpawnObjective = enabled)
            state.copy(currentCustomProfile = updated)
        }
    }

    fun updateCrocomireObjective(enabled: Boolean) {
        _uiState.update { state ->
            val updated = state.currentCustomProfile.copy(crocomireObjective = enabled)
            state.copy(currentCustomProfile = updated)
        }
    }

    fun updateBotwoonObjective(enabled: Boolean) {
        _uiState.update { state ->
            val updated = state.currentCustomProfile.copy(botwoonObjective = enabled)
            state.copy(currentCustomProfile = updated)
        }
    }

    fun updateGoldenTorizoObjective(enabled: Boolean) {
        _uiState.update { state ->
            val updated = state.currentCustomProfile.copy(goldenTorizoObjective = enabled)
            state.copy(currentCustomProfile = updated)
        }
    }

    fun updateBombTorizoObjective(enabled: Boolean) {
        _uiState.update { state ->
            val updated = state.currentCustomProfile.copy(bombTorizoObjective = enabled)
            state.copy(currentCustomProfile = updated)
        }
    }

    fun updateChozoStatuesObjective(enabled: Boolean) {
        _uiState.update { state ->
            val updated = state.currentCustomProfile.copy(chozoStatuesObjective = enabled)
            state.copy(currentCustomProfile = updated)
        }
    }

    fun updatePiratesObjective(enabled: Boolean) {
        _uiState.update { state ->
            val updated = state.currentCustomProfile.copy(piratesObjective = enabled)
            state.copy(currentCustomProfile = updated)
        }
    }

    fun updateMetroidsObjective(enabled: Boolean) {
        _uiState.update { state ->
            val updated = state.currentCustomProfile.copy(metroidsObjective = enabled)
            state.copy(currentCustomProfile = updated)
        }
    }

    fun updateShinesparkTiles(tiles: Int) {
        _uiState.update { state ->
            val updated = state.currentCustomProfile.copy(shinesparkTiles = tiles)
            state.copy(currentCustomProfile = updated)
        }
    }

    fun updateHeatedShinesparkTiles(tiles: Int) {
        _uiState.update { state ->
            val updated = state.currentCustomProfile.copy(heatedShinesparkTiles = tiles)
            state.copy(currentCustomProfile = updated)
        }
    }

    fun updateResourceMultiplier(multiplier: Float) {
        _uiState.update { state ->
            val updated = state.currentCustomProfile.copy(resourceMultiplier = multiplier)
            state.copy(currentCustomProfile = updated)
        }
    }

    fun updateFarmTimeLimit(limit: Int) {
        _uiState.update { state ->
            val updated = state.currentCustomProfile.copy(farmTimeLimit = limit)
            state.copy(currentCustomProfile = updated)
        }
    }

    fun updateEscapeTimerMultiplier(multiplier: Float) {
        _uiState.update { state ->
            val updated = state.currentCustomProfile.copy(escapeTimerMultiplier = multiplier)
            state.copy(currentCustomProfile = updated)
        }
    }

    fun updateProgressionRate(rate: String) {
        _uiState.update { state ->
            val updated = state.currentCustomProfile.copy(progressionRate = rate)
            state.copy(currentCustomProfile = updated)
        }
    }

    fun updateItemPlacementStyle(style: String) {
        _uiState.update { state ->
            val updated = state.currentCustomProfile.copy(itemPlacementStyle = style)
            state.copy(currentCustomProfile = updated)
        }
    }

    fun updateItemPriorityStrength(strength: String) {
        _uiState.update { state ->
            val updated = state.currentCustomProfile.copy(itemPriorityStrength = strength)
            state.copy(currentCustomProfile = updated)
        }
    }

    fun updateQolMotherBrainFight(fight: String) {
        _uiState.update { state ->
            val updated = state.currentCustomProfile.copy(qolMotherBrainFight = fight)
            state.copy(currentCustomProfile = updated)
        }
    }

    fun updateQolRoomOutlineRevealed(enabled: Boolean) {
        _uiState.update { state ->
            val updated = state.currentCustomProfile.copy(qolRoomOutlineRevealed = enabled)
            state.copy(currentCustomProfile = updated)
        }
    }

    fun updateQolHazardMarkers(enabled: Boolean) {
        _uiState.update { state ->
            val updated = state.currentCustomProfile.copy(qolHazardMarkers = enabled)
            state.copy(currentCustomProfile = updated)
        }
    }

    fun updateQolFixBlueEchoes(enabled: Boolean) {
        _uiState.update { state ->
            val updated = state.currentCustomProfile.copy(qolFixBlueEchoes = enabled)
            state.copy(currentCustomProfile = updated)
        }
    }

    fun updateQolEarlySave(enabled: Boolean) {
        _uiState.update { state ->
            val updated = state.currentCustomProfile.copy(qolEarlySave = enabled)
            state.copy(currentCustomProfile = updated)
        }
    }

    fun updateQolAmmoRefillAll(enabled: Boolean) {
        _uiState.update { state ->
            val updated = state.currentCustomProfile.copy(qolAmmoRefillAll = enabled)
            state.copy(currentCustomProfile = updated)
        }
    }

    fun updateSamusSprite(sprite: String) {
        _uiState.update { state ->
            val updated = state.currentCustomProfile.copy(samusSprite = sprite)
            state.copy(currentCustomProfile = updated)
        }
    }

    fun updateEtankColor(color: String) {
        _uiState.update { state ->
            val updated = state.currentCustomProfile.copy(etankColor = color)
            state.copy(currentCustomProfile = updated)
        }
    }

    fun updateReserveHudStyle(enabled: Boolean) {
        _uiState.update { state ->
            val updated = state.currentCustomProfile.copy(reserveHudStyle = enabled)
            state.copy(currentCustomProfile = updated)
        }
    }

    fun updateTransitionLetters(enabled: Boolean) {
        _uiState.update { state ->
            val updated = state.currentCustomProfile.copy(transitionLetters = enabled)
            state.copy(currentCustomProfile = updated)
        }
    }

    fun updateMapTheme(theme: String) {
        _uiState.update { state ->
            val updated = state.currentCustomProfile.copy(mapTheme = theme)
            state.copy(currentCustomProfile = updated)
        }
    }

    fun updateRoomPalettes(palette: String) {
        _uiState.update { state ->
            val updated = state.currentCustomProfile.copy(roomPalettes = palette)
            state.copy(currentCustomProfile = updated)
        }
    }

    fun updateTileTheme(theme: String) {
        _uiState.update { state ->
            val updated = state.currentCustomProfile.copy(tileTheme = theme)
            state.copy(currentCustomProfile = updated)
        }
    }

    fun updateDoorTheme(theme: String) {
        _uiState.update { state ->
            val updated = state.currentCustomProfile.copy(doorTheme = theme)
            state.copy(currentCustomProfile = updated)
        }
    }

    fun updateMusic(music: String) {
        _uiState.update { state ->
            val updated = state.currentCustomProfile.copy(music = music)
            state.copy(currentCustomProfile = updated)
        }
    }

    fun updateShaking(shaking: String) {
        _uiState.update { state ->
            val updated = state.currentCustomProfile.copy(shaking = shaking)
            state.copy(currentCustomProfile = updated)
        }
    }

    fun updateFlashing(flashing: String) {
        _uiState.update { state ->
            val updated = state.currentCustomProfile.copy(flashing = flashing)
            state.copy(currentCustomProfile = updated)
        }
    }

    fun updateDisableBeeping(enabled: Boolean) {
        _uiState.update { state ->
            val updated = state.currentCustomProfile.copy(disableBeeping = enabled)
            state.copy(currentCustomProfile = updated)
        }
    }

    fun updateItemDotChange(change: String) {
        _uiState.update { state ->
            val updated = state.currentCustomProfile.copy(itemDotChange = change)
            state.copy(currentCustomProfile = updated)
        }
    }

    fun updateRoomNames(enabled: Boolean) {
        _uiState.update { state ->
            val updated = state.currentCustomProfile.copy(roomNames = enabled)
            state.copy(currentCustomProfile = updated)
        }
    }

    fun updateBossIcons(enabled: Boolean) {
        _uiState.update { state ->
            val updated = state.currentCustomProfile.copy(bossIcons = enabled)
            state.copy(currentCustomProfile = updated)
        }
    }

    fun updateMinibossIcons(enabled: Boolean) {
        _uiState.update { state ->
            val updated = state.currentCustomProfile.copy(minibossIcons = enabled)
            state.copy(currentCustomProfile = updated)
        }
    }

    fun updateSaveIcons(enabled: Boolean) {
        _uiState.update { state ->
            val updated = state.currentCustomProfile.copy(saveIcons = enabled)
            state.copy(currentCustomProfile = updated)
        }
    }

    fun updateStatuesHallwayTiling(tiling: String) {
        _uiState.update { state ->
            val updated = state.currentCustomProfile.copy(statuesHallwayTiling = tiling)
            state.copy(currentCustomProfile = updated)
        }
    }

    fun updateStatuesHallwayAudio(audio: String) {
        _uiState.update { state ->
            val updated = state.currentCustomProfile.copy(statuesHallwayAudio = audio)
            state.copy(currentCustomProfile = updated)
        }
    }

    fun updateVanillaScrewAttackAnimation(enabled: Boolean) {
        _uiState.update { state ->
            val updated = state.currentCustomProfile.copy(vanillaScrewAttackAnimation = enabled)
            state.copy(currentCustomProfile = updated)
        }
    }

    private fun ConfigProfileEntity.withControlBinding(action: String, newButton: String): ConfigProfileEntity {
        val currentMap = mutableMapOf(
            "shot" to controlShot,
            "jump" to controlJump,
            "dash" to controlDash,
            "itemSelect" to controlItemSelect,
            "itemCancel" to controlItemCancel,
            "angleUp" to controlAngleUp,
            "angleDown" to controlAngleDown
        )

        val oldButton = currentMap[action] ?: "X"
        if (oldButton == newButton) return this

        val conflictingAction = currentMap.entries.firstOrNull { it.value == newButton && it.key != action }?.key

        currentMap[action] = newButton
        if (conflictingAction != null) {
            currentMap[conflictingAction] = oldButton
        }

        return copy(
            controlShot = currentMap["shot"] ?: controlShot,
            controlJump = currentMap["jump"] ?: controlJump,
            controlDash = currentMap["dash"] ?: controlDash,
            controlItemSelect = currentMap["itemSelect"] ?: controlItemSelect,
            controlItemCancel = currentMap["itemCancel"] ?: controlItemCancel,
            controlAngleUp = currentMap["angleUp"] ?: controlAngleUp,
            controlAngleDown = currentMap["angleDown"] ?: controlAngleDown
        )
    }

    fun updateControlShot(button: String) {
        _uiState.update { state ->
            val updated = state.currentCustomProfile.withControlBinding("shot", button)
            state.copy(currentCustomProfile = updated)
        }
    }

    fun updateControlJump(button: String) {
        _uiState.update { state ->
            val updated = state.currentCustomProfile.withControlBinding("jump", button)
            state.copy(currentCustomProfile = updated)
        }
    }

    fun updateControlDash(button: String) {
        _uiState.update { state ->
            val updated = state.currentCustomProfile.withControlBinding("dash", button)
            state.copy(currentCustomProfile = updated)
        }
    }

    fun updateControlItemSelect(button: String) {
        _uiState.update { state ->
            val updated = state.currentCustomProfile.withControlBinding("itemSelect", button)
            state.copy(currentCustomProfile = updated)
        }
    }

    fun updateControlItemCancel(button: String) {
        _uiState.update { state ->
            val updated = state.currentCustomProfile.withControlBinding("itemCancel", button)
            state.copy(currentCustomProfile = updated)
        }
    }

    fun updateControlAngleUp(button: String) {
        _uiState.update { state ->
            val updated = state.currentCustomProfile.withControlBinding("angleUp", button)
            state.copy(currentCustomProfile = updated)
        }
    }

    fun updateControlAngleDown(button: String) {
        _uiState.update { state ->
            val updated = state.currentCustomProfile.withControlBinding("angleDown", button)
            state.copy(currentCustomProfile = updated)
        }
    }

    fun toggleQuickReloadButton(button: String, enabled: Boolean) {
        _uiState.update { state ->
            val updated = state.currentCustomProfile.toggleQuickReloadButton(button, enabled)
            state.copy(currentCustomProfile = updated)
        }
    }

    fun toggleSaveStateButton(button: String, enabled: Boolean) {
        _uiState.update { state ->
            val updated = state.currentCustomProfile.toggleSaveStateButton(button, enabled)
            state.copy(currentCustomProfile = updated)
        }
    }

    fun toggleLoadStateButton(button: String, enabled: Boolean) {
        _uiState.update { state ->
            val updated = state.currentCustomProfile.toggleLoadStateButton(button, enabled)
            state.copy(currentCustomProfile = updated)
        }
    }

    fun updateSpinLockX(enabled: Boolean) {
        _uiState.update { state ->
            val updated = state.currentCustomProfile.copy(spinLockX = enabled)
            state.copy(currentCustomProfile = updated)
        }
    }

    fun updateSpinLockL(enabled: Boolean) {
        _uiState.update { state ->
            val updated = state.currentCustomProfile.copy(spinLockL = enabled)
            state.copy(currentCustomProfile = updated)
        }
    }

    fun updateSpinLockR(enabled: Boolean) {
        _uiState.update { state ->
            val updated = state.currentCustomProfile.copy(spinLockR = enabled)
            state.copy(currentCustomProfile = updated)
        }
    }

    fun updateSpinLockUp(enabled: Boolean) {
        _uiState.update { state ->
            val updated = state.currentCustomProfile.copy(spinLockUp = enabled)
            state.copy(currentCustomProfile = updated)
        }
    }

    fun updateMoonwalk(enabled: Boolean) {
        _uiState.update { state ->
            val updated = state.currentCustomProfile.copy(moonwalk = enabled)
            state.copy(currentCustomProfile = updated)
        }
    }

    fun updateDisableSpikesuit(enabled: Boolean) {
        _uiState.update { state ->
            val updated = state.currentCustomProfile.copy(disableSpikesuit = enabled)
            state.copy(currentCustomProfile = updated)
        }
    }

    fun updateDisableBluesuit(enabled: Boolean) {
        _uiState.update { state ->
            val updated = state.currentCustomProfile.copy(disableBluesuit = enabled)
            state.copy(currentCustomProfile = updated)
        }
    }

    fun updateDoorLocksSizeOnMap(size: String) {
        _uiState.update { state ->
            val updated = state.currentCustomProfile.copy(doorLocksSizeOnMap = size)
            state.copy(currentCustomProfile = updated)
        }
    }

    fun updateEnergyFreeShinesparks(enabled: Boolean) {
        _uiState.update { state ->
            val updated = state.currentCustomProfile.copy(energyFreeShinesparks = enabled)
            state.copy(currentCustomProfile = updated)
        }
    }

    fun updateAllEnemiesRespawn(enabled: Boolean) {
        _uiState.update { state ->
            val updated = state.currentCustomProfile.copy(allEnemiesRespawn = enabled)
            state.copy(currentCustomProfile = updated)
        }
    }

    fun showSaveDialog(show: Boolean) {
        _uiState.update { it.copy(showSaveDialog = show) }
    }

    fun saveCurrentProfile(name: String) {
        val trimmedName = name.trim().ifEmpty { "Custom Preset" }
        viewModelScope.launch {
            val repo = configProfileRepository ?: return@launch
            val profileToSave = _uiState.value.currentCustomProfile.copy(
                id = _uiState.value.selectedProfileId ?: 0,
                name = trimmedName
            )
            val newId = repo.saveProfile(profileToSave)
            _uiState.update { state ->
                state.copy(
                    selectedProfileId = if (profileToSave.id > 0) profileToSave.id else newId.toInt(),
                    currentCustomProfile = profileToSave.copy(id = if (profileToSave.id > 0) profileToSave.id else newId.toInt()),
                    showSaveDialog = false
                )
            }
        }
    }

    fun deleteProfile(profileId: Int) {
        viewModelScope.launch {
            val repo = configProfileRepository ?: return@launch
            repo.deleteProfile(profileId)
            _uiState.update { state ->
                if (state.selectedProfileId == profileId) {
                    state.copy(
                        selectedProfileId = null,
                        currentCustomProfile = ConfigProfileEntity(
                            name = "Custom Preset",
                            samusSprite = state.favoriteSprite
                        )
                    )
                } else {
                    state
                }
            }
        }
    }

    fun resetCustomProfile() {
        _uiState.update { state ->
            state.copy(
                selectedProfileId = null,
                currentCustomProfile = ConfigProfileEntity(
                    name = "Custom Preset",
                    samusSprite = state.favoriteSprite,
                    controlShot = state.defaultControls.shot,
                    controlJump = state.defaultControls.jump,
                    controlDash = state.defaultControls.dash,
                    controlItemSelect = state.defaultControls.itemSelect,
                    controlItemCancel = state.defaultControls.itemCancel,
                    controlAngleUp = state.defaultControls.angleUp,
                    controlAngleDown = state.defaultControls.angleDown
                )
            )
        }
    }

    fun updateCustomizationControlShot(button: String) {
        _uiState.update { it.copy(seedCustomizationProfile = it.seedCustomizationProfile.withControlBinding("shot", button)) }
    }
    fun updateCustomizationControlJump(button: String) {
        _uiState.update { it.copy(seedCustomizationProfile = it.seedCustomizationProfile.withControlBinding("jump", button)) }
    }
    fun updateCustomizationControlDash(button: String) {
        _uiState.update { it.copy(seedCustomizationProfile = it.seedCustomizationProfile.withControlBinding("dash", button)) }
    }
    fun updateCustomizationControlItemSelect(button: String) {
        _uiState.update { it.copy(seedCustomizationProfile = it.seedCustomizationProfile.withControlBinding("itemSelect", button)) }
    }
    fun updateCustomizationControlItemCancel(button: String) {
        _uiState.update { it.copy(seedCustomizationProfile = it.seedCustomizationProfile.withControlBinding("itemCancel", button)) }
    }
    fun updateCustomizationControlAngleUp(button: String) {
        _uiState.update { it.copy(seedCustomizationProfile = it.seedCustomizationProfile.withControlBinding("angleUp", button)) }
    }
    fun updateCustomizationControlAngleDown(button: String) {
        _uiState.update { it.copy(seedCustomizationProfile = it.seedCustomizationProfile.withControlBinding("angleDown", button)) }
    }
    fun toggleCustomizationQuickReloadButton(button: String, enabled: Boolean) {
        _uiState.update { it.copy(seedCustomizationProfile = it.seedCustomizationProfile.toggleQuickReloadButton(button, enabled)) }
    }
    fun toggleCustomizationSaveStateButton(button: String, enabled: Boolean) {
        _uiState.update { it.copy(seedCustomizationProfile = it.seedCustomizationProfile.toggleSaveStateButton(button, enabled)) }
    }
    fun toggleCustomizationLoadStateButton(button: String, enabled: Boolean) {
        _uiState.update { it.copy(seedCustomizationProfile = it.seedCustomizationProfile.toggleLoadStateButton(button, enabled)) }
    }
    fun updateCustomizationSavestate(setting: String) {
        _uiState.update { it.copy(seedCustomizationProfile = it.seedCustomizationProfile.copy(savestate = setting)) }
    }
    fun updateCustomizationMoonwalk(enabled: Boolean) {
        _uiState.update { it.copy(seedCustomizationProfile = it.seedCustomizationProfile.copy(moonwalk = enabled)) }
    }
    fun updateCustomizationSamusSprite(sprite: String) {
        _uiState.update { it.copy(seedCustomizationProfile = it.seedCustomizationProfile.copy(samusSprite = sprite)) }
    }
    fun updateCustomizationEtankColor(color: String) {
        _uiState.update { it.copy(seedCustomizationProfile = it.seedCustomizationProfile.copy(etankColor = color)) }
    }
    fun updateCustomizationMusic(music: String) {
        _uiState.update { it.copy(seedCustomizationProfile = it.seedCustomizationProfile.copy(music = music)) }
    }
    fun updateCustomizationDisableBeeping(enabled: Boolean) {
        _uiState.update { it.copy(seedCustomizationProfile = it.seedCustomizationProfile.copy(disableBeeping = enabled)) }
    }

    fun dismissGenerationModal() {
        _uiState.update {
            it.copy(
                showGenerationModal = false,
                isGenerating = false,
                isCustomizingSeed = false,
                generationStep = null,
                generationError = null,
                generatedSeedId = null,
                generatedRomPath = null,
                generationSuccess = false,
                pendingSeedId = null,
                pendingCustomizeUrl = null,
                pendingUnheaderedBaseRomBytes = null,
                isLoading = false
            )
        }
    }

    fun generateRom(context: Context) {
        val state = _uiState.value
        if (!state.isBaseRomValid || state.baseRomUri.isNullOrEmpty()) {
            _uiState.update {
                it.copy(
                    showGenerationModal = true,
                    isGenerating = false,
                    isError = true,
                    errorMessage = "Base ROM is not configured or invalid. Please select a valid Super Metroid JU Base ROM first.",
                    generationError = "Base ROM is not configured or invalid. Please select a valid Super Metroid JU Base ROM first.",
                    generationStep = null
                )
            }
            return
        }

        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    showGenerationModal = true,
                    isGenerating = true,
                    isLoading = true,
                    isError = false,
                    errorMessage = null,
                    generationError = null,
                    generationSuccess = false,
                    isCustomizingSeed = false,
                    generationStep = GenerationStep.READING_BASE_ROM,
                    generatedSeedId = null,
                    generatedRomPath = null
                )
            }

            val baseRomUriStr = _uiState.value.baseRomUri
            val baseRomBytes = try {
                if (!baseRomUriStr.isNullOrEmpty()) {
                    val uri = Uri.parse(baseRomUriStr)
                    if (uri != null && (uri.scheme == "content" || uri.scheme == "file")) {
                        context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                    } else {
                        val file = File(baseRomUriStr)
                        if (file.exists()) file.readBytes()
                        else context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                    }
                } else null
            } catch (_: Exception) {
                null
            }

            if (baseRomBytes == null) {
                _uiState.update {
                    it.copy(
                        isGenerating = false,
                        isLoading = false,
                        isError = true,
                        errorMessage = "Base ROM not found or unreadable. Please re-select Base ROM.",
                        generationError = "Base ROM not found or unreadable. Please re-select Base ROM."
                    )
                }
                return@launch
            }

            _uiState.update {
                it.copy(generationStep = GenerationStep.GENERATING_SEED)
            }

            val step1Result = mapRandoRepository.randomizeSeedStep1(baseRomBytes, activeConfigProfile).getOrElse { error ->
                _uiState.update {
                    it.copy(
                        isGenerating = false,
                        isLoading = false,
                        isError = true,
                        errorMessage = error.localizedMessage ?: "Seed generation failed",
                        generationError = error.localizedMessage ?: "Seed generation failed"
                    )
                }
                return@launch
            }

            val favSprite = _uiState.value.favoriteSprite
            var profileForCustomization = if (activeConfigProfile.samusSprite == "samus_vanilla" && favSprite.isNotBlank()) {
                activeConfigProfile.copy(samusSprite = favSprite)
            } else {
                activeConfigProfile
            }

            val defCtrl = _uiState.value.defaultControls
            val isDefaultControls = profileForCustomization.controlShot == "X" &&
                    profileForCustomization.controlJump == "A" &&
                    profileForCustomization.controlDash == "B" &&
                    profileForCustomization.controlItemSelect == "Select" &&
                    profileForCustomization.controlItemCancel == "Y" &&
                    profileForCustomization.controlAngleUp == "R" &&
                    profileForCustomization.controlAngleDown == "L"

            if (isDefaultControls) {
                profileForCustomization = profileForCustomization.copy(
                    controlShot = defCtrl.shot,
                    controlJump = defCtrl.jump,
                    controlDash = defCtrl.dash,
                    controlItemSelect = defCtrl.itemSelect,
                    controlItemCancel = defCtrl.itemCancel,
                    controlAngleUp = defCtrl.angleUp,
                    controlAngleDown = defCtrl.angleDown
                )
            }

            _uiState.update {
                it.copy(
                    isGenerating = false,
                    isLoading = false,
                    isCustomizingSeed = true,
                    pendingSeedId = step1Result.seedId,
                    generatedSeedId = step1Result.seedId,
                    pendingCustomizeUrl = step1Result.customizeUrl,
                    pendingUnheaderedBaseRomBytes = step1Result.unheaderedBaseRomBytes,
                    seedCustomizationProfile = profileForCustomization,
                    generationStep = GenerationStep.PATCHING_ROM
                )
            }
        }
    }

    fun finalizeSeedCustomization(context: Context) {
        val state = _uiState.value
        val customizeUrl = state.pendingCustomizeUrl ?: return
        val unheaderedBytes = state.pendingUnheaderedBaseRomBytes ?: return
        val seedId = state.pendingSeedId ?: state.generatedSeedId ?: ""

        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isGenerating = true,
                    isLoading = true,
                    isCustomizingSeed = false,
                    generationStep = GenerationStep.PATCHING_ROM
                )
            }

            val patchedSfcBytes = mapRandoRepository.customizeSeedStep2(
                customizeUrl = customizeUrl,
                unheaderedBaseRomBytes = unheaderedBytes,
                settings = state.seedCustomizationProfile
            ).getOrElse { error ->
                _uiState.update {
                    it.copy(
                        isGenerating = false,
                        isLoading = false,
                        isError = true,
                        errorMessage = error.localizedMessage ?: "ROM customization step failed",
                        generationError = error.localizedMessage ?: "ROM customization step failed"
                    )
                }
                return@launch
            }

            _uiState.update {
                it.copy(generationStep = GenerationStep.SAVING_ROM)
            }

            val mapLayoutName = if (_uiState.value.isCustomMode) {
                "Custom: ${_uiState.value.currentCustomProfile.name}"
            } else {
                _uiState.value.selectedPreset ?: "Default"
            }

            try {
                val presetName = if (_uiState.value.isCustomMode) {
                    _uiState.value.currentCustomProfile.name.ifEmpty { "Custom" }
                } else {
                    _uiState.value.selectedPreset ?: "Default"
                }
                val sanitizedPreset = presetName.replace(Regex("[^a-zA-Z0-9_-]"), "_").trim('_').ifEmpty { "Preset" }
                val romFileName = "$sanitizedPreset-$seedId.sfc"
                val gameTitle = "$presetName-$seedId"

                var finalRomPath = ""
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    try {
                        val contentValues = android.content.ContentValues().apply {
                            put(MediaStore.MediaColumns.DISPLAY_NAME, romFileName)
                            put(MediaStore.MediaColumns.MIME_TYPE, "application/octet-stream")
                            put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
                        }
                        val uri = context.contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
                        if (uri != null) {
                            context.contentResolver.openOutputStream(uri)?.use { os ->
                                os.write(patchedSfcBytes)
                                os.flush()
                            }
                            finalRomPath = uri.toString()
                        }
                    } catch (e: Exception) {
                        safeLogE("Failed to write to MediaStore: ${e.message}", e)
                    }
                }

                if (finalRomPath.isEmpty()) {
                    val localRomFile = saveRomToLocalAppStorage(context, romFileName, patchedSfcBytes)
                    finalRomPath = localRomFile?.absolutePath ?: ""
                }

                val gameEntity = GameEntity(
                    title = gameTitle,
                    romPath = finalRomPath,
                    mapLayout = mapLayoutName,
                    skillAssumptions = state.seedCustomizationProfile.logicSkill.ifEmpty { "Medium" },
                    configProfileJson = com.maprandroid.data.local.ConfigProfileJsonAdapter.toJson(state.seedCustomizationProfile)
                )
                gameRepository.insert(gameEntity)

                _uiState.update {
                    it.copy(
                        isGenerating = false,
                        isLoading = false,
                        generationSuccess = true,
                        generationStep = GenerationStep.COMPLETE,
                        generatedRomPath = finalRomPath,
                        pendingSeedId = null,
                        pendingCustomizeUrl = null,
                        pendingUnheaderedBaseRomBytes = null,
                        isError = false
                    )
                }
            } catch (e: Exception) {
                safeLogE("Error saving generated ROM: ${e.message}", e)
                _uiState.update {
                    it.copy(
                        isGenerating = false,
                        isLoading = false,
                        isError = true,
                        errorMessage = "Error saving ROM: ${e.message}",
                        generationError = "Error saving ROM: ${e.message}"
                    )
                }
            }
        }
    }

    fun generateRom() {
        val state = _uiState.value
        if (!state.isBaseRomValid || state.baseRomUri.isNullOrEmpty()) {
            _uiState.update {
                it.copy(
                    showGenerationModal = true,
                    isGenerating = false,
                    isError = true,
                    errorMessage = "Base ROM is not configured or invalid. Please select a valid Super Metroid JU Base ROM first.",
                    generationError = "Base ROM is not configured or invalid. Please select a valid Super Metroid JU Base ROM first."
                )
            }
            return
        }

        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    showGenerationModal = true,
                    isGenerating = true,
                    isLoading = true,
                    isError = false,
                    errorMessage = null,
                    generationError = null,
                    generationStep = GenerationStep.GENERATING_SEED
                )
            }

            val result = if (_uiState.value.isCustomMode) {
                mapRandoRepository.generateCustomSeed(_uiState.value.currentCustomProfile)
            } else {
                val preset = _uiState.value.selectedPreset ?: "Default"
                mapRandoRepository.generateSeed(preset)
            }

            result.fold(
                onSuccess = { seedResponse ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            pendingSeedId = seedResponse.seedId,
                            generatedSeedId = seedResponse.seedId,
                            generationStep = GenerationStep.READING_BASE_ROM,
                            isError = false
                        )
                    }
                },
                onFailure = { error ->
                    _uiState.update {
                        it.copy(
                            isGenerating = false,
                            isLoading = false,
                            isError = true,
                            errorMessage = error.localizedMessage ?: "Seed generation failed",
                            generationError = error.localizedMessage ?: "Seed generation failed"
                        )
                    }
                }
            )
        }
    }

    fun getProfileForPreset(presetName: String): ConfigProfileEntity {
        return com.maprandroid.data.model.PresetCatalog.getProfileForPreset(
            presetName = presetName,
            savedProfiles = _uiState.value.savedProfiles
        )
    }

    val activeConfigProfile: ConfigProfileEntity
        get() {
            val baseProfile = if (_uiState.value.isCustomMode) {
                _uiState.value.currentCustomProfile
            } else {
                val presetName = _uiState.value.selectedPreset ?: "Default"
                getProfileForPreset(presetName)
            }

            val favSprite = _uiState.value.favoriteSprite
            val defCtrl = _uiState.value.defaultControls

            var adjusted = if (baseProfile.samusSprite == "samus_vanilla" && favSprite.isNotBlank()) {
                baseProfile.copy(samusSprite = favSprite)
            } else {
                baseProfile
            }

            val isDefaultControls = adjusted.controlShot == "X" &&
                    adjusted.controlJump == "A" &&
                    adjusted.controlDash == "B" &&
                    adjusted.controlItemSelect == "Select" &&
                    adjusted.controlItemCancel == "Y" &&
                    adjusted.controlAngleUp == "R" &&
                    adjusted.controlAngleDown == "L"

            if (isDefaultControls) {
                adjusted = adjusted.copy(
                    controlShot = defCtrl.shot,
                    controlJump = defCtrl.jump,
                    controlDash = defCtrl.dash,
                    controlItemSelect = defCtrl.itemSelect,
                    controlItemCancel = defCtrl.itemCancel,
                    controlAngleUp = defCtrl.angleUp,
                    controlAngleDown = defCtrl.angleDown
                )
            }

            return adjusted
        }

    fun downloadAndSaveRom(context: Context) {
        val seedId = _uiState.value.pendingSeedId ?: _uiState.value.generatedSeedId ?: return
        val baseRomUriStr = _uiState.value.baseRomUri
        val mapLayoutName = if (_uiState.value.isCustomMode) {
            "Custom: ${_uiState.value.currentCustomProfile.name}"
        } else {
            _uiState.value.selectedPreset ?: "Default"
        }

        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    showGenerationModal = true,
                    isGenerating = true,
                    isLoading = true,
                    isError = false,
                    errorMessage = null,
                    generationError = null,
                    generationStep = GenerationStep.READING_BASE_ROM
                )
            }

            val baseRomBytes = try {
                if (!baseRomUriStr.isNullOrEmpty()) {
                    val uri = Uri.parse(baseRomUriStr)
                    if (uri != null && (uri.scheme == "content" || uri.scheme == "file")) {
                        context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                    } else {
                        val file = File(baseRomUriStr)
                        if (file.exists()) file.readBytes()
                        else context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                    }
                } else null
            } catch (_: Exception) {
                null
            }

            if (baseRomBytes == null) {
                _uiState.update {
                    it.copy(
                        isGenerating = false,
                        isLoading = false,
                        isError = true,
                        errorMessage = "Base ROM not found or unreadable. Please re-select Base ROM.",
                        generationError = "Base ROM not found or unreadable. Please re-select Base ROM."
                    )
                }
                return@launch
            }

            _uiState.update { it.copy(generationStep = GenerationStep.PATCHING_ROM) }

            mapRandoRepository.generateAndCustomizeRom(baseRomBytes, activeConfigProfile).fold(
                onSuccess = { (customSeedId, patchedSfcBytes) ->
                    try {
                        val seedId = customSeedId.ifEmpty { "UNKNOWN" }
                        val patchedSha1 = RomPatcher.computeSha1(patchedSfcBytes)
                        safeLogI("Successfully generated and customized ROM via 2-step API for seed $seedId. Output size: ${patchedSfcBytes.size} bytes, SHA-1: $patchedSha1")

                        val baseSha1 = RomPatcher.computeSha1(if (baseRomBytes.size % 1024 == 512) baseRomBytes.copyOfRange(512, baseRomBytes.size) else baseRomBytes)
                        if (patchedSha1 == baseSha1) {
                            throw IllegalStateException("MapRando API returned an unmodified ROM.")
                        }

                        _uiState.update { it.copy(generationStep = GenerationStep.SAVING_ROM) }

                        val presetName = if (_uiState.value.isCustomMode) {
                            _uiState.value.currentCustomProfile.name.ifEmpty { "Custom" }
                        } else {
                            _uiState.value.selectedPreset ?: "Default"
                        }
                        val sanitizedPreset = presetName.replace(Regex("[^a-zA-Z0-9_-]"), "_").trim('_').ifEmpty { "Preset" }
                        val romFileName = "$sanitizedPreset-$seedId.sfc"
                        val gameTitle = "$presetName-$seedId"

                        var finalRomPath = ""
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                            try {
                                val contentValues = android.content.ContentValues().apply {
                                    put(MediaStore.MediaColumns.DISPLAY_NAME, romFileName)
                                    put(MediaStore.MediaColumns.MIME_TYPE, "application/octet-stream")
                                    put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
                                }
                                val uri = context.contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
                                if (uri != null) {
                                    context.contentResolver.openOutputStream(uri)?.use { os ->
                                        os.write(patchedSfcBytes)
                                        os.flush()
                                    }
                                    safeLogI("Wrote 2-step generated ROM bytes to SAF output stream $uri")
                                    finalRomPath = uri.toString()
                                }
                            } catch (e: Exception) {
                                safeLogE("Failed to write to MediaStore: ${e.message}", e)
                            }
                        }
                        
                        if (finalRomPath.isEmpty()) {
                            val localRomFile = saveRomToLocalAppStorage(context, romFileName, patchedSfcBytes)
                            finalRomPath = localRomFile?.absolutePath ?: ""
                        }

                        val gameEntity = GameEntity(
                            title = gameTitle,
                            romPath = finalRomPath,
                            mapLayout = mapLayoutName,
                            skillAssumptions = activeConfigProfile.logicSkill.ifEmpty { "Medium" },
                            configProfileJson = com.maprandroid.data.local.ConfigProfileJsonAdapter.toJson(activeConfigProfile)
                        )
                        gameRepository.insert(gameEntity)

                        _uiState.update {
                            it.copy(
                                isGenerating = false,
                                isLoading = false,
                                generationSuccess = true,
                                generationStep = GenerationStep.COMPLETE,
                                generatedRomPath = finalRomPath,
                                pendingSeedId = null,
                                isError = false
                            )
                        }
                    } catch (e: Exception) {
                        safeLogE("Error saving 2-step generated ROM: ${e.message}", e)
                        _uiState.update {
                            it.copy(
                                isGenerating = false,
                                isLoading = false,
                                isError = true,
                                errorMessage = "Failed to save ROM: ${e.localizedMessage}",
                                generationError = "Failed to save ROM: ${e.localizedMessage}"
                            )
                        }
                    }
                },
                onFailure = { generateError ->
                    safeLogE("2-step ROM generation and customization failed: ${generateError.message}", generateError)
                    _uiState.update {
                        it.copy(
                            isGenerating = false,
                            isLoading = false,
                            isError = true,
                            errorMessage = generateError.localizedMessage ?: generateError.message ?: "ROM generation failed",
                            generationError = generateError.localizedMessage ?: generateError.message ?: "ROM generation failed"
                        )
                    }
                }
            )
        }
    }

    private fun safeLogI(msg: String) {
        try {
            android.util.Log.i("ConfigViewModel", msg)
        } catch (_: Throwable) {
            println("[ConfigViewModel] $msg")
        }
    }



    private fun safeLogE(msg: String, t: Throwable? = null) {
        try {
            if (t != null) android.util.Log.e("ConfigViewModel", msg, t)
            else android.util.Log.e("ConfigViewModel", msg)
        } catch (_: Throwable) {
            println("[ConfigViewModel ERROR] $msg: ${t?.message}")
        }
    }

    private fun saveRomToLocalAppStorage(context: Context, fileName: String, romBytes: ByteArray): File? {
        return try {
            val romsDir = try {
                context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS) ?: File(context.filesDir, Environment.DIRECTORY_DOWNLOADS)
            } catch (_: Exception) {
                File(context.filesDir, Environment.DIRECTORY_DOWNLOADS)
            }
            if (!romsDir.exists()) {
                romsDir.mkdirs()
            }
            val targetName = if (fileName.endsWith(".sfc")) fileName else "$fileName.sfc"
            val localFile = File(romsDir, targetName)
            localFile.writeBytes(romBytes)
            safeLogI("Saved local ROM file: ${localFile.absolutePath} (${romBytes.size} bytes)")
            localFile
        } catch (e: Exception) {
            safeLogE("Failed to save local ROM file: ${e.message}", e)
            null
        }
    }

    fun clearPendingSeedId() {
        _uiState.update { it.copy(pendingSeedId = null, isLoading = false) }
    }

    fun dismissSuccess() {
        _uiState.update { it.copy(generationSuccess = false) }
    }

    fun clearError() {
        _uiState.update { it.copy(isError = false, errorMessage = null) }
    }
}

class ConfigViewModelFactory(
    private val mapRandoRepository: MapRandoRepository,
    private val gameRepository: GameRepository,
    private val configProfileRepository: ConfigProfileRepository? = null,
    private val settingsRepository: SettingsRepository? = null
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ConfigViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return ConfigViewModel(
                mapRandoRepository,
                gameRepository,
                configProfileRepository,
                settingsRepository
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
