package kr.tripbet.app.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kr.tripbet.app.Josa
import kr.tripbet.app.R
import kr.tripbet.app.data.DemoData
import kr.tripbet.app.data.PetJudgement
import kr.tripbet.app.data.PetProfile
import kr.tripbet.app.data.PetReason
import kr.tripbet.app.data.PetSpot
import kr.tripbet.app.data.PetVerdict
import kr.tripbet.app.data.cleanStopName
import kr.tripbet.app.data.closedAtArrival
import kr.tripbet.app.data.judgePet
import kr.tripbet.app.ui.theme.*
import java.time.LocalTime

// ───────────────────────── 작은 그림 ─────────────────────────

@Composable
fun PawIcon(size: Dp = 22.dp, color: Color = TripTheme.colors.text) {
    Canvas(Modifier.size(size)) {
        val w = this.size.width
        drawCircle(color, w * 0.11f, Offset(w * 0.20f, w * 0.42f)); drawCircle(color, w * 0.11f, Offset(w * 0.40f, w * 0.22f))
        drawCircle(color, w * 0.11f, Offset(w * 0.60f, w * 0.22f)); drawCircle(color, w * 0.11f, Offset(w * 0.80f, w * 0.42f))
        drawOval(color, Offset(w * 0.27f, w * 0.50f), androidx.compose.ui.geometry.Size(w * 0.46f, w * 0.38f))
    }
}

/** 판정 표시(모양으로도 구분): ✓ / ! / ✕ 를 색 원 안에. */
@Composable
fun VerdictMark(v: PetVerdict, size: Dp = 26.dp) {
    val c = TripTheme.colors
    val col = when (v) { PetVerdict.OK -> c.green; PetVerdict.CHECK -> c.ochre; PetVerdict.NO -> c.stamp }
    Canvas(Modifier.size(size)) {
        val w = this.size.width; val sw = w * 0.1f
        drawCircle(col)
        val white = Color.White
        when (v) {
            PetVerdict.OK -> drawPath(Path().apply { moveTo(w * 0.27f, w * 0.52f); lineTo(w * 0.44f, w * 0.68f); lineTo(w * 0.74f, w * 0.34f) }, white, style = Stroke(sw, cap = StrokeCap.Round))
            PetVerdict.CHECK -> { drawLine(white, Offset(w * 0.5f, w * 0.24f), Offset(w * 0.5f, w * 0.57f), sw, StrokeCap.Round); drawCircle(white, sw * 0.7f, Offset(w * 0.5f, w * 0.75f)) }
            PetVerdict.NO -> { drawLine(white, Offset(w * 0.32f, w * 0.32f), Offset(w * 0.68f, w * 0.68f), sw, StrokeCap.Round); drawLine(white, Offset(w * 0.68f, w * 0.32f), Offset(w * 0.32f, w * 0.68f), sw, StrokeCap.Round) }
        }
    }
}

@Composable
private fun verdictColor(v: PetVerdict): Color { val c = TripTheme.colors; return when (v) { PetVerdict.OK -> c.green; PetVerdict.CHECK -> c.ochre; PetVerdict.NO -> c.stamp } }

@Composable
private fun verdictLabel(v: PetVerdict) = stringResource(when (v) { PetVerdict.OK -> R.string.pet_v_ok; PetVerdict.CHECK -> R.string.pet_v_check; PetVerdict.NO -> R.string.pet_v_no })

/** 경유지 줄 오른쪽 꼬리표: 발바닥 + ✓/!/✕ + 글자. 누르면 상세. */
@Composable
fun PetStopTag(v: PetVerdict, onClick: () -> Unit) {
    val c = TripTheme.colors
    Row(
        Modifier.clip(RoundedCornerShape(24.dp)).border(2.dp, verdictColor(v), RoundedCornerShape(24.dp)).clickable(onClick = onClick).padding(horizontal = 12.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        PawIcon(20.dp, c.text); Spacer(Modifier.width(6.dp)); VerdictMark(v, 22.dp); Spacer(Modifier.width(6.dp))
        Text(verdictLabel(v), style = TripType.LabelSmall.copy(fontWeight = FontWeight.Bold).ko(), color = c.text, maxLines = 1)
    }
}

/** 제목 줄 오른쪽 칩 「반려동물 동반」: 켜짐은 채움, 꺼짐은 테두리. 누르면 동반 화면. */
@Composable
fun PetChip(g: Game) {
    val c = TripTheme.colors
    val shape = RoundedCornerShape(24.dp)
    Row(
        Modifier.clip(shape).then(if (g.petOn) Modifier.background(c.surfaceAccent) else Modifier.border(2.dp, c.outline, shape)).clickable { g.petScreen = 1 }.padding(horizontal = 14.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        PawIcon(20.dp, if (g.petOn) c.text else c.textSecondary); Spacer(Modifier.width(8.dp))
        Text(stringResource(R.string.pet_chip), style = TripType.LabelSmall.copy(fontWeight = FontWeight.Bold).ko(), color = if (g.petOn) c.text else c.textSecondary, maxLines = 1)
    }
}

@Composable
fun petName(p: PetProfile): String = p.name.ifBlank { stringResource(R.string.pet_name_default) }

// ───────────────────────── 30 동반 모드 켜기 ─────────────────────────

@Composable
fun PetSettingsScreen(g: Game) {
    val c = TripTheme.colors
    Frame(
        title = stringResource(R.string.pet_screen_title), onBack = { g.petScreen = 0 },
        note = stringResource(R.string.pet_local_note),
        button = stringResource(R.string.pet_see_rest), onButton = { g.petScreen = 0 }
    ) {
        SurfaceCard(Modifier.fillMaxWidth()) {
            Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(stringResource(R.string.pet_title), style = ItemTitle.ko(), color = c.text)
                    Text(stringResource(R.string.pet_sub), style = TripType.LabelSmall.ko(), color = c.textSecondary)
                }
                TripSwitch(g.petOn) { g.switchPet(!g.petOn); if (g.petOn && !g.pet.filled) g.petScreen = 2 }
            }
        }
        if (g.petOn) {
            Spacer(Modifier.height(10.dp))
            SurfaceCard(Modifier.fillMaxWidth().clickable { g.petScreen = 2 }) {
                Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(stringResource(R.string.pet_info_title), style = ItemTitle.ko(), color = c.text)
                        Text(
                            if (g.pet.filled) stringResource(R.string.pet_summary, petName(g.pet), g.pet.heightCm, g.pet.weightKg) else stringResource(R.string.pet_empty),
                            style = TripType.LabelSmall.ko(), color = c.textSecondary, maxLines = 1
                        )
                    }
                    Text(stringResource(R.string.pet_edit), style = TripType.LabelMedium.ko(), color = c.textSecondary)
                    Spacer(Modifier.width(8.dp)); Chevron()
                }
            }
        }
    }
}

// ───────────────────────── 31 반려견 정보 ─────────────────────────

@Composable
fun PetProfileScreen(g: Game) {
    val c = TripTheme.colors
    var name by remember { mutableStateOf(g.pet.name) }
    var h by remember { mutableStateOf(g.pet.heightCm) }       // 0 = 아직 안 정함
    var w by remember { mutableStateOf(g.pet.weightKg) }
    var d by remember { mutableStateOf(g.pet.dangerous) }
    Frame(
        title = stringResource(R.string.pet_profile_title), onBack = { g.petScreen = 1 },
        note = stringResource(R.string.pet_profile_hint),
        button = stringResource(R.string.pet_save), onButton = { g.savePet(PetProfile(name.trim(), h, w, d)); g.petScreen = 1 }
    ) {
        SurfaceCard(Modifier.fillMaxWidth()) {
            Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(R.string.pet_f_name), style = ItemTitle.ko(), color = c.text, modifier = Modifier.width(120.dp))
                Box(Modifier.weight(1f).height(TripDims.Touch).clip(RoundedCornerShape(TripDims.ButtonRadius)).background(c.surfaceHigh).padding(horizontal = 20.dp), contentAlignment = Alignment.CenterStart) {
                    if (name.isEmpty()) Text(stringResource(R.string.pet_f_name_hint), style = TripType.BodySmall.ko(), color = c.textSecondary, maxLines = 1)
                    BasicTextField(name, { if (it.length <= 8) name = it }, singleLine = true, textStyle = TripType.BodySmall.ko().copy(color = c.text), cursorBrush = SolidColor(c.primary), modifier = Modifier.fillMaxWidth())
                }
            }
            Divider()
            NumberRow(stringResource(R.string.pet_f_height), stringResource(R.string.pet_f_height_sub), h, stringResource(R.string.pet_unit_cm),
                { h = if (h == 0) 40 else (h - 5).coerceAtLeast(5) }, { h = if (h == 0) 40 else (h + 5).coerceAtMost(100) })
            Divider()
            NumberRow(stringResource(R.string.pet_f_weight), null, w, stringResource(R.string.pet_unit_kg),
                { w = if (w == 0) 10 else (w - 1).coerceAtLeast(1) }, { w = if (w == 0) 10 else (w + 1).coerceAtMost(80) })
            Divider()
            Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(R.string.pet_f_danger), style = ItemTitle.ko(), color = c.text, modifier = Modifier.weight(1f))
                Segmented(listOf(stringResource(R.string.pet_danger_no), stringResource(R.string.pet_danger_yes)), if (d) 1 else 0, Modifier.width(300.dp)) { d = it == 1 }
            }
        }
    }
}

@Composable
private fun NumberRow(label: String, sub: String?, value: Int, unit: String, onMinus: () -> Unit, onPlus: () -> Unit) {
    val c = TripTheme.colors
    Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(label, style = ItemTitle.ko(), color = c.text)
            if (sub != null) Text(sub, style = TripType.LabelSmall.ko(), color = c.textSecondary)
        }
        Stepper("−", onMinus)
        Text(if (value == 0) stringResource(R.string.pet_unset) else "$value$unit", style = TripType.BodyMedium.copy(fontWeight = FontWeight.Bold).ko(),
            color = if (value == 0) c.textSecondary else c.text, modifier = Modifier.widthIn(min = 120.dp), textAlign = TextAlign.Center, maxLines = 1)
        Stepper("+", onPlus)
    }
}

// ───────────────────────── 판정 글·도착 시각 ─────────────────────────

/** 판정 사유 한 줄(문구표 11의 틀). 조사는 받침에 맞춰. */
@Composable
fun petReasonText(j: PetJudgement, s: PetSpot, p: PetProfile): String {
    val nm = petName(p)
    return when (j.reason) {
        PetReason.ALL_OK -> stringResource(R.string.pet_r_ok, nm, p.heightCm, p.weightKg, Josa.eunNeun(nm))
        PetReason.MISSING -> stringResource(R.string.pet_r_missing, j.missing.joinToString("·"))
        PetReason.NO_PROFILE -> stringResource(R.string.pet_r_noprofile)
        PetReason.HEIGHT_OVER -> stringResource(R.string.pet_r_height, s.heightLimitCm ?: 0, nm, Josa.eunNeun(nm), p.heightCm)
        PetReason.WEIGHT_OVER -> stringResource(R.string.pet_r_weight, s.weightLimitKg ?: 0, nm, Josa.eunNeun(nm), p.weightKg)
        PetReason.DANGEROUS -> ""
        PetReason.CONSTRUCTION -> stringResource(R.string.pet_r_construction)
        PetReason.CLOSED -> stringResource(R.string.pet_r_closed)
    }
}

/** 출발 시각(시연용 시각이 있으면 그것, 없으면 지금) + 그 휴게소까지 걸리는 분 = 도착 예상 시각. */
fun petArrival(g: Game, data: DemoData, s: PetSpot): LocalTime {
    val depart = g.petStore?.demoDepart ?: LocalTime.now()
    val min = data.restStops.firstOrNull { cleanStopName(it.name) == s.name }?.minFromStart ?: 0
    return depart.plusMinutes(min.toLong())
}

fun fmt(t: LocalTime) = "%02d:%02d".format(t.hour, t.minute)

// ───────────────────────── 33~35 휴게소 반려견 놀이터 상세 ─────────────────────────

@Composable
fun PetDetailScreen(g: Game, data: DemoData) {
    val c = TripTheme.colors
    val spot = g.petData?.spots?.firstOrNull { it.name == g.petStop }
    if (spot == null) { g.petScreen = 0; return }
    val p = g.pet
    val j = judgePet(spot, p)
    val arrival = petArrival(g, data, spot)
    val closed = closedAtArrival(spot, arrival) == true
    val no = j.verdict == PetVerdict.NO
    val stopFull = data.restStops.firstOrNull { cleanStopName(it.name) == spot.name }
    val sel = stopFull != null && stopFull.order in g.picked
    Frame(
        title = stringResource(R.string.pet_detail_title, spot.name + "휴게소"), onBack = { g.petScreen = 0 },
        note = stringResource(R.string.pet_asof, spot.asOfText ?: g.petData?.asOfFallback ?: "") +
            (g.petStore?.demoDepart?.let { " · " + stringResource(R.string.pet_demo_time, fmt(it)) } ?: ""),
        button = stringResource(if (no) R.string.pet_other_rest else R.string.pet_to_waypoint),
        onButton = { if (!no && stopFull != null && !sel && g.picked.size < 2) g.picked.add(stopFull.order); g.petScreen = 0 }
    ) {
        SurfaceCard(Modifier.fillMaxWidth()) {
            Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                VerdictMark(j.verdict, 52.dp); Spacer(Modifier.width(16.dp))
                Column {
                    Text(verdictLabel(j.verdict), style = TripType.TitleMedium.ko(), color = c.text)
                    Text(petReasonText(j, spot, p), style = TripType.LabelSmall.ko(), color = c.textSecondary, maxLines = 2)
                }
            }
        }
        if (closed) {
            Spacer(Modifier.height(8.dp))
            SurfaceCard(Modifier.fillMaxWidth()) {
                Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                    VerdictMark(PetVerdict.CHECK, 30.dp); Spacer(Modifier.width(12.dp))
                    Column {
                        Text(stringResource(R.string.pet_closed_arrive), style = ItemTitle.ko(), color = c.text)
                        Text(stringResource(R.string.pet_closed_arrive_detail, fmt(arrival), spot.hours ?: ""), style = TripType.LabelSmall.ko(), color = c.textSecondary, maxLines = 2)
                    }
                }
            }
        }
        Spacer(Modifier.height(8.dp))
        val none = stringResource(R.string.pet_val_none)
        val hasProfile = p.filled
        val rows = buildList {
            spot.constructionOrClosed?.let { add(Triple(stringResource(R.string.pet_row_status), spot.status ?: "", PetVerdict.NO as PetVerdict?)) }
            add(Triple(stringResource(R.string.pet_row_height),
                spot.heightLimitCm?.let { stringResource(R.string.pet_val_height, it) } ?: none,
                if (!hasProfile) null else spot.heightLimitCm?.let { if (p.heightCm > it) PetVerdict.NO else PetVerdict.OK } ?: PetVerdict.CHECK))
            spot.weightLimitKg?.let { add(Triple(stringResource(R.string.pet_row_weight), stringResource(R.string.pet_val_weight, it), if (!hasProfile) null else if (p.weightKg > it) PetVerdict.NO else PetVerdict.OK)) }
            add(Triple(stringResource(R.string.pet_row_size), spot.sizeSplit ?: none, null))
            add(Triple(stringResource(R.string.pet_row_breed), spot.breedRule ?: none, if (p.dangerous) PetVerdict.CHECK else null))
            add(Triple(stringResource(R.string.pet_row_fee), spot.fee ?: none, null))
            add(Triple(stringResource(R.string.pet_row_hours), spot.hours ?: none, if (closed) PetVerdict.CHECK else null))
            spot.direction?.let { add(Triple(stringResource(R.string.pet_row_dir), it, null)) }
        }
        SurfaceCard(Modifier.fillMaxWidth()) {
            rows.forEachIndexed { i, (label, value, mark) ->
                if (i > 0) Divider()
                Row(Modifier.fillMaxWidth().padding(vertical = 5.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(label, style = TripType.LabelSmall.ko(), color = c.textSecondary, modifier = Modifier.width(130.dp))
                    Text(value, style = TripType.BodySmall.copy(fontWeight = FontWeight.Bold).ko(), color = c.text, modifier = Modifier.weight(1f), maxLines = 2)
                    if (mark != null) VerdictMark(mark, 26.dp)
                }
            }
        }
    }
}
