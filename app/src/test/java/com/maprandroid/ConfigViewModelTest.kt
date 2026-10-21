package com.maprandroid

import android.content.ContentResolver
import android.content.ContextWrapper
import android.net.Uri
import com.maprandroid.data.local.ConfigProfileDao
import com.maprandroid.data.local.ConfigProfileEntity
import com.maprandroid.data.local.GameDao
import com.maprandroid.data.local.GameEntity
import com.maprandroid.data.remote.GenerateSeedRequest
import com.maprandroid.data.remote.MapRandoApiService
import com.maprandroid.data.remote.SeedDetailsResponse
import com.maprandroid.data.repository.ConfigProfileRepository
import com.maprandroid.data.repository.GameRepository
import com.maprandroid.data.repository.MapRandoRepository
import com.maprandroid.ui.config.ConfigUiState
import com.maprandroid.ui.config.ConfigViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.ResponseBody
import okhttp3.ResponseBody.Companion.toResponseBody
import retrofit2.Response
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

@OptIn(ExperimentalCoroutinesApi::class)
class ConfigViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    
    private val mockApiService = object : MapRandoApiService {
        override suspend fun getSeedDetails(seedId: String): SeedDetailsResponse = SeedDetailsResponse(seedId = seedId, hash = "hash")
        override suspend fun getApiSeedDetails(seedId: String): SeedDetailsResponse = SeedDetailsResponse(seedId = seedId, hash = "hash")
        override suspend fun randomizeSeed(request: GenerateSeedRequest): SeedDetailsResponse {
            val seedId = if (request.customSettings != null) "CUSTOM_SEED_999" else "SEED_123"
            return SeedDetailsResponse(seedId = seedId, hash = "hash")
        }
        override suspend fun randomizeApiSeed(request: GenerateSeedRequest): SeedDetailsResponse = randomizeSeed(request)
        override suspend fun generateSeed(request: GenerateSeedRequest): SeedDetailsResponse = SeedDetailsResponse(seedId = "SEED_123", hash = "hash")
        override suspend fun randomizeMultipart(spoilerToken: RequestBody, settingsJson: RequestBody, rom: MultipartBody.Part): Response<ResponseBody> {
            val jsonStr = """{"seed_url": "/seed/TEST_SEED_123/"}"""
            val body = jsonStr.toByteArray().toResponseBody("application/json".toMediaTypeOrNull())
            return Response.success(body)
        }
        override suspend fun customizeRom(customizeUrl: String, body: RequestBody): Response<ResponseBody> {
            val patchedRomBytes = ByteArray(3145728) { 0x55.toByte() }
            val responseBody = patchedRomBytes.toResponseBody("application/octet-stream".toMediaTypeOrNull())
            return Response.success(responseBody)
        }
        override suspend fun downloadRom(seedId: String): ResponseBody = throw RuntimeException("Download unavailable in mock")
        override suspend fun downloadPatch(seedId: String): ResponseBody = throw RuntimeException("Download unavailable in mock")
        override suspend fun downloadApiPatch(seedId: String): ResponseBody = throw RuntimeException("Download unavailable in mock")
        override suspend fun downloadSeedDownload(seedId: String): ResponseBody = throw RuntimeException("Download unavailable in mock")
    }

    private val mockGameDao = object : GameDao {
        override fun getAllGames() = flowOf(emptyList<GameEntity>())
        override suspend fun getGameById(gameId: Int): GameEntity? = null
        override suspend fun insertGame(game: GameEntity) {}
        override suspend fun deleteGame(gameId: Int) {}
        override suspend fun updateGameCompletion(gameId: Int, isCompleted: Boolean) {}
    }

    private val mockProfilesState = MutableStateFlow<List<ConfigProfileEntity>>(emptyList())

    private val mockConfigProfileDao = object : ConfigProfileDao {
        override fun getAllProfiles() = mockProfilesState
        override suspend fun getProfileById(id: Int): ConfigProfileEntity? = mockProfilesState.value.find { it.id == id }
        override suspend fun insertProfile(profile: ConfigProfileEntity): Long {
            val newId = (mockProfilesState.value.maxOfOrNull { it.id } ?: 0) + 1
            val created = profile.copy(id = newId)
            mockProfilesState.value = mockProfilesState.value + created
            return newId.toLong()
        }
        override suspend fun updateProfile(profile: ConfigProfileEntity) {
            mockProfilesState.value = mockProfilesState.value.map { if (it.id == profile.id) profile else it }
        }
        override suspend fun deleteProfile(profile: ConfigProfileEntity) {
            mockProfilesState.value = mockProfilesState.value.filter { it.id != profile.id }
        }
        override suspend fun deleteProfileById(id: Int) {
            mockProfilesState.value = mockProfilesState.value.filter { it.id != id }
        }
    }

    private lateinit var repository: MapRandoRepository
    private lateinit var gameRepository: GameRepository
    private lateinit var profileRepository: ConfigProfileRepository
    private lateinit var viewModel: ConfigViewModel

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        mockProfilesState.value = emptyList()
        repository = MapRandoRepository(mockApiService)
        gameRepository = GameRepository(mockGameDao)
        profileRepository = ConfigProfileRepository(mockConfigProfileDao)
        viewModel = ConfigViewModel(repository, gameRepository, profileRepository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun presets_areLoadedSuccessfully() = runTest {
        testScheduler.advanceUntilIdle()
        val state = viewModel.uiState.value
        assertFalse(state.presets.isEmpty())
        assertNotNull(state.selectedPreset)
        assertEquals("Default", state.selectedPreset)
    }

    @Test
    fun selectPreset_updatesState() = runTest {
        testScheduler.advanceUntilIdle()
        viewModel.selectPreset("Wild")
        assertEquals("Wild", viewModel.uiState.value.selectedPreset)
    }

    @Test
    fun generateRom_withoutBaseRom_showsError() = runTest {
        testScheduler.advanceUntilIdle()
        viewModel.generateRom()
        testScheduler.advanceUntilIdle()
        val state = viewModel.uiState.value
        assertTrue(state.isError)
        assertNotNull(state.errorMessage)
        assertNull(state.pendingSeedId)
    }

    @Test
    fun generateRom_presetMode_withBaseRom_setsPendingSeedId() = runTest {
        testScheduler.advanceUntilIdle()
        val dummyRomUri = "content://com.android.providers.downloads.documents/document/1"
        val stateWithRom = viewModel.uiState.value.copy(
            baseRomUri = dummyRomUri,
            isBaseRomValid = true
        )
        val field = ConfigViewModel::class.java.getDeclaredField("_uiState")
        field.isAccessible = true
        @Suppress("UNCHECKED_CAST")
        (field.get(viewModel) as MutableStateFlow<ConfigUiState>).value = stateWithRom

        viewModel.generateRom()
        testScheduler.advanceUntilIdle()
        val state = viewModel.uiState.value
        assertEquals("SEED_123", state.pendingSeedId)
        assertFalse(state.isLoading)
    }

    @Test
    fun setCustomMode_switchesMode() = runTest {
        testScheduler.advanceUntilIdle()
        viewModel.setCustomMode(true)
        assertTrue(viewModel.uiState.value.isCustomMode)
        viewModel.setCustomMode(false)
        assertFalse(viewModel.uiState.value.isCustomMode)
    }

    @Test
    fun customOptions_updateStateCorrectly() = runTest {
        testScheduler.advanceUntilIdle()
        viewModel.updateLogicSkill("Expert")
        viewModel.updateItemProgression("Slow")
        viewModel.updateItemPool("Chozo")
        viewModel.updateMapRandomization("Full Area")
        viewModel.updateQolFastElevators(false)
        viewModel.updateDifficultyResource("Harsh")
        viewModel.updateDamageMultiplier("2.0x")

        val current = viewModel.uiState.value.currentCustomProfile
        assertEquals("Expert", current.logicSkill)
        assertEquals("Slow", current.itemProgression)
        assertEquals("Chozo", current.itemPool)
        assertEquals("Full Area", current.mapRandomization)
        assertFalse(current.qolFastElevators)
        assertEquals("Harsh", current.difficultyResource)
        assertEquals("2.0x", current.damageMultiplier)
        assertNull(viewModel.uiState.value.selectedProfileId)
    }

    @Test
    fun expandedTechAndSkillOptions_updateStateCorrectly() = runTest {
        testScheduler.advanceUntilIdle()
        viewModel.updateTechWallJump(false)
        viewModel.updateTechIBJ(true)
        viewModel.updateTechShinespark(true)
        viewModel.updateTechShortCharge(true)
        viewModel.updateTechMockball(true)
        viewModel.updateTechSpringBall(true)
        viewModel.updateTechMidAirMorph(true)
        viewModel.updateTechDamageBoosts(true)
        viewModel.updateLogicOptions("Strict")

        val current = viewModel.uiState.value.currentCustomProfile
        assertFalse(current.techWallJump)
        assertTrue(current.techIBJ)
        assertTrue(current.techShinespark)
        assertTrue(current.techShortCharge)
        assertTrue(current.techMockball)
        assertTrue(current.techSpringBall)
        assertTrue(current.techMidAirMorph)
        assertTrue(current.techDamageBoosts)
        assertEquals("Strict", current.logicOptions)
    }

    @Test
    fun expandedProgressionAndPoolOptions_updateStateCorrectly() = runTest {
        testScheduler.advanceUntilIdle()
        viewModel.updateMajorMinorDistribution("Shuffled")
        viewModel.updateScavengerHunt(true)
        viewModel.updateScavengerRequiredCount(15)
        viewModel.updateChozoHunt(true)
        viewModel.updateSuperMissileCount(20)
        viewModel.updatePowerBombCount(12)
        viewModel.updateStartMorph(true)
        viewModel.updateStartCharge(true)
        viewModel.updateStartVaria(true)

        val current = viewModel.uiState.value.currentCustomProfile
        assertEquals("Shuffled", current.majorMinorDistribution)
        assertTrue(current.scavengerHunt)
        assertEquals(15, current.scavengerRequiredCount)
        assertTrue(current.chozoHunt)
        assertEquals(20, current.superMissileCount)
        assertEquals(12, current.powerBombCount)
        assertTrue(current.startMorph)
        assertTrue(current.startCharge)
        assertTrue(current.startVaria)
    }

    @Test
    fun expandedMapAndQolOptions_updateStateCorrectly() = runTest {
        testScheduler.advanceUntilIdle()
        viewModel.updateBossRandomization("Randomized")
        viewModel.updateDoorLockRandomization("Single/Double")
        viewModel.updateElevatorRando(true)
        viewModel.updateEscapeRando(true)
        viewModel.updateQolFastDoors(true)
        viewModel.updateQolFastText(true)
        viewModel.updateQolAutoChargeBeam(true)
        viewModel.updateAimButtonSetting("Separate")
        viewModel.updateMapIcons("Detailed")
        viewModel.updateItemMarkers("Detailed")
        viewModel.updateMusicShuffle(true)
        viewModel.updatePaletteShuffle(true)

        val current = viewModel.uiState.value.currentCustomProfile
        assertEquals("Randomized", current.bossRandomization)
        assertEquals("Single/Double", current.doorLockRandomization)
        assertTrue(current.elevatorRando)
        assertTrue(current.escapeRando)
        assertTrue(current.qolFastDoors)
        assertTrue(current.qolFastText)
        assertTrue(current.qolAutoChargeBeam)
        assertEquals("Separate", current.aimButtonSetting)
        assertEquals("Detailed", current.mapIcons)
        assertEquals("Detailed", current.itemMarkers)
        assertTrue(current.musicShuffle)
        assertTrue(current.paletteShuffle)
    }

    @Test
    fun saveCurrentProfile_savesToDatabase() = runTest {
        testScheduler.advanceUntilIdle()
        viewModel.updateLogicSkill("Hard")
        viewModel.saveCurrentProfile("My Speedrun Profile")
        testScheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertNotNull(state.selectedProfileId)
        assertEquals("My Speedrun Profile", state.currentCustomProfile.name)
        assertEquals("Hard", state.currentCustomProfile.logicSkill)
        assertFalse(mockProfilesState.value.isEmpty())
    }

    @Test
    fun loadProfile_updatesActiveProfile() = runTest {
        testScheduler.advanceUntilIdle()
        val profile = ConfigProfileEntity(
            id = 10,
            name = "Tournament Profile",
            logicSkill = "Master",
            mapRandomization = "Wild"
        )
        viewModel.loadProfile(profile)

        val state = viewModel.uiState.value
        assertEquals(10, state.selectedProfileId)
        assertEquals("Master", state.currentCustomProfile.logicSkill)
        assertEquals("Wild", state.currentCustomProfile.mapRandomization)
    }

    @Test
    fun editingSelectedProfile_preservesSelectedProfileId() = runTest {
        testScheduler.advanceUntilIdle()
        val profile = ConfigProfileEntity(
            id = 10,
            name = "Tournament Profile",
            logicSkill = "Master",
            mapRandomization = "Wild"
        )
        viewModel.loadProfile(profile)
        assertEquals(10, viewModel.uiState.value.selectedProfileId)

        viewModel.updateLogicSkill("Hard")
        viewModel.updateQolFastDoors(true)

        val state = viewModel.uiState.value
        assertEquals(10, state.selectedProfileId)
        assertEquals("Hard", state.currentCustomProfile.logicSkill)
        assertTrue(state.currentCustomProfile.qolFastDoors)
    }

    @Test
    fun deleteProfile_removesFromDatabaseAndResetsIfActive() = runTest {
        testScheduler.advanceUntilIdle()
        viewModel.saveCurrentProfile("To Delete")
        testScheduler.advanceUntilIdle()

        val createdId = viewModel.uiState.value.selectedProfileId
        assertNotNull(createdId)

        viewModel.deleteProfile(createdId!!)
        testScheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertNull(state.selectedProfileId)
        assertEquals("Custom Preset", state.currentCustomProfile.name)
    }

    @Test
    fun getProfileForPreset_returnsPresetConfiguration() = runTest {
        testScheduler.advanceUntilIdle()
        viewModel.setCustomMode(false)
        viewModel.selectPreset("Summer Series Expert Challenge")

        val profile = viewModel.activeConfigProfile
        assertEquals("Summer Series Expert Challenge", profile.name)
        assertEquals("Expert", profile.logicSkill)
        assertEquals("Full Area", profile.mapRandomization)
    }

    @Test
    fun presetCatalog_definesOfficialPresetsCorrectly() = runTest {
        val presets = com.maprandroid.data.model.PresetCatalog.OFFICIAL_PRESETS
        assertFalse(presets.isEmpty())

        val wildPreset = com.maprandroid.data.model.PresetCatalog.getPresetByName("Wild")
        assertNotNull(wildPreset)
        assertEquals("Random", wildPreset!!.profile.startLocationMode)

        val expertPreset = com.maprandroid.data.model.PresetCatalog.getPresetByName("Summer Series Expert Challenge")
        assertNotNull(expertPreset)
        assertEquals("Expert", expertPreset!!.profile.logicSkill)
        assertEquals("Random", expertPreset.profile.startLocationMode)
    }

    @Test
    fun generateRom_customMode_formatsCustomParameters() = runTest {
        testScheduler.advanceUntilIdle()
        val stateWithRom = viewModel.uiState.value.copy(
            baseRomUri = "content://rom/1",
            isBaseRomValid = true
        )
        val field = ConfigViewModel::class.java.getDeclaredField("_uiState")
        field.isAccessible = true
        @Suppress("UNCHECKED_CAST")
        (field.get(viewModel) as MutableStateFlow<ConfigUiState>).value = stateWithRom

        viewModel.setCustomMode(true)
        viewModel.updateLogicSkill("Master")
        viewModel.generateRom()
        testScheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals("CUSTOM_SEED_999", state.pendingSeedId)
    }

    @get:Rule
    val tempFolder = TemporaryFolder()

    @Test
    fun downloadAndSaveRomToUri_writesIdenticalPatchedBytesToSafAndLocalFileAndRecordsLocalPath() = runTest {
        testScheduler.advanceUntilIdle()

        val baseRomFile = tempFolder.newFile("base_rom.sfc")
        val baseRomBytes = ByteArray(3145728) { 0x00.toByte() }
        baseRomFile.writeBytes(baseRomBytes)

        val insertedGames = mutableListOf<GameEntity>()
        val mockGameDaoWithCapture = object : GameDao {
            override fun getAllGames() = flowOf(insertedGames.toList())
            override suspend fun getGameById(gameId: Int): GameEntity? = insertedGames.find { it.id == gameId }
            override suspend fun insertGame(game: GameEntity) {
                insertedGames.add(game)
            }
            override suspend fun deleteGame(gameId: Int) {}
            override suspend fun updateGameCompletion(gameId: Int, isCompleted: Boolean) {}
        }
        val gameRepoWithCapture = GameRepository(mockGameDaoWithCapture)

        val testVm = ConfigViewModel(repository, gameRepoWithCapture, profileRepository)

        val stateWithPending = testVm.uiState.value.copy(
            pendingSeedId = "TEST_SEED_123",
            baseRomUri = baseRomFile.absolutePath,
            isBaseRomValid = true
        )
        val field = ConfigViewModel::class.java.getDeclaredField("_uiState")
        field.isAccessible = true
        @Suppress("UNCHECKED_CAST")
        (field.get(testVm) as MutableStateFlow<ConfigUiState>).value = stateWithPending

        val testContext = object : ContextWrapper(null) {
            override fun getExternalFilesDir(type: String?): File {
                val dir = File(tempFolder.root, type ?: "files")
                dir.mkdirs()
                return dir
            }
            override fun getFilesDir(): File {
                val dir = File(tempFolder.root, "files")
                dir.mkdirs()
                return dir
            }
            override fun getContentResolver(): ContentResolver {
                return object : ContentResolver(null) {}
            }
        }

        val dummySafUri = Uri.parse("content://downloads/dest.sfc")
        testVm.downloadAndSaveRom(testContext)
        testScheduler.advanceUntilIdle()

        // Verify state
        val finalState = testVm.uiState.value
        assertFalse("Expected no error but got: ${finalState.errorMessage}", finalState.isError)
        assertTrue("Expected generationSuccess to be true, but state was $finalState", finalState.generationSuccess)
        assertNull(finalState.pendingSeedId)

        // Verify inserted GameEntity
        assertEquals(1, insertedGames.size)
        val insertedGame = insertedGames.first()
        assertEquals("Default-TEST_SEED_123", insertedGame.title)

        // Verify GameEntity romPath points to local app ROMs file (.sfc)
        assertTrue("romPath should point to local SFC file, but was: ${insertedGame.romPath}", insertedGame.romPath.endsWith("Default-TEST_SEED_123.sfc"))
        val localRomFile = File(insertedGame.romPath)
        assertTrue("Local ROM file should exist at ${localRomFile.absolutePath}", localRomFile.exists())

        // Verify local app ROM file has non-empty patched bytes
        val localBytes = localRomFile.readBytes()
        assertTrue("Local ROM file should not be empty", localBytes.isNotEmpty())

        // Verify patched bytes differ from original unpatched base ROM bytes
        assertFalse("Patched ROM bytes must differ from unpatched base ROM", baseRomBytes.contentEquals(localBytes))
    }

    @Test
    fun downloadAndSaveRom_whenServerFails_reportsErrorWithoutDummyFallback() = runTest {
        testScheduler.advanceUntilIdle()

        val baseRomFile = tempFolder.newFile("base_rom_err.sfc")
        val baseRomBytes = ByteArray(3145728) { 0x00.toByte() }
        baseRomFile.writeBytes(baseRomBytes)

        val mockApiError = object : MapRandoApiService {
            override suspend fun getSeedDetails(seedId: String) = SeedDetailsResponse(seedId = seedId)
            override suspend fun getApiSeedDetails(seedId: String) = SeedDetailsResponse(seedId = seedId)
            override suspend fun randomizeSeed(request: GenerateSeedRequest) = SeedDetailsResponse(seedId = "ERR_SEED")
            override suspend fun randomizeApiSeed(request: GenerateSeedRequest) = SeedDetailsResponse(seedId = "ERR_SEED")
            override suspend fun generateSeed(request: GenerateSeedRequest) = SeedDetailsResponse(seedId = "ERR_SEED")
            override suspend fun randomizeMultipart(spoilerToken: RequestBody, settingsJson: RequestBody, rom: MultipartBody.Part): Response<ResponseBody> {
                val errorBody = "Internal Server Error 500".toResponseBody("text/plain".toMediaTypeOrNull())
                return Response.error(500, errorBody)
            }
            override suspend fun customizeRom(customizeUrl: String, body: RequestBody): Response<ResponseBody> {
                return Response.success("ok".toResponseBody())
            }
            override suspend fun downloadRom(seedId: String): ResponseBody = throw RuntimeException("Should not be called")
            override suspend fun downloadPatch(seedId: String) = downloadRom(seedId)
            override suspend fun downloadApiPatch(seedId: String) = downloadRom(seedId)
            override suspend fun downloadSeedDownload(seedId: String) = downloadRom(seedId)
        }

        val errRepo = MapRandoRepository(mockApiError)
        val testVm = ConfigViewModel(errRepo, gameRepository, profileRepository)

        val stateWithPending = testVm.uiState.value.copy(
            pendingSeedId = "ERR_SEED",
            baseRomUri = baseRomFile.absolutePath,
            isBaseRomValid = true
        )
        val field = ConfigViewModel::class.java.getDeclaredField("_uiState")
        field.isAccessible = true
        @Suppress("UNCHECKED_CAST")
        (field.get(testVm) as MutableStateFlow<ConfigUiState>).value = stateWithPending

        val testContext = object : ContextWrapper(null) {
            override fun getContentResolver(): ContentResolver = object : ContentResolver(null) {}
        }

        testVm.downloadAndSaveRom(testContext)
        testScheduler.advanceUntilIdle()

        val finalState = testVm.uiState.value
        assertTrue("Expected error state when server fails", finalState.isError)
        assertFalse("Expected generationSuccess to be false", finalState.generationSuccess)
        assertNotNull(finalState.errorMessage)
        val errorMsg = requireNotNull(finalState.errorMessage)
        assertTrue("Error message should mention server error or status 500: $errorMsg", errorMsg.contains("500") || errorMsg.contains("Step 1"))
    }

    @Test
    fun downloadAndSaveRomToUri_usingTwoStepApi_writesFullPatchedRomToSafAndLocalFileAndRecordsGameEntity() = runTest {
        testScheduler.advanceUntilIdle()

        val baseRomFile = tempFolder.newFile("base_rom_twostep.sfc")
        val baseRomBytes = ByteArray(3145728) { 0x11.toByte() }
        baseRomFile.writeBytes(baseRomBytes)

        val expectedPatchedRom = ByteArray(3145728) { 0x77.toByte() }

        val insertedGames = mutableListOf<GameEntity>()
        val mockGameDaoWithCapture = object : GameDao {
            override fun getAllGames() = flowOf(insertedGames.toList())
            override suspend fun getGameById(gameId: Int): GameEntity? = insertedGames.find { it.id == gameId }
            override suspend fun insertGame(game: GameEntity) {
                insertedGames.add(game)
            }
            override suspend fun deleteGame(gameId: Int) {}
            override suspend fun updateGameCompletion(gameId: Int, isCompleted: Boolean) {}
        }
        val gameRepoWithCapture = GameRepository(mockGameDaoWithCapture)

        val mockApiWithTwoStep = object : MapRandoApiService {
            override suspend fun getSeedDetails(seedId: String) = SeedDetailsResponse(seedId = seedId, hash = "hash")
            override suspend fun getApiSeedDetails(seedId: String) = SeedDetailsResponse(seedId = seedId, hash = "hash")
            override suspend fun randomizeSeed(request: GenerateSeedRequest) = SeedDetailsResponse(seedId = "TWO_STEP_777", hash = "hash")
            override suspend fun randomizeApiSeed(request: GenerateSeedRequest) = randomizeSeed(request)
            override suspend fun generateSeed(request: GenerateSeedRequest) = randomizeSeed(request)

            override suspend fun randomizeMultipart(
                spoilerToken: RequestBody,
                settingsJson: RequestBody,
                rom: MultipartBody.Part
            ): Response<ResponseBody> {
                val jsonStr = """{"seed_url": "/seed/777/"}"""
                val body = jsonStr.toByteArray().toResponseBody("application/json".toMediaTypeOrNull())
                return Response.success(body)
            }

            override suspend fun customizeRom(customizeUrl: String, body: RequestBody): Response<ResponseBody> {
                val responseBody = ResponseBody.create("application/octet-stream".toMediaTypeOrNull(), expectedPatchedRom)
                return Response.success(responseBody)
            }

            override suspend fun downloadRom(seedId: String): ResponseBody = throw RuntimeException("Not called in 2-step flow")
            override suspend fun downloadPatch(seedId: String) = downloadRom(seedId)
            override suspend fun downloadApiPatch(seedId: String) = downloadRom(seedId)
            override suspend fun downloadSeedDownload(seedId: String) = downloadRom(seedId)
        }

        val customRepo = MapRandoRepository(mockApiWithTwoStep)
        val testVm = ConfigViewModel(customRepo, gameRepoWithCapture, profileRepository)

        val stateWithPending = testVm.uiState.value.copy(
            pendingSeedId = "TWO_STEP_777",
            baseRomUri = baseRomFile.absolutePath,
            isBaseRomValid = true
        )
        val field = ConfigViewModel::class.java.getDeclaredField("_uiState")
        field.isAccessible = true
        @Suppress("UNCHECKED_CAST")
        (field.get(testVm) as MutableStateFlow<ConfigUiState>).value = stateWithPending

        val testContext = object : ContextWrapper(null) {
            override fun getExternalFilesDir(type: String?): File {
                val dir = File(tempFolder.root, type ?: "files")
                dir.mkdirs()
                return dir
            }
            override fun getFilesDir(): File {
                val dir = File(tempFolder.root, "files")
                dir.mkdirs()
                return dir
            }
            override fun getContentResolver(): ContentResolver {
                return object : ContentResolver(null) {}
            }
        }

        testVm.downloadAndSaveRom(testContext)
        testScheduler.advanceUntilIdle()

        val finalState = testVm.uiState.value
        assertFalse("Expected no error but got: ${finalState.errorMessage}", finalState.isError)
        assertTrue("Expected generationSuccess to be true", finalState.generationSuccess)

        assertEquals(1, insertedGames.size)
        val insertedGame = insertedGames.first()
        assertEquals("Default-777", insertedGame.title)

        val localRomFile = File(insertedGame.romPath)
        assertTrue("Local ROM file must exist at ${localRomFile.absolutePath}", localRomFile.exists())
        val localBytes = localRomFile.readBytes()
        assertEquals(3145728, localBytes.size)
        assertArrayEquals(expectedPatchedRom, localBytes)
    }

    @Test
    fun dismissGenerationModal_clearsModalState() = runTest {
        testScheduler.advanceUntilIdle()
        val dummyState = viewModel.uiState.value.copy(
            showGenerationModal = true,
            isGenerating = true,
            generationError = "Error",
            generationSuccess = true,
            generatedSeedId = "123",
            generatedRomPath = "/path/to/rom.sfc"
        )
        val field = ConfigViewModel::class.java.getDeclaredField("_uiState")
        field.isAccessible = true
        @Suppress("UNCHECKED_CAST")
        (field.get(viewModel) as MutableStateFlow<ConfigUiState>).value = dummyState

        viewModel.dismissGenerationModal()

        val state = viewModel.uiState.value
        assertFalse(state.showGenerationModal)
        assertFalse(state.isGenerating)
        assertNull(state.generationError)
        assertNull(state.generatedSeedId)
        assertNull(state.generatedRomPath)
        assertFalse(state.generationSuccess)
    }

    @Test
    fun generateRomWithContext_runsPipeline_setsCompleteAndRomPath() = runTest {
        testScheduler.advanceUntilIdle()

        val baseRomFile = tempFolder.newFile("base_rom_full.sfc")
        val baseRomBytes = ByteArray(3145728) { 0x00.toByte() }
        baseRomFile.writeBytes(baseRomBytes)

        val insertedGames = mutableListOf<GameEntity>()
        val mockGameDaoWithCapture = object : GameDao {
            override fun getAllGames() = flowOf(insertedGames.toList())
            override suspend fun getGameById(gameId: Int): GameEntity? = insertedGames.find { it.id == gameId }
            override suspend fun insertGame(game: GameEntity) {
                insertedGames.add(game)
            }
            override suspend fun deleteGame(gameId: Int) {}
            override suspend fun updateGameCompletion(gameId: Int, isCompleted: Boolean) {}
        }
        val gameRepoWithCapture = GameRepository(mockGameDaoWithCapture)

        val testVm = ConfigViewModel(repository, gameRepoWithCapture, profileRepository)

        val stateWithRom = testVm.uiState.value.copy(
            baseRomUri = baseRomFile.absolutePath,
            isBaseRomValid = true
        )
        val field = ConfigViewModel::class.java.getDeclaredField("_uiState")
        field.isAccessible = true
        @Suppress("UNCHECKED_CAST")
        (field.get(testVm) as MutableStateFlow<ConfigUiState>).value = stateWithRom

        val testContext = object : ContextWrapper(null) {
            override fun getExternalFilesDir(type: String?): File {
                val dir = File(tempFolder.root, type ?: "files")
                dir.mkdirs()
                return dir
            }
            override fun getFilesDir(): File {
                val dir = File(tempFolder.root, "files")
                dir.mkdirs()
                return dir
            }
            override fun getContentResolver(): ContentResolver {
                return object : ContentResolver(null) {}
            }
        }

        testVm.generateRom(testContext)
        testScheduler.advanceUntilIdle()

        val state = testVm.uiState.value
        assertTrue("isCustomizingSeed should be true after Step 1", state.isCustomizingSeed)
        assertEquals("TEST_SEED_123", state.pendingSeedId)

        testVm.finalizeSeedCustomization(testContext)
        testScheduler.advanceUntilIdle()

        val finalState = testVm.uiState.value
        assertTrue(finalState.showGenerationModal)
        assertTrue(finalState.generationSuccess)
        assertFalse(finalState.isGenerating)
        assertEquals(com.maprandroid.ui.config.GenerationStep.COMPLETE, finalState.generationStep)
        assertNotNull(finalState.generatedRomPath)
        assertTrue(finalState.generatedRomPath!!.endsWith(".sfc"))
    }

    @Test
    fun activeConfigProfile_customMode_appliesDefaultControlsAndFavoriteSprite() = runTest {
        testScheduler.advanceUntilIdle()
        viewModel.setCustomMode(true)

        val field = ConfigViewModel::class.java.getDeclaredField("_uiState")
        field.isAccessible = true
        @Suppress("UNCHECKED_CAST")
        val stateFlow = field.get(viewModel) as MutableStateFlow<ConfigUiState>

        val customControls = com.maprandroid.data.local.DefaultControls(
            shot = "B",
            jump = "Y",
            dash = "A",
            itemSelect = "Select",
            itemCancel = "X",
            angleUp = "R",
            angleDown = "L"
        )
        stateFlow.value = stateFlow.value.copy(
            favoriteSprite = "samus_dread",
            defaultControls = customControls
        )

        val profile = viewModel.activeConfigProfile
        assertEquals("samus_dread", profile.samusSprite)
        assertEquals("B", profile.controlShot)
        assertEquals("Y", profile.controlJump)
        assertEquals("A", profile.controlDash)
        assertEquals("X", profile.controlItemCancel)
    }
}
