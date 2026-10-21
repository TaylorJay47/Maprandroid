package com.maprandroid.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "config_profiles")
data class ConfigProfileEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,

    // 1. Skill Assumptions
    val logicSkill: String = "Medium", // Basic, Medium, Hard, Very Hard, Expert, Expert+, Extreme, Extreme+, Insane, Insane+
    val techOverridesJson: String = "{}",
    val techWallJump: Boolean = true,
    val techIBJ: Boolean = true,
    val techShinespark: Boolean = true,
    val techShortCharge: Boolean = false,
    val techMockball: Boolean = true,
    val techSpringBall: Boolean = false,
    val techMidAirMorph: Boolean = false,
    val techDamageBoosts: Boolean = false,
    val logicOptions: String = "Default",
    val shinesparkTiles: Int = 25,
    val heatedShinesparkTiles: Int = 29,
    val resourceMultiplier: Float = 2.5f,
    val farmTimeLimit: Int = 90,
    val escapeTimerMultiplier: Float = 2.0f,

    // 2. Item Progression
    val itemProgression: String = "Normal", // Normal, Tricky, Technical, Challenge, Desolate
    val progressionRate: String = "Uniform", // Uniform, Fast, Slow
    val itemPlacementStyle: String = "Local", // Local, Unrestricted
    val itemPriorityStrength: String = "Moderate", // Moderate, High, Low
    val randomTank: Boolean = true,
    val spazerBeforePlasma: Boolean = true,
    val itemPool: String = "Major/Minor", // Major/Minor, Full, Chozo, Scavenger
    val majorMinorDistribution: String = "Standard", // Standard, Choice, Shuffled
    val scavengerHunt: Boolean = false,
    val chozoHunt: Boolean = false,
    val scavengerRequiredCount: Int = 10,
    val superMissileCount: Int = 15,
    val powerBombCount: Int = 10,
    val startMorph: Boolean = false,
    val startCharge: Boolean = false,
    val startVaria: Boolean = false,
    val ammoCollectFraction: Float = 0.7f,

    // 3. Quality of Life Options
    val qolPreset: String = "Default", // Off, Low, Default, High, Max
    val qolFastElevators: Boolean = true,
    val qolFastDoors: Boolean = true,
    val qolFastText: Boolean = true,
    val qolCutsceneSkip: Boolean = true,
    val qolQuickMorph: Boolean = true,
    val qolSpinJumpRestart: Boolean = true,
    val qolAutoChargeBeam: Boolean = false,
    val qolMotherBrainFight: String = "Short", // Short, Vanilla, Skip
    val qolItemMarkers: String = "Simple", // 3-Tiered, Simple, None
    val qolRoomOutlineRevealed: Boolean = true,
    val qolHazardMarkers: Boolean = true,
    val qolFixBlueEchoes: Boolean = true,
    val qolEarlySave: Boolean = true,
    val qolAmmoRefillAll: Boolean = true,

    // 4. Objectives
    val objectivePreset: String = "Bosses", // None, Bosses, Minibosses, Chozos, Pirates, Metroids, Random
    val kraidObjective: Boolean = true,
    val phantoonObjective: Boolean = true,
    val draygonObjective: Boolean = true,
    val ridleyObjective: Boolean = true,
    val sporeSpawnObjective: Boolean = false,
    val crocomireObjective: Boolean = false,
    val botwoonObjective: Boolean = false,
    val goldenTorizoObjective: Boolean = false,
    val bombTorizoObjective: Boolean = false,
    val chozoStatuesObjective: Boolean = false,
    val piratesObjective: Boolean = false,
    val metroidsObjective: Boolean = false,
    val minObjectives: Int = 4,
    val maxObjectives: Int = 4,

    // 5. Map Layout
    val mapRandomization: String = "Vanilla", // Vanilla, Small, Standard, Wild
    val bossRandomization: String = "Vanilla", // Vanilla, Randomized, Chaos
    val doorLockRandomization: String = "Vanilla", // Vanilla, Color, Single/Double, Randomized
    val elevatorRando: Boolean = false,
    val escapeRando: Boolean = false,
    val areaAssignmentBaseOrder: String = "Depth", // Standard, Size, Depth, Random
    val shipInCrateria: Boolean = false,
    val motherBrainInTourian: Boolean = false,

    // 6. Doors
    val doorsPreset: String = "Ammo", // Blue, Ammo, Beam
    val redDoorsCount: Int = 30,
    val greenDoorsCount: Int = 15,
    val yellowDoorsCount: Int = 10,
    val beamDoorsCount: Int = 0,

    // 7. Start Location
    val startLocationMode: String = "Ship", // Ship, Random, Escape, Custom
    val startLocationRoom: String = "Landing Site",

    // 8. Save the Animals
    val saveAnimals: String = "Optional", // No, Yes, Optional, Random

    // 9. Other Options
    val wallJumpSetting: String = "Vanilla", // Vanilla, Collectible
    val speedBoosterSetting: String = "Vanilla", // Vanilla, Split
    val doorLocksSizeOnMap: String = "Large", // Small, Large
    val energyFreeShinesparks: Boolean = false,
    val allEnemiesRespawn: Boolean = false,
    val disableSpikesuit: Boolean = false,
    val disableBluesuit: Boolean = false,
    val enableMajorGlitches: Boolean = false,
    val raceMode: Boolean = false,
    val savestate: String = "No", // No, Limited, Unlimited

    // 10. Cosmetics & ROM Customization & Controls
    val samusSprite: String = "samus_vanilla",
    val etankColor: String = "96de38",
    val reserveHudStyle: Boolean = true,
    val transitionLetters: Boolean = true,
    val mapTheme: String = "Light",
    val roomPalettes: String = "vanilla",
    val tileTheme: String = "none",
    val doorTheme: String = "vanilla",
    val music: String = "area",
    val shaking: String = "Vanilla",
    val flashing: String = "Vanilla",
    val disableBeeping: Boolean = false,
    val itemDotChange: String = "Fade",
    val roomNames: Boolean = true,
    val bossIcons: Boolean = true,
    val minibossIcons: Boolean = true,
    val saveIcons: Boolean = true,
    val statuesHallwayTiling: String = "Default",
    val statuesHallwayAudio: String = "Enabled",
    val vanillaScrewAttackAnimation: Boolean = false,

    val controlShot: String = "X",
    val controlJump: String = "A",
    val controlDash: String = "B",
    val controlItemSelect: String = "Select",
    val controlItemCancel: String = "Y",
    val controlAngleUp: String = "R",
    val controlAngleDown: String = "L",

    val quickReloadL: Boolean = false,
    val quickReloadR: Boolean = false,
    val quickReloadSelect: Boolean = false,
    val quickReloadStart: Boolean = false,

    val saveStateSelect: Boolean = true,
    val saveStateR: Boolean = true,
    val saveStateL: Boolean = true,
    val saveStateStart: Boolean = false,

    val loadStateSelect: Boolean = true,
    val loadStateL: Boolean = true,
    val loadStateR: Boolean = true,
    val loadStateStart: Boolean = false,

    val quickReloadButtons: String = "L,R,Select,Start",
    val saveStateButtons: String = "X,L,R,Select",
    val loadStateButtons: String = "Y,L,R,Select",

    val spinLockX: Boolean = false,
    val spinLockL: Boolean = false,
    val spinLockR: Boolean = false,
    val spinLockUp: Boolean = false,

    val moonwalk: Boolean = true,

    val aimButtonSetting: String = "Standard", // Standard, Separate, One Button
    val mapIcons: String = "Standard", // Standard, Detailed, Hidden
    val itemMarkers: String = "Simple", // Simple, Detailed, None
    val musicShuffle: Boolean = false,
    val paletteShuffle: Boolean = false,
    val difficultyResource: String = "Standard",
    val damageMultiplier: String = "1.0x", // 0.5x, 1.0x, 1.5x, 2.0x, 3.0x

    val updatedAt: Long = System.currentTimeMillis()
)

typealias ConfigProfile = ConfigProfileEntity

object ConfigProfileJsonAdapter {
    private val moshi by lazy {
        com.squareup.moshi.Moshi.Builder()
            .addLast(com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory())
            .build()
    }
    private val adapter by lazy { moshi.adapter(ConfigProfileEntity::class.java) }

    fun toJson(profile: ConfigProfileEntity): String {
        return try {
            adapter.toJson(profile)
        } catch (_: Exception) {
            "{}"
        }
    }

    fun fromJson(jsonStr: String, fallbackLogicSkill: String = "Medium", fallbackMapLayout: String = "Vanilla"): ConfigProfileEntity {
        if (jsonStr.isBlank() || jsonStr == "{}") {
            return ConfigProfileEntity(
                name = "Generated Seed",
                logicSkill = fallbackLogicSkill.ifEmpty { "Medium" },
                mapRandomization = fallbackMapLayout.ifEmpty { "Vanilla" }
            )
        }
        return try {
            adapter.fromJson(jsonStr) ?: ConfigProfileEntity(
                name = "Generated Seed",
                logicSkill = fallbackLogicSkill.ifEmpty { "Medium" },
                mapRandomization = fallbackMapLayout.ifEmpty { "Vanilla" }
            )
        } catch (_: Exception) {
            ConfigProfileEntity(
                name = "Generated Seed",
                logicSkill = fallbackLogicSkill.ifEmpty { "Medium" },
                mapRandomization = fallbackMapLayout.ifEmpty { "Vanilla" }
            )
        }
    }
}

val ALL_SNES_BUTTONS = listOf("A", "B", "X", "Y", "L", "R", "Select", "Start", "Up", "Down", "Left", "Right")

fun ConfigProfileEntity.toggleQuickReloadButton(button: String, enabled: Boolean): ConfigProfileEntity {
    val currentSet = quickReloadButtons.split(",").map { it.trim() }.filter { it.isNotEmpty() }.toMutableSet()
    if (enabled) currentSet.add(button) else currentSet.remove(button)
    val newStr = currentSet.joinToString(",")
    return copy(
        quickReloadButtons = newStr,
        quickReloadL = currentSet.contains("L"),
        quickReloadR = currentSet.contains("R"),
        quickReloadSelect = currentSet.contains("Select"),
        quickReloadStart = currentSet.contains("Start")
    )
}

fun ConfigProfileEntity.toggleSaveStateButton(button: String, enabled: Boolean): ConfigProfileEntity {
    val currentSet = saveStateButtons.split(",").map { it.trim() }.filter { it.isNotEmpty() }.toMutableSet()
    if (enabled) currentSet.add(button) else currentSet.remove(button)
    val newStr = currentSet.joinToString(",")
    return copy(
        saveStateButtons = newStr,
        saveStateSelect = currentSet.contains("Select"),
        saveStateR = currentSet.contains("R"),
        saveStateL = currentSet.contains("L"),
        saveStateStart = currentSet.contains("Start")
    )
}

fun ConfigProfileEntity.toggleLoadStateButton(button: String, enabled: Boolean): ConfigProfileEntity {
    val currentSet = loadStateButtons.split(",").map { it.trim() }.filter { it.isNotEmpty() }.toMutableSet()
    if (enabled) currentSet.add(button) else currentSet.remove(button)
    val newStr = currentSet.joinToString(",")
    return copy(
        loadStateButtons = newStr,
        loadStateSelect = currentSet.contains("Select"),
        loadStateL = currentSet.contains("L"),
        loadStateR = currentSet.contains("R"),
        loadStateStart = currentSet.contains("Start")
    )
}
