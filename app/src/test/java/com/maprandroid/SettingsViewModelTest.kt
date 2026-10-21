package com.maprandroid

import android.content.Context
import android.content.ContextWrapper
import com.maprandroid.data.local.SettingsRepository
import com.maprandroid.ui.settings.SettingsViewModel
import com.maprandroid.util.EmulatorInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

@OptIn(ExperimentalCoroutinesApi::class)
class SettingsViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    @get:Rule
    val tempFolder = TemporaryFolder()

    private class FakeSettingsRepository(context: Context) : SettingsRepository(context) {
        val emulatorPackageFlow = MutableStateFlow<String?>(null)
        val uriFlow = MutableStateFlow<String?>(null)
        val nameFlow = MutableStateFlow<String?>(null)
        val statusFlow = MutableStateFlow<String?>("No Base ROM selected")
        val validFlow = MutableStateFlow(false)
        val savedSpritesFlow = MutableStateFlow<List<String>>(emptyList())
        val favoriteSpriteFlow = MutableStateFlow("samus_vanilla")

        override val emulatorPackageName: Flow<String?> get() = emulatorPackageFlow
        override val baseRomUri: Flow<String?> get() = uriFlow
        override val baseRomName: Flow<String?> get() = nameFlow
        override val baseRomStatus: Flow<String?> get() = statusFlow
        override val isBaseRomValid: Flow<Boolean> get() = validFlow
        override val savedSprites: Flow<List<String>> get() = savedSpritesFlow
        override val favoriteSprite: Flow<String> get() = favoriteSpriteFlow

        override suspend fun saveFavoriteSprite(sprite: String) {
            favoriteSpriteFlow.value = sprite
        }

        override suspend fun saveSprites(sprites: List<String>) {
            savedSpritesFlow.value = sprites
        }

        override suspend fun saveEmulatorPackageName(packageName: String) {
            emulatorPackageFlow.value = packageName
        }

        override suspend fun saveBaseRomInfo(uri: String, name: String, status: String, isValid: Boolean) {
            uriFlow.value = uri
            nameFlow.value = name
            statusFlow.value = status
            validFlow.value = isValid
        }

        override suspend fun clearBaseRom() {
            uriFlow.value = null
            nameFlow.value = null
            statusFlow.value = "No Base ROM selected"
            validFlow.value = false
        }

        override fun getAvailableEmulators(): List<EmulatorInfo> {
            return listOf(EmulatorInfo("Snes9x EX+", "com.snes9x.ex"))
        }
    }

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun saveEmulatorPackageName_updatesSelectedEmulatorFlow() = runTest {
        val mockContext = object : ContextWrapper(null) {
            override fun getFilesDir() = tempFolder.root
            override fun getApplicationContext() = this
        }

        val repository = FakeSettingsRepository(mockContext)
        val viewModel = SettingsViewModel(repository, mockContext)

        backgroundScope.launch { viewModel.selectedEmulatorPackage.collect {} }
        testScheduler.advanceUntilIdle()

        viewModel.saveEmulatorPackageName("com.snes9x.ex")
        testScheduler.advanceUntilIdle()

        assertEquals("com.snes9x.ex", viewModel.selectedEmulatorPackage.value)
    }

    @Test
    fun clearBaseRom_clearsBaseRomInfo() = runTest {
        val mockContext = object : ContextWrapper(null) {
            override fun getFilesDir() = tempFolder.root
            override fun getApplicationContext() = this
        }

        val repository = FakeSettingsRepository(mockContext)
        repository.saveBaseRomInfo(
            uri = "content://roms/1",
            name = "SuperMetroid.sfc",
            status = "JU 1.0 Valid Headerless",
            isValid = true
        )

        val viewModel = SettingsViewModel(repository, mockContext)

        backgroundScope.launch { viewModel.baseRomName.collect {} }
        backgroundScope.launch { viewModel.isBaseRomValid.collect {} }
        testScheduler.advanceUntilIdle()

        assertEquals("SuperMetroid.sfc", viewModel.baseRomName.value)
        assertTrue(viewModel.isBaseRomValid.value)

        viewModel.clearBaseRom()
        testScheduler.advanceUntilIdle()

        assertFalse(viewModel.isBaseRomValid.value)
    }

    @Test
    fun updateOfficialPresets_updatesPresetStatus() = runTest {
        val mockContext = object : ContextWrapper(null) {
            override fun getFilesDir() = tempFolder.root
            override fun getApplicationContext() = this
        }

        val repository = FakeSettingsRepository(mockContext)
        val fakeMapRandoRepo = object : com.maprandroid.data.repository.MapRandoRepository(com.maprandroid.data.remote.NetworkModule.mapRandoApiService) {
            override suspend fun fetchAndUpdateOfficialPresets(): Result<List<String>> {
                com.maprandroid.data.model.PresetCatalog.updateOfficialPresets(listOf("Default", "Wild"))
                return Result.success(listOf("Default", "Wild"))
            }
        }
        val viewModel = SettingsViewModel(repository, mockContext, fakeMapRandoRepo)

        viewModel.updateOfficialPresets()
        testScheduler.advanceUntilIdle()

        assertNotNull(viewModel.presetUpdateStatus.value)
        assertFalse(viewModel.isUpdatingPresets.value)
    }

    @Test
    fun updateSpriteList_updatesSpriteStatusAndSavesSprites() = runTest {
        val mockContext = object : ContextWrapper(null) {
            override fun getFilesDir() = tempFolder.root
            override fun getApplicationContext() = this
        }

        val repository = FakeSettingsRepository(mockContext)
        val fakeMapRandoRepo = object : com.maprandroid.data.repository.MapRandoRepository(com.maprandroid.data.remote.NetworkModule.mapRandoApiService) {
            override suspend fun fetchAndUpdateSpriteList(githubUrl: String): Result<List<String>> {
                val sprites = listOf("samus_vanilla", "samus_dread", "samus_fusion")
                com.maprandroid.data.model.SpriteCatalog.updateSprites(sprites)
                return Result.success(sprites)
            }
        }
        val viewModel = SettingsViewModel(repository, mockContext, fakeMapRandoRepo)

        viewModel.updateSpriteList()
        testScheduler.advanceUntilIdle()

        assertNotNull(viewModel.spriteUpdateStatus.value)
        assertTrue(viewModel.spriteUpdateStatus.value!!.contains("3"))
        assertFalse(viewModel.isUpdatingSprites.value)
        assertEquals(3, repository.savedSpritesFlow.value.size)
    }

    @Test
    fun saveFavoriteSprite_updatesFavoriteSpriteFlow() = runTest {
        val mockContext = object : ContextWrapper(null) {
            override fun getFilesDir() = tempFolder.root
            override fun getApplicationContext() = this
        }

        val repository = FakeSettingsRepository(mockContext)
        val viewModel = SettingsViewModel(repository, mockContext)

        backgroundScope.launch { viewModel.favoriteSprite.collect {} }
        testScheduler.advanceUntilIdle()

        assertEquals("samus_vanilla", viewModel.favoriteSprite.value)

        viewModel.saveFavoriteSprite("samus_dread")
        testScheduler.advanceUntilIdle()

        assertEquals("samus_dread", viewModel.favoriteSprite.value)
    }
}
