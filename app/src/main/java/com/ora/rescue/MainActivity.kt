@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.ora.rescue

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.*
import androidx.navigation.navArgument

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            OraRescueTheme {
                OraApp()
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

    fun home() = nav.navigate("home") {
        popUpTo("home") { inclusive = true }
        launchSingleTop = true
    }

    LaunchedEffect(vm) {
        vm.events.collect { event ->
            if (event.destination == "home") home()
            snackbars.showSnackbar(event.message)
        }
    }

    Scaffold(
        containerColor = OraCreamPaper,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "ora rescue",
                            fontFamily = FontFamily.Serif,
                            fontWeight = FontWeight.Bold,
                            color = OraRoseDark
                        )
                        Text(
                            text = "A little room to begin again",
                            style = MaterialTheme.typography.labelSmall,
                            color = OraPlumMuted
                        )
                    }
                },
                navigationIcon = {
                    if (route != "home") {
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
                            color = OraRose
                        )
                    }
                    Spacer(Modifier.width(16.dp))
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = OraCreamPaper)
            )
        },
        snackbarHost = { SnackbarHost(snackbars) },
        bottomBar = {
            if (route == "home" || route == "recover") {
                NavigationBar(containerColor = Color(0xFFF7ECF0)) {
                    NavigationBarItem(
                        selected = route == "home",
                        onClick = { if (!busy) home() },
                        icon = { Icon(Icons.Rounded.Home, contentDescription = null) },
                        label = { Text("Study desk") },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = OraRose,
                            selectedTextColor = OraRose,
                            indicatorColor = OraRoseLight
                        )
                    )
                    NavigationBarItem(
                        selected = route == "recover",
                        onClick = {
                            if (!busy) nav.navigate("recover") { launchSingleTop = true }
                        },
                        icon = { Icon(Icons.Rounded.AutoAwesome, contentDescription = null) },
                        label = { Text("Recover plan") },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = OraRose,
                            selectedTextColor = OraRose,
                            indicatorColor = OraRoseLight
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
                        color = OraRose
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
                                onRecover = { nav.navigate("recover") },
                                onAvailability = vm::setAvailability,
                                onProgress = vm::progress
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
                                onAvailability = { home() }
                            )
                        }
                    }
                }
            }
        }
    }
}
