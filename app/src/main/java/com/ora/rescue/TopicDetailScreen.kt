package com.ora.rescue

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun TopicDetailScreen(
    id: Long,
    snapshot: StudySnapshot,
    onEdit: () -> Unit,
    onBack: () -> Unit
) {
    val task = snapshot.tasks.firstOrNull { it.id == id }
    val colors = MaterialTheme.colorScheme
    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        if (task == null) {
            Text("This topic is no longer available.", style = MaterialTheme.typography.titleMedium)
            Button(onClick = onBack) { Text("Go back") }
            return@Column
        }

        val clock by rememberStudyClock()
        val today = clock.toLocalDate().toEpochDay()
        val completed = snapshot.completed(task.id)
        val remaining = snapshot.remaining(task)
        val planned = snapshot.sessions.filter {
            it.taskId == task.id && it.status == StudySession.PENDING && it.day >= today
        }.sumOf { it.allocatedMinutes }

        Text(task.subject, color = colors.primary, style = MaterialTheme.typography.labelLarge)
        Text(task.topic, style = MaterialTheme.typography.headlineMedium)
        OutlinedCard(colors = CardDefaults.outlinedCardColors(containerColor = colors.surface)) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text("Exam · ${dateWithYear(task.examDay)}", style = MaterialTheme.typography.titleMedium)
                Text(daysRemainingLabel(task.examDay, today), color = colors.onSurfaceVariant)
                PriorityBadge(task.priority)
            }
        }
        OutlinedCard(colors = CardDefaults.outlinedCardColors(containerColor = colors.surface)) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text("Study progress", style = MaterialTheme.typography.titleMedium)
                Text("$completed of ${task.totalMinutes} min completed")
                LinearProgressIndicator(
                    progress = { (completed.toFloat() / task.totalMinutes).coerceIn(0f, 1f) },
                    modifier = Modifier.fillMaxWidth()
                )
                Text("$planned min planned · $remaining min remaining", color = colors.onSurfaceVariant)
            }
        }
        Button(onClick = onEdit, modifier = Modifier.fillMaxWidth()) { Text("Edit topic") }
    }
}
