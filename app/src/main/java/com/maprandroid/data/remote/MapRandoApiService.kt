package com.maprandroid.data.remote

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part
import retrofit2.http.Path
import retrofit2.http.Streaming
import retrofit2.http.Url

interface MapRandoApiService {
    @GET("seed/{seedId}")
    suspend fun getSeedDetails(@Path("seedId") seedId: String): SeedDetailsResponse

    @GET("api/seed/{seedId}")
    suspend fun getApiSeedDetails(@Path("seedId") seedId: String): SeedDetailsResponse

    @POST("randomize")
    suspend fun randomizeSeed(@Body request: GenerateSeedRequest): SeedDetailsResponse

    @POST("api/randomize")
    suspend fun randomizeApiSeed(@Body request: GenerateSeedRequest): SeedDetailsResponse

    @POST("generate")
    suspend fun generateSeed(@Body request: GenerateSeedRequest): SeedDetailsResponse

    @Multipart
    @POST("randomize")
    suspend fun randomizeMultipart(
        @Part("spoiler_token") spoilerToken: RequestBody,
        @Part("settings") settingsJson: RequestBody,
        @Part rom: MultipartBody.Part
    ): Response<ResponseBody>

    @Streaming
    @POST
    suspend fun customizeRom(
        @Url customizeUrl: String,
        @Body body: RequestBody
    ): Response<ResponseBody>

    @Streaming
    @GET("seed/{seedId}/rom")
    suspend fun downloadRom(@Path("seedId") seedId: String): ResponseBody

    @Streaming
    @GET("seed/{seedId}/patch")
    suspend fun downloadPatch(@Path("seedId") seedId: String): ResponseBody

    @Streaming
    @GET("seed/{seedId}/download")
    suspend fun downloadSeedDownload(@Path("seedId") seedId: String): ResponseBody

    @Streaming
    @GET("api/seed/{seedId}/patch")
    suspend fun downloadApiPatch(@Path("seedId") seedId: String): ResponseBody
}

@JsonClass(generateAdapter = true)
data class GenerateSeedRequest(
    val preset: String = "Custom",
    @field:Json(name = "custom_settings") val customSettings: Map<String, String>? = null,
    val logic: String? = null,
    @field:Json(name = "area_assignment") val areaAssignment: String? = null,
    @field:Json(name = "area_layout") val areaLayout: String? = null,
    @field:Json(name = "item_progression") val itemProgression: String? = null,
    @field:Json(name = "item_pool") val itemPool: String? = null,
    @field:Json(name = "map_randomization") val mapRandomization: String? = null,
    @field:Json(name = "qol_settings") val qolSettings: Map<String, Boolean>? = null,
    @field:Json(name = "tech_settings") val techSettings: Map<String, Boolean>? = null,
    @field:Json(name = "item_settings") val itemSettings: Map<String, String>? = null,
    @field:Json(name = "map_settings") val mapSettings: Map<String, String>? = null,
    @field:Json(name = "other_settings") val otherSettings: Map<String, String>? = null,
    val difficulty: String? = null,
)

@JsonClass(generateAdapter = true)
data class SeedDetailsResponse(
    @field:Json(name = "seed_id") val seedId: String = "",
    @field:Json(name = "id") val id: String? = null,
    @field:Json(name = "seed_url") val seedUrl: String? = null,
    val hash: String = "",
    val ips: String? = null,
    @field:Json(name = "ips_patch") val ipsPatch: String? = null,
    val bps: String? = null,
    @field:Json(name = "bps_patch") val bpsPatch: String? = null,
    val patch: String? = null,
    @field:Json(name = "patch_data") val patchData: String? = null,
    val data: String? = null
) {
    fun getEffectiveSeedId(): String {
        if (seedId.isNotBlank()) return seedId
        if (!id.isNullOrBlank()) return id
        if (!seedUrl.isNullOrBlank()) {
            val clean = seedUrl.trimEnd('/')
            val lastSegment = clean.substringAfterLast('/')
            if (lastSegment.isNotBlank()) return lastSegment
        }
        return ""
    }

    fun getPatchBase64(): String? {
        val candidate = ips ?: ipsPatch ?: patch ?: patchData ?: bps ?: bpsPatch ?: data
        return if (!candidate.isNullOrBlank()) candidate else null
    }
}

@JsonClass(generateAdapter = true)
data class PresetsResponse(
    val presets: List<String>
)

