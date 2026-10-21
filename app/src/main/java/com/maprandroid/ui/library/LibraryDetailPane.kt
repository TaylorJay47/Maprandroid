package com.maprandroid.ui.library

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Assignment
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.maprandroid.data.local.ConfigProfileJsonAdapter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryDetailPane(
    gameId: Int,
    viewModel: LibraryViewModel,
    onPlayClick: (Int, String) -> Unit,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()
    val game = uiState.games.find { it.id == gameId }

    var showDeleteDialog by remember { mutableStateOf(false) }
    var expandAllCategories by remember { mutableStateOf(true) }

    if (game == null) {
        Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Game not found.")
        }
        return
    }

    val profile = remember(game.configProfileJson, game.skillAssumptions, game.mapLayout) {
        ConfigProfileJsonAdapter.fromJson(game.configProfileJson, game.skillAssumptions, game.mapLayout)
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
            // Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                IconButton(onClick = onBackClick) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                }
                Text(
                    text = game.title,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
            }

            // Compact Action Buttons Row at the top
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(
                    onClick = { onPlayClick(game.id, game.romPath) },
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(horizontal = 4.dp, vertical = 8.dp)
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Play", style = MaterialTheme.typography.labelLarge)
                }

                OutlinedButton(
                    onClick = { viewModel.toggleGameCompleted(game.id) },
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(horizontal = 4.dp, vertical = 8.dp)
                ) {
                    Icon(
                        imageVector = if (game.isCompleted) Icons.Default.CheckCircle else Icons.Default.Check,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (game.isCompleted) "Completed" else "Complete",
                        style = MaterialTheme.typography.labelLarge
                    )
                }

                OutlinedButton(
                    onClick = { showDeleteDialog = true },
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(horizontal = 4.dp, vertical = 8.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Delete", style = MaterialTheme.typography.labelLarge)
                }
            }

            // Overview Card
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Overview & Location", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text("ROM Path:", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(game.romPath, style = MaterialTheme.typography.bodySmall)
                    HorizontalDivider()
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Created:", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(formatDate(game.createdAt), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                    }
                }
            }

            // Categories Section Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Configuration Details (10 Categories)",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                TextButton(onClick = { expandAllCategories = !expandAllCategories }) {
                    Text(if (expandAllCategories) "Collapse All" else "Expand All")
                }
            }

            // 1. Skill Assumptions
            CategoryDetailCard(
                title = "1. Skill Assumptions",
                icon = Icons.Default.Psychology,
                isExpanded = expandAllCategories,
                items = listOf(
                    "Logic Skill Level" to profile.logicSkill,
                    "Logic Options" to profile.logicOptions
                )
            )

            // 2. Item Progression
            CategoryDetailCard(
                title = "2. Item Progression & Pool",
                icon = Icons.Default.CardGiftcard,
                isExpanded = expandAllCategories,
                items = listOf(
                    "Progression Speed" to profile.itemProgression,
                    "Progression Rate" to profile.progressionRate,
                    "Placement Style" to profile.itemPlacementStyle,
                    "Item Pool Mode" to profile.itemPool,
                    "Major/Minor Distribution" to profile.majorMinorDistribution,
                    "Scavenger Hunt Mode" to if (profile.scavengerHunt) "Enabled (${profile.scavengerRequiredCount} Items)" else "Disabled",
                    "Chozo Hunt Mode" to formatBool(profile.chozoHunt),
                    "Super Missile Count" to "${profile.superMissileCount}",
                    "Power Bomb Count" to "${profile.powerBombCount}",
                    "Starting Gear" to listOfNotNull(
                        if (profile.startMorph) "Morph" else null,
                        if (profile.startCharge) "Charge" else null,
                        if (profile.startVaria) "Varia" else null
                    ).ifEmpty { listOf("None") }.joinToString(", ")
                )
            )

            // 3. Quality of Life Options
            CategoryDetailCard(
                title = "3. Quality of Life & Visuals",
                icon = Icons.Default.Speed,
                isExpanded = expandAllCategories,
                items = listOf(
                    "QoL Preset" to profile.qolPreset,
                    "Fast Elevators" to formatBool(profile.qolFastElevators),
                    "Fast Doors" to formatBool(profile.qolFastDoors),
                    "Fast Text" to formatBool(profile.qolFastText),
                    "Cutscene Skips" to formatBool(profile.qolCutsceneSkip),
                    "Quick Morph" to formatBool(profile.qolQuickMorph),
                    "Spin Jump Restart" to formatBool(profile.qolSpinJumpRestart),
                    "Auto-Charge Beam" to formatBool(profile.qolAutoChargeBeam),
                    "Item Markers" to profile.qolItemMarkers,
                    "Mother Brain Fight" to profile.qolMotherBrainFight
                )
            )

            // 4. Objectives
            CategoryDetailCard(
                title = "4. Objectives",
                icon = Icons.AutoMirrored.Filled.Assignment,
                isExpanded = expandAllCategories,
                items = listOf(
                    "Objective Preset" to profile.objectivePreset,
                    "Required Bosses" to listOfNotNull(
                        if (profile.kraidObjective) "Kraid" else null,
                        if (profile.phantoonObjective) "Phantoon" else null,
                        if (profile.draygonObjective) "Draygon" else null,
                        if (profile.ridleyObjective) "Ridley" else null
                    ).ifEmpty { listOf("None") }.joinToString(", "),
                    "Objectives Required Count" to "${profile.minObjectives} - ${profile.maxObjectives}"
                )
            )

            // 5. Map Layout
            CategoryDetailCard(
                title = "5. Map & Environment",
                icon = Icons.Default.Place,
                isExpanded = expandAllCategories,
                items = listOf(
                    "Area Randomization" to profile.mapRandomization,
                    "Boss Randomization" to profile.bossRandomization,
                    "Door Lock Randomization" to profile.doorLockRandomization,
                    "Elevator Randomization" to formatBool(profile.elevatorRando),
                    "Escape Randomization" to formatBool(profile.escapeRando)
                )
            )

            // 6. Doors
            CategoryDetailCard(
                title = "6. Doors Settings",
                icon = Icons.Default.Lock,
                isExpanded = expandAllCategories,
                items = listOf(
                    "Doors Preset" to profile.doorsPreset,
                    "Red Doors Count" to "${profile.redDoorsCount}",
                    "Green Doors Count" to "${profile.greenDoorsCount}",
                    "Yellow Doors Count" to "${profile.yellowDoorsCount}"
                )
            )

            // 7. Start Location
            CategoryDetailCard(
                title = "7. Start Location",
                icon = Icons.Default.Navigation,
                isExpanded = expandAllCategories,
                items = listOf(
                    "Start Location Mode" to profile.startLocationMode
                )
            )

            // 8. Save the Animals
            CategoryDetailCard(
                title = "8. Save the Animals",
                icon = Icons.Default.Favorite,
                isExpanded = expandAllCategories,
                items = listOf(
                    "Save Animals Requirement" to profile.saveAnimals
                )
            )

            // 9. Other Options
            CategoryDetailCard(
                title = "9. Other Options & Tuning",
                icon = Icons.Default.Tune,
                isExpanded = expandAllCategories,
                items = listOf(
                    "Wall Jump Setting" to profile.wallJumpSetting,
                    "Speed Booster Setting" to profile.speedBoosterSetting,
                    "Area Assignment Order" to profile.areaAssignmentBaseOrder,
                    "Ship in Crateria" to formatBool(profile.shipInCrateria),
                    "Mother Brain in Tourian" to formatBool(profile.motherBrainInTourian),
                    "Savestate System" to profile.savestate,
                    "Race Mode" to formatBool(profile.raceMode),
                    "Major Glitches" to formatBool(profile.enableMajorGlitches)
                )
            )

            // 10. Controls, Quick Reload & Cosmetics
            val quickReloadActive = listOfNotNull(
                if (profile.quickReloadL) "L" else null,
                if (profile.quickReloadR) "R" else null,
                if (profile.quickReloadSelect) "Select" else null,
                if (profile.quickReloadStart) "Start" else null
            ).joinToString("+").ifEmpty { "None" }

            val spinLockActive = listOfNotNull(
                if (profile.spinLockX) "X" else null,
                if (profile.spinLockL) "L" else null,
                if (profile.spinLockR) "R" else null,
                if (profile.spinLockUp) "Up" else null
            ).joinToString(", ").ifEmpty { "None" }

            CategoryDetailCard(
                title = "10. Controls, Quick Reload & Cosmetics",
                icon = Icons.Default.SportsEsports,
                isExpanded = expandAllCategories,
                items = listOf(
                    "Controls (Shot/Jump/Dash)" to "${profile.controlShot} / ${profile.controlJump} / ${profile.controlDash}",
                    "Item Select / Cancel" to "${profile.controlItemSelect} / ${profile.controlItemCancel}",
                    "Aim Angle Up / Down" to "${profile.controlAngleUp} / ${profile.controlAngleDown}",
                    "Quick Reload Combination" to quickReloadActive,
                    "Spin Lock Buttons" to spinLockActive,
                    "Moonwalk Mode" to formatBool(profile.moonwalk),
                    "Samus Sprite" to profile.samusSprite,
                    "E-Tank Color" to "#${profile.etankColor}",
                    "Map Theme / Palettes" to "${profile.mapTheme} / ${profile.roomPalettes}",
                    "Music / Low Health Beep" to "${profile.music} / Beep: ${if (profile.disableBeeping) "Disabled" else "Enabled"}"
                )
            )
        }
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete ROM?") },
            text = { Text("Are you sure you want to delete '${game.title}'? This will remove the game record and delete the ROM file from your device storage.") },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteDialog = false
                        viewModel.deleteGame(context, game)
                        onBackClick()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showDeleteDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun CategoryDetailCard(
    title: String,
    icon: ImageVector,
    isExpanded: Boolean,
    items: List<Pair<String, String>>
) {
    var expandedState by remember { mutableStateOf(isExpanded) }

    LaunchedEffect(isExpanded) {
        expandedState = isExpanded
    }

    OutlinedCard(
        modifier = Modifier.fillMaxWidth(),
        onClick = { expandedState = !expandedState }
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                    Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                }
                Icon(
                    imageVector = if (expandedState) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = if (expandedState) "Collapse" else "Expand",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (expandedState) {
                Spacer(modifier = Modifier.height(8.dp))
                HorizontalDivider()
                Spacer(modifier = Modifier.height(8.dp))
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    items.forEach { (label, value) ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }
        }
    }
}

private fun formatBool(value: Boolean): String = if (value) "Enabled" else "Disabled"

private fun formatDate(timestamp: Long): String {
    return SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault()).format(Date(timestamp))
}
