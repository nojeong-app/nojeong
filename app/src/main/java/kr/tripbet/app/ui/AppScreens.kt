package kr.tripbet.app.ui

import androidx.annotation.StringRes
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.Hyphens
import androidx.compose.ui.text.style.LineBreak
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kr.tripbet.app.DrivingGate
import kr.tripbet.app.DrivingState
import kr.tripbet.app.ArrivalSink
import kr.tripbet.app.QuestionBank
import kr.tripbet.app.DemoNaviProvider
import kr.tripbet.app.Josa
import kr.tripbet.app.QuizDriver
import kr.tripbet.app.QuizSession
import kr.tripbet.app.R
import kr.tripbet.app.RideMeter
import kr.tripbet.app.data.DemoData
import kr.tripbet.app.data.RestFood
import kr.tripbet.app.data.Voucher
import kr.tripbet.app.data.loadQuiz
import kr.tripbet.app.ui.theme.*
import kotlin.math.abs

// ───────────────────────── 상태 ─────────────────────────

enum class Step { DEST, WAY, BET, GUESS, READY }
enum class Mode { PARKED, DRIVING }

/** 같은 휴게소에서는 음식 질문(이름·가격·재료) 중 하나만 낸다. */
val FOOD_KINDS = setOf(Kind.FOOD_NAME, Kind.FOOD_PRICE, Kind.FOOD_MATERIAL)

/** 내기 종류. 모두 예측형: 멈췄을 때 걸고, 달린 뒤 실제 값으로 확인한다. DRIVING 은 달리는 동안 켜 두는 기능. */
enum class Kind(@StringRes val title: Int, @StringRes val sub: Int, val mode: Mode, val step: Int, val start: Int) {
    FOOD_PRICE(R.string.bet_food_price, R.string.bet_food_price_sub, Mode.PARKED, 1000, 11000),
    FOOD_MATERIAL(R.string.bet_food_material, R.string.bet_food_material_sub, Mode.PARKED, 1, 0),
    FOOD_NAME(R.string.bet_food_name, R.string.bet_food_name_sub, Mode.PARKED, 1, 0),
    GAS_PRICE(R.string.bet_gas, R.string.bet_gas_sub, Mode.PARKED, 20, 1840),
    FACILITY(R.string.bet_facility, R.string.bet_facility_sub, Mode.PARKED, 1, 0),
    FESTIVAL(R.string.bet_festival, R.string.bet_festival_sub, Mode.PARKED, 1, 0),
    ARRIVAL(R.string.bet_arrival, R.string.bet_arrival_sub, Mode.PARKED, 1, 0),        // 값: +빨리, −늦게(분)
    FUEL(R.string.bet_fuel, R.string.bet_fuel_sub, Mode.PARKED, 1, 12),
    ALERTS(R.string.bet_drive_alerts, R.string.bet_drive_alerts_sub, Mode.DRIVING, 0, 0),
    QUIZ(R.string.bet_drive_quiz, R.string.bet_drive_quiz_sub, Mode.DRIVING, 0, 0),
}

val PLAYERS = listOf("민지", "준호")
class Dest(@StringRes val name: Int, @StringRes val tag: Int, val km: Int, val min: Int)

val DESTS = listOf(
    Dest(R.string.dest_name_gangneung, R.string.dest_tag_gangneung, 210, 160), Dest(R.string.dest_name_sokcho, R.string.dest_tag_sokcho, 210, 150),
    Dest(R.string.dest_name_jeonju, R.string.dest_tag_jeonju, 250, 180), Dest(R.string.dest_name_yeosu, R.string.dest_tag_yeosu, 400, 270),
)

/**
 * DEMO — 시연용 실제 값(차 데이터가 없을 때만 쓴다). SDK 값이 들어오면 RideMeter 가 우선한다.
 * 영상·기획서 설명: "가상 차량 시연, 실제 차량에서는 내비 도착 신호로 판정".
 */
object DemoActual {
    const val ARRIVAL_MIN = -6      // 내비보다 6분 늦게
    const val FUEL = 13
    const val MATERIAL_INDEX = 1
}

class Game {
    var step by mutableStateOf(Step.DEST)
    var dest by mutableStateOf(DESTS[0])
    val picked = mutableStateListOf<Int>()                 // 경유지(휴게소 순서), 최대 2
    var kind by mutableStateOf(Kind.FOOD_PRICE)            // 지금 거는 내기
    var turn by mutableStateOf(0)                          // 답을 정하는 사람(0 민지, 1 준호)
    val guesses = mutableStateMapOf<String, Int>()         // "종류:이름" → 답
    val late = mutableStateMapOf<String, Boolean>()        // 도착 내기: 「늦게 도착」을 골랐는지(분이 0이어도 구분)
    val betsDone = mutableStateListOf<Kind>()              // 두 사람 답까지 정해 둔 내기
    var resultIdx by mutableStateOf(0)
    var tab by mutableStateOf(Mode.PARKED)
    var group by mutableStateOf<Int?>(null)                // 걸기 묶음(0 휴게소, 1 운행). null 이면 묶음 목록
    val drivingOn = mutableStateListOf<Kind>()             // 출발 전에 켜 둔 기능
    var started by mutableStateOf(false)
    var showResult by mutableStateOf(false)
    var quiz by mutableStateOf<QuizSession?>(null)
    val wallet = mutableStateListOf<Voucher>()
    var showWallet by mutableStateOf(false)
    var caption by mutableStateOf<String?>(null)          // 음성 대사 자막
    fun has(k: Kind, who: String) = guesses.containsKey("${k.name}:$who")
    fun guess(k: Kind, who: String) = guesses["${k.name}:$who"] ?: k.start
    fun setGuess(k: Kind, who: String, v: Int) { guesses["${k.name}:$who"] = v }
}

val LocalCompact = compositionLocalOf { false }
val LocalBank = compositionLocalOf<QuestionBank?> { null }

/** 한국어 줄바꿈을 낱말(어절) 단위로: 낱말 가운데서 끊지 않는다. */
fun TextStyle.ko() = copy(
    lineBreak = LineBreak(LineBreak.Strategy.HighQuality, LineBreak.Strictness.Strict, LineBreak.WordBreak.Phrase),
    hyphens = Hyphens.None,
)

/** 목록 항목 제목: 본문 크기(28) 굵게. */
val ItemTitle get() = TripType.BodyMedium.copy(fontWeight = FontWeight.Bold)

fun hm(min: Int, h: String, m: String) = if (min >= 60) "${min / 60}$h" + (if (min % 60 > 0) " ${min % 60}$m" else "") else "${min}$m"

// ───────────────────────── 루트 ─────────────────────────

@Composable
fun TripBetApp(locationGranted: Boolean = true) {
    val c = TripTheme.colors
    val ctx = LocalContext.current
    val data = remember { DemoData(ctx) }
    val bank = remember { QuestionBank(ctx) }
    val g = remember { Game() }
    val driving = DrivingGate.state == DrivingState.DRIVING
    val quizData = remember { loadQuiz(ctx) }
    val driverRef = remember { arrayOfNulls<QuizDriver>(1) }
    // DEMO 도착 신호: 위치(GPX 재생)가 고른 휴게소 700m 안에 들어오면 「도착」을 낸다. 실제 차량은 내비 SDK 가 ArrivalSink 로 알린다.
    val demoNavi = remember { DemoNaviProvider { driverRef[0]?.speak(ctx.getString(R.string.tts_arrived), listOf("arrived")) } }
    val driver = remember {
        QuizDriver(ctx, quizData, { g.caption = it }, { loc -> if (DrivingGate.state == DrivingState.DRIVING && g.started) demoNavi.onLocation(loc, data.restStops.filter { it.order in g.picked }) }) { g.quiz = it }
            .also { driverRef[0] = it }
    }
    SideEffect { kr.tripbet.app.ArrivalBus.sink = ArrivalSink { driverRef[0]?.speak(ctx.getString(R.string.tts_arrived), listOf("arrived")) } }
    DisposableEffect(locationGranted) { driver.start(); onDispose { driver.stop() } }
    driver.driving = driving
    driver.enabled = Kind.QUIZ in g.drivingOn && g.started
    // 멈추면: 달리는 중 퀴즈는 마무리하고 확인 단계로, 걸어 둔 내기는 결과를 보여 준다
    LaunchedEffect(driving) {
        if (!driving) {
            g.quiz?.let { q -> q.finished = true; if (q.picks.none { it != null }) q.confirmed = true }
            if (g.started && g.betsDone.isNotEmpty()) { g.resultIdx = 0; g.showResult = true }
        }
    }
    // 자막은 8초 뒤 사라진다
    LaunchedEffect(g.caption) { if (g.caption != null) { kotlinx.coroutines.delay(8000); g.caption = null } }
    // 내기 중간 알림. DEMO: 달리기 시작 60초 뒤 한 번. 실제 차량에서는 다음 휴게소 도착 1~2분 전 내비 신호로 알린다.
    LaunchedEffect(driving, g.started, Kind.ALERTS in g.drivingOn) {
        if (driving && g.started && Kind.ALERTS in g.drivingOn) {
            kotlinx.coroutines.delay(60_000)
            driver.speak(ctx.getString(R.string.tts_alert, 3), listOf("alert3"))
        }
    }
    // DEMO 표기: 시연용 값을 쓰는 화면(달리는 중 18분, 도착·연비·재료 결과)에서만
    val betK = g.betsDone.getOrNull(g.resultIdx)
    val demoShown = (driving && g.quiz == null) ||
        (!driving && g.showResult && g.quiz == null && !g.showWallet &&
            ((betK == Kind.ARRIVAL && RideMeter.arrivalVsNaviMin == null) || (betK == Kind.FUEL && RideMeter.fuelEconomy == null) || betK == Kind.FOOD_MATERIAL))
    BoxWithConstraints(Modifier.fillMaxSize().background(c.background)) {
        CompositionLocalProvider(LocalCompact provides (maxWidth < 900.dp), LocalBank provides bank) {
            val q = g.quiz
            Column(Modifier.fillMaxSize()) {
            g.caption?.let { cap ->
                Row(Modifier.fillMaxWidth().padding(top = 8.dp, start = 16.dp, end = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.weight(1f).clip(RoundedCornerShape(20.dp)).background(c.surfaceAccent).padding(horizontal = 18.dp, vertical = 6.dp)) {
                        Text(cap, style = TripType.LabelMedium.ko(), color = c.text, maxLines = 2)
                    }
                    if (demoShown) { Spacer(Modifier.width(8.dp)); DemoTag() }   // 자막이 있으면 꼬리표는 자막 줄 오른쪽 끝에 붙여 겹치지 않게
                }
            }
            Box(Modifier.weight(1f).fillMaxWidth()) {
            NojeongTag(Modifier.align(Alignment.TopStart).padding(start = pad(), top = 4.dp))
            when {
                driving -> DrivingScreen(g, data)
                g.showWallet -> WalletScreen(g)
                q != null && !q.confirmed -> if (q.fixing) FixScreen(q) else ConfirmScreen(q)
                q != null -> QuizResultScreen(g, q, quizData.credit) { driver.clear() }
                g.showResult -> ResultScreen(g, data)
                else -> when (g.step) {
                    Step.DEST -> DestScreen(g)
                    Step.WAY -> WayScreen(g, data)
                    Step.BET -> BetPickScreen(g, data)
                    Step.GUESS -> GuessScreen(g, data)
                    Step.READY -> ReadyScreen(g)
                }
            }
            }
            }
            if (demoShown && g.caption == null) Box(Modifier.align(Alignment.TopEnd).padding(top = 6.dp, end = 10.dp)) { DemoTag() }
        }
    }
}

// ───────────────────────── 공통 부품 ─────────────────────────

@Composable
fun pad(): Dp = if (LocalCompact.current) 28.dp else TripDims.ScreenPadding

/** 화면 틀: 위 머리글, 가운데 내용(스크롤), 아래 힌트와 전폭 알약 버튼. */
@Composable
fun Frame(
    title: String?, onBack: (() -> Unit)? = null, hint: String? = null, hintMic: Boolean = false,
    button: String? = null, buttonPrimary: Boolean = true, onButton: () -> Unit = {},
    button2: String? = null, onButton2: () -> Unit = {}, note: String? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val c = TripTheme.colors
    Column(Modifier.fillMaxSize().padding(horizontal = pad()).padding(top = 34.dp, bottom = 14.dp)) {
        if (title != null) {
            Row(Modifier.fillMaxWidth().padding(bottom = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                if (onBack != null) { BackButton(onBack); Spacer(Modifier.width(20.dp)) }
                Text(title, style = TripType.TitleMedium.ko(), color = c.text, maxLines = 1)
            }
        }
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()), content = content)
        if (hint != null) {
            Row(Modifier.fillMaxWidth().padding(top = 4.dp, bottom = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                if (hintMic) { MicIcon(); Spacer(Modifier.width(10.dp)) }
                Text(hint, style = TripType.LabelMedium.ko(), color = c.textSecondary)
            }
        }
        if (note != null) Text(note, style = TripType.LabelSmall.ko(), color = TripTheme.colors.textSecondary, modifier = Modifier.padding(bottom = 6.dp))
        if (button != null) Pill(button, buttonPrimary, onButton)
        if (button2 != null) { Spacer(Modifier.height(8.dp)); Pill(button2, false, onButton2) }
    }
}

@Composable
fun Pill(label: String, primary: Boolean = true, onClick: () -> Unit) {
    val c = TripTheme.colors
    Box(
        Modifier.fillMaxWidth().height(TripDims.Touch).clip(RoundedCornerShape(TripDims.ButtonRadius))
            .background(if (primary) c.primary else c.surfaceHigh).clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) { Text(label, style = TripType.BodyLarge.copy(fontWeight = FontWeight.Bold).ko(), color = if (primary) c.onPrimary else c.text) }
}

@Composable
fun BackButton(onClick: () -> Unit) {
    val c = TripTheme.colors
    Box(Modifier.size(TripDims.Touch).clip(CircleShape).background(c.surfaceHigh).clickable(onClick = onClick), contentAlignment = Alignment.Center) {
        Canvas(Modifier.size(28.dp)) {
            val p = Path().apply { moveTo(size.width * 0.65f, size.height * 0.1f); lineTo(size.width * 0.3f, size.height * 0.5f); lineTo(size.width * 0.65f, size.height * 0.9f) }
            drawPath(p, c.text, style = Stroke(4.dp.toPx(), cap = StrokeCap.Round))
        }
    }
}

@Composable
fun MicIcon(size: Dp = 28.dp) {
    val c = TripTheme.colors
    Canvas(Modifier.size(size)) {
        val w = this.size.width; val h = this.size.height
        drawRoundRect(c.textSecondary, Offset(w * 0.36f, 0f), Size(w * 0.28f, h * 0.55f), CornerRadius(w * 0.14f))
        drawArc(c.textSecondary, 0f, 180f, false, Offset(w * 0.22f, h * 0.2f), Size(w * 0.56f, h * 0.55f), style = Stroke(w * 0.07f))
        drawLine(c.textSecondary, Offset(w * 0.5f, h * 0.75f), Offset(w * 0.5f, h * 0.95f), w * 0.07f)
    }
}

/** 선택됨(파란 원+체크) / 아직(테두리만 있는 원). */
@Composable
fun Mark(selected: Boolean, dim: Dp = 44.dp) {
    val c = TripTheme.colors
    Canvas(Modifier.size(dim)) {
        val w = size.width; val h = size.height
        if (selected) {
            drawCircle(c.primary)
            val p = Path().apply { moveTo(w * 0.27f, h * 0.52f); lineTo(w * 0.44f, h * 0.68f); lineTo(w * 0.74f, h * 0.34f) }
            drawPath(p, c.onPrimary, style = Stroke(4.dp.toPx(), cap = StrokeCap.Round))
        } else drawCircle(c.outline, radius = size.minDimension / 2 - 1.5.dp.toPx(), style = Stroke(3.dp.toPx()))
    }
}

@Composable
fun Chevron() {
    val c = TripTheme.colors
    Canvas(Modifier.size(24.dp)) {
        val p = Path().apply { moveTo(size.width * 0.3f, size.height * 0.1f); lineTo(size.width * 0.72f, size.height * 0.5f); lineTo(size.width * 0.3f, size.height * 0.9f) }
        drawPath(p, c.textSecondary, style = Stroke(3.dp.toPx(), cap = StrokeCap.Round))
    }
}

@Composable
fun Segmented(labels: List<String>, selected: Int, modifier: Modifier = Modifier, onSelect: (Int) -> Unit) {
    val c = TripTheme.colors
    Row(modifier.fillMaxWidth().height(TripDims.Touch).clip(RoundedCornerShape(TripDims.ButtonRadius)).background(c.surfaceHigh).padding(6.dp)) {
        labels.forEachIndexed { i, l ->
            Box(
                Modifier.weight(1f).fillMaxHeight().clip(RoundedCornerShape(TripDims.ButtonRadius))
                    .background(if (i == selected) c.surfaceAccent else Color.Transparent).clickable { onSelect(i) },
                contentAlignment = Alignment.Center
            ) { Text(l, style = TripType.BodySmall.copy(fontWeight = FontWeight.Bold).ko(), color = if (i == selected) c.text else c.textSecondary, maxLines = 1) }
        }
    }
}

@Composable
fun TripSwitch(on: Boolean, onToggle: () -> Unit) {
    val c = TripTheme.colors
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(stringResource(if (on) R.string.switch_on else R.string.switch_off), style = TripType.LabelMedium.ko(), color = c.text)
        Spacer(Modifier.width(16.dp))
        Box(
            Modifier.size(width = 100.dp, height = 58.dp).clip(RoundedCornerShape(29.dp))
                .background(if (on) c.primary else c.surfaceAccent).clickable(onClick = onToggle).padding(5.dp),
            contentAlignment = if (on) Alignment.CenterEnd else Alignment.CenterStart
        ) { Box(Modifier.size(48.dp).clip(CircleShape).background(Color.White)) }
    }
}

@Composable
fun SurfaceCard(modifier: Modifier = Modifier, dashed: Boolean = false, content: @Composable ColumnScope.() -> Unit) {
    val c = TripTheme.colors
    val shape = RoundedCornerShape(TripDims.CardRadius)
    var m = modifier.clip(shape).background(c.surface)
    if (dashed) m = m.drawBehind {
        drawRoundRect(c.outline, cornerRadius = CornerRadius(TripDims.CardRadius.toPx()), style = Stroke(3.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(14f, 10f))))
    }
    Column(m.padding(horizontal = 28.dp, vertical = 8.dp), content = content)
}

@Composable
fun Tag(text: String) {
    Box(Modifier.clip(RoundedCornerShape(20.dp)).background(TripTheme.colors.point).padding(horizontal = 16.dp, vertical = 4.dp)) {
        Text(text, style = TripType.LabelMedium.copy(fontWeight = FontWeight.Bold).ko(), color = Color(0xFF131417))
    }
}

/** 「가상 차량 시연」 꼬리표: 작게(20sp), 노란 바탕+어두운 글자. 화면 오른쪽 위 모서리에 얹는다. */
@Composable
fun DemoTag() {
    Box(Modifier.clip(RoundedCornerShape(14.dp)).background(TripTheme.colors.point).padding(horizontal = 12.dp, vertical = 2.dp)) {
        Text(stringResource(R.string.demo_label), style = TripType.LabelSmall.copy(fontWeight = FontWeight.Bold).ko(), color = Color(0xFF131417), maxLines = 1)
    }
}

/** 충전기 번개 모양(휴게소에 충전기가 있을 때). */
@Composable
fun BoltIcon() {
    val c = TripTheme.colors
    Canvas(Modifier.size(width = 14.dp, height = 20.dp)) {
        val p = Path().apply {
            moveTo(size.width * 0.62f, 0f); lineTo(size.width * 0.05f, size.height * 0.58f); lineTo(size.width * 0.45f, size.height * 0.58f)
            lineTo(size.width * 0.32f, size.height); lineTo(size.width * 0.95f, size.height * 0.38f); lineTo(size.width * 0.55f, size.height * 0.38f); close()
        }
        drawPath(p, c.active)
    }
}

@Composable
fun Divider() {
    Box(Modifier.fillMaxWidth().height(2.dp).background(TripTheme.colors.outlineMuted))
}

/** 사진 자리 모자이크(관광공사 사진은 개발용 빌드에서 불러올 때까지 비워 둠). */
@Composable
fun Mosaic(seed: Int, modifier: Modifier = Modifier) {
    val c = TripTheme.colors
    Canvas(modifier.clip(RoundedCornerShape(20.dp)).background(c.surfaceHigh)) {
        val cols = 8; val rows = 8; val gap = 4.dp.toPx()
        val w = (size.width - gap * (cols - 1)) / cols; val h = (size.height - gap * (rows - 1)) / rows
        var s = seed * 7919 + 13
        for (r in 0 until rows) for (k in 0 until cols) {
            s = (s * 1103515245 + 12345) and 0x7fffffff
            val col = when (s % 9) { 0, 1 -> c.pixelOn; 2 -> c.pixelStop; else -> null } ?: continue
            drawRoundRect(col, Offset(k * (w + gap), r * (h + gap)), Size(w, h), CornerRadius(2.dp.toPx()))
        }
    }
}

@Composable
fun won(v: Int) = "%,d".format(v) + stringResource(R.string.unit_won)

// ───────────────────────── 1 여행지 ─────────────────────────

@Composable
fun DestScreen(g: Game) {
    val c = TripTheme.colors
    val name = stringResource(g.dest.name)
    Frame(
        title = stringResource(R.string.dest_title), hint = stringResource(R.string.dest_hint, name), hintMic = true,
        button = stringResource(R.string.dest_button, name + Josa.euRo(name)), onButton = { g.picked.clear(); g.step = Step.WAY }
    ) {
        DESTS.forEach { d ->
            val sel = g.dest === d
            Row(
                Modifier.fillMaxWidth().padding(vertical = 2.dp).clip(RoundedCornerShape(TripDims.CardRadius))
                    .background(if (sel) c.surface else Color.Transparent).clickable { g.dest = d }.padding(horizontal = 20.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(Modifier.weight(1f)) {
                    Text(stringResource(d.name), style = TripType.TitleSmall.ko(), color = c.text)
                    Text(stringResource(d.tag), style = TripType.LabelSmall.ko(), color = c.textSecondary)
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(hm(d.min, "시간", "분"), style = TripType.BodyMedium.copy(fontWeight = FontWeight.Bold).ko(), color = c.text)
                    Text(stringResource(R.string.dest_km, d.km), style = TripType.LabelSmall.ko(), color = c.textSecondary)
                }
                if (sel) { Spacer(Modifier.width(16.dp)); Mark(true) }
            }
        }
    }
}

// ───────────────────────── 2 경유지 ─────────────────────────

enum class Tab(@StringRes val label: Int) { REST(R.string.way_tab_rest), FOOD(R.string.way_tab_food), EV(R.string.way_tab_ev), GAS(R.string.way_tab_gas) }

@Composable
fun WayScreen(g: Game, data: DemoData) {
    val c = TripTheme.colors
    var tab by remember { mutableStateOf(Tab.REST) }
    Frame(
        title = stringResource(R.string.way_title), onBack = { g.step = Step.DEST },
        hint = stringResource(R.string.way_hint, g.picked.size),
        button = stringResource(R.string.way_button), onButton = { g.step = Step.BET }
    ) {
        Segmented(Tab.values().map { stringResource(it.label) }, tab.ordinal) { tab = Tab.values()[it] }
        Spacer(Modifier.height(12.dp))
        SurfaceCard(Modifier.fillMaxWidth()) {
            when (tab) {
                Tab.REST -> data.restStops.filter { it.order in listOf(1, 4, 5) }.forEachIndexed { i, r ->
                    val sel = r.order in g.picked
                    if (i > 0) Divider()
                    Row(Modifier.fillMaxWidth().clickable { if (sel) g.picked.remove(r.order) else if (g.picked.size < 2) g.picked.add(r.order) }.padding(vertical = 2.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(r.name, style = ItemTitle.ko(), color = c.text)
                            }
                            val charge = data.chargeStops.any { it.name.contains(r.name.removeSuffix("휴게소")) }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(stringResource(R.string.way_rest_line, hm(r.minFromStart, "시간", "분")), style = TripType.LabelSmall.ko(), color = c.textSecondary, maxLines = 1)
                                if (charge) { Spacer(Modifier.width(8.dp)); BoltIcon() }
                            }
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            RoofArt()   // 카드 오른쪽 위에 두어 기와 모양으로 읽히게
                            Spacer(Modifier.height(3.dp))
                            Box(Modifier.clip(RoundedCornerShape(16.dp)).background(c.surfaceHigh).padding(horizontal = 12.dp, vertical = 2.dp)) {
                                Text(stringResource(R.string.way_tag_food), style = TripType.LabelSmall.ko(), color = c.textSecondary, maxLines = 1)
                            }
                        }
                        Spacer(Modifier.width(12.dp))
                        Mark(sel)
                    }
                }
                Tab.FOOD -> data.places.indices.filter { data.placeTypes[it] == 39 }.take(5).forEachIndexed { i, idx ->
                    val p = data.places[idx]
                    if (i > 0) Divider()
                    Column(Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
                        Text(p.title, style = ItemTitle.ko(), color = c.text, maxLines = 1)
                        Text(p.addr, style = TripType.LabelSmall.ko(), color = c.textSecondary, maxLines = 1)
                    }
                }
                Tab.EV -> data.chargeStops.forEachIndexed { i, s ->
                    if (i > 0) Divider()
                    Column(Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
                        Text(stringResource(R.string.way_ev_title, s.name), style = ItemTitle.ko(), color = c.text, maxLines = 1)
                        Text(stringResource(R.string.way_ev_line, s.chargers, s.availableNow), style = TripType.LabelSmall.ko(), color = c.textSecondary, maxLines = 1)
                    }
                }
                Tab.GAS -> Column(Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
                    Text(stringResource(R.string.way_gas_title), style = ItemTitle.ko(), color = c.text)
                    Text(stringResource(R.string.way_gas_body), style = TripType.LabelSmall.ko(), color = c.textSecondary)
                }
            }
        }
    }
}

// ───────────────────────── 3 내기 고르기 ─────────────────────────

@Composable
fun guessText(g: Game, data: DemoData, k: Kind, who: String): String = choicesFor(g, data, k).first.getOrElse(g.guess(k, who)) { "" }

@Composable
fun BetPickScreen(g: Game, data: DemoData) {
    val c = TripTheme.colors
    Frame(
        title = null, hint = null,
        button = stringResource(R.string.bet_start), onButton = { g.started = true; g.step = Step.READY }
    ) {
        Row(Modifier.fillMaxWidth().padding(bottom = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            BackButton { if (g.tab == Mode.PARKED && g.group != null) g.group = null else g.step = Step.WAY }
            Spacer(Modifier.width(12.dp))
            Segmented(listOf(stringResource(R.string.bet_tab_parked), stringResource(R.string.bet_tab_driving)), g.tab.ordinal) { g.tab = Mode.values()[it]; g.group = null }
        }
        SurfaceCard(Modifier.fillMaxWidth()) {
            // 멈췼을 때 탭: 먼저 묶음(휴게소·운행) 목록, 묶음을 열면 그 안의 내기 종류
            val pickedStop = data.restStops.firstOrNull { it.order in g.picked } ?: data.restStops.first()
            val destName = stringResource(g.dest.name)
            val gasOk = data.restInfo(pickedStop.name)?.gasoline != null            // 덕평은 양방향 공용 표기라 휘발유 질문 제외
            val festOk = data.festivalsThisMonth(destName).isNotEmpty()           // 목적지 이번 달 행사가 없으면 축제 질문 제외
            val groups = buildList {
                add(Triple(R.string.bet_group_rest, R.string.bet_group_rest_sub, listOf(Kind.FOOD_NAME, Kind.FOOD_PRICE, Kind.FOOD_MATERIAL) + (if (gasOk) listOf(Kind.GAS_PRICE) else emptyList()) + listOf(Kind.FACILITY)))
                add(Triple(R.string.bet_group_drive, R.string.bet_group_drive_sub, listOf(Kind.ARRIVAL, Kind.FUEL)))
                if (festOk) add(Triple(R.string.bet_group_region, R.string.bet_group_region_sub, listOf(Kind.FESTIVAL)))
            }
            val list = if (g.tab == Mode.PARKED) (g.group?.let { groups[it].third } ?: emptyList()) else Kind.values().filter { it.mode == g.tab }
            if (g.tab == Mode.PARKED && g.group == null) {
                groups.forEachIndexed { gi, (title, sub, kinds) ->
                    if (gi > 0) Divider()
                    Row(Modifier.fillMaxWidth().clickable { g.group = gi }.padding(vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(stringResource(title), style = ItemTitle.ko(), color = c.text, maxLines = 1)
                            val doneN = kinds.count { it in g.betsDone }
                            Text(stringResource(sub) + (if (doneN > 0) " · ${doneN}/${kinds.size}" else ""), style = TripType.LabelSmall.ko(), color = c.textSecondary, maxLines = 1)
                        }
                        Text(stringResource(R.string.bet_group_count, kinds.size), style = TripType.LabelMedium.ko(), color = c.textSecondary)
                        Spacer(Modifier.width(10.dp)); Chevron()
                    }
                }
            }
            list.forEachIndexed { i, k ->
                if (i > 0) Divider()
                if (g.tab == Mode.PARKED) {
                    val done = k in g.betsDone
                    val foodTaken = k in FOOD_KINDS && g.betsDone.any { it in FOOD_KINDS && it != k }   // 같은 휴게소 음식 질문은 하나만
                    val doneText = if (done) PLAYERS.map { w -> w + " " + guessText(g, data, k, w) }.let { it.joinToString("   ") } else ""
                    Row(Modifier.fillMaxWidth().clickable(enabled = !foodTaken) { g.kind = k; g.turn = 0; g.step = Step.GUESS }.padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(stringResource(k.title), style = ItemTitle.ko(), color = c.text, maxLines = 1)
                            Text(
                                if (done) doneText else if (foodTaken) stringResource(R.string.bet_food_once) else stringResource(k.sub),
                                style = TripType.LabelSmall.ko(), color = c.textSecondary, maxLines = 1
                            )
                        }
                        Chevron()
                    }
                } else {
                    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(stringResource(k.title), style = ItemTitle.ko(), color = c.text, maxLines = 1)
                            Text(stringResource(k.sub), style = TripType.LabelSmall.ko(), color = c.textSecondary, maxLines = 1)
                        }
                        TripSwitch(k in g.drivingOn) { if (k in g.drivingOn) g.drivingOn.remove(k) else g.drivingOn.add(k) }
                    }
                }
            }
        }
        if (g.tab == Mode.DRIVING) Row(Modifier.fillMaxWidth().padding(top = 10.dp), verticalAlignment = Alignment.Top) {
            MicIcon(); Spacer(Modifier.width(10.dp))
            Text(stringResource(R.string.bet_drive_hint), style = TripType.LabelMedium.ko(), color = c.textSecondary)
        }
    }
}

/** 재료 맞히기 보기: 정답은 가운데. 데이터에 없는 재료를 오답으로 쓴다. */
fun materialOptions(food: RestFood?): List<String> {
    val mats = food?.materials?.takeIf { it.isNotEmpty() } ?: listOf("한우", "콩나물", "무", "파")
    val correct = mats.getOrNull(1) ?: mats.first()
    val pool = listOf("두부", "시금치", "버섯", "당근", "양파").filter { it !in mats }
    return listOf(pool.getOrElse(0) { "두부" }, correct, pool.getOrElse(1) { "버섯" })
}

// ───────────────────────── 10 답 정하기 ─────────────────────────

/**
 * 예측 내기의 보기 5개와 정답 번호(0부터). 정답 자리는 휴게소·종류마다 정해진 값으로 섞어(화면 사이 같은 값) 매번 같은 자리가 되지 않게 한다.
 * 정답 값은 SDK 값이 있으면 그것, 없으면 시연용 값(DEMO).
 */
@Composable
fun choicesFor(g: Game, data: DemoData, k: Kind): Pair<List<String>, Int> {
    val stop = data.restStops.firstOrNull { it.order in g.picked } ?: data.restStops.first()
    val food = stop.foods.first()
    val matFood = stop.foods.firstOrNull { it.materials.isNotEmpty() } ?: food
    val rnd = java.util.Random((stop.order * 31 + k.ordinal * 7 + 5).toLong())
    return when (k) {
        Kind.FOOD_PRICE -> {
            val actual = food.price ?: 10000
            val pos = rnd.nextInt(5)                      // 정답 자리. 값은 작은 순서로 늘어놓는다
            List(5) { won(actual + (it - pos) * 1000) } to pos
        }
        Kind.ARRIVAL -> {
            val m = RideMeter.arrivalVsNaviMin ?: DemoActual.ARRIVAL_MIN   // +빨리 -늦게(분)
            val idx = when { m >= 5 -> 0; m >= 1 -> 1; m == 0 -> 2; m >= -4 -> 3; else -> 4 }
            listOf(R.string.arr_b0, R.string.arr_b1, R.string.arr_b2, R.string.arr_b3, R.string.arr_b4).map { stringResource(it) } to idx
        }
        Kind.FUEL -> {
            val f = RideMeter.fuelEconomy ?: DemoActual.FUEL.toFloat()
            val idx = when { f < 10 -> 0; f < 12 -> 1; f < 14 -> 2; f < 16 -> 3; else -> 4 }
            listOf(R.string.fuel_b0, R.string.fuel_b1, R.string.fuel_b2, R.string.fuel_b3, R.string.fuel_b4).map { stringResource(it) } to idx
        }
        Kind.FOOD_NAME -> {
            val correct = food.foodName
            val others = data.restStops.flatMap { s -> s.foods.map { it.foodName } }.distinct().filter { it != correct }.shuffled(rnd).take(4)
            val pos = rnd.nextInt(5)
            others.toMutableList().also { it.add(pos, correct) } to pos
        }
        Kind.GAS_PRICE -> {
            val actual = data.restInfo(stop.name)?.gasoline ?: 1840
            val pos = rnd.nextInt(5)
            List(5) { won(actual + (it - pos) * 20) } to pos        // 20원 간격, 작은 순서
        }
        Kind.FESTIVAL -> {
            val dest = stringResource(g.dest.name)
            val now = data.festivalsThisMonth(dest)
            val correct = now.firstOrNull()?.name ?: ""
            val others = data.festivals.filter { it.name != correct && it !in now }.map { it.name }.shuffled(rnd).take(4)
            val pos = rnd.nextInt(5)
            others.toMutableList().also { it.add(pos.coerceAtMost(it.size), correct) } to pos.coerceAtMost(others.size)
        }
        Kind.FACILITY -> {
            val info = data.restInfo(stop.name)
            val have = (info?.facilities ?: listOf("편의점")).filter { it != "기타" }
            val correct = have[rnd.nextInt(have.size)]
            val others = data.allFacilities.filter { it !in have && it != "기타" }.shuffled(rnd).take(4)
            val pos = rnd.nextInt(5)
            others.toMutableList().also { it.add(pos, correct) } to pos
        }
        else -> {
            val mats = matFood.materials.takeIf { it.isNotEmpty() } ?: listOf("한우", "콩나물", "무", "파")
            val correct = mats.getOrNull(1) ?: mats.first()
            val pool = listOf("두부", "시금치", "버섯", "당근", "양파", "애호박").filter { it !in mats }.take(4)
            val pos = rnd.nextInt(5)
            val list = pool.toMutableList().also { it.add(pos, correct) }
            list to pos
        }
    }
}

/** 질문 문장. 문장 틀은 questions.json(질문 은행)에서 고르고, 정답이 문장 안에 들어 있으면 그 틀은 건너뛴다. */
@Composable
fun questionFor(g: Game, data: DemoData, k: Kind): String {
    val stop = data.restStops.firstOrNull { it.order in g.picked } ?: data.restStops.first()
    val food = stop.foods.first()
    val matFood = stop.foods.firstOrNull { it.materials.isNotEmpty() } ?: food
    val (labels, correctIdx) = choicesFor(g, data, k)
    val bank = LocalBank.current
    val vars = mapOf("rest" to stop.name, "dest" to stringResource(g.dest.name), "food" to (if (k == Kind.FOOD_MATERIAL) matFood.foodName else food.foodName))
    return bank?.question(k.name, stop.order + g.betsDone.size, vars, labels.getOrElse(correctIdx) { "" })
        ?: stringResource(k.title)
}

@Composable
fun GuessScreen(g: Game, data: DemoData) {
    val c = TripTheme.colors
    val k = g.kind
    val who = PLAYERS[g.turn]
    val stop = data.restStops.firstOrNull { it.order in g.picked } ?: data.restStops.first()
    val food = stop.foods.first()
    val matFood = stop.foods.firstOrNull { it.materials.isNotEmpty() } ?: food
    val (labels, _) = choicesFor(g, data, k)
    val picked = if (g.has(k, who)) g.guess(k, who) else -1
    Frame(title = stringResource(R.string.guess_turn, who), onBack = { g.step = Step.BET }) {
        SurfaceCard(Modifier.fillMaxWidth()) {
            val label = when (k) {
                Kind.ARRIVAL -> stringResource(R.string.guess_arrival_route, stringResource(g.dest.name))
                Kind.FUEL -> stringResource(R.string.guess_fuel_label)
                Kind.FESTIVAL -> stringResource(g.dest.name)
                else -> stop.name
            }
            Text(label, style = TripType.LabelSmall.ko(), color = c.textSecondary)
            Text(questionFor(g, data, k), style = TripType.TitleSmall.ko(), color = c.text, maxLines = 3)
        }
        Spacer(Modifier.height(10.dp))
        // 보기 5개(번호 1~5)와 여섯째 칸 「답 정하기」: 2열, 칸 높이 76dp. 「3번」처럼 음성으로 고르는 것을 전제로 번호를 크게 보인다.
        val cells = labels.size + 1
        for (row in 0 until (cells + 1) / 2) {
            Row(Modifier.fillMaxWidth().padding(bottom = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                for (col in 0..1) {
                    val idx = row * 2 + col
                    Box(Modifier.weight(1f)) {
                        when {
                            idx < labels.size -> ChoiceCell(idx + 1, labels[idx], picked == idx) { g.setGuess(k, who, idx) }
                            idx == labels.size -> Pill(stringResource(R.string.guess_button), primary = picked >= 0) {
                                if (picked >= 0) { if (g.turn == 0) g.turn = 1 else { if (k !in g.betsDone) g.betsDone.add(k); g.step = Step.BET } }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ChoiceCell(num: Int, label: String, selected: Boolean, onClick: () -> Unit) {
    val c = TripTheme.colors
    val shape = RoundedCornerShape(TripDims.CardRadius)
    Row(
        Modifier.fillMaxWidth().height(TripDims.Touch).shadow(2.dp, shape).clip(shape).background(c.surface)
            .then(if (selected) Modifier.border(4.dp, c.primary, shape) else Modifier)
            .clickable(onClick = onClick).padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(Modifier.size(44.dp).clip(CircleShape).background(if (selected) c.primary else c.surfaceHigh), contentAlignment = Alignment.Center) {
            Text("$num", style = TripType.BodyMedium.copy(fontWeight = FontWeight.Bold).ko(), color = if (selected) c.onPrimary else c.textSecondary)
        }
        Spacer(Modifier.width(12.dp))
        Text(label, style = TripType.BodySmall.copy(fontWeight = FontWeight.Bold).ko(), color = c.text, maxLines = 2)
    }
}

@Composable
fun Stepper(label: String, onClick: () -> Unit) {
    val c = TripTheme.colors
    Box(
        Modifier.height(TripDims.Touch).widthIn(min = 120.dp).clip(RoundedCornerShape(TripDims.ButtonRadius)).background(c.surfaceHigh).clickable(onClick = onClick).padding(horizontal = 20.dp),
        contentAlignment = Alignment.Center
    ) { Text(label, style = TripType.BodyMedium.copy(fontWeight = FontWeight.Bold).ko(), color = c.text, maxLines = 1) }
}

// ───────────────────────── 출발 준비 ─────────────────────────

@Composable
fun ReadyScreen(g: Game) {
    val c = TripTheme.colors
    Frame(
        title = null, hint = stringResource(R.string.ready_hint), hintMic = true,
        note = stringResource(R.string.ai_voice_note),   // 모델 라이선스(OpenRAIL-M)에 따른 기계 합성 표시(앱 안 한 곳에 상시)
        button = stringResource(R.string.ready_again), buttonPrimary = false, onButton = { g.started = false; g.step = Step.BET }
    ) {
        Spacer(Modifier.height(24.dp))
        Text(stringResource(R.string.ready_title), style = TripType.HeadlineSmall.ko(), color = c.text)
        Spacer(Modifier.height(8.dp))
        Text(stringResource(R.string.ready_sub, stringResource(g.dest.name), g.picked.size), style = TripType.BodyMedium.ko(), color = c.textSecondary)
        Spacer(Modifier.height(20.dp))
        SurfaceCard(Modifier.fillMaxWidth()) {
            val lines = buildList {
                g.betsDone.forEach { add(stringResource(R.string.ready_parked, stringResource(it.title))) }
                g.drivingOn.forEach { add(stringResource(R.string.ready_driving, stringResource(it.title))) }
            }
            if (lines.isEmpty()) Text(stringResource(R.string.ready_none), style = TripType.BodySmall.ko(), color = c.textSecondary, modifier = Modifier.padding(vertical = 8.dp))
            lines.forEach { Text(it, style = TripType.BodySmall.ko(), color = c.text, modifier = Modifier.padding(vertical = 4.dp)) }
        }
    }
}

// ───────────────────────── 5·6 달리는 중 ─────────────────────────

/** 달리는 중: 가운데 정렬. 버튼·입력·목록·결과 없음, 움직임 없음. */
@Composable
fun DrivingScreen(g: Game, data: DemoData) {
    val c = TripTheme.colors
    val next = data.restStops.firstOrNull { it.order in g.picked } ?: data.restStops.first()
    val qz = g.quiz
    Column(Modifier.fillMaxSize().padding(horizontal = pad()), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
        if (qz != null) {
            Text(stringResource(R.string.quiz_drive_top, qz.place.nearRest, qz.place.restKm?.toString() ?: "?"), style = TripType.BodyMedium.ko(), color = c.textSecondary, textAlign = TextAlign.Center)
            Spacer(Modifier.height(8.dp))
            Text(qz.place.name, style = TripType.HeadlineMedium.ko(), color = c.text, textAlign = TextAlign.Center, maxLines = 2)
            Spacer(Modifier.height(6.dp))
            Text(stringResource(R.string.quiz_drive_reward, qz.place.voucher.title, Josa.eulReul(qz.place.voucher.title)), style = TripType.LabelMedium.ko(), color = c.textSecondary, textAlign = TextAlign.Center)
            Spacer(Modifier.height(20.dp))
            PatchworkProgress(passed = 3, rest = 9, description = qz.place.name)
            Spacer(Modifier.height(14.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                MicIcon(); Spacer(Modifier.width(10.dp))
                Text(stringResource(R.string.quiz_drive_line1), style = TripType.BodySmall.ko(), color = c.textSecondary)
            }
            Text(stringResource(R.string.quiz_drive_line2), style = TripType.LabelMedium.ko(), color = c.textSecondary, textAlign = TextAlign.Center)
            Text(stringResource(R.string.quiz_drive_line3), style = TripType.LabelMedium.ko(), color = c.textSecondary, textAlign = TextAlign.Center)
        } else {
            Text(stringResource(R.string.drive_next), style = TripType.BodyLarge.ko(), color = c.textSecondary)
            Text(next.name, style = TripType.HeadlineSmall.ko(), color = c.text)
            Row(verticalAlignment = Alignment.Bottom) {
                Text("18", style = TripType.DisplayDriving.copy(fontSize = 168.sp, lineHeight = 180.sp).ko(), color = c.text)
                Text(" " + stringResource(R.string.drive_min_unit), style = TripType.TitleMedium.ko(), color = c.textSecondary, modifier = Modifier.padding(bottom = 28.dp))
            }
            PatchworkProgress(passed = 4, rest = 8, description = next.name)
            Spacer(Modifier.height(16.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                MicIcon(); Spacer(Modifier.width(10.dp))
                Text(stringResource(if (g.started) R.string.drive_voice else R.string.drive_voice_idle), style = TripType.BodySmall.ko(), color = c.textSecondary)
            }

        }
    }
}

// ───────────────────────── 4·11 결과·벌칙·동점 ─────────────────────────

@Composable
fun ResultScreen(g: Game, data: DemoData) {
    val c = TripTheme.colors
    val k = g.betsDone.getOrNull(g.resultIdx) ?: Kind.FOOD_PRICE
    val stop = data.restStops.firstOrNull { it.order in g.picked } ?: data.restStops.first()
    val food = stop.foods.first()
    val matFood = stop.foods.firstOrNull { it.materials.isNotEmpty() } ?: food
    val (labels, correctIdx) = choicesFor(g, data, k)
    // 판정: 정답 보기=승, 아무도 못 맞히면 가장 가까운 보기, 같은 보기=동점. 실제 값은 choicesFor 안(SDK 값 또는 시연용 값)
    val diffs = PLAYERS.map { who -> abs(g.guess(k, who) - correctIdx) }
    val tie = diffs[0] == diffs[1]
    val loser = if (diffs[0] > diffs[1]) 0 else 1
    val actual = when (k) {
        Kind.FOOD_PRICE -> food.price ?: 10000
        Kind.ARRIVAL -> RideMeter.arrivalVsNaviMin ?: DemoActual.ARRIVAL_MIN
        Kind.FUEL -> RideMeter.fuelEconomy?.toInt() ?: DemoActual.FUEL
        else -> 0
    }
    val matOptions = labels
    val diffTexts = PLAYERS.map { who -> labels.getOrElse(g.guess(k, who)) { "" } }
    // 「꼴찌」 도장: 지는 사람 줄에 0.35초 동안 커졌다 찍힌다(결과 화면은 멈춘 뒤에만 보이므로 달리는 중엔 없음)
    val stamp = remember { androidx.compose.animation.core.Animatable(1.8f) }
    LaunchedEffect(g.resultIdx) { stamp.snapTo(1.8f); stamp.animateTo(1f, androidx.compose.animation.core.tween(350)) }
    val sub = when (k) {
        Kind.FOOD_PRICE -> stringResource(R.string.result_price, won(actual))
        Kind.FOOD_NAME -> stringResource(R.string.result_food_name, labels[correctIdx], Josa.ieyo(labels[correctIdx]))
        Kind.GAS_PRICE -> stringResource(R.string.result_gas, labels[correctIdx])
        Kind.FESTIVAL -> stringResource(R.string.result_festival, labels[correctIdx], Josa.ieyo(labels[correctIdx]))
        Kind.FACILITY -> stringResource(R.string.result_facility, labels[correctIdx], Josa.iGa(labels[correctIdx]))
        Kind.ARRIVAL -> if (actual > 0) stringResource(R.string.result_arrival_early, actual) else if (actual < 0) stringResource(R.string.result_arrival_late, -actual) else stringResource(R.string.result_arrival_same)
        Kind.FUEL -> stringResource(R.string.result_fuel, "$actual")
        else -> stringResource(R.string.result_material, matOptions[correctIdx], Josa.ieyo(matOptions[correctIdx]))
    }
    Frame(
        title = null, button = stringResource(R.string.result_ok),
        onButton = {
            if (g.resultIdx + 1 < g.betsDone.size) g.resultIdx += 1
            else { g.showResult = false; g.started = false; g.betsDone.clear(); g.guesses.clear(); g.late.clear(); g.resultIdx = 0; g.step = Step.BET }
        }
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(if (tie) stringResource(R.string.result_tie) else stringResource(R.string.result_loser, PLAYERS[loser]), style = TripType.HeadlineSmall.ko(), color = c.text, modifier = Modifier.weight(1f))
            if (!tie) SealStamp("꼴\n찌", Modifier.graphicsLayer { scaleX = stamp.value; scaleY = stamp.value; alpha = ((1.8f - stamp.value) / 0.6f).coerceIn(0f, 1f) })
        }
        Text(sub, style = TripType.BodySmall.ko(), color = c.textSecondary)
        Spacer(Modifier.height(12.dp))
        PLAYERS.forEachIndexed { i, n ->
            Row(Modifier.fillMaxWidth().padding(vertical = 5.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(52.dp).clip(CircleShape).background(c.surfaceHigh), contentAlignment = Alignment.Center) {
                    Text(n.take(1), style = TripType.LabelMedium.copy(fontWeight = FontWeight.Bold).ko(), color = c.text)
                }
                Spacer(Modifier.width(14.dp))
                val lose = !tie && i == loser
                // D안: 이긴(또는 동점) 쪽 = 조각보 2칸, 진 쪽 = 창살 6칸
                Box(Modifier.weight(1f)) {
                    if (lose) PatchworkProgress(passed = 0, rest = 6, height = 28.dp, cellWidth = 22.dp)
                    else PatchworkProgress(passed = 2, rest = 0, height = 28.dp, cellWidth = 20.dp)
                }
                Text(diffTexts[i], style = TripType.BodyMedium.copy(fontWeight = FontWeight.Bold).ko(), color = c.text)
            }
        }
        Spacer(Modifier.height(8.dp))
        SurfaceCard(Modifier.fillMaxWidth()) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(stringResource(R.string.result_card_label, stop.name), style = TripType.LabelMedium.ko(), color = c.textSecondary)
                    Text(food.foodName, style = TripType.TitleMedium.ko(), color = c.text, maxLines = 1)
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text("%,d".format(food.price ?: 0), style = TripType.DisplayResult.copy(fontSize = 52.sp, lineHeight = 60.sp).ko(), color = c.text)
                        Text(" " + stringResource(R.string.unit_won), style = TripType.LabelMedium.ko(), color = c.textSecondary, modifier = Modifier.padding(bottom = 8.dp))
                    }
                }
                PotArt()
            }
        }
        Spacer(Modifier.height(8.dp))
        Text(stringResource(R.string.result_price_source), style = TripType.LabelSmall.ko(), color = c.textSecondary)

    }
}

/** 「꼴찌」 도장. scale 1.8 → 1 로 줄며 나타난다. 빨강 면 위 어두운 글자(대비 확보). */
@Composable
fun Stamp(scale: Float) {
    val c = TripTheme.colors
    val alpha = ((1.8f - scale) / 0.6f).coerceIn(0f, 1f)
    Box(
        Modifier.padding(end = 10.dp).graphicsLayer { scaleX = scale; scaleY = scale; rotationZ = -8f; this.alpha = alpha }
            .clip(RoundedCornerShape(8.dp)).background(c.negative).padding(horizontal = 10.dp, vertical = 2.dp)
    ) { Text(stringResource(R.string.result_stamp), style = TripType.LabelSmall.copy(fontWeight = FontWeight.Bold), color = c.onNegative, maxLines = 1) }
}

/** 픽셀 국밥 그림(장식). */
@Composable
fun Bowl() {
    val c = TripTheme.colors
    Canvas(Modifier.size(width = 120.dp, height = 90.dp)) {
        val cell = 11.dp.toPx(); val gap = 2.dp.toPx()
        val rows = listOf("  x  x  ", "         ", " yyyyyyy ", "bbbbbbbbb", "bbbbbbbbb", " bbbbbbb ", "  bbbbb  ")
        rows.forEachIndexed { r, line -> line.forEachIndexed { k, ch ->
            val col = when (ch) { 'x', 'b' -> c.pixelOff; 'y' -> c.point; else -> null }
            if (col != null) drawRoundRect(col, Offset(k * (cell + gap), r * (cell + gap)), Size(cell, cell), CornerRadius(2.dp.toPx()))
        } }
    }
}

// ───────────────────────── 12 답 확인 ─────────────────────────

/** 멈춘 뒤, 음성으로 들은 답을 한 번 확인한다. 맞아요 → 다음, 고칠래요 → 한 번만 고칠 수 있다. */
@Composable
fun ConfirmScreen(q: QuizSession) {
    val c = TripTheme.colors
    val list = q.items.indices.filter { q.picks[it] != null }
    val i = list.getOrNull(q.confirmAt)
    if (i == null) { SideEffect { q.confirmed = true }; return }
    val item = q.items[i]
    Frame(
        title = stringResource(R.string.confirm_title), onBack = { if (q.confirmAt > 0) q.confirmAt -= 1 },
        hint = stringResource(R.string.confirm_hint),
        button = stringResource(R.string.confirm_yes), onButton = { if (q.confirmAt + 1 < list.size) q.confirmAt += 1 else q.confirmed = true },
        button2 = if (!q.fixed[i]) stringResource(R.string.confirm_fix) else null, onButton2 = { q.fixing = true }
    ) {
        SurfaceCard(Modifier.fillMaxWidth()) {
            Text(item.question, style = TripType.LabelMedium.ko(), color = c.textSecondary)
            Text(stringResource(R.string.confirm_heard), style = TripType.BodyMedium.ko(), color = c.textSecondary)
            Text(item.choices[q.picks[i] ?: 0], style = TripType.DisplayResult.copy(fontSize = 56.sp, lineHeight = 68.sp).ko(), color = c.text, maxLines = 2)
        }
    }
}

@Composable
fun FixScreen(q: QuizSession) {
    val c = TripTheme.colors
    val list = q.items.indices.filter { q.picks[it] != null }
    val i = list.getOrNull(q.confirmAt) ?: return
    val item = q.items[i]
    Frame(title = stringResource(R.string.confirm_fix_title), onBack = { q.fixing = false }) {
        SurfaceCard(Modifier.fillMaxWidth()) {
            Text(item.question, style = TripType.LabelMedium.ko(), color = c.textSecondary)
            item.choices.forEachIndexed { k, ch ->
                if (k > 0) Divider()
                Row(Modifier.fillMaxWidth().clickable {
                    q.picks[i] = k; q.fixed[i] = true; q.fixing = false
                    if (q.confirmAt + 1 < list.size) q.confirmAt += 1 else q.confirmed = true
                }.padding(vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(ch, style = ItemTitle.ko(), color = c.text, modifier = Modifier.weight(1f))
                    Mark(q.picks[i] == k)
                }
            }
        }
    }
}

// ───────────────────────── 7·9 퀴즈 결과·상품권 ─────────────────────────

@Composable
fun QuizResultScreen(g: Game, q: QuizSession, credit: String, onDone: () -> Unit) {
    val c = TripTheme.colors
    val p = q.place
    val ok = q.passed
    val show = if (ok) q.items.lastIndex else (q.items.indices.firstOrNull { q.picks[it] != q.items[it].answer } ?: 0)
    val it0 = q.items[show]
    val answerText = it0.choices[it0.answer]
    Frame(
        title = stringResource(if (ok) R.string.qres_right else R.string.qres_wrong),
        button = stringResource(if (ok) R.string.qres_get else R.string.qres_close),
        onButton = { if (ok) { g.wallet.add(p.voucher); g.showWallet = true }; g.quiz = null; onDone() }
    ) {
        SurfaceCard(Modifier.fillMaxWidth()) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                PlacePhoto(p.imageFile, p.name.hashCode(), Modifier.size(132.dp))
                Spacer(Modifier.width(20.dp))
                Column(Modifier.weight(1f)) {
                    Text(p.name, style = TripType.TitleMedium.ko(), color = c.text, maxLines = 1)
                    Text(if (ok) stringResource(R.string.qres_all_right) else stringResource(R.string.qres_score, q.items.size, q.correctCount), style = TripType.BodySmall.copy(fontWeight = FontWeight.Bold).ko(), color = c.text, maxLines = 2)
                    Row(Modifier.padding(vertical = 4.dp)) {
                        q.items.indices.forEach { i -> Mark(q.picks[i] == q.items[i].answer, 22.dp); Spacer(Modifier.width(6.dp)) }
                    }
                    Text(stringResource(R.string.qres_answer, answerText, Josa.ieyo(answerText)), style = TripType.LabelSmall.ko(), color = c.textSecondary, maxLines = 1)
                    Text(stringResource(R.string.qres_basis, it0.basis), style = TripType.LabelSmall.ko(), color = c.textSecondary, maxLines = 2)
                }
            }
        }
        Spacer(Modifier.height(10.dp))
        if (ok) VoucherCard(p.voucher, p.venueName)
        else SurfaceCard(Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.qres_wrong_note), style = TripType.BodySmall.ko(), color = c.textSecondary, modifier = Modifier.padding(vertical = 8.dp))
        }
        Spacer(Modifier.height(6.dp))
        Text(if (ok) stringResource(R.string.qres_notice, credit) else stringResource(R.string.qres_credit, credit), style = TripType.LabelSmall.ko(), color = c.textSecondary)
    }
}

/** 명소 사진: 내장 사진 파일이 있으면 그것, 없으면 모자이크. 출처 표기는 결과 화면 아래 한 줄. */
@Composable
fun PlacePhoto(file: String?, seed: Int, modifier: Modifier) {
    val ctx = LocalContext.current
    val bmp = remember(file) {
        file?.let { f -> try { ctx.assets.open("demo/img/$f").use { s -> android.graphics.BitmapFactory.decodeStream(s) }?.asImageBitmap() } catch (e: Exception) { null } }
    }
    if (bmp != null) Image(bmp, null, modifier.clip(RoundedCornerShape(20.dp)).background(TripTheme.colors.surfaceHigh), contentScale = ContentScale.Fit) else Mosaic(seed, modifier)
}

@Composable
fun VoucherCard(v: Voucher, venue: String?) {
    val c = TripTheme.colors
    val region = v.where.removeSuffix("에서 사용").removeSuffix(" 가맹점")
    SurfaceCard(Modifier.fillMaxWidth(), dashed = true) {
        MeanderLine(Modifier.padding(top = 6.dp, bottom = 4.dp))
        Tag(stringResource(R.string.voucher_tag))
        Spacer(Modifier.height(4.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(v.title, style = ItemTitle.ko(), color = c.text, maxLines = 1)
                Text(
                    if (venue != null) stringResource(R.string.voucher_where_example, region, venue) else stringResource(R.string.voucher_where, region),
                    style = TripType.LabelSmall.ko(), color = c.textSecondary, maxLines = 1
                )
            }
            Row(verticalAlignment = Alignment.Bottom) {
                Text("%,d".format(v.won), style = TripType.TitleLarge.ko(), color = c.text)
                Text(" " + stringResource(R.string.unit_won), style = TripType.LabelMedium.ko(), color = c.textSecondary, modifier = Modifier.padding(bottom = 6.dp))
            }
        }
    }
}

// ───────────────────────── 8 상품권 지갑 ─────────────────────────

@Composable
fun WalletScreen(g: Game) {
    val c = TripTheme.colors
    Frame(title = stringResource(R.string.wallet_title), onBack = { g.showWallet = false }) {
        if (g.wallet.isEmpty()) Text(stringResource(R.string.wallet_empty), style = TripType.BodyMedium.ko(), color = c.textSecondary)
        g.wallet.toList().forEach { v -> VoucherCard(v, null); Spacer(Modifier.height(12.dp)) }
        Text(stringResource(R.string.wallet_hint), style = TripType.LabelSmall.ko(), color = c.textSecondary)
    }
}




