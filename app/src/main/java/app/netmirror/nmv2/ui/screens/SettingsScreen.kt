package app.netmirror.nmv2.ui.screens

import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.netmirror.nmv2.BuildConfig
import app.netmirror.nmv2.R
import app.netmirror.nmv2.data.DisplayCapabilities
import app.netmirror.nmv2.data.RefreshRateMode
import app.netmirror.nmv2.data.SettingsRoute
import app.netmirror.nmv2.ui.theme.NetMirrorColors

@Composable
fun SettingsSuiteScreen(
    currentRoute: SettingsRoute,
    isHdEnabled: Boolean,
    isHdSyncing: Boolean,
    selectedRefreshRate: RefreshRateMode,
    displayCapabilities: DisplayCapabilities,
    onCloseSettings: () -> Unit,
    onNavigateSubpage: (SettingsRoute) -> Unit,
    onNavigateBackFromSubpage: () -> Unit,
    onToggleHd: () -> Unit,
    onSelectRefreshRate: (RefreshRateMode) -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler(enabled = true) {
        if (currentRoute == SettingsRoute.MAIN) {
            onCloseSettings()
        } else {
            onNavigateBackFromSubpage()
        }
    }

    Surface(
        color = NetMirrorColors.SettingsBg,
        modifier = modifier
            .fillMaxSize()
            .testTag("settings_suite_container")
    ) {
        AnimatedContent(
            targetState = currentRoute,
            transitionSpec = {
                val enteringSubpage = targetState != SettingsRoute.MAIN
                if (enteringSubpage) {
                    (slideInHorizontally(animationSpec = tween(320)) { it } + fadeIn(tween(240)))
                        .togetherWith(slideOutHorizontally(animationSpec = tween(320)) { -it / 4 } + fadeOut(tween(200)))
                } else {
                    (slideInHorizontally(animationSpec = tween(320)) { -it / 4 } + fadeIn(tween(240)))
                        .togetherWith(slideOutHorizontally(animationSpec = tween(320)) { it } + fadeOut(tween(200)))
                }
            },
            label = "settings_navigation"
        ) { route ->
            when (route) {
                SettingsRoute.MAIN -> MainSettingsPage(
                    isHdEnabled = isHdEnabled,
                    isHdSyncing = isHdSyncing,
                    onClose = onCloseSettings,
                    onToggleHd = onToggleHd,
                    onOpenAppInfo = { onNavigateSubpage(SettingsRoute.APP_INFO) },
                    onOpenAdvanced = { onNavigateSubpage(SettingsRoute.ADVANCED) }
                )

                SettingsRoute.APP_INFO -> AppInfoSubpage(
                    selectedRefreshRate = selectedRefreshRate,
                    displayCapabilities = displayCapabilities,
                    onBack = onNavigateBackFromSubpage
                )

                SettingsRoute.ADVANCED -> AdvancedSettingsSubpage(
                    selectedMode = selectedRefreshRate,
                    displayCapabilities = displayCapabilities,
                    onBack = onNavigateBackFromSubpage,
                    onSelectMode = onSelectRefreshRate
                )
            }
        }
    }
}

@Composable
private fun SettingsHeader(
    title: String,
    isRootClose: Boolean,
    onBackOrClose: () -> Unit,
    testTag: String
) {
    Surface(
        color = NetMirrorColors.HeaderBg,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .windowInsetsPadding(WindowInsets.statusBars)
                    .padding(horizontal = 20.dp, vertical = 14.dp)
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(NetMirrorColors.BackButtonBg)
                        .border(1.dp, NetMirrorColors.BackButtonBorder, CircleShape)
                        .clickable(onClick = onBackOrClose)
                        .testTag(testTag)
                ) {
                    Icon(
                        imageVector = if (isRootClose) Icons.Rounded.Close else Icons.AutoMirrored.Rounded.ArrowBack,
                        contentDescription = stringResource(
                            if (isRootClose) R.string.cd_close_settings else R.string.cd_back
                        ),
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Spacer(modifier = Modifier.width(14.dp))

                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge
                )
            }
            HorizontalDivider(color = NetMirrorColors.CardBorder, thickness = 1.dp)
        }
    }
}

@Composable
private fun SettingsScrollContainer(
    content: @Composable ColumnScope.() -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .windowInsetsPadding(WindowInsets.navigationBars)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 720.dp)
                .padding(bottom = 48.dp),
            content = content
        )
    }
}

@Composable
private fun SectionHeader(text: String) {
    Text(
        text = text.uppercase(),
        style = MaterialTheme.typography.labelMedium,
        modifier = Modifier.padding(start = 12.dp, top = 20.dp, bottom = 10.dp)
    )
}

@Composable
private fun CardGroup(content: @Composable ColumnScope.() -> Unit) {
    Surface(
        color = NetMirrorColors.CardSurface,
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, NetMirrorColors.CardBorder),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(content = content)
    }
}

@Composable
private fun MainSettingsPage(
    isHdEnabled: Boolean,
    isHdSyncing: Boolean,
    onClose: () -> Unit,
    onToggleHd: () -> Unit,
    onOpenAppInfo: () -> Unit,
    onOpenAdvanced: () -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        SettingsHeader(
            title = stringResource(R.string.settings_title),
            isRootClose = true,
            onBackOrClose = onClose,
            testTag = "settings_close_button"
        )

        SettingsScrollContainer {
            SectionHeader(stringResource(R.string.settings_section_quality))
            CardGroup {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .minimumInteractiveComponentSize()
                        .clickable(enabled = !isHdSyncing, onClick = onToggleHd)
                        .padding(16.dp)
                        .testTag("settings_1080p_row")
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(44.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                Brush.linearGradient(
                                    colors = listOf(
                                        NetMirrorColors.CrimsonRed,
                                        NetMirrorColors.CrimsonGradientEnd
                                    )
                                )
                            )
                    ) {
                        Text(
                            text = "HD",
                            color = Color.White,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 14.sp
                        )
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.settings_1080p_title),
                            style = MaterialTheme.typography.titleMedium
                        )
                        Spacer(modifier = Modifier.height(3.dp))
                        Text(
                            text = stringResource(
                                if (isHdEnabled) R.string.settings_1080p_on else R.string.settings_1080p_off
                            ),
                            style = MaterialTheme.typography.bodySmall
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Switch(
                        checked = isHdEnabled,
                        onCheckedChange = { if (!isHdSyncing) onToggleHd() },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = NetMirrorColors.CrimsonRed,
                            uncheckedThumbColor = Color.White,
                            uncheckedTrackColor = NetMirrorColors.SwitchTrackOff,
                            uncheckedBorderColor = Color.Transparent
                        ),
                        modifier = Modifier.testTag("settings_1080p_switch")
                    )
                }
            }

            SectionHeader(stringResource(R.string.settings_section_general))
            CardGroup {
                SettingsNavigationRow(
                    icon = Icons.Rounded.Info,
                    iconTint = NetMirrorColors.InfoBlue,
                    iconBg = NetMirrorColors.InfoBlueBg,
                    title = stringResource(R.string.settings_app_info_title),
                    subtitle = stringResource(R.string.settings_app_info_subtitle),
                    onClick = onOpenAppInfo,
                    testTag = "settings_app_info_button"
                )
                HorizontalDivider(color = NetMirrorColors.DividerColor, thickness = 1.dp)
                SettingsNavigationRow(
                    icon = Icons.Rounded.Bolt,
                    iconTint = NetMirrorColors.AdvancedAmber,
                    iconBg = NetMirrorColors.AdvancedAmberBg,
                    title = stringResource(R.string.settings_advanced_title),
                    subtitle = stringResource(R.string.settings_advanced_subtitle),
                    onClick = onOpenAdvanced,
                    testTag = "settings_advanced_button"
                )
            }
        }
    }
}

@Composable
private fun SettingsNavigationRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconTint: Color,
    iconBg: Color,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    testTag: String
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .minimumInteractiveComponentSize()
            .clickable(onClick = onClick)
            .padding(16.dp)
            .testTag(testTag)
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(44.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(iconBg)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconTint,
                modifier = Modifier.size(22.dp)
            )
        }

        Spacer(modifier = Modifier.width(16.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(3.dp))
            Text(text = subtitle, style = MaterialTheme.typography.bodySmall)
        }

        Icon(
            imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
            contentDescription = null,
            tint = NetMirrorColors.TextMuted,
            modifier = Modifier.size(22.dp)
        )
    }
}

@Composable
private fun AppInfoSubpage(
    selectedRefreshRate: RefreshRateMode,
    displayCapabilities: DisplayCapabilities,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val supportedAbis = Build.SUPPORTED_ABIS.joinToString(", ").ifBlank { "Universal" }
    val motionSummary = when (selectedRefreshRate) {
        RefreshRateMode.VRR -> "Dynamic VRR (Up to ${displayCapabilities.maxSupportedHz}Hz) / 900ms Physics"
        else -> "${selectedRefreshRate.hz}Hz / 900ms Hardware-Accelerated Physics"
    }

    val specs = listOf(
        "Package Name" to context.packageName,
        "Target SDK" to "Android 15 (API 35)",
        "Architecture" to supportedAbis,
        "Motion Engine" to motionSummary,
        "Ad Policy" to "Automated Background Handshake (Zero Intrusive Popups)",
        "Security & Sandbox" to "Audited (No screen capture, no mic, zero trackers)",
        "Developer & Maintainer" to "smartworldarafath"
    )

    Column(modifier = Modifier.fillMaxSize()) {
        SettingsHeader(
            title = stringResource(R.string.app_info_title),
            isRootClose = false,
            onBackOrClose = onBack,
            testTag = "app_info_back_button"
        )

        SettingsScrollContainer {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 18.dp, bottom = 12.dp)
            ) {
                Image(
                    painter = painterResource(id = R.drawable.img_netmirror_logo),
                    contentDescription = stringResource(R.string.cd_netmirror_logo),
                    modifier = Modifier
                        .size(72.dp)
                        .clip(RoundedCornerShape(16.dp))
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = stringResource(R.string.app_name),
                    style = MaterialTheme.typography.headlineMedium
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Version ${BuildConfig.VERSION_NAME} (Build ${BuildConfig.VERSION_CODE})",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = NetMirrorColors.CrimsonRed,
                        fontWeight = FontWeight.SemiBold
                    )
                )
            }

            SectionHeader(stringResource(R.string.app_info_specs_header))
            CardGroup {
                specs.forEachIndexed { index, (label, value) ->
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Text(text = label, style = MaterialTheme.typography.titleMedium)
                        Spacer(modifier = Modifier.height(3.dp))
                        Text(text = value, style = MaterialTheme.typography.bodySmall)
                    }
                    if (index < specs.lastIndex) {
                        HorizontalDivider(color = NetMirrorColors.DividerColor, thickness = 1.dp)
                    }
                }
            }
        }
    }
}

@Composable
private fun AdvancedSettingsSubpage(
    selectedMode: RefreshRateMode,
    displayCapabilities: DisplayCapabilities,
    onBack: () -> Unit,
    onSelectMode: (RefreshRateMode) -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        SettingsHeader(
            title = stringResource(R.string.advanced_title),
            isRootClose = false,
            onBackOrClose = onBack,
            testTag = "advanced_back_button"
        )

        SettingsScrollContainer {
            SectionHeader(stringResource(R.string.advanced_refresh_header))
            CardGroup {
                RefreshRateMode.entries.forEachIndexed { index, mode ->
                    val isSelected = selectedMode == mode
                    val supported = displayCapabilities.isRateSupported(mode.hz)
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .minimumInteractiveComponentSize()
                            .clickable { onSelectMode(mode) }
                            .padding(16.dp)
                            .testTag("refresh_rate_option_${mode.hz}")
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(22.dp)
                                .clip(CircleShape)
                                .background(
                                    if (isSelected) NetMirrorColors.CrimsonRed else Color.Transparent
                                )
                                .border(
                                    width = 2.dp,
                                    color = if (isSelected) NetMirrorColors.CrimsonRed else NetMirrorColors.TextMuted,
                                    shape = CircleShape
                                )
                        ) {
                            if (isSelected) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(Color.White)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(16.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = mode.title,
                                    style = MaterialTheme.typography.titleMedium
                                )
                                if (!supported) {
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Unsupported",
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            color = NetMirrorColors.TextMuted,
                                            fontSize = 11.sp
                                        )
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(3.dp))
                            Text(
                                text = mode.subtitle,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }

                    if (index < RefreshRateMode.entries.lastIndex) {
                        HorizontalDivider(color = NetMirrorColors.DividerColor, thickness = 1.dp)
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "Detected Hardware Max: ${displayCapabilities.maxSupportedHz}Hz (Active: ${displayCapabilities.activeHz}Hz). ${stringResource(R.string.advanced_refresh_note)}",
                style = MaterialTheme.typography.bodySmall.copy(
                    color = NetMirrorColors.TextMuted,
                    fontSize = 12.sp
                ),
                modifier = Modifier.padding(horizontal = 16.dp)
            )
        }
    }
}
