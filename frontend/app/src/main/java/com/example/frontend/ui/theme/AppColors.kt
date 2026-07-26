package com.example.frontend.ui.theme

import androidx.compose.runtime.staticCompositionLocalOf

/**
 * Semantic colors outside the standard Material3 ColorScheme roles — M3 has no
 * built-in "success" slot. Provided via [LocalAppExtraColors] in [FrontendTheme]
 * so call sites read it the same way they read `MaterialTheme.colorScheme.error`.
 */
data class AppExtraColors(
    val success: androidx.compose.ui.graphics.Color,
    val onSuccess: androidx.compose.ui.graphics.Color,
)

val LightExtraColors = AppExtraColors(success = SuccessLight, onSuccess = OnSuccessLight)
val DarkExtraColors = AppExtraColors(success = SuccessDark, onSuccess = OnSuccessDark)

val LocalAppExtraColors = staticCompositionLocalOf { LightExtraColors }
