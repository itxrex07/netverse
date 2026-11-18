package app.netmirror.nmv2.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import app.netmirror.nmv2.R

val DisplayFontFamily = FontFamily(
    Font(R.font.plus_jakarta_sans, FontWeight.Normal),
    Font(R.font.plus_jakarta_sans, FontWeight.SemiBold),
    Font(R.font.plus_jakarta_sans, FontWeight.Bold),
    Font(R.font.plus_jakarta_sans, FontWeight.ExtraBold)
)

val BodyFontFamily = FontFamily(
    Font(R.font.inter, FontWeight.Normal),
    Font(R.font.inter, FontWeight.Medium),
    Font(R.font.inter, FontWeight.SemiBold),
    Font(R.font.inter, FontWeight.Bold)
)

val NetMirrorTypography = Typography(
    headlineMedium = TextStyle(
        fontFamily = DisplayFontFamily,
        fontWeight = FontWeight.ExtraBold,
        fontSize = 22.sp,
        lineHeight = 28.sp,
        color = NetMirrorColors.TextPrimary
    ),
    titleLarge = TextStyle(
        fontFamily = DisplayFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 20.sp,
        lineHeight = 26.sp,
        letterSpacing = 0.3.sp,
        color = NetMirrorColors.TextPrimary
    ),
    titleMedium = TextStyle(
        fontFamily = BodyFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 16.sp,
        lineHeight = 22.sp,
        color = NetMirrorColors.TextBody
    ),
    bodyMedium = TextStyle(
        fontFamily = BodyFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        color = NetMirrorColors.TextSecondary
    ),
    bodySmall = TextStyle(
        fontFamily = BodyFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 13.sp,
        lineHeight = 17.sp,
        color = NetMirrorColors.TextSecondary
    ),
    labelMedium = TextStyle(
        fontFamily = DisplayFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 1.2.sp,
        color = NetMirrorColors.TextSecondary
    )
)

private val NetMirrorDarkColorScheme = darkColorScheme(
    primary = NetMirrorColors.CrimsonRed,
    onPrimary = Color.White,
    secondary = NetMirrorColors.InfoBlue,
    onSecondary = Color.White,
    tertiary = NetMirrorColors.UpdateGreen,
    onTertiary = Color.White,
    background = NetMirrorColors.BgDeep,
    onBackground = NetMirrorColors.TextPrimary,
    surface = NetMirrorColors.SettingsBg,
    onSurface = NetMirrorColors.TextPrimary,
    surfaceVariant = NetMirrorColors.CardSurfaceSolid,
    onSurfaceVariant = NetMirrorColors.TextSecondary,
    outline = NetMirrorColors.CardBorder
)

@Composable
fun NetMirrorTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = NetMirrorDarkColorScheme,
        typography = NetMirrorTypography,
        content = content
    )
}
