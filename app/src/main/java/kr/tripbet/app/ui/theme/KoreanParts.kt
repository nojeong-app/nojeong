package kr.tripbet.app.ui.theme

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kr.tripbet.app.R

/*
 * 한국 스타일 D안 부품 (디자인/시안/v5_한국, D안_적용사항.md).
 * 이름표·도장 글자는 고운바탕 굵게(SIL OFL 1.1)에서 「노정한턱나눔꼴찌·숫자」만 남긴 부분 글꼴 nojeong_serif.ttf (글꼴 이름 Nojeong Serif).
 * 라이선스 원문은 assets/licenses/GowunBatang_OFL.txt. 도장·뚝배기·기와·뇌문은 일반 모양을 직접 그린 것이다.
 */
val NojeongSerif = FontFamily(Font(R.font.nojeong_serif, FontWeight.Bold))

/** 모든 화면 왼쪽 위의 작은 이름표(22sp, 보조 글자색). */
@Composable
fun NojeongTag(modifier: Modifier = Modifier) {
    Text(
        "노정", modifier = modifier,
        style = TextStyle(fontFamily = NojeongSerif, fontWeight = FontWeight.Bold, fontSize = 22.sp, lineHeight = 28.sp),
        color = TripTheme.colors.textSecondary,
    )
}

/** 도장(낙관): 64dp 사각, 모서리 9dp, 단청 적 면, 안쪽 이중 테두리, -5도, 잉크 얼룩 점 3개. */
@Composable
fun SealStamp(text: String = "한\n턱", modifier: Modifier = Modifier, size: Dp = 64.dp) {
    val c = TripTheme.colors
    Box(modifier.size(size).rotate(-5f), contentAlignment = Alignment.Center) {
        Canvas(Modifier.size(size)) {
            drawRoundRect(c.stamp, size = this.size, cornerRadius = CornerRadius(9.dp.toPx()))
            drawRoundRect(
                c.onStamp.copy(alpha = 0.55f), topLeft = Offset(3.dp.toPx(), 3.dp.toPx()),
                size = Size(this.size.width - 6.dp.toPx(), this.size.height - 6.dp.toPx()),
                cornerRadius = CornerRadius(7.dp.toPx()), style = Stroke(2.dp.toPx()),
            )
            val dot = Color.Black.copy(alpha = 0.16f)
            drawCircle(dot, 1.6.dp.toPx(), Offset(this.size.width * 0.18f, this.size.height * 0.28f))
            drawCircle(dot, 1.4.dp.toPx(), Offset(this.size.width * 0.74f, this.size.height * 0.70f))
            drawCircle(dot, 1.2.dp.toPx(), Offset(this.size.width * 0.60f, this.size.height * 0.18f))
        }
        Text(
            text,
            style = TextStyle(fontFamily = NojeongSerif, fontWeight = FontWeight.Bold, fontSize = 25.sp, lineHeight = 26.sp, textAlign = TextAlign.Center),
            color = c.onStamp,
        )
    }
}

/** 픽셀 그림 그리기: 글자 하나 = 한 칸. colors 가 null 을 돌려주면 빈 칸. */
private fun DrawScope.pixelMap(map: List<String>, cell: Float, gap: Float, colors: (Char) -> Color?) {
    map.forEachIndexed { y, row ->
        row.forEachIndexed { x, ch ->
            colors(ch)?.let { drawRoundRect(it, Offset(x * (cell + gap), y * (cell + gap)), Size(cell, cell), CornerRadius(1f)) }
        }
    }
}

private val POT_MAP = listOf("....1...1.......", "...1...1........", "..sssssssssss...", ".bbbbbbbbbbbbbb.", "hbbbbbbbbbbbbbbh", ".bbbbbbbbbbbbbb.", "..bbbbbbbbbbbb..", "...dddddddddd...")
private val ROOF_MAP = listOf("r......rrrrrr......r", ".r...rrrrrrrrrr...r.", "..rrrrrrrrrrrrrrrr..", "...rrrrrrrrrrrrrr...", ".....pp......pp.....")

/** 벌칙 음식 그림: 뚝배기 16×8 칸(칸 9dp, 간격 2dp). */
@Composable
fun PotArt(modifier: Modifier = Modifier) {
    val c = TripTheme.colors
    Canvas(modifier.width((16 * 11).dp).height((8 * 11).dp).semantics { contentDescription = "뚝배기 그림" }) {
        pixelMap(POT_MAP, 9.dp.toPx(), 2.dp.toPx()) { ch ->
            when (ch) { '1' -> c.surfaceHigh; 's' -> c.point; 'b', 'h' -> c.pot; 'd' -> c.potDark; else -> null }
        }
    }
}

/** 휴게소 카드의 기와지붕: 20×5 칸(칸 4dp, 간격 1dp). 경유지 화면 휴게소 카드에만 쓴다. */
@Composable
fun RoofArt(modifier: Modifier = Modifier) {
    val c = TripTheme.colors
    Canvas(modifier.width(100.dp).height(25.dp)) {
        pixelMap(ROOF_MAP, 4.dp.toPx(), 1.dp.toPx()) { ch -> when (ch) { 'r' -> c.roof; 'p' -> c.stamp; else -> null } }
    }
}

/** 상품권 카드 윗줄 뇌문(네모 소용돌이) 한 줄: 높이 12dp, 황토색, 타일 16×12dp 가로 반복. */
@Composable
fun MeanderLine(modifier: Modifier = Modifier) {
    val color = TripTheme.colors.ochre
    Canvas(modifier.fillMaxWidth().height(12.dp)) {
        val tw = 16.dp.toPx(); val th = 12.dp.toPx(); val sw = 1.6.dp.toPx()
        var x = 0f
        while (x < size.width) {
            val p = Path().apply {
                moveTo(x + tw / 16f, th * 11f / 12f); lineTo(x + tw / 16f, th / 12f); lineTo(x + tw * 15f / 16f, th / 12f)
                lineTo(x + tw * 15f / 16f, th * 8f / 12f); lineTo(x + tw * 6f / 16f, th * 8f / 12f)
                lineTo(x + tw * 6f / 16f, th * 5f / 12f); lineTo(x + tw * 11f / 16f, th * 5f / 12f)
            }
            drawPath(p, color, style = Stroke(sw))
            x += tw
        }
    }
}
