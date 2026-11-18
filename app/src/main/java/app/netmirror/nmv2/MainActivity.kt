package app.netmirror.nmv2

import android.content.pm.ActivityInfo
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.netmirror.nmv2.data.MirrorConnectionState
import app.netmirror.nmv2.system.DisplayRefreshRateManager
import app.netmirror.nmv2.ui.NetMirrorViewModel
import app.netmirror.nmv2.ui.components.StartupLoaderOverlay
import app.netmirror.nmv2.ui.screens.OfflineGatewayScreen
import app.netmirror.nmv2.ui.screens.SettingsSuiteScreen
import app.netmirror.nmv2.ui.theme.NetMirrorColors
import app.netmirror.nmv2.ui.theme.NetMirrorTheme
import app.netmirror.nmv2.webview.NetMirrorJsBridge
import app.netmirror.nmv2.webview.NetMirrorWebView
import app.netmirror.nmv2.webview.WebViewController
import kotlinx.coroutines.delay

class MainActivity : ComponentActivity() {

    private val viewModel: NetMirrorViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(NetMirrorColors.BgDeep.toArgb()),
            navigationBarStyle = SystemBarStyle.dark(NetMirrorColors.BgDeep.toArgb())
        )

        val capabilities = DisplayRefreshRateManager.queryCapabilities(this)
        viewModel.updateDisplayCapabilities(capabilities)

        setContent {
            NetMirrorTheme {
                NetMirrorRootApp(
                    viewModel = viewModel,
                    onApplyRefreshRate = { hz ->
                        val ok = DisplayRefreshRateManager.applyRefreshRate(this, hz)
                        viewModel.updateDisplayCapabilities(
                            DisplayRefreshRateManager.queryCapabilities(this)
                        )
                        ok
                    },
                    onFullscreenToggle = { fullscreen ->
                        setSystemFullscreen(fullscreen)
                    },
                    onExitApp = {
                        finish()
                    }
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        val currentHz = viewModel.selectedRefreshRate.value.hz
        DisplayRefreshRateManager.applyRefreshRate(this, currentHz)
        viewModel.updateDisplayCapabilities(DisplayRefreshRateManager.queryCapabilities(this))
    }

    private fun setSystemFullscreen(fullscreen: Boolean) {
        val controller = WindowCompat.getInsetsController(window, window.decorView)
        if (fullscreen) {
            controller.systemBarsBehavior =
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            controller.hide(WindowInsetsCompat.Type.systemBars())
            requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
        } else {
            controller.show(WindowInsetsCompat.Type.systemBars())
            requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_FULL_SENSOR
        }
    }
}

@Composable
private fun NetMirrorRootApp(
    viewModel: NetMirrorViewModel,
    onApplyRefreshRate: (Int) -> Boolean,
    onFullscreenToggle: (Boolean) -> Unit,
    onExitApp: () -> Unit
) {
    val mirrorState by viewModel.mirrorState.collectAsStateWithLifecycle()
    val isPageLoading by viewModel.isPageLoading.collectAsStateWithLifecycle()
    val isContentReady by viewModel.isContentReady.collectAsStateWithLifecycle()
    val isSettingsOpen by viewModel.isSettingsOpen.collectAsStateWithLifecycle()
    val settingsRoute by viewModel.settingsRoute.collectAsStateWithLifecycle()
    val isHdEnabled by viewModel.isHdEnabled.collectAsStateWithLifecycle()
    val isHdSyncing by viewModel.isHdSyncing.collectAsStateWithLifecycle()
    val selectedRefreshRate by viewModel.selectedRefreshRate.collectAsStateWithLifecycle()
    val displayCapabilities by viewModel.displayCapabilities.collectAsStateWithLifecycle()
    val isPlayerFullscreen by viewModel.isPlayerFullscreen.collectAsStateWithLifecycle()
    val toastMessage by viewModel.toastMessage.collectAsStateWithLifecycle()

    val webViewController = remember { WebViewController() }

    val jsBridge = remember(webViewController) {
        NetMirrorJsBridge(
            onShowToast = { msg -> viewModel.showToast(msg) },
            onSetRefreshRate = { rate -> onApplyRefreshRate(rate) },
            onOpenSettings = { viewModel.openSettings() },
            onExitApp = onExitApp,
            onPlayerStateChanged = { active -> viewModel.setPlayerFullscreen(active) },
            onContentReady = { ready -> viewModel.onContentReady(ready) }
        )
    }

    // Apply persisted refresh rate on first composition
    LaunchedEffect(selectedRefreshRate) {
        onApplyRefreshRate(selectedRefreshRate.hz)
    }

    // Auto-dismiss branded toast
    LaunchedEffect(toastMessage) {
        if (toastMessage != null) {
            delay(2500)
            viewModel.clearToast()
        }
    }

    // Root back navigation when WebView is active
    BackHandler(enabled = !isSettingsOpen) {
        if (isPlayerFullscreen) {
            webViewController.evaluatePressFromApp { _ ->
                viewModel.setPlayerFullscreen(false)
                onFullscreenToggle(false)
            }
        } else {
            webViewController.evaluatePressFromApp { handled ->
                if (!handled) {
                    if (webViewController.canGoBack()) {
                        webViewController.goBack()
                    } else {
                        onExitApp()
                    }
                }
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(NetMirrorColors.BgDeep)
    ) {
        when (val state = mirrorState) {
            is MirrorConnectionState.Resolving -> {
                StartupLoaderOverlay(
                    statusMessage = stringResource(R.string.loading_mirror_probe)
                )
            }

            is MirrorConnectionState.Ready -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .then(
                            if (isPlayerFullscreen) Modifier
                            else Modifier
                                .windowInsetsPadding(WindowInsets.statusBars)
                                .windowInsetsPadding(WindowInsets.navigationBars)
                        )
                ) {
                    NetMirrorWebView(
                        url = state.url,
                        controller = webViewController,
                        jsBridge = jsBridge,
                        onPageStarted = { viewModel.onWebViewPageStarted() },
                        onPageFinished = { loadedUrl -> viewModel.onWebViewPageFinished(loadedUrl) },
                        onPageError = { desc -> viewModel.onWebViewError(desc) },
                        onFullscreenChanged = { fullscreen ->
                            viewModel.setPlayerFullscreen(fullscreen)
                            onFullscreenToggle(fullscreen)
                        },
                        modifier = Modifier.fillMaxSize()
                    )
                }

                // Subtle native Settings trigger when WebView is ready and not in fullscreen video
                AnimatedVisibility(
                    visible = !isPlayerFullscreen && !isSettingsOpen && !isPageLoading && isContentReady,
                    enter = fadeIn(tween(250)),
                    exit = fadeOut(tween(200)),
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .windowInsetsPadding(WindowInsets.statusBars)
                        .padding(top = 10.dp, end = 12.dp)
                ) {
                    Surface(
                        color = NetMirrorColors.HeaderBg,
                        shape = CircleShape,
                        border = BorderStroke(1.dp, NetMirrorColors.BackButtonBorder),
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .size(44.dp)
                            .clip(CircleShape)
                            .clickable { viewModel.openSettings() }
                            .testTag("floating_settings_button")
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Rounded.Settings,
                                contentDescription = stringResource(R.string.cd_open_settings),
                                tint = NetMirrorColors.TextPrimary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }

            is MirrorConnectionState.Error -> {
                OfflineGatewayScreen(
                    errorMessage = state.message,
                    onRetry = { viewModel.resolveMirror() },
                    onOpenSettings = { viewModel.openSettings() }
                )
            }
        }

        // Seamless Startup Loader while mirror loads or server ad-gate is skipped
        AnimatedVisibility(
            visible = mirrorState is MirrorConnectionState.Ready && (isPageLoading || !isContentReady),
            enter = fadeIn(tween(150)),
            exit = fadeOut(tween(450))
        ) {
            StartupLoaderOverlay(
                statusMessage = stringResource(R.string.loading_banner_text)
            )
        }

        // Dedicated Full-Screen Settings Suite
        AnimatedVisibility(
            visible = isSettingsOpen,
            enter = slideInHorizontally(animationSpec = tween(350)) { it } + fadeIn(tween(250)),
            exit = slideOutHorizontally(animationSpec = tween(320)) { it } + fadeOut(tween(220))
        ) {
            SettingsSuiteScreen(
                currentRoute = settingsRoute,
                isHdEnabled = isHdEnabled,
                isHdSyncing = isHdSyncing,
                selectedRefreshRate = selectedRefreshRate,
                displayCapabilities = displayCapabilities,
                onCloseSettings = { viewModel.closeSettings() },
                onNavigateSubpage = { route -> viewModel.navigateSettingsSubpage(route) },
                onNavigateBackFromSubpage = { viewModel.navigateBackFromSubpage() },
                onToggleHd = {
                    viewModel.toggleHdQuality(webViewController.currentUrl()) { hdOn ->
                        webViewController.syncHdCookieToWebView(hdOn)
                    }
                },
                onSelectRefreshRate = { mode ->
                    viewModel.selectRefreshRate(mode, onApplyRefreshRate)
                }
            )
        }

        // Branded NetMirror Toast Overlay
        AnimatedVisibility(
            visible = toastMessage != null,
            enter = slideInVertically(tween(250)) { it / 2 } + fadeIn(tween(250)),
            exit = slideOutVertically(tween(250)) { it / 2 } + fadeOut(tween(200)),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .windowInsetsPadding(WindowInsets.navigationBars)
                .padding(bottom = 84.dp, start = 24.dp, end = 24.dp)
        ) {
            Surface(
                color = NetMirrorColors.ToastBg,
                shape = RoundedCornerShape(30.dp),
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.15f)),
                shadowElevation = 10.dp
            ) {
                Text(
                    text = toastMessage.orEmpty(),
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp)
                )
            }
        }
    }
}
