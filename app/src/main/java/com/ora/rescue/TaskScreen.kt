@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class, androidx.compose.foundation.layout.ExperimentalLayoutApi::class)

package com.ora.rescue

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import java.time.LocalDate

@Composable
fun TaskScreen(
    id: Long,
    snapshot: StudySnapshot,
    busy: Boolean,
    onSave: (StudyTask) -> Unit,
    onDelete: (Long) -> Unit,
    onBack: () -> Unit
) {
    val existing = snapshot.tasks.find { it.id == id }
    if (id != 0L && existing == null) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("This study topic is no longer available.", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(16.dp))
            Button(onClick = onBack) { Text("Return to Study Desk") }
        }
        return
    }

    val today = LocalDate.now().toEpochDay()
    var subject by rememberSaveable(id) { mutableStateOf(existing?.subject ?: "") }
    var topic by rememberSaveable(id) { mutableStateOf(existing?.topic ?: "") }
    var minutes by rememberSaveable(id) { mutableStateOf(existing?.totalMinutes?.toString() ?: "") }
    var examDay by rememberSaveable(id) {
        mutableLongStateOf(existing?.examDay ?: LocalDate.now().plusWeeks(2).toEpochDay())
    }
    var priority by rememberSaveable(id) { mutableIntStateOf(existing?.priority ?: 2) }
    var submitted by rememberSaveable(id) { mutableStateOf(false) }
    var showDatePicker by remember { mutableStateOf(false) }
    var showConfirmDelete by remember { mutableStateOf(false) }

    val completed = existing?.let { snapshot.completed(it.id) } ?: 0
    val parsedMinutes = minutes.toIntOrNull()
    val validMinutes = parsedMinutes != null && parsedMinutes in 1..100000 && parsedMinutes >= completed
    val validDate = examDay > today
    val validSubject = subject.isNotBlank()
    val validTopic = topic.isNotBlank()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .imePadding(),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 36.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Eyebrow(if (id == 0L) "A SMALL BEGINNING" else "EDIT STUDY TOPIC")
                Text(
                    text = if (id == 0L) "Make space for a topic." else "Refine your study topic.",
                    style = MaterialTheme.typography.headlineMedium,
                    fontFamily = OraFontFamily,
                    fontWeight = FontWeight.Bold,
                    color = OraPlumInk
                )
                Text(
                    text = "Estimate the work honestly. You can adjust it as you learn.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = OraPlumMuted
                )
            }
        }

        item {
            OutlinedTextField(
                value = subject,
                onValueChange = { subject = it },
                label = { Text("Subject") },
                placeholder = { Text("e.g. Operating Systems, Calculus, Marketing") },
                leadingIcon = {
                    Icon(Icons.AutoMirrored.Rounded.MenuBook, contentDescription = null, tint = OraRose)
                },
                singleLine = true,
                isError = submitted && !validSubject,
                supportingText = {
                    if (submitted && !validSubject) {
                        Text("Please enter a subject name.")
                    } else {
                        Text("Course or subject unit.")
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp)
            )
        }

        item {
            OutlinedTextField(
                value = topic,
                onValueChange = { topic = it },
                label = { Text("Topic name") },
                placeholder = { Text("e.g. CPU Scheduling & Deadlock Prevention") },
                leadingIcon = {
                    Icon(Icons.Rounded.EditNote, contentDescription = null, tint = OraRose)
                },
                minLines = 2,
                isError = submitted && !validTopic,
                supportingText = {
                    if (submitted && !validTopic) {
                        Text("Please enter a specific topic or chapter.")
                    } else {
                        Text("The specific concept or material to review.")
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp)
            )
        }

        item {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    "Exam deadline",
                    fontWeight = FontWeight.SemiBold,
                    style = MaterialTheme.typography.bodyMedium,
                    color = OraPlumInk
                )
                OutlinedButton(
                    onClick = { showDatePicker = true },
                    modifier = Modifier
                        .fillMaxWidth()
                        .defaultMinSize(minHeight = 50.dp),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Icon(Icons.Rounded.Event, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(10.dp))
                    Text(
                        text = "Exam · ${dateWithYear(examDay)} (${daysRemainingLabel(examDay, today)})",
                        fontWeight = FontWeight.Medium
                    )
                }
                if (submitted && !validDate) {
                    Text(
                        "Exam date must be after today.",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(start = 12.dp)
                    )
                } else {
                    Text(
                        "Ora only schedules recovery sessions before this date.",
                        style = MaterialTheme.typography.bodySmall,
                        color = OraPlumMuted,
                        modifier = Modifier.padding(start = 12.dp)
                    )
                }
            }
        }

        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = minutes,
                    onValueChange = { if (it.length <= 6) minutes = it },
                    label = { Text("Total estimated study minutes") },
                    leadingIcon = {
                        Icon(Icons.Rounded.Timer, contentDescription = null, tint = OraRose)
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    isError = submitted && !validMinutes,
                    supportingText = {
                        Text(
                            if (submitted && !validMinutes) {
                                "Enter 1–100,000 minutes (at least $completed minutes already completed)."
                            } else {
                                "Total estimated time for this topic, including completed work."
                            }
                        )
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp)
                )

                // Quick duration helper chips
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    listOf(30, 45, 60, 90, 120).forEach { addMins ->
                        FilterChip(
                            selected = false,
                            onClick = {
                                val current = minutes.toIntOrNull() ?: 0
                                minutes = if (current == 0) addMins.toString() else (current + addMins).toString()
                            },
                            label = { Text("+$addMins m", style = MaterialTheme.typography.labelSmall) }
                        )
                    }
                }
            }
        }

        if (completed > 0) {
            item {
                NoteCard(
                    title = "Your effort stays with you",
                    text = "$completed minutes are already completed for this topic. Total study time cannot be reduced below the work already done.",
                    icon = Icons.Rounded.Verified,
                    containerColor = OraSageLight.copy(alpha = 0.8f),
                    titleColor = OraSage
                )
            }
        }

        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    "Priority level",
                    fontWeight = FontWeight.SemiBold,
                    style = MaterialTheme.typography.bodyMedium,
                    color = OraPlumInk
                )
                Text(
                    "Higher priority topics are scheduled first when available study time is limited.",
                    style = MaterialTheme.typography.bodySmall,
                    color = OraPlumMuted
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    for (level in listOf(1, 2, 3)) {
                        val isSelected = priority == level
                        OutlinedCard(
                            onClick = { priority = level },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.outlinedCardColors(
                                containerColor = if (isSelected) OraRoseLight else Color.White
                            ),
                            border = CardDefaults.outlinedCardBorder(enabled = true).let { border ->
                                if (isSelected) androidx.compose.foundation.BorderStroke(2.dp, OraRose) else border
                            }
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 12.dp, horizontal = 8.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                RadioButton(
                                    selected = isSelected,
                                    onClick = { priority = level },
                                    colors = RadioButtonDefaults.colors(selectedColor = OraRose)
                                )
                                Text(
                                    text = priorityLabel(level),
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = if (isSelected) OraRose else OraPlumInk
                                )
                            }
                        }
                    }
                }
            }
        }

        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = {
                        submitted = true
                        if (validSubject && validTopic && validDate && validMinutes) {
                            val nonNullMinutes = parsedMinutes ?: return@Button
                            onSave(
                                StudyTask(
                                    id = id,
                                    subject = subject.trim(),
                                    topic = topic.trim(),
                                    examDay = examDay,
                                    totalMinutes = nonNullMinutes,
                                    priority = priority
                                )
                            )
                        }
                    },
                    enabled = !busy,
                    modifier = Modifier
                        .fillMaxWidth()
                        .defaultMinSize(minHeight = 52.dp),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Icon(Icons.Rounded.Save, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(if (id == 0L) "Save Topic" else "Update Topic")
                }

                Text(
                    "Saving a topic clears future pending allocations. Preview your recovery plan afterwards to update your schedule.",
                    style = MaterialTheme.typography.bodySmall,
                    color = OraPlumMuted,
                    modifier = Modifier.padding(horizontal = 4.dp)
                )
            }
        }

        if (id != 0L) {
            item {
                OutlinedButton(
                    onClick = { showConfirmDelete = true },
                    enabled = !busy,
                    modifier = Modifier
                        .fillMaxWidth()
                        .defaultMinSize(minHeight = 48.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Icon(Icons.Rounded.DeleteOutline, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Delete topic and its history")
                }
            }
        }
    }

    if (showDatePicker) {
        ChooseDateDialog(
            initialDay = examDay,
            minDay = today + 1,
            onDismiss = { showDatePicker = false },
            onChoose = {
                examDay = it
                showDatePicker = false
            }
        )
    }

    if (showConfirmDelete) {
        AlertDialog(
            onDismissRequest = { showConfirmDelete = false },
            icon = { Icon(Icons.Rounded.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
            title = { Text("Delete '$topic'?", fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    "This topic and all its scheduled sessions and progress history will be permanently deleted from the local database. The future plan will need a fresh preview. This action cannot be undone."
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showConfirmDelete = false
                        onDelete(id)
                    },
                    enabled = !busy,
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Delete Permanently")
                }
            },
            dismissButton = {
                TextButton(onClick = { showConfirmDelete = false }) {
                    Text("Keep Topic")
                }
            }
        )
    }
}

