package uz.hijriy.app.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import uz.hijriy.app.HijriyApp

private data class Tab(val route: String, val label: String, val icon: ImageVector)

private val tabs = listOf(
    Tab("home", "Asosiy", Icons.Filled.Home),
    Tab("prayer", "Namoz", Icons.Filled.Schedule),
    Tab("quran", "Qur'on", Icons.AutoMirrored.Filled.MenuBook),
    Tab("calendar", "Taqvim", Icons.Filled.CalendarMonth),
    Tab("more", "Yana", Icons.Filled.MoreHoriz),
)

fun NavHostController.go(route: String) = navigate(route) { launchSingleTop = true }

@Composable
fun AppRoot(app: HijriyApp) {
    val nav = rememberNavController()
    val entry by nav.currentBackStackEntryAsState()
    val route = entry?.destination?.route
    val showBar = tabs.any { it.route == route }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            if (showBar) NavigationBar(containerColor = MaterialTheme.colorScheme.surfaceContainerLow) {
                tabs.forEach { t ->
                    NavigationBarItem(
                        selected = route == t.route,
                        onClick = {
                            nav.navigate(t.route) {
                                popUpTo(nav.graph.findStartDestination().id) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = { Icon(t.icon, t.label) },
                        label = { Text(t.label) }
                    )
                }
            }
        }
    ) { pad ->
        Box(Modifier.fillMaxSize().padding(bottom = if (showBar) pad.calculateBottomPadding() else androidx.compose.ui.unit.Dp(0f))) {
            NavHost(nav, startDestination = "home") {
                composable("home") { HomeScreen(app, nav) }
                composable("prayer") { PrayerScreen(app, nav) }
                composable("quran") { QuranListScreen(app, nav) }
                composable("calendar") { CalendarScreen(app, nav, 0) }
                composable("more") { MoreScreen(nav) }
                composable("converter") { CalendarScreen(app, nav, 2, standalone = true) }
                composable("weather") { WeatherScreen(app, nav) }
                composable("qibla") { QiblaScreen(app, nav) }
                composable("settings") { SettingsScreen(app, nav) }
                composable("location") { LocationScreen(app, nav) }
                composable("names") { NamesScreen(nav) }
                composable("tasbeh") { TasbehScreen(app, nav) }
                composable(
                    "mushaf/{page}",
                    arguments = listOf(navArgument("page") { type = NavType.IntType })
                ) { e -> MushafScreen(app, nav, e.arguments?.getInt("page") ?: 1) }
                composable(
                    "reader/{sura}?ayah={ayah}",
                    arguments = listOf(
                        navArgument("sura") { type = NavType.IntType },
                        navArgument("ayah") { type = NavType.IntType; defaultValue = 1 },
                    )
                ) { e ->
                    ReaderScreen(
                        app, nav,
                        e.arguments?.getInt("sura") ?: 1,
                        e.arguments?.getInt("ayah") ?: 1
                    )
                }
            }
        }
    }
}
