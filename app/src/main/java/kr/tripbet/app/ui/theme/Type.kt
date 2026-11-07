package kr.tripbet.app.ui.theme

import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineBreak
import androidx.compose.ui.unit.sp
import kr.tripbet.app.R

/**
 * 글꼴 Asta Sans (42dot, SIL OFL 1.1 — 앱에 넣어도 됨. 라이선스 원문: 디자인/글꼴_라이선스/AstaSans_OFL.txt).
 * res/font/asta_sans.ttf 는 가변 글꼴(굵기 300~800)이라 굵기별로 같은 파일을 가변 축 값만 바꿔 연다.
 * minSdk 28 이라 가변 축(API 26+)을 쓸 수 있다.
 */
@OptIn(ExperimentalTextApi::class)
private fun asta(weight: Int) = Font(
    resId = R.font.asta_sans,
    weight = FontWeight(weight),
    variationSettings = FontVariation.Settings(FontVariation.weight(weight)),
)

val AstaSans = FontFamily(asta(400), asta(500), asta(700), asta(800))

/**
 * 글자 크기 단계. Pleos 서체 문서 표(Connect-S 기준)를 그대로 옮겼다.
 * 차량 화면은 휴대폰이 아니다: 가장 작은 글자가 20, 본문은 26~30.
 * 규칙: 읽어야 하는 글(주 글자)은 Body 이상(26+)이고, Google 기준 "주 글자 32dp 이상"을 지켜야 하는 곳은 Title.Small(32) 이상을 쓴다.
 * Label 20·24는 보조 정보 전용(단위, 출처 표기)이며 최소 18px 아래로는 절대 내려가지 않는다.
 */
object TripType {
    private fun s(size: Int, line: Int, weight: Int) = TextStyle(
        fontFamily = AstaSans,
        fontSize = size.sp,
        lineHeight = line.sp,
        fontWeight = FontWeight(weight),
        // 한국어 줄 바꿈이 낱말 가운데서 끊기지 않게
        lineBreak = LineBreak.Paragraph,
    )

    val HeadlineLarge = s(60, 80, 800)
    val HeadlineMedium = s(56, 72, 800)
    val HeadlineSmall = s(48, 64, 700)

    val TitleLarge = s(40, 52, 800)
    val TitleMedium = s(36, 44, 700)
    val TitleSmall = s(32, 40, 700)

    val BodyLarge = s(30, 38, 500)      // 'Body Large Strong' 자리는 700 으로 .copy(fontWeight = Bold)
    val BodyMedium = s(28, 36, 400)
    val BodySmall = s(26, 34, 400)

    /** 큰 숫자·결과 제목 전용(Pleos 표 밖). 읽는 글에는 쓰지 않는다. */
    val DisplayResult = s(96, 108, 800)     // 예: 준호 님이 졌어요
    val DisplayDriving = s(168, 176, 800)   // 주행 중 남은 시간 숫자(v4 시안 기준 168, 658×552 창)

    val LabelMedium = s(24, 28, 500)
    val LabelSmall = s(20, 24, 500)     // 앱 안 최소 글자. 이보다 작게 쓰지 않는다.
}
