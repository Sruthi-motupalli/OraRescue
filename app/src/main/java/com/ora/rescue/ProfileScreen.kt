@file:OptIn(
    androidx.compose.material3.ExperimentalMaterial3Api::class,
    androidx.compose.foundation.layout.ExperimentalLayoutApi::class
)

package com.ora.rescue

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import java.time.LocalDate

val StudentAvatars = listOf("👩🏻‍💻", "🧑🏽‍🎓", "📚", "✨")

@Composable
fun ProfileScreen(
    snapshot: StudySnapshot,
    busy: Boolean,
    themePreference: ThemePreference = ThemePreference.SYSTEM,
    onUpdateProfile: (String, Int, Boolean, Int) -> Unit,
    onSetTheme: (ThemePreference) -> Unit = {},
    onNavigateToCalendar: () -> Unit
) {
    val cs = MaterialTheme.colorScheme
    val accents = LocalOraAccents.current
    val profile = snapshot.profile
    val today = LocalDate.now().toEpochDay()
    val todayDone = snapshot.sessions.filter { it.day == today }.sumOf { it.completedMinutes }
    val totalDoneMinutes = snapshot.sessions.sumOf { it.completedMinutes }
    val doneTopics = snapshot.tasks.count { snapshot.remaining(it) == 0 }
    val openTopics = snapshot.tasks.count { snapshot.remaining(it) > 0 }

    var showEditNameDialog by remember { mutableStateOf(false) }
    var showEditGoalDialog by remember { mutableStateOf(false) }
    var showPreferencesDialog by remember { mutableStateOf(false) }
    var showAboutDialog by remember { mutableStateOf(false) }
    var showAvatarDialog by remember { mutableStateOf(false) }

    val goalPercent = if (profile.studyGoalMinutes > 0) {
        ((todayDone.toFloat() / profile.studyGoalMinutes) * 100).toInt().coerceIn(0, 100)
    } else 0

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 40.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Column {
                Eyebrow("YOUR PERSONAL SPACE")
                Text(
                    text = "My Profile",
                    fontFamily = OraFontFamily,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = cs.onBackground
                )
            }
        }

        // Student Avatar & Name Card
        item {
            OutlinedCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.outlinedCardColors(containerColor = cs.surface)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(22.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Avatar with gradient halo
                    Box(
                        modifier = Modifier
                            .size(96.dp)
                            .clip(CircleShape)
                            .background(
                                accents.avatar
                            )
                            .border(3.dp, cs.surface, CircleShape)
                            .clickable { showAvatarDialog = true },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = StudentAvatars.getOrElse(profile.avatarIndex) { "👩🏻‍💻" },
                            style = MaterialTheme.typography.headlineLarge
                        )
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = profile.name,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = cs.onBackground
                        )
                        IconButton(
                            onClick = { showEditNameDialog = true },
                            modifier = Modifier.size(48.dp)
                        ) {
                            Icon(Icons.Rounded.Edit, contentDescription = "Edit name", tint = cs.primary, modifier = Modifier.size(16.dp))
                        }
                    }

                    Text(
                        text = "One step at a time.",
                        style = MaterialTheme.typography.bodySmall,
                        color = cs.onSurfaceVariant
                    )
                }
            }
        }

        // 3 Real Stat Tiles
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                StatTile(
                    symbol = "✓",
                    symbolColor = cs.onTertiaryContainer,
                    symbolBg = cs.tertiaryContainer,
                    value = "$doneTopics",
                    label = "Topics Done",
                    modifier = Modifier.weight(1f)
                )
                StatTile(
                    symbol = "◷",
                    symbolColor = cs.onSecondaryContainer,
                    symbolBg = cs.secondaryContainer,
                    value = "${totalDoneMinutes / 60} h ${totalDoneMinutes % 60} m",
                    label = "Focus Time",
                    modifier = Modifier.weight(1f)
                )
                StatTile(
                    symbol = "ϟ",
                    symbolColor = cs.onErrorContainer,
                    symbolBg = cs.errorContainer,
                    value = "$openTopics",
                    label = "Open Topics",
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // Daily Study Target Card
        item {
            Surface(
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier.fillMaxWidth(),
                color = Color.Transparent
            ) {
                Box(
                    modifier = Modifier
                        .background(
                            accents.banner
                        )
                        .padding(18.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Icon(Icons.Rounded.Star, contentDescription = null, tint = accents.onBanner, modifier = Modifier.size(16.dp))
                                Text("Your Study Goal", style = MaterialTheme.typography.labelMedium, color = accents.onBanner.copy(alpha = 0.9f))
                            }
                            TextButton(
                                onClick = { showEditGoalDialog = true },
                                colors = ButtonDefaults.textButtonColors(contentColor = accents.onBanner)
                            ) {
                                Text("Adjust Target ›", style = MaterialTheme.typography.labelSmall)
                            }
                        }

                        Text(
                            text = "${profile.studyGoalMinutes} min daily target",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = accents.onBanner
                        )

                        Text(
                            text = if (todayDone >= profile.studyGoalMinutes)
                                "Goal reached for today! Wonderful effort."
                            else if (todayDone > 0)
                                "$todayDone of ${profile.studyGoalMinutes} min completed today ($goalPercent%)."
                            else
                                "Small steps. Stronger habits.",
                            style = MaterialTheme.typography.bodySmall,
                            color = accents.onBanner.copy(alpha = 0.9f)
                        )

                        LinearProgressIndicator(
                            progress = { if (profile.studyGoalMinutes > 0) (todayDone.toFloat() / profile.studyGoalMinutes).coerceIn(0f, 1f) else 0f },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(RoundedCornerShape(3.dp)),
                            color = accents.onBanner,
                            trackColor = accents.onBanner.copy(alpha = 0.3f)
                        )
                    }
                }
            }
        }

        // Appearance Selector Card
        item {
            OutlinedCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.outlinedCardColors(containerColor = cs.surface)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Rounded.Palette, contentDescription = null, tint = cs.primary, modifier = Modifier.size(20.dp))
                        Text("Appearance", fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.titleSmall, color = cs.onBackground)
                    }
                    Text(
                        "Choose how Ora Rescue looks on this device.",
                        style = MaterialTheme.typography.bodySmall,
                        color = cs.onSurfaceVariant
                    )
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        ThemePreference.entries.forEach { pref ->
                            val selected = themePreference == pref
                            val label = when (pref) {
                                ThemePreference.LIGHT  -> "Light"
                                ThemePreference.DARK   -> "Dark"
                                ThemePreference.SYSTEM -> "System"
                            }
                            val icon = when (pref) {
                                ThemePreference.LIGHT  -> Icons.Rounded.LightMode
                                ThemePreference.DARK   -> Icons.Rounded.DarkMode
                                ThemePreference.SYSTEM -> Icons.Rounded.SettingsBrightness
                            }
                            FilterChip(
                                selected = selected,
                                onClick = { onSetTheme(pref) },
                                label = { Text(label, style = MaterialTheme.typography.labelMedium) },
                                leadingIcon = {
                                    Icon(icon, contentDescription = null, modifier = Modifier.size(16.dp))
                                },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = cs.primaryContainer,
                                    selectedLabelColor = cs.onPrimaryContainer,
                                    selectedLeadingIconColor = cs.onPrimaryContainer
                                )
                            )
                        }
                    }
                }
            }
        }

        // Settings Menu Card
        item {
            OutlinedCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.outlinedCardColors(containerColor = cs.surface)
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    ProfileMenuItem(
                        icon = Icons.Rounded.Tune,
                        title = "Study Preferences",
                        subtitle = if (profile.preferredSplit) "Default: Split topics across days" else "Default: Fit into one continuous window",
                        onClick = { showPreferencesDialog = true }
                    )
                    HorizontalDivider(color = cs.outlineVariant)
                    ProfileMenuItem(
                        icon = Icons.Rounded.CalendarToday,
                        title = "Daily Availability",
                        subtitle = "Open calendar to configure free-time windows",
                        onClick = onNavigateToCalendar
                    )
                    HorizontalDivider(color = cs.outlineVariant)
                    ProfileMenuItem(
                        icon = Icons.Rounded.Info,
                        title = "About Ora Rescue",
                        subtitle = "100% on-device SQLite Room database",
                        onClick = { showAboutDialog = true }
                    )
                }
            }
        }

        // Privacy Footer
        item {
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Rounded.Lock, contentDescription = null, tint = cs.onSurfaceVariant, modifier = Modifier.size(14.dp))
                Spacer(Modifier.width(6.dp))
                Text(
                    text = "Your study data stays securely on this device.",
                    style = MaterialTheme.typography.bodySmall,
                    color = cs.onSurfaceVariant
                )
            }
        }
    }

    // Edit Name Dialog
    if (showEditNameDialog) {
        var newName by rememberSaveable { mutableStateOf(profile.name) }
        val validName = newName.isNotBlank() && newName.trim().length <= 40
        AlertDialog(
            onDismissRequest = { showEditNameDialog = false },
            title = { Text("Edit Student Name", fontWeight = FontWeight.Bold) },
            text = {
                OutlinedTextField(
                    value = newName,
                    onValueChange = { if (it.length <= 50) newName = it },
                    label = { Text("Display name") },
                    isError = !validName,
                    supportingText = { if (!validName) Text("Enter a name with 1 to 40 characters.") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    enabled = validName && !busy,
                    onClick = {
                        onUpdateProfile(newName.trim(), profile.studyGoalMinutes, profile.preferredSplit, profile.avatarIndex)
                        showEditNameDialog = false
                    }
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditNameDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Edit Goal Dialog
    if (showEditGoalDialog) {
        val cs2 = MaterialTheme.colorScheme
        var goalText by rememberSaveable { mutableStateOf(profile.studyGoalMinutes.toString()) }
        val goal = goalText.toIntOrNull()
        val validGoal = goal != null && goal in 15..1440
        AlertDialog(
            onDismissRequest = { showEditGoalDialog = false },
            title = { Text("Daily Study Target", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Set your daily study target in minutes (e.g. 60, 120, 180).", style = MaterialTheme.typography.bodySmall, color = cs2.onSurfaceVariant)
                    OutlinedTextField(
                        value = goalText,
                        onValueChange = { if (it.length <= 4) goalText = it },
                        label = { Text("Daily minutes") },
                        isError = !validGoal,
                        supportingText = { if (!validGoal) Text("Enter 15 to 1,440 minutes.") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(60, 90, 120, 180, 240).forEach { mins ->
                            FilterChip(
                                selected = goalText == mins.toString(),
                                onClick = { goalText = mins.toString() },
                                label = { Text("${mins}m", style = MaterialTheme.typography.labelSmall) }
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    enabled = validGoal && !busy,
                    onClick = {
                        val minutes = goal ?: return@Button
                        onUpdateProfile(profile.name, minutes, profile.preferredSplit, profile.avatarIndex)
                        showEditGoalDialog = false
                    }
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditGoalDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Preferences Dialog
    if (showPreferencesDialog) {
        val cs2 = MaterialTheme.colorScheme
        var defaultSplit by rememberSaveable { mutableStateOf(profile.preferredSplit) }
        AlertDialog(
            onDismissRequest = { showPreferencesDialog = false },
            title = { Text("Study Preferences", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Split longer topics by default", fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodyMedium)
                            Text("When enabled, recovery plans spread topics across multiple study days if needed.", style = MaterialTheme.typography.bodySmall, color = cs2.onSurfaceVariant)
                        }
                        Switch(checked = defaultSplit, onCheckedChange = { defaultSplit = it })
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onUpdateProfile(profile.name, profile.studyGoalMinutes, defaultSplit, profile.avatarIndex)
                        showPreferencesDialog = false
                    }
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { showPreferencesDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Avatar Picker Dialog
    if (showAvatarDialog) {
        val cs2 = MaterialTheme.colorScheme
        AlertDialog(
            onDismissRequest = { showAvatarDialog = false },
            title = { Text("Choose Avatar", fontWeight = FontWeight.Bold) },
            text = {
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    StudentAvatars.forEachIndexed { index, emoji ->
                        Box(
                            modifier = Modifier
                                .size(54.dp)
                                .clip(CircleShape)
                                .background(
                                    if (profile.avatarIndex == index) cs2.primaryContainer else Color.Transparent
                                )
                                .border(
                                    if (profile.avatarIndex == index) 2.dp else 1.dp,
                                    if (profile.avatarIndex == index) cs2.primary else cs2.outlineVariant,
                                    CircleShape
                                )
                                .clickable {
                                    onUpdateProfile(profile.name, profile.studyGoalMinutes, profile.preferredSplit, index)
                                    showAvatarDialog = false
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(emoji, style = MaterialTheme.typography.headlineMedium)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showAvatarDialog = false }) {
                    Text("Close")
                }
            }
        )
    }

    // About Dialog
    if (showAboutDialog) {
        val cs2 = MaterialTheme.colorScheme
        AlertDialog(
            onDismissRequest = { showAboutDialog = false },
            icon = { Icon(Icons.Rounded.School, contentDescription = null, tint = cs2.primary) },
            title = { Text("About Ora Rescue", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Version 2.0 (Room Schema v2)", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                    Text(
                        "Ora Rescue is a student study recovery planner. It deterministically rearranges unfinished study topics before upcoming exam deadlines based on configured free-time windows.",
                        style = MaterialTheme.typography.bodySmall
                    )
                    HorizontalDivider(color = cs2.outlineVariant)
                    Text(
                        "• 100% On-Device: All data is stored in your device's local SQLite Room database.\n• Privacy First: No accounts, no telemetry, no cloud transmission.\n• Deterministic EDF Scheduling: Prioritizes nearest deadlines and highest priority topics.",
                        style = MaterialTheme.typography.bodySmall,
                        color = cs2.onSurfaceVariant
                    )
                }
            },
            confirmButton = {
                Button(onClick = { showAboutDialog = false }) {
                    Text("Got It")
                }
            }
        )
    }
}

@Composable
private fun StatTile(
    symbol: String,
    symbolColor: Color,
    symbolBg: Color,
    value: String,
    label: String,
    modifier: Modifier = Modifier
) {
    val cs = MaterialTheme.colorScheme
    Surface(
        modifier = modifier,
        color = cs.surfaceVariant,
        shape = RoundedCornerShape(18.dp)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Surface(
                shape = CircleShape,
                color = symbolBg,
                modifier = Modifier.size(28.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(symbol, color = symbolColor, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelMedium)
                }
            }
            Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = cs.onBackground)
            Text(label, color = cs.onSurfaceVariant, style = MaterialTheme.typography.labelSmall)
        }
    }
}

@Composable
private fun ProfileMenuItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    trailingText: String? = null,
    onClick: () -> Unit
) {
    val cs = MaterialTheme.colorScheme
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 18.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            modifier = Modifier.weight(1f)
        ) {
            Icon(icon, contentDescription = null, tint = cs.primary, modifier = Modifier.size(22.dp))
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(title, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodyMedium, color = cs.onBackground)
                Text(subtitle, style = MaterialTheme.typography.labelSmall, color = cs.onSurfaceVariant)
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            if (trailingText != null) {
                Text(trailingText, style = MaterialTheme.typography.labelSmall, color = cs.onSurfaceVariant)
            }
            Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, contentDescription = null, tint = cs.outline)
        }
    }
}
