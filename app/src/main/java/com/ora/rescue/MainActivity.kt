@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.ora.rescue

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.enableEdgeToEdge
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.*
import androidx.navigation.navArgument

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val vm: StudyViewModel = viewModel()
            val themePreference by vm.themePreference.collectAsStateWithLifecycle()
            OraRescueTheme(themePreference = themePreference) {
                val dark = LocalOraAccents.current.dark
                SideEffect {
                    WindowCompat.getInsetsController(window, window.decorView).apply {
                        isAppearanceLightStatusBars = !dark
                        isAppearanceLightNavigationBars = !dark
                    }
                }
                OraApp(vm)
            }
        }
    }
}
@Composable
private fun OraApp(vm: StudyViewModel = viewModel()) {
    val state by vm.state.collectAsStateWithLifecycle()
    val busy by vm.busy.collectAsStateWithLifecycle()
    val preview by vm.preview.collectAsStateWithLifecycle()
    val selectedChapterId by vm.selectedChapterId.collectAsStateWithLifecycle()
    val nav = rememberNavController()
    val backStack by nav.currentBackStackEntryAsState()
    val route = backStack?.destination?.route ?: "home"
    val snackbars = remember { SnackbarHostState() }

    fun navigateTab(destination: String) {
        nav.navigate(destination) {
            popUpTo(nav.graph.findStartDestination().id) {
                saveState = true
            }
            launchSingleTop = true
            restoreState = true
        }
    }

    LaunchedEffect(vm) {
        vm.events.collect { event ->
            event.destination?.let { destination ->
                if (destination in setOf("home", "calendar", "recover", "profile")) {
                    navigateTab(destination)
                } else {
                    nav.navigate(destination) { launchSingleTop = true }
                }
            }
            snackbars.showSnackbar(event.message)
        }
    }

    val isTopicDetail = route == "topic/{id}"
    val isTopicForm = route == "task/{id}"
    val isCourses = route == "courses"
    val isQuiz = route.startsWith("quiz/")
    val isAuxiliary = isTopicForm || isTopicDetail || isQuiz
    val isAddingTopic = isTopicForm && backStack?.arguments?.getLong("id") == 0L
    val cs = MaterialTheme.colorScheme

    Scaffold(
        containerColor = cs.background,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = when {
                            isAddingTopic -> "Add topic"
                            isTopicForm -> "Edit topic"
                            isTopicDetail -> "Topic details"
                            isCourses -> "Course library"
                            isQuiz -> "Chapter challenge"
                            else -> "ora rescue"
                        },
                        fontWeight = FontWeight.Bold,
                        color = cs.primary
                    )
                },
                navigationIcon = {
                    if (isAuxiliary) {
                        IconButton(
                            onClick = { nav.popBackStack() },
                            enabled = !busy,
                            modifier = Modifier.size(48.dp)
                        ) {
                            Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Navigate back")
                        }
                    }
                },
                actions = {
                    if (busy) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.5.dp,
                            color = cs.primary
                        )
                    }
                    if (route == "home") {
                        IconButton(onClick = { nav.navigate("task/0") { launchSingleTop = true } }, enabled = !busy) {
                            Icon(Icons.Rounded.AddTask, contentDescription = "Add task without course material")
                        }
                        IconButton(onClick = { nav.navigate("courses") { launchSingleTop = true } }) {
                            Icon(Icons.Rounded.LibraryBooks, contentDescription = "Open course library")
                        }
                    }
                    Spacer(Modifier.width(16.dp))
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = cs.background)
            )
        },
        snackbarHost = { SnackbarHost(snackbars) },
        bottomBar = {
            if (!isQuiz) NavigationBar(
                containerColor = cs.surface.copy(alpha = 0.97f),
                tonalElevation = 6.dp
            ) {
                NavigationBarItem(
                    selected = route == "home",
                    onClick = { navigateTab("home") },
                    icon = { Icon(Icons.Rounded.Home, contentDescription = null) },
                    label = { Text("Home", style = MaterialTheme.typography.labelSmall) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = cs.primary,
                        selectedTextColor = cs.primary,
                        indicatorColor = cs.primaryContainer
                    )
                )

                NavigationBarItem(
                    selected = route == "calendar",
                    onClick = { navigateTab("calendar") },
                    icon = { Icon(Icons.Rounded.CalendarMonth, contentDescription = null) },
                    label = { Text("Calendar", style = MaterialTheme.typography.labelSmall) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = cs.primary,
                        selectedTextColor = cs.primary,
                        indicatorColor = cs.primaryContainer
                    )
                )

                NavigationBarItem(
                    selected = route == "courses",
                    onClick = { nav.navigate("courses") { launchSingleTop = true } },
                    icon = { Icon(Icons.Rounded.LibraryBooks, contentDescription = null) },
                    label = { Text("Material", style = MaterialTheme.typography.labelSmall) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = cs.primary,
                        selectedTextColor = cs.primary,
                        indicatorColor = cs.primaryContainer
                    )
                )

                NavigationBarItem(
                    selected = route == "recover",
                    onClick = { navigateTab("recover") },
                    icon = { Icon(Icons.Rounded.Schedule, contentDescription = null) },
                    label = { Text("Plan", style = MaterialTheme.typography.labelSmall) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = cs.primary,
                        selectedTextColor = cs.primary,
                        indicatorColor = cs.primaryContainer
                    )
                )

                NavigationBarItem(
                    selected = route == "profile",
                    onClick = { navigateTab("profile") },
                    icon = { Icon(Icons.Rounded.Person, contentDescription = null) },
                    label = { Text("Profile", style = MaterialTheme.typography.labelSmall) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = cs.primary,
                        selectedTextColor = cs.primary,
                        indicatorColor = cs.primaryContainer
                    )
                )
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .padding(padding)
                .consumeWindowInsets(padding)
                .fillMaxSize()
        ) {
            when {
                state.loading -> {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center),
                        color = cs.primary
                    )
                }
                state.error != null -> {
                    Column(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text(
                            text = state.error ?: "Unable to load data.",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.error
                        )
                        Button(onClick = vm::reload) {
                            Text("Try Again")
                        }
                    }
                }
                else -> {
                    NavHost(nav, startDestination = "home") {
                        composable("home") {
                            HomeScreen(
                                snapshot = state.snapshot,
                                busy = busy,
                                onAdd = { nav.navigate("task/0") { launchSingleTop = true } },
                                onOpenTopic = { nav.navigate("topic/$it") { launchSingleTop = true } },
                                onRecover = { navigateTab("recover") },
                                onCalendar = { navigateTab("calendar") },
                                onProgress = vm::progress,
                                onCompleteNow = vm::completeNow,
                                onSkip = { id ->
                                    vm.progress(id, 0)
                                    navigateTab("recover")
                                }
                            )
                        }
                        composable("calendar") {
                            CalendarScreen(
                                snapshot = state.snapshot,
                                busy = busy,
                                onAddTimeWindow = vm::addTimeWindow,
                                onEditTimeWindow = vm::editTimeWindow,
                                onDeleteTimeWindow = vm::deleteTimeWindow,
                                onClearDayWindows = vm::clearTimeWindows,
                                onCopyWindows = vm::copyTimeWindows,
                                onProgress = vm::progress,
                                onOpenTopic = { nav.navigate("topic/$it") { launchSingleTop = true } },
                                onNavigateToRecovery = { navigateTab("recover") }
                            )
                        }
                        composable(
                            route = "topic/{id}",
                            arguments = listOf(navArgument("id") { type = NavType.LongType })
                        ) { entry ->
                            val id = entry.arguments?.getLong("id") ?: 0L
                            TopicDetailScreen(
                                id = id,
                                snapshot = state.snapshot,
                                onEdit = { nav.navigate("task/$id") { launchSingleTop = true } },
                                onPractice = { nav.navigate("quiz/chapter/$it") },
                                onBack = { nav.popBackStack() }
                            )
                        }
                        composable(
                            route = "task/{id}",
                            arguments = listOf(navArgument("id") { type = NavType.LongType })
                        ) { entry ->
                            val id = entry.arguments?.getLong("id") ?: 0
                            TaskScreen(
                                id = id,
                                snapshot = state.snapshot,
                                busy = busy,
                                onSave = vm::save,
                                onDelete = vm::delete,
                                onBack = { nav.popBackStack() }
                            )
                        }
                        composable("recover") {
                            RecoveryScreen(
                                snapshot = state.snapshot,
                                preview = preview,
                                busy = busy,
                                onGenerate = vm::preview,
                                onCancel = vm::cancelPreview,
                                onAccept = vm::accept,
                                onAvailability = { navigateTab("calendar") }
                            )
                        }
                        composable("courses") {
                            CourseScreen(
                                snapshot = state.snapshot,
                                busy = busy,
                                selectedChapterId = selectedChapterId,
                                onImport = vm::importCourse,
                                onAddQuestion = vm::addStudentQuestion,
                                onDeleteCourse = vm::deleteCourse,
                                onStudy = { taskId ->
                                    vm.selectStudyChapter(taskId)
                                    nav.navigate("topic/$taskId")
                                }
                            )
                        }
                        composable(
                            route = "quiz/session/{id}",
                            arguments = listOf(navArgument("id") { type = NavType.LongType })
                        ) { entry ->
                            val sessionId = entry.arguments?.getLong("id") ?: 0L
                            val chapterId = state.snapshot.sessions.firstOrNull { it.id == sessionId }
                                ?.let { state.snapshot.chapterForTask(it.taskId)?.id } ?: 0L
                            QuizScreen(
                                chapterId = chapterId,
                                sessionId = sessionId,
                                snapshot = state.snapshot,
                                busy = busy,
                                onSave = vm::saveQuizAttempt,
                                onDone = { nav.popBackStack() }
                            )
                        }
                        composable(
                            route = "quiz/chapter/{id}",
                            arguments = listOf(navArgument("id") { type = NavType.LongType })
                        ) { entry ->
                            val chapterId = entry.arguments?.getLong("id") ?: 0L
                            QuizScreen(
                                chapterId = chapterId,
                                sessionId = null,
                                snapshot = state.snapshot,
                                busy = busy,
                                onSave = vm::saveQuizAttempt,
                                onDone = { nav.popBackStack() }
                            )
                        }
                        composable("profile") {
                            ProfileScreen(
                                snapshot = state.snapshot,
                                busy = busy,
                                themePreference = vm.themePreference.collectAsStateWithLifecycle().value,
                                onUpdateProfile = vm::updateProfile,
                                onSetTheme = vm::setThemePreference,
                                onNavigateToCalendar = { navigateTab("calendar") }
                            )
                        }
                    }
                }
            }
        }
    }
}
