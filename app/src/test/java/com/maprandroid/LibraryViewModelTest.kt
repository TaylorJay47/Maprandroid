package com.maprandroid

import android.content.ContextWrapper
import com.maprandroid.data.local.GameDao
import com.maprandroid.data.local.GameEntity
import com.maprandroid.data.local.SettingsRepository
import com.maprandroid.data.repository.GameRepository
import com.maprandroid.ui.library.LibraryViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

@OptIn(ExperimentalCoroutinesApi::class)
class LibraryViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    @get:Rule
    val tempFolder = TemporaryFolder()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun deleteGame_deletesFileFromStorageAndRemovesFromDao() = runTest {
        val testFile = tempFolder.newFile("SM_MapRando_TEST.sfc")
        testFile.writeBytes(ByteArray(100) { 0x12 })
        assertTrue("Test file should exist initially", testFile.exists())

        val gamesList = mutableListOf(
            GameEntity(id = 1, title = "Seed: TEST", romPath = testFile.absolutePath, mapLayout = "Default", skillAssumptions = "Medium")
        )

        val mockDao = object : GameDao {
            override fun getAllGames() = flowOf(gamesList.toList())
            override suspend fun getGameById(gameId: Int) = gamesList.find { it.id == gameId }
            override suspend fun insertGame(game: GameEntity) { gamesList.add(game) }
            override suspend fun deleteGame(gameId: Int) { gamesList.removeAll { it.id == gameId } }
            override suspend fun updateGameCompletion(gameId: Int, isCompleted: Boolean) {
                val index = gamesList.indexOfFirst { it.id == gameId }
                if (index != -1) {
                    gamesList[index] = gamesList[index].copy(isCompleted = isCompleted)
                }
            }
        }

        val gameRepo = GameRepository(mockDao)
        val mockContext = object : ContextWrapper(null) {
            override fun getFilesDir() = tempFolder.root
            override fun getApplicationContext() = this
        }

        val viewModel = LibraryViewModel(gameRepo, SettingsRepository(mockContext))
        testScheduler.advanceUntilIdle()

        viewModel.deleteGame(mockContext, gamesList.first())
        testScheduler.advanceUntilIdle()

        // Verify storage file was deleted
        assertFalse("ROM file should be deleted from storage", testFile.exists())

        // Verify game entry was deleted from repo
        assertTrue("Game entity list in DAO should be empty", gamesList.isEmpty())
    }

    @Test
    fun toggleGameCompleted_updatesCompletionStatus() = runTest {
        val gamesList = mutableListOf(
            GameEntity(id = 1, title = "Seed: TEST", romPath = "path", mapLayout = "Default", skillAssumptions = "Expert", isCompleted = false)
        )

        val mockDao = object : GameDao {
            override fun getAllGames() = flowOf(gamesList.toList())
            override suspend fun getGameById(gameId: Int) = gamesList.find { it.id == gameId }
            override suspend fun insertGame(game: GameEntity) { gamesList.add(game) }
            override suspend fun deleteGame(gameId: Int) { gamesList.removeAll { it.id == gameId } }
            override suspend fun updateGameCompletion(gameId: Int, isCompleted: Boolean) {
                val index = gamesList.indexOfFirst { it.id == gameId }
                if (index != -1) {
                    gamesList[index] = gamesList[index].copy(isCompleted = isCompleted)
                }
            }
        }

        val gameRepo = GameRepository(mockDao)
        val mockContext = object : ContextWrapper(null) {
            override fun getFilesDir() = tempFolder.root
            override fun getApplicationContext() = this
        }

        val viewModel = LibraryViewModel(gameRepo, SettingsRepository(mockContext))
        testScheduler.advanceUntilIdle()

        viewModel.toggleGameCompleted(1)
        testScheduler.advanceUntilIdle()

        assertTrue("Game should be marked as completed", gamesList.first().isCompleted)
    }
}
