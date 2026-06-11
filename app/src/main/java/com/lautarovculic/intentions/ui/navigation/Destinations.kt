package com.lautarovculic.intentions.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.ui.graphics.vector.ImageVector

object Routes {
    const val DASHBOARD = "dashboard"
    const val APPS = "apps"
    const val BUILDER = "builder"
    const val CAPTURE = "capture"
    const val HISTORY = "history"
    const val CONSOLE = "console"
    const val PROVIDER = "provider"
    const val FUZZER = "fuzzer"
    const val COMPONENTS = "components"
    fun components(pkg: String) = "$COMPONENTS/$pkg"
}

data class BottomItem(val route: String, val label: String, val icon: ImageVector)

val BottomItems = listOf(
    BottomItem(Routes.DASHBOARD, "Dash", Icons.Filled.Dashboard),
    BottomItem(Routes.APPS, "Apps", Icons.Filled.Apps),
    BottomItem(Routes.BUILDER, "Builder", Icons.Filled.Build),
    BottomItem(Routes.CAPTURE, "Capture", Icons.Filled.Wifi),
    BottomItem(Routes.HISTORY, "History", Icons.AutoMirrored.Filled.List),
)
