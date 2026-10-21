package com.maprandroid.ui.settings

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle

import com.maprandroid.ui.config.CustomDropdownField

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val availableEmulators by viewModel.availableEmulators.collectAsStateWithLifecycle()
    val selectedEmulatorPackage by viewModel.selectedEmulatorPackage.collectAsStateWithLifecycle()
    val baseRomName by viewModel.baseRomName.collectAsStateWithLifecycle()
    val baseRomStatus by viewModel.baseRomStatus.collectAsStateWithLifecycle()
    val isBaseRomValid by viewModel.isBaseRomValid.collectAsStateWithLifecycle()
    val isUpdatingPresets by viewModel.isUpdatingPresets.collectAsStateWithLifecycle()
    val presetUpdateStatus by viewModel.presetUpdateStatus.collectAsStateWithLifecycle()
    val isUpdatingSprites by viewModel.isUpdatingSprites.collectAsStateWithLifecycle()
    val spriteUpdateStatus by viewModel.spriteUpdateStatus.collectAsStateWithLifecycle()
    val favoriteSprite by viewModel.favoriteSprite.collectAsStateWithLifecycle()
    val availableSprites by viewModel.availableSprites.collectAsStateWithLifecycle()
    val defaultControls by viewModel.defaultControls.collectAsStateWithLifecycle()

    val openBaseRomLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            viewModel.selectBaseRomFile(context, uri)
        }
    }

    Scaffold { padding ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Base ROM Settings Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = if (isBaseRomValid) {
                        MaterialTheme.colorScheme.surfaceContainerHigh
                    } else {
                        MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.25f)
                    }
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Base ROM Settings",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = if (isBaseRomValid) Icons.Default.CheckCircle else Icons.Default.SdStorage,
                            contentDescription = null,
                            tint = if (isBaseRomValid) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = baseRomName ?: "No Base ROM Selected",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = baseRomStatus ?: "Please select a valid Super Metroid JU 1.0 ROM.",
                                style = MaterialTheme.typography.bodySmall,
                                color = if (isBaseRomValid) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.error
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (isBaseRomValid || !baseRomName.isNullOrEmpty()) {
                            TextButton(onClick = { viewModel.clearBaseRom() }) {
                                Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Clear")
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                        }
                        Button(onClick = { openBaseRomLauncher.launch(arrayOf("*/*")) }) {
                            Icon(Icons.Default.FolderOpen, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(if (isBaseRomValid) "Change ROM" else "Select ROM")
                        }
                    }
                }
            }

            // Emulator Settings Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Preferred Emulator Application",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Choose which installed app to use when launching generated ROMs.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    // System Default Option
                    val isSystemDefaultSelected = selectedEmulatorPackage.isNullOrEmpty()
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .clickable(
                                role = Role.RadioButton,
                                onClick = { viewModel.saveEmulatorPackageName(null) }
                            ),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isSystemDefaultSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface
                        )
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = isSystemDefaultSelected,
                                onClick = null
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "System Default / Auto-detect",
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = "Allow Android to offer matching SNES emulators.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    if (availableEmulators.isEmpty()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "No standard SNES emulators detected. Install RetroArch, Snes9x EX+, or Lemuroid from Google Play.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        availableEmulators.forEach { emulator ->
                            val isSelected = (emulator.packageName == selectedEmulatorPackage)
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                                    .clickable(
                                        role = Role.RadioButton,
                                        onClick = { viewModel.saveEmulatorPackageName(emulator.packageName) }
                                    ),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface
                                )
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    RadioButton(
                                        selected = isSelected,
                                        onClick = null
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column {
                                        Text(
                                            text = emulator.name,
                                            style = MaterialTheme.typography.bodyLarge,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                        Text(
                                            text = emulator.packageName,
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

            // Official Presets Settings Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Official Presets Sync",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Pull the latest official randomizer presets dynamically from maprando.com.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    if (!presetUpdateStatus.isNullOrEmpty()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = presetUpdateStatus!!,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        FilledTonalButton(
                            onClick = { viewModel.updateOfficialPresets() },
                            enabled = !isUpdatingPresets
                        ) {
                            if (isUpdatingPresets) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    strokeWidth = 2.dp
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Updating...")
                            } else {
                                Icon(
                                    imageVector = Icons.Default.Refresh,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Update Official Presets")
                            }
                        }
                    }
                }
            }

            // Samus Sprites Sync Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Samus Sprites Sync",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Pull the latest Samus sprite options dynamically from MapRandoSprites repository.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    if (!spriteUpdateStatus.isNullOrEmpty()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = spriteUpdateStatus!!,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        FilledTonalButton(
                            onClick = { viewModel.updateSpriteList() },
                            enabled = !isUpdatingSprites
                        ) {
                            if (isUpdatingSprites) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    strokeWidth = 2.dp
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Updating...")
                            } else {
                                Icon(
                                    imageVector = Icons.Default.Refresh,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Update Sprite List")
                            }
                        }
                    }
                }
            }

            // Favorite Sprite Setting Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Favorite Sprite Settings",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Select your default Samus sprite for new seeds and seed customizations.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    val spriteOptions = remember(favoriteSprite, availableSprites) {
                        if (availableSprites.contains(favoriteSprite)) {
                            availableSprites
                        } else {
                            listOf(favoriteSprite) + availableSprites
                        }
                    }

                    CustomDropdownField(
                        label = "Favorite Sprite",
                        selectedValue = favoriteSprite,
                        options = spriteOptions,
                        onOptionSelected = { viewModel.saveFavoriteSprite(it) },
                        description = "Default character sprite applied when customizing new seeds."
                    )
                }
            }

            // Default Controls Setting Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Default Controls Settings",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Set your default controller bindings applied to new seeds and custom profiles.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    com.maprandroid.ui.config.ControllerBindingRow(
                        actionName = "Shot",
                        selectedButton = defaultControls.shot,
                        onButtonSelected = { viewModel.updateDefaultControl("shot", it) }
                    )
                    com.maprandroid.ui.config.ControllerBindingRow(
                        actionName = "Jump",
                        selectedButton = defaultControls.jump,
                        onButtonSelected = { viewModel.updateDefaultControl("jump", it) }
                    )
                    com.maprandroid.ui.config.ControllerBindingRow(
                        actionName = "Dash",
                        selectedButton = defaultControls.dash,
                        onButtonSelected = { viewModel.updateDefaultControl("dash", it) }
                    )
                    com.maprandroid.ui.config.ControllerBindingRow(
                        actionName = "Item Select",
                        selectedButton = defaultControls.itemSelect,
                        onButtonSelected = { viewModel.updateDefaultControl("itemSelect", it) }
                    )
                    com.maprandroid.ui.config.ControllerBindingRow(
                        actionName = "Item Cancel",
                        selectedButton = defaultControls.itemCancel,
                        onButtonSelected = { viewModel.updateDefaultControl("itemCancel", it) }
                    )
                    com.maprandroid.ui.config.ControllerBindingRow(
                        actionName = "Aim Angle Up",
                        selectedButton = defaultControls.angleUp,
                        onButtonSelected = { viewModel.updateDefaultControl("angleUp", it) }
                    )
                    com.maprandroid.ui.config.ControllerBindingRow(
                        actionName = "Aim Angle Down",
                        selectedButton = defaultControls.angleDown,
                        onButtonSelected = { viewModel.updateDefaultControl("angleDown", it) }
                    )
                }
            }

            // About Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Maprandroid",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Version 1.0.0",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Map Randomizer API: maprando.com",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
