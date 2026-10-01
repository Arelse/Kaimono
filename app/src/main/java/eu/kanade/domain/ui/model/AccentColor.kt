package eu.kanade.domain.ui.model

import androidx.compose.ui.graphics.Color

/**
 * Flourish accent colors. Independent of [AppTheme] - this drives the optional animated
 * gradient background, not the app's actual color scheme.
 */
enum class AccentColor(val swatch: Color, val primary: Color, val secondary: Color) {
    EMBER(Color(0xFFE53935), Color(0xFFE53935), Color(0xFF6E1414)),
    MINT(Color(0xFF1FAE7A), Color(0xFF1FAE7A), Color(0xFF0B4A33)),
    ROYAL(Color(0xFF8B2FD1), Color(0xFF8B2FD1), Color(0xFF34104F)),
    VOID(Color(0xFF6B6B6B), Color(0xFF4A4A4A), Color(0xFF0A0A0A)),
    WALLPAPER(Color(0xFF4A7FE8), Color(0xFF4A7FE8), Color(0xFF26337A)),
}
