@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.ora.rescue

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.enableEdgeToEdge
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.draw.clip
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
            if (event.destination == "home") {
                navigateTab("home")
            }
            snackbars.showSnackbar(event.message)
        }
    }

    val isTopLevelDestination = route in listOf("home", "calendar", "recover", "profile")
    val cs = MaterialTheme.colorScheme
    val accents = LocalOraAccents.current

    Scaffold(
        containerColor = cs.background,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "ora rescue",
                            fontFamily = OraFontFamily,
                            fontWeight = FontWeight.Bold,
                            color = cs.primary
                        )
                        Text(
                            text = when (route) {
                                "calendar" -> "Your study schedule"
                                "recover"  -> "A fresh start"
                                "profile"  -> "Your personal space"
                                else       -> "Make today productive"
                            },
                            style = MaterialTheme.typography.labelSmall,
                            color = cs.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    if (!isTopLevelDestination) {
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
                    Spacer(Modifier.width(16.dp))
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = cs.background)
            )
        },
        snackbarHost = { SnackbarHost(snackbars) },
        bottomBar = {
            if (isTopLevelDestination) {
                NavigationBar(
                    containerColor = cs.surface.copy(alpha = 0.97f),
                    tonalElevation = 6.dp
                ) {
                    NavigationBarItem(
                        selected = route == "home",
                        onClick = { navigateTab("home") },
                        icon = { Icon(Icons.Rounded.Home, contentDescription = "Home") },
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
                        icon = { Icon(Icons.Rounded.CalendarMonth, contentDescription = "Calendar") },
                        label = { Text("Calendar", style = MaterialTheme.typography.labelSmall) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = cs.primary,
                            selectedTextColor = cs.primary,
                            indicatorColor = cs.primaryContainer
                        )
                    )

                    // Prominent Center Add Topic Action
                    IconButton(
                        onClick = {
                            if (!busy) nav.navigate("task/0") { launchSingleTop = true }
                        },
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(
                                accents.banner
                            )
                    ) {
                        Icon(
                            Icons.Rounded.Add,
                            contentDescription = "Add study topic",
                            tint = accents.onBanner,
                            modifier = Modifier.size(26.dp)
                        )
                    }

                    NavigationBarItem(
                        selected = route == "recover",
                        onClick = { navigateTab("recover") },
                        icon = { Icon(Icons.Rounded.AutoAwesome, contentDescription = "Recovery") },
                        label = { Text("Recovery", style = MaterialTheme.typography.labelSmall) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = cs.primary,
                            selectedTextColor = cs.primary,
                            indicatorColor = cs.primaryContainer
                        )
                    )

                    NavigationBarItem(
                        selected = route == "profile",
                        onClick = { navigateTab("profile") },
                        icon = { Icon(Icons.Rounded.Person, contentDescription = "Profile") },
                        label = { Text("Profile", style = MaterialTheme.typography.labelSmall) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = cs.primary,
                            selectedTextColor = cs.primary,
                            indicatorColor = cs.primaryContainer
                        )
                    )
                }
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .padding(padding)
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
                                onAdd = { nav.navigate("task/0") },
                                onEdit = { nav.navigate("task/$it") },
                                onRecover = { navigateTab("recover") },
                                onCalendar = { navigateTab("calendar") },
                                onProgress = vm::progress
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
                                onEditTopic = { nav.navigate("task/$it") },
                                onNavigateToRecovery = { navigateTab("recover") }
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
