package com.yx.chatrobot.ui.theme

import android.util.Log
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material.MaterialTheme
import androidx.compose.material.darkColors
import androidx.compose.material.lightColors
import androidx.compose.ui.graphics.Color
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.lifecycle.viewmodel.compose.viewModel
import com.yx.chatrobot.ui.AppViewModelProvider
import com.yx.chatrobot.ui.config.ConfigViewModel
import com.yx.chatrobot.ui.login.LoginViewModel
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import android.app.Activity

private val DarkColorPalette = darkColors(
    primary = Color(0xFFB0BEC5),
    primaryVariant = Color(0xFF90A4AE),
    secondary = Color(0xFF607D8B),
    background = Color(0xFF121212),
    surface = Color(0xFF1E1E1E),
    onPrimary = Color.Black,
    onSecondary = Color.White,
    onBackground = Color.White,
    onSurface = Color.White
)

private val LightColorPalette = lightColors(
    primary = Color(0xFF9E9E9E),
    primaryVariant = Color(0xFF757575),
    secondary = Color(0xFF607D8B),
    background = Color.White,
    surface = Color.White,
    onPrimary = Color.Black,
    onSecondary = Color.White,
    onBackground = Color.Black,
    onSurface = Color.Black
)

@Composable
fun ChatRobotTheme(
    loginViewModel: LoginViewModel = viewModel(factory = AppViewModelProvider.Factory),
    content: @Composable () -> Unit
) {
    val themeState = loginViewModel.themeState.collectAsState().value
    val fontState = loginViewModel.fontState.collectAsState().value
    val colors = if (themeState) DarkColorPalette else LightColorPalette
    val dark = androidx.compose.foundation.isSystemInDarkTheme()
    val fontTypography = when (fontState) {
        "小" -> TypographySmall
        "中" -> TypographyMedium
        "大" -> TypographyLarge
        else -> TypographySmall
    }
    MaterialTheme(
        colors = colors,
        typography = fontTypography,
        shapes = Shapes,
        content = content
    )

    val view = LocalView.current
    SideEffect {
        val window = (view.context as? Activity)?.window
        if (window != null) {
            window.statusBarColor = colors.background.value.toInt()
            window.navigationBarColor = colors.background.value.toInt()
            val controller = WindowCompat.getInsetsController(window, window.decorView)
            controller?.isAppearanceLightStatusBars = !dark
            controller?.isAppearanceLightNavigationBars = !dark
        }
    }
}