package com.lautarovculic.intentions

import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.lautarovculic.intentions.ui.navigation.BottomItems
import com.lautarovculic.intentions.ui.navigation.Routes
import com.lautarovculic.intentions.ui.screens.apps.AppsScreen
import com.lautarovculic.intentions.ui.screens.builder.BuilderScreen
import com.lautarovculic.intentions.ui.screens.capture.CaptureScreen
import com.lautarovculic.intentions.ui.screens.components.ComponentsScreen
import com.lautarovculic.intentions.ui.screens.console.RootConsoleScreen
import com.lautarovculic.intentions.ui.screens.dashboard.DashboardScreen
import com.lautarovculic.intentions.ui.screens.fuzzer.FuzzerScreen
import com.lautarovculic.intentions.ui.screens.history.HistoryScreen
import com.lautarovculic.intentions.ui.screens.provider.ProviderLabScreen
import com.lautarovculic.intentions.ui.theme.IntentionsTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        // Best-effort notification permission for the capture foreground service.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerForActivityResult(ActivityResultContracts.RequestPermission()) {}
                .launch(android.Manifest.permission.POST_NOTIFICATIONS)
        }
        setContent { IntentionsTheme { IntentionsApp() } }
    }
}

@Composable
private fun IntentionsApp() {
    val navController = rememberNavController()
    val backStack by navController.currentBackStackEntryAsState()
    val currentRoute = backStack?.destination?.route

    val openRoute: (String) -> Unit = { route ->
        navController.navigate(route) { launchSingleTop = true }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            NavigationBar {
                BottomItems.forEach { item ->
                    val selected = currentRoute == item.route
                    NavigationBarItem(
                        selected = selected,
                        onClick = {
                            if (!selected) {
                                navController.navigate(item.route) {
                                    popUpTo(Routes.DASHBOARD) { inclusive = false }
                                    launchSingleTop = true
                                }
                            }
                        },
                        icon = { Icon(item.icon, contentDescription = item.label) },
                        label = { Text(item.label) },
                    )
                }
            }
        },
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = Routes.DASHBOARD,
            modifier = Modifier.padding(padding),
        ) {
            composable(Routes.DASHBOARD) { DashboardScreen(onNavigate = openRoute) }
            composable(Routes.APPS) {
                AppsScreen(onOpenApp = { pkg -> openRoute(Routes.components(pkg)) })
            }
            composable(
                route = "${Routes.COMPONENTS}/{package}",
                arguments = listOf(navArgument("package") { type = NavType.StringType }),
            ) { entry ->
                ComponentsScreen(
                    packageName = entry.arguments?.getString("package").orEmpty(),
                    onBack = { navController.popBackStack() },
                    onNavigate = openRoute,
                )
            }
            composable(Routes.BUILDER) { BuilderScreen(onNavigate = openRoute) }
            composable(Routes.CAPTURE) { CaptureScreen(onNavigate = openRoute) }
            composable(Routes.HISTORY) { HistoryScreen(onNavigate = openRoute) }
            composable(Routes.CONSOLE) { RootConsoleScreen() }
            composable(Routes.PROVIDER) { ProviderLabScreen() }
            composable(Routes.FUZZER) { FuzzerScreen() }
        }
    }
}
