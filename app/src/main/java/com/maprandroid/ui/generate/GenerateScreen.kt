package com.maprandroid.ui.generate

import android.widget.Toast
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.maprandroid.ui.config.ConfigUiState
import com.maprandroid.ui.config.ConfigViewModel
import com.maprandroid.ui.config.ErrorMessageCard
import com.maprandroid.ui.config.GenerationProgressModal
import com.maprandroid.ui.config.PresetsList
import com.maprandroid.util.EmulatorLauncher

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GenerateScreen(
    viewModel: ConfigViewModel,
    onEditPresetClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    if (uiState.showGenerationModal) {
        GenerationProgressModal(
            uiState = uiState,
            viewModel = viewModel,
            onDismiss = { viewModel.dismissGenerationModal() },
            onLaunchRom = { romPath, pkgName ->
                viewModel.dismissGenerationModal()
                if (romPath.isNotEmpty()) {
                    val success = EmulatorLauncher.launchEmulator(context, romPath, pkgName)
                    if (!success) {
                        Toast.makeText(context, "Could not launch emulator.", Toast.LENGTH_LONG).show()
                    }
                }
            }
        )
    }

    Scaffold(
        floatingActionButton = {
            val hasBaseRom = uiState.isBaseRomValid
            val canGenerate = (uiState.isCustomMode || uiState.selectedPreset != null) && !uiState.isLoading

            FloatingActionButton(
                onClick = {
                    if (hasBaseRom && canGenerate) {
                        viewModel.generateRom(context)
                    }
                },
                containerColor = if (hasBaseRom && canGenerate) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerHighest,
                contentColor = if (hasBaseRom && canGenerate) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (uiState.isLoading) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                    } else {
                        Icon(
                            imageVector = if (hasBaseRom) Icons.Default.PlayArrow else Icons.Default.Warning,
                            contentDescription = null
                        )
                        Text(
                            text = if (hasBaseRom) "Generate Seed" else "Configure ROM in Settings",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    ) { padding ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
        ) {
            val errMsg = uiState.errorMessage
            if (uiState.isError && errMsg != null) {
                ErrorMessageCard(errorMessage = errMsg) {
                    viewModel.clearError()
                    viewModel.fetchPresets()
                }
            }

            // Mode Selector: Official Server Presets vs Custom User Presets
            SingleChoiceSegmentedButtonRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
            ) {
                SegmentedButton(
                    selected = !uiState.isCustomMode,
                    onClick = { viewModel.setCustomMode(false) },
                    shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2)
                ) {
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Official Presets")
                }
                SegmentedButton(
                    selected = uiState.isCustomMode,
                    onClick = { viewModel.setCustomMode(true) },
                    shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2)
                ) {
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Custom Presets")
                }
            }

            Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                if (!uiState.isCustomMode) {
                    // Official Presets List
                    PresetsList(
                        presets = uiState.presets,
                        selectedPreset = uiState.selectedPreset,
                        isLoading = uiState.isLoading,
                        onSelectPreset = { viewModel.selectPreset(it) },
                        onRetry = { viewModel.fetchPresets() }
                    )
                } else {
                    // Custom Presets Selector & Summary
                    CustomPresetsSummarySection(
                        uiState = uiState,
                        viewModel = viewModel,
                        onEditPresetClick = onEditPresetClick
                    )
                }
            }
        }
    }
}

@Composable
private fun CustomPresetsSummarySection(
    uiState: ConfigUiState,
    viewModel: ConfigViewModel,
    onEditPresetClick: (() -> Unit)? = null
) {
    LaunchedEffect(uiState.isCustomMode, uiState.selectedProfileId, uiState.savedProfiles) {
        if (uiState.isCustomMode && uiState.selectedProfileId == null && uiState.savedProfiles.isNotEmpty()) {
            viewModel.loadProfile(uiState.savedProfiles.first())
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Text(
                    text = "Select Custom Preset",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                if (uiState.savedProfiles.isEmpty()) {
                    Text(
                        text = "No saved custom presets found. Create and save custom presets in the Presets tab.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        uiState.savedProfiles.forEach { profile ->
                            val isSelected = uiState.selectedProfileId == profile.id
                            FilterChip(
                                selected = isSelected,
                                onClick = { viewModel.loadProfile(profile) },
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

        // Profile Details Summary Card
        val profile = uiState.currentCustomProfile
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = profile.name,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f)
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Badge(
                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                        ) {
                            Text(
                                text = profile.logicSkill,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                style = MaterialTheme.typography.labelMedium
                            )
                        }
                        if (onEditPresetClick != null) {
                            OutlinedButton(
                                onClick = onEditPresetClick,
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                modifier = Modifier.height(32.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Edit,
                                    contentDescription = "Edit Preset",
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Edit", style = MaterialTheme.typography.labelMedium)
                            }
                        }
                    }
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

                SectionHeader("1. Skill Assumptions")
                SummaryRow("Skill Level", profile.logicSkill)

                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                SectionHeader("2. Item Progression & Pool")
                SummaryRow("Progression Preset", profile.itemProgression)
                SummaryRow("Progression Rate / Style", "${profile.progressionRate} / ${profile.itemPlacementStyle}")
                SummaryRow("Item Pool Size", profile.itemPool)
                val startingItems = mutableListOf<String>()
                if (profile.startMorph) startingItems.add("Morph")
                if (profile.startCharge) startingItems.add("Charge")
                if (profile.startVaria) startingItems.add("Varia")
                if (startingItems.isNotEmpty()) {
                    SummaryRow("Starting Items", startingItems.joinToString(", "))
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                SectionHeader("3. Quality of Life")
                SummaryRow("QoL Preset", profile.qolPreset)
                val qolFeatures = mutableListOf<String>()
                if (profile.qolFastElevators) qolFeatures.add("Fast Elevators")
                if (profile.qolFastDoors) qolFeatures.add("Fast Doors")
                if (profile.qolFastText) qolFeatures.add("Fast Text")
                if (profile.qolCutsceneSkip) qolFeatures.add("Cutscene Skip")
                if (profile.qolQuickMorph) qolFeatures.add("Quick Morph")
                if (profile.qolSpinJumpRestart) qolFeatures.add("Spin Jump Restart")
                if (profile.qolAutoChargeBeam) qolFeatures.add("Auto Charge Beam")
                if (qolFeatures.isNotEmpty()) {
                    SummaryRow("Enabled Features", qolFeatures.joinToString(", "))
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                SectionHeader("4. Objectives")
                SummaryRow("Objective Preset", profile.objectivePreset)
                val objectives = mutableListOf<String>()
                if (profile.kraidObjective) objectives.add("Kraid")
                if (profile.phantoonObjective) objectives.add("Phantoon")
                if (profile.draygonObjective) objectives.add("Draygon")
                if (profile.ridleyObjective) objectives.add("Ridley")
                SummaryRow("Required Bosses", if (objectives.isEmpty()) "None" else objectives.joinToString(", "))
                SummaryRow("Min / Max Objectives", "${profile.minObjectives} - ${profile.maxObjectives}")

                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                SectionHeader("5. Map Layout & Doors")
                SummaryRow("Map Layout", profile.mapRandomization)
                SummaryRow("Boss Rando", profile.bossRandomization)
                SummaryRow("Door Lock Rando", profile.doorLockRandomization)
                SummaryRow("Doors Preset", profile.doorsPreset)
                SummaryRow("Door Counts (R/G/Y)", "${profile.redDoorsCount} Red / ${profile.greenDoorsCount} Green / ${profile.yellowDoorsCount} Yellow")

                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                SectionHeader("6. Start Location & Save Animals")
                SummaryRow("Start Location Mode", profile.startLocationMode)
                SummaryRow("Save Animals", profile.saveAnimals)

                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                SectionHeader("7. Other Options & Variations")
                SummaryRow("Wall Jump Mechanic", profile.wallJumpSetting)
                SummaryRow("Speed Booster Mechanic", profile.speedBoosterSetting)
                SummaryRow("Area Assignment Order", profile.areaAssignmentBaseOrder)
                SummaryRow("Race Mode", if (profile.raceMode) "Enabled" else "Disabled")
                SummaryRow("In-Game Savestate", profile.savestate)
                SummaryRow("Major Glitches", if (profile.enableMajorGlitches) "Enabled" else "Disabled")
                SummaryRow("Resource / Damage", "${profile.difficultyResource} / ${profile.damageMultiplier}")
            }
        }
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelLarge,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(vertical = 4.dp)
    )
}

@Composable
private fun SummaryRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold
        )
    }
}
