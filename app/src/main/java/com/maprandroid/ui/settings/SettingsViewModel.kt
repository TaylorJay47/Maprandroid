package com.maprandroid.ui.settings

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.maprandroid.data.local.SettingsRepository
import com.maprandroid.util.BaseRomValidator
import com.maprandroid.util.EmulatorInfo
import com.maprandroid.util.EmulatorLauncher
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class SettingsUiState(
    val selectedEmulatorPackage: String? = null,
    val availableEmulators: List<EmulatorInfo> = emptyList(),
    val baseRomName: String? = null,
    val baseRomStatus: String = "No Base ROM selected",
    val isBaseRomValid: Boolean = false
)

class SettingsViewModel(
    private val settingsRepository: SettingsRepository,
    private val context: Context,
    private val mapRandoRepository: com.maprandroid.data.repository.MapRandoRepository = com.maprandroid.data.repository.MapRandoRepository()
) : ViewModel() {

    private val _availableEmulators = MutableStateFlow<List<EmulatorInfo>>(emptyList())
    val availableEmulators: StateFlow<List<EmulatorInfo>> = _availableEmulators.asStateFlow()

    private val _isUpdatingPresets = MutableStateFlow(false)
    val isUpdatingPresets: StateFlow<Boolean> = _isUpdatingPresets.asStateFlow()

    private val _presetUpdateStatus = MutableStateFlow<String?>(null)
    val presetUpdateStatus: StateFlow<String?> = _presetUpdateStatus.asStateFlow()

    private val _isUpdatingSprites = MutableStateFlow(false)
    val isUpdatingSprites: StateFlow<Boolean> = _isUpdatingSprites.asStateFlow()

    private val _spriteUpdateStatus = MutableStateFlow<String?>(null)
    val spriteUpdateStatus: StateFlow<String?> = _spriteUpdateStatus.asStateFlow()

    val selectedEmulatorPackage: StateFlow<String?> = settingsRepository.emulatorPackageName
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )

    val baseRomName: StateFlow<String?> = settingsRepository.baseRomName
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )

    val baseRomStatus: StateFlow<String?> = settingsRepository.baseRomStatus
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = "No Base ROM selected"
        )

    val isBaseRomValid: StateFlow<Boolean> = settingsRepository.isBaseRomValid
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = false
        )

    val favoriteSprite: StateFlow<String> = settingsRepository.favoriteSprite
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = "samus_vanilla"
        )

    val defaultControls: StateFlow<com.maprandroid.data.local.DefaultControls> = settingsRepository.defaultControls
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = com.maprandroid.data.local.DefaultControls()
        )

    val availableSprites: StateFlow<List<String>> = settingsRepository.savedSprites
        .map { sprites ->
            if (sprites.isNotEmpty()) {
                com.maprandroid.data.model.SpriteCatalog.updateSprites(sprites)
            }
            com.maprandroid.data.model.SpriteCatalog.availableSprites
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = com.maprandroid.data.model.SpriteCatalog.availableSprites
        )

    init {
        loadAvailableEmulators()
        observeSavedSprites()
    }

    private fun observeSavedSprites() {
        viewModelScope.launch {
            settingsRepository.savedSprites.collect { sprites ->
                if (sprites.isNotEmpty()) {
                    com.maprandroid.data.model.SpriteCatalog.updateSprites(sprites)
                }
            }
        }
    }

    fun loadAvailableEmulators() {
        _availableEmulators.value = settingsRepository.getAvailableEmulators()
    }

    fun getAvailableEmulators(): List<EmulatorInfo> {
        val emulators = settingsRepository.getAvailableEmulators()
        _availableEmulators.value = emulators
        return emulators
    }

    fun saveEmulatorPackageName(packageName: String?) {
        viewModelScope.launch {
            if (packageName == null) {
                settingsRepository.saveEmulatorPackageName("")
            } else {
                settingsRepository.saveEmulatorPackageName(packageName)
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

                settingsRepository.saveBaseRomInfo(
                    uri = uri.toString(),
                    name = fileName,
                    status = validation.statusMessage,
                    isValid = validation.isValid
                )
            } catch (e: Exception) {
                settingsRepository.saveBaseRomInfo(
                    uri = uri.toString(),
                    name = "Invalid Base ROM",
                    status = "Error reading Base ROM: ${e.localizedMessage}",
                    isValid = false
                )
            }
        }
    }

    fun clearBaseRom() {
        viewModelScope.launch {
            settingsRepository.clearBaseRom()
        }
    }

    fun updateOfficialPresets() {
        viewModelScope.launch {
            _isUpdatingPresets.value = true
            _presetUpdateStatus.value = "Fetching current presets from maprando.com..."

            val result = mapRandoRepository.fetchAndUpdateOfficialPresets()
            result.fold(
                onSuccess = { presets ->
                    _isUpdatingPresets.value = false
                    _presetUpdateStatus.value = "Successfully updated ${presets.size} official presets from maprando.com!"
                },
                onFailure = { error ->
                    _isUpdatingPresets.value = false
                    _presetUpdateStatus.value = "Failed to update presets: ${error.localizedMessage ?: "Network error"}"
                }
            )
        }
    }

    fun updateSpriteList() {
        viewModelScope.launch {
            _isUpdatingSprites.value = true
            _spriteUpdateStatus.value = "Fetching current sprites from MapRandoSprites..."

            val result = mapRandoRepository.fetchAndUpdateSpriteList()
            result.fold(
                onSuccess = { sprites ->
                    _isUpdatingSprites.value = false
                    _spriteUpdateStatus.value = "Successfully updated ${sprites.size} Samus sprites!"
                    settingsRepository.saveSprites(sprites)
                },
                onFailure = { error ->
                    _isUpdatingSprites.value = false
                    _spriteUpdateStatus.value = "Failed to update sprites: ${error.localizedMessage ?: "Network error"}"
                }
            )
        }
    }

    fun saveFavoriteSprite(sprite: String) {
        viewModelScope.launch {
            settingsRepository.saveFavoriteSprite(sprite)
        }
    }

    fun updateDefaultControl(action: String, button: String) {
        viewModelScope.launch {
            val current = defaultControls.value
            val updated = when (action) {
                "shot" -> current.copy(shot = button)
                "jump" -> current.copy(jump = button)
                "dash" -> current.copy(dash = button)
                "itemSelect" -> current.copy(itemSelect = button)
                "itemCancel" -> current.copy(itemCancel = button)
                "angleUp" -> current.copy(angleUp = button)
                "angleDown" -> current.copy(angleDown = button)
                else -> current
            }
            settingsRepository.saveDefaultControls(updated)
        }
    }
}

class SettingsViewModelFactory(
    private val settingsRepository: SettingsRepository,
    private val context: Context
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(SettingsViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return SettingsViewModel(settingsRepository, context) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

