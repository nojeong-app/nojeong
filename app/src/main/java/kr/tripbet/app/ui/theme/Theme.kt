package kr.tripbet.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** 크기·간격 규칙(디자인/디자인_규칙.md 5절). 화면 코드는 숫자를 직접 쓰지 말고 이 값을 쓴다. */
object TripDims {
    /** 터치 칸 최소 크기 — Google 차량 기준 76dp. 글자만 작아도 누르는 칸은 이 크기 이상. */
    val Touch: Dp = 76.dp
    /** 터치 칸 사이 간격 — Google 권장 24dp 이상. */
    val TouchGap: Dp = 24.dp
    /** 화면 바깥 여백. */
    val ScreenPadding: Dp = 64.dp
    /** 카드 안쪽 여백. */
    val CardPadding: Dp = 40.dp
    /** 카드 사이 간격. */
    val CardGap: Dp = 24.dp
    /** 카드 모서리 32(v3 시안 기준, Pleos 문서에 수치 없음). */
    val CardRadius: Dp = 32.dp
    /** 버튼·칩 모서리. */
    val ButtonRadius: Dp = 38.dp   // 높이 76의 절반: 알약 모양
    /** 아이콘: 주 44 / 보조 36 / 3차 24 (Google). Pleos 격자는 48. */
    val IconMain: Dp = 44.dp
    val IconSub: Dp = 36.dp
    val IconTertiary: Dp = 24.dp
}

val LocalTripColors = staticCompositionLocalOf { TripColorsLight }

object TripTheme {
    val colors: TripColors
        @Composable @ReadOnlyComposable get() = LocalTripColors.current
}

/**
 * 앱 전체 테마. 앱이 따로 고르지 않고 차량 시스템의 낮/밤 설정(uiMode night)을 그대로 따른다.
 * darkTheme 기본값 isSystemInDarkTheme() 가 그 값이다 — Pleos 요구: 조명에 따라 밝기·대비 자동 조정.
 * 화면 코드에서 darkColorScheme() 을 직접 쓰지 말고 반드시 TripBetTheme { } 로 감쌀 것.
 */
@Composable
fun TripBetTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val c = if (darkTheme) TripColorsDark else TripColorsLight

    // Material3 부품(기본 Text·Button 등)이 우리 색과 글꼴을 따라가도록 이어 준다.
    val scheme = (if (darkTheme) darkColorScheme() else lightColorScheme()).copy(
        primary = c.primary, onPrimary = c.onPrimary,
        secondary = c.active, onSecondary = c.onActive,
        tertiary = c.warning, onTertiary = c.onWarning,
        error = c.negative, onError = c.onNegative,
        background = c.background, onBackground = c.text,
        surface = c.surface, onSurface = c.text,
        surfaceVariant = c.surfaceHigh, onSurfaceVariant = c.textSecondary,
        outline = c.outline, outlineVariant = c.outlineMuted,
        surfaceContainer = c.surface, surfaceContainerHigh = c.surfaceHigh,
    )

    val typography = Typography(
        displayLarge = TripType.HeadlineLarge, displayMedium = TripType.HeadlineMedium, displaySmall = TripType.HeadlineSmall,
        headlineLarge = TripType.TitleLarge, headlineMedium = TripType.TitleMedium, headlineSmall = TripType.TitleSmall,
        titleLarge = TripType.TitleSmall, titleMedium = TripType.BodyLarge, titleSmall = TripType.BodyMedium,
        bodyLarge = TripType.BodyLarge, bodyMedium = TripType.BodyMedium, bodySmall = TripType.BodySmall,
        labelLarge = TripType.BodySmall, labelMedium = TripType.LabelMedium, labelSmall = TripType.LabelSmall,
    )

    CompositionLocalProvider(LocalTripColors provides c) {
        MaterialTheme(colorScheme = scheme, typography = typography, content = content)
    }
}
