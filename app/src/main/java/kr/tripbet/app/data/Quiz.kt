package kr.tripbet.app.data

import android.content.Context
import org.json.JSONObject
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/** 관광명소 퀴즈 데이터(assets/demo/quiz.json). 출처: 한국관광공사 관광정보 활용. 상품권은 시연용 예시. */
data class Voucher(val title: String, val won: Int, val where: String)
data class QuizItem(val question: String, val choices: List<String>, val answer: Int, val basis: String)
data class QuizPlace(
    val name: String, val lat: Double, val lng: Double, val addr: String, val region: String,
    val nearRest: String, val restKm: Double?, val voucher: Voucher,
    val venueName: String?, val venueDistM: Int?, val quiz: List<QuizItem>, val imageFile: String? = null,
)
class QuizData(val credit: String, val voucherNote: String, val places: List<QuizPlace>)

fun loadQuiz(ctx: Context): QuizData {
    val o = JSONObject(DataStore(ctx).demo("quiz.json"))
    val a = o.getJSONArray("places")
    val places = List(a.length()) { i ->
        val p = a.getJSONObject(i)
        val v = p.getJSONObject("voucher")
        val q = p.getJSONArray("quiz")
        QuizPlace(
            p.getString("name"), p.getDouble("lat"), p.getDouble("lng"), p.getString("addr"), p.getString("region"),
            p.getString("nearRest").replace("(강릉)", "").replace("(인천)", ""), p.optDouble("restKm").takeIf { !it.isNaN() },
            Voucher(v.getString("title"), v.getInt("won"), v.getString("where")),
            p.optJSONObject("venue")?.getString("name"), p.optJSONObject("venue")?.getInt("distM"),
            List(q.length()) { j -> q.getJSONObject(j).let { x ->
                QuizItem(x.getString("q"), x.getJSONArray("choices").let { c -> List(c.length()) { c.getString(it) } }, x.getInt("answer"), x.getString("basis"))
            } },
            if (p.isNull("imageFile")) null else p.getString("imageFile")
        )
    }
    return QuizData(o.getString("credit"), o.getString("voucherNote"), places)
}

/** 두 지점 사이 직선 거리(m). */
fun distanceM(lat1: Double, lng1: Double, lat2: Double, lng2: Double): Double {
    val r = 6371000.0
    val dLat = Math.toRadians(lat2 - lat1); val dLng = Math.toRadians(lng2 - lng1)
    val s = sin(dLat / 2) * sin(dLat / 2) + cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) * sin(dLng / 2) * sin(dLng / 2)
    return 2 * r * atan2(sqrt(s), sqrt(1 - s))
}
