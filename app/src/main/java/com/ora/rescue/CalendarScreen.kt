@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class, androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
package com.ora.rescue

import androidx.compose.foundation.background
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import java.time.YearMonth
import java.time.format.DateTimeFormatter

@Composable
fun CalendarScreen(
    snapshot: StudySnapshot, busy: Boolean,
    onAddTimeWindow: (Long, Int, Int) -> Unit,
    onEditTimeWindow: (Long, Long, Int, Int) -> Unit,
    onDeleteTimeWindow: (Long) -> Unit,
    onClearDayWindows: (Long) -> Unit,
    onCopyWindows: (Long, List<Long>) -> Unit,
    onProgress: (Long, Int) -> Unit,
    onOpenTopic: (Long) -> Unit,
    onNavigateToRecovery: () -> Unit
) {
    val cs = MaterialTheme.colorScheme
    val accents = LocalOraAccents.current
    val clock by rememberStudyClock()
    val today = clock.toLocalDate().toEpochDay()
    var selected by rememberSaveable { mutableLongStateOf(today) }
    // Store an ISO string: YearMonth itself cannot be saved in an Android Bundle.
    var monthText by rememberSaveable { mutableStateOf(YearMonth.now().toString()) }
    val month = YearMonth.parse(monthText)
    val windows = snapshot.timeWindows.filter { it.day == selected }.sortedBy { it.startMinute }
    val sessions = snapshot.sessions.filter { it.day == selected }.sortedBy { it.startMinute }
    val exams = snapshot.tasks.filter { it.examDay == selected }
    val completed = sessions.sumOf { it.completedMinutes }
    val planned = sessions.filter { it.status == StudySession.PENDING }.sumOf { it.allocatedMinutes }
    var editing by remember { mutableStateOf<TimeWindow?>(null) }
    var editor by remember { mutableStateOf(false) }
    var copying by remember { mutableStateOf(false) }
    var dayOff by remember { mutableStateOf(false) }
    var recording by remember { mutableStateOf<StudySession?>(null) }
    val now = clock.hour * 60 + clock.minute
    val past = selected < today

    LazyColumn(contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item { Column {
            Eyebrow("YOUR STUDY SCHEDULE")
            Text("Calendar", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            Text("Choose when you have room to recover.", color = cs.onSurfaceVariant)
        } }
        item { Surface(shape = RoundedCornerShape(24.dp), color = cs.surface) {
            Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                    IconButton(onClick = { monthText = month.minusMonths(1).toString() }) { Icon(Icons.Rounded.ChevronLeft, "Previous month") }
                    Text(month.format(DateTimeFormatter.ofPattern("MMMM yyyy")), fontWeight = FontWeight.Bold)
                    IconButton(onClick = { monthText = month.plusMonths(1).toString() }) { Icon(Icons.Rounded.ChevronRight, "Next month") }
                }
                Row { listOf("M","T","W","T","F","S","S").forEach { label -> Box(Modifier.weight(1f), contentAlignment = Alignment.Center) { Text(label, color = cs.onSurfaceVariant, style = MaterialTheme.typography.labelSmall) } } }
                val offset = month.atDay(1).dayOfWeek.value - 1
                repeat((offset + month.lengthOfMonth() + 6) / 7) { week -> Row {
                    repeat(7) { col ->
                        val num = week * 7 + col - offset + 1
                        if (num !in 1..month.lengthOfMonth()) Spacer(Modifier.weight(1f).height(48.dp))
                        else {
                            val d = month.atDay(num).toEpochDay()
                            val free = snapshot.timeWindows.any { it.day == d }
                            val exam = snapshot.tasks.any { it.examDay == d }
                            val planned = snapshot.sessions.any { it.day == d }
                            Column(Modifier.weight(1f).height(48.dp)
                                .background(if (d == selected) cs.primary else if (d == today) cs.primaryContainer else Color.Transparent, CircleShape)
                                .semantics { contentDescription = "${dateLabel(d)}${if (free) ", free time" else ""}${if (exam) ", exam" else ""}${if (planned) ", study sessions" else ""}" }
                                .selectable(selected = d == selected, role = Role.Button, onClick = { selected = d }), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                                Text("$num", color = if (d == selected) cs.onPrimary else cs.onSurface, style = MaterialTheme.typography.bodySmall)
                                Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                                    if (free) Box(Modifier.size(4.dp).background(if (d == selected) cs.onPrimary else cs.tertiary, CircleShape))
                                    if (planned) Box(Modifier.size(4.dp).background(if (d == selected) cs.onPrimary else cs.primary, CircleShape))
                                    if (exam) Box(Modifier.size(4.dp).background(if (d == selected) cs.onPrimary else accents.warning, CircleShape))
                                }
                            }
                        }
                    }
                } }
                FlowRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    CalendarLegend("Free time", cs.tertiary)
                    CalendarLegend("Sessions", cs.primary)
                    CalendarLegend("Exams", accents.warning)
                }
                TextButton(onClick = { selected = today; monthText = YearMonth.now().toString() }, modifier = Modifier.align(Alignment.CenterHorizontally)) { Text("Jump to Today") }
            }
        } }
        item { Surface(shape = RoundedCornerShape(22.dp), color = Color.Transparent) {
            Column(Modifier.fillMaxWidth().background(accents.banner).padding(20.dp)) {
                Text(dateLabel(selected), color = accents.onBanner, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                Text("$completed min completed · $planned min planned", color = accents.onBanner, style = MaterialTheme.typography.bodyMedium)
                Text("${snapshot.dayCapacity(selected)} min of configured free time", color = accents.onBanner, style = MaterialTheme.typography.bodySmall)
            }
        } }
        item { Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("Your free time", fontWeight = FontWeight.Bold)
            if (!past) TextButton(onClick = { editing = null; editor = true }, enabled = !busy) { Text("Add free time") }
        } }
        if (windows.isEmpty()) item { NoteCard("${if (past) "Archived date" else "Make room for your studies"}", if (snapshot.dayCapacity(selected) > 0) "${snapshot.dayCapacity(selected)} legacy minutes have no clock times. Add windows to replace that total with exact hours." else "No free windows on this date. Add one, or keep this as a day off.") }
        items(windows, key = { "window-${it.id}" }) { w ->
            Surface(shape = RoundedCornerShape(18.dp), color = cs.surface) {
                Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.Schedule, null, tint = cs.primary)
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(w.timeRangeString, fontWeight = FontWeight.SemiBold)
                        Text(if (selected == today && w.endMinute <= now) "Elapsed · excluded from recovery" else "${w.durationMinutes} min available", color = cs.onSurfaceVariant, style = MaterialTheme.typography.labelSmall)
                    }
                    if (!past) {
                        IconButton(onClick = { editing = w; editor = true }, enabled = !busy) { Icon(Icons.Rounded.Edit, "Edit ${w.timeRangeString}", tint = cs.primary) }
                        IconButton(onClick = { onDeleteTimeWindow(w.id) }, enabled = !busy) { Icon(Icons.Rounded.Close, "Remove ${w.timeRangeString}", tint = cs.onSurfaceVariant) }
                    }
                }
            }
        }
        if (!past) item { Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(Triple("Morning", 540, 660), Triple("Afternoon", 960, 1080), Triple("Evening", 1200, 1290)).forEach { (label, a, b) ->
                    OutlinedButton(onClick = { onAddTimeWindow(selected, a, b) }, enabled = !busy) { Text("+ $label") }
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedButton(onClick = { dayOff = true }, enabled = !busy, modifier = Modifier.weight(1f)) { Text("Day off") }
                OutlinedButton(onClick = { copying = true }, enabled = !busy && windows.isNotEmpty(), modifier = Modifier.weight(1f)) { Text("Copy to dates") }
            }
            Button(onClick = onNavigateToRecovery, enabled = !busy, modifier = Modifier.fillMaxWidth()) { Text("Create plan") }
        } }
        item { Text("Agenda · ${dateLabel(selected)}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold) }
        if (exams.isNotEmpty()) item {
            NoteCard("Exams", exams.joinToString("\n") { "${it.subject} · ${it.topic}" },
                containerColor = accents.warningContainer, titleColor = accents.warning)
        }
        if (sessions.isEmpty()) item {
            Text(
                if (past) "No study sessions were saved for this date."
                else "No study sessions yet. Add free time, then create and save a plan.",
                color = cs.onSurfaceVariant
            )
        }
        items(sessions, key = { "session-${it.id}" }) { session -> snapshot.tasks.find { it.id == session.taskId }?.let { task ->
            SessionCard(task, session, busy, selected <= today && (!session.hasExactTimes || selected < today || session.startMinute <= now), { onOpenTopic(task.id) }, { recording = session })
        } }
    }
    if (editor) WindowEditor(selected, editing, windows, busy, onDismiss = { editor = false }, onSave = { a, b ->
        val id = editing?.id
        if (id == null) onAddTimeWindow(selected, a, b) else onEditTimeWindow(id, selected, a, b)
        editor = false
    })
    if (dayOff) AlertDialog(onDismissRequest = { dayOff = false }, title = { Text("Make this a day off?") }, text = { Text("Free windows on ${dateLabel(selected)} will be removed. Completed study history stays saved; generate a new recovery plan afterwards.") }, confirmButton = { TextButton(onClick = { onClearDayWindows(selected); dayOff = false }, enabled = !busy) { Text("Mark day off") } }, dismissButton = { TextButton(onClick = { dayOff = false }) { Text("Keep windows") } })
    if (copying) {
        var targets by remember { mutableStateOf(setOf<Long>()) }
        AlertDialog(onDismissRequest = { copying = false }, title = { Text("Copy free windows") }, text = { Column(Modifier.verticalScroll(rememberScrollState())) {
            Text("Choose upcoming dates. Existing windows are kept. If a conflict is found, nothing is copied.", style = MaterialTheme.typography.bodySmall)
            (1L..7L).forEach { step -> val d = selected + step; if (d >= today) Row(verticalAlignment = Alignment.CenterVertically) { Checkbox(d in targets, { targets = if (it) targets + d else targets - d }); Text(dateLabel(d)) } }
        } }, confirmButton = { TextButton(onClick = { onCopyWindows(selected, targets.sorted()); copying = false }, enabled = !busy && targets.isNotEmpty()) { Text("Copy windows") } }, dismissButton = { TextButton(onClick = { copying = false }) { Text("Cancel") } })
    }
    recording?.let { session -> val task = snapshot.tasks.find { it.id == session.taskId }; val max = minOf(session.allocatedMinutes, task?.let { snapshot.remaining(it) } ?: 0)
        MinutesDialog("Record study progress", max, max, "0 means missed. Partial completion keeps the remaining work available for recovery.", listOf("Missed" to 0, "Half" to max / 2, "Full" to max), { recording = null }, { onProgress(session.id, it); recording = null })
    }
}
@Composable
private fun CalendarLegend(label: String, color: Color) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Box(Modifier.size(6.dp).background(color, CircleShape))
        Text(label, style = MaterialTheme.typography.labelSmall)
    }
}

@Composable
private fun WindowEditor(day: Long, editing: TimeWindow?, windows: List<TimeWindow>, busy: Boolean, onDismiss: () -> Unit, onSave: (Int, Int) -> Unit) {
    val cs = MaterialTheme.colorScheme
    var start by rememberSaveable { mutableIntStateOf(editing?.startMinute ?: 960) }
    var end by rememberSaveable { mutableIntStateOf(editing?.endMinute ?: 1020) }
    var error by rememberSaveable { mutableStateOf<String?>(null) }
    var pickingStart by rememberSaveable { mutableStateOf<Boolean?>(null) }
    if (pickingStart != null) {
        val isStart = pickingStart == true
        val value = if (isStart) start else end
        val picker = rememberTimePickerState(
            initialHour = (value / 60) % 24,
            initialMinute = value % 60,
            is24Hour = true
        )
        AlertDialog(
            onDismissRequest = { pickingStart = null },
            title = { Text(if (isStart) "Start time" else "End time") },
            text = { TimeInput(state = picker) },
            confirmButton = {
                TextButton(onClick = {
                    val minute = picker.hour * 60 + picker.minute
                    if (isStart) start = minute else end = minute
                    error = null
                    pickingStart = null
                }) { Text("Set time") }
            },
            dismissButton = { TextButton(onClick = { pickingStart = null }) { Text("Cancel") } }
        )
        return
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (editing == null) "Add free time" else "Edit free time") },
        text = {
            Column(
                Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(dateLabel(day), color = cs.onSurfaceVariant)
                OutlinedButton(onClick = { pickingStart = true }, modifier = Modifier.fillMaxWidth()) {
                    Text("Start · ${TimeWindow.formatMinuteOfDay(start)}")
                }
                OutlinedButton(onClick = { pickingStart = false }, modifier = Modifier.fillMaxWidth()) {
                    Text("End · ${TimeWindow.formatMinuteOfDay(end)}")
                }
                TextButton(onClick = { end = 1440; error = null }) { Text("End at midnight (24:00)") }
                Text("${(end - start).coerceAtLeast(0)} min", style = MaterialTheme.typography.bodySmall)
                error?.let { Text(it, color = cs.error, style = MaterialTheme.typography.bodySmall) }
            }
        },
        confirmButton = {
            Button(onClick = {
                error = TimeWindowValidator.validateWindow(day, start, end, windows, ignoreId = editing?.id ?: 0)
                if (error == null) onSave(start, end)
            }, enabled = !busy) { Text("Save window") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}
