package com.maprandroid.ui.library

import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.maprandroid.data.local.GameEntity
import com.maprandroid.data.local.SettingsRepository
import com.maprandroid.data.repository.GameRepository
import com.maprandroid.util.EmulatorInfo
import com.maprandroid.util.EmulatorLauncher
import java.io.File
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class LibraryUiState(
    val games: List<GameEntity> = emptyList(),
    val emulatorPackageName: String? = null,
    val isLoading: Boolean = true
)

class LibraryViewModel(
    private val gameRepository: GameRepository,
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    val uiState: StateFlow<LibraryUiState> = combine(
        gameRepository.allGames,
        settingsRepository.emulatorPackageName
    ) { games, emulatorPackageName ->
        LibraryUiState(
            games = games,
            emulatorPackageName = emulatorPackageName,
            isLoading = false
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = LibraryUiState(isLoading = true)
    )

    fun getAvailableEmulators(): List<EmulatorInfo> {
        return settingsRepository.getAvailableEmulators()
    }

    fun saveEmulatorPackageName(packageName: String) {
        viewModelScope.launch {
            settingsRepository.saveEmulatorPackageName(packageName)
        }
    }

    fun toggleGameCompleted(gameId: Int) {
        viewModelScope.launch {
            val game = uiState.value.games.find { it.id == gameId }
                ?: gameRepository.getGameById(gameId)
            if (game != null) {
                gameRepository.setGameCompleted(gameId, !game.isCompleted)
            }
        }
    }

    fun deleteGame(context: Context, game: GameEntity) {
        viewModelScope.launch {
            deleteRomStorageFiles(context, game.romPath, game.title)
            gameRepository.delete(game.id)
        }
    }

    fun deleteGame(gameId: Int, context: Context? = null) {
        viewModelScope.launch {
            val game = uiState.value.games.find { it.id == gameId }
                ?: gameRepository.getGameById(gameId)
            if (game != null && context != null) {
                deleteRomStorageFiles(context, game.romPath, game.title)
            }
            gameRepository.delete(gameId)
        }
    }

    private fun deleteRomStorageFiles(context: Context, romPath: String, title: String? = null) {
        if (romPath.isBlank()) return

        fun safeLogI(msg: String) {
            try { android.util.Log.i("LibraryViewModel", msg) } catch (_: Throwable) { println("[LibraryViewModel] $msg") }
        }
        fun safeLogE(msg: String, t: Throwable? = null) {
            try { if (t != null) android.util.Log.e("LibraryViewModel", msg, t) else android.util.Log.e("LibraryViewModel", msg) } catch (_: Throwable) { println("[LibraryViewModel ERR] $msg") }
        }

        if (romPath.startsWith("content://", ignoreCase = true)) {
            try {
                val uri = Uri.parse(romPath)
                val rows = context.contentResolver.delete(uri, null, null)
                safeLogI("Deleted content URI $romPath (rows affected: $rows)")
            } catch (e: Exception) {
                safeLogE("Failed to delete content URI $romPath: ${e.message}", e)
            }
        }

        try {
            val resolvedFile = EmulatorLauncher.resolveRomFile(context, romPath)
            if (resolvedFile != null && resolvedFile.exists()) {
                val deleted = resolvedFile.delete()
                safeLogI("Deleted resolved ROM file ${resolvedFile.absolutePath}: $deleted")
            }
        } catch (e: Exception) {
            safeLogE("Failed to delete resolved ROM file: ${e.message}", e)
        }

        try {
            val cleanPath = if (romPath.startsWith("file://", ignoreCase = true)) {
                Uri.parse(romPath).path ?: ""
            } else {
                romPath
            }
            if (cleanPath.isNotEmpty()) {
                val directFile = File(cleanPath)
                if (directFile.exists()) {
                    val deleted = directFile.delete()
                    safeLogI("Deleted direct ROM file ${directFile.absolutePath}: $deleted")
                }
            }
        } catch (e: Exception) {
            safeLogE("Failed to delete direct ROM file: ${e.message}", e)
        }

        val fileNameCandidate = try {
            val fName = File(romPath).name
            if (fName.endsWith(".sfc", ignoreCase = true)) fName
            else {
                val seedId = title?.removePrefix("Seed:")?.trim() ?: ""
                if (seedId.isNotEmpty()) "SM_MapRando_$seedId.sfc" else ""
            }
        } catch (_: Exception) { "" }

        if (fileNameCandidate.isNotEmpty()) {
            val searchDirs = listOfNotNull(
                try { context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS) } catch (_: Exception) { null },
                try { Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS) } catch (_: Exception) { null },
                try { context.filesDir } catch (_: Exception) { null },
                try { File(context.filesDir, Environment.DIRECTORY_DOWNLOADS) } catch (_: Exception) { null }
            )

            for (dir in searchDirs) {
                try {
                    val targetFile = File(dir, fileNameCandidate)
                    if (targetFile.exists()) {
                        val deleted = targetFile.delete()
                        safeLogI("Deleted target ROM file ${targetFile.absolutePath}: $deleted")
                    }
                } catch (_: Exception) {}
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                try {
                    val selection = "${MediaStore.MediaColumns.DISPLAY_NAME} = ?"
                    val selectionArgs = arrayOf(fileNameCandidate)
                    val rows = context.contentResolver.delete(
                        MediaStore.Downloads.EXTERNAL_CONTENT_URI,
                        selection,
                        selectionArgs
                    )
                    safeLogI("Deleted MediaStore entry for $fileNameCandidate (rows affected: $rows)")
                } catch (e: Exception) {
                    safeLogE("Failed MediaStore delete for $fileNameCandidate: ${e.message}", e)
                }
            }
        }
    }
}

class LibraryViewModelFactory(
    private val gameRepository: GameRepository,
    private val settingsRepository: SettingsRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(LibraryViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return LibraryViewModel(gameRepository, settingsRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
