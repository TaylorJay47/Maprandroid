package com.maprandroid.ui.presets

import androidx.compose.animation.*
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.maprandroid.data.local.ConfigProfileEntity
import com.maprandroid.data.model.TechCatalog
import com.maprandroid.data.model.TechCategory
import com.maprandroid.ui.config.ConfigViewModel
import com.maprandroid.ui.config.CustomDropdownField
import com.maprandroid.ui.config.CustomSliderField
import com.maprandroid.ui.config.CustomSwitchCard
import com.maprandroid.ui.config.ProfileManagementHeader
import com.maprandroid.ui.config.SaveProfileDialog

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PresetsScreen(
    viewModel: ConfigViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    if (uiState.showSaveDialog) {
        SaveProfileDialog(
            initialName = uiState.currentCustomProfile.name.ifEmpty { "Custom Preset" },
            isEditing = uiState.selectedProfileId != null,
            onDismiss = { viewModel.showSaveDialog(false) },
            onSave = { name -> viewModel.saveCurrentProfile(name) }
        )
    }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = { viewModel.showSaveDialog(true) },
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Save,
                        contentDescription = "Save Preset"
                    )
                    Text(
                        text = if (uiState.selectedProfileId != null) "Update Preset" else "Save Preset",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    ) { padding ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Spacer(modifier = Modifier.height(4.dp))

            ProfileManagementHeader(
                savedProfiles = uiState.savedProfiles,
                selectedProfileId = uiState.selectedProfileId,
                currentProfile = uiState.currentCustomProfile,
                onSelectProfile = { viewModel.loadProfile(it) },
                onResetClick = { viewModel.resetCustomProfile() },
                onDeleteClick = { viewModel.deleteProfile(it) }
            )

            val profile = uiState.currentCustomProfile

            // 1. Skill Assumptions
            ParameterCategoryCard(
                title = "1. Skill Assumptions",
                description = "Determines required gameplay execution difficulty and trick logic.",
                selectedValue = profile.logicSkill,
                options = listOf("Basic", "Medium", "Hard", "Very Hard", "Expert", "Expert+", "Extreme", "Extreme+", "Insane", "Insane+"),
                onOptionSelected = { viewModel.updateLogicSkill(it) }
            ) {
                TechSettingsManager(
                    profile = profile,
                    viewModel = viewModel
                )
            }

            // 2. Item Progression
            ParameterCategoryCard(
                title = "2. Item Progression",
                description = "Controls progression pacing, key item density, and hunt modes.",
                selectedValue = profile.itemProgression,
                options = listOf("Normal", "Tricky", "Technical", "Challenge", "Desolate"),
                onOptionSelected = { viewModel.updateItemProgression(it) }
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    CustomDropdownField(
                        label = "Item Pool Size",
                        selectedValue = profile.itemPool,
                        options = listOf("Major/Minor", "Full", "Chozo", "Scavenger"),
                        onOptionSelected = { viewModel.updateItemPool(it) }
                    )
                    CustomSwitchCard("Start Morph Ball", "Begin game with Morph Ball in inventory", profile.startMorph, { viewModel.updateStartMorph(it) })
                    CustomSwitchCard("Start Charge Beam", "Begin game with Charge Beam in inventory", profile.startCharge, { viewModel.updateStartCharge(it) })
                    CustomSwitchCard("Start Varia Suit", "Begin game with Varia Suit in inventory", profile.startVaria, { viewModel.updateStartVaria(it) })
                    CustomSwitchCard("Scavenger Hunt", "Require collecting key items in scavenger sequence", profile.scavengerHunt, { viewModel.updateScavengerHunt(it) })
                    CustomSwitchCard("Chozo Hunt", "Require visiting Chozo statues to unlock Tourian", profile.chozoHunt, { viewModel.updateChozoHunt(it) })
                    CustomSliderField("Super Missile Packs", profile.superMissileCount, 1f..30f, 29, "packs", { viewModel.updateSuperMissileCount(it) })
                    CustomSliderField("Power Bomb Packs", profile.powerBombCount, 1f..30f, 29, "packs", { viewModel.updatePowerBombCount(it) })
                }
            }

            // 3. Quality-of-Life (QoL)
            ParameterCategoryCard(
                title = "3. Quality-of-Life Options",
                description = "Speed optimizations, cutscene skip, fast transitions, and morph tools.",
                selectedValue = profile.qolPreset,
                options = listOf("Off", "Low", "Default", "High", "Max"),
                onOptionSelected = { viewModel.setQolPreset(it) }
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    CustomSwitchCard("Fast Elevators", "Accelerate elevator travel speed", profile.qolFastElevators, { viewModel.updateQolFastElevators(it) })
                    CustomSwitchCard("Fast Doors", "Accelerate door transition animations", profile.qolFastDoors, { viewModel.updateQolFastDoors(it) })
                    CustomSwitchCard("Fast Text", "Instant dialog text rendering", profile.qolFastText, { viewModel.updateQolFastText(it) })
                    CustomSwitchCard("Cutscene Skip", "Automatically skip long intro/boss cutscenes", profile.qolCutsceneSkip, { viewModel.updateQolCutsceneSkip(it) })
                    CustomSwitchCard("Quick Morph", "One-button morph toggle", profile.qolQuickMorph, { viewModel.updateQolQuickMorph(it) })
                    CustomSwitchCard("Spin Jump Restart", "Allow restarting spin jumps in mid-air", profile.qolSpinJumpRestart, { viewModel.updateQolSpinJumpRestart(it) })
                    CustomSwitchCard("Auto Charge Beam", "Charge beam automatically charges without holding button", profile.qolAutoChargeBeam, { viewModel.updateQolAutoChargeBeam(it) })
                }
            }

            // 4. Objectives
            ParameterCategoryCard(
                title = "4. Objectives",
                description = "Requirements necessary to unlock Tourian and reach Mother Brain.",
                selectedValue = profile.objectivePreset,
                options = listOf("None", "Bosses", "Minibosses", "Chozos", "Pirates", "Metroids", "Random"),
                onOptionSelected = { viewModel.updateObjectivePreset(it) }
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    CustomSwitchCard("Kraid Defeated", "Require defeating Kraid", profile.kraidObjective, { viewModel.updateKraidObjective(it) })
                    CustomSwitchCard("Phantoon Defeated", "Require defeating Phantoon", profile.phantoonObjective, { viewModel.updatePhantoonObjective(it) })
                    CustomSwitchCard("Draygon Defeated", "Require defeating Draygon", profile.draygonObjective, { viewModel.updateDraygonObjective(it) })
                    CustomSwitchCard("Ridley Defeated", "Require defeating Ridley", profile.ridleyObjective, { viewModel.updateRidleyObjective(it) })
                    CustomSliderField("Min Objectives", profile.minObjectives, 1f..10f, 9, "objectives", { viewModel.updateMinObjectives(it) })
                    CustomSliderField("Max Objectives", profile.maxObjectives, 1f..10f, 9, "objectives", { viewModel.updateMaxObjectives(it) })
                }
            }

            // 5. Map Layout
            ParameterCategoryCard(
                title = "5. Map Layout",
                description = "Determines layout size, area shuffling, bosses, and doors.",
                selectedValue = profile.mapRandomization,
                options = listOf("Vanilla", "Small", "Standard", "Wild"),
                onOptionSelected = { viewModel.updateMapRandomization(it) }
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    CustomDropdownField(
                        label = "Boss Randomization",
                        selectedValue = profile.bossRandomization,
                        options = listOf("Vanilla", "Randomized", "Chaos"),
                        onOptionSelected = { viewModel.updateBossRandomization(it) }
                    )
                    CustomDropdownField(
                        label = "Door Lock Randomization",
                        selectedValue = profile.doorLockRandomization,
                        options = listOf("Vanilla", "Color", "Single/Double", "Randomized"),
                        onOptionSelected = { viewModel.updateDoorLockRandomization(it) }
                    )
                    CustomSwitchCard("Elevator Shuffling", "Shuffle planetary elevator transitions", profile.elevatorRando, { viewModel.updateElevatorRando(it) })
                    CustomSwitchCard("Escape Sequence Rando", "Randomize Tourian escape route", profile.escapeRando, { viewModel.updateEscapeRando(it) })
                }
            }

            // 6. Doors
            ParameterCategoryCard(
                title = "6. Doors",
                description = "Controls door lock distribution and door types across the map.",
                selectedValue = profile.doorsPreset,
                options = listOf("Blue", "Ammo", "Beam"),
                onOptionSelected = { viewModel.updateDoorsPreset(it) }
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    CustomSliderField("Red Missile Doors", profile.redDoorsCount, 0f..50f, 50, "doors", { viewModel.updateRedDoorsCount(it) })
                    CustomSliderField("Green Super Doors", profile.greenDoorsCount, 0f..50f, 50, "doors", { viewModel.updateGreenDoorsCount(it) })
                    CustomSliderField("Yellow PowerBomb Doors", profile.yellowDoorsCount, 0f..50f, 50, "doors", { viewModel.updateYellowDoorsCount(it) })
                }
            }

            // 7. Start Location
            ParameterCategoryCard(
                title = "7. Start Location",
                description = "Spawn location at the start of the seed run.",
                selectedValue = if (profile.startLocationMode == "Landing Site") "Ship" else profile.startLocationMode,
                options = listOf("Ship", "Random", "Escape"),
                onOptionSelected = { viewModel.updateStartLocationMode(it) }
            ) {}

            // 8. Save the Animals
            ParameterCategoryCard(
                title = "8. Save the Animals",
                description = "Requirements regarding Etecoons and Dachora during escape.",
                selectedValue = profile.saveAnimals,
                options = listOf("No", "Yes", "Optional", "Random"),
                onOptionSelected = { viewModel.updateSaveAnimals(it) }
            ) {}

            // 9. Other Options & Variations
            ParameterCategoryCard(
                title = "9. Other Options & Variations",
                description = "Glitches, item mechanics, area assignment, and race rules.",
                selectedValue = if (profile.raceMode) "Race Mode" else "Standard",
                options = listOf("Standard", "Custom"),
                onOptionSelected = { }
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    CustomDropdownField(
                        label = "Wall Jump Mechanics",
                        selectedValue = profile.wallJumpSetting,
                        options = listOf("Vanilla", "Collectible"),
                        onOptionSelected = { viewModel.updateWallJumpSetting(it) }
                    )
                    CustomDropdownField(
                        label = "Speed Booster Mechanics",
                        selectedValue = profile.speedBoosterSetting,
                        options = listOf("Vanilla", "Split"),
                        onOptionSelected = { viewModel.updateSpeedBoosterSetting(it) }
                    )
                    CustomDropdownField(
                        label = "Area Assignment Base Order",
                        selectedValue = profile.areaAssignmentBaseOrder,
                        options = listOf("Depth", "Size", "Random"),
                        onOptionSelected = { viewModel.updateAreaAssignmentBaseOrder(it) }
                    )
                    CustomSwitchCard("Ship in Crateria", "Keep Landing Site ship in Crateria area", profile.shipInCrateria, { viewModel.updateShipInCrateria(it) })
                    CustomSwitchCard("Mother Brain in Tourian", "Keep Mother Brain in Tourian area", profile.motherBrainInTourian, { viewModel.updateMotherBrainInTourian(it) })
                    CustomSwitchCard("Race Mode", "Lock spoiler log behind token", profile.raceMode, { viewModel.updateRaceMode(it) })
                    CustomSwitchCard("Enable Major Glitches", "Allow GT code and out of bounds", profile.enableMajorGlitches, { viewModel.updateEnableMajorGlitches(it) })
                    CustomDropdownField(
                        label = "In-Game Savestates",
                        selectedValue = profile.savestate,
                        options = listOf("No", "Limited", "Unlimited"),
                        onOptionSelected = { viewModel.updateSavestate(it) }
                    )
                    CustomDropdownField(
                        label = "Aim Button Configuration",
                        selectedValue = profile.aimButtonSetting,
                        options = listOf("Standard", "Separate", "One Button"),
                        onOptionSelected = { viewModel.updateAimButtonSetting(it) }
                    )
                    CustomDropdownField(
                        label = "Map Icons",
                        selectedValue = profile.mapIcons,
                        options = listOf("Standard", "Detailed", "Hidden"),
                        onOptionSelected = { viewModel.updateMapIcons(it) }
                    )
                    CustomDropdownField(
                        label = "Item Markers",
                        selectedValue = profile.itemMarkers,
                        options = listOf("Simple", "Detailed", "None"),
                        onOptionSelected = { viewModel.updateItemMarkers(it) }
                    )
                    CustomSwitchCard("Music Shuffle", "Shuffle area soundtrack tracks", profile.musicShuffle, { viewModel.updateMusicShuffle(it) })
                    CustomSwitchCard("Palette Shuffle", "Shuffle area color palettes", profile.paletteShuffle, { viewModel.updatePaletteShuffle(it) })
                    CustomDropdownField(
                        label = "Damage Multiplier",
                        selectedValue = profile.damageMultiplier,
                        options = listOf("0.5x", "1.0x", "1.5x", "2.0x", "3.0x"),
                        onOptionSelected = { viewModel.updateDamageMultiplier(it) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(72.dp))
        }
    }
}

@Composable
private fun TechSettingsManager(
    profile: ConfigProfileEntity,
    viewModel: ConfigViewModel
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf<TechCategory?>(null) }
    val collapsedDifficulties = remember { mutableStateMapOf<String, Boolean>() }

    val defaultTechs = remember(profile.logicSkill) {
        TechCatalog.getTechsForPreset(profile.logicSkill)
    }

    val overrides = remember(profile.techOverridesJson) {
        parseOverridesMap(profile.techOverridesJson)
    }

    val filteredTechs = remember(searchQuery, selectedCategory, profile.logicSkill, profile.techOverridesJson) {
        TechCatalog.ALL_TECHS.filter { tech ->
            val matchesCategory = (selectedCategory == null || tech.category == selectedCategory)
            val matchesQuery = searchQuery.isBlank() ||
                    tech.displayName.contains(searchQuery, ignoreCase = true) ||
                    tech.name.contains(searchQuery, ignoreCase = true)
            matchesCategory && matchesQuery
        }
    }

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        // Search Bar
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            label = { Text("Search Techniques") },
            placeholder = { Text("e.g. WallJump, Mockball, GateGlitch...") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            trailingIcon = if (searchQuery.isNotEmpty()) {
                { IconButton(onClick = { searchQuery = "" }) { Icon(Icons.Default.Clear, contentDescription = "Clear") } }
            } else null,
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        // Category Filter Chips
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            FilterChip(
                selected = selectedCategory == null,
                onClick = { selectedCategory = null },
                label = { Text("All (${TechCatalog.ALL_TECHS.size})") }
            )
            TechCategory.entries.forEach { category ->
                val count = TechCatalog.ALL_TECHS.count { it.category == category }
                FilterChip(
                    selected = selectedCategory == category,
                    onClick = { selectedCategory = if (selectedCategory == category) null else category },
                    label = { Text("${category.displayName} ($count)") }
                )
            }
        }

        // Action Row: Bulk Toggles
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Showing ${filteredTechs.size} Techniques",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Row {
                if (overrides.isNotEmpty()) {
                    TextButton(onClick = { viewModel.resetTechOverrides() }) {
                        Text("Reset Overrides")
                    }
                }
                if (selectedCategory != null) {
                    Spacer(modifier = Modifier.width(4.dp))
                    OutlinedButton(
                        onClick = { viewModel.bulkUpdateTechCategory(selectedCategory!!, true) },
                        contentPadding = PaddingValues(horizontal = 8.dp)
                    ) {
                        Text("Enable All", style = MaterialTheme.typography.labelSmall)
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                    OutlinedButton(
                        onClick = { viewModel.bulkUpdateTechCategory(selectedCategory!!, false) },
                        contentPadding = PaddingValues(horizontal = 8.dp)
                    ) {
                        Text("Disable All", style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
        }

        // Collapsible Difficulty Sections
        TechCatalog.PRESET_ORDER.forEach { difficulty ->
            val difficultyTechs = filteredTechs.filter {
                it.minPreset.equals(difficulty, ignoreCase = true) ||
                        (it.minPreset.removeSuffix("+").equals(difficulty.removeSuffix("+"), ignoreCase = true) && it.minPreset.endsWith("+") == difficulty.endsWith("+"))
            }

            if (difficultyTechs.isNotEmpty()) {
                val isCollapsed = collapsedDifficulties[difficulty] ?: true

                Surface(
                    onClick = { collapsedDifficulties[difficulty] = !isCollapsed },
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp, bottom = 2.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = if (isCollapsed) Icons.Default.KeyboardArrowRight else Icons.Default.KeyboardArrowDown,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "$difficulty Difficulty",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                        Surface(
                            color = MaterialTheme.colorScheme.primary,
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(
                                text = "${difficultyTechs.size} techs",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                            )
                        }
                    }
                }

                if (!isCollapsed) {
                    difficultyTechs.forEach { tech ->
                        val isEnabled = overrides[tech.name] ?: defaultTechs[tech.name] ?: true
                        val isOverridden = overrides.containsKey(tech.name)

                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { viewModel.updateTechOverride(tech.name, !isEnabled) },
                            colors = CardDefaults.cardColors(
                                containerColor = if (isOverridden) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f) else MaterialTheme.colorScheme.surface
                            )
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = tech.displayName,
                                            style = MaterialTheme.typography.bodyLarge,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                        if (isOverridden) {
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Badge(containerColor = MaterialTheme.colorScheme.tertiaryContainer) {
                                                Text("Custom", style = MaterialTheme.typography.labelSmall)
                                            }
                                        }
                                    }
                                    Text(
                                        text = "${tech.name} • Level: ${tech.minPreset}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Switch(
                                    checked = isEnabled,
                                    onCheckedChange = { viewModel.updateTechOverride(tech.name, it) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun parseOverridesMap(jsonStr: String): Map<String, Boolean> {
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ParameterCategoryCard(
    title: String,
    description: String,
    selectedValue: String,
    options: List<String>,
    onOptionSelected: (String) -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    var dropdownExpanded by remember { mutableStateOf(false) }

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Main Category Dropdown Selector
                ExposedDropdownMenuBox(
                    expanded = dropdownExpanded,
                    onExpandedChange = { dropdownExpanded = !dropdownExpanded },
                    modifier = Modifier.weight(1f)
                ) {
                    OutlinedTextField(
                        value = selectedValue,
                        onValueChange = {},
                        readOnly = true,
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = dropdownExpanded) },
                        colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
                        modifier = Modifier
                            .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                            .fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = dropdownExpanded,
                        onDismissRequest = { dropdownExpanded = false }
                    ) {
                        options.forEach { option ->
                            DropdownMenuItem(
                                text = { Text(option) },
                                onClick = {
                                    onOptionSelected(option)
                                    dropdownExpanded = false
                                }
                            )
                        }
                    }
                }

                // ⚙ Cog Button for Fine-Grained Customization
                FilledIconToggleButton(
                    checked = expanded,
                    onCheckedChange = { expanded = it },
                    colors = IconButtonDefaults.filledIconToggleButtonColors(
                        containerColor = if (expanded) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerHighest,
                        contentColor = if (expanded) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = "Customize $title"
                    )
                }
            }

            // Expandable Fine-Grained Options Panel
            AnimatedVisibility(
                visible = expanded,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Column(modifier = Modifier.padding(top = 12.dp)) {
                    HorizontalDivider(modifier = Modifier.padding(bottom = 12.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(bottom = 8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Tune,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Fine-Tuned Options",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    content()
                }
            }
        }
    }
}
