@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.ora.rescue

import android.content.Intent
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import java.time.LocalDate

private data class PickedMaterial(val name: String, val uri: String, val mimeType: String)

@Composable
fun CourseScreen(
    snapshot: StudySnapshot,
    busy: Boolean,
    selectedChapterId: Long?,
    onImport: (String, String, String, Long, Int) -> Unit,
    onAddQuestion: (Long, String, List<String>, Int, String) -> Unit,
    onDeleteCourse: (Long) -> Unit,
    onStudy: (Long) -> Unit
) {
    val context = LocalContext.current
    var picked by remember { mutableStateOf<PickedMaterial?>(null) }
    var questionChapter by remember { mutableStateOf<StudyChapter?>(null) }
    var deleteMaterial by remember { mutableStateOf<CourseMaterial?>(null) }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            runCatching { context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION) }
            val name = context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) cursor.getString(0) else null
            } ?: uri.lastPathSegment ?: "Course material"
            picked = PickedMaterial(name, uri.toString(), context.contentResolver.getType(uri) ?: "application/octet-stream")
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Eyebrow("Course library")
                Text("Turn material into a study path", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                Text(
                    "Import notes, confirm the chapter plan, study each topic, then complete a short mastery challenge.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Button(
                    onClick = { launcher.launch(arrayOf("application/pdf", "application/vnd.openxmlformats-officedocument.wordprocessingml.document", "application/vnd.openxmlformats-officedocument.presentationml.presentation", "text/plain", "application/zip")) },
                    enabled = !busy,
                    modifier = Modifier.fillMaxWidth().heightIn(min = 50.dp)
                ) {
                    Icon(Icons.Rounded.UploadFile, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Upload course material")
                }
            }
        }

        item {
            NoteCard(
                title = "Hybrid question bank",
                text = "Choose a chapter to study. After you finish its scheduled session, Gemini creates a short quiz from this material and Ora Rescue caches it for offline review.",
                icon = Icons.Rounded.AutoAwesome
            )
        }

        if (snapshot.courseMaterials.isEmpty()) {
            item {
                EmptyStateCard(
                    icon = Icons.Rounded.LibraryBooks,
                    title = "No course material yet",
                    description = "Upload the Machine Learning pack to create chapter topics and curated quizzes. Other files start with one editable study chapter.",
                    actionText = "Choose a file",
                    onAction = { launcher.launch(arrayOf("application/pdf", "application/vnd.openxmlformats-officedocument.wordprocessingml.document", "application/vnd.openxmlformats-officedocument.presentationml.presentation", "text/plain", "application/zip")) }
                )
            }
        } else {
            items(snapshot.courseMaterials, key = { it.id }) { material ->
                val chapters = snapshot.chapters.filter { it.materialId == material.id }
                OutlinedCard(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.outlinedCardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Rounded.MenuBook, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(Modifier.width(10.dp))
                            Column(Modifier.weight(1f)) {
                                Text(material.name, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                                Text("${chapters.size} chapters", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
                            }
                            IconButton(onClick = { deleteMaterial = material }, enabled = !busy) {
                                Icon(Icons.Rounded.DeleteOutline, contentDescription = "Delete ${material.name}")
                            }
                        }
                        chapters.forEach { chapter ->
                            val task = snapshot.tasks.firstOrNull { it.chapterId == chapter.id }
                            val questions = snapshot.questionsForChapter(chapter.id)
                            val attempt = snapshot.bestAttempt(chapter.id)
                            HorizontalDivider()
                            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                Column(Modifier.weight(1f)) {
                                    Text(chapter.title, fontWeight = FontWeight.SemiBold)
                                    val result = attempt?.let { " · ${QuizScoring.masteryLabel(it.score, it.total)} ${"★".repeat(QuizScoring.stars(it.score, it.total))}" } ?: ""
                                    Text("${questions.size} questions$result", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                IconButton(onClick = { questionChapter = chapter }, enabled = !busy) {
                                    Icon(Icons.Rounded.AddCircleOutline, contentDescription = "Add question")
                                }
                                FilledTonalButton(onClick = { task?.id?.let(onStudy) }, enabled = task != null && !busy) {
                                    Text(if (chapter.id == selectedChapterId) "Selected" else "Study")
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    picked?.let { material ->
        ImportCourseDialog(material, busy, onDismiss = { picked = null }) { exam, minutes ->
            onImport(material.name, material.uri, material.mimeType, exam, minutes)
            picked = null
        }
    }
    questionChapter?.let { chapter ->
        AddQuestionDialog(chapter.title, onDismiss = { questionChapter = null }) { prompt, options, answer, explanation ->
            onAddQuestion(chapter.id, prompt, options, answer, explanation)
            questionChapter = null
        }
    }
    deleteMaterial?.let { material ->
        AlertDialog(
            onDismissRequest = { deleteMaterial = null },
            title = { Text("Delete course?") },
            text = { Text("This removes ${material.name}, its chapter topics, pending sessions and quiz history. Courses with completed study history are protected and cannot be deleted.") },
            confirmButton = { Button(onClick = { onDeleteCourse(material.id); deleteMaterial = null }, enabled = !busy) { Text("Delete course") } },
            dismissButton = { TextButton(onClick = { deleteMaterial = null }) { Text("Cancel") } }
        )
    }
}

@Composable
private fun ImportCourseDialog(material: PickedMaterial, busy: Boolean, onDismiss: () -> Unit, onImport: (Long, Int) -> Unit) {
    val today = LocalDate.now().toEpochDay()
    var examDay by rememberSaveable { mutableLongStateOf(today + 30) }
    var minutes by rememberSaveable { mutableStateOf("60") }
    var datePicker by remember { mutableStateOf(false) }
    val chapters = CourseTemplates.chaptersFor(material.name)
    val parsed = minutes.toIntOrNull()
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Create chapter plan") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(material.name, fontWeight = FontWeight.SemiBold)
                Text("${chapters.size} chapter${if (chapters.size == 1) "" else "s"} detected", color = MaterialTheme.colorScheme.primary)
                chapters.take(10).forEachIndexed { index, title -> Text("${index + 1}. $title", style = MaterialTheme.typography.bodySmall) }
                OutlinedButton(onClick = { datePicker = true }, modifier = Modifier.fillMaxWidth()) { Text("Exam · ${dateWithYear(examDay)}") }
                OutlinedTextField(
                    value = minutes,
                    onValueChange = { if (it.length <= 4) minutes = it.filter(Char::isDigit) },
                    label = { Text("Minutes per chapter") },
                    supportingText = { Text("You can edit individual topics later.") },
                    isError = parsed == null || parsed !in 15..1440,
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = { Button(onClick = { onImport(examDay, parsed ?: 60) }, enabled = !busy && parsed != null && parsed in 15..1440 && examDay > today) { Text("Create study path") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
    if (datePicker) ChooseDateDialog(examDay, minDay = today + 1, onDismiss = { datePicker = false }) { examDay = it; datePicker = false }
}

@Composable
private fun AddQuestionDialog(title: String, onDismiss: () -> Unit, onSave: (String, List<String>, Int, String) -> Unit) {
    var prompt by rememberSaveable { mutableStateOf("") }
    val answers = remember { mutableStateListOf("", "", "", "") }
    var correct by rememberSaveable { mutableIntStateOf(0) }
    var explanation by rememberSaveable { mutableStateOf("") }
    val valid = prompt.isNotBlank() && answers.all { it.isNotBlank() }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add question · $title") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(prompt, { prompt = it }, label = { Text("Question") }, modifier = Modifier.fillMaxWidth())
                answers.forEachIndexed { index, answer ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(selected = correct == index, onClick = { correct = index })
                        OutlinedTextField(answer, { answers[index] = it }, label = { Text("Answer ${index + 1}${if (correct == index) " · correct" else ""}") }, modifier = Modifier.weight(1f))
                    }
                }
                OutlinedTextField(explanation, { explanation = it }, label = { Text("Explanation (optional)") }, modifier = Modifier.fillMaxWidth())
            }
        },
        confirmButton = { Button(onClick = { onSave(prompt, answers.toList(), correct, explanation) }, enabled = valid) { Text("Add question") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}
