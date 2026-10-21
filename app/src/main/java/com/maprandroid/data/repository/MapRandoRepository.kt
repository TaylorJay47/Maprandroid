package com.maprandroid.data.repository

import android.util.Log
import com.maprandroid.data.local.ConfigProfileEntity
import com.maprandroid.data.remote.GenerateSeedRequest
import com.maprandroid.data.remote.MapRandoApiService
import com.maprandroid.data.remote.PresetsResponse
import com.maprandroid.data.remote.SeedDetailsResponse
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.ResponseBody
import okhttp3.ResponseBody.Companion.toResponseBody
import java.io.ByteArrayOutputStream
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap

data class CustomizedRomResult(
    val seedId: String,
    val romBytes: ByteArray
)

open class MapRandoRepository(private val apiService: MapRandoApiService = com.maprandroid.data.remote.NetworkModule.mapRandoApiService) {

    private val seedPatchCache = ConcurrentHashMap<String, ByteArray>()

    private fun safeLogI(msg: String) {
        try {
            Log.i("MapRandoRepository", msg)
        } catch (_: Throwable) {
            println("[MapRandoRepository] $msg")
        }
    }

    private fun safeLogW(msg: String) {
        try {
            Log.w("MapRandoRepository", msg)
        } catch (_: Throwable) {
            println("[MapRandoRepository WARNING] $msg")
        }
    }

    private fun safeLogE(msg: String, tr: Throwable? = null) {
        try {
            if (tr != null) Log.e("MapRandoRepository", msg, tr)
            else Log.e("MapRandoRepository", msg)
        } catch (_: Throwable) {
            println("[MapRandoRepository ERROR] $msg ${tr?.message ?: ""}")
        }
    }

    private fun decodeBase64(str: String): ByteArray {
        return try {
            android.util.Base64.decode(str, android.util.Base64.DEFAULT)
        } catch (_: Throwable) {
            val table = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/"
            val clean = str.filter { it in table }
            val output = ByteArrayOutputStream()
            var buffer = 0
            var bits = 0
            for (ch in clean) {
                val v = table.indexOf(ch)
                if (v < 0) continue
                buffer = (buffer shl 6) or v
                bits += 6
                if (bits >= 8) {
                    bits -= 8
                    output.write((buffer ushr bits) and 0xFF)
                }
            }
            output.toByteArray()
        }
    }

    /**
     * Standard Super Metroid Map Rando presets pulled from PresetCatalog.
     */
    val defaultPresets = com.maprandroid.data.model.PresetCatalog.DEFAULT_PRESET_NAMES

    suspend fun getSeedDetails(seedId: String): Result<SeedDetailsResponse> {
        return try {
            val response = apiService.getSeedDetails(seedId)
            parseAndCachePatchFromResponse(seedId, response)
            Result.success(response)
        } catch (e: Exception) {
            try {
                val apiResp = apiService.getApiSeedDetails(seedId)
                parseAndCachePatchFromResponse(seedId, apiResp)
                Result.success(apiResp)
            } catch (_: Exception) {
                Result.failure(e)
            }
        }
    }

    fun getPresets(): Result<PresetsResponse> {
        return Result.success(PresetsResponse(presets = defaultPresets))
    }

    open suspend fun fetchAndUpdateOfficialPresets(): Result<List<String>> {
        return kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            try {
                val url = java.net.URL("https://maprando.com/generate")
                val connection = (url.openConnection() as java.net.HttpURLConnection).apply {
                    requestMethod = "GET"
                    connectTimeout = 10000
                    readTimeout = 10000
                    setRequestProperty("User-Agent", "Mozilla/5.0 (Android; MapRandoApp)")
                }

                val html = connection.inputStream.bufferedReader().use { it.readText() }
                connection.disconnect()

                val parsed = parsePresetsFromHtml(html)
                if (parsed.isNotEmpty()) {
                    com.maprandroid.data.model.PresetCatalog.updateOfficialPresets(parsed)
                    Result.success(parsed)
                } else {
                    Result.success(com.maprandroid.data.model.PresetCatalog.DEFAULT_PRESET_NAMES)
                }
            } catch (e: Exception) {
                safeLogE("Failed to update official presets from maprando.com: ${e.message}", e)
                Result.failure(e)
            }
        }
    }

    open fun parsePresetsFromHtml(html: String): List<String> {
        val presetNames = LinkedHashSet<String>()

        val selectBlockRegex = Regex("""(?is)<select[^>]*(id|name|class)=["']?[^"'>]*preset[^"'>]*["']?[^>]*>(.*?)</select>""")
        val match = selectBlockRegex.find(html)

        if (match != null) {
            val selectBody = match.groupValues[2]
            val optionRegex = Regex("""(?i)<option[^>]*>([^<]+)</option>""")
            optionRegex.findAll(selectBody).forEach { m ->
                val text = m.groupValues[1].trim()
                if (text.isNotBlank() && !text.equals("Custom", ignoreCase = true) && !text.contains("Select", ignoreCase = true)) {
                    presetNames.add(text)
                }
            }
        }

        if (presetNames.isEmpty()) {
            val corePresets = com.maprandroid.data.model.PresetCatalog.OFFICIAL_PRESETS.map { it.name }
            presetNames.addAll(corePresets)
        }

        return presetNames.toList()
    }

    open suspend fun fetchAndUpdateSpriteList(
        githubUrl: String = "https://github.com/blkerby/MapRandoSprites/tree/8412c6120dce5aa7ae7077697d0dc705a4844609/samus_sprites"
    ): Result<List<String>> {
        return kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            try {
                var ref = "8412c6120dce5aa7ae7077697d0dc705a4844609"
                val refMatch = Regex("""/tree/([a-zA-Z0-9_.-]+)/samus_sprites""").find(githubUrl)
                if (refMatch != null) {
                    ref = refMatch.groupValues[1]
                }

                var parsedSprites: List<String> = emptyList()

                try {
                    val apiUrl = "https://api.github.com/repos/blkerby/MapRandoSprites/contents/samus_sprites?ref=$ref"
                    val connection = (java.net.URL(apiUrl).openConnection() as java.net.HttpURLConnection).apply {
                        requestMethod = "GET"
                        connectTimeout = 10000
                        readTimeout = 10000
                        setRequestProperty("User-Agent", "Mozilla/5.0 (Android; MapRandoApp)")
                        setRequestProperty("Accept", "application/vnd.github.v3+json")
                    }

                    if (connection.responseCode == 200) {
                        val json = connection.inputStream.bufferedReader().use { it.readText() }
                        connection.disconnect()
                        parsedSprites = parseSpritesFromGitHubJson(json)
                    } else {
                        connection.disconnect()
                    }
                } catch (e: Exception) {
                    safeLogW("GitHub API call for sprites failed (${e.message}), falling back to web page scraping")
                }

                if (parsedSprites.isEmpty()) {
                    val connection = (java.net.URL(githubUrl).openConnection() as java.net.HttpURLConnection).apply {
                        requestMethod = "GET"
                        connectTimeout = 10000
                        readTimeout = 10000
                        setRequestProperty("User-Agent", "Mozilla/5.0 (Android; MapRandoApp)")
                    }

                    val html = connection.inputStream.bufferedReader().use { it.readText() }
                    connection.disconnect()
                    parsedSprites = parseSpritesFromGitHubHtml(html)
                }

                if (parsedSprites.isNotEmpty()) {
                    com.maprandroid.data.model.SpriteCatalog.updateSprites(parsedSprites)
                    Result.success(com.maprandroid.data.model.SpriteCatalog.availableSprites)
                } else {
                    Result.success(com.maprandroid.data.model.SpriteCatalog.availableSprites)
                }
            } catch (e: Exception) {
                safeLogE("Failed to update sprite list from GitHub: ${e.message}", e)
                Result.failure(e)
            }
        }
    }

    open fun parseSpritesFromGitHubJson(json: String): List<String> {
        val sprites = LinkedHashSet<String>()
        val nameRegex = Regex("""["']name["']\s*:\s*["']([^"']+)["']""")

        nameRegex.findAll(json).forEach { m ->
            val name = m.groupValues[1].trim()
            val cleaned = cleanSpriteName(name)
            if (cleaned != null) {
                sprites.add(cleaned)
            }
        }

        return formatSpriteList(sprites)
    }

    open fun parseSpritesFromGitHubHtml(html: String): List<String> {
        val sprites = LinkedHashSet<String>()

        val treePathRegex = Regex("""samus_sprites/([a-zA-Z0-9_.-]+)""")
        treePathRegex.findAll(html).forEach { m ->
            val rawName = m.groupValues[1].trim()
            val cleaned = cleanSpriteName(rawName)
            if (cleaned != null) {
                sprites.add(cleaned)
            }
        }

        val jsonNameRegex = Regex("""["']name["']\s*:\s*["']([^"']+)["']""")
        jsonNameRegex.findAll(html).forEach { m ->
            val rawName = m.groupValues[1].trim()
            val cleaned = cleanSpriteName(rawName)
            if (cleaned != null) {
                sprites.add(cleaned)
            }
        }

        return formatSpriteList(sprites)
    }

    private fun cleanSpriteName(raw: String): String? {
        var name = raw.trim()
        if (name.isBlank()) return null

        val ignoredFiles = setOf(
            "readme", "readme.md", ".gitignore", "license", "license.txt",
            ".ds_store", "samus_sprites", "contents", "tree"
        )
        if (ignoredFiles.contains(name.lowercase())) return null

        val exts = listOf(".rdc", ".png", ".json", ".zip", ".sdc")
        for (ext in exts) {
            if (name.lowercase().endsWith(ext)) {
                name = name.substring(0, name.length - ext.length)
            }
        }

        if (name.isBlank() || name.contains("/") || name.contains("\\")) return null
        return name
    }

    private fun formatSpriteList(rawSprites: Collection<String>): List<String> {
        val result = LinkedHashSet<String>()
        result.add("samus_vanilla")
        result.addAll(rawSprites)
        return result.toList()
    }

    private fun parseAndCachePatchFromResponse(requestedSeedId: String, response: SeedDetailsResponse): String {
        val effectiveSeedId = response.getEffectiveSeedId().ifEmpty { requestedSeedId }
        val b64Patch = response.getPatchBase64()
        if (!b64Patch.isNullOrEmpty()) {
            val patchBytes = decodeBase64(b64Patch)
            if (patchBytes.isNotEmpty()) {
                seedPatchCache[effectiveSeedId] = patchBytes
                if (requestedSeedId.isNotBlank()) {
                    seedPatchCache[requestedSeedId] = patchBytes
                }
                safeLogI("Full MapRando API patch parsed from SeedDetailsResponse for seed $effectiveSeedId: ${patchBytes.size} bytes (${String.format(Locale.US, "%.2f", patchBytes.size / 1024.0)} KB)")
            }
        }
        return effectiveSeedId
    }

    suspend fun generateSeed(preset: String): Result<SeedDetailsResponse> {
        val request = GenerateSeedRequest(preset = preset)
        val attempts = listOf<suspend () -> SeedDetailsResponse>(
            { apiService.randomizeSeed(request) },
            { apiService.randomizeApiSeed(request) },
            { apiService.generateSeed(request) }
        )

        for (attempt in attempts) {
            try {
                val response = attempt()
                val finalSeedId = parseAndCachePatchFromResponse("", response).ifEmpty {
                    "SM_${preset.replace(" ", "_")}_${System.currentTimeMillis() % 1000000}"
                }
                return Result.success(response.copy(seedId = finalSeedId))
            } catch (_: Exception) {
            }
        }

        // Fallback seed generation for offline/dev environment
        val fallbackSeedId = "SM_${preset.replace(" ", "_")}_${(100000..999999).random()}"
        safeLogW("Network seed generation failed. Using offline fallback seed ID: $fallbackSeedId")
        return Result.success(SeedDetailsResponse(seedId = fallbackSeedId, hash = "MapRando_$preset"))
    }

    suspend fun generateCustomSeed(profile: ConfigProfileEntity): Result<SeedDetailsResponse> {
        val qolMap = mapOf(
            "fast_elevators" to profile.qolFastElevators,
            "fast_doors" to profile.qolFastDoors,
            "fast_text" to profile.qolFastText,
            "cutscene_skip" to profile.qolCutsceneSkip,
            "quick_morph" to profile.qolQuickMorph,
            "spin_jump_restart" to profile.qolSpinJumpRestart,
            "auto_charge_beam" to profile.qolAutoChargeBeam,
            "room_outline_revealed" to profile.qolRoomOutlineRevealed,
            "hazard_markers" to profile.qolHazardMarkers,
            "fix_blue_echoes" to profile.qolFixBlueEchoes,
            "early_save" to profile.qolEarlySave,
            "ammo_refill_all" to profile.qolAmmoRefillAll
        )

        val overrides = parseTechOverrides(profile.techOverridesJson)
        val defaultTechs = com.maprandroid.data.model.TechCatalog.getTechsForPreset(profile.logicSkill)
        val techMap = com.maprandroid.data.model.TechCatalog.ALL_TECHS.associate { tech ->
            tech.name to (overrides[tech.name] ?: defaultTechs[tech.name] ?: true)
        }

        val itemSettingsMap = mapOf(
            "item_progression" to profile.itemProgression,
            "progression_rate" to profile.progressionRate,
            "item_placement_style" to profile.itemPlacementStyle,
            "item_priority_strength" to profile.itemPriorityStrength,
            "item_pool" to profile.itemPool,
            "major_minor_distribution" to profile.majorMinorDistribution,
            "scavenger_hunt" to profile.scavengerHunt.toString(),
            "chozo_hunt" to profile.chozoHunt.toString(),
            "scavenger_required_count" to profile.scavengerRequiredCount.toString(),
            "super_missile_count" to profile.superMissileCount.toString(),
            "power_bomb_count" to profile.powerBombCount.toString(),
            "ammo_collect_fraction" to profile.ammoCollectFraction.toString(),
            "start_morph" to profile.startMorph.toString(),
            "start_charge" to profile.startCharge.toString(),
            "start_varia" to profile.startVaria.toString()
        )

        val mapSettingsMap = mapOf(
            "map_randomization" to profile.mapRandomization,
            "boss_randomization" to profile.bossRandomization,
            "door_lock_randomization" to profile.doorLockRandomization,
            "elevator_rando" to profile.elevatorRando.toString(),
            "escape_rando" to profile.escapeRando.toString(),
            "area_assignment_base_order" to profile.areaAssignmentBaseOrder,
            "ship_in_crateria" to profile.shipInCrateria.toString(),
            "mother_brain_in_tourian" to profile.motherBrainInTourian.toString(),
            "start_location_mode" to profile.startLocationMode,
            "start_location_room" to profile.startLocationRoom
        )

        val otherSettingsMap = mapOf(
            "logic_skill" to profile.logicSkill,
            "logic_options" to profile.logicOptions,
            "wall_jump_setting" to profile.wallJumpSetting,
            "speed_booster_setting" to profile.speedBoosterSetting,
            "door_locks_size_on_map" to profile.doorLocksSizeOnMap,
            "energy_free_shinesparks" to profile.energyFreeShinesparks.toString(),
            "all_enemies_respawn" to profile.allEnemiesRespawn.toString(),
            "disable_spikesuit" to profile.disableSpikesuit.toString(),
            "disable_bluesuit" to profile.disableBluesuit.toString(),
            "enable_major_glitches" to profile.enableMajorGlitches.toString(),
            "race_mode" to profile.raceMode.toString(),
            "savestate" to profile.savestate,
            "save_animals" to profile.saveAnimals,
            "difficulty_resource" to profile.difficultyResource,
            "damage_multiplier" to profile.damageMultiplier
        )

        val customSettingsMap = mutableMapOf<String, String>().apply {
            putAll(itemSettingsMap)
            putAll(mapSettingsMap)
            putAll(otherSettingsMap)
            techMap.forEach { (k, v) -> put("tech_$k", v.toString()) }
            qolMap.forEach { (k, v) -> put("qol_$k", v.toString()) }
        }

        val request = GenerateSeedRequest(
            preset = "Custom: ${profile.name}",
            customSettings = customSettingsMap,
            logic = profile.logicSkill,
            areaAssignment = profile.areaAssignmentBaseOrder,
            areaLayout = profile.mapRandomization,
            itemProgression = profile.itemProgression,
            itemPool = profile.itemPool,
            mapRandomization = profile.mapRandomization,
            qolSettings = qolMap,
            techSettings = techMap,
            itemSettings = itemSettingsMap,
            mapSettings = mapSettingsMap,
            otherSettings = otherSettingsMap,
            difficulty = "${profile.difficultyResource} (${profile.damageMultiplier})"
        )

        val attempts = listOf<suspend () -> SeedDetailsResponse>(
            { apiService.randomizeSeed(request) },
            { apiService.randomizeApiSeed(request) },
            { apiService.generateSeed(request) }
        )

        for (attempt in attempts) {
            try {
                val response = attempt()
                val finalSeedId = parseAndCachePatchFromResponse("", response).ifEmpty {
                    "SM_Custom_${System.currentTimeMillis() % 1000000}"
                }
                return Result.success(response.copy(seedId = finalSeedId))
            } catch (_: Exception) {
            }
        }

        // Fallback seed generation for offline/dev environment
        val sanitizedName = profile.name.replace(" ", "_").take(15)
        val fallbackSeedId = "SM_${sanitizedName}_${(100000..999999).random()}"
        safeLogW("Network custom seed generation failed. Using offline fallback seed ID: $fallbackSeedId")
        return Result.success(SeedDetailsResponse(seedId = fallbackSeedId, hash = "MapRando_${profile.name}"))
    }

    fun buildSettingsJson(profile: ConfigProfileEntity): String {
        val areaAssignmentObj = mapOf<String, Any>(
            "preset" to profile.areaAssignmentBaseOrder,
            "base_order" to profile.areaAssignmentBaseOrder,
            "ship_in_crateria" to profile.shipInCrateria,
            "mother_brain_in_tourian" to profile.motherBrainInTourian
        )

        val otherSettings = mapOf<String, Any?>(
            "wall_jump" to profile.wallJumpSetting,
            "speed_booster" to profile.speedBoosterSetting,
            "area_assignment" to areaAssignmentObj,
            "door_locks_size" to profile.doorLocksSizeOnMap,
            "maps_revealed" to null,
            "map_station_reveal" to "Full",
            "energy_free_shinesparks" to profile.energyFreeShinesparks,
            "all_enemies_respawn" to profile.allEnemiesRespawn,
            "race_mode" to profile.raceMode,
            "savestate" to profile.savestate,
            "disable_spikesuit" to profile.disableSpikesuit,
            "disable_bluesuit" to profile.disableBluesuit,
            "enable_major_glitches" to profile.enableMajorGlitches
        )

        val defaultTechs = com.maprandroid.data.model.TechCatalog.getTechsForPreset(profile.logicSkill)
        val overrides = parseTechOverrides(profile.techOverridesJson)

        val techSettingsList = com.maprandroid.data.model.TechCatalog.ALL_TECHS.map { tech ->
            val enabled = overrides[tech.name] ?: defaultTechs[tech.name] ?: true
            mapOf("id" to tech.id, "name" to tech.name, "enabled" to enabled)
        }

        val mapSettings = mapOf<String, Any?>(
            "preset" to profile.mapRandomization,
            "source" to profile.mapRandomization,
            "area_assignment" to areaAssignmentObj,
            "generation" to mapOf(
                "small_map" to (profile.mapRandomization == "Small"),
                "landmarks" to emptyList<String>(),
                "maridia_water" to mapOf("preset" to "Unrestricted", "probabilities" to listOf(0.33, 0.33, 0.34)),
                "norfair_heat" to mapOf("preset" to "Unrestricted", "probabilities" to listOf(0.33, 0.33, 0.34)),
                "reward_missing_connect_utility" to 1.0,
                "reward_area_x" to 1.0,
                "reward_area_y" to 1.0,
                "target_area_rooms" to listOf(0.0, 0.0, 0.0, 0.0, 0.0, 0.0),
                "target_area_x" to listOf(0.0, 0.0, 0.0, 0.0, 0.0, 0.0),
                "target_area_y" to listOf(0.0, 0.0, 0.0, 0.0, 0.0, 0.0),
                "reward_graph_diameter" to 1.0,
                "reward_save_distance" to 1.0,
                "reward_refill_distance" to 1.0,
                "reward_area_distinct_crossing" to 1.0
            )
        )

        val skillAssumptionSettings = mapOf<String, Any>(
            "preset" to profile.logicSkill.ifEmpty { "Medium" },
            "shinespark_tiles" to profile.shinesparkTiles,
            "heated_shinespark_tiles" to profile.heatedShinesparkTiles,
            "speed_ball_tiles" to 0.0,
            "shinecharge_leniency_frames" to 0,
            "resource_multiplier" to profile.resourceMultiplier,
            "farm_time_limit" to profile.farmTimeLimit,
            "gate_glitch_leniency" to 0,
            "door_stuck_leniency" to 0,
            "bomb_into_cf_leniency" to 0,
            "jump_into_cf_leniency" to 0,
            "flash_suit_distance" to 0,
            "blue_suit_distance" to 0,
            "spike_suit_leniency" to 0,
            "spike_xmode_leniency" to 0,
            "spike_speed_keep_leniency" to 0,
            "elevator_cf_leniency" to 0,
            "crystal_spark_leniency" to 0,
            "phantoon_proficiency" to 1.0,
            "draygon_proficiency" to 1.0,
            "ridley_proficiency" to 1.0,
            "botwoon_proficiency" to 1.0,
            "mother_brain_proficiency" to 1.0,
            "escape_timer_multiplier" to profile.escapeTimerMultiplier,
            "tech_settings" to techSettingsList,
            "notable_settings" to emptyList<Map<String, Any>>()
        )

        val itemProgressionSettings = mapOf<String, Any>(
            "preset" to profile.itemProgression.ifEmpty { "Normal" },
            "progression_rate" to profile.progressionRate,
            "item_placement_style" to profile.itemPlacementStyle,
            "item_priority_strength" to profile.itemPriorityStrength,
            "random_tank" to profile.randomTank,
            "spazer_before_plasma" to profile.spazerBeforePlasma,
            "item_pool_preset" to profile.itemPool,
            "major_minor_distribution" to profile.majorMinorDistribution,
            "scavenger_hunt" to profile.scavengerHunt,
            "chozo_hunt" to profile.chozoHunt,
            "scavenger_required_count" to profile.scavengerRequiredCount,
            "ammo_collect_fraction" to profile.ammoCollectFraction,
            "super_missile_count" to profile.superMissileCount,
            "power_bomb_count" to profile.powerBombCount
        )

        val qualityOfLifeSettings = mapOf<String, Any>(
            "preset" to profile.qolPreset,
            "fast_elevators" to profile.qolFastElevators,
            "fast_doors" to profile.qolFastDoors,
            "fast_text" to profile.qolFastText,
            "cutscene_skip" to profile.qolCutsceneSkip,
            "quick_morph" to profile.qolQuickMorph,
            "spin_jump_restart" to profile.qolSpinJumpRestart,
            "auto_charge_beam" to profile.qolAutoChargeBeam,
            "mother_brain_fight" to profile.qolMotherBrainFight,
            "item_markers" to profile.qolItemMarkers,
            "room_outline_revealed" to profile.qolRoomOutlineRevealed,
            "hazard_markers" to profile.qolHazardMarkers,
            "fix_blue_echoes" to profile.qolFixBlueEchoes,
            "early_save" to profile.qolEarlySave,
            "ammo_refill_all" to profile.qolAmmoRefillAll
        )

        val objectiveOptions = listOf(
            mapOf("objective" to "Kraid", "setting" to if (profile.kraidObjective) "Yes" else "No"),
            mapOf("objective" to "Phantoon", "setting" to if (profile.phantoonObjective) "Yes" else "No"),
            mapOf("objective" to "Draygon", "setting" to if (profile.draygonObjective) "Yes" else "No"),
            mapOf("objective" to "Ridley", "setting" to if (profile.ridleyObjective) "Yes" else "No"),
            mapOf("objective" to "Spore Spawn", "setting" to if (profile.sporeSpawnObjective) "Yes" else "No"),
            mapOf("objective" to "Crocomire", "setting" to if (profile.crocomireObjective) "Yes" else "No"),
            mapOf("objective" to "Botwoon", "setting" to if (profile.botwoonObjective) "Yes" else "No"),
            mapOf("objective" to "Golden Torizo", "setting" to if (profile.goldenTorizoObjective) "Yes" else "No"),
            mapOf("objective" to "Bomb Torizo", "setting" to if (profile.bombTorizoObjective) "Yes" else "No"),
            mapOf("objective" to "Chozo Statues", "setting" to if (profile.chozoStatuesObjective) "Yes" else "No"),
            mapOf("objective" to "Pirates", "setting" to if (profile.piratesObjective) "Yes" else "No"),
            mapOf("objective" to "Metroids", "setting" to if (profile.metroidsObjective) "Yes" else "No")
        )

        val objectiveSettings = mapOf<String, Any>(
            "preset" to profile.objectivePreset,
            "objective_options" to objectiveOptions,
            "min_objectives" to profile.minObjectives,
            "max_objectives" to profile.maxObjectives
        )

        val doorsSettings = mapOf<String, Any>(
            "preset" to profile.doorsPreset,
            "red_doors_count" to profile.redDoorsCount,
            "green_doors_count" to profile.greenDoorsCount,
            "yellow_doors_count" to profile.yellowDoorsCount,
            "beam_doors_count" to profile.beamDoorsCount
        )

        val startLocationSettings = mapOf<String, Any>(
            "mode" to profile.startLocationMode,
            "start_location" to profile.startLocationRoom
        )

        val settingsMap = mapOf<String, Any?>(
            "version" to 123,
            "name" to profile.name.ifEmpty { "Custom Seed" },
            "skill_assumption_settings" to skillAssumptionSettings,
            "item_progression_settings" to itemProgressionSettings,
            "quality_of_life_settings" to qualityOfLifeSettings,
            "objective_settings" to objectiveSettings,
            "map_settings" to mapSettings,
            "map_layout" to profile.mapRandomization,
            "boss_randomization" to profile.bossRandomization,
            "door_lock_randomization" to profile.doorLockRandomization,
            "elevator_rando" to profile.elevatorRando,
            "escape_rando" to profile.escapeRando,
            "doors_settings" to doorsSettings,
            "start_location_settings" to startLocationSettings,
            "save_animals" to profile.saveAnimals,
            "other_settings" to otherSettings
        )
        val moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()
        val adapter = moshi.adapter(Map::class.java)
        return adapter.toJson(settingsMap)
    }

    private fun parseTechOverrides(jsonStr: String): Map<String, Boolean> {
        if (jsonStr.isBlank() || jsonStr == "{}") return emptyMap()
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
            emptyMap()
        }
    }

    data class Step1RandomizeResult(
        val seedId: String,
        val customizeUrl: String,
        val unheaderedBaseRomBytes: ByteArray
    )

    suspend fun randomizeSeedStep1(
        baseRomBytes: ByteArray,
        settings: ConfigProfileEntity
    ): Result<Step1RandomizeResult> {
        return try {
            val octetMediaType = "application/octet-stream".toMediaTypeOrNull()

            val unheaderedBytes = if (baseRomBytes.size == 3146240 || baseRomBytes.size % 1024 == 512) {
                baseRomBytes.copyOfRange(512, baseRomBytes.size)
            } else {
                baseRomBytes
            }

            val spoilerToken = "".toRequestBody(null)
            val settingsJsonStr = buildSettingsJson(settings)
            val settingsJson = settingsJsonStr.toRequestBody(null)

            val romBody = unheaderedBytes.toRequestBody(octetMediaType)
            val romPart = MultipartBody.Part.createFormData("rom", "Super Metroid (JU) [!].smc", romBody)

            val response1 = apiService.randomizeMultipart(
                spoilerToken = spoilerToken,
                settingsJson = settingsJson,
                rom = romPart
            )

            val code1 = response1.code()
            val message1 = response1.message()
            if (!response1.isSuccessful) {
                val errBody = response1.errorBody()?.string() ?: ""
                safeLogE("Step 1 randomize HTTP $code1 ($message1) failed. Response body: $errBody")
                throw IllegalStateException("Step 1 randomize HTTP $code1 failed: $errBody")
            }

            val response1Body = response1.body()
                ?: run {
                    safeLogE("Step 1 randomize returned null body (HTTP $code1 $message1)")
                    throw IllegalStateException("Step 1 randomize returned null body (HTTP $code1)")
                }

            val response1String = response1Body.string()
            safeLogI("Step 1 randomize success: HTTP $code1 $message1. Response body: $response1String")

            val moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()
            val mapAdapter = moshi.adapter(Map::class.java)
            val jsonMap = try { mapAdapter.fromJson(response1String) } catch (_: Exception) { null }

            val rawSeedUrl = (jsonMap?.get("seed_url") as? String)
                ?: (jsonMap?.get("url") as? String)
                ?: (jsonMap?.get("seed_id") as? String ?: jsonMap?.get("id") as? String)?.let { "/seed/$it/" }
                ?: throw IllegalStateException("Step 1 response missing seed_url/seed_id: $response1String")

            val seedUrlClean = rawSeedUrl.trim()
            val customizeUrl = if (seedUrlClean.startsWith("http")) {
                "${seedUrlClean.removeSuffix("/")}/customize"
            } else {
                "https://maprando.com/${seedUrlClean.trim('/')}/customize"
            }

            val extractedSeedId = seedUrlClean.trim('/').substringAfterLast('/')
            safeLogI("Step 1 randomize successful for seed $extractedSeedId -> customizeUrl: $customizeUrl")
            Result.success(
                Step1RandomizeResult(
                    seedId = extractedSeedId,
                    customizeUrl = customizeUrl,
                    unheaderedBaseRomBytes = unheaderedBytes
                )
            )
        } catch (e: Exception) {
            safeLogE("randomizeSeedStep1 failed with exception: ${e.message}", e)
            Result.failure(e)
        }
    }

    suspend fun customizeSeedStep2(
        customizeUrl: String,
        unheaderedBaseRomBytes: ByteArray,
        settings: ConfigProfileEntity
    ): Result<ByteArray> {
        return try {
            val octetMediaType = "application/octet-stream".toMediaTypeOrNull()

            fun getShortcutFieldName(prefix: String, buttonLabel: String): String {
                val suffix = when (buttonLabel.lowercase()) {
                    "a" -> "a"
                    "b" -> "b"
                    "x" -> "x"
                    "y" -> "y"
                    "l" -> "l"
                    "r" -> "r"
                    "select" -> "select"
                    "start" -> "start"
                    "up" -> "up"
                    "down" -> "down"
                    "left" -> "left"
                    "right" -> "right"
                    else -> buttonLabel.lowercase()
                }
                return "${prefix}_$suffix"
            }

            val builder = MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("rom", "Super Metroid (JU) [!].smc", unheaderedBaseRomBytes.toRequestBody(octetMediaType))
                .addFormDataPart("samus_sprite", settings.samusSprite)
                .addFormDataPart("etank_color", settings.etankColor)
                .addFormDataPart("reserve_hud_style", settings.reserveHudStyle.toString())
                .addFormDataPart("transition_letters", settings.transitionLetters.toString())
                .addFormDataPart("map_theme", settings.mapTheme)
                .addFormDataPart("room_palettes", settings.roomPalettes)
                .addFormDataPart("tile_theme", settings.tileTheme)
                .addFormDataPart("door_theme", settings.doorTheme)
                .addFormDataPart("music", settings.music)
                .addFormDataPart("shaking", settings.shaking)
                .addFormDataPart("flashing", settings.flashing)
                .addFormDataPart("disable_beeping", settings.disableBeeping.toString())
                .addFormDataPart("item_dot_change", settings.itemDotChange)
                .addFormDataPart("room_names", settings.roomNames.toString())
                .addFormDataPart("boss_icons", settings.bossIcons.toString())
                .addFormDataPart("miniboss_icons", settings.minibossIcons.toString())
                .addFormDataPart("save_icons", settings.saveIcons.toString())
                .addFormDataPart("statues_hallway_tiling", settings.statuesHallwayTiling)
                .addFormDataPart("statues_hallway_audio", settings.statuesHallwayAudio)
                .addFormDataPart("vanilla_screw_attack_animation", settings.vanillaScrewAttackAnimation.toString())
                // Controller Mappings
                .addFormDataPart("control_shot", settings.controlShot)
                .addFormDataPart("control_jump", settings.controlJump)
                .addFormDataPart("control_dash", settings.controlDash)
                .addFormDataPart("control_item_select", settings.controlItemSelect)
                .addFormDataPart("control_item_cancel", settings.controlItemCancel)
                .addFormDataPart("control_angle_up", settings.controlAngleUp)
                .addFormDataPart("control_angle_down", settings.controlAngleDown)
                .addFormDataPart("moonwalk", settings.moonwalk.toString())

            // Quick Reload Buttons - MapRandomizer Rust server checks `if let Some(x) = setting && x.0 == "on"`
            val quickReloadList = settings.quickReloadButtons.split(",").map { it.trim() }.filter { it.isNotEmpty() }
            for (btn in quickReloadList) {
                builder.addFormDataPart(getShortcutFieldName("quick_reload", btn), "on")
            }

            // Save State Buttons - MapRandomizer Rust server checks `if let Some(x) = setting && x.0 == "on"`
            val saveStateList = settings.saveStateButtons.split(",").map { it.trim() }.filter { it.isNotEmpty() }
            for (btn in saveStateList) {
                builder.addFormDataPart(getShortcutFieldName("save_state", btn), "on")
            }

            // Load State Buttons - MapRandomizer Rust server checks `if let Some(x) = setting && x.0 == "on"`
            val loadStateList = settings.loadStateButtons.split(",").map { it.trim() }.filter { it.isNotEmpty() }
            for (btn in loadStateList) {
                builder.addFormDataPart(getShortcutFieldName("load_state", btn), "on")
            }

            // Spin Lock Buttons - MapRandomizer Rust server checks `if let Some(x) = setting && x.0 == "on"`
            val spinLocks = listOfNotNull(
                if (settings.spinLockX) "X" else null,
                if (settings.spinLockL) "L" else null,
                if (settings.spinLockR) "R" else null,
                if (settings.spinLockUp) "Up" else null
            )
            for (btn in spinLocks) {
                builder.addFormDataPart(getShortcutFieldName("spin_lock", btn), "on")
            }

            val response2 = apiService.customizeRom(customizeUrl, builder.build())

            val code2 = response2.code()
            val message2 = response2.message()
            if (!response2.isSuccessful) {
                val errBody = response2.errorBody()?.string() ?: ""
                safeLogE("Step 2 customize HTTP $code2 ($message2) failed at URL $customizeUrl. Response body: $errBody")
                throw IllegalStateException("Step 2 customize HTTP $code2 failed at $customizeUrl: $errBody")
            }

            val response2Body = response2.body()
                ?: run {
                    safeLogE("Step 2 customize returned null body at URL $customizeUrl (HTTP $code2 $message2)")
                    throw IllegalStateException("Step 2 customize returned null body at $customizeUrl (HTTP $code2)")
                }

            val patchedRomBytes = response2Body.bytes()
            if (patchedRomBytes.size < 1000000) {
                val errorStr = if (patchedRomBytes.isNotEmpty()) String(patchedRomBytes, Charsets.UTF_8) else "Empty response"
                safeLogE("Step 2 customize returned invalid ROM data (size ${patchedRomBytes.size}) at URL $customizeUrl. Response: $errorStr")
                throw IllegalStateException("Step 2 customize returned invalid ROM data (size ${patchedRomBytes.size}) at $customizeUrl: $errorStr")
            }

            safeLogI("Step 2 customize successful. Received complete patched ROM (${patchedRomBytes.size} bytes)")
            Result.success(patchedRomBytes)
        } catch (e: Exception) {
            safeLogE("customizeSeedStep2 failed with exception: ${e.message}", e)
            Result.failure(e)
        }
    }

    suspend fun generateAndCustomizeRom(
        baseRomBytes: ByteArray,
        settings: ConfigProfileEntity
    ): Result<CustomizedRomResult> {
        val step1Res = randomizeSeedStep1(baseRomBytes, settings).getOrElse { return Result.failure(it) }
        val step2Res = customizeSeedStep2(step1Res.customizeUrl, step1Res.unheaderedBaseRomBytes, settings).getOrElse { return Result.failure(it) }
        return Result.success(CustomizedRomResult(seedId = step1Res.seedId, romBytes = step2Res))
    }

    private fun parsePatchFromResponseBody(seedId: String, rawBytes: ByteArray): ByteArray {
        if (rawBytes.isEmpty()) return rawBytes

        // Check if rawBytes is JSON or Base64 string
        try {
            val strCandidate = String(rawBytes, Charsets.UTF_8).trim()
            if (strCandidate.startsWith("{") || strCandidate.startsWith("[")) {
                val moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()
                val adapter = moshi.adapter(SeedDetailsResponse::class.java)
                val responseObj = adapter.fromJson(strCandidate)
                val b64Patch = responseObj?.getPatchBase64()
                if (!b64Patch.isNullOrEmpty()) {
                    val decoded = decodeBase64(b64Patch)
                    if (decoded.isNotEmpty()) {
                        safeLogI("Parsed full Base64 patch (${decoded.size} bytes / ${String.format(Locale.US, "%.2f", decoded.size / 1024.0)} KB) from JSON response for seed $seedId")
                        return decoded
                    }
                }
            } else if (strCandidate.length >= 8 && strCandidate.all { it in "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/=\r\n" }) {
                val decoded = decodeBase64(strCandidate)
                if (decoded.isNotEmpty()) {
                    safeLogI("Parsed raw Base64 patch string (${decoded.size} bytes / ${String.format(Locale.US, "%.2f", decoded.size / 1024.0)} KB) for seed $seedId")
                    return decoded
                }
            }
        } catch (_: Throwable) {
        }

        safeLogI("Retrieved raw binary patch/ROM response (${rawBytes.size} bytes / ${String.format(Locale.US, "%.2f", rawBytes.size / 1024.0)} KB) for seed $seedId")
        return rawBytes
    }

    suspend fun downloadRom(seedId: String): Result<ResponseBody> {
        // 1. Check in-memory patch cache
        val cached = seedPatchCache[seedId]
        if (cached != null && cached.isNotEmpty()) {
            safeLogI("Using cached full MapRando patch for seed $seedId (${cached.size} bytes / ${String.format(Locale.US, "%.2f", cached.size / 1024.0)} KB)")
            val mediaType = "application/octet-stream".toMediaTypeOrNull()
            return Result.success(cached.toResponseBody(mediaType))
        }

        // 2. Query network endpoints
        val downloadCalls = listOf<suspend () -> ResponseBody>(
            { apiService.downloadRom(seedId) },
            { apiService.downloadPatch(seedId) },
            { apiService.downloadApiPatch(seedId) },
            { apiService.downloadSeedDownload(seedId) }
        )

        var lastError: Throwable? = null

        for (call in downloadCalls) {
            try {
                val responseBody = call()
                val rawBytes = responseBody.bytes()
                if (rawBytes.isNotEmpty()) {
                    val parsedPatch = parsePatchFromResponseBody(seedId, rawBytes)
                    seedPatchCache[seedId] = parsedPatch
                    val mediaType = "application/octet-stream".toMediaTypeOrNull()
                    return Result.success(parsedPatch.toResponseBody(mediaType))
                }
            } catch (e: Throwable) {
                lastError = e
            }
        }

        // 3. Try details API endpoint as fallback
        try {
            val detailsRes = getSeedDetails(seedId)
            detailsRes.getOrNull()?.let { details ->
                val b64 = details.getPatchBase64()
                if (!b64.isNullOrEmpty()) {
                    val decoded = decodeBase64(b64)
                    if (decoded.isNotEmpty()) {
                        seedPatchCache[seedId] = decoded
                        safeLogI("Fetched full Base64 patch via getSeedDetails for seed $seedId (${decoded.size} bytes / ${String.format(Locale.US, "%.2f", decoded.size / 1024.0)} KB)")
                        val mediaType = "application/octet-stream".toMediaTypeOrNull()
                        return Result.success(decoded.toResponseBody(mediaType))
                    }
                }
            }
        } catch (_: Throwable) {
        }

        return Result.failure(lastError ?: Exception("Unable to download patch for seed $seedId"))
    }
}
