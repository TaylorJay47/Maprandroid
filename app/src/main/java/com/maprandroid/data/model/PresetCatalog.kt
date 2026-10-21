package com.maprandroid.data.model

import com.maprandroid.data.local.ConfigProfileEntity

data class PresetDefinition(
    val id: String,
    val name: String,
    val description: String,
    val profile: ConfigProfileEntity
)

object PresetCatalog {

    val CASUAL_EXPLORER = PresetDefinition(
        id = "casual_explorer",
        name = "Casual Explorer",
        description = "Beginner-friendly preset with Casual logic, max QoL, auto charge beam, and vanilla map layout.",
        profile = ConfigProfileEntity(
            name = "Casual Explorer",
            logicSkill = "Casual",
            startLocationMode = "Ship",
            techWallJump = true, techIBJ = false, techShinespark = false, techShortCharge = false,
            techMockball = false, techSpringBall = false, techMidAirMorph = false, techDamageBoosts = false,
            logicOptions = "Default",
            itemProgression = "Normal", itemPool = "Major/Minor", majorMinorDistribution = "Standard",
            mapRandomization = "Vanilla", bossRandomization = "Vanilla", doorLockRandomization = "Vanilla",
            elevatorRando = false, escapeRando = false,
            qolFastElevators = true, qolFastDoors = true, qolFastText = true, qolCutsceneSkip = true, qolQuickMorph = true, qolSpinJumpRestart = true, qolAutoChargeBeam = true,
            difficultyResource = "Casual", damageMultiplier = "1.0x"
        )
    )

    val STANDARD = PresetDefinition(
        id = "standard",
        name = "Standard",
        description = "Standard map rando experience with Hard logic skill, fast QoL, and single/double door locks.",
        profile = ConfigProfileEntity(
            name = "Standard",
            logicSkill = "Hard",
            startLocationMode = "Ship",
            techWallJump = true, techIBJ = true, techShinespark = true, techShortCharge = false,
            techMockball = true, techSpringBall = false, techMidAirMorph = false, techDamageBoosts = false,
            logicOptions = "Default",
            itemProgression = "Normal", itemPool = "Full", majorMinorDistribution = "Standard",
            mapRandomization = "Standard", bossRandomization = "Vanilla", doorLockRandomization = "Single/Double",
            elevatorRando = false, escapeRando = false,
            qolFastElevators = true, qolFastDoors = true, qolFastText = true, qolCutsceneSkip = true, qolQuickMorph = true, qolSpinJumpRestart = true,
            difficultyResource = "Standard", damageMultiplier = "1.0x"
        )
    )

    val MENTOR_TOURNAMENT = PresetDefinition(
        id = "mentor_tournament",
        name = "Mentor Tournament",
        description = "Tournament preset designed for learning competitive rando with Medium logic skill and vanilla map layout.",
        profile = ConfigProfileEntity(
            name = "Mentor Tournament",
            logicSkill = "Medium",
            startLocationMode = "Ship",
            techWallJump = true, techIBJ = true, techShinespark = true, techShortCharge = false,
            techMockball = true, techSpringBall = false, techMidAirMorph = false, techDamageBoosts = false,
            logicOptions = "Default",
            itemProgression = "Normal", itemPool = "Full", majorMinorDistribution = "Standard",
            mapRandomization = "Vanilla", bossRandomization = "Vanilla", doorLockRandomization = "Single/Double",
            elevatorRando = false, escapeRando = false,
            qolFastElevators = true, qolFastDoors = true, qolFastText = true, qolCutsceneSkip = true, qolQuickMorph = true, qolSpinJumpRestart = true
        )
    )

    val COMMUNITY_RACE_SEASON_5 = PresetDefinition(
        id = "community_race_season_5",
        name = "Community Race Season 5",
        description = "Official Community Race Season 5 preset featuring Hard logic skill and standard map area shuffling.",
        profile = ConfigProfileEntity(
            name = "Community Race Season 5",
            logicSkill = "Hard",
            startLocationMode = "Random",
            techWallJump = true, techIBJ = true, techShinespark = true, techShortCharge = false,
            techMockball = true, techSpringBall = false, techMidAirMorph = false, techDamageBoosts = false,
            logicOptions = "Default",
            itemProgression = "Normal", itemPool = "Full", majorMinorDistribution = "Standard",
            mapRandomization = "Standard", bossRandomization = "Vanilla", doorLockRandomization = "Single/Double",
            elevatorRando = false, escapeRando = false,
            qolFastElevators = true, qolFastDoors = true, qolFastText = true, qolCutsceneSkip = true, qolQuickMorph = true, qolSpinJumpRestart = true
        )
    )

    val EXPERT_CHALLENGE = PresetDefinition(
        id = "expert_challenge",
        name = "Summer Series Expert Challenge",
        description = "High-difficulty competitive preset with Expert logic, strict options, random start, and full area shuffling.",
        profile = ConfigProfileEntity(
            name = "Summer Series Expert Challenge",
            logicSkill = "Expert",
            logicOptions = "Strict",
            startLocationMode = "Random",
            techWallJump = true, techIBJ = true, techShinespark = true, techShortCharge = true,
            techMockball = true, techSpringBall = true, techMidAirMorph = true, techDamageBoosts = true,
            itemProgression = "Tricky", itemPool = "Full", majorMinorDistribution = "Shuffled",
            mapRandomization = "Full Area", bossRandomization = "Randomized", doorLockRandomization = "Randomized",
            elevatorRando = true, escapeRando = true,
            qolFastElevators = true, qolFastDoors = true, qolFastText = true, qolCutsceneSkip = true, qolQuickMorph = true, qolSpinJumpRestart = true,
            difficultyResource = "Harsh", damageMultiplier = "1.5x"
        )
    )

    val WILD = PresetDefinition(
        id = "wild",
        name = "Wild",
        description = "Maximum chaos preset with Wild map layout, randomized bosses, door locks, and random start location.",
        profile = ConfigProfileEntity(
            name = "Wild",
            logicSkill = "Hard",
            startLocationMode = "Random",
            techWallJump = true, techIBJ = true, techShinespark = true, techShortCharge = false,
            techMockball = true, techSpringBall = false, techMidAirMorph = false, techDamageBoosts = false,
            logicOptions = "Default",
            itemProgression = "Normal", itemPool = "Full", majorMinorDistribution = "Shuffled",
            mapRandomization = "Wild", bossRandomization = "Randomized", doorLockRandomization = "Randomized",
            elevatorRando = true, escapeRando = true,
            qolFastElevators = true, qolFastDoors = true, qolFastText = true, qolCutsceneSkip = true, qolQuickMorph = true, qolSpinJumpRestart = true
        )
    )

    val OFFICIAL_PRESETS: List<PresetDefinition> = listOf(
        PresetDefinition("default", "Default", "Default standard map rando configuration.", STANDARD.profile.copy(name = "Default")),
        COMMUNITY_RACE_SEASON_5,
        MENTOR_TOURNAMENT,
        EXPERT_CHALLENGE
    )

    private val dynamicPresetsList = mutableListOf<String>()

    fun updateOfficialPresets(scrapedNames: List<String>) {
        synchronized(dynamicPresetsList) {
            dynamicPresetsList.clear()
            dynamicPresetsList.addAll(scrapedNames)
        }
    }

    val DEFAULT_PRESET_NAMES: List<String>
        get() {
            synchronized(dynamicPresetsList) {
                if (dynamicPresetsList.isNotEmpty()) {
                    return dynamicPresetsList.toList()
                }
            }
            return OFFICIAL_PRESETS.map { it.name }.distinct()
        }

    fun getPresetByName(name: String): PresetDefinition? {
        val trimmed = name.trim()
        return OFFICIAL_PRESETS.find { it.name.equals(trimmed, ignoreCase = true) || it.id.equals(trimmed, ignoreCase = true) }
    }

    fun getProfileForPreset(presetName: String, savedProfiles: List<ConfigProfileEntity> = emptyList()): ConfigProfileEntity {
        val trimmed = presetName.trim()
        val savedMatch = savedProfiles.find { it.name.equals(trimmed, ignoreCase = true) }
        if (savedMatch != null) return savedMatch

        val officialMatch = getPresetByName(trimmed)
        if (officialMatch != null) return officialMatch.profile.copy(name = trimmed)

        return when (trimmed.lowercase()) {
            "casual", "basic" -> CASUAL_EXPLORER.profile.copy(name = trimmed)
            "medium" -> MENTOR_TOURNAMENT.profile.copy(name = trimmed)
            "hard" -> STANDARD.profile.copy(name = trimmed)
            "expert", "expert challenge" -> EXPERT_CHALLENGE.profile.copy(name = trimmed)
            "very hard", "master", "insane" -> ConfigProfileEntity(
                name = trimmed,
                logicSkill = if (trimmed.equals("very hard", true)) "Very Hard" else "Expert",
                startLocationMode = "Random",
                techWallJump = true, techIBJ = true, techShinespark = true, techShortCharge = true,
                techMockball = true, techSpringBall = true, techMidAirMorph = true, techDamageBoosts = true,
                itemProgression = "Tricky", itemPool = "Full", majorMinorDistribution = "Standard",
                mapRandomization = "Standard", bossRandomization = "Vanilla", doorLockRandomization = "Single/Double",
                qolFastElevators = true, qolFastDoors = true, qolFastText = true, qolCutsceneSkip = true, qolQuickMorph = true, qolSpinJumpRestart = true
            )
            else -> STANDARD.profile.copy(name = trimmed)
        }
    }
}
