@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.ora.rescue

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun RecoveryScreen(
    snapshot: StudySnapshot,
    preview: RecoveryPlan?,
    busy: Boolean,
    onGenerate: (Boolean) -> Unit,
    onCancel: () -> Unit,
    onAccept: () -> Unit,
    onAvailability: () -> Unit
) {
    val cs = MaterialTheme.colorScheme
    var split by rememberSaveable(snapshot.profile.preferredSplit) { mutableStateOf(snapshot.profile.preferredSplit) }
    val clock by rememberStudyClock()
    val today = clock.toLocalDate().toEpochDay()
    val isCurrent = preview?.revision == snapshot.revision &&
            preview.startDay == today &&
            preview.splitTasks == split
    val plan = if (isCurrent) preview else null
    val currentMinute = clock.hour * 60 + clock.minute
    val canSave = plan != null && plan.sessions.none {
        it.day == today && it.hasExactTimes && it.startMinute < currentMinute
    }
    val hasUnfinishedTasks = snapshot.tasks.any { snapshot.remaining(it) > 0 }
    val editFreeTime = {
        onCancel()
        onAvailability()
    }

    Column(Modifier.fillMaxSize()) {
        RecoveryContent(
            snapshot = snapshot,
            plan = plan,
            busy = busy,
            split = split,
            hasUnfinishedTasks = hasUnfinishedTasks,
            onSplitChange = { split = it; onCancel() },
            onGenerate = onGenerate,
            onAvailability = editFreeTime,
            modifier = Modifier.weight(1f)
        )
        if (plan != null) {
            Surface(color = cs.surface, tonalElevation = 2.dp) {
                Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp)) {
                    if (!canSave) {
                        Text(
                            "A proposed session has started. Recalculate to update the times.",
                            style = MaterialTheme.typography.bodySmall,
                            color = cs.error
                        )
                    }
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(onClick = onCancel, enabled = !busy, modifier = Modifier.weight(1f)) {
                            Text("Discard preview")
                        }
                        Button(
                            onClick = { if (canSave) onAccept() else onGenerate(split) },
                            enabled = !busy,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(if (canSave) "Save plan" else "Recalculate")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun RecoveryContent(
    snapshot: StudySnapshot,
    plan: RecoveryPlan?,
    busy: Boolean,
    split: Boolean,
    hasUnfinishedTasks: Boolean,
    onSplitChange: (Boolean) -> Unit,
    onGenerate: (Boolean) -> Unit,
    onAvailability: () -> Unit,
    modifier: Modifier = Modifier
) {
    val cs = MaterialTheme.colorScheme
    val accents = LocalOraAccents.current
    LazyColumn(
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 40.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Eyebrow("A FRESH START")
                Text(
                    text = "Recovery Plan",
                    fontFamily = OraFontFamily,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = cs.onSurface
                )
                Text(
                    text = "${snapshot.tasks.sumOf { snapshot.remaining(it) }} min remaining across topics.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = cs.onSurfaceVariant
                )
            }
        }

        item {
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = cs.primaryContainer.copy(alpha = 0.6f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(18.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Split longer topics",
                            fontWeight = FontWeight.SemiBold,
                            style = MaterialTheme.typography.titleSmall,
                            color = cs.onSurface
                        )
                        Text(
                            text = if (split)
                                "Allow topics to span multiple free windows and study days."
                            else
                                "Require each topic to fit in ONE continuous free window.",
                            style = MaterialTheme.typography.bodySmall,
                            color = cs.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = split,
                        onCheckedChange = onSplitChange,
                        enabled = !busy
                    )
                }
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = onAvailability,
                    enabled = !busy,
                    modifier = Modifier
                        .weight(1f)
                        .defaultMinSize(minHeight = 48.dp),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Icon(Icons.Rounded.CalendarToday, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Edit free time")
                }

                Button(
                    onClick = { onGenerate(split) },
                    enabled = !busy && hasUnfinishedTasks,
                    modifier = Modifier
                        .weight(1.2f)
                        .defaultMinSize(minHeight = 48.dp),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Icon(Icons.Rounded.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(if (plan == null) "Create plan" else "Recalculate")
                }
            }
        }

        if (!hasUnfinishedTasks) {
            item {
                NoteCard(
                    title = "Nothing waiting",
                    text = "All your study topics are completed, or you haven't added one yet. Visit your study desk to add a topic.",
                    icon = Icons.Rounded.Celebration,
                    containerColor = cs.tertiaryContainer.copy(alpha = 0.7f),
                    titleColor = cs.onTertiaryContainer
                )
            }
        }

        if (plan == null && hasUnfinishedTasks) {
            item {
                NoteCard(
                    title = "A preview you control",
                    text = "Review proposed study sessions, exact time slots, and any capacity shortfall before accepting. Your existing saved plan stays untouched until you accept.",
                    icon = Icons.Rounded.Info,
                    containerColor = cs.surfaceVariant
                )
            }
        }

        if (plan != null) {
            val totalPlaced = plan.sessions.sumOf { it.minutes }
            val totalShortfall = plan.unplaced.sumOf { it.minutes }

            item {
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = if (totalShortfall > 0) accents.warningContainer else cs.tertiaryContainer,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                if (totalShortfall > 0) Icons.Rounded.WarningAmber else Icons.Rounded.CheckCircle,
                                contentDescription = null,
                                tint = if (totalShortfall > 0) accents.warning else cs.onTertiaryContainer,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = "$totalPlaced min scheduled",
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.titleMedium,
                                color = if (totalShortfall > 0) accents.warning else cs.onTertiaryContainer
                            )
                        }
                        Text(
                            text = if (totalShortfall > 0)
                                "$totalShortfall min could not fit before their exams. Review the affected topics below."
                            else
                                "All outstanding study work successfully scheduled into your free-time windows before upcoming exams!",
                            style = MaterialTheme.typography.bodyMedium,
                            color = cs.onSurface
                        )
                        if (totalShortfall > 0) {
                            Text(
                                if (split) "Add free time before the affected exams, or check the dates and estimates."
                                else "Add a longer window before the exam, or turn on splitting and recalculate.",
                                style = MaterialTheme.typography.bodySmall
                            )
                            TextButton(onClick = onAvailability, enabled = !busy) { Text("Edit free time") }
                        }
                    }
                }
            }

            // Unplaced work list
            if (plan.unplaced.isNotEmpty()) {
                item {
                    Text(
                        "Work that cannot fit (${plan.unplaced.size})",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleSmall,
                        color = accents.warning
                    )
                }

                items(plan.unplaced, key = { "unplaced-${it.taskId}" }) { work ->
                    val task = snapshot.tasks.find { it.id == work.taskId }
                    OutlinedCard(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.outlinedCardColors(containerColor = cs.surface),
                        border = CardDefaults.outlinedCardBorder(enabled = true).let {
                            androidx.compose.foundation.BorderStroke(1.dp, accents.warning.copy(alpha = 0.5f))
                        }
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "${task?.subject ?: "Subject"} · ${task?.topic ?: "Topic"}",
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.titleSmall,
                                    color = cs.onSurface,
                                    modifier = Modifier.weight(1f)
                                )
                                Surface(
                                    color = accents.warningContainer,
                                    shape = RoundedCornerShape(6.dp)
                                ) {
                                    Text(
                                        "${work.minutes} min unallocated",
                                        Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                        color = accents.warning,
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                            Text(
                                text = work.reason,
                                style = MaterialTheme.typography.bodySmall,
                                color = cs.onSurfaceVariant
                            )
                            if (!split && work.reason.contains("whole remaining task")) {
                                Text(
                                    "Tip: Enable 'Split longer topics' above to allow this topic to divide across multiple free windows.",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = cs.primary,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                }
            }

            // Proposed schedule grouped by day
            val days = plan.sessions.groupBy { it.day }.toSortedMap()
            if (days.isNotEmpty()) {
                item {
                    Text(
                        "Your Revised Schedule",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleSmall,
                        color = cs.onSurface,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }

                items(days.keys.toList(), key = { "day-$it" }) { day ->
                    val sessions = days.getValue(day)
                    val available = snapshot.dayCapacity(day)
                    val done = snapshot.sessions.filter { it.day == day }.sumOf { it.completedMinutes }
                    val proposedMins = sessions.sumOf { it.minutes }

                    OutlinedCard(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.outlinedCardColors(containerColor = cs.surface)
                    ) {
                        Column(
                            modifier = Modifier.padding(18.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        Icons.Rounded.CalendarToday,
                                        contentDescription = null,
                                        tint = cs.primary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(Modifier.width(8.dp))
                                    Text(
                                        text = dateLabel(day),
                                        fontWeight = FontWeight.Bold,
                                        style = MaterialTheme.typography.titleMedium,
                                        color = cs.onSurface
                                    )
                                }
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = cs.primaryContainer
                                ) {
                                    Text(
                                        text = "$proposedMins min",
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = cs.onPrimaryContainer
                                    )
                                }
                            }

                            Text(
                                text = "$proposedMins min proposed + $done min completed / $available min daily capacity",
                                style = MaterialTheme.typography.bodySmall,
                                color = cs.onSurfaceVariant
                            )

                            LinearProgressIndicator(
                                progress = { if (available > 0) ((proposedMins + done).toFloat() / available).coerceIn(0f, 1f) else 0f },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(6.dp),
                                color = cs.primary,
                                trackColor = cs.surfaceVariant
                            )

                            sessions.forEach { session ->
                                val task = snapshot.tasks.find { it.id == session.taskId }
                                HorizontalDivider(color = cs.outlineVariant)

                                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "${task?.subject} · ${task?.topic}",
                                            fontWeight = FontWeight.SemiBold,
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = cs.onSurface,
                                            modifier = Modifier.weight(1f)
                                        )
                                        task?.priority?.let { PriorityBadge(it) }
                                    }

                                    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                                        Text(
                                            text = if (session.hasExactTimes)
                                                "${session.timeRangeString} (${session.minutes} min) · exam ${task?.examDay?.let(::dateLabel)}"
                                            else
                                                "${session.minutes} min · exam ${task?.examDay?.let(::dateLabel)}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = cs.onSurfaceVariant
                                        )

                                        val oldDates = snapshot.sessions
                                            .filter { it.taskId == session.taskId && it.status == StudySession.PENDING }
                                            .map { dateLabel(it.day) }
                                            .distinct()

                                        Text(
                                            text = if (oldDates.isEmpty()) "Previously unscheduled → ${dateLabel(day)}"
                                            else "${oldDates.joinToString()} → ${dateLabel(day)}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = cs.primary,
                                            fontWeight = FontWeight.Medium
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            item {
                Text(
                    "Saving replaces upcoming pending sessions. Completed study stays in your history.",
                    style = MaterialTheme.typography.bodySmall,
                    color = cs.onSurfaceVariant
                )
            }
        }
    }
}
