package org.rayista.subobus.ui

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/**
 * 一套可选主题。scheme 遵循 Material 3 颜色角色，App 内部只通过这些角色取色，
 * 不在界面里写死颜色。
 */
data class AppThemeOption(
    val id: String,
    val label: String,
    val scheme: ColorScheme,
    val isDark: Boolean
)

/* ---------------- 4 套亮色 ---------------- */
private val LightBlue = lightColorScheme(
    primary = Color(0xFF0061A4),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFD1E4FF),
    onPrimaryContainer = Color(0xFF001D36),
    secondary = Color(0xFF535F70),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFD7E3F7),
    onSecondaryContainer = Color(0xFF101C2B),
    tertiary = Color(0xFF6B5778),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFF2DAFF),
    onTertiaryContainer = Color(0xFF251431),
    error = Color(0xFFBA1A1A),
    onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF410002),
    background = Color(0xFFFDFCFF),
    onBackground = Color(0xFF1A1C1E),
    surface = Color(0xFFFDFCFF),
    onSurface = Color(0xFF1A1C1E),
    surfaceVariant = Color(0xFFDFE2EB),
    onSurfaceVariant = Color(0xFF43474E),
    outline = Color(0xFF73777F),
    outlineVariant = Color(0xFFC3C7CF)
)

private val LightTeal = lightColorScheme(
    primary = Color(0xFF00696D),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFF9CF1F5),
    onPrimaryContainer = Color(0xFF002021),
    secondary = Color(0xFF4A6365),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFCCE7E8),
    onSecondaryContainer = Color(0xFF051F20),
    tertiary = Color(0xFF4F5F7B),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFD7E2FF),
    onTertiaryContainer = Color(0xFF091C36),
    error = Color(0xFFBA1A1A),
    onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF410002),
    background = Color(0xFFFAFDFC),
    onBackground = Color(0xFF191C1C),
    surface = Color(0xFFFAFDFC),
    onSurface = Color(0xFF191C1C),
    surfaceVariant = Color(0xFFDAE4E5),
    onSurfaceVariant = Color(0xFF3F4849),
    outline = Color(0xFF6F7979),
    outlineVariant = Color(0xFFBEC8C9)
)

private val LightAmber = lightColorScheme(
    primary = Color(0xFF8B5000),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFFFDCBE),
    onPrimaryContainer = Color(0xFF2C1600),
    secondary = Color(0xFF6F5B40),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFF9DEBE),
    onSecondaryContainer = Color(0xFF271904),
    tertiary = Color(0xFF5C6133),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFE1E6AE),
    onTertiaryContainer = Color(0xFF191E00),
    error = Color(0xFFBA1A1A),
    onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF410002),
    background = Color(0xFFFFF8F3),
    onBackground = Color(0xFF1F1B16),
    surface = Color(0xFFFFF8F3),
    onSurface = Color(0xFF1F1B16),
    surfaceVariant = Color(0xFFF0E0D0),
    onSurfaceVariant = Color(0xFF4F4539),
    outline = Color(0xFF817567),
    outlineVariant = Color(0xFFD3C4B4)
)

private val LightViolet = lightColorScheme(
    primary = Color(0xFF6750A4),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFEADDFF),
    onPrimaryContainer = Color(0xFF21005D),
    secondary = Color(0xFF625B71),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFE8DEF8),
    onSecondaryContainer = Color(0xFF1D192B),
    tertiary = Color(0xFF7D5260),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFFFD8E4),
    onTertiaryContainer = Color(0xFF31111D),
    error = Color(0xFFB3261E),
    onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFF9DEDC),
    onErrorContainer = Color(0xFF410E0B),
    background = Color(0xFFFFFBFE),
    onBackground = Color(0xFF1C1B1F),
    surface = Color(0xFFFFFBFE),
    onSurface = Color(0xFF1C1B1F),
    surfaceVariant = Color(0xFFE7E0EC),
    onSurfaceVariant = Color(0xFF49454F),
    outline = Color(0xFF79747E),
    outlineVariant = Color(0xFFCAC4D0)
)

/* ---------------- 3 套暗色 + 1 套纯黑 ---------------- */
private val DarkBlue = darkColorScheme(
    primary = Color(0xFF9ECAFF),
    onPrimary = Color(0xFF003258),
    primaryContainer = Color(0xFF00497D),
    onPrimaryContainer = Color(0xFFD1E4FF),
    secondary = Color(0xFFBBC7DB),
    onSecondary = Color(0xFF253140),
    secondaryContainer = Color(0xFF3B4858),
    onSecondaryContainer = Color(0xFFD7E3F7),
    tertiary = Color(0xFFD6BEE4),
    onTertiary = Color(0xFF3B2948),
    tertiaryContainer = Color(0xFF523F5F),
    onTertiaryContainer = Color(0xFFF2DAFF),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6),
    background = Color(0xFF1A1C1E),
    onBackground = Color(0xFFE2E2E6),
    surface = Color(0xFF1A1C1E),
    onSurface = Color(0xFFE2E2E6),
    surfaceVariant = Color(0xFF43474E),
    onSurfaceVariant = Color(0xFFC3C7CF),
    outline = Color(0xFF8D9199),
    outlineVariant = Color(0xFF5E6268)
)

private val DarkForest = darkColorScheme(
    primary = Color(0xFF4CD9DE),
    onPrimary = Color(0xFF003739),
    primaryContainer = Color(0xFF004F51),
    onPrimaryContainer = Color(0xFF6FF6FB),
    secondary = Color(0xFFB1CCCD),
    onSecondary = Color(0xFF1C3435),
    secondaryContainer = Color(0xFF334B4C),
    onSecondaryContainer = Color(0xFFCCE7E8),
    tertiary = Color(0xFFB7C8EB),
    onTertiary = Color(0xFF21314C),
    tertiaryContainer = Color(0xFF374763),
    onTertiaryContainer = Color(0xFFD7E2FF),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6),
    background = Color(0xFF191C1C),
    onBackground = Color(0xFFE0E3E3),
    surface = Color(0xFF191C1C),
    onSurface = Color(0xFFE0E3E3),
    surfaceVariant = Color(0xFF3F4849),
    onSurfaceVariant = Color(0xFFBEC8C9),
    outline = Color(0xFF899393),
    outlineVariant = Color(0xFF4A5354)
)

private val DarkCoffee = darkColorScheme(
    primary = Color(0xFFFFB86B),
    onPrimary = Color(0xFF4A2800),
    primaryContainer = Color(0xFF6A3C00),
    onPrimaryContainer = Color(0xFFFFDCBE),
    secondary = Color(0xFFD8C3A5),
    onSecondary = Color(0xFF3B2E1A),
    secondaryContainer = Color(0xFF534430),
    onSecondaryContainer = Color(0xFFF5DFC0),
    tertiary = Color(0xFFC2CC8B),
    onTertiary = Color(0xFF2E3400),
    tertiaryContainer = Color(0xFF444B18),
    onTertiaryContainer = Color(0xFFDEE8A4),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6),
    background = Color(0xFF1F1B16),
    onBackground = Color(0xFFEBE1D9),
    surface = Color(0xFF1F1B16),
    onSurface = Color(0xFFEBE1D9),
    surfaceVariant = Color(0xFF4F4539),
    onSurfaceVariant = Color(0xFFD2C4B8),
    outline = Color(0xFF9B8F82),
    outlineVariant = Color(0xFF5A4F42)
)

private val PureBlack = darkColorScheme(
    primary = Color(0xFFD0BCFF),
    onPrimary = Color(0xFF381E72),
    primaryContainer = Color(0xFF4F378B),
    onPrimaryContainer = Color(0xFFEADDFF),
    secondary = Color(0xFFCCC2DC),
    onSecondary = Color(0xFF332D41),
    secondaryContainer = Color(0xFF4A4458),
    onSecondaryContainer = Color(0xFFE8DEF8),
    tertiary = Color(0xFFEFB8C8),
    onTertiary = Color(0xFF492532),
    tertiaryContainer = Color(0xFF633B48),
    onTertiaryContainer = Color(0xFFFFD8E4),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6),
    background = Color(0xFF000000),
    onBackground = Color(0xFFE6E1E9),
    surface = Color(0xFF000000),
    onSurface = Color(0xFFE6E1E9),
    surfaceVariant = Color(0xFF1C1C1E),
    onSurfaceVariant = Color(0xFFCAC4D0),
    outline = Color(0xFF938F99),
    outlineVariant = Color(0xFF3A3A3C)
)

val appThemeOptions: List<AppThemeOption> = listOf(
    AppThemeOption("light_blue", "天空蓝", LightBlue, false),
    AppThemeOption("light_teal", "青碧", LightTeal, false),
    AppThemeOption("light_amber", "暖阳", LightAmber, false),
    AppThemeOption("light_violet", "紫罗兰", LightViolet, false),
    AppThemeOption("dark_blue", "夜空蓝", DarkBlue, true),
    AppThemeOption("dark_forest", "深林绿", DarkForest, true),
    AppThemeOption("dark_coffee", "暖棕", DarkCoffee, true),
    AppThemeOption("pure_black", "纯黑", PureBlack, true)
)

val LocalAppTheme = staticCompositionLocalOf { appThemeOptions[4] }

@Composable
fun SuboTheme(themeId: String = "dark_blue", content: @Composable () -> Unit) {
    val option = appThemeOptions.firstOrNull { it.id == themeId } ?: appThemeOptions[4]
    CompositionLocalProvider(LocalAppTheme provides option) {
        MaterialTheme(colorScheme = option.scheme, content = content)
    }
}

/* ---------------- 界面语义色：全部从 ColorScheme 角色取 ---------------- */
val ColorScheme.appTopBar: Color get() = primaryContainer
val ColorScheme.appOnTopBar: Color get() = onPrimaryContainer
val ColorScheme.appPanel: Color get() = surfaceVariant
val ColorScheme.appPanelBorder: Color get() = outlineVariant
val ColorScheme.appAccent: Color get() = primary
val ColorScheme.appMuted: Color get() = onSurfaceVariant
val ColorScheme.appText: Color get() = onSurface
val ColorScheme.appDistance: Color get() = primary
val ColorScheme.appButtonPrimary: Color get() = primary
val ColorScheme.appOnButtonPrimary: Color get() = onPrimary
val ColorScheme.appButtonStop: Color get() = error
val ColorScheme.appOnButtonStop: Color get() = onError
val ColorScheme.appButtonNormal: Color get() = secondaryContainer
val ColorScheme.appOnButtonNormal: Color get() = onSecondaryContainer
val ColorScheme.appKey: Color get() = surface
val ColorScheme.appOnKey: Color get() = onSurface
val ColorScheme.appKeyPressed: Color get() = primaryContainer
val ColorScheme.appOnKeyPressed: Color get() = onPrimaryContainer
