package com.maprandroid

import com.maprandroid.data.remote.GenerateSeedRequest
import com.maprandroid.data.remote.MapRandoApiService
import com.maprandroid.data.remote.SeedDetailsResponse
import com.maprandroid.data.repository.MapRandoRepository
import com.maprandroid.util.RomPatcher
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.ResponseBody
import okhttp3.ResponseBody.Companion.toResponseBody
import retrofit2.Response
import org.junit.Assert.*
import org.junit.Test
import java.io.ByteArrayOutputStream
import java.util.Base64

@OptIn(ExperimentalCoroutinesApi::class)
class MapRandoRepositoryTest {

    private fun generateLargeIpsPatch(numRecords: Int = 1500): ByteArray {
        val stream = ByteArrayOutputStream()
        stream.write("PATCH".toByteArray(Charsets.US_ASCII))

        for (i in 0 until numRecords) {
            val offset = i * 128
            val recordData = ByteArray(64) { (it + i).toByte() }

            stream.write(byteArrayOf(
                ((offset ushr 16) and 0xFF).toByte(),
                ((offset ushr 8) and 0xFF).toByte(),
                (offset and 0xFF).toByte()
            ))
            stream.write(byteArrayOf(
                ((recordData.size ushr 8) and 0xFF).toByte(),
                (recordData.size and 0xFF).toByte()
            ))
            stream.write(recordData)
        }

        stream.write("EOF".toByteArray(Charsets.US_ASCII))
        return stream.toByteArray()
    }

    @Test
    fun parsePresetsFromHtml_targetsPresetSelectOnly() {
        val mockHtml = """
            <html>
            <body>
              <select id="preset" name="preset">
                <option value="Default">Default</option>
                <option value="Community Race Season 5">Community Race Season 5</option>
                <option value="Mentor Tournament">Mentor Tournament</option>
                <option value="Summer Series Expert Challenge">Summer Series Expert Challenge</option>
                <option value="Standard">Standard</option>
                <option value="Wild">Wild</option>
              </select>
              <select id="doors">
                <option value="Red">Red</option>
                <option value="Green">Green</option>
                <option value="Yellow">Yellow</option>
              </select>
            </body>
            </html>
        """.trimIndent()

        val repository = MapRandoRepository(object : MapRandoApiService {
            override suspend fun getSeedDetails(seedId: String) = SeedDetailsResponse()
            override suspend fun getApiSeedDetails(seedId: String) = SeedDetailsResponse()
            override suspend fun randomizeSeed(request: GenerateSeedRequest) = SeedDetailsResponse()
            override suspend fun randomizeApiSeed(request: GenerateSeedRequest) = SeedDetailsResponse()
            override suspend fun generateSeed(request: GenerateSeedRequest) = SeedDetailsResponse()
            override suspend fun randomizeMultipart(spoilerToken: RequestBody, settingsJson: RequestBody, rom: MultipartBody.Part) = throw RuntimeException()
            override suspend fun customizeRom(customizeUrl: String, body: RequestBody): Response<ResponseBody> = throw RuntimeException()
            override suspend fun downloadRom(seedId: String) = throw RuntimeException()
            override suspend fun downloadPatch(seedId: String) = throw RuntimeException()
            override suspend fun downloadApiPatch(seedId: String) = throw RuntimeException()
            override suspend fun downloadSeedDownload(seedId: String) = throw RuntimeException()
        })

        val parsed = repository.parsePresetsFromHtml(mockHtml)
        assertEquals(6, parsed.size)
        assertTrue(parsed.contains("Default"))
        assertTrue(parsed.contains("Wild"))
        assertFalse(parsed.contains("Red"))
        assertFalse(parsed.contains("Yellow"))
    }

    @Test
    fun parseSpritesFromGitHubJson_extractsSpriteNames() {
        val json = """
            [
              {"name": "samus_dread", "type": "dir"},
              {"name": "samus_fusion.rdc", "type": "file"},
              {"name": "README.md", "type": "file"},
              {"name": ".gitignore", "type": "file"},
              {"name": "samus_pixel.png", "type": "file"}
            ]
        """.trimIndent()

        val repository = MapRandoRepository(object : MapRandoApiService {
            override suspend fun getSeedDetails(seedId: String) = SeedDetailsResponse()
            override suspend fun getApiSeedDetails(seedId: String) = SeedDetailsResponse()
            override suspend fun randomizeSeed(request: GenerateSeedRequest) = SeedDetailsResponse()
            override suspend fun randomizeApiSeed(request: GenerateSeedRequest) = SeedDetailsResponse()
            override suspend fun generateSeed(request: GenerateSeedRequest) = SeedDetailsResponse()
            override suspend fun randomizeMultipart(spoilerToken: RequestBody, settingsJson: RequestBody, rom: MultipartBody.Part) = throw RuntimeException()
            override suspend fun customizeRom(customizeUrl: String, body: RequestBody): Response<ResponseBody> = throw RuntimeException()
            override suspend fun downloadRom(seedId: String) = throw RuntimeException()
            override suspend fun downloadPatch(seedId: String) = throw RuntimeException()
            override suspend fun downloadApiPatch(seedId: String) = throw RuntimeException()
            override suspend fun downloadSeedDownload(seedId: String) = throw RuntimeException()
        })

        val parsed = repository.parseSpritesFromGitHubJson(json)
        assertTrue(parsed.contains("samus_vanilla"))
        assertTrue(parsed.contains("samus_dread"))
        assertTrue(parsed.contains("samus_fusion"))
        assertTrue(parsed.contains("samus_pixel"))
        assertFalse(parsed.contains("README.md"))
        assertFalse(parsed.contains(".gitignore"))
    }

    @Test
    fun parseSpritesFromGitHubHtml_extractsSpriteNames() {
        val html = """
            <html>
            <body>
              <a href="/blkerby/MapRandoSprites/tree/8412c6120dce5aa7ae7077697d0dc705a4844609/samus_sprites/samus_dread">samus_dread</a>
              <a href="/blkerby/MapRandoSprites/tree/8412c6120dce5aa7ae7077697d0dc705a4844609/samus_sprites/samus_fusion">samus_fusion</a>
              <a href="/blkerby/MapRandoSprites/blob/8412c6120dce5aa7ae7077697d0dc705a4844609/samus_sprites/samus_super.rdc">samus_super.rdc</a>
            </body>
            </html>
        """.trimIndent()

        val repository = MapRandoRepository(object : MapRandoApiService {
            override suspend fun getSeedDetails(seedId: String) = SeedDetailsResponse()
            override suspend fun getApiSeedDetails(seedId: String) = SeedDetailsResponse()
            override suspend fun randomizeSeed(request: GenerateSeedRequest) = SeedDetailsResponse()
            override suspend fun randomizeApiSeed(request: GenerateSeedRequest) = SeedDetailsResponse()
            override suspend fun generateSeed(request: GenerateSeedRequest) = SeedDetailsResponse()
            override suspend fun randomizeMultipart(spoilerToken: RequestBody, settingsJson: RequestBody, rom: MultipartBody.Part) = throw RuntimeException()
            override suspend fun customizeRom(customizeUrl: String, body: RequestBody): Response<ResponseBody> = throw RuntimeException()
            override suspend fun downloadRom(seedId: String) = throw RuntimeException()
            override suspend fun downloadPatch(seedId: String) = throw RuntimeException()
            override suspend fun downloadApiPatch(seedId: String) = throw RuntimeException()
            override suspend fun downloadSeedDownload(seedId: String) = throw RuntimeException()
        })

        val parsed = repository.parseSpritesFromGitHubHtml(html)
        assertTrue(parsed.contains("samus_vanilla"))
        assertTrue(parsed.contains("samus_dread"))
        assertTrue(parsed.contains("samus_fusion"))
        assertTrue(parsed.contains("samus_super"))
    }

    @Test
    fun generateSeed_withDirectBase64Patch_parsesAndCachesFullPatch() = runTest {
        val largeIps = generateLargeIpsPatch(2000) // ~140 KB binary IPS patch
        val b64Ips = Base64.getEncoder().encodeToString(largeIps)

        assertTrue("Large patch binary should be ~140KB", largeIps.size > 100000)

        val mockApi = object : MapRandoApiService {
            override suspend fun getSeedDetails(seedId: String) = SeedDetailsResponse(seedId = seedId, ips = b64Ips)
            override suspend fun getApiSeedDetails(seedId: String) = SeedDetailsResponse(seedId = seedId, ips = b64Ips)
            override suspend fun randomizeSeed(request: GenerateSeedRequest) = SeedDetailsResponse(seedId = "FULL_SEED_999", ips = b64Ips)
            override suspend fun randomizeApiSeed(request: GenerateSeedRequest) = randomizeSeed(request)
            override suspend fun generateSeed(request: GenerateSeedRequest) = randomizeSeed(request)
            override suspend fun randomizeMultipart(spoilerToken: RequestBody, settingsJson: RequestBody, rom: MultipartBody.Part): Response<ResponseBody> = throw RuntimeException("Not called")
            override suspend fun customizeRom(customizeUrl: String, body: RequestBody): Response<ResponseBody> = throw RuntimeException("Not called")
            override suspend fun downloadRom(seedId: String): ResponseBody = throw RuntimeException("Not called")
            override suspend fun downloadPatch(seedId: String): ResponseBody = throw RuntimeException("Not called")
            override suspend fun downloadApiPatch(seedId: String): ResponseBody = throw RuntimeException("Not called")
            override suspend fun downloadSeedDownload(seedId: String): ResponseBody = throw RuntimeException("Not called")
        }

        val repository = MapRandoRepository(mockApi)

        val result = repository.generateSeed("Standard")
        assertTrue(result.isSuccess)
        val seedResponse = result.getOrNull()
        assertNotNull(seedResponse)
        assertEquals("FULL_SEED_999", seedResponse?.seedId)

        // Download ROM should return the cached full 140KB patch!
        val downloadResult = repository.downloadRom("FULL_SEED_999")
        assertTrue(downloadResult.isSuccess)

        val patchBytes = downloadResult.getOrNull()?.bytes()
        assertNotNull(patchBytes)
        assertEquals(largeIps.size, patchBytes!!.size)

        // Apply to 3MB base ROM
        val baseRom = ByteArray(3145728) { 0x00.toByte() }
        val patched = RomPatcher.applyPatch(baseRom, patchBytes)
        assertEquals(3145728, patched.size)
        assertFalse(baseRom.contentEquals(patched))
    }

    @Test
    fun downloadRom_returningJsonResponseWithBase64Ips_parsesFullPatch() = runTest {
        val largeIps = generateLargeIpsPatch(1500) // ~100 KB binary IPS patch
        val b64Ips = Base64.getEncoder().encodeToString(largeIps)

        val jsonResponseStr = """
            {
                "seed_id": "NET_SEED_888",
                "ips": "$b64Ips"
            }
        """.trimIndent()

        val mockApi = object : MapRandoApiService {
            override suspend fun getSeedDetails(seedId: String) = SeedDetailsResponse(seedId = seedId)
            override suspend fun getApiSeedDetails(seedId: String) = SeedDetailsResponse(seedId = seedId)
            override suspend fun randomizeSeed(request: GenerateSeedRequest) = SeedDetailsResponse(seedId = "NET_SEED_888")
            override suspend fun randomizeApiSeed(request: GenerateSeedRequest) = randomizeSeed(request)
            override suspend fun generateSeed(request: GenerateSeedRequest) = randomizeSeed(request)
            override suspend fun randomizeMultipart(spoilerToken: RequestBody, settingsJson: RequestBody, rom: MultipartBody.Part): Response<ResponseBody> = throw RuntimeException("Not called")
            override suspend fun customizeRom(customizeUrl: String, body: RequestBody): Response<ResponseBody> = throw RuntimeException("Not called")
            override suspend fun downloadRom(seedId: String): ResponseBody {
                return jsonResponseStr.toByteArray(Charsets.UTF_8).toResponseBody("application/json".toMediaTypeOrNull())
            }
            override suspend fun downloadPatch(seedId: String) = downloadRom(seedId)
            override suspend fun downloadApiPatch(seedId: String) = downloadRom(seedId)
            override suspend fun downloadSeedDownload(seedId: String) = downloadRom(seedId)
        }

        val repository = MapRandoRepository(mockApi)

        val downloadResult = repository.downloadRom("NET_SEED_888")
        assertTrue(downloadResult.isSuccess)

        val downloadedPatchBytes = downloadResult.getOrNull()?.bytes()
        assertNotNull(downloadedPatchBytes)
        assertEquals(largeIps.size, downloadedPatchBytes!!.size)

        val baseRom = ByteArray(3145728) { 0x00.toByte() }
        val patched = RomPatcher.applyPatch(baseRom, downloadedPatchBytes)
        assertEquals(3145728, patched.size)
        assertFalse(baseRom.contentEquals(patched))
    }

    @Test
    fun generateAndCustomizeRom_twoStepMultipartFlow_returnsFull3MBPatchedRom() = runTest {
        val baseRomBytes = ByteArray(3145728) { 0xAA.toByte() }
        val patchedRomBytes = ByteArray(3145728) { 0xBB.toByte() }

        var step1Called = false
        var step2Called = false
        var receivedCustomizeUrl = ""

        val mockApi = object : MapRandoApiService {
            override suspend fun getSeedDetails(seedId: String) = SeedDetailsResponse(seedId = seedId)
            override suspend fun getApiSeedDetails(seedId: String) = SeedDetailsResponse(seedId = seedId)
            override suspend fun randomizeSeed(request: GenerateSeedRequest) = SeedDetailsResponse(seedId = "123456")
            override suspend fun randomizeApiSeed(request: GenerateSeedRequest) = randomizeSeed(request)
            override suspend fun generateSeed(request: GenerateSeedRequest) = randomizeSeed(request)

            override suspend fun randomizeMultipart(
                spoilerToken: RequestBody,
                settingsJson: RequestBody,
                rom: MultipartBody.Part
            ): Response<ResponseBody> {
                step1Called = true
                val jsonStr = """{"seed_url": "/seed/123456/"}"""
                val body = jsonStr.toByteArray().toResponseBody("application/json".toMediaTypeOrNull())
                return Response.success(body)
            }

            override suspend fun customizeRom(
                customizeUrl: String,
                body: RequestBody
            ): Response<ResponseBody> {
                step2Called = true
                receivedCustomizeUrl = customizeUrl
                val respBody = patchedRomBytes.toResponseBody("application/octet-stream".toMediaTypeOrNull())
                return Response.success(respBody)
            }

            override suspend fun downloadRom(seedId: String): ResponseBody = throw RuntimeException("Not called")
            override suspend fun downloadPatch(seedId: String) = downloadRom(seedId)
            override suspend fun downloadApiPatch(seedId: String) = downloadRom(seedId)
            override suspend fun downloadSeedDownload(seedId: String) = downloadRom(seedId)
        }

        val repository = MapRandoRepository(mockApi)
        val testProfile = com.maprandroid.data.local.ConfigProfileEntity(name = "Test 2Step Profile")

        val result = repository.generateAndCustomizeRom(baseRomBytes, testProfile)
        assertTrue(result.isSuccess)

        val outputResult = result.getOrNull()
        assertNotNull(outputResult)
        assertEquals("123456", outputResult!!.seedId)
        assertEquals(3145728, outputResult.romBytes.size)
        assertArrayEquals(patchedRomBytes, outputResult.romBytes)

        assertTrue("Step 1 (randomizeMultipart) should have been called", step1Called)
        assertTrue("Step 2 (customizeRom) should have been called", step2Called)
        assertEquals("https://maprando.com/seed/123456/customize", receivedCustomizeUrl)
    }

    @Test
    fun buildSettingsJson_formatsValidVersion122Json() {
        val mockApi = object : MapRandoApiService {
            override suspend fun getSeedDetails(seedId: String) = SeedDetailsResponse(seedId = seedId)
            override suspend fun getApiSeedDetails(seedId: String) = SeedDetailsResponse(seedId = seedId)
            override suspend fun randomizeSeed(request: GenerateSeedRequest) = SeedDetailsResponse(seedId = "123456")
            override suspend fun randomizeApiSeed(request: GenerateSeedRequest) = randomizeSeed(request)
            override suspend fun generateSeed(request: GenerateSeedRequest) = randomizeSeed(request)
            override suspend fun randomizeMultipart(spoilerToken: RequestBody, settingsJson: RequestBody, rom: MultipartBody.Part): Response<ResponseBody> = throw RuntimeException("Not called")
            override suspend fun customizeRom(customizeUrl: String, body: RequestBody): Response<ResponseBody> = throw RuntimeException("Not called")
            override suspend fun downloadRom(seedId: String): ResponseBody = throw RuntimeException("Not called")
            override suspend fun downloadPatch(seedId: String) = downloadRom(seedId)
            override suspend fun downloadApiPatch(seedId: String) = downloadRom(seedId)
            override suspend fun downloadSeedDownload(seedId: String) = downloadRom(seedId)
        }

        val repository = MapRandoRepository(mockApi)
        val profile = com.maprandroid.data.local.ConfigProfileEntity(
            name = "Settings JSON Test Profile",
            logicSkill = "Expert",
            qolFastDoors = true
        )

        val json = repository.buildSettingsJson(profile)
        assertTrue(json.contains("\"version\":123") || json.contains("\"version\": 123"))
        assertTrue(json.contains("Settings JSON Test Profile"))
        assertTrue(json.contains("Expert"))
    }

    @Test
    fun generateAndCustomizeRom_withHeaderedRom_stripsHeaderToUnheadered() = runTest {
        val headeredRomBytes = ByteArray(3146240) { 0xCC.toByte() }
        val patchedRomBytes = ByteArray(3145728) { 0xDD.toByte() }

        var receivedRomSize = -1L

        val mockApi = object : MapRandoApiService {
            override suspend fun getSeedDetails(seedId: String) = SeedDetailsResponse(seedId = seedId)
            override suspend fun getApiSeedDetails(seedId: String) = SeedDetailsResponse(seedId = seedId)
            override suspend fun randomizeSeed(request: GenerateSeedRequest) = SeedDetailsResponse(seedId = "123456")
            override suspend fun randomizeApiSeed(request: GenerateSeedRequest) = randomizeSeed(request)
            override suspend fun generateSeed(request: GenerateSeedRequest) = randomizeSeed(request)

            override suspend fun randomizeMultipart(
                spoilerToken: RequestBody,
                settingsJson: RequestBody,
                rom: MultipartBody.Part
            ): Response<ResponseBody> {
                receivedRomSize = rom.body.contentLength()
                val jsonStr = """{"seed_url": "/seed/123456/"}"""
                val body = jsonStr.toByteArray().toResponseBody("application/json".toMediaTypeOrNull())
                return Response.success(body)
            }

            override suspend fun customizeRom(
                customizeUrl: String,
                body: RequestBody
            ): Response<ResponseBody> {
                val respBody = patchedRomBytes.toResponseBody("application/octet-stream".toMediaTypeOrNull())
                return Response.success(respBody)
            }

            override suspend fun downloadRom(seedId: String): ResponseBody = throw RuntimeException("Not called")
            override suspend fun downloadPatch(seedId: String) = downloadRom(seedId)
            override suspend fun downloadApiPatch(seedId: String) = downloadRom(seedId)
            override suspend fun downloadSeedDownload(seedId: String) = downloadRom(seedId)
        }

        val repository = MapRandoRepository(mockApi)
        val testProfile = com.maprandroid.data.local.ConfigProfileEntity(name = "Test Headered Profile")

        val result = repository.generateAndCustomizeRom(headeredRomBytes, testProfile)
        assertTrue(result.isSuccess)
        assertEquals(3145728L, receivedRomSize)
    }
}