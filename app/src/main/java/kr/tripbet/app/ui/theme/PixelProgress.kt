package kr.tripbet.app.ui.theme

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.ceil
import kotlin.math.roundToInt

/*
 * 픽셀 진행 표시 — 아이오닉 Parametric Pixel에서 빌린 "네모 점 줄".
 * 규칙(디자인/디자인_규칙.md 8절):
 *  1. 움직이지 않는다. 값이 바뀔 때만 다시 그린다(애니메이션·깜빡임 없음 — 주행 중 규정).
 *  2. 픽셀만으로 정보를 전하지 않는다. 같은 정보를 글(남은 km·분)로 옆에 함께 쓴다.
 *  3. 켜진 칸=primary, 꺼진 칸=pixelOff, 경유지=pixelStop(키 큰 칸).
 *  4. 칸은 정사각형에 가깝게, 모서리 2dp 안팎(둥근 알약 모양 금지).
 */

/**
 * 여정 줄: 출발 → 도착. [progress] 0..1 만큼 켜지고, [stops] 위치(0..1)에 경유지 표식이 선다.
 * 경유지는 최대 2곳이므로 stops.size <= 2.
 */
@Composable
fun PixelJourney(
    progress: Float,
    stops: List<Float>,
    modifier: Modifier = Modifier,
    cells: Int = 40,
    cellHeight: Dp = 24.dp,
    stopExtra: Dp = 16.dp,
    description: String = "",
) {
    val c = TripTheme.colors
    val lit = (progress.coerceIn(0f, 1f) * cells).roundToInt()
    val stopIdx = stops.map { (it.coerceIn(0f, 1f) * (cells - 1)).roundToInt() }.toSet()
    Canvas(
        modifier
            .fillMaxWidth()
            .height(cellHeight + stopExtra * 2)
            .semantics { contentDescription = description }
    ) {
        val gap = 4.dp.toPx()
        val w = (size.width - gap * (cells - 1)) / cells
        val h = cellHeight.toPx()
        val extra = stopExtra.toPx()
        for (i in 0 until cells) {
            val x = i * (w + gap)
            val isStop = i in stopIdx
            val color = when {
                isStop -> c.pixelStop
                i < lit -> c.pixelOn
                else -> c.pixelOff
            }
            val top = if (isStop) 0f else extra
            val height = if (isStop) h + extra * 2 else h
            pixel(color, x, top, w, height)
        }
    }
}

/**
 * 다음 휴게소까지 남은 거리: 가까워질수록 켜진 칸이 줄어든다(=남은 양).
 * [remainFraction] 1 = 방금 출발, 0 = 도착. 칸 수가 적어(기본 12) 한눈에 읽힌다. 남은 km·분은 반드시 글로 곁들일 것.
 */
@Composable
fun PixelCountdown(
    remainFraction: Float,
    modifier: Modifier = Modifier,
    cells: Int = 12,
    cellHeight: Dp = 32.dp,
    description: String = "",
) {
    val c = TripTheme.colors
    val lit = ceil(remainFraction.coerceIn(0f, 1f) * cells).toInt().coerceAtMost(cells)
    Canvas(
        modifier
            .fillMaxWidth()
            .height(cellHeight)
            .semantics { contentDescription = description }
    ) {
        val gap = 6.dp.toPx()
        val w = (size.width - gap * (cells - 1)) / cells
        for (i in 0 until cells) {
            // 오른쪽(도착 쪽)부터 꺼지지 않고, 왼쪽(지나온 쪽)부터 꺼진다
            val on = i >= cells - lit
            pixel(if (on) c.pixelOn else c.pixelOff, i * (w + gap), 0f, w, size.height)
        }
    }
}

private fun DrawScope.pixel(color: Color, x: Float, y: Float, w: Float, h: Float) {
    drawRoundRect(color, Offset(x, y), Size(w, h), CornerRadius(2.dp.toPx()))
}


/**
 * D안 조각보 진행 표시: 지나온 칸 [passed] 는 쪽빛·황토·단청 초록·연분홍 조각보(칸 폭 1.0·1.5·0.8·1.25 비율로 조금씩 다르게),
 * 남은 칸 [rest] 는 격자 창살. 주의: 옛 PixelCountdown 은 「켜진 칸 = 남은 양」이었고 여기는 「지나온 구간 = 조각보」로 방향이 반대.
 * [cellWidth] 를 주면 칸 폭을 고정(결과 화면 막대용: 이긴 쪽 passed 2칸, 진 쪽 rest 6칸), 없으면 전체 폭에 맞춘다.
 */
@Composable
fun PatchworkProgress(
    passed: Int,
    rest: Int,
    modifier: Modifier = Modifier,
    height: Dp = 36.dp,
    cellWidth: Dp? = null,
    description: String = "",
) {
    val c = TripTheme.colors
    val palette = listOf(c.primary, c.ochre, c.green, c.pink)
    val weights = listOf(1.0f, 1.5f, 0.8f, 1.25f)
    val total = passed + rest
    Canvas(
        (if (cellWidth == null) modifier.fillMaxWidth() else modifier.width(cellWidth * total + 4.dp * (total - 1)))
            .height(height)
            .semantics { contentDescription = description }
    ) {
        val gap = if (cellWidth != null) 4.dp.toPx() else 3.dp.toPx()
        val w = (0 until total).map { i -> if (i < passed) weights[i % 4] else 1f }
        val unit = if (cellWidth != null) cellWidth.toPx() else (size.width - gap * (total - 1)) / w.sum()
        var x = 0f
        for (i in 0 until total) {
            val cw = if (cellWidth != null) cellWidth.toPx() else unit * w[i]
            if (i < passed) {
                drawRect(palette[i % 4], Offset(x, 0f), Size(cw, size.height))
            } else {
                drawRect(c.pixelOff, Offset(x, 0f), Size(cw, size.height))
                val lw = 2.dp.toPx()
                drawRect(c.background, Offset(x + cw / 3f - lw / 2, 0f), Size(lw, size.height))
                drawRect(c.background, Offset(x + 2 * cw / 3f - lw / 2, 0f), Size(lw, size.height))
                drawRect(c.background, Offset(x, size.height / 2f - lw / 2), Size(cw, lw))
            }
            x += cw + gap
        }
    }
}
