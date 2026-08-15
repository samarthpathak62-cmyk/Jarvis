package com.example.ui.theme

import androidx.compose.ui.graphics.Color

// Atmospheric / Immersive Media Canvas Palette
val AtmosphericDark = Color(0xFF05060A)
val AtmosphericDarkSurface = Color(0xFF090C15)
val AtmosphericDarkSecondary = Color(0xFF0D121F)
val AtmosphericGlass = Color(0x0DFFFFFF) // bg-white/5
val AtmosphericGlassCard = Color(0x14FFFFFF) // bg-white/8
val AtmosphericGlassHover = Color(0x1FFFFFFF) // bg-white/12

// Atmospheric Cyan & Glowing Accents
val CyanAtmospheric = Color(0xFF22D3EE) // Cyan-400
val CyanCore = Color(0xFF06B6D4)        // Cyan-500
val CyanHighlight = Color(0xFF67E8F9)   // Cyan-300
val CyanDeepGlow = Color(0xFF083344)    // Cyan-950 / Deep glow
val BlueAtmospheric = Color(0xFF1E3A8A) // Blue-900 / Ambient depth
val TealAccent = Color(0xFF14B8A6)

// Text Colors (Tailwind Slate scale)
val TextPrimary = Color(0xFFF1F5F9)    // Slate-100
val TextSecondary = Color(0xFFCBD5E1)  // Slate-300
val TextMuted = Color(0xFF94A3B8)      // Slate-400
val TextTertiary = Color(0xFF64748B)   // Slate-500
val TextDarker = Color(0xFF475569)     // Slate-600

// Atmospheric Glass Borders
val GlassBorder = Color(0x1AFFFFFF)        // border-white/10
val GlassBorderSubtle = Color(0x0FFFFFFF)  // border-white/6
val GlassBorderActive = Color(0x4022D3EE)  // border-cyan-400/25
val GlassBorderCyan = Color(0x3306B6D4)    // border-cyan-500/20

// Status Colors
val StatusOnline = Color(0xFF22D3EE)
val StatusThinking = Color(0xFFF59E0B)
val StatusError = Color(0xFFEF4444)

// Backwards compatibility aliases for existing references
val CyberBackground = AtmosphericDark
val CyberBackgroundSecondary = AtmosphericDarkSurface
val CyberSurface = AtmosphericDarkSecondary
val CyberSurfaceGlass = AtmosphericGlass
val CyberSurfaceCard = AtmosphericGlassCard
val CyanNeon = CyanAtmospheric
val CyanGlow = CyanHighlight
val BlueDeep = BlueAtmospheric
val BlueElectric = Color(0xFF0284C7)
val BlueAccent = Color(0xFF38BDF8)

