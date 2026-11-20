package com.example.mementoclone.ui.theme

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat

/**
 * 所有界面颜色都从这里取。字段是 mutableState，切换深色模式时整棵树会自动重组，
 * 各个界面不用改成参数传递。
 */
object Palette {
    var dark by mutableStateOf(false)

    var bg by mutableStateOf(Color(0xFFF3F5F9))
    var surface by mutableStateOf(Color(0xFFFFFFFF))
    var textMain by mutableStateOf(Color(0xFF16202C))
    var muted by mutableStateOf(Color(0xFF6B7A90))
    var muted2 by mutableStateOf(Color(0xFF93A1B5))
    var line by mutableStateOf(Color(0xFFE7ECF2))
    var primary by mutableStateOf(Color(0xFF1976D2))
    var primarySoft by mutableStateOf(Color(0xFFE8F2FC))
    var teal by mutableStateOf(Color(0xFF0F9D8A))
    var tealSoft by mutableStateOf(Color(0xFFE3F6F2))
    var amber by mutableStateOf(Color(0xFFB45309))
    var amberSoft by mutableStateOf(Color(0xFFFFF3E2))
    var danger by mutableStateOf(Color(0xFFD14343))
    var dangerSoft by mutableStateOf(Color(0xFFFDECEC))

    fun apply(night: Boolean) {
        dark = night
        if (night) {
            bg = Color(0xFF0D1218)
            surface = Color(0xFF161E27)
            textMain = Color(0xFFE9EFF6)
            muted = Color(0xFFA6B6C6)
            muted2 = Color(0xFF7E8FA2)
            line = Color(0xFF27313C)
            primary = Color(0xFF5AA9F0)
            primarySoft = Color(0xFF16324B)
            teal = Color(0xFF3EC9B3)
            tealSoft = Color(0xFF10322D)
            amber = Color(0xFFF0AC55)
            amberSoft = Color(0xFF34260F)
            danger = Color(0xFFF0796F)
            dangerSoft = Color(0xFF3A1C1C)
        } else {
            bg = Color(0xFFF3F5F9)
            surface = Color(0xFFFFFFFF)
            textMain = Color(0xFF16202C)
            muted = Color(0xFF6B7A90)
            muted2 = Color(0xFF93A1B5)
            line = Color(0xFFE7ECF2)
            primary = Color(0xFF1976D2)
            primarySoft = Color(0xFFE8F2FC)
            teal = Color(0xFF0F9D8A)
            tealSoft = Color(0xFFE3F6F2)
            amber = Color(0xFFB45309)
            amberSoft = Color(0xFFFFF3E2)
            danger = Color(0xFFD14343)
            dangerSoft = Color(0xFFFDECEC)
        }
    }
}

val Bg: Color get() = Palette.bg
val Surface: Color get() = Palette.surface
val TextMain: Color get() = Palette.textMain
val Muted: Color get() = Palette.muted
val Muted2: Color get() = Palette.muted2
val Line: Color get() = Palette.line
val Primary: Color get() = Palette.primary
val PrimarySoft: Color get() = Palette.primarySoft
val Teal: Color get() = Palette.teal
val TealSoft: Color get() = Palette.tealSoft
val Amber: Color get() = Palette.amber
val AmberSoft: Color get() = Palette.amberSoft
val Danger: Color get() = Palette.danger
val DangerSoft: Color get() = Palette.dangerSoft

fun soft(color: Color): Color = color.copy(alpha = 0.12f)

private fun lightScheme() = lightColorScheme(
    primary = Palette.primary,
    onPrimary = Color.White,
    primaryContainer = Palette.primarySoft,
    onPrimaryContainer = if (Palette.dark) Color(0xFFCFE6FF) else Color(0xFF0E4D8A),
    secondary = Palette.teal,
    onSecondary = Color.White,
    secondaryContainer = Palette.tealSoft,
    background = Palette.bg,
    onBackground = Palette.textMain,
    surface = Palette.surface,
    onSurface = Palette.textMain,
    surfaceVariant = if (Palette.dark) Color(0xFF1E2833) else Color(0xFFEFF3F8),
    onSurfaceVariant = Palette.muted,
    outline = Palette.line,
    outlineVariant = Palette.line,
    error = Palette.danger,
    onError = Color.White,
    errorContainer = Palette.dangerSoft,
    onErrorContainer = if (Palette.dark) Color(0xFFFFD2CE) else Color(0xFF8C2020)
)

private fun nightScheme() = darkColorScheme(
    primary = Palette.primary,
    onPrimary = Color(0xFF06213A),
    primaryContainer = Palette.primarySoft,
    onPrimaryContainer = Color(0xFFCFE6FF),
    secondary = Palette.teal,
    onSecondary = Color(0xFF032B26),
    secondaryContainer = Palette.tealSoft,
    background = Palette.bg,
    onBackground = Palette.textMain,
    surface = Palette.surface,
    onSurface = Palette.textMain,
    surfaceVariant = Color(0xFF1E2833),
    onSurfaceVariant = Palette.muted,
    outline = Palette.line,
    outlineVariant = Palette.line,
    error = Palette.danger,
    onError = Color(0xFF3A0A08),
    errorContainer = Palette.dangerSoft,
    onErrorContainer = Color(0xFFFFD2CE)
)

private val AppTypography = Typography(
    displaySmall = TextStyle(fontSize = 30.sp, fontWeight = FontWeight.Bold, letterSpacing = (-0.5).sp),
    headlineSmall = TextStyle(fontSize = 22.sp, fontWeight = FontWeight.Bold),
    titleLarge = TextStyle(fontSize = 18.sp, fontWeight = FontWeight.Bold),
    titleMedium = TextStyle(fontSize = 15.sp, fontWeight = FontWeight.SemiBold),
    titleSmall = TextStyle(fontSize = 13.sp, fontWeight = FontWeight.SemiBold),
    bodyLarge = TextStyle(fontSize = 15.sp),
    bodyMedium = TextStyle(fontSize = 13.5.sp),
    bodySmall = TextStyle(fontSize = 12.sp),
    labelLarge = TextStyle(fontSize = 13.sp, fontWeight = FontWeight.SemiBold),
    labelMedium = TextStyle(fontSize = 11.5.sp),
    labelSmall = TextStyle(fontSize = 10.5.sp, fontWeight = FontWeight.Medium)
)

@Composable
fun MementoTheme(content: @Composable () -> Unit) {
    val view = LocalView.current
    val night = Palette.dark
    val scheme = if (night) nightScheme() else lightScheme()
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = Palette.bg.toArgb()
            window.navigationBarColor = Palette.surface.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !night
            WindowCompat.getInsetsController(window, view).isAppearanceLightNavigationBars = !night
        }
    }
    MaterialTheme(colorScheme = scheme, typography = AppTypography, content = content)
}
