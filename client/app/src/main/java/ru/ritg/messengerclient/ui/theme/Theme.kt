package ru.ritg.messengerclient.ui.theme

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val AppColorScheme = lightColorScheme(
    primary = Blue40,
    onPrimary = Color.White,
    secondary = BlueGrey40,
    tertiary = LightBlue40,
    background = Color(0xFFF5F5F5),
    surface = Color.White,
    surfaceVariant = Color.White,
    surfaceContainerLow = Color.White,
    surfaceContainer = Color.White,
    surfaceContainerHigh = Color.White,
    surfaceContainerHighest = Color.White,
    onSurface = Color.Black,
    onSurfaceVariant = Color(0xFF666666)
)

/**
 * Тёмная тема приложения.
 *
 * Устанавливает цветовую схему Material3 и цвет статус-бара.
 *
 * @param content Compose-контент
 */
@Composable
fun MessengerClientTheme(
    content: @Composable () -> Unit
) {
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = DarkGray.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
        }
    }
    MaterialTheme(
        colorScheme = AppColorScheme,
        typography = Typography,
        content = content
    )
}
