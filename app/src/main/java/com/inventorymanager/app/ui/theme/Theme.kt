package com.inventorymanager.app.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.isSystemInDarkTheme

private val LightColors = lightColorScheme(
    primary = Teal,
    secondary = Gold,
    tertiary = Rose,
    background = Cream,
    surface = Cream,
)

private val DarkColors = darkColorScheme(
    primary = Teal,
    secondary = Gold,
    tertiary = Rose,
)

@Composable
fun InventoryManagerTheme(content: @Composable () -> Unit) {
    val context = LocalContext.current
    val darkTheme = isSystemInDarkTheme()
    val colorScheme = when {
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val activity = context as? Activity
            when {
                activity != null && darkTheme -> dynamicDarkColorScheme(activity)
                activity != null -> dynamicLightColorScheme(activity)
                darkTheme -> DarkColors
                else -> LightColors
            }
        }
        darkTheme -> DarkColors
        else -> LightColors
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content,
    )
}
