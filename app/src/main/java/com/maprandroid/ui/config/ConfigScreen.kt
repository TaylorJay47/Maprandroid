package com.maprandroid.ui.config

import android.content.res.Configuration
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.maprandroid.data.local.ConfigProfileEntity
import com.maprandroid.util.EmulatorLauncher

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConfigScreen(
    viewModel: ConfigViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

    val openBaseRomLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            viewModel.selectBaseRomFile(context, uri)
        }
    }

    if (uiState.showSaveDialog) {
        SaveProfileDialog(
            initialName = uiState.currentCustomProfile.name.ifEmpty { "Custom Preset" },
            isEditing = uiState.selectedProfileId != null,
            onDismiss = { viewModel.showSaveDialog(false) },
            onSave = { name -> viewModel.saveCurrentProfile(name) }
        )
    }

    if (uiState.showGenerationModal) {
        GenerationProgressModal(
            uiState = uiState,
            onDismiss = { viewModel.dismissGenerationModal() },
            onLaunchRom = { romPath, pkgName ->
                viewModel.dismissGenerationModal()
                if (!romPath.isNullOrEmpty()) {
                    val success = EmulatorLauncher.launchEmulator(context, romPath, pkgName)
                    if (!success) {
                        Toast.makeText(context, "Could not launch emulator.", Toast.LENGTH_LONG).show()
                    }
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Seed Generator") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                )
            )
        }
    ) { padding ->
        Box(
            modifier = modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            if (isLandscape) {
                LandscapeConfigLayout(
                    uiState = uiState,
                    viewModel = viewModel,
                    onSelectBaseRom = { openBaseRomLauncher.launch(arrayOf("*/*")) }
                )
            } else {
                PortraitConfigLayout(
                    uiState = uiState,
                    viewModel = viewModel,
                    onSelectBaseRom = { openBaseRomLauncher.launch(arrayOf("*/*")) }
                )
            }
        }
    }
}

@Composable
private fun LandscapeConfigLayout(
    uiState: ConfigUiState,
    viewModel: ConfigViewModel,
    onSelectBaseRom: () -> Unit
) {
    val context = LocalContext.current
    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val totalWidth = maxWidth
        val sideRailWidth = totalWidth * 0.22f
        val mainContentWidth = totalWidth * 0.78f

        Column(modifier = Modifier.fillMaxSize().padding(12.dp)) {
            // Header Bar in Landscape: Base ROM + Mode + Profile Bar + Generate
            Card(
                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Base ROM Quick Status
                    Card(
                        modifier = Modifier.weight(1.2f).clickable { onSelectBaseRom() },
                        colors = CardDefaults.cardColors(
                            containerColor = if (uiState.isBaseRomValid) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f) else MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f)
                        )
                    ) {
                        Row(modifier = Modifier.padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = if (uiState.isBaseRomValid) Icons.Default.CheckCircle else Icons.Default.Warning,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp),
                                tint = if (uiState.isBaseRomValid) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Column {
                                Text(
                                    text = uiState.baseRomName ?: "Select Base ROM",
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1
                                )
                                Text(
                                    text = uiState.baseRomStatus,
                                    style = MaterialTheme.typography.bodySmall,
                                    maxLines = 1
                                )
                            }
                        }
                    }

                    // Segmented Button: Presets vs Custom
                    SingleChoiceSegmentedButtonRow(
                        modifier = Modifier.weight(1f)
                    ) {
                        SegmentedButton(
                            selected = !uiState.isCustomMode,
                            onClick = { viewModel.setCustomMode(false) },
                            shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2)
                        ) {
                            Icon(Icons.AutoMirrored.Filled.List, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Presets")
                        }
                        SegmentedButton(
                            selected = uiState.isCustomMode,
                            onClick = { viewModel.setCustomMode(true) },
                            shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2)
                        ) {
                            Icon(Icons.Default.Settings, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Custom")
                        }
                    }

                    // Profile Actions or Active Preset
                    if (uiState.isCustomMode) {
                        Row(modifier = Modifier.weight(1.5f), verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = uiState.currentCustomProfile.name,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.weight(1f),
                                maxLines = 1
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            FilledTonalButton(
                                onClick = { viewModel.showSaveDialog(true) },
                                contentPadding = PaddingValues(horizontal = 8.dp)
                            ) {
                                Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Save")
                            }
                            if (uiState.selectedProfileId != null) {
                                IconButton(onClick = { viewModel.deleteProfile(uiState.selectedProfileId) }) {
                                    Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
                                }
                            }
                        }
                    } else {
                        Text(
                            text = "Preset: ${uiState.selectedPreset ?: "Default"}",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.weight(1.5f)
                        )
                    }

                    // Generate ROM Button
                    Button(
                        onClick = { viewModel.generateRom(context) },
                        enabled = (uiState.isCustomMode || uiState.selectedPreset != null) && !uiState.isLoading,
                        contentPadding = PaddingValues(horizontal = 16.dp)
                    ) {
                        if (uiState.isLoading) {
                            CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                        } else {
                            Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Generate")
                        }
                    }
                }
            }

            if (uiState.isError && uiState.errorMessage != null) {
                ErrorMessageCard(errorMessage = uiState.errorMessage) {
                    viewModel.clearError()
                    viewModel.fetchPresets()
                }
            }

            // Body Area: Left Navigation Rail (~22%) + Right Parameters Grid (~78%)
            if (!uiState.isCustomMode) {
                // Presets Mode Layout
                PresetsList(
                    presets = uiState.presets,
                    selectedPreset = uiState.selectedPreset,
                    isLoading = uiState.isLoading,
                    onSelectPreset = { viewModel.selectPreset(it) },
                    onRetry = { viewModel.fetchPresets() }
                )
            } else {
                // Custom Mode Layout: 22% Left Sidebar / 78% Right Options Grid
                Row(modifier = Modifier.fillMaxSize()) {
                    // Left Side Category Rail
                    Surface(
                        modifier = Modifier
                            .width(sideRailWidth)
                            .fillMaxHeight()
                            .padding(end = 8.dp),
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceContainer
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(8.dp)
                                .verticalScroll(rememberScrollState()),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = "Saved Presets",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                FilterChip(
                                    selected = uiState.selectedProfileId == null,
                                    onClick = { viewModel.resetCustomProfile() },
                                    label = { Text("New Preset") }
                                )
                                uiState.savedProfiles.forEach { profile ->
                                    FilterChip(
                                        selected = uiState.selectedProfileId == profile.id,
                                        onClick = { viewModel.loadProfile(profile) },
                                        label = { Text(profile.name) }
                                    )
                                }
                            }

                            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                            Text(
                                text = "Categories",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )

                            val categories = getCategoriesList()
                            categories.forEachIndexed { index, category ->
                                val isSelected = uiState.activeTab == index
                                NavigationRailItem(
                                    selected = isSelected,
                                    onClick = { viewModel.setActiveTab(index) },
                                    icon = { Icon(category.icon, contentDescription = null) },
                                    label = { Text(category.title, style = MaterialTheme.typography.labelSmall) },
                                    alwaysShowLabel = true,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }
                    }

                    // Right Side Parameter Options Grid (~78% width)
                    Box(
                        modifier = Modifier
                            .width(mainContentWidth)
                            .fillMaxHeight()
                    ) {
                        CategoryContentGrid(
                            activeTab = uiState.activeTab,
                            profile = uiState.currentCustomProfile,
                            viewModel = viewModel
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PortraitConfigLayout(
    uiState: ConfigUiState,
    viewModel: ConfigViewModel,
    onSelectBaseRom: () -> Unit
) {
    val context = LocalContext.current
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Base ROM Setup Card
        BaseRomCard(
            baseRomName = uiState.baseRomName,
            baseRomStatus = uiState.baseRomStatus,
            isValid = uiState.isBaseRomValid,
            onSelectBaseRom = onSelectBaseRom
        )

        if (uiState.isError && uiState.errorMessage != null) {
            ErrorMessageCard(errorMessage = uiState.errorMessage) {
                viewModel.clearError()
                viewModel.fetchPresets()
            }
        }

        // Mode Selector: Presets vs Custom
        SingleChoiceSegmentedButtonRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp)
        ) {
            SegmentedButton(
                selected = !uiState.isCustomMode,
                onClick = { viewModel.setCustomMode(false) },
                shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2)
            ) {
                Icon(Icons.AutoMirrored.Filled.List, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Presets")
            }
            SegmentedButton(
                selected = uiState.isCustomMode,
                onClick = { viewModel.setCustomMode(true) },
                shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2)
            ) {
                Icon(Icons.Default.Settings, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Custom Preset")
            }
        }

        if (!uiState.isCustomMode) {
            Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                PresetsList(
                    presets = uiState.presets,
                    selectedPreset = uiState.selectedPreset,
                    isLoading = uiState.isLoading,
                    onSelectPreset = { viewModel.selectPreset(it) },
                    onRetry = { viewModel.fetchPresets() }
                )
            }
        } else {
            // Custom Profile Portrait View
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                ProfileManagementHeader(
                    savedProfiles = uiState.savedProfiles,
                    selectedProfileId = uiState.selectedProfileId,
                    currentProfile = uiState.currentCustomProfile,
                    onSelectProfile = { viewModel.loadProfile(it) },
                    onSaveClick = { viewModel.showSaveDialog(true) },
                    onResetClick = { viewModel.resetCustomProfile() },
                    onDeleteClick = { id -> viewModel.deleteProfile(id) }
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Category Tabs
                val categories = getCategoriesList()
                ScrollableTabRow(
                    selectedTabIndex = uiState.activeTab,
                    edgePadding = 0.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    categories.forEachIndexed { index, category ->
                        Tab(
                            selected = uiState.activeTab == index,
                            onClick = { viewModel.setActiveTab(index) },
                            text = { Text(category.title) },
                            icon = { Icon(category.icon, contentDescription = null) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                    CategoryContentGrid(
                        activeTab = uiState.activeTab,
                        profile = uiState.currentCustomProfile,
                        viewModel = viewModel
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Generate Button
        Button(
            onClick = { viewModel.generateRom(context) },
            enabled = (uiState.isCustomMode || uiState.selectedPreset != null) && !uiState.isLoading,
            modifier = Modifier.fillMaxWidth()
        ) {
            if (uiState.isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    color = MaterialTheme.colorScheme.onPrimary,
                    strokeWidth = 2.dp
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Generating Seed...")
            } else {
                val btnLabel = if (!uiState.isBaseRomValid) {
                    "Select Base ROM First"
                } else if (uiState.isCustomMode) {
                    "Generate ROM (${uiState.currentCustomProfile.name})"
                } else {
                    "Generate ROM (${uiState.selectedPreset ?: "Default"})"
                }
                Icon(Icons.Default.PlayArrow, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(btnLabel)
            }
        }
    }
}

@Composable
internal fun CategoryContentGrid(
    activeTab: Int,
    profile: ConfigProfileEntity,
    viewModel: ConfigViewModel
) {
    var techSearchQuery by remember { mutableStateOf("") }
    var selectedTechCategory by remember { mutableStateOf<com.maprandroid.data.model.TechCategory?>(null) }
    val collapsedDifficulties = remember { mutableStateMapOf<String, Boolean>() }

    LazyVerticalGrid(
        columns = GridCells.Adaptive(320.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        when (activeTab) {
            0 -> {
                // Skill & Techs
                item {
                    CustomDropdownField(
                        label = "Logic Skill Level",
                        selectedValue = profile.logicSkill,
                        options = listOf("Basic", "Medium", "Hard", "Very Hard", "Expert", "Expert+", "Extreme", "Extreme+", "Insane", "Insane+"),
                        onOptionSelected = { viewModel.updateLogicSkill(it) },
                        description = "Determines required gameplay techniques and logic stringency."
                    )
                }
                item {
                    CustomDropdownField(
                        label = "Logic Options",
                        selectedValue = profile.logicOptions,
                        options = listOf("Default", "Strict", "Minimal"),
                        onOptionSelected = { viewModel.updateLogicOptions(it) },
                        description = "Stringency level for execution requirements."
                    )
                }
                item {
                    CustomSliderField(
                        title = "Shinespark Tiles Required",
                        value = profile.shinesparkTiles,
                        valueRange = 15f..35f,
                        steps = 20,
                        unitLabel = "Tiles",
                        onValueChange = { viewModel.updateShinesparkTiles(it) },
                        description = "Minimum horizontal distance assumed available to charge a shinespark."
                    )
                }
                item {
                    CustomSliderField(
                        title = "Heated Shinespark Tiles Required",
                        value = profile.heatedShinesparkTiles,
                        valueRange = 15f..35f,
                        steps = 20,
                        unitLabel = "Tiles",
                        onValueChange = { viewModel.updateHeatedShinesparkTiles(it) },
                        description = "Minimum runway length assumed in heated rooms without Varia."
                    )
                }
                item {
                    CustomSliderField(
                        title = "Farm Time Limit",
                        value = profile.farmTimeLimit,
                        valueRange = 0f..180f,
                        steps = 36,
                        unitLabel = "Sec",
                        onValueChange = { viewModel.updateFarmTimeLimit(it) },
                        description = "Maximum time the logic assumes a player will farm drops."
                    )
                }

                item(span = { androidx.compose.foundation.lazy.grid.GridItemSpan(maxLineSpan) }) {
                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                }

                // Tech Overrides Header with Category Filter Buttons along the top
                item(span = { androidx.compose.foundation.lazy.grid.GridItemSpan(maxLineSpan) }) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                text = "Fine-Tuned Tech Options",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Select a category along the top and expand difficulty sections to fine-tune individual techniques.",
                                style = MaterialTheme.typography.bodySmall
                            )
                            Spacer(modifier = Modifier.height(8.dp))

                            // Category filter chips/buttons along the top
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                FilterChip(
                                    selected = selectedTechCategory == null,
                                    onClick = { selectedTechCategory = null },
                                    label = { Text("All Categories") }
                                )
                                com.maprandroid.data.model.TechCategory.entries.forEach { cat ->
                                    FilterChip(
                                        selected = selectedTechCategory == cat,
                                        onClick = {
                                            selectedTechCategory = if (selectedTechCategory == cat) null else cat
                                        },
                                        label = { Text(cat.displayName) }
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            OutlinedTextField(
                                value = techSearchQuery,
                                onValueChange = { techSearchQuery = it },
                                placeholder = { Text("Search techs...") },
                                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }

                // Group matching techs by difficulty
                val matchingTechs = com.maprandroid.data.model.TechCatalog.ALL_TECHS.filter { tech ->
                    val matchesCategory = selectedTechCategory == null || tech.category == selectedTechCategory
                    val matchesQuery = techSearchQuery.isBlank() ||
                            tech.displayName.contains(techSearchQuery, ignoreCase = true) ||
                            tech.name.contains(techSearchQuery, ignoreCase = true)
                    matchesCategory && matchesQuery
                }

                val overrides = try {
                    val jsonObject = org.json.JSONObject(profile.techOverridesJson)
                    val map = mutableMapOf<String, Boolean>()
                    val keys = jsonObject.keys()
                    while (keys.hasNext()) {
                        val k = keys.next()
                        map[k] = jsonObject.getBoolean(k)
                    }
                    map
                } catch (_: Exception) {
                    emptyMap()
                }
                val defaultTechs = com.maprandroid.data.model.TechCatalog.getTechsForPreset(profile.logicSkill)

                com.maprandroid.data.model.TechCatalog.PRESET_ORDER.forEach { difficulty ->
                    val difficultyTechs = matchingTechs.filter {
                        it.minPreset.equals(difficulty, ignoreCase = true) ||
                                (it.minPreset.removeSuffix("+").equals(difficulty.removeSuffix("+"), ignoreCase = true) && it.minPreset.endsWith("+") == difficulty.endsWith("+"))
                    }

                    if (difficultyTechs.isNotEmpty()) {
                        val isCollapsed = collapsedDifficulties[difficulty] ?: true

                        item(span = { androidx.compose.foundation.lazy.grid.GridItemSpan(maxLineSpan) }) {
                            Surface(
                                onClick = { collapsedDifficulties[difficulty] = !isCollapsed },
                                color = MaterialTheme.colorScheme.primaryContainer,
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 8.dp, bottom = 4.dp)
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
                        }

                        if (!isCollapsed) {
                            items(difficultyTechs.size) { index ->
                                val tech = difficultyTechs[index]
                                val isChecked = overrides[tech.name] ?: defaultTechs[tech.name] ?: true

                                CustomSwitchCard(
                                    title = tech.displayName,
                                    description = "${tech.category.displayName} • Level: ${tech.minPreset}",
                                    checked = isChecked,
                                    onCheckedChange = { viewModel.updateTechOverride(tech.name, it) }
                                )
                            }
                        }
                    }
                }
            }
            1 -> {
                // Progression & Pool
                item {
                    CustomDropdownField(
                        label = "Item Progression Preset",
                        selectedValue = profile.itemProgression,
                        options = listOf("Normal", "Tricky", "Technical", "Challenge", "Desolate"),
                        onOptionSelected = { viewModel.updateItemProgression(it) },
                        description = "Key item placement difficulty and pacing."
                    )
                }
                item {
                    CustomDropdownField(
                        label = "Progression Rate",
                        selectedValue = profile.progressionRate,
                        options = listOf("Uniform", "Fast", "Slow"),
                        onOptionSelected = { viewModel.updateProgressionRate(it) },
                        description = "Rate at which progressive items are distributed."
                    )
                }
                item {
                    CustomDropdownField(
                        label = "Item Placement Style",
                        selectedValue = profile.itemPlacementStyle,
                        options = listOf("Local", "Unrestricted"),
                        onOptionSelected = { viewModel.updateItemPlacementStyle(it) },
                        description = "Locality constraints for progression items."
                    )
                }
                item {
                    CustomDropdownField(
                        label = "Item Priority Strength",
                        selectedValue = profile.itemPriorityStrength,
                        options = listOf("Moderate", "High", "Low"),
                        onOptionSelected = { viewModel.updateItemPriorityStrength(it) },
                        description = "Weighting given to key progression items."
                    )
                }
                item {
                    CustomDropdownField(
                        label = "Item Pool Mode",
                        selectedValue = profile.itemPool,
                        options = listOf("Major/Minor", "Full", "Chozo", "Scavenger"),
                        onOptionSelected = { viewModel.updateItemPool(it) },
                        description = "Structure of item locations and pool distribution."
                    )
                }
                item {
                    CustomDropdownField(
                        label = "Major/Minor Distribution",
                        selectedValue = profile.majorMinorDistribution,
                        options = listOf("Standard", "Choice", "Shuffled"),
                        onOptionSelected = { viewModel.updateMajorMinorDistribution(it) },
                        description = "Separation or mixing of major and minor items."
                    )
                }
                item {
                    CustomSwitchCard(
                        title = "Random Tank Placements",
                        description = "Allows random placements of Energy/Reserve tanks",
                        checked = profile.randomTank,
                        onCheckedChange = { viewModel.updateScavengerHunt(it) }
                    )
                }
                item {
                    CustomSwitchCard(
                        title = "Spazer Before Plasma",
                        description = "Guarantees Spazer is placed before Plasma Beam",
                        checked = profile.spazerBeforePlasma,
                        onCheckedChange = { viewModel.updateChozoHunt(it) }
                    )
                }
                item {
                    CustomSwitchCard(
                        title = "Scavenger Hunt Mode",
                        description = "Requires collecting a set list of items before Tourian",
                        checked = profile.scavengerHunt,
                        onCheckedChange = { viewModel.updateScavengerHunt(it) }
                    )
                }
                if (profile.scavengerHunt) {
                    item {
                        CustomSliderField(
                            title = "Scavenger Required Items",
                            value = profile.scavengerRequiredCount,
                            valueRange = 5f..25f,
                            steps = 19,
                            unitLabel = "Items",
                            onValueChange = { viewModel.updateScavengerRequiredCount(it) },
                            description = "Number of scavenger items required to open Tourian."
                        )
                    }
                }
                item {
                    CustomSwitchCard(
                        title = "Chozo Hunt Mode",
                        description = "Places key items exclusively on Chozo statues",
                        checked = profile.chozoHunt,
                        onCheckedChange = { viewModel.updateChozoHunt(it) }
                    )
                }
                item {
                    CustomSliderField(
                        title = "Super Missile Count",
                        value = profile.superMissileCount,
                        valueRange = 5f..30f,
                        steps = 24,
                        unitLabel = "Supers",
                        onValueChange = { viewModel.updateSuperMissileCount(it) },
                        description = "Total number of Super Missiles in the item pool."
                    )
                }
                item {
                    CustomSliderField(
                        title = "Power Bomb Count",
                        value = profile.powerBombCount,
                        valueRange = 5f..25f,
                        steps = 19,
                        unitLabel = "PBs",
                        onValueChange = { viewModel.updatePowerBombCount(it) },
                        description = "Total number of Power Bombs in the item pool."
                    )
                }
                item {
                    CustomSwitchCard(
                        title = "Start with Morph Ball",
                        description = "Equips Morph Ball at game start",
                        checked = profile.startMorph,
                        onCheckedChange = { viewModel.updateStartMorph(it) }
                    )
                }
                item {
                    CustomSwitchCard(
                        title = "Start with Charge Beam",
                        description = "Equips Charge Beam at game start",
                        checked = profile.startCharge,
                        onCheckedChange = { viewModel.updateStartCharge(it) }
                    )
                }
                item {
                    CustomSwitchCard(
                        title = "Start with Varia Suit",
                        description = "Equips Varia Suit at game start",
                        checked = profile.startVaria,
                        onCheckedChange = { viewModel.updateStartVaria(it) }
                    )
                }
            }
            2 -> {
                // Map, Doors & Objectives
                item {
                    CustomDropdownField(
                        label = "Map Randomization",
                        selectedValue = profile.mapRandomization,
                        options = listOf("Vanilla", "Small", "Standard", "Wild"),
                        onOptionSelected = { viewModel.updateMapRandomization(it) },
                        description = "Controls room connection and area transition shuffling."
                    )
                }
                item {
                    CustomDropdownField(
                        label = "Boss Randomization",
                        selectedValue = profile.bossRandomization,
                        options = listOf("Vanilla", "Randomized", "Chaos"),
                        onOptionSelected = { viewModel.updateBossRandomization(it) },
                        description = "Shuffles boss fight locations and requirements."
                    )
                }
                item {
                    CustomDropdownField(
                        label = "Door Lock Randomization",
                        selectedValue = profile.doorLockRandomization,
                        options = listOf("Vanilla", "Color", "Single/Double", "Randomized"),
                        onOptionSelected = { viewModel.updateDoorLockRandomization(it) },
                        description = "Shuffles door colors and beam lock types."
                    )
                }
                item {
                    CustomSwitchCard(
                        title = "Elevator Randomization",
                        description = "Shuffles elevator connections between areas",
                        checked = profile.elevatorRando,
                        onCheckedChange = { viewModel.updateElevatorRando(it) }
                    )
                }
                item {
                    CustomSwitchCard(
                        title = "Escape Randomization",
                        description = "Randomizes escape route timers and door states",
                        checked = profile.escapeRando,
                        onCheckedChange = { viewModel.updateEscapeRando(it) }
                    )
                }
                item {
                    CustomDropdownField(
                        label = "Area Assignment Order",
                        selectedValue = profile.areaAssignmentBaseOrder,
                        options = listOf("Standard", "Size", "Depth", "Random"),
                        onOptionSelected = { viewModel.updateAreaAssignmentBaseOrder(it) },
                        description = "Ordering logic for placing area transition connections."
                    )
                }
                item {
                    CustomSwitchCard(
                        title = "Ship in Crateria",
                        description = "Forces Samus's Landing Site to remain in Crateria",
                        checked = profile.shipInCrateria,
                        onCheckedChange = { viewModel.updateShipInCrateria(it) }
                    )
                }
                item {
                    CustomSwitchCard(
                        title = "Mother Brain in Tourian",
                        description = "Forces Mother Brain fight to remain in Tourian",
                        checked = profile.motherBrainInTourian,
                        onCheckedChange = { viewModel.updateMotherBrainInTourian(it) }
                    )
                }
                item {
                    CustomDropdownField(
                        label = "Objective Preset",
                        selectedValue = profile.objectivePreset,
                        options = listOf("None", "Bosses", "Minibosses", "Chozos", "Pirates", "Metroids", "Random"),
                        onOptionSelected = { viewModel.updateObjectivePreset(it) },
                        description = "Pre-selected objective collection to open Tourian."
                    )
                }
                item {
                    CustomSliderField(
                        title = "Min Required Objectives",
                        value = profile.minObjectives,
                        valueRange = 1f..12f,
                        steps = 10,
                        unitLabel = "Objectives",
                        onValueChange = { viewModel.updateMinObjectives(it) },
                        description = "Minimum number of objectives needed to unlock Tourian."
                    )
                }
                item {
                    CustomSliderField(
                        title = "Max Required Objectives",
                        value = profile.maxObjectives,
                        valueRange = 1f..12f,
                        steps = 10,
                        unitLabel = "Objectives",
                        onValueChange = { viewModel.updateMaxObjectives(it) },
                        description = "Maximum number of objectives selected in objective pool."
                    )
                }

                // Objectives Checkboxes
                item {
                    CustomSwitchCard(title = "Kraid Objective", description = "Defeat Kraid", checked = profile.kraidObjective, onCheckedChange = { viewModel.updateKraidObjective(it) })
                }
                item {
                    CustomSwitchCard(title = "Phantoon Objective", description = "Defeat Phantoon", checked = profile.phantoonObjective, onCheckedChange = { viewModel.updatePhantoonObjective(it) })
                }
                item {
                    CustomSwitchCard(title = "Draygon Objective", description = "Defeat Draygon", checked = profile.draygonObjective, onCheckedChange = { viewModel.updateDraygonObjective(it) })
                }
                item {
                    CustomSwitchCard(title = "Ridley Objective", description = "Defeat Ridley", checked = profile.ridleyObjective, onCheckedChange = { viewModel.updateRidleyObjective(it) })
                }
                item {
                    CustomSwitchCard(title = "Spore Spawn Objective", description = "Defeat Spore Spawn", checked = profile.sporeSpawnObjective, onCheckedChange = { viewModel.updateSporeSpawnObjective(it) })
                }
                item {
                    CustomSwitchCard(title = "Crocomire Objective", description = "Defeat Crocomire", checked = profile.crocomireObjective, onCheckedChange = { viewModel.updateCrocomireObjective(it) })
                }
                item {
                    CustomSwitchCard(title = "Botwoon Objective", description = "Defeat Botwoon", checked = profile.botwoonObjective, onCheckedChange = { viewModel.updateBotwoonObjective(it) })
                }
                item {
                    CustomSwitchCard(title = "Golden Torizo Objective", description = "Defeat Golden Torizo", checked = profile.goldenTorizoObjective, onCheckedChange = { viewModel.updateGoldenTorizoObjective(it) })
                }
                item {
                    CustomSwitchCard(title = "Bomb Torizo Objective", description = "Defeat Bomb Torizo", checked = profile.bombTorizoObjective, onCheckedChange = { viewModel.updateBombTorizoObjective(it) })
                }
                item {
                    CustomSwitchCard(title = "Chozo Statues Objective", description = "Activate Chozo statues", checked = profile.chozoStatuesObjective, onCheckedChange = { viewModel.updateChozoStatuesObjective(it) })
                }
                item {
                    CustomSwitchCard(title = "Pirates Objective", description = "Clear Space Pirate rooms", checked = profile.piratesObjective, onCheckedChange = { viewModel.updatePiratesObjective(it) })
                }
                item {
                    CustomSwitchCard(title = "Metroids Objective", description = "Clear all Metroid rooms", checked = profile.metroidsObjective, onCheckedChange = { viewModel.updateMetroidsObjective(it) })
                }

                // Doors Settings
                item {
                    CustomDropdownField(
                        label = "Doors Preset",
                        selectedValue = profile.doorsPreset,
                        options = listOf("Blue", "Ammo", "Beam"),
                        onOptionSelected = { viewModel.updateDoorsPreset(it) },
                        description = "Preset door hatch lock distribution."
                    )
                }
                item {
                    CustomSliderField(
                        title = "Red Doors Count (Missile)",
                        value = profile.redDoorsCount,
                        valueRange = 0f..50f,
                        steps = 50,
                        unitLabel = "Doors",
                        onValueChange = { viewModel.updateRedDoorsCount(it) },
                        description = "Number of Red Missile doors placed."
                    )
                }
                item {
                    CustomSliderField(
                        title = "Green Doors Count (Super)",
                        value = profile.greenDoorsCount,
                        valueRange = 0f..30f,
                        steps = 30,
                        unitLabel = "Doors",
                        onValueChange = { viewModel.updateGreenDoorsCount(it) },
                        description = "Number of Green Super Missile doors placed."
                    )
                }
                item {
                    CustomSliderField(
                        title = "Yellow Doors Count (Power Bomb)",
                        value = profile.yellowDoorsCount,
                        valueRange = 0f..25f,
                        steps = 25,
                        unitLabel = "Doors",
                        onValueChange = { viewModel.updateYellowDoorsCount(it) },
                        description = "Number of Yellow Power Bomb doors placed."
                    )
                }
                item {
                    CustomSliderField(
                        title = "Beam Doors Count",
                        value = profile.beamDoorsCount,
                        valueRange = 0f..25f,
                        steps = 25,
                        unitLabel = "Doors",
                        onValueChange = { viewModel.updateBeamDoorsCount(it) },
                        description = "Number of Beam-locked doors placed."
                    )
                }

                // Start Location & Animals
                item {
                    CustomDropdownField(
                        label = "Start Location Mode",
                        selectedValue = profile.startLocationMode,
                        options = listOf("Ship", "Random", "Escape", "Custom"),
                        onOptionSelected = { viewModel.updateStartLocationMode(it) },
                        description = "Starting room selection for Samus."
                    )
                }
                item {
                    CustomDropdownField(
                        label = "Save the Animals",
                        selectedValue = profile.saveAnimals,
                        options = listOf("No", "Yes", "Optional", "Random"),
                        onOptionSelected = { viewModel.updateSaveAnimals(it) },
                        description = "Rules regarding saving Dachora & Etecoons during escape."
                    )
                }
            }
            3 -> {
                // QoL, Cosmetics & Controls
                item {
                    CustomDropdownField(
                        label = "Quality-of-Life Preset",
                        selectedValue = profile.qolPreset,
                        options = listOf("Off", "Low", "Default", "High", "Max"),
                        onOptionSelected = { viewModel.setQolPreset(it) },
                        description = "Pre-configured Quality-of-Life feature set."
                    )
                }
                item {
                    CustomSwitchCard(
                        title = "Fast Elevators",
                        description = "Accelerates elevator travel speed",
                        checked = profile.qolFastElevators,
                        onCheckedChange = { viewModel.updateQolFastElevators(it) }
                    )
                }
                item {
                    CustomSwitchCard(
                        title = "Fast Doors",
                        description = "Accelerates door transition speed",
                        checked = profile.qolFastDoors,
                        onCheckedChange = { viewModel.updateQolFastDoors(it) }
                    )
                }
                item {
                    CustomSwitchCard(
                        title = "Fast Text",
                        description = "Displays text messages instantly",
                        checked = profile.qolFastText,
                        onCheckedChange = { viewModel.updateQolFastText(it) }
                    )
                }
                item {
                    CustomSwitchCard(
                        title = "Cutscene Skips",
                        description = "Skips unskippable intro and boss cutscenes",
                        checked = profile.qolCutsceneSkip,
                        onCheckedChange = { viewModel.updateQolCutsceneSkip(it) }
                    )
                }
                item {
                    CustomSwitchCard(
                        title = "Quick Morph",
                        description = "Instant Morph Ball button shortcut",
                        checked = profile.qolQuickMorph,
                        onCheckedChange = { viewModel.updateQolQuickMorph(it) }
                    )
                }
                item {
                    CustomSwitchCard(
                        title = "Spin Jump Restart",
                        description = "Allows restarting spin jumps mid-air",
                        checked = profile.qolSpinJumpRestart,
                        onCheckedChange = { viewModel.updateQolSpinJumpRestart(it) }
                    )
                }
                item {
                    CustomSwitchCard(
                        title = "Auto-Charge Beam",
                        description = "Automatically charges beam without holding shoot",
                        checked = profile.qolAutoChargeBeam,
                        onCheckedChange = { viewModel.updateQolAutoChargeBeam(it) }
                    )
                }
                item {
                    CustomDropdownField(
                        label = "Mother Brain Fight",
                        selectedValue = profile.qolMotherBrainFight,
                        options = listOf("Short", "Vanilla", "Skip"),
                        onOptionSelected = { viewModel.updateQolMotherBrainFight(it) },
                        description = "Cutscene length and fight length for Mother Brain."
                    )
                }
                item {
                    CustomDropdownField(
                        label = "Item Markers on Map",
                        selectedValue = profile.qolItemMarkers,
                        options = listOf("3-Tiered", "Simple", "None"),
                        onOptionSelected = { viewModel.updateItemMarkers(it) },
                        description = "Visual icon style for item locations on in-game map."
                    )
                }
                item {
                    CustomSwitchCard(
                        title = "Reveal Room Outlines",
                        description = "Shows room borders on map upon area entry",
                        checked = profile.qolRoomOutlineRevealed,
                        onCheckedChange = { viewModel.updateQolRoomOutlineRevealed(it) }
                    )
                }
                item {
                    CustomSwitchCard(
                        title = "Hazard Markers on Map",
                        description = "Icons for heat/acid/lava/water hazards",
                        checked = profile.qolHazardMarkers,
                        onCheckedChange = { viewModel.updateQolHazardMarkers(it) }
                    )
                }
                item {
                    CustomSwitchCard(
                        title = "Fix Blue Echoes Glitch",
                        description = "Fixes visual blue residual trail glitches",
                        checked = profile.qolFixBlueEchoes,
                        onCheckedChange = { viewModel.updateQolFixBlueEchoes(it) }
                    )
                }
                item {
                    CustomSwitchCard(
                        title = "Early Save Point Access",
                        description = "Enables early save station access before bosses",
                        checked = profile.qolEarlySave,
                        onCheckedChange = { viewModel.updateQolEarlySave(it) }
                    )
                }
                item {
                    CustomSwitchCard(
                        title = "Refill All Ammo at Stations",
                        description = "Refills Missiles, Supers, and Power Bombs at refill stations",
                        checked = profile.qolAmmoRefillAll,
                        onCheckedChange = { viewModel.updateQolAmmoRefillAll(it) }
                    )
                }
                item {
                    CustomSwitchCard(
                        title = "Reserve Tank HUD Style",
                        description = "Modern HUD display for reserve tanks",
                        checked = profile.reserveHudStyle,
                        onCheckedChange = { viewModel.updateReserveHudStyle(it) }
                    )
                }
                item {
                    CustomSwitchCard(
                        title = "Area Transition Letters",
                        description = "Displays area name letters during transitions",
                        checked = profile.transitionLetters,
                        onCheckedChange = { viewModel.updateTransitionLetters(it) }
                    )
                }
                item {
                    CustomSwitchCard(
                        title = "Display Room Names on Map",
                        description = "Shows room name labels on map screen",
                        checked = profile.roomNames,
                        onCheckedChange = { viewModel.updateRoomNames(it) }
                    )
                }
                item {
                    CustomSwitchCard(
                        title = "Display Boss Icons on Map",
                        description = "Shows boss icons on map screen",
                        checked = profile.bossIcons,
                        onCheckedChange = { viewModel.updateBossIcons(it) }
                    )
                }
                item {
                    CustomSwitchCard(
                        title = "Display Miniboss Icons on Map",
                        description = "Shows miniboss icons on map screen",
                        checked = profile.minibossIcons,
                        onCheckedChange = { viewModel.updateMinibossIcons(it) }
                    )
                }
                item {
                    CustomSwitchCard(
                        title = "Display Save Station Icons on Map",
                        description = "Shows save station icons on map screen",
                        checked = profile.saveIcons,
                        onCheckedChange = { viewModel.updateSaveIcons(it) }
                    )
                }
            }
            4 -> {
                // Controls & Quick Reload
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(text = "Controller Bindings & Layout", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Text(text = "Customize button mappings for all 7 standard SNES controller inputs.", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
                item {
                    ControllerBindingRow(
                        actionName = "Shot",
                        selectedButton = profile.controlShot,
                        onButtonSelected = { viewModel.updateControlShot(it) }
                    )
                }
                item {
                    ControllerBindingRow(
                        actionName = "Jump",
                        selectedButton = profile.controlJump,
                        onButtonSelected = { viewModel.updateControlJump(it) }
                    )
                }
                item {
                    ControllerBindingRow(
                        actionName = "Dash",
                        selectedButton = profile.controlDash,
                        onButtonSelected = { viewModel.updateControlDash(it) }
                    )
                }
                item {
                    ControllerBindingRow(
                        actionName = "Item Select",
                        selectedButton = profile.controlItemSelect,
                        onButtonSelected = { viewModel.updateControlItemSelect(it) }
                    )
                }
                item {
                    ControllerBindingRow(
                        actionName = "Item Cancel",
                        selectedButton = profile.controlItemCancel,
                        onButtonSelected = { viewModel.updateControlItemCancel(it) }
                    )
                }
                item {
                    ControllerBindingRow(
                        actionName = "Aim Angle Up",
                        selectedButton = profile.controlAngleUp,
                        onButtonSelected = { viewModel.updateControlAngleUp(it) }
                    )
                }
                item {
                    ControllerBindingRow(
                        actionName = "Aim Angle Down",
                        selectedButton = profile.controlAngleDown,
                        onButtonSelected = { viewModel.updateControlAngleDown(it) }
                    )
                }

                // Quick Reload & In-Game Save States Header
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(text = "Quick Reload & In-Game Save States", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Text(text = "Configure instant reloading, in-game save state shortcuts, and spin locking.", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
                item {
                    val activeSet = profile.quickReloadButtons.split(",").map { it.trim() }.toSet()
                    MultiButtonShortcutCard(
                        actionName = "Quick Reload Action",
                        description = "Buttons held simultaneously in-game to reload the last save point.",
                        buttons = com.maprandroid.data.local.ALL_SNES_BUTTONS.map { btn -> btn to activeSet.contains(btn) },
                        onToggle = { btn, checked -> viewModel.toggleQuickReloadButton(btn, checked) }
                    )
                }
                item {
                    CustomDropdownField(
                        label = "In-Game Savestates Mode",
                        selectedValue = profile.savestate,
                        options = listOf("No", "Limited", "Unlimited"),
                        onOptionSelected = { viewModel.updateSavestate(it) },
                        description = "Enables in-game save state creation and loading shortcuts."
                    )
                }
                item {
                    val activeSet = profile.saveStateButtons.split(",").map { it.trim() }.toSet()
                    MultiButtonShortcutCard(
                        actionName = "Save State Action",
                        description = "Buttons held simultaneously in-game to save current state (requires Savestates enabled).",
                        buttons = com.maprandroid.data.local.ALL_SNES_BUTTONS.map { btn -> btn to activeSet.contains(btn) },
                        onToggle = { btn, checked -> viewModel.toggleSaveStateButton(btn, checked) }
                    )
                }
                item {
                    val activeSet = profile.loadStateButtons.split(",").map { it.trim() }.toSet()
                    MultiButtonShortcutCard(
                        actionName = "Load State Action",
                        description = "Buttons held simultaneously in-game to load saved state (requires Savestates enabled).",
                        buttons = com.maprandroid.data.local.ALL_SNES_BUTTONS.map { btn -> btn to activeSet.contains(btn) },
                        onToggle = { btn, checked -> viewModel.toggleLoadStateButton(btn, checked) }
                    )
                }
                item {
                    CustomSwitchCard(
                        title = "Spin Lock X",
                        description = "Locks spin jump orientation when holding X",
                        checked = profile.spinLockX,
                        onCheckedChange = { viewModel.updateSpinLockX(it) }
                    )
                }
                item {
                    CustomSwitchCard(
                        title = "Spin Lock L",
                        description = "Locks spin jump orientation when holding L",
                        checked = profile.spinLockL,
                        onCheckedChange = { viewModel.updateSpinLockL(it) }
                    )
                }
                item {
                    CustomSwitchCard(
                        title = "Spin Lock R",
                        description = "Locks spin jump orientation when holding R",
                        checked = profile.spinLockR,
                        onCheckedChange = { viewModel.updateSpinLockR(it) }
                    )
                }
                item {
                    CustomSwitchCard(
                        title = "Spin Lock Up",
                        description = "Locks spin jump orientation when holding Up",
                        checked = profile.spinLockUp,
                        onCheckedChange = { viewModel.updateSpinLockUp(it) }
                    )
                }
                item {
                    CustomSwitchCard(
                        title = "Moonwalk Mode",
                        description = "Allows walking backward while firing beam",
                        checked = profile.moonwalk,
                        onCheckedChange = { viewModel.updateMoonwalk(it) }
                    )
                }
            }
            5 -> {
                // Cosmetics & Audio
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(text = "Cosmetics & Visual Themes", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Text(text = "Customize Samus sprite, HUD, themes, and room palettes.", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
                item {
                    val spriteOptions = remember(profile.samusSprite) {
                        val catalogSprites = com.maprandroid.data.model.SpriteCatalog.availableSprites
                        if (catalogSprites.contains(profile.samusSprite)) {
                            catalogSprites
                        } else {
                            listOf(profile.samusSprite) + catalogSprites
                        }
                    }
                    CustomDropdownField(
                        label = "Samus Sprite",
                        selectedValue = profile.samusSprite,
                        options = spriteOptions,
                        onOptionSelected = { viewModel.updateSamusSprite(it) },
                        description = "Visual character sprite used for Samus."
                    )
                }
                item {
                    EtankColorPickerCard(
                        selectedHex = profile.etankColor,
                        onColorSelected = { viewModel.updateEtankColor(it) }
                    )
                }
                item {
                    CustomDropdownField(
                        label = "Map Theme",
                        selectedValue = profile.mapTheme,
                        options = listOf("Light", "Dark", "Vanilla"),
                        onOptionSelected = { viewModel.updateMapTheme(it) },
                        description = "Map UI color scheme."
                    )
                }
                item {
                    CustomDropdownField(
                        label = "Room Palettes",
                        selectedValue = profile.roomPalettes,
                        options = listOf("vanilla", "area", "shuffled"),
                        onOptionSelected = { viewModel.updateRoomPalettes(it) },
                        description = "Color palette theme for rooms across Zebes."
                    )
                }
                item {
                    CustomDropdownField(
                        label = "Tile Theme",
                        selectedValue = profile.tileTheme,
                        options = listOf("none", "outer_space"),
                        onOptionSelected = { viewModel.updateTileTheme(it) },
                        description = "Tileset graphic theme overrides."
                    )
                }
                item {
                    CustomDropdownField(
                        label = "Door Theme",
                        selectedValue = profile.doorTheme,
                        options = listOf("vanilla", "alternate"),
                        onOptionSelected = { viewModel.updateDoorTheme(it) },
                        description = "Visual style of door hatches."
                    )
                }
                item {
                    CustomDropdownField(
                        label = "Item Dot Display",
                        selectedValue = profile.itemDotChange,
                        options = listOf("Fade", "Disappear", "None"),
                        onOptionSelected = { viewModel.updateItemDotChange(it) },
                        description = "Behavior of item dots on map after collection."
                    )
                }
                item {
                    CustomSwitchCard(
                        title = "Vanilla Screw Attack Animation",
                        description = "Use original vanilla animation effect for Screw Attack",
                        checked = profile.vanillaScrewAttackAnimation,
                        onCheckedChange = { viewModel.updateVanillaScrewAttackAnimation(it) }
                    )
                }

                // Audio & Convenience Effects Header
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(text = "Audio & Accessibility Effects", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Text(text = "Configure music, low health warnings, screen shaking, and flashing.", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
                item {
                    CustomDropdownField(
                        label = "Music Track Selection",
                        selectedValue = profile.music,
                        options = listOf("area", "vanilla", "disabled", "shuffled"),
                        onOptionSelected = { viewModel.updateMusic(it) },
                        description = "Background music assignment for zones."
                    )
                }
                item {
                    CustomSwitchCard(
                        title = "Disable Low-Health Beeping",
                        description = "Disables repeating warning alarm sound when Samus is low on health",
                        checked = profile.disableBeeping,
                        onCheckedChange = { viewModel.updateDisableBeeping(it) }
                    )
                }
                item {
                    CustomDropdownField(
                        label = "Screen Shaking Effects",
                        selectedValue = profile.shaking,
                        options = listOf("Vanilla", "Reduced", "Disabled"),
                        onOptionSelected = { viewModel.updateShaking(it) },
                        description = "Intensity of screen shake visual effects."
                    )
                }
                item {
                    CustomDropdownField(
                        label = "Screen Flashing Effects",
                        selectedValue = profile.flashing,
                        options = listOf("Vanilla", "Reduced", "Disabled"),
                        onOptionSelected = { viewModel.updateFlashing(it) },
                        description = "Intensity of bright flash visual effects."
                    )
                }
                item {
                    CustomDropdownField(
                        label = "Statues Hallway Tiling",
                        selectedValue = profile.statuesHallwayTiling,
                        options = listOf("Default", "Vanilla"),
                        onOptionSelected = { viewModel.updateStatuesHallwayTiling(it) },
                        description = "Visual tiling style for statues hallway room."
                    )
                }
                item {
                    CustomDropdownField(
                        label = "Statues Hallway Audio",
                        selectedValue = profile.statuesHallwayAudio,
                        options = listOf("Enabled", "Disabled"),
                        onOptionSelected = { viewModel.updateStatuesHallwayAudio(it) },
                        description = "Audio ambience in statues hallway room."
                    )
                }

                // Difficulty & Tuning Header
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(text = "Difficulty & Game Tuning", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Text(text = "Configure wall jumping, speed booster splits, glitch allowances, and race mode.", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
                item {
                    CustomDropdownField(
                        label = "Wall Jump Setting",
                        selectedValue = profile.wallJumpSetting,
                        options = listOf("Vanilla", "Collectible"),
                        onOptionSelected = { viewModel.updateWallJumpSetting(it) },
                        description = "Whether wall jump is available from start or requires item collect."
                    )
                }
                item {
                    CustomDropdownField(
                        label = "Speed Booster Setting",
                        selectedValue = profile.speedBoosterSetting,
                        options = listOf("Vanilla", "Split"),
                        onOptionSelected = { viewModel.updateSpeedBoosterSetting(it) },
                        description = "Vanilla Speed Booster vs Split (Spark & Blue booster)."
                    )
                }
                item {
                    CustomDropdownField(
                        label = "Door Locks Size on Map",
                        selectedValue = profile.doorLocksSizeOnMap,
                        options = listOf("Small", "Large"),
                        onOptionSelected = { viewModel.updateDoorLocksSizeOnMap(it) },
                        description = "Size of door lock icons rendered on the map."
                    )
                }
                item {
                    CustomSwitchCard(
                        title = "Energy-Free Shinesparks",
                        description = "Prevents shinesparks from draining Samus's energy",
                        checked = profile.energyFreeShinesparks,
                        onCheckedChange = { viewModel.updateEnergyFreeShinesparks(it) }
                    )
                }
                item {
                    CustomSwitchCard(
                        title = "All Enemies Respawn",
                        description = "Causes defeated enemies to respawn on room re-entry",
                        checked = profile.allEnemiesRespawn,
                        onCheckedChange = { viewModel.updateAllEnemiesRespawn(it) }
                    )
                }
                item {
                    CustomSwitchCard(
                        title = "Disable Spikesuits",
                        description = "Disables spike suit glitch techniques in logic",
                        checked = profile.disableSpikesuit,
                        onCheckedChange = { viewModel.updateDisableSpikesuit(it) }
                    )
                }
                item {
                    CustomSwitchCard(
                        title = "Disable Blue Suits",
                        description = "Disables blue suit glitch techniques in logic",
                        checked = profile.disableBluesuit,
                        onCheckedChange = { viewModel.updateDisableBluesuit(it) }
                    )
                }
                item {
                    CustomSwitchCard(
                        title = "Enable Major Glitches",
                        description = "Enables major glitch techniques in game logic",
                        checked = profile.enableMajorGlitches,
                        onCheckedChange = { viewModel.updateEnableMajorGlitches(it) }
                    )
                }
                item {
                    CustomSwitchCard(
                        title = "Race Mode",
                        description = "Hides spoiler logs and disables in-game savestates for competitive races",
                        checked = profile.raceMode,
                        onCheckedChange = { viewModel.updateRaceMode(it) }
                    )
                }

                item {
                    CustomDropdownField(
                        label = "Wall Jump Setting",
                        selectedValue = profile.wallJumpSetting,
                        options = listOf("Vanilla", "Collectible"),
                        onOptionSelected = { viewModel.updateWallJumpSetting(it) },
                        description = "Whether wall jump is available from start or requires item collect."
                    )
                }
                item {
                    CustomDropdownField(
                        label = "Speed Booster Setting",
                        selectedValue = profile.speedBoosterSetting,
                        options = listOf("Vanilla", "Split"),
                        onOptionSelected = { viewModel.updateSpeedBoosterSetting(it) },
                        description = "Vanilla Speed Booster vs Split (Spark & Blue booster)."
                    )
                }
                item {
                    CustomDropdownField(
                        label = "Door Locks Size on Map",
                        selectedValue = profile.doorLocksSizeOnMap,
                        options = listOf("Small", "Large"),
                        onOptionSelected = { viewModel.updateDoorLocksSizeOnMap(it) },
                        description = "Size of door lock icons rendered on the map."
                    )
                }
                item {
                    CustomSwitchCard(
                        title = "Energy-Free Shinesparks",
                        description = "Prevents shinesparks from draining Samus's energy",
                        checked = profile.energyFreeShinesparks,
                        onCheckedChange = { viewModel.updateEnergyFreeShinesparks(it) }
                    )
                }
                item {
                    CustomSwitchCard(
                        title = "All Enemies Respawn",
                        description = "Causes defeated enemies to respawn on room re-entry",
                        checked = profile.allEnemiesRespawn,
                        onCheckedChange = { viewModel.updateAllEnemiesRespawn(it) }
                    )
                }
                item {
                    CustomSwitchCard(
                        title = "Disable Spikesuits",
                        description = "Disables spike suit glitch techniques in logic",
                        checked = profile.disableSpikesuit,
                        onCheckedChange = { viewModel.updateDisableSpikesuit(it) }
                    )
                }
                item {
                    CustomSwitchCard(
                        title = "Disable Blue Suits",
                        description = "Disables blue suit glitch techniques in logic",
                        checked = profile.disableBluesuit,
                        onCheckedChange = { viewModel.updateDisableBluesuit(it) }
                    )
                }
                item {
                    CustomSwitchCard(
                        title = "Enable Major Glitches",
                        description = "Allows major glitches (out of bounds, door skip) in logic",
                        checked = profile.enableMajorGlitches,
                        onCheckedChange = { viewModel.updateEnableMajorGlitches(it) }
                    )
                }
                item {
                    CustomSwitchCard(
                        title = "Race Mode",
                        description = "Hides spoiler logs and seed details until completion",
                        checked = profile.raceMode,
                        onCheckedChange = { viewModel.updateRaceMode(it) }
                    )
                }
                item {
                    CustomDropdownField(
                        label = "Savestates Allowed",
                        selectedValue = profile.savestate,
                        options = listOf("No", "Limited", "Unlimited"),
                        onOptionSelected = { viewModel.updateSavestate(it) },
                        description = "In-game savestate availability rules."
                    )
                }
                item {
                    CustomDropdownField(
                        label = "Resource Tanks Availability",
                        selectedValue = profile.difficultyResource,
                        options = listOf("Casual", "Standard", "Harsh"),
                        onOptionSelected = { viewModel.updateDifficultyResource(it) },
                        description = "Availability of Energy Tanks, Reserve Tanks, and ammo upgrades."
                    )
                }
                item {
                    CustomDropdownField(
                        label = "Damage Multiplier",
                        selectedValue = profile.damageMultiplier,
                        options = listOf("0.5x", "1.0x", "1.5x", "2.0x", "3.0x"),
                        onOptionSelected = { viewModel.updateDamageMultiplier(it) },
                        description = "Multiplier applied to all enemy and hazard damage."
                    )
                }
            }
        }
    }
}

@Composable
internal fun PresetsList(
    presets: List<String>,
    selectedPreset: String?,
    isLoading: Boolean,
    onSelectPreset: (String) -> Unit,
    onRetry: () -> Unit
) {
    if (presets.isEmpty() && !isLoading) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(text = "No presets available.", style = MaterialTheme.typography.bodyLarge)
                Spacer(modifier = Modifier.height(12.dp))
                Button(onClick = onRetry) {
                    Icon(Icons.Default.Refresh, contentDescription = "Retry")
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Retry Loading Presets")
                }
            }
        }
    } else {
        LazyColumn(modifier = Modifier.fillMaxSize()) {
            items(presets) { preset ->
                val isSelected = (preset == selectedPreset)
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                        .selectable(
                            selected = isSelected,
                            onClick = { onSelectPreset(preset) },
                            role = Role.RadioButton
                        ),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(selected = isSelected, onClick = null)
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = preset,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = getPresetDescription(preset),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun EtankColorPickerCard(
    selectedHex: String,
    onColorSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val presetColors = listOf(
        "96de38" to "Lime (Default)",
        "ff3333" to "Red",
        "3399ff" to "Cyan/Blue",
        "ffff33" to "Yellow",
        "a855f7" to "Purple",
        "ff8c00" to "Orange",
        "ff69b4" to "Pink",
        "ffffff" to "White"
    )

    var customHexInput by remember(selectedHex) { mutableStateOf(selectedHex) }

    fun parseColor(hex: String): Color {
        return try {
            val clean = hex.removePrefix("#").trim()
            if (clean.length == 6) {
                val colorInt = android.graphics.Color.parseColor("#$clean")
                Color(colorInt)
            } else {
                Color(0xFF96DE38)
            }
        } catch (_: Exception) {
            Color(0xFF96DE38)
        }
    }

    val currentColor = remember(selectedHex) { parseColor(selectedHex) }

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Energy Tank Display Color",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "Color palette used for E-Tank icons on the HUD.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(currentColor)
                        .border(2.dp, MaterialTheme.colorScheme.outline, CircleShape)
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = "Preset Color Swatches",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.height(6.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                presetColors.forEach { (hex, name) ->
                    val isSelected = selectedHex.equals(hex, ignoreCase = true)
                    val swatchColor = parseColor(hex)

                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(swatchColor)
                            .border(
                                width = if (isSelected) 3.dp else 1.dp,
                                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                                shape = CircleShape
                            )
                            .clickable {
                                customHexInput = hex
                                onColorSelected(hex)
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        if (isSelected) {
                            Icon(
                                Icons.Default.Check,
                                contentDescription = name,
                                tint = if (hex == "ffffff" || hex == "ffff33") Color.Black else Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            OutlinedTextField(
                value = customHexInput,
                onValueChange = { input ->
                    val clean = input.removePrefix("#").take(6)
                    customHexInput = clean
                    if (clean.length == 6 && clean.all { it in "0123456789abcdefABCDEF" }) {
                        onColorSelected(clean.lowercase())
                    }
                },
                label = { Text("Custom Hex Color Code") },
                prefix = { Text("#", fontWeight = FontWeight.Bold) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ControllerBindingRow(
    actionName: String,
    selectedButton: String,
    allButtons: List<String> = listOf("X", "A", "B", "Y", "L", "R", "Select"),
    onButtonSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = actionName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = "Button: $selectedButton",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                allButtons.forEach { btn ->
                    val isSelected = (btn == selectedButton)
                    FilterChip(
                        selected = isSelected,
                        onClick = { onButtonSelected(btn) },
                        label = { Text(btn, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal) },
                        leadingIcon = if (isSelected) {
                            { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(14.dp)) }
                        } else null
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MultiButtonShortcutCard(
    actionName: String,
    description: String,
    buttons: List<Pair<String, Boolean>>,
    onToggle: (String, Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val activeButtons = buttons.filter { it.second }.map { it.first }
    val combinationText = if (activeButtons.isNotEmpty()) activeButtons.joinToString(" + ") else "None"

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = actionName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Surface(
                    color = if (activeButtons.isNotEmpty()) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = "Shortcut: $combinationText",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = if (activeButtons.isNotEmpty()) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                buttons.forEach { (btnLabel, isChecked) ->
                    FilterChip(
                        selected = isChecked,
                        onClick = { onToggle(btnLabel, !isChecked) },
                        label = { Text(btnLabel, fontWeight = if (isChecked) FontWeight.Bold else FontWeight.Normal) },
                        leadingIcon = if (isChecked) {
                            { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(14.dp)) }
                        } else null
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomDropdownField(
    label: String,
    selectedValue: String,
    options: List<String>,
    onOptionSelected: (String) -> Unit,
    modifier: Modifier = Modifier,
    description: String? = null
) {
    var expanded by remember { mutableStateOf(false) }

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(
                text = label,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            if (description != null) {
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            ExposedDropdownMenuBox(
                expanded = expanded,
                onExpandedChange = { expanded = !expanded },
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedTextField(
                    value = selectedValue,
                    onValueChange = {},
                    readOnly = true,
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                    colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
                    modifier = Modifier
                        .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                        .fillMaxWidth()
                )
                ExposedDropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false }
                ) {
                    options.forEach { option ->
                        DropdownMenuItem(
                            text = { Text(option) },
                            onClick = {
                                onOptionSelected(option)
                                expanded = false
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun CustomSwitchCard(
    title: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) },
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange
            )
        }
    }
}

@Composable
fun CustomSliderField(
    title: String,
    value: Int,
    valueRange: ClosedFloatingPointRange<Float>,
    steps: Int,
    unitLabel: String = "",
    onValueChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
    description: String? = null
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "$value $unitLabel",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            if (description != null) {
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Slider(
                value = value.toFloat(),
                onValueChange = { onValueChange(it.toInt()) },
                valueRange = valueRange,
                steps = steps,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
internal fun BaseRomCard(
    baseRomName: String?,
    baseRomStatus: String,
    isValid: Boolean,
    onSelectBaseRom: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isValid) {
                MaterialTheme.colorScheme.surfaceContainerHigh
            } else {
                MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.25f)
            }
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = if (isValid) Icons.Default.CheckCircle else Icons.Default.SdStorage,
                contentDescription = null,
                tint = if (isValid) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                modifier = Modifier.size(28.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Base Super Metroid ROM",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = baseRomName ?: "Not Configured",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = baseRomStatus,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (isValid) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.error
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            OutlinedButton(
                onClick = onSelectBaseRom,
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.FolderOpen,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(if (isValid) "Change" else "Select")
            }
        }
    }
}

@Composable
internal fun ErrorMessageCard(
    errorMessage: String,
    onRetry: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer,
            contentColor = MaterialTheme.colorScheme.onErrorContainer
        ),
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 12.dp)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Warning,
                contentDescription = "Error",
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(text = "Notice", style = MaterialTheme.typography.titleMedium)
                Text(text = errorMessage, style = MaterialTheme.typography.bodyMedium)
            }
            Spacer(modifier = Modifier.width(8.dp))
            FilledTonalButton(onClick = onRetry) {
                Icon(imageVector = Icons.Default.Refresh, contentDescription = "Retry", modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Retry")
            }
        }
    }
}

internal data class CategoryTab(val title: String, val icon: ImageVector)

internal fun getCategoriesList(): List<CategoryTab> {
    return listOf(
        CategoryTab("Skill & Techs", Icons.Default.Psychology),
        CategoryTab("Progression & Pool", Icons.Default.CardGiftcard),
        CategoryTab("Map & Environment", Icons.Default.Place),
        CategoryTab("QoL & Visuals", Icons.Default.Speed),
        CategoryTab("Difficulty & Tuning", Icons.Default.Shield)
    )
}

@Composable
internal fun ProfileManagementHeader(
    savedProfiles: List<ConfigProfileEntity>,
    selectedProfileId: Int?,
    currentProfile: ConfigProfileEntity,
    onSelectProfile: (ConfigProfileEntity) -> Unit,
    onSaveClick: (() -> Unit)? = null,
    onResetClick: () -> Unit,
    onDeleteClick: (Int) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = currentProfile.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (onSaveClick != null) {
                        FilledTonalButton(onClick = onSaveClick, contentPadding = PaddingValues(horizontal = 12.dp)) {
                            Icon(Icons.Default.Save, contentDescription = "Save", modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Save Preset")
                        }
                    }
                    if (selectedProfileId != null) {
                        if (onSaveClick != null) {
                            Spacer(modifier = Modifier.width(6.dp))
                        }
                        IconButton(onClick = { onDeleteClick(selectedProfileId) }) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete Preset", tint = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                FilterChip(
                    selected = selectedProfileId == null,
                    onClick = onResetClick,
                    label = { Text("New Preset") },
                    leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp)) }
                )

                savedProfiles.forEach { profile ->
                    val isSelected = selectedProfileId == profile.id
                    FilterChip(
                        selected = isSelected,
                        onClick = { onSelectProfile(profile) },
                        label = { Text(profile.name) },
                        leadingIcon = if (isSelected) {
                            { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                        } else null
                    )
                }
            }
        }
    }
}

@Composable
internal fun SaveProfileDialog(
    initialName: String,
    isEditing: Boolean = false,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit
) {
    var nameText by remember(initialName) { mutableStateOf(initialName) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (isEditing) "Update Preset" else "Save Preset") },
        text = {
            Column {
                Text(
                    text = if (isEditing) {
                        "Update existing preset settings for quick loading."
                    } else {
                        "Save current settings for quick loading."
                    },
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = nameText,
                    onValueChange = { nameText = it },
                    label = { Text("Preset Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onSave(nameText) },
                enabled = nameText.isNotBlank()
            ) {
                Text(if (isEditing) "Update" else "Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun GenerationProgressModal(
    uiState: ConfigUiState,
    viewModel: ConfigViewModel? = null,
    onDismiss: () -> Unit,
    onLaunchRom: (String, String?) -> Unit
) {
    val context = LocalContext.current

    if (uiState.isCustomizingSeed && viewModel != null) {
        var activeSubTab by remember { mutableIntStateOf(0) }
        val customProfile = uiState.seedCustomizationProfile

        AlertDialog(
            onDismissRequest = onDismiss,
            properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false),
            modifier = Modifier
                .fillMaxWidth(0.88f)
                .widthIn(max = 750.dp),
            title = {
                Text(
                    text = "Customize Seed #${uiState.generatedSeedId ?: uiState.pendingSeedId ?: ""}",
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 420.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    PrimaryTabRow(selectedTabIndex = activeSubTab, modifier = Modifier.fillMaxWidth()) {
                        Tab(selected = activeSubTab == 0, onClick = { activeSubTab = 0 }, text = { Text("Controls") })
                        Tab(selected = activeSubTab == 1, onClick = { activeSubTab = 1 }, text = { Text("Reload & States") })
                        Tab(selected = activeSubTab == 2, onClick = { activeSubTab = 2 }, text = { Text("Cosmetics & Audio") })
                    }

                    when (activeSubTab) {
                        0 -> {
                            ControllerBindingRow(
                                actionName = "Shot",
                                selectedButton = customProfile.controlShot,
                                onButtonSelected = { viewModel.updateCustomizationControlShot(it) }
                            )
                            ControllerBindingRow(
                                actionName = "Jump",
                                selectedButton = customProfile.controlJump,
                                onButtonSelected = { viewModel.updateCustomizationControlJump(it) }
                            )
                            ControllerBindingRow(
                                actionName = "Dash",
                                selectedButton = customProfile.controlDash,
                                onButtonSelected = { viewModel.updateCustomizationControlDash(it) }
                            )
                            ControllerBindingRow(
                                actionName = "Item Select",
                                selectedButton = customProfile.controlItemSelect,
                                onButtonSelected = { viewModel.updateCustomizationControlItemSelect(it) }
                            )
                            ControllerBindingRow(
                                actionName = "Item Cancel",
                                selectedButton = customProfile.controlItemCancel,
                                onButtonSelected = { viewModel.updateCustomizationControlItemCancel(it) }
                            )
                            ControllerBindingRow(
                                actionName = "Aim Angle Up",
                                selectedButton = customProfile.controlAngleUp,
                                onButtonSelected = { viewModel.updateCustomizationControlAngleUp(it) }
                            )
                            ControllerBindingRow(
                                actionName = "Aim Angle Down",
                                selectedButton = customProfile.controlAngleDown,
                                onButtonSelected = { viewModel.updateCustomizationControlAngleDown(it) }
                            )
                        }
                        1 -> {
                            val quickReloadSet = customProfile.quickReloadButtons.split(",").map { it.trim() }.toSet()
                            MultiButtonShortcutCard(
                                actionName = "Quick Reload Action",
                                description = "Buttons held simultaneously in-game to reload the last save point.",
                                buttons = com.maprandroid.data.local.ALL_SNES_BUTTONS.map { btn -> btn to quickReloadSet.contains(btn) },
                                onToggle = { btn, checked -> viewModel.toggleCustomizationQuickReloadButton(btn, checked) }
                            )

                            CustomDropdownField(
                                label = "In-Game Savestates Mode",
                                selectedValue = customProfile.savestate,
                                options = listOf("No", "Limited", "Unlimited"),
                                onOptionSelected = { viewModel.updateCustomizationSavestate(it) },
                                description = "Enables in-game save state creation and loading shortcuts."
                            )

                            val saveStateSet = customProfile.saveStateButtons.split(",").map { it.trim() }.toSet()
                            MultiButtonShortcutCard(
                                actionName = "Save State Action",
                                description = "Buttons held simultaneously in-game to save current state (requires Savestates enabled).",
                                buttons = com.maprandroid.data.local.ALL_SNES_BUTTONS.map { btn -> btn to saveStateSet.contains(btn) },
                                onToggle = { btn, checked -> viewModel.toggleCustomizationSaveStateButton(btn, checked) }
                            )

                            val loadStateSet = customProfile.loadStateButtons.split(",").map { it.trim() }.toSet()
                            MultiButtonShortcutCard(
                                actionName = "Load State Action",
                                description = "Buttons held simultaneously in-game to load saved state (requires Savestates enabled).",
                                buttons = com.maprandroid.data.local.ALL_SNES_BUTTONS.map { btn -> btn to loadStateSet.contains(btn) },
                                onToggle = { btn, checked -> viewModel.toggleCustomizationLoadStateButton(btn, checked) }
                            )

                            CustomSwitchCard(
                                title = "Moonwalk Mode",
                                description = "Walk backwards while firing",
                                checked = customProfile.moonwalk,
                                onCheckedChange = { viewModel.updateCustomizationMoonwalk(it) }
                            )
                        }
                        2 -> {
                            val customizationSpriteOptions = remember(customProfile.samusSprite) {
                                val catalogSprites = com.maprandroid.data.model.SpriteCatalog.availableSprites
                                if (catalogSprites.contains(customProfile.samusSprite)) {
                                    catalogSprites
                                } else {
                                    listOf(customProfile.samusSprite) + catalogSprites
                                }
                            }
                            CustomDropdownField(
                                label = "Samus Sprite",
                                selectedValue = customProfile.samusSprite,
                                options = customizationSpriteOptions,
                                onOptionSelected = { viewModel.updateCustomizationSamusSprite(it) }
                            )
                            EtankColorPickerCard(
                                selectedHex = customProfile.etankColor,
                                onColorSelected = { viewModel.updateCustomizationEtankColor(it) }
                            )
                            CustomDropdownField(
                                label = "Music Track Selection",
                                selectedValue = customProfile.music,
                                options = listOf("area", "vanilla", "disabled", "shuffled"),
                                onOptionSelected = { viewModel.updateCustomizationMusic(it) }
                            )
                            CustomSwitchCard(
                                title = "Disable Low Health Beep",
                                description = "Disables repeating alarm when health is low",
                                checked = customProfile.disableBeeping,
                                onCheckedChange = { viewModel.updateCustomizationDisableBeeping(it) }
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = { viewModel.finalizeSeedCustomization(context) }
                ) {
                    Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Apply & Patch ROM")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = onDismiss) {
                    Text("Cancel")
                }
            }
        )
        return
    }
    AlertDialog(
        onDismissRequest = {
            if (!uiState.isGenerating) {
                onDismiss()
            }
        },
        icon = {
            when {
                uiState.isGenerating -> CircularProgressIndicator(modifier = Modifier.size(36.dp), strokeWidth = 3.dp)
                uiState.generationSuccess -> Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(36.dp), tint = MaterialTheme.colorScheme.primary)
                uiState.generationError != null -> Icon(Icons.Default.Error, contentDescription = null, modifier = Modifier.size(36.dp), tint = MaterialTheme.colorScheme.error)
            }
        },
        title = {
            Text(
                text = when {
                    uiState.isGenerating -> "Generating Seed..."
                    uiState.generationSuccess -> "Generation Complete!"
                    else -> "Generation Failed"
                },
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (uiState.generationError != null) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = uiState.generationError,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                } else {
                    val steps = listOf(
                        GenerationStep.GENERATING_SEED,
                        GenerationStep.READING_BASE_ROM,
                        GenerationStep.PATCHING_ROM,
                        GenerationStep.SAVING_ROM
                    )

                    val currentStepNum = uiState.generationStep?.stepNumber ?: 0

                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        steps.forEach { step ->
                            val isDone = uiState.generationSuccess || step.stepNumber < currentStepNum
                            val isCurrent = uiState.isGenerating && step.stepNumber == currentStepNum

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                when {
                                    isDone -> {
                                        Icon(
                                            Icons.Default.CheckCircle,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                    isCurrent -> {
                                        CircularProgressIndicator(
                                            strokeWidth = 2.dp,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                    else -> {
                                        Icon(
                                            Icons.Default.RadioButtonUnchecked,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.width(10.dp))

                                Text(
                                    text = step.description,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal,
                                    color = when {
                                        isDone -> MaterialTheme.colorScheme.onSurface
                                        isCurrent -> MaterialTheme.colorScheme.primary
                                        else -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                    }
                                )
                            }
                        }
                    }

                    if (uiState.generationSuccess) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(
                                    text = "Seed ${uiState.generatedSeedId ?: ""} Ready!",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                                Text(
                                    text = "ROM saved to device library and downloads.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            if (uiState.generationSuccess) {
                Button(
                    onClick = {
                        val romPath = uiState.generatedRomPath ?: ""
                        val pkgName = uiState.emulatorPackageName
                        onLaunchRom(romPath, pkgName)
                    }
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Launch ROM")
                }
            }
        },
        dismissButton = {
            if (uiState.generationSuccess) {
                OutlinedButton(onClick = onDismiss) {
                    Text("Dismiss")
                }
            } else if (!uiState.isGenerating) {
                TextButton(onClick = onDismiss) {
                    Text("Close")
                }
            }
        }
    )
}

internal fun getPresetDescription(preset: String): String {
    return when (preset) {
        "Default" -> "Standard balanced map layout and item placement."
        "Community Race Season 5" -> "Official race settings with standard logic and area transitions."
        "Mentor Tournament" -> "Beginner-friendly progression with accessible item checks."
        "Summer Series Expert Challenge" -> "High skill logic requirement with complex area shuffling."
        "Standard" -> "Classic Super Metroid randomizer experience."
        "Wild" -> "Unrestricted wild area and item placement."
        else -> "Custom map rando preset."
    }
}
