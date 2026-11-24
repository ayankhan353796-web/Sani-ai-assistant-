package com.example.data.themes

import androidx.compose.ui.graphics.Color
import com.example.data.model.AutoReplyApp
import com.example.data.model.AvatarKind
import com.example.data.model.AvatarOption
import com.example.data.model.MicStyleOption
import com.example.data.model.SaniThemeConfig

object ThemesCatalog {

    val allThemes: List<SaniThemeConfig> = listOf(
        SaniThemeConfig(
            id = "solar_flare",
            name = "Solar Flare",
            category = "Signature",
            background = Color(0xFF321205),
            secondaryBackground = Color(0xFF4A1D0B),
            primaryAccent = Color(0xFFFF852A),
            secondaryAccent = Color(0xFFFFB83F),
            primaryText = Color(0xFFFFF0DC),
            secondaryText = Color(0xFFD6B69D),
            glowCyan = Color(0xFF35DDEB),
            glowPurple = Color(0xFF913CFF)
        ),
        SaniThemeConfig(
            id = "midnight_gold",
            name = "Midnight Gold",
            category = "Luxury",
            background = Color(0xFF12100E),
            secondaryBackground = Color(0xFF231D16),
            primaryAccent = Color(0xFFFFB83F),
            secondaryAccent = Color(0xFFFFD875),
            primaryText = Color(0xFFFFF7E6),
            secondaryText = Color(0xFFD2C2A8),
            glowCyan = Color(0xFF35DDEB),
            glowPurple = Color(0xFF913CFF)
        ),
        SaniThemeConfig(
            id = "neon_purple",
            name = "Neon Purple",
            category = "Cyber",
            background = Color(0xFF140926),
            secondaryBackground = Color(0xFF241142),
            primaryAccent = Color(0xFF913CFF),
            secondaryAccent = Color(0xFFD06BFF),
            primaryText = Color(0xFFF6EEFF),
            secondaryText = Color(0xFFC5B1E3),
            glowCyan = Color(0xFF35DDEB),
            glowPurple = Color(0xFFFF4FD8)
        ),
        SaniThemeConfig(
            id = "ocean_blue",
            name = "Ocean Blue",
            category = "Nature",
            background = Color(0xFF061A2B),
            secondaryBackground = Color(0xFF0E2C47),
            primaryAccent = Color(0xFF2FA8FF),
            secondaryAccent = Color(0xFF66D1FF),
            primaryText = Color(0xFFEAF7FF),
            secondaryText = Color(0xFFA7C9E2),
            glowCyan = Color(0xFF35DDEB),
            glowPurple = Color(0xFF7B61FF)
        ),
        SaniThemeConfig(
            id = "cyber_cyan",
            name = "Cyber Cyan",
            category = "Cyber",
            background = Color(0xFF051C22),
            secondaryBackground = Color(0xFF0B303A),
            primaryAccent = Color(0xFF35DDEB),
            secondaryAccent = Color(0xFF7BF2FF),
            primaryText = Color(0xFFE9FEFF),
            secondaryText = Color(0xFFA3D2D8),
            glowCyan = Color(0xFF00F5D4),
            glowPurple = Color(0xFF913CFF)
        ),
        SaniThemeConfig(
            id = "emerald_glow",
            name = "Emerald Glow",
            category = "Nature",
            background = Color(0xFF061E16),
            secondaryBackground = Color(0xFF0F3527),
            primaryAccent = Color(0xFF2CE59B),
            secondaryAccent = Color(0xFF7AF2BD),
            primaryText = Color(0xFFEAFFF6),
            secondaryText = Color(0xFFA7D4BF),
            glowCyan = Color(0xFF35DDEB),
            glowPurple = Color(0xFF913CFF)
        ),
        SaniThemeConfig(
            id = "rose_gold",
            name = "Rose Gold",
            category = "Luxury",
            background = Color(0xFF281018),
            secondaryBackground = Color(0xFF421A27),
            primaryAccent = Color(0xFFFF7A9E),
            secondaryAccent = Color(0xFFFFB59D),
            primaryText = Color(0xFFFFF0F4),
            secondaryText = Color(0xFFE0B6C1),
            glowCyan = Color(0xFF35DDEB),
            glowPurple = Color(0xFF913CFF)
        ),
        SaniThemeConfig(
            id = "dark_galaxy",
            name = "Dark Galaxy",
            category = "Cosmic",
            background = Color(0xFF090B1A),
            secondaryBackground = Color(0xFF151936),
            primaryAccent = Color(0xFF7C6BFF),
            secondaryAccent = Color(0xFF35DDEB),
            primaryText = Color(0xFFF0F3FF),
            secondaryText = Color(0xFFB3BBD9),
            glowCyan = Color(0xFF35DDEB),
            glowPurple = Color(0xFF913CFF)
        ),
        SaniThemeConfig(
            id = "crimson_ember",
            name = "Crimson Ember",
            category = "Signature",
            background = Color(0xFF29080A),
            secondaryBackground = Color(0xFF451015),
            primaryAccent = Color(0xFFFF4D4D),
            secondaryAccent = Color(0xFFFF964F),
            primaryText = Color(0xFFFFECEC),
            secondaryText = Color(0xFFD9AEAE),
            glowCyan = Color(0xFF35DDEB),
            glowPurple = Color(0xFF913CFF)
        ),
        SaniThemeConfig(
            id = "obsidian_jade",
            name = "Obsidian Jade",
            category = "Luxury",
            background = Color(0xFF0B1412),
            secondaryBackground = Color(0xFF162723),
            primaryAccent = Color(0xFF00D09C),
            secondaryAccent = Color(0xFFFFC857),
            primaryText = Color(0xFFEFFFFB),
            secondaryText = Color(0xFFA8C7BE),
            glowCyan = Color(0xFF35DDEB),
            glowPurple = Color(0xFF913CFF)
        ),
        SaniThemeConfig(
            id = "sunset_amber",
            name = "Sunset Amber",
            category = "Warm",
            background = Color(0xFF2B1209),
            secondaryBackground = Color(0xFF471E10),
            primaryAccent = Color(0xFFFF6D3F),
            secondaryAccent = Color(0xFFFFCC5C),
            primaryText = Color(0xFFFFF3E8),
            secondaryText = Color(0xFFD8B8A6),
            glowCyan = Color(0xFF35DDEB),
            glowPurple = Color(0xFF913CFF)
        ),
        SaniThemeConfig(
            id = "arctic_frost",
            name = "Arctic Frost",
            category = "Minimal",
            background = Color(0xFF0C1824),
            secondaryBackground = Color(0xFF162B3D),
            primaryAccent = Color(0xFF70D6FF),
            secondaryAccent = Color(0xFFC2F0FF),
            primaryText = Color(0xFFF3FAFF),
            secondaryText = Color(0xFFB3CCE0),
            glowCyan = Color(0xFF35DDEB),
            glowPurple = Color(0xFF8E7CFF)
        ),
        SaniThemeConfig(
            id = "violet_horizon",
            name = "Violet Horizon",
            category = "Cosmic",
            background = Color(0xFF1A0D2E),
            secondaryBackground = Color(0xFF2C174C),
            primaryAccent = Color(0xFFA66BFF),
            secondaryAccent = Color(0xFFFF88DC),
            primaryText = Color(0xFFF8F2FF),
            secondaryText = Color(0xFFCBB6E8),
            glowCyan = Color(0xFF35DDEB),
            glowPurple = Color(0xFF913CFF)
        ),
        SaniThemeConfig(
            id = "copper_luxe",
            name = "Copper Luxe",
            category = "Luxury",
            background = Color(0xFF26130C),
            secondaryBackground = Color(0xFF3E2015),
            primaryAccent = Color(0xFFE58249),
            secondaryAccent = Color(0xFFF4B678),
            primaryText = Color(0xFFFFF1E6),
            secondaryText = Color(0xFFD3B49F),
            glowCyan = Color(0xFF35DDEB),
            glowPurple = Color(0xFF913CFF)
        ),
        SaniThemeConfig(
            id = "cobalt_pulse",
            name = "Cobalt Pulse",
            category = "Cyber",
            background = Color(0xFF081430),
            secondaryBackground = Color(0xFF112452),
            primaryAccent = Color(0xFF4C8BFF),
            secondaryAccent = Color(0xFF35DDEB),
            primaryText = Color(0xFFEEF4FF),
            secondaryText = Color(0xFFAFC2E8),
            glowCyan = Color(0xFF35DDEB),
            glowPurple = Color(0xFF913CFF)
        ),
        SaniThemeConfig(
            id = "sakura_dusk",
            name = "Sakura Dusk",
            category = "Warm",
            background = Color(0xFF24101C),
            secondaryBackground = Color(0xFF3C1B2F),
            primaryAccent = Color(0xFFFF85B3),
            secondaryAccent = Color(0xFFFFC2D9),
            primaryText = Color(0xFFFFF2F7),
            secondaryText = Color(0xFFDDB7C7),
            glowCyan = Color(0xFF35DDEB),
            glowPurple = Color(0xFF913CFF)
        ),
        SaniThemeConfig(
            id = "aurora_mint",
            name = "Aurora Mint",
            category = "Nature",
            background = Color(0xFF091D1C),
            secondaryBackground = Color(0xFF123331),
            primaryAccent = Color(0xFF42F5C5),
            secondaryAccent = Color(0xFF93FFE3),
            primaryText = Color(0xFFECFFFA),
            secondaryText = Color(0xFFA9D6CC),
            glowCyan = Color(0xFF35DDEB),
            glowPurple = Color(0xFF913CFF)
        ),
        SaniThemeConfig(
            id = "bronze_mirage",
            name = "Bronze Mirage",
            category = "Warm",
            background = Color(0xFF22160A),
            secondaryBackground = Color(0xFF382411),
            primaryAccent = Color(0xFFD9903B),
            secondaryAccent = Color(0xFFF7C97E),
            primaryText = Color(0xFFFFF4E3),
            secondaryText = Color(0xFFCDB99C),
            glowCyan = Color(0xFF35DDEB),
            glowPurple = Color(0xFF913CFF)
        ),
        SaniThemeConfig(
            id = "indigo_storm",
            name = "Indigo Storm",
            category = "Cosmic",
            background = Color(0xFF0F1029),
            secondaryBackground = Color(0xFF1B1D47),
            primaryAccent = Color(0xFF6B7AFF),
            secondaryAccent = Color(0xFFB5BEFF),
            primaryText = Color(0xFFF1F3FF),
            secondaryText = Color(0xFFB7BCE3),
            glowCyan = Color(0xFF35DDEB),
            glowPurple = Color(0xFF913CFF)
        ),
        SaniThemeConfig(
            id = "champagne_noir",
            name = "Champagne Noir",
            category = "Minimal",
            background = Color(0xFF161311),
            secondaryBackground = Color(0xFF26211D),
            primaryAccent = Color(0xFFE8C88B),
            secondaryAccent = Color(0xFFFFE5B4),
            primaryText = Color(0xFFFFFBF2),
            secondaryText = Color(0xFFCBBFAD),
            glowCyan = Color(0xFF35DDEB),
            glowPurple = Color(0xFF913CFF)
        ),
        SaniThemeConfig(
            id = "magma_core",
            name = "Magma Core",
            category = "Signature",
            background = Color(0xFF240B04),
            secondaryBackground = Color(0xFF3D1308),
            primaryAccent = Color(0xFFFF5E1A),
            secondaryAccent = Color(0xFFFFAA00),
            primaryText = Color(0xFFFFEFE6),
            secondaryText = Color(0xFFD7B09E),
            glowCyan = Color(0xFF35DDEB),
            glowPurple = Color(0xFF913CFF)
        ),
        SaniThemeConfig(
            id = "electric_lime",
            name = "Electric Lime",
            category = "Cyber",
            background = Color(0xFF111C09),
            secondaryBackground = Color(0xFF1D3010),
            primaryAccent = Color(0xFFA8FF35),
            secondaryAccent = Color(0xFFD7FF85),
            primaryText = Color(0xFFF6FFEB),
            secondaryText = Color(0xFFBDD1A3),
            glowCyan = Color(0xFF35DDEB),
            glowPurple = Color(0xFF913CFF)
        ),
        SaniThemeConfig(
            id = "velvet_plum",
            name = "Velvet Plum",
            category = "Luxury",
            background = Color(0xFF210B1F),
            secondaryBackground = Color(0xFF381335),
            primaryAccent = Color(0xFFE056FD),
            secondaryAccent = Color(0xFFFF9FF3),
            primaryText = Color(0xFFFDF0FF),
            secondaryText = Color(0xFFD6B2D9),
            glowCyan = Color(0xFF35DDEB),
            glowPurple = Color(0xFF913CFF)
        ),
        SaniThemeConfig(
            id = "sapphire_night",
            name = "Sapphire Night",
            category = "Cosmic",
            background = Color(0xFF071326),
            secondaryBackground = Color(0xFF0E2342),
            primaryAccent = Color(0xFF3D9BFF),
            secondaryAccent = Color(0xFF8CC5FF),
            primaryText = Color(0xFFEEF6FF),
            secondaryText = Color(0xFFA9C3E0),
            glowCyan = Color(0xFF35DDEB),
            glowPurple = Color(0xFF913CFF)
        ),
        SaniThemeConfig(
            id = "terracotta_glow",
            name = "Terracotta Glow",
            category = "Warm",
            background = Color(0xFF2B150E),
            secondaryBackground = Color(0xFF452318),
            primaryAccent = Color(0xFFE86F4B),
            secondaryAccent = Color(0xFFFFB285),
            primaryText = Color(0xFFFFF2EC),
            secondaryText = Color(0xFFD7B6AA),
            glowCyan = Color(0xFF35DDEB),
            glowPurple = Color(0xFF913CFF)
        ),
        SaniThemeConfig(
            id = "starlight_silver",
            name = "Starlight Silver",
            category = "Minimal",
            background = Color(0xFF12151C),
            secondaryBackground = Color(0xFF202530),
            primaryAccent = Color(0xFFC2D0E6),
            secondaryAccent = Color(0xFF35DDEB),
            primaryText = Color(0xFFF7FAFF),
            secondaryText = Color(0xFFB0BACB),
            glowCyan = Color(0xFF35DDEB),
            glowPurple = Color(0xFF913CFF)
        ),
        SaniThemeConfig(
            id = "teal_matrix",
            name = "Teal Matrix",
            category = "Cyber",
            background = Color(0xFF041B19),
            secondaryBackground = Color(0xFF092E2B),
            primaryAccent = Color(0xFF1DE9B6),
            secondaryAccent = Color(0xFF64FFDA),
            primaryText = Color(0xFFE9FFFA),
            secondaryText = Color(0xFFA0D1C8),
            glowCyan = Color(0xFF35DDEB),
            glowPurple = Color(0xFF913CFF)
        ),
        SaniThemeConfig(
            id = "royal_amethyst",
            name = "Royal Amethyst",
            category = "Luxury",
            background = Color(0xFF180D24),
            secondaryBackground = Color(0xFF2A173E),
            primaryAccent = Color(0xFFB66CFF),
            secondaryAccent = Color(0xFFFFB83F),
            primaryText = Color(0xFFF8F0FF),
            secondaryText = Color(0xFFC9B4DE),
            glowCyan = Color(0xFF35DDEB),
            glowPurple = Color(0xFF913CFF)
        )
    )

    val particleSpheres: List<AvatarOption> = listOf(
        AvatarOption(
            id = "sphere_neon_particle",
            name = "Neon Particle Sphere",
            subtitle = "Electric cyan & magenta dust",
            kind = AvatarKind.PARTICLE_SPHERE,
            primaryColor = Color(0xFF35DDEB),
            secondaryColor = Color(0xFF913CFF)
        ),
        AvatarOption(
            id = "sphere_deep_blue_star",
            name = "Deep Blue Star Sphere",
            subtitle = "Celestial orbital starfield",
            kind = AvatarKind.PARTICLE_SPHERE,
            primaryColor = Color(0xFF2FA8FF),
            secondaryColor = Color(0xFF9CE3FF)
        ),
        AvatarOption(
            id = "sphere_solar_gold",
            name = "Solar Gold Sphere",
            subtitle = "Radiant amber plasma swarm",
            kind = AvatarKind.PARTICLE_SPHERE,
            primaryColor = Color(0xFFFF852A),
            secondaryColor = Color(0xFFFFB83F)
        ),
        AvatarOption(
            id = "sphere_nebula_violet",
            name = "Nebula Violet Sphere",
            subtitle = "Quantum harmonic cluster",
            kind = AvatarKind.PARTICLE_SPHERE,
            primaryColor = Color(0xFF913CFF),
            secondaryColor = Color(0xFFFF6EC7)
        )
    )

    val logoOrbs: List<AvatarOption> = listOf(
        AvatarOption("orb_cyber_purple", "Cyber Violet Orb", "Signature Purple & Cyan", AvatarKind.LOGO_ORB, Color(0xFF913CFF), Color(0xFF35DDEB)),
        AvatarOption("orb_solar_blaze", "Solar Blaze Orb", "Molten Orange & Gold", AvatarKind.LOGO_ORB, Color(0xFFFF852A), Color(0xFFFFB83F)),
        AvatarOption("orb_aqua_pulse", "Aqua Pulse Orb", "Cyan & Ice Teal", AvatarKind.LOGO_ORB, Color(0xFF35DDEB), Color(0xFF1DE9B6)),
        AvatarOption("orb_crimson_nova", "Crimson Nova Orb", "Ruby Red & Amber", AvatarKind.LOGO_ORB, Color(0xFFFF4D4D), Color(0xFFFF964F)),
        AvatarOption("orb_emerald_core", "Emerald Core Orb", "Neon Mint & Lime", AvatarKind.LOGO_ORB, Color(0xFF2CE59B), Color(0xFFA8FF35)),
        AvatarOption("orb_rose_quartz", "Rose Quartz Orb", "Rose Pink & Champagne", AvatarKind.LOGO_ORB, Color(0xFFFF7A9E), Color(0xFFFFD875)),
        AvatarOption("orb_sapphire_halo", "Sapphire Halo Orb", "Cobalt Blue & Cyan", AvatarKind.LOGO_ORB, Color(0xFF4C8BFF), Color(0xFF35DDEB)),
        AvatarOption("orb_golden_crown", "Golden Crown Orb", "Pure Gold & Bronze", AvatarKind.LOGO_ORB, Color(0xFFFFB83F), Color(0xFFE58249)),
        AvatarOption("orb_magenta_prism", "Magenta Prism Orb", "Neon Pink & Violet", AvatarKind.LOGO_ORB, Color(0xFFE056FD), Color(0xFF7C6BFF)),
        AvatarOption("orb_aurora_wave", "Aurora Wave Orb", "Mint Green & Sky", AvatarKind.LOGO_ORB, Color(0xFF42F5C5), Color(0xFF70D6FF)),
        AvatarOption("orb_obsidian_spark", "Obsidian Spark Orb", "Silver & Electric Blue", AvatarKind.LOGO_ORB, Color(0xFFC2D0E6), Color(0xFF3D9BFF)),
        AvatarOption("orb_sunset_fusion", "Sunset Fusion Orb", "Coral Orange & Purple", AvatarKind.LOGO_ORB, Color(0xFFFF6D3F), Color(0xFF913CFF))
    )

    val originalSaniAvatar = AvatarOption(
        id = "original_sani",
        name = "Original SANI",
        subtitle = "Animated AI companion with blinking & lip-sync",
        kind = AvatarKind.ORIGINAL_SANI,
        primaryColor = Color(0xFFFF852A),
        secondaryColor = Color(0xFF35DDEB)
    )

    val micStyles: List<MicStyleOption> = listOf(
        MicStyleOption("neon_ring", "Neon Energy Ring", "Pulsing cyan & purple halo"),
        MicStyleOption("solar_capsule", "Solar Glow Capsule", "Warm orange & gold aura"),
        MicStyleOption("wave_spectrum", "Wave Spectrum", "Audio-reactive equalizer ring"),
        MicStyleOption("minimal_glass", "Minimal Glass", "Clean frosted amber beacon")
    )

    val supportedAutoReplyApps: List<AutoReplyApp> = listOf(
        AutoReplyApp("com.whatsapp", "WhatsApp"),
        AutoReplyApp("com.whatsapp.w4b", "WhatsApp Business"),
        AutoReplyApp("org.telegram.messenger", "Telegram"),
        AutoReplyApp("com.google.android.apps.messaging", "Google Messages / SMS"),
        AutoReplyApp("com.android.mms", "Android Messages"),
        AutoReplyApp("com.samsung.android.messaging", "Samsung Messages"),
        AutoReplyApp("com.facebook.orca", "Messenger"),
        AutoReplyApp("org.thoughtcrime.securesms", "Signal")
    )

    fun themeById(id: String): SaniThemeConfig =
        allThemes.firstOrNull { it.id == id } ?: allThemes.first()

    fun avatarById(id: String): AvatarOption {
        if (id == originalSaniAvatar.id) return originalSaniAvatar
        return (particleSpheres + logoOrbs).firstOrNull { it.id == id } ?: originalSaniAvatar
    }

    fun orbById(id: String): AvatarOption =
        logoOrbs.firstOrNull { it.id == id } ?: logoOrbs.first()
}
