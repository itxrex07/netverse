package app.netmirror.nmv2.data

enum class RefreshRateMode(
    val hz: Int,
    val title: String,
    val subtitle: String
) {
    HZ_60(
        hz = 60,
        title = "60Hz (Standard)",
        subtitle = "Standard smooth 60 FPS, maximum battery efficiency"
    ),
    HZ_90(
        hz = 90,
        title = "90Hz (Balanced)",
        subtitle = "Enhanced fluid scrolling with balanced battery consumption"
    ),
    HZ_120(
        hz = 120,
        title = "120Hz (Ultra Smooth 120 FPS)",
        subtitle = "Peak fluid animations on high refresh rate displays"
    ),
    VRR(
        hz = 0,
        title = "Dynamic / Variable (VRR)",
        subtitle = "Touch boost to maximum Hz while scrolling, drops to 60Hz when idle"
    );

    companion object {
        fun fromHz(hz: Int): RefreshRateMode = entries.find { it.hz == hz } ?: VRR
    }
}

enum class SettingsRoute {
    MAIN,
    APP_INFO,
    ADVANCED
}

sealed interface MirrorConnectionState {
    data object Resolving : MirrorConnectionState
    data class Ready(val url: String) : MirrorConnectionState
    data class Error(val message: String, val triedUrls: List<String>) : MirrorConnectionState
}

data class DisplayCapabilities(
    val activeHz: Int,
    val maxSupportedHz: Int,
    val supportedFixedRates: Set<Int>
) {
    fun isRateSupported(hz: Int): Boolean {
        if (hz == 0 || hz == 60) return true
        return supportedFixedRates.any { kotlin.math.abs(it - hz) <= 3 }
    }
}
