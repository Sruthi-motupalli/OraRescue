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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.MenuBook
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

@Composable
fun HomeScreen(
    snapshot: StudySnapshot,
    busy: Boolean,
    onAdd: () -> Unit,
    onOpenTopic: (Long) -> Unit,
    onRecover: () -> Unit,
    onCalendar: () -> Unit,
    onProgress: (Long, Int) -> Unit,
    onCompleteNow: (Long, Int) -> Unit,
    onSkip: (Long) -> Unit
) {
    val cs = MaterialTheme.colorScheme
    val accents = LocalOraAccents.current
    var searchQuery by rememberSaveable { mutableStateOf("") }
    var selectedFilter by rememberSaveable { mutableStateOf("All") } // "All", "In Progress", "Completed", or a subject name
    var tab by rememberSaveable { mutableIntStateOf(0) } // 0: My Study Plan, 1: All Topics

    val clock by rememberStudyClock()
    val todayEpoch = clock.toLocalDate().toEpochDay()
    val profile = snapshot.profile

    // Real stats
    val now = clock.hour * 60 + clock.minute
    val todaySessions = snapshot.sessions.filter { it.day == todayEpoch }.sortedBy { it.startMinute }
    val activeSessions = snapshot.sessions.filter { it.status == StudySession.PENDING }
    val todayPlan = activeSessions.filter { it.day == todayEpoch }.sortedBy { it.startMinute }
    val overduePlan = activeSessions.filter { it.day < todayEpoch }.sortedWith(compareBy<StudySession> { it.day }.thenBy { it.startMinute })
    val upcomingPlan = activeSessions.filter { it.day > todayEpoch }.sortedWith(compareBy<StudySession> { it.day }.thenBy { it.startMinute })
    val nextSession = todaySessions.firstOrNull {
        it.status == StudySession.PENDING && it.hasExactTimes && it.endMinute > now
    }
    val nextTopic = nextSession?.let { session -> snapshot.tasks.find { it.id == session.taskId } }
    val todayWorked = todaySessions.sumOf { it.completedMinutes }
    val todayPlanned = todaySessions.sumOf { it.allocatedMinutes }
    val todayCompletedTasks = todaySessions.count { it.status == StudySession.CLOSED && it.completedMinutes > 0 }
    val todayTaskCount = todaySessions.size
    val todayCapacity = snapshot.dayCapacity(todayEpoch)
    val totalDoneMinutes = snapshot.sessions.sumOf { it.completedMinutes }
    val doneTopics = snapshot.tasks.count { snapshot.remaining(it) == 0 }
    val openTasks = snapshot.tasks.count { snapshot.remaining(it) > 0 }
    val outstanding = snapshot.tasks.sumOf { snapshot.remaining(it) }

    // Nearest exam calculation
    val nearestExamTask = snapshot.tasks
        .filter { it.examDay >= todayEpoch }
        .minWithOrNull(compareBy<StudyTask> { it.examDay }.thenByDescending { it.priority }.thenBy { it.id })

    val greetingTime = when (clock.hour) {
        in 0..11 -> "Good Morning"
        in 12..17 -> "Good Afternoon"
        else -> "Good Evening"
    }

    // Filtered topics
    val filteredTasks = snapshot.tasks.filter { task ->
        val matchesSearch = searchQuery.isBlank() ||
                task.topic.contains(searchQuery, ignoreCase = true) ||
                task.subject.contains(searchQuery, ignoreCase = true)
        val matchesFilter = when (selectedFilter) {
            "All" -> true
            "In Progress" -> snapshot.remaining(task) > 0
            "Completed" -> snapshot.remaining(task) == 0
            else -> task.subject.equals(selectedFilter, ignoreCase = true)
        }
        matchesSearch && matchesFilter
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 40.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Greeting & Avatar Header
        item {
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Eyebrow(greetingTime)
                    Text(
                        text = "Hello, ${profile.name} 👋",
                        fontFamily = OraFontFamily,
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = cs.onSurface
                    )
                    Text(
                        text = "Make today productive and achieve your goals.",
                        style = MaterialTheme.typography.bodySmall,
                        color = cs.onSurfaceVariant
                    )
                }

                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(
                            accents.avatar
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        StudentAvatars.getOrElse(profile.avatarIndex) { "👩🏻‍💻" },
                        style = MaterialTheme.typography.titleLarge
                    )
                }
            }
        }

        if (nextSession != null && nextTopic != null) {
            item {
                NoteCard(
                    title = if (nextSession.startMinute <= now) "Study session now" else "Next session today",
                    text = "${nextSession.timeRangeString} · ${nextTopic.subject} · ${nextTopic.topic}",
                    icon = Icons.Rounded.Schedule
                )
            }
        }

        // Nearest Exam Spotlight Banner (if an exam is upcoming)
        nearestExamTask?.let { task ->
            item {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = cs.surface,
                    modifier = Modifier.fillMaxWidth().border(1.dp, cs.outlineVariant, RoundedCornerShape(16.dp))
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Surface(shape = CircleShape, color = cs.errorContainer, modifier = Modifier.size(32.dp)) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(Icons.Rounded.Event, contentDescription = null, tint = cs.onErrorContainer, modifier = Modifier.size(16.dp))
                            }
                        }
                        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(
                                "Nearest Exam: ${task.subject} · ${task.topic}",
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.bodyMedium,
                                color = cs.onSurface
                            )
                            Text(
                                "${daysRemainingLabel(task.examDay, todayEpoch)} (${dateLabel(task.examDay)}) · ${snapshot.remaining(task)} min left",
                                style = MaterialTheme.typography.bodySmall,
                                color = cs.onSurfaceVariant
                            )
                        }
                        TextButton(onClick = { onOpenTopic(task.id) }) {
                            Text("View", style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
            }
        }

        // Study statistics
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Surface(
                    modifier = Modifier.weight(1f),
                    color = cs.surface,
                    shape = RoundedCornerShape(18.dp)
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Surface(shape = CircleShape, color = cs.tertiaryContainer, modifier = Modifier.size(28.dp)) {
                            Box(contentAlignment = Alignment.Center) {
                                Text("✓", color = cs.onTertiaryContainer, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelMedium)
                            }
                        }
                        Text("$doneTopics", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = cs.onSurface)
                        Text("Topics Done", color = cs.onSurfaceVariant, style = MaterialTheme.typography.labelSmall)
                    }
                }

                Surface(
                    modifier = Modifier.weight(1f),
                    color = cs.surface,
                    shape = RoundedCornerShape(18.dp)
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Surface(shape = CircleShape, color = cs.secondaryContainer, modifier = Modifier.size(28.dp)) {
                            Box(contentAlignment = Alignment.Center) {
                                Text("◷", color = cs.onSecondaryContainer, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelMedium)
                            }
                        }
                        Text("${totalDoneMinutes / 60} h ${totalDoneMinutes % 60} m", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = cs.onSurface)
                        Text("Focus Time", color = cs.onSurfaceVariant, style = MaterialTheme.typography.labelSmall)
                    }
                }

                Surface(
                    modifier = Modifier.weight(1f),
                    color = cs.surface,
                    shape = RoundedCornerShape(18.dp)
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Surface(shape = CircleShape, color = cs.primaryContainer, modifier = Modifier.size(28.dp)) {
                            Box(contentAlignment = Alignment.Center) {
                                Text("ϟ", color = cs.onPrimaryContainer, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelMedium)
                            }
                        }
                        Text("$openTasks", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = cs.onSurface)
                        Text("Open Topics", color = cs.onSurfaceVariant, style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
        }

        // Today's Goal & Recovery Shortcut Banner
        item {
            val goalProgress = if (todayPlanned > 0) (todayWorked.toFloat() / todayPlanned).coerceIn(0f, 1f) else 0f
            val goalPercent = (goalProgress * 100).toInt()

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
                            Text("Today's plan", style = MaterialTheme.typography.labelSmall, color = accents.onBanner.copy(alpha = 0.9f))
                            IconButton(
                                onClick = onRecover,
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(CircleShape)
                                    .background(cs.surface)
                            ) {
                                Icon(Icons.Rounded.AutoAwesome, contentDescription = "Open Recovery", tint = cs.primary, modifier = Modifier.size(16.dp))
                            }
                        }

                        Text(
                            text = if (todayTaskCount > 0) "$todayCompletedTasks of $todayTaskCount tasks · $todayWorked of $todayPlanned min" else "Nothing scheduled today",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = accents.onBanner
                        )

                        Text(
                            text = "${todayPlan.size} active today · $outstanding min remaining across topics",
                            style = MaterialTheme.typography.bodySmall,
                            color = accents.onBanner
                        )

                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            LinearProgressIndicator(
                                progress = { goalProgress },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(6.dp)
                                    .clip(RoundedCornerShape(3.dp)),
                                color = accents.onBanner,
                                trackColor = accents.onBanner.copy(alpha = 0.3f)
                            )
                            Text(
                                "$goalPercent%",
                                style = MaterialTheme.typography.labelSmall,
                                color = accents.onBanner,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }

        // Todoist-style persistent plan queue vs educational topic library.
        item {
            TabRow(
                selectedTabIndex = tab,
                containerColor = Color.Transparent,
                divider = { HorizontalDivider(color = cs.outlineVariant) }
            ) {
                Tab(
                    selected = tab == 0,
                    onClick = { tab = 0 },
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Icon(Icons.Rounded.Checklist, contentDescription = null, modifier = Modifier.size(16.dp))
                            Text("My Plan (${activeSessions.size})")
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

        if (tab == 0) {
            // TAB 0: today first; completed cards disappear immediately.
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Today",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = cs.onSurface
                    )
                    TextButton(onClick = onCalendar) {
                        Icon(Icons.Rounded.CalendarToday, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Calendar", style = MaterialTheme.typography.labelSmall)
                    }
                }
            }

            if (todayPlan.isEmpty()) {
                item {
                    OutlinedCard(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.outlinedCardColors(containerColor = cs.surface)
                    ) {
                        Column(
                            modifier = Modifier.padding(18.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = if (todayTaskCount > 0) "Today is complete" else "Nothing active today",
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.bodyMedium,
                                color = cs.onSurface
                            )
                            Text(
                                text = when {
                                    todayTaskCount > 0 -> "Completed tasks are saved in your progress and removed from this list."
                                    snapshot.tasks.isEmpty() -> "Add your first study topic to start planning."
                                    openTasks == 0 -> "Your current topics are complete. Add another topic when you’re ready."
                                    todayCapacity == 0 -> "Add free time today or on another date before your exam."
                                    else -> "Create a plan to fit your remaining topics into your free time."
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = cs.onSurfaceVariant
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.padding(top = 4.dp)) {
                                when {
                                    openTasks == 0 -> Button(onClick = onAdd) { Text("Add topic") }
                                    todayCapacity == 0 -> Button(onClick = onCalendar) { Text("Add free time") }
                                    else -> Button(onClick = onRecover) { Text("Create plan") }
                                }
                            }
                        }
                    }
                }
            } else {
                items(todayPlan, key = { "plan-session-${it.id}" }) { session ->
                    val task = snapshot.tasks.find { it.id == session.taskId }
                    if (task != null) {
                        SessionCard(
                            task = task,
                            session = session,
                            busy = busy,
                            scheduleLabel = when {
                                session.day < todayEpoch -> "Overdue · ${dateLabel(session.day)}"
                                session.day == todayEpoch -> "Today"
                                else -> dateLabel(session.day)
                            },
                            // Home always offers a direct action. Future work is first moved to "now" by the repository.
                            canRecord = true,
                            onEdit = { onOpenTopic(task.id) },
                            onProgress = {
                                if (session.hasExactTimes && session.startMinute > now) onCompleteNow(session.id, minOf(session.allocatedMinutes, snapshot.remaining(task)))
                                else onProgress(session.id, minOf(session.allocatedMinutes, snapshot.remaining(task)))
                            },
                            onSkip = if (session.day < todayEpoch) ({ onSkip(session.id) }) else null
                        )
                    }
                }
            }

            if (overduePlan.isNotEmpty()) {
                item { Text("Needs recovery (${overduePlan.size})", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold) }
                items(overduePlan, key = { "overdue-${it.id}" }) { session ->
                    snapshot.tasks.find { it.id == session.taskId }?.let { task ->
                        SessionCard(task, session, busy, true, { onOpenTopic(task.id) }, { onCompleteNow(session.id, minOf(session.allocatedMinutes, snapshot.remaining(task))) }, "Overdue · ${dateLabel(session.day)}", { onSkip(session.id) })
                    }
                }
            }
            if (upcomingPlan.isNotEmpty()) {
                item { Text("Upcoming (${upcomingPlan.size})", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold) }
                items(upcomingPlan.take(3), key = { "upcoming-${it.id}" }) { session ->
                    snapshot.tasks.find { it.id == session.taskId }?.let { task ->
                        SessionCard(task, session, busy, true, { onOpenTopic(task.id) }, { onCompleteNow(session.id, minOf(session.allocatedMinutes, snapshot.remaining(task))) }, dateLabel(session.day))
                    }
                }
            }
        } else {
            // TAB 1: ALL TOPICS WITH SEARCH & FILTER
            item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    // Search Bar
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text("Search by topic or subject...") },
                        leadingIcon = {
                            Icon(Icons.Rounded.Search, contentDescription = null, tint = cs.primary)
                        },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { searchQuery = "" }) {
                                    Icon(Icons.Rounded.Close, contentDescription = "Clear search")
                                }
                            }
                        },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp)
                    )

                    // Filter Chips Row
                    val distinctSubjects = snapshot.tasks.map { it.subject }.distinct()
                    val filterOptions = listOf("All", "In Progress", "Completed") + distinctSubjects

                    LazyRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        contentPadding = PaddingValues(horizontal = 1.dp)
                    ) {
                        items(filterOptions) { filter ->
                            FilterChip(
                                selected = selectedFilter == filter,
                                onClick = { selectedFilter = filter },
                                label = {
                                    Text(
                                        text = filter,
                                        style = MaterialTheme.typography.labelMedium,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            )
                        }
                    }
                }
            }

            if (filteredTasks.isEmpty()) {
                item {
                    OutlinedCard(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.outlinedCardColors(containerColor = cs.surface)
                    ) {
                        Column(
                            modifier = Modifier.padding(20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = if (searchQuery.isNotBlank() || selectedFilter != "All") "No matching study topics found" else "No study topics added yet",
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.bodyMedium,
                                color = cs.onSurface
                            )
                            Text(
                                text = if (searchQuery.isNotBlank()) "Try a different search term or reset the filter."
                                else "Add your first study topic to start planning your exam preparation.",
                                style = MaterialTheme.typography.bodySmall,
                                color = cs.onSurfaceVariant
                            )
                            if (searchQuery.isBlank() && selectedFilter == "All") {
                                Button(onClick = onAdd, shape = RoundedCornerShape(12.dp)) {
                                    Text("Add First Topic")
                                }
                            }
                        }
                    }
                }
            } else {
                items(filteredTasks, key = { "topic-${it.id}" }) { task ->
                    TopicCard(
                        task = task,
                        remaining = snapshot.remaining(task),
                        completed = snapshot.completed(task.id),
                        today = todayEpoch,
                        onClick = { onOpenTopic(task.id) }
                    )
                }
            }

            item {
                OutlinedButton(
                    onClick = onAdd,
                    enabled = !busy,
                    modifier = Modifier.fillMaxWidth().defaultMinSize(minHeight = 48.dp),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Add Another Study Topic")
                }
            }
        }
    }

}
