@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class, androidx.compose.foundation.layout.ExperimentalLayoutApi::class)

package com.ora.rescue

import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.automirrored.rounded.MenuBook
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.delay

val DayFormat: DateTimeFormatter = DateTimeFormatter.ofPattern("EEE, d MMM")
val FullDayFormat: DateTimeFormatter = DateTimeFormatter.ofPattern("EEE, d MMM yyyy")

fun dateLabel(day: Long): String = LocalDate.ofEpochDay(day).format(DayFormat)
fun dateWithYear(day: Long): String = LocalDate.ofEpochDay(day).format(FullDayFormat)

@Composable
internal fun rememberStudyClock(): State<LocalDateTime> = produceState(LocalDateTime.now()) {
    while (true) {
        value = LocalDateTime.now()
        // Session times have minute precision. Refresh at the next minute boundary.
        delay((60_000L - value.second * 1_000L - value.nano / 1_000_000).coerceAtLeast(1L))
    }
}

fun daysRemainingLabel(examDay: Long, today: Long): String = when {
    examDay == today -> "Exam is today"
    examDay == today + 1 -> "Exam tomorrow"
    examDay > today -> "Exam in ${examDay - today} days"
    else -> "Exam date passed"
}

fun priorityLabel(priority: Int): String = when (priority) {
    3 -> "High"
    2 -> "Medium"
    else -> "Low"
}

@Composable
fun Eyebrow(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text.uppercase(),
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.primary,
        fontWeight = FontWeight.Bold,
        modifier = modifier
    )
}

@Composable
fun PriorityBadge(priority: Int, modifier: Modifier = Modifier) {
    val cs = MaterialTheme.colorScheme
    val (color, container) = when (priority) {
        3 -> cs.onPrimaryContainer to cs.primaryContainer
        2 -> cs.onSecondaryContainer to cs.secondaryContainer
        else -> cs.onTertiaryContainer to cs.tertiaryContainer
    }
    Surface(
        color = container,
        shape = RoundedCornerShape(8.dp),
        modifier = modifier
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .background(color, shape = RoundedCornerShape(3.dp))
            )
            Text(
                text = "${priorityLabel(priority)} Priority",
                color = color,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
fun NoteCard(
    title: String,
    text: String,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    containerColor: Color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f),
    titleColor: Color = MaterialTheme.colorScheme.onBackground
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = containerColor
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (icon != null) {
                Icon(icon, contentDescription = null, tint = titleColor, modifier = Modifier.size(20.dp))
            }
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(title, fontWeight = FontWeight.SemiBold, color = titleColor, style = MaterialTheme.typography.titleSmall)
                Text(text, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onBackground)
            }
        }
    }
}

@Composable
fun EmptyStateCard(
    icon: ImageVector,
    title: String,
    description: String,
    modifier: Modifier = Modifier,
    actionText: String? = null,
    onAction: (() -> Unit)? = null
) {
    OutlinedCard(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.outlinedCardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.size(52.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(28.dp))
                }
            }
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 12.dp),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
            if (actionText != null && onAction != null) {
                Spacer(Modifier.height(4.dp))
                Button(
                    onClick = onAction,
                    modifier = Modifier.defaultMinSize(minHeight = 48.dp)
                ) {
                    Text(actionText)
                }
            }
        }
    }
}

@Composable
fun DeskSummaryBanner(
    planned: Int,
    worked: Int,
    capacity: Int,
    openTasks: Int,
    outstanding: Int,
    modifier: Modifier = Modifier
) {
    val accents = LocalOraAccents.current
    val progress = if (capacity > 0) ((planned + worked).toFloat() / capacity).coerceIn(0f, 1f) else 0f
    val percentText = if (capacity > 0) "${((planned + worked) * 100) / capacity}% booked" else "No capacity set"

    Surface(
        shape = RoundedCornerShape(24.dp),
        modifier = modifier.fillMaxWidth(),
        color = Color.Transparent
    ) {
        Column(
            modifier = Modifier
                .background(
                    accents.banner
                )
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.Spa, contentDescription = null, tint = accents.onBanner, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(
                        "Today’s Goal · Keep Going!",
                        fontWeight = FontWeight.SemiBold,
                        style = MaterialTheme.typography.bodyMedium,
                        color = accents.onBanner
                    )
                }
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = accents.onBanner.copy(alpha = 0.2f)
                ) {
                    Text(
                        text = percentText,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Medium,
                        color = accents.onBanner
                    )
                }
            }

            Text(
                text = "$planned min planned",
                style = MaterialTheme.typography.headlineMedium,
                fontFamily = OraFontFamily,
                fontWeight = FontWeight.Bold,
                color = accents.onBanner
            )

            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp),
                    color = accents.onBanner,
                    trackColor = accents.onBanner.copy(alpha = 0.7f)
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        "$worked min completed",
                        style = MaterialTheme.typography.labelSmall,
                        color = accents.onBanner,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        "$capacity min daily capacity",
                        style = MaterialTheme.typography.labelSmall,
                        color = accents.onBanner.copy(alpha = 0.8f)
                    )
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "$openTasks open topics to study",
                    style = MaterialTheme.typography.labelMedium,
                    color = accents.onBanner
                )
                Text(
                    "$outstanding min left overall",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = accents.onBanner
                )
            }
        }
    }
}

@Composable
fun DaySelector(
    selectedDay: Long,
    today: Long,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onOpenDatePicker: () -> Unit,
    onJumpToToday: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            IconButton(
                onClick = onPrevious,
                modifier = Modifier.size(48.dp)
            ) {
                Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Previous study day")
            }

            OutlinedButton(
                onClick = onOpenDatePicker,
                modifier = Modifier
                    .weight(1f)
                    .defaultMinSize(minHeight = 44.dp)
                    .padding(horizontal = 4.dp),
                shape = RoundedCornerShape(14.dp)
            ) {
                Icon(Icons.Rounded.CalendarToday, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(8.dp))
                Text(
                    text = if (selectedDay == today) "Today · ${dateLabel(selectedDay)}" else dateLabel(selectedDay),
                    fontWeight = FontWeight.SemiBold,
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            IconButton(
                onClick = onNext,
                modifier = Modifier.size(48.dp)
            ) {
                Icon(Icons.AutoMirrored.Rounded.ArrowForward, contentDescription = "Next study day")
            }
        }

        if (selectedDay != today) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center
            ) {
                TextButton(
                    onClick = onJumpToToday,
                    modifier = Modifier.defaultMinSize(minHeight = 36.dp)
                ) {
                    Icon(Icons.Rounded.Today, contentDescription = null, modifier = Modifier.size(15.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Jump back to Today", style = MaterialTheme.typography.labelMedium)
                }
            }
        }
    }
}

@Composable
fun TopicCard(
    task: StudyTask,
    remaining: Int,
    completed: Int,
    today: Long,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val progress = if (task.totalMinutes > 0) (completed.toFloat() / task.totalMinutes).coerceIn(0f, 1f) else 0f
    val isFinished = remaining == 0
    val cs = MaterialTheme.colorScheme

    OutlinedCard(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.outlinedCardColors(containerColor = cs.surface)
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.AutoMirrored.Rounded.MenuBook,
                    contentDescription = null,
                    tint = cs.primary,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = task.subject,
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = cs.primary
                )
                PriorityBadge(task.priority)
            }

            Text(
                text = task.topic,
                fontWeight = FontWeight.SemiBold,
                style = MaterialTheme.typography.titleMedium,
                color = cs.onBackground
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = daysRemainingLabel(task.examDay, today) + " · ${dateLabel(task.examDay)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = cs.onSurfaceVariant
                )
                Text(
                    text = if (isFinished) "0 min remaining" else "$remaining min remaining",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = if (isFinished) cs.tertiary else cs.onBackground
                )
            }

            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp),
                color = if (isFinished) cs.tertiary else cs.primary,
                trackColor = cs.surfaceVariant
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "$completed of ${task.totalMinutes} min completed",
                    style = MaterialTheme.typography.labelSmall,
                    color = cs.onSurfaceVariant
                )
                if (isFinished) {
                    Surface(
                        color = cs.tertiaryContainer,
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(Icons.Rounded.CheckCircle, contentDescription = null, tint = cs.tertiary, modifier = Modifier.size(14.dp))
                            Text("Completed", color = cs.tertiary, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SessionCard(
    task: StudyTask,
    session: StudySession,
    busy: Boolean,
    canRecord: Boolean,
    onEdit: () -> Unit,
    onProgress: () -> Unit,
    modifier: Modifier = Modifier
) {
    val cs = MaterialTheme.colorScheme
    OutlinedCard(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.outlinedCardColors(containerColor = cs.surface)
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = task.subject,
                    modifier = Modifier.weight(1f),
                    color = cs.primary,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold
                )
                PriorityBadge(task.priority)
            }

            Text(
                text = task.topic,
                fontWeight = FontWeight.SemiBold,
                style = MaterialTheme.typography.titleMedium,
                color = cs.onBackground,
                modifier = Modifier.clickable(onClick = onEdit)
            )

            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = if (session.hasExactTimes)
                        "${session.timeRangeString} (${session.allocatedMinutes} min)"
                    else
                        "${session.allocatedMinutes} min scheduled session",
                    style = MaterialTheme.typography.bodySmall,
                    color = cs.onSurfaceVariant
                )
                when (session.status) {
                    StudySession.CLOSED -> Surface(color = cs.tertiaryContainer, shape = RoundedCornerShape(6.dp)) {
                        Text(
                            "${session.completedMinutes} min completed",
                            Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                            color = cs.onTertiaryContainer,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    StudySession.MISSED -> Surface(color = cs.errorContainer, shape = RoundedCornerShape(6.dp)) {
                        Text(
                            "Missed · ready to recover",
                            Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                            color = cs.onErrorContainer,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    else -> Surface(color = cs.primaryContainer, shape = RoundedCornerShape(6.dp)) {
                        Text(
                            if (canRecord) "Planned" else "Upcoming",
                            Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                            color = cs.onPrimaryContainer,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(
                    onClick = onEdit,
                    enabled = !busy,
                    modifier = Modifier.defaultMinSize(minHeight = 44.dp)
                ) {
                    Text("View Topic")
                }
                Spacer(Modifier.width(8.dp))
                if (session.status == StudySession.PENDING) {
                    FilledTonalButton(
                        onClick = onProgress,
                        enabled = !busy && canRecord,
                        modifier = Modifier.defaultMinSize(minHeight = 44.dp)
                    ) {
                        Icon(Icons.Rounded.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Record progress")
                    }
                }
            }
        }
    }
}

@Composable
fun MinutesDialog(
    title: String,
    initial: Int,
    max: Int,
    explanation: String,
    quickPresets: List<Pair<String, Int>> = emptyList(),
    onDismiss: () -> Unit,
    onSave: (Int) -> Unit
) {
    var value by rememberSaveable { mutableStateOf(initial.toString()) }
    val parsed = value.toIntOrNull()
    val valid = parsed != null && parsed in 0..max

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Text(explanation, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)

                if (quickPresets.isNotEmpty()) {
                    Text("Quick presets:", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        quickPresets.forEach { (label, presetVal) ->
                            if (presetVal <= max) {
                                FilterChip(
                                    selected = parsed == presetVal,
                                    onClick = { value = presetVal.toString() },
                                    label = { Text(label, style = MaterialTheme.typography.labelSmall) }
                                )
                            }
                        }
                    }
                }

                OutlinedTextField(
                    value = value,
                    onValueChange = { if (it.length <= 6) value = it },
                    label = { Text("Minutes") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    isError = !valid,
                    supportingText = {
                        Text(if (!valid) "Enter a number between 0 and $max minutes." else "Max $max minutes.")
                    },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { parsed?.let(onSave) },
                enabled = valid,
                modifier = Modifier.defaultMinSize(minHeight = 44.dp)
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.defaultMinSize(minHeight = 44.dp)
            ) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun ChooseDateDialog(
    initialDay: Long,
    minDay: Long? = null,
    onDismiss: () -> Unit,
    onChoose: (Long) -> Unit
) {
    val initial = LocalDate.ofEpochDay(initialDay).atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
    val pickerState = rememberDatePickerState(
        initialSelectedDateMillis = initial,
        selectableDates = object : SelectableDates {
            override fun isSelectableDate(utcTimeMillis: Long): Boolean {
                if (minDay == null) return true
                val date = Instant.ofEpochMilli(utcTimeMillis).atZone(ZoneOffset.UTC).toLocalDate().toEpochDay()
                return date >= minDay
            }
        }
    )

    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                onClick = {
                    pickerState.selectedDateMillis?.let {
                        val chosen = Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate().toEpochDay()
                        onChoose(chosen)
                    }
                },
                enabled = pickerState.selectedDateMillis != null,
                modifier = Modifier.defaultMinSize(minHeight = 44.dp)
            ) {
                Text("Confirm Date")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, modifier = Modifier.defaultMinSize(minHeight = 44.dp)) {
                Text("Cancel")
            }
        }
    ) {
        DatePicker(state = pickerState)
    }
}

