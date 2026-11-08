package kr.tripbet.app.data

import android.content.Context
import org.json.JSONObject
import java.time.LocalTime

/** 반려견 프로필. 이름은 선택. 기기 안(SharedPreferences)에만 저장하고 서버로 보내지 않는다. 체고·몸무게 0 은 아직 안 넣음. */
data class PetProfile(val name: String = "", val heightCm: Int = 0, val weightKg: Int = 0, val dangerous: Boolean = false) {
    val filled get() = heightCm > 0 && weightKg > 0
}

class PetStore(ctx: Context) {
    private val sp = ctx.getSharedPreferences("pet", Context.MODE_PRIVATE)
    var on: Boolean
        get() = sp.getBoolean("on", false)
        set(v) { sp.edit().putBoolean("on", v).apply() }
    var profile: PetProfile
        get() = PetProfile(sp.getString("name", "") ?: "", sp.getInt("h", 0), sp.getInt("w", 0), sp.getBoolean("d", false))
        set(p) { sp.edit().putString("name", p.name).putInt("h", p.heightCm).putInt("w", p.weightKg).putBoolean("d", p.dangerous).apply() }

    /** 시연 영상용 출발 시각("HH:MM"). 앱 내부 폴더 files/demo_depart.txt 에 adb 로 넣었을 때만 쓴다. 있으면 화면에 「시연용 시각」을 적는다. */
    private val demoFile = java.io.File(ctx.filesDir, "demo_depart.txt")
    val demoDepart: LocalTime?
        get() = runCatching { LocalTime.parse(demoFile.readText().trim()) }.getOrNull()
}

/** 휴게소 반려견 시설. null 은 공공 근거에서 확인되지 않은 것(추측하지 않는다). */
data class PetSpot(
    val name: String, val direction: String?, val kind: String?, val heightLimitCm: Int?, val weightLimitKg: Int?,
    val sizeSplit: String?, val breedRule: String?, val fee: String?, val hours: String?, val status: String?, val asOfText: String?,
) {
    val constructionOrClosed: String? get() = when {
        status == null -> null
        status.contains("공사") -> "construction"
        status.contains("폐쇄") -> "closed"
        else -> null
    }
    /** 「10:00~19:00」 꼴에서 연 시각·닫는 시각. 읽을 수 없으면 null. */
    val openRange: Pair<LocalTime, LocalTime>? get() {
        val m = Regex("(\\d{1,2}):(\\d{2})\\s*[~∼-]\\s*(\\d{1,2}):(\\d{2})").find(hours ?: return null) ?: return null
        return runCatching {
            val g = m.groupValues
            LocalTime.of(g[1].toInt(), g[2].toInt()) to (if (g[3].toInt() == 24) LocalTime.MAX else LocalTime.of(g[3].toInt(), g[4].toInt()))
        }.getOrNull()
    }
}

enum class PetVerdict { OK, CHECK, NO }
enum class PetReason { ALL_OK, MISSING, NO_PROFILE, HEIGHT_OVER, WEIGHT_OVER, DANGEROUS, CONSTRUCTION, CLOSED }
data class PetJudgement(val verdict: PetVerdict, val reason: PetReason, val missing: List<String> = emptyList())

/**
 * 이용 조건 판정(순수 계산).
 * 이용 불가: 체고·몸무게 한도 초과가 분명하거나 공사 중·폐쇄 근거가 있을 때.
 * 이용 가능: 체고(또는 몸무게) 한도가 자료에 있고 맞으며, 맹견 프로필이 아닐 때.
 * 조건 확인 필요: 그 밖(한도 자료 없음, 맹견 프로필인데 맹견 규정을 글로만 알 때, 프로필 비어 있음).
 */
fun judgePet(s: PetSpot, p: PetProfile): PetJudgement {
    when (s.constructionOrClosed) {
        "construction" -> return PetJudgement(PetVerdict.NO, PetReason.CONSTRUCTION)
        "closed" -> return PetJudgement(PetVerdict.NO, PetReason.CLOSED)
    }
    if (!p.filled) return PetJudgement(PetVerdict.CHECK, PetReason.NO_PROFILE)
    s.heightLimitCm?.let { if (p.heightCm > it) return PetJudgement(PetVerdict.NO, PetReason.HEIGHT_OVER) }
    s.weightLimitKg?.let { if (p.weightKg > it) return PetJudgement(PetVerdict.NO, PetReason.WEIGHT_OVER) }
    val missing = buildList {
        if (s.heightLimitCm == null && s.weightLimitKg == null) add("체고 한도")
        if (p.dangerous) add("맹견 규정")
    }
    return if (missing.isEmpty()) PetJudgement(PetVerdict.OK, PetReason.ALL_OK) else PetJudgement(PetVerdict.CHECK, PetReason.MISSING, missing)
}

/** 도착 예상 시각이 운영 시간 밖이면 true(운영 시간을 모르면 null). */
fun closedAtArrival(s: PetSpot, arrival: LocalTime): Boolean? {
    val r = s.openRange ?: return null
    return arrival.isBefore(r.first) || !arrival.isBefore(r.second)
}

/** assets/demo/pet_rest.json(반려시설_휴게소.json 그대로). 반려 시설 근거가 있는 휴게소(pet 가 null 이 아닌 곳)만 담는다. 없으면 빈 목록. */
class PetData(ctx: Context) {
    val asOfFallback: String
    val spots: List<PetSpot>

    init {
        val text = try { DataStore(ctx).demo("pet_rest.json") } catch (e: Exception) { null }
        if (text == null) { asOfFallback = ""; spots = emptyList() }
        else {
            val o = JSONObject(text)
            asOfFallback = o.optJSONObject("asOf")?.optString("kexCount") ?: ""
            val a = o.getJSONArray("stops")
            val list = ArrayList<PetSpot>()
            for (i in 0 until a.length()) {
                val s = a.getJSONObject(i)
                val p = s.optJSONObject("pet") ?: continue
                if (!p.optBoolean("playground", false)) continue
                fun str(k: String) = if (p.isNull(k)) null else p.optString(k).takeIf { it.isNotBlank() }
                fun num(k: String) = if (p.isNull(k)) null else p.optInt(k)
                list.add(PetSpot(
                    cleanStopName(s.getString("name")), s.optString("direction").takeIf { it.isNotBlank() && !it.startsWith("미확인") },
                    str("kind"), num("heightLimitCm"), num("weightLimitKg"), str("sizeSplit"), str("muzzleOrBreedRule"),
                    str("fee"), str("hours"), str("status"), s.optString("asOfText").takeIf { it.isNotBlank() }
                ))
            }
            spots = list
        }
    }

    fun forStop(stopName: String): PetSpot? { val n = cleanStopName(stopName); return spots.firstOrNull { it.name == n } }
}

/** 「여주(강릉)휴게소」 → 「여주」 */
fun cleanStopName(s: String): String = s.replace(Regex("\\(.*?\\)"), "").replace("휴게소", "").trim()
