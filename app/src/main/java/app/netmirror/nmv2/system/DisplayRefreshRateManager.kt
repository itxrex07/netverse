package app.netmirror.nmv2.system

import android.app.Activity
import android.os.Build
import android.view.Display
import app.netmirror.nmv2.data.DisplayCapabilities
import kotlin.math.abs
import kotlin.math.roundToInt

object DisplayRefreshRateManager {

    fun queryCapabilities(activity: Activity): DisplayCapabilities {
        val display = getDisplay(activity)
            ?: return DisplayCapabilities(
                activeHz = 60,
                maxSupportedHz = 60,
                supportedFixedRates = setOf(60)
            )

        val modes = display.supportedModes
        val currentMode = display.mode
        val supportedRates = modes
            .map { it.refreshRate.roundToInt() }
            .filter { it >= 50 }
            .toSet()
            .ifEmpty { setOf(60) }

        val maxHz = supportedRates.maxOrNull() ?: 60
        return DisplayCapabilities(
            activeHz = currentMode.refreshRate.roundToInt(),
            maxSupportedHz = maxHz,
            supportedFixedRates = supportedRates
        )
    }

    /**
     * Applies the requested refresh rate (0 = VRR/Dynamic, 60, 90, 120).
     * Returns true if supported and applied, false if the hardware doesn't support the fixed rate.
     */
    fun applyRefreshRate(activity: Activity, targetHz: Int): Boolean {
        val display = getDisplay(activity) ?: return targetHz == 0 || targetHz == 60
        val modes = display.supportedModes.toList()
        val window = activity.window ?: return false
        val params = window.attributes

        if (targetHz == 0) {
            // Dynamic / Variable (VRR): prefer system default or highest refresh mode matching current resolution
            val currentMode = display.mode
            val bestMode = modes
                .filter { it.physicalWidth == currentMode.physicalWidth && it.physicalHeight == currentMode.physicalHeight }
                .maxByOrNull { it.refreshRate }
            params.preferredDisplayModeId = bestMode?.modeId ?: 0
            window.attributes = params
            return true
        }

        val currentMode = display.mode
        val matchingMode = modes
            .filter {
                it.physicalWidth == currentMode.physicalWidth &&
                    it.physicalHeight == currentMode.physicalHeight &&
                    abs(it.refreshRate - targetHz.toFloat()) <= 3.0f
            }
            .minByOrNull { abs(it.refreshRate - targetHz.toFloat()) }
            ?: modes
                .filter { abs(it.refreshRate - targetHz.toFloat()) <= 3.0f }
                .minByOrNull { abs(it.refreshRate - targetHz.toFloat()) }

        if (matchingMode == null && targetHz > 60) {
            // Enforce standard 60Hz fallback when device lacks physical 90Hz/120Hz hardware
            val fallback60 = modes.minByOrNull { abs(it.refreshRate - 60f) }
            if (fallback60 != null) {
                params.preferredDisplayModeId = fallback60.modeId
                window.attributes = params
            }
            return false
        }

        if (matchingMode != null) {
            params.preferredDisplayModeId = matchingMode.modeId
            window.attributes = params
        }
        return true
    }

    @Suppress("DEPRECATION")
    private fun getDisplay(activity: Activity): Display? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            activity.display
        } else {
            activity.windowManager.defaultDisplay
        }
    }
}
