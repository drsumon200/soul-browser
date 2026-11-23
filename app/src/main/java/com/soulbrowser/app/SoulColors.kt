package com.soulbrowser.app

import androidx.compose.ui.graphics.Color

object SoulColors {
    // Current active theme mode (mutable, toggled live by settings)
    var isDark: Boolean = true

    // Dark Purple Palette
    val DarkInk = Color(0xFF140D26)
    val DarkPanel = Color(0xFF1F1538)
    val DarkPanel2 = Color(0xFF2C1E4D)
    val DarkBorder = Color(0xFF3B2A66)
    val DarkText = Color(0xFFF1F5F9)
    val DarkTextSub = Color(0xFFCBD5E1)
    val DarkMuted = Color(0xFF94A3B8)
    val DarkNavBarBg = Color(0xD91F1538)

    // Light Theme Palette (Crisp Zen Clean)
    val LightInk = Color(0xFFF8FAFC)
    val LightPanel = Color(0xFFFFFFFF)
    val LightPanel2 = Color(0xFFF1F5F9)
    val LightBorder = Color(0xFFE2E8F0)
    val LightText = Color(0xFF0F172A)
    val LightTextSub = Color(0xFF475569)
    val LightMuted = Color(0xFF64748B)
    val LightNavBarBg = Color(0xE6FFFFFF)

    // Dynamic Getters responding to active theme
    val Ink: Color get() = if (isDark) DarkInk else LightInk
    val Panel: Color get() = if (isDark) DarkPanel else LightPanel
    val Panel2: Color get() = if (isDark) DarkPanel2 else LightPanel2
    val Panel3: Color get() = if (isDark) DarkBorder else LightBorder
    val Border: Color get() = if (isDark) DarkBorder else LightBorder
    val Text: Color get() = if (isDark) DarkText else LightText
    val TextSub: Color get() = if (isDark) DarkTextSub else LightTextSub
    val Muted: Color get() = if (isDark) DarkMuted else LightMuted
    val NavBarBg: Color get() = if (isDark) DarkNavBarBg else LightNavBarBg

    // Shared Signature Accents
    val Lavender = Color(0xFFA855F7)
    val LavenderLight = Color(0xFF2C1E4D)
    val NeonPurple = Color(0xFFC084FC)
    val Mint = Color(0xFF2DD4BF)
    val Rose = Color(0xFFF43F5E)
    val Amber = Color(0xFFFBBF24)
}
