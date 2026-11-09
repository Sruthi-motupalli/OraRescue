@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.ora.rescue

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.MenuBook
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import java.time.LocalDate

@Composable
fun HomeScreen(
    snapshot: StudySnapshot,
    busy: Boolean,
    onAdd: () -> Unit,
    onEdit: (Long) -> Unit,
    onRecover: () -> Unit,
    onAvailability: (Long, Int) -> Unit,
    onProgress: (Long, Int) -> Unit
) {
    var selectedDay by rememberSaveable { mutableLongStateOf(LocalDate.now().toEpochDay()) }
    var tab by rememberSaveable { mutableIntStateOf(0) }
    var showDatePicker by remember { mutableStateOf(false) }
    var showAvailabilityDialog by remember { mutableStateOf(false) }
    var progressSession by remember { mutableStateOf<StudySession?>(null) }

    val today = LocalDate.now().toEpochDay()
    val daySessions = snapshot.sessions.filter { it.day == selectedDay }
    val dayCapacity = snapshot.availability.find { it.day == selectedDay }?.minutes ?: 0
    val worked = daySessions.sumOf { it.completedMinutes }
    val planned = daySessions.filter { it.status == StudySession.PENDING }.sumOf { it.allocatedMinutes }
    val openTasks = snapshot.tasks.count { snapshot.remaining(it) > 0 }
    val outstanding = snapshot.tasks.sumOf { snapshot.remaining(it) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Eyebrow("YOUR STUDY DESK")
                    Text(
                        text = "One step at a time.",
                        fontFamily = FontFamily.Serif,
                        style = MaterialTheme.typography.headlineMedium,
                        color = OraPlumInk
                    )
                }
                FilledTonalIconButton(
                    onClick = onAdd,
                    enabled = !busy,
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(Icons.Rounded.Add, contentDescription = "Add study topic", tint = OraRose)
                }
            }
        }

        item {
            DeskSummaryBanner(
                planned = planned,
                worked = worked,
                capacity = dayCapacity,
                openTasks = openTasks,
                outstanding = outstanding
            )
        }

        item {
            DaySelector(
                selectedDay = selectedDay,
                today = today,
                onPrevious = { selectedDay-- },
                onNext = { selectedDay++ },
                onOpenDatePicker = { showDatePicker = true },
                onJumpToToday = { selectedDay = today }
            )
        }

        item {
            val isPast = selectedDay < today
            OutlinedButton(
                onClick = { showAvailabilityDialog = true },
                enabled = !busy && !isPast,
                modifier = Modifier
                    .fillMaxWidth()
                    .defaultMinSize(minHeight = 48.dp),
                shape = RoundedCornerShape(14.dp)
            ) {
                Icon(Icons.Rounded.Schedule, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text(
                    text = when {
                        isPast -> "Past date · $dayCapacity min was allocated"
                        dayCapacity == 0 -> "Set available study time (currently 0 min)"
                        else -> "Edit availability · $dayCapacity min configured"
                    }
                )
            }
        }

        item {
            TabRow(
                selectedTabIndex = tab,
                containerColor = Color.Transparent,
                divider = { HorizontalDivider(color = OraBorderLight) }
            ) {
                Tab(
                    selected = tab == 0,
                    onClick = { tab = 0 },
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Icon(Icons.Rounded.CalendarMonth, contentDescription = null, modifier = Modifier.size(16.dp))
                            Text("Daily Plan (${daySessions.size})")
                        }
                    }
                )
                Tab(
                    selected = tab == 1,
                    onClick = { tab = 1 },
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Icon(Icons.AutoMirrored.Rounded.MenuBook, contentDescription = null, modifier = Modifier.size(16.dp))
                            Text("All Topics (${snapshot.tasks.size})")
                        }
                    }
                )
            }
        }

        if (snapshot.tasks.isEmpty()) {
            item {
                EmptyStateCard(
                    icon = Icons.AutoMirrored.Rounded.MenuBook,
                    title = "A fresh page awaits",
                    description = "Add your first study topic, set your exam deadline, and define your available study time.",
                    actionText = "Add Your First Topic",
                    onAction = onAdd
                )
            }
        } else if (tab == 0) {
            if (daySessions.isEmpty()) {
                item {
                    if (dayCapacity == 0) {
                        EmptyStateCard(
                            icon = Icons.Rounded.Schedule,
                            title = "No study hours set for this day",
                            description = if (selectedDay >= today)
                                "Set how many minutes you want to study on ${dateLabel(selectedDay)}, or enjoy a restful day off. Once set, generate a recovery preview."
                            else "No study sessions were scheduled for this day.",
                            actionText = if (selectedDay >= today) "Set Available Time" else null,
                            onAction = { showAvailabilityDialog = true }
                        )
                    } else {
                        EmptyStateCard(
                            icon = Icons.Rounded.AutoAwesome,
                            title = "Ready for study recovery",
                            description = "You have $dayCapacity min available on ${dateLabel(selectedDay)}. Open Recover Plan to preview and assign your study topics.",
                            actionText = "Preview Recovery Plan",
                            onAction = onRecover
                        )
                    }
                }
            } else {
                items(daySessions, key = { "session-${it.id}" }) { session ->
                    val task = snapshot.tasks.find { it.id == session.taskId }
                    if (task != null) {
                        SessionCard(
                            task = task,
                            session = session,
                            busy = busy,
                            canRecord = selectedDay <= today,
                            onEdit = { onEdit(task.id) },
                            onProgress = { progressSession = session }
                        )
                    }
                }

                if (openTasks > 0) {
                    item {
                        Button(
                            onClick = onRecover,
                            enabled = !busy,
                            modifier = Modifier
                                .fillMaxWidth()
                                .defaultMinSize(minHeight = 50.dp)
                        ) {
                            Icon(Icons.Rounded.AutoAwesome, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Preview a recovery plan")
                        }
                    }
                }
            }
        } else {
            // All Topics Tab
            items(snapshot.tasks, key = { "task-${it.id}" }) { task ->
                TopicCard(
                    task = task,
                    remaining = snapshot.remaining(task),
                    completed = snapshot.completed(task.id),
                    today = today,
                    onClick = { onEdit(task.id) }
                )
            }

            item {
                OutlinedButton(
                    onClick = onAdd,
                    enabled = !busy,
                    modifier = Modifier
                        .fillMaxWidth()
                        .defaultMinSize(minHeight = 48.dp)
                ) {
                    Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Add another study topic")
                }
            }
        }

        item {
            Text(
                text = "Small steps count. Your progress stays securely on this device.",
                style = MaterialTheme.typography.bodySmall,
                color = OraPlumMuted,
                modifier = Modifier.padding(top = 8.dp)
            )
        }
    }

    if (showDatePicker) {
        ChooseDateDialog(
            initialDay = selectedDay,
            onDismiss = { showDatePicker = false },
            onChoose = {
                selectedDay = it
                showDatePicker = false
            }
        )
    }

    if (showAvailabilityDialog) {
        MinutesDialog(
            title = "Study Time for ${dateLabel(selectedDay)}",
            initial = dayCapacity,
            max = 1440,
            explanation = "Set a realistic daily study target. 0 minutes indicates a day off. Note: Saving availability clears the future uncompleted plan so you can preview a fresh schedule.",
            quickPresets = listOf(
                "Day off (0m)" to 0,
                "1h (60m)" to 60,
                "2h (120m)" to 120,
                "3h (180m)" to 180,
                "4h (240m)" to 240
            ),
            onDismiss = { showAvailabilityDialog = false },
            onSave = {
                onAvailability(selectedDay, it)
                showAvailabilityDialog = false
            }
        )
    }

    progressSession?.let { session ->
        val task = snapshot.tasks.find { it.id == session.taskId }
        val remaining = task?.let { snapshot.remaining(it) } ?: 0
        val maxRecordable = minOf(session.allocatedMinutes, remaining)

        MinutesDialog(
            title = "Record Study Progress",
            initial = maxRecordable,
            max = maxRecordable,
            explanation = "Enter completed minutes for '${task?.topic ?: "this session"}'. Enter 0 for a missed session. Each session can be recorded once; recovery will reschedule any remaining unfinished work.",
            quickPresets = listOfNotNull(
                "Missed (0m)" to 0,
                if (maxRecordable >= 30) "Half (${maxRecordable / 2}m)" to (maxRecordable / 2) else null,
                "Full (${maxRecordable}m)" to maxRecordable
            ),
            onDismiss = { progressSession = null },
            onSave = {
                onProgress(session.id, it)
                progressSession = null
            }
        )
    }
}

