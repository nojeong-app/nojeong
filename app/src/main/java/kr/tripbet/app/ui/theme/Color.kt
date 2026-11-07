package kr.tripbet.app.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

/**
 * 색 이름표 — 노정 한국 스타일 D안(디자인/시안/v5_한국/D안_적용사항.md).
 * 낮은 한지색(무늬 없는 단색), 밤은 먹색. 주 색은 쪽빛, 도장은 단청 적, 조각보는 쪽빛·황토·단청 초록·연분홍.
 * 글자 대비는 모두 4.5:1 이상(검산은 디자인/시안/v5_한국/설명.md), 색 칸(그림 요소)은 3:1 이상.
 * 색을 바꿀 때는 이 파일의 TripColorsLight / TripColorsDark 두 곳만 고친다.
 * 현대차·Pleos 로고나 브랜드 외관은 쓰지 않는다.
 */
@Immutable
data class TripColors(
    val isDark: Boolean,
    // 바탕·면
    val background: Color,        // 앱 전체 바탕(낮 한지색, 밤 먹색)
    val surface: Color,           // 카드
    val surfaceHigh: Color,       // 카드 안의 눌러 보이는 칸
    val surfaceAccent: Color,     // 가장 진한 면(칩 선택 바탕 등)
    val popup: Color,             // 팝업·시트
    val outline: Color,           // 구분선(선명)
    val outlineMuted: Color,      // 구분선(옅음)
    // 글자
    val text: Color,              // 주 글자
    val textSecondary: Color,     // 보조 글자 (대비 4.5:1 이상 확인된 색)
    val textDisabled: Color,      // 쓸 수 없는 칸 (비활성 전용)
    // 의미 색
    val primary: Color,           // 쪽빛: 주 버튼·진행
    val onPrimary: Color,         // 쪽빛 면 위 글자
    val active: Color,            // 켜짐·이김 배지(글자는 어두운 색)
    val onActive: Color,
    val warning: Color,           // 주의(주유소 등)
    val onWarning: Color,
    val negative: Color,          // 단청 적: 진 사람·경고
    val onNegative: Color,        // 적색 면 위 글자
    val point: Color,             // 노랑 포인트(국물, 꼬리표 등)
    // 버튼
    val buttonFilled: Color,
    val buttonFilledPressed: Color,
    val onButtonFilled: Color,
    val buttonBasic: Color,
    val buttonBasicPressed: Color,
    // 픽셀 진행 표시
    val pixelOn: Color,
    val pixelOff: Color,          // 남은 칸(창살 바탕)
    val pixelStop: Color,         // 경유지 표식
    // ── 한국 스타일 D안 ──
    val softPrimary: Color,       // 조각보 연한 쪽빛
    val ochre: Color,             // 조각보 황토
    val green: Color,             // 조각보 단청 초록
    val pink: Color,              // 조각보 연분홍
    val stamp: Color,             // 도장 단청 적
    val onStamp: Color,           // 도장 글자
    val pot: Color,               // 뚝배기 몸
    val potDark: Color,           // 뚝배기 굽
    val roof: Color,              // 기와 지붕
)

private val Ink = Color(0xFF131417)

val TripColorsLight = TripColors(
    isDark = false,
    background = Color(0xFFF6F1E7),      // 한지색(무늬 없음)
    surface = Color(0xFFFFFCF5),
    surfaceHigh = Color(0xFFECE5D6),
    surfaceAccent = Color(0xFFDDD4C2),
    popup = Color(0xFFFFFCF5),
    outline = Color(0xFFCFC6B3),
    outlineMuted = Color(0xFFE2DACA),
    text = Color(0xFF1B1814),
    textSecondary = Color(0xFF6B6458),   // 한지 위 5.2:1, 카드 위 5.7:1
    textDisabled = Color(0xFFA39A89),
    primary = Color(0xFF1E4DA0),         // 쪽빛
    onPrimary = Color(0xFFFFFFFF),       // 8.0:1
    active = Color(0xFF02C265),
    onActive = Ink,
    warning = Color(0xFFFF8A00),
    onWarning = Ink,
    negative = Color(0xFFB8392A),        // 단청 적
    onNegative = Color(0xFFFFFFFF),      // 5.7:1
    point = Color(0xFFFFC224),
    buttonFilled = Color(0xFF4A453D),
    buttonFilledPressed = Color(0xFF34302A),
    onButtonFilled = Color(0xFFFFFFFF),
    buttonBasic = Color(0x0D1B1814),
    buttonBasicPressed = Color(0x1A1B1814),
    pixelOn = Color(0xFF1E4DA0),
    pixelOff = Color(0xFFCFC6B3),
    pixelStop = Color(0xFF2E8A68),
    softPrimary = Color(0xFF6A8CC8),
    ochre = Color(0xFFB07A33),
    green = Color(0xFF2E8A68),
    pink = Color(0xFFC9627C),
    stamp = Color(0xFFB8392A),
    onStamp = Color(0xFFFFFFFF),
    pot = Color(0xFFA8693F),
    potDark = Color(0xFF6E4527),
    roof = Color(0xFF4A453D),
)

val TripColorsDark = TripColors(
    isDark = true,
    background = Color(0xFF15130F),      // 먹색
    surface = Color(0xFF1F1C17),
    surfaceHigh = Color(0xFF34302A),
    surfaceAccent = Color(0xFF4A453D),
    popup = Color(0xFF34302A),
    outline = Color(0xFF4A453D),
    outlineMuted = Color(0xFF34302A),
    text = Color(0xFFF4EFE6),
    textSecondary = Color(0xFFB8B1A6),   // 먹색 위 8.7:1
    textDisabled = Color(0xFF6E675C),
    primary = Color(0xFF6A9AF2),         // 쪽빛
    onPrimary = Color(0xFF15130F),       // 6.6:1
    active = Color(0xFF00E074),
    onActive = Ink,
    warning = Color(0xFFFF8A00),
    onWarning = Ink,
    negative = Color(0xFFD6523C),        // 단청 적
    onNegative = Color(0xFF15130F),      // 4.5:1
    point = Color(0xFFFFC224),
    buttonFilled = Color(0xFFE4DDD0),
    buttonFilledPressed = Color(0xFFC9C1B2),
    onButtonFilled = Color(0xFF15130F),
    buttonBasic = Color(0x0DF4EFE6),
    buttonBasicPressed = Color(0x1AF4EFE6),
    pixelOn = Color(0xFF6A9AF2),
    pixelOff = Color(0xFF4A453D),
    pixelStop = Color(0xFF3FA37E),
    softPrimary = Color(0xFF5A7FC2),
    ochre = Color(0xFFC9996A),
    green = Color(0xFF3FA37E),
    pink = Color(0xFFE88AA3),
    stamp = Color(0xFFD6523C),
    onStamp = Color(0xFF15130F),
    pot = Color(0xFFA8693F),
    potDark = Color(0xFF5E3B25),
    roof = Color(0xFF8A8378),
)
