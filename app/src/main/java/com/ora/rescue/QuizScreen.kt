@file:OptIn(androidx.compose.animation.ExperimentalAnimationApi::class)

package com.ora.rescue

import android.media.AudioManager
import android.media.ToneGenerator
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

@Composable
fun QuizScreen(
    chapterId: Long,
    sessionId: Long?,
    snapshot: StudySnapshot,
    busy: Boolean,
    onSave: (Long, Long?, Int, Int) -> Unit,
    onDone: () -> Unit
) {
    val chapter = snapshot.chapters.firstOrNull { it.id == chapterId }
    val available = snapshot.questionsForChapter(chapterId)
    val questions = remember(chapterId, available.map { it.id }) { available.shuffled().take(5) }
    var index by rememberSaveable(chapterId) { mutableIntStateOf(0) }
    var score by rememberSaveable(chapterId) { mutableIntStateOf(0) }
    var combo by rememberSaveable(chapterId) { mutableIntStateOf(0) }
    var hearts by rememberSaveable(chapterId) { mutableIntStateOf(3) }
    var selected by rememberSaveable(chapterId, index) { mutableIntStateOf(-1) }
    var checked by rememberSaveable(chapterId, index) { mutableStateOf(false) }
    var finished by rememberSaveable(chapterId) { mutableStateOf(false) }
    var muted by rememberSaveable { mutableStateOf(false) }
    var reducedMotion by rememberSaveable { mutableStateOf(false) }
    val haptics = LocalHapticFeedback.current
    val tone = remember { ToneGenerator(AudioManager.STREAM_MUSIC, 55) }
    DisposableEffect(Unit) { onDispose { tone.release() } }

    if (chapter == null || questions.isEmpty()) {
        Box(Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
            EmptyStateCard(
                icon = Icons.Rounded.Quiz,
                title = "Quiz not ready",
                description = "Add reviewed questions to this chapter from the Course Library before starting a challenge.",
                actionText = "Back to courses",
                onAction = onDone
            )
        }
        return
    }

    if (finished) {
        QuizResult(
            chapter = chapter,
            score = score,
            total = questions.size,
            reducedMotion = reducedMotion,
            busy = busy,
            onFinish = {
                onSave(chapterId, sessionId, score, questions.size)
                onDone()
            }
        )
        return
    }

    val question = questions[index]
    val correct = selected == question.correctIndex
    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            LinearProgressIndicator(
                progress = { (index + if (checked) 1f else 0f) / questions.size },
                modifier = Modifier.weight(1f).height(10.dp),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.primaryContainer
            )
            Spacer(Modifier.width(12.dp))
            Text("${index + 1}/${questions.size}", fontWeight = FontWeight.Bold)
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Surface(shape = RoundedCornerShape(14.dp), color = MaterialTheme.colorScheme.errorContainer) {
                Text("${"♥".repeat(hearts)}${"♡".repeat(3 - hearts)}", modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp), color = MaterialTheme.colorScheme.onErrorContainer)
            }
            if (combo >= 2) Surface(shape = RoundedCornerShape(14.dp), color = MaterialTheme.colorScheme.secondaryContainer) {
                Text("🔥 $combo combo", modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp), color = MaterialTheme.colorScheme.onSecondaryContainer)
            }
            Row {
                IconButton(onClick = { muted = !muted }) { Icon(if (muted) Icons.Rounded.VolumeOff else Icons.Rounded.VolumeUp, contentDescription = if (muted) "Turn sound on" else "Mute quiz") }
                IconButton(onClick = { reducedMotion = !reducedMotion }) { Icon(Icons.Rounded.MotionPhotosOff, contentDescription = "Toggle reduced motion") }
            }
        }
        Eyebrow(chapter.title)
        AnimatedContent(targetState = question.id, transitionSpec = { fadeIn() togetherWith fadeOut() }, label = "question") {
            Text(question.prompt, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        }
        Text("Choose the best answer", color = MaterialTheme.colorScheme.onSurfaceVariant)

        question.optionList().forEachIndexed { optionIndex, option ->
            val chosen = selected == optionIndex
            val isAnswer = optionIndex == question.correctIndex
            val container = when {
                checked && isAnswer -> MaterialTheme.colorScheme.tertiaryContainer
                checked && chosen && !isAnswer -> MaterialTheme.colorScheme.errorContainer
                chosen -> MaterialTheme.colorScheme.primaryContainer
                else -> MaterialTheme.colorScheme.surface
            }
            val answerScale by animateFloatAsState(
                targetValue = if (!reducedMotion && checked && (chosen || isAnswer)) 1.025f else 1f,
                animationSpec = spring(),
                label = "answer feedback"
            )
            OutlinedCard(
                modifier = Modifier.fillMaxWidth().scale(answerScale).clickable(enabled = !checked) { selected = optionIndex },
                colors = CardDefaults.outlinedCardColors(containerColor = container),
                shape = RoundedCornerShape(18.dp)
            ) {
                Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Surface(shape = RoundedCornerShape(10.dp), color = MaterialTheme.colorScheme.surfaceVariant) {
                        Text(('A'.code + optionIndex).toChar().toString(), modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp), fontWeight = FontWeight.Bold)
                    }
                    Spacer(Modifier.width(12.dp))
                    Text(option, modifier = Modifier.weight(1f), fontWeight = if (chosen) FontWeight.SemiBold else FontWeight.Normal)
                    if (checked && isAnswer) Icon(Icons.Rounded.CheckCircle, contentDescription = "Correct answer", tint = MaterialTheme.colorScheme.tertiary)
                    if (checked && chosen && !isAnswer) Icon(Icons.Rounded.Cancel, contentDescription = "Incorrect answer", tint = MaterialTheme.colorScheme.error)
                }
            }
        }

        AnimatedVisibility(visible = checked, enter = if (reducedMotion) fadeIn() else fadeIn() + scaleIn()) {
            NoteCard(
                title = if (correct) "Correct!${if (combo >= 2) " $combo in a row" else ""}" else "Good try — learn from this one",
                text = question.explanation,
                icon = if (correct) Icons.Rounded.Celebration else Icons.Rounded.Lightbulb,
                containerColor = if (correct) MaterialTheme.colorScheme.tertiaryContainer else MaterialTheme.colorScheme.errorContainer
            )
        }

        Button(
            onClick = {
                if (!checked) {
                    checked = true
                    if (correct) {
                        score++
                        combo++
                        if (!muted) tone.startTone(ToneGenerator.TONE_PROP_ACK, 120)
                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                    } else {
                        combo = 0
                        hearts = (hearts - 1).coerceAtLeast(0)
                        if (!muted) tone.startTone(ToneGenerator.TONE_PROP_NACK, 180)
                    }
                } else if (index == questions.lastIndex) {
                    finished = true
                } else {
                    index++
                }
            },
            enabled = selected >= 0,
            modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp)
        ) {
            Text(if (!checked) "Check answer" else if (index == questions.lastIndex) "See results" else "Next challenge")
        }
        Text("Question source · ${question.source}", modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun QuizResult(chapter: StudyChapter, score: Int, total: Int, reducedMotion: Boolean, busy: Boolean, onFinish: () -> Unit) {
    val stars = QuizScoring.stars(score, total)
    Column(
        modifier = Modifier.fillMaxSize().padding(28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        AnimatedVisibility(visible = true, enter = if (reducedMotion) fadeIn() else fadeIn() + scaleIn(initialScale = .65f)) {
            Surface(shape = RoundedCornerShape(36.dp), color = MaterialTheme.colorScheme.primaryContainer, modifier = Modifier.size(140.dp)) {
                Box(contentAlignment = Alignment.Center) { Icon(Icons.Rounded.EmojiEvents, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(72.dp)) }
            }
        }
        Spacer(Modifier.height(24.dp))
        Text(QuizScoring.masteryLabel(score, total), style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Text(chapter.title, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
        Spacer(Modifier.height(16.dp))
        Text(if (stars == 0) "☆☆☆" else "${"★".repeat(stars)}${"☆".repeat(3 - stars)}", style = MaterialTheme.typography.displaySmall, color = MaterialTheme.colorScheme.primary)
        Text("$score of $total correct", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(24.dp))
        Button(onClick = onFinish, enabled = !busy, modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp)) { Text("Save result") }
    }
}
