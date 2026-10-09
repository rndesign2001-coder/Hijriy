package uz.hijriy.app.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import uz.hijriy.app.ui.theme.LocalExtra
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
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
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
    var intro by androidx.compose.runtime.saveable.rememberSaveable { androidx.compose.runtime.mutableStateOf(true) }
    Box(Modifier.fillMaxSize()) {
        AppContent(app)
        if (intro) IntroScreen { intro = false }
    }
}

@Composable
private fun AppContent(app: HijriyApp) {
    val nav = rememberNavController()
    val pending by app.pendingRoute.collectAsState()
    androidx.compose.runtime.LaunchedEffect(pending) {
        pending?.let { r -> runCatching { nav.navigate(r) { launchSingleTop = true } }; app.pendingRoute.value = null }
    }
    val entry by nav.currentBackStackEntryAsState()
    val route = entry?.destination?.route
    val showBar = tabs.any { it.route == route }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            if (showBar) PremiumBottomBar(route) { r ->
                nav.navigate(r) {
                    popUpTo(nav.graph.findStartDestination().id) { saveState = true }
                    launchSingleTop = true
                    restoreState = true
                }
            }
        }
    ) { pad ->
        Box(Modifier.fillMaxSize().padding(bottom = if (showBar) (pad.calculateBottomPadding() - 20.dp).coerceAtLeast(0.dp) else 0.dp)) {
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
                composable("wallpaper") { WallpaperScreen(app, nav) }
                composable("duas") { DuasScreen(app, nav) }
                composable("qazo") { QazoScreen(app, nav) }
                composable("search") { SearchScreen(app, nav) }
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

// ------------------------------ Pastki panel ------------------------------

/** Suzuvchi, yumaloq pastki panel: tanlangan bo'lim uchun animatsiyali "tabletka", o'rtada ko'tarilgan Qur'on tugmasi. */
@Composable
private fun PremiumBottomBar(route: String?, onSelect: (String) -> Unit) {
    val ex = LocalExtra.current
    val cs = MaterialTheme.colorScheme
    val shape = RoundedCornerShape(30.dp)
    Box(
        Modifier.fillMaxWidth().navigationBarsPadding()
            .padding(horizontal = 14.dp).padding(bottom = 10.dp)
            .height(88.dp)
    ) {
        Box(
            Modifier.align(Alignment.BottomCenter).fillMaxWidth().height(68.dp)
                .shadow(if (ex.dark) 6.dp else 16.dp, shape, ambientColor = Color(0x33000000), spotColor = cs.primary.copy(alpha = 0.35f))
                .clip(shape)
                .background(
                    if (ex.dark) Brush.verticalGradient(listOf(cs.surfaceContainerHighest, cs.surfaceContainerHigh))
                    else Brush.verticalGradient(listOf(Color.White, cs.surfaceContainerLow))
                )
                .border(1.dp, cs.outlineVariant.copy(alpha = if (ex.dark) 0.5f else 0.6f), shape)
        )
        Row(Modifier.fillMaxSize().padding(horizontal = 6.dp), verticalAlignment = Alignment.Bottom) {
            tabs.forEachIndexed { i, t ->
                val sel = route == t.route
                if (i == tabs.size / 2) CenterTab(t, sel, Modifier.weight(1f)) { onSelect(t.route) }
                else BarTab(t, sel, Modifier.weight(1f)) { onSelect(t.route) }
            }
        }
    }
}

@Composable
private fun BarTab(t: Tab, selected: Boolean, modifier: Modifier, onClick: () -> Unit) {
    val cs = MaterialTheme.colorScheme
    val ex = LocalExtra.current
    val src = remember { MutableInteractionSource() }
    val pillW by animateDpAsState(if (selected) 54.dp else 30.dp, spring(dampingRatio = 0.6f, stiffness = 500f), label = "pill")
    val pillA by animateFloatAsState(if (selected) 1f else 0f, tween(250), label = "pillA")
    val iconScale by animateFloatAsState(if (selected) 1.08f else 1f, spring(dampingRatio = 0.4f), label = "ic")
    val fg by animateColorAsState(if (selected) cs.primary else cs.onSurfaceVariant.copy(alpha = 0.85f), tween(250), label = "fg")
    Column(
        modifier.height(68.dp)
            .clip(RoundedCornerShape(22.dp))
            .clickable(src, null, role = Role.Tab, onClick = onClick)
            .semantics { this.selected = selected },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            Modifier.width(pillW).height(32.dp)
                .graphicsLayer { alpha = 0.999f }
                .clip(RoundedCornerShape(16.dp))
                .background(
                    Brush.horizontalGradient(
                        listOf(cs.primary.copy(alpha = 0.16f * pillA), ex.palette.accent.copy(alpha = 0.22f * pillA))
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(t.icon, null, tint = fg, modifier = Modifier.size(23.dp).graphicsLayer { scaleX = iconScale; scaleY = iconScale })
        }
        Spacer(Modifier.height(3.dp))
        Text(
            t.label, color = fg, fontSize = 11.5.sp, maxLines = 1,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
        )
    }
}

@Composable
private fun CenterTab(t: Tab, selected: Boolean, modifier: Modifier, onClick: () -> Unit) {
    val cs = MaterialTheme.colorScheme
    val ex = LocalExtra.current
    val src = remember { MutableInteractionSource() }
    val pressed by src.collectIsPressedAsState()
    val scale by animateFloatAsState(if (pressed) 0.9f else if (selected) 1.05f else 1f, spring(dampingRatio = 0.45f), label = "cs")
    val ring by animateColorAsState(if (selected) Color(0xFFE9C46A) else Color.White.copy(alpha = 0.7f), tween(300), label = "ring")
    Column(
        modifier.height(88.dp)
            .clickable(src, null, role = Role.Tab, onClick = onClick)
            .semantics { this.selected = selected },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Bottom
    ) {
        Box(
            Modifier.size(58.dp)
                .graphicsLayer { scaleX = scale; scaleY = scale }
                .shadow(10.dp, CircleShape, spotColor = cs.primary.copy(alpha = 0.6f), ambientColor = cs.primary.copy(alpha = 0.4f))
                .clip(CircleShape)
                .background(Brush.linearGradient(listOf(ex.palette.accent, cs.primary, ex.palette.primaryDark)))
                .border(2.5.dp, ring, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(t.icon, null, tint = Color.White, modifier = Modifier.size(28.dp))
        }
        Spacer(Modifier.height(4.dp))
        Text(
            t.label, color = if (selected) cs.primary else cs.onSurfaceVariant.copy(alpha = 0.85f), fontSize = 11.5.sp, maxLines = 1,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
        )
        Spacer(Modifier.height(9.dp))
    }
}
