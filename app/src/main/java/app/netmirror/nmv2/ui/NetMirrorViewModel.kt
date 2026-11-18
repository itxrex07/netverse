package app.netmirror.nmv2.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import app.netmirror.nmv2.data.DisplayCapabilities
import app.netmirror.nmv2.data.MirrorConnectionState
import app.netmirror.nmv2.data.NetMirrorRepository
import app.netmirror.nmv2.data.RefreshRateMode
import app.netmirror.nmv2.data.SettingsPreferences
import app.netmirror.nmv2.data.SettingsRoute
import app.netmirror.nmv2.system.NetworkMonitor
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class NetMirrorViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = NetMirrorRepository()
    private val preferences = SettingsPreferences(application.applicationContext)
    private val networkMonitor = NetworkMonitor(application.applicationContext)

    private val _mirrorState = MutableStateFlow<MirrorConnectionState>(MirrorConnectionState.Resolving)
    val mirrorState: StateFlow<MirrorConnectionState> = _mirrorState.asStateFlow()

    private val _isPageLoading = MutableStateFlow(true)
    val isPageLoading: StateFlow<Boolean> = _isPageLoading.asStateFlow()

    private val _isContentReady = MutableStateFlow(false)
    val isContentReady: StateFlow<Boolean> = _isContentReady.asStateFlow()

    private var contentFallbackJob: Job? = null

    private val _isSettingsOpen = MutableStateFlow(false)
    val isSettingsOpen: StateFlow<Boolean> = _isSettingsOpen.asStateFlow()

    private val _settingsRoute = MutableStateFlow(SettingsRoute.MAIN)
    val settingsRoute: StateFlow<SettingsRoute> = _settingsRoute.asStateFlow()

    private val _isHdEnabled = MutableStateFlow(false)
    val isHdEnabled: StateFlow<Boolean> = _isHdEnabled.asStateFlow()

    private val _isHdSyncing = MutableStateFlow(false)
    val isHdSyncing: StateFlow<Boolean> = _isHdSyncing.asStateFlow()

    private val _selectedRefreshRate = MutableStateFlow(RefreshRateMode.VRR)
    val selectedRefreshRate: StateFlow<RefreshRateMode> = _selectedRefreshRate.asStateFlow()

    private val _displayCapabilities = MutableStateFlow(
        DisplayCapabilities(activeHz = 60, maxSupportedHz = 60, supportedFixedRates = setOf(60))
    )
    val displayCapabilities: StateFlow<DisplayCapabilities> = _displayCapabilities.asStateFlow()

    private val _isPlayerFullscreen = MutableStateFlow(false)
    val isPlayerFullscreen: StateFlow<Boolean> = _isPlayerFullscreen.asStateFlow()

    private val _toastMessage = MutableStateFlow<String?>(null)
    val toastMessage: StateFlow<String?> = _toastMessage.asStateFlow()

    init {
        viewModelScope.launch {
            _isHdEnabled.value = preferences.isHdEnabled.first()
            val savedHz = preferences.preferredRefreshRateHz.first()
            _selectedRefreshRate.value = RefreshRateMode.fromHz(savedHz)
            resolveMirror()
        }
        viewModelScope.launch {
            networkMonitor.isOnline.collect { online ->
                if (online && _mirrorState.value is MirrorConnectionState.Error) {
                    resolveMirror()
                }
            }
        }
    }

    fun resolveMirror() {
        viewModelScope.launch {
            if (!networkMonitor.isCurrentlyConnected()) {
                _isPageLoading.value = false
                _isContentReady.value = false
                _mirrorState.value = MirrorConnectionState.Error(
                    message = "Device is offline. Connect to Wi-Fi or mobile data and retry.",
                    triedUrls = emptyList()
                )
                return@launch
            }
            _mirrorState.value = MirrorConnectionState.Resolving
            _isPageLoading.value = true
            val cached = preferences.lastMirrorUrl.first()
            val result = repository.resolveActiveMirror(cached)
            result.onSuccess { url ->
                preferences.setLastMirrorUrl(url)
                // Restore persisted 1080p cookie before loading
                repository.syncHdCookie(url, _isHdEnabled.value)
                _mirrorState.value = MirrorConnectionState.Ready(url)
            }.onFailure { err ->
                _isPageLoading.value = false
                _mirrorState.value = MirrorConnectionState.Error(
                    message = err.message ?: "Unable to connect to NetMirror.",
                    triedUrls = NetMirrorRepository.PRIMARY_MIRRORS + NetMirrorRepository.CHECK_PHP_HOSTS
                )
            }
        }
    }

    fun onWebViewPageStarted() {
        _isPageLoading.value = true
    }

    fun onWebViewPageFinished(currentUrl: String?) {
        _isPageLoading.value = false
        val cookieHd = repository.readHdCookie(currentUrl)
        if (cookieHd != null && cookieHd != _isHdEnabled.value) {
            _isHdEnabled.value = cookieHd
            viewModelScope.launch { preferences.setHdEnabled(cookieHd) }
        }
        // Safety fallback so loader never stays pinned if a mirror uses custom markup
        if (!_isContentReady.value) {
            contentFallbackJob?.cancel()
            contentFallbackJob = viewModelScope.launch {
                delay(6500)
                _isContentReady.value = true
            }
        }
    }

    fun onContentReady(ready: Boolean) {
        if (ready) {
            contentFallbackJob?.cancel()
            _isContentReady.value = true
        } else {
            _isContentReady.value = false
        }
    }

    fun onWebViewError(description: String) {
        contentFallbackJob?.cancel()
        _isPageLoading.value = false
        _isContentReady.value = false
        _mirrorState.value = MirrorConnectionState.Error(
            message = description,
            triedUrls = NetMirrorRepository.PRIMARY_MIRRORS
        )
    }

    fun openSettings() {
        _settingsRoute.value = SettingsRoute.MAIN
        _isSettingsOpen.value = true
    }

    fun closeSettings() {
        _settingsRoute.value = SettingsRoute.MAIN
        _isSettingsOpen.value = false
    }

    fun navigateSettingsSubpage(route: SettingsRoute) {
        _settingsRoute.value = route
    }

    fun navigateBackFromSubpage() {
        _settingsRoute.value = SettingsRoute.MAIN
    }

    fun toggleHdQuality(activePageUrl: String?, onSyncedToWebView: (Boolean) -> Unit) {
        if (_isHdSyncing.value) return
        viewModelScope.launch {
            _isHdSyncing.value = true
            val current = _isHdEnabled.value
            val result = repository.toggleHdSetting(activePageUrl, current)
            result.onSuccess { isHdOn ->
                _isHdEnabled.value = isHdOn
                preferences.setHdEnabled(isHdOn)
                onSyncedToWebView(isHdOn)
                showToast(if (isHdOn) "1080p Full HD Enabled" else "1080p Standard Quality Enabled")
            }.onFailure {
                showToast("Error toggling 1080p setting")
            }
            _isHdSyncing.value = false
        }
    }

    fun updateDisplayCapabilities(capabilities: DisplayCapabilities) {
        _displayCapabilities.value = capabilities
    }

    fun selectRefreshRate(mode: RefreshRateMode, applyToWindow: (Int) -> Boolean) {
        val applied = applyToWindow(mode.hz)
        if (applied) {
            _selectedRefreshRate.value = mode
            viewModelScope.launch { preferences.setPreferredRefreshRateHz(mode.hz) }
            val label = if (mode.hz == 0) "VRR (Dynamic)" else "${mode.hz}Hz"
            showToast("Refresh rate set to $label")
        } else {
            showToast("Your Device Doesn't Support ${mode.hz}Hz")
        }
    }

    fun setPlayerFullscreen(fullscreen: Boolean) {
        _isPlayerFullscreen.value = fullscreen
    }

    fun showToast(message: String) {
        _toastMessage.value = message
    }

    fun clearToast() {
        _toastMessage.value = null
    }
}
