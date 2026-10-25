package dev.nova.core.ui

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import dev.nova.core.domain.model.PerformanceMode
import dev.nova.core.domain.model.ThemeMode

/**
 * Theme engine: Light / Dark / Auto / AMOLED Black.
 * Android 12+ usa Material You (cores extraídas do wallpaper); antes disso, paleta Material padrão.
 */
@Composable
fun NovaTheme(mode: ThemeMode, performance: PerformanceMode, content: @Composable () -> Unit) {
    val dark = when (mode) {
        ThemeMode.LIGHT -> false
        ThemeMode.DARK, ThemeMode.AMOLED_BLACK -> true
        ThemeMode.AUTO -> isSystemInDarkTheme()
    }
    val context = LocalContext.current
    val base = when {
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ->
            if (dark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        dark -> darkColorScheme()
        else -> lightColorScheme()
    }
    // AMOLED: preto real nos fundos => pixels desligados (economia de bateria no Dynamic AMOLED 2X).
    val scheme = if (mode == ThemeMode.AMOLED_BLACK) {
        base.copy(background = Color.Black, surface = Color.Black, surfaceVariant = Color(0xFF121212))
    } else {
        base
    }
    CompositionLocalProvider(LocalPerformance provides performance) {
        MaterialTheme(colorScheme = scheme, content = content)
    }
}
