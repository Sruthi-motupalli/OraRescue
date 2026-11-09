@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.ora.rescue

import androidx.compose.foundation.background
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import java.time.LocalDate

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
    var split by rememberSaveable { mutableStateOf(true) }
    val today = LocalDate.now().toEpochDay()
    val isCurrent = preview?.revision == snapshot.revision &&
            preview.startDay == today &&
            preview.splitTasks == split
    val plan = if (isCurrent) preview else null
    val hasUnfinishedTasks = snapshot.tasks.any { snapshot.remaining(it) > 0 }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 36.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Eyebrow("ROOM TO BEGIN AGAIN")
                Text(
                    text = "Find your next step.",
                    style = MaterialTheme.typography.headlineMedium,
                    fontFamily = FontFamily.Serif,
                    fontWeight = FontWeight.Bold,
                    color = OraPlumInk
                )
                Text(
                    text = "Ora places unfinished work before each exam, prioritizing nearest deadlines and higher priority topics into your configured study availability.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = OraPlumMuted
                )
            }
        }

        item {
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = OraRoseLight.copy(alpha = 0.6f),
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
                            color = OraPlumInk
                        )
                        Text(
                            text = if (split)
                                "Allow topics to span multiple study days when daily capacity is limited."
                            else
                                "Keep each topic as a single study block on one day.",
                            style = MaterialTheme.typography.bodySmall,
                            color = OraPlumMuted
                        )
                    }
                    Switch(
                        checked = split,
                        onCheckedChange = {
                            split = it
                            onCancel()
                        },
                        enabled = !busy,
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = OraRose
                        )
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
                    onClick = {
                        onCancel()
                        onAvailability()
                    },
                    enabled = !busy,
                    modifier = Modifier
                        .weight(1f)
                        .defaultMinSize(minHeight = 48.dp),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Icon(Icons.Rounded.Schedule, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Daily availability")
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
                    Text(if (plan == null) "Generate preview" else "Recalculate")
                }
            }
        }

        if (!hasUnfinishedTasks) {
            item {
                NoteCard(
                    title = "Nothing waiting",
                    text = "All your study topics are completed, or you haven't added one yet. Visit your study desk to add a topic.",
                    icon = Icons.Rounded.Celebration,
                    containerColor = OraSageLight.copy(alpha = 0.7f),
                    titleColor = OraSage
                )
            }
        }

        if (plan == null && hasUnfinishedTasks) {
            item {
                NoteCard(
                    title = "A preview you control",
                    text = "Review proposed study sessions and any capacity shortfall before accepting. Your existing saved plan stays untouched until you accept.",
                    icon = Icons.Rounded.Info,
                    containerColor = OraCreamVariant
                )
            }
        }

        if (plan != null) {
            val totalPlaced = plan.sessions.sumOf { it.minutes }
            val totalShortfall = plan.unplaced.sumOf { it.minutes }

            item {
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = if (totalShortfall > 0) OraAmberLight else OraSageLight,
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
                                tint = if (totalShortfall > 0) OraAmber else OraSage,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = "$totalPlaced min found a place",
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.titleMedium,
                                color = if (totalShortfall > 0) OraAmber else OraSage
                            )
                        }
                        Text(
                            text = if (totalShortfall > 0)
                                "$totalShortfall min remain unallocated due to capacity or deadline limits. Review unallocated topics below."
                            else
                                "All outstanding study work successfully scheduled before upcoming exam deadlines!",
                            style = MaterialTheme.typography.bodyMedium,
                            color = OraPlumInk
                        )
                        Text(
                            text = "Daily capacity accounts for work already completed today. Completed work history is fully preserved.",
                            style = MaterialTheme.typography.labelSmall,
                            color = OraPlumMuted
                        )
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
                        color = OraAmber
                    )
                }

                items(plan.unplaced, key = { "unplaced-${it.taskId}" }) { work ->
                    val task = snapshot.tasks.find { it.id == work.taskId }
                    OutlinedCard(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.outlinedCardColors(containerColor = Color.White),
                        border = CardDefaults.outlinedCardBorder(enabled = true).let {
                            androidx.compose.foundation.BorderStroke(1.dp, OraAmber.copy(alpha = 0.5f))
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
                                    color = OraPlumInk,
                                    modifier = Modifier.weight(1f)
                                )
                                Surface(
                                    color = OraAmberLight,
                                    shape = RoundedCornerShape(6.dp)
                                ) {
                                    Text(
                                        "${work.minutes} min unallocated",
                                        Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                        color = OraAmber,
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                            Text(
                                text = work.reason,
                                style = MaterialTheme.typography.bodySmall,
                                color = OraPlumMuted
                            )
                            if (!split && work.reason.contains("whole remaining task")) {
                                Text(
                                    "Tip: Enable 'Split longer topics' above to allow this topic to divide across multiple days.",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = OraRose,
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
                        "Proposed Daily Schedule",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleSmall,
                        color = OraPlumInk,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }

                items(days.keys.toList(), key = { "day-$it" }) { day ->
                    val sessions = days.getValue(day)
                    val available = snapshot.availability.find { it.day == day }?.minutes ?: 0
                    val done = snapshot.sessions.filter { it.day == day }.sumOf { it.completedMinutes }
                    val proposedMins = sessions.sumOf { it.minutes }

                    OutlinedCard(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.outlinedCardColors(containerColor = Color.White)
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
                                        tint = OraRose,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(Modifier.width(8.dp))
                                    Text(
                                        text = dateLabel(day),
                                        fontWeight = FontWeight.Bold,
                                        style = MaterialTheme.typography.titleMedium,
                                        color = OraPlumInk
                                    )
                                }
                                Text(
                                    text = "$proposedMins min proposed",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = OraRose
                                )
                            }

                            Text(
                                text = "$proposedMins min proposed + $done min completed / $available min daily available",
                                style = MaterialTheme.typography.bodySmall,
                                color = OraPlumMuted
                            )

                            LinearProgressIndicator(
                                progress = { if (available > 0) ((proposedMins + done).toFloat() / available).coerceIn(0f, 1f) else 0f },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(6.dp),
                                color = OraRose,
                                trackColor = OraCreamVariant
                            )

                            sessions.forEach { session ->
                                val task = snapshot.tasks.find { it.id == session.taskId }
                                HorizontalDivider(color = OraBorderLight)

                                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "${task?.subject} · ${task?.topic}",
                                            fontWeight = FontWeight.SemiBold,
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = OraPlumInk,
                                            modifier = Modifier.weight(1f)
                                        )
                                        task?.priority?.let { PriorityBadge(it) }
                                    }

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            "${session.minutes} min · exam ${task?.examDay?.let(::dateLabel)}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = OraPlumMuted
                                        )

                                        val oldDates = snapshot.sessions
                                            .filter { it.taskId == session.taskId && it.status == StudySession.PENDING }
                                            .map { dateLabel(it.day) }
                                            .distinct()

                                        Text(
                                            text = if (oldDates.isEmpty()) "Previously unscheduled → ${dateLabel(day)}"
                                            else "${oldDates.joinToString()} → ${dateLabel(day)}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = OraRose,
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
                Column(
                    modifier = Modifier.padding(top = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = onAccept,
                        enabled = !busy,
                        modifier = Modifier
                            .fillMaxWidth()
                            .defaultMinSize(minHeight = 52.dp),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Icon(Icons.Rounded.CheckCircle, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Accept & Apply Recovery Plan")
                    }

                    OutlinedButton(
                        onClick = onCancel,
                        enabled = !busy,
                        modifier = Modifier
                            .fillMaxWidth()
                            .defaultMinSize(minHeight = 48.dp),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Text("Discard Preview")
                    }

                    Text(
                        text = "Accepting saves your new schedule. If your topics, availability, or the date change, generate a fresh preview before accepting.",
                        style = MaterialTheme.typography.bodySmall,
                        color = OraPlumMuted,
                        modifier = Modifier.padding(horizontal = 4.dp)
                    )
                }
            }
        }
    }
}

