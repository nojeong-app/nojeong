package kr.tripbet.app.data

import android.content.Context
import org.json.JSONArray

/** assets/demo 의 미리 담은 데이터(서울→강릉, 영동선 강릉방향). 제출용 빌드는 이것만 쓴다. */
data class RestStop(val order: Int, val name: String, val minFromStart: Int, val foods: List<RestFood>, val lat: Double? = null, val lng: Double? = null)
data class ChargeStop(val name: String, val addr: String, val chargers: Int, val availableNow: Int)

/** 휴게소 이름 끝의 방향 표기 "(강릉)" 등을 뗀다. */
private fun cleanName(s: String): String =
    s.replace("(강릉)", "").replace("(인천)", "").replace("(서울)", "").trim()

/** 음식 이름 앞의 머리표("특)" 등)를 뗀다. */
private fun cleanFood(s: String): String = s.replace(Regex("^[가-힣A-Za-z]{1,3}\\)"), "").trim()

/** 도로공사 공개 자료: 휴게소 편의시설·휘발유 가격(assets/demo/restinfo.json). */
data class RestInfo(val gasoline: Int?, val facilities: List<String>)

/** 한국관광공사 searchFestival2 에서 받아 둔 행사(assets/demo/festivals.json). */
data class Festival(val name: String, val city: String, val start: String, val end: String)

class DemoData(ctx: Context) {
    private val store = DataStore(ctx)

    private val placeRows = JSONArray(store.demo("places.json")).let { a -> List(a.length()) { a.getJSONObject(it) } }
    val places: List<Place> = placeRows.map { o ->
        Place(o.getString("title"), o.getString("addr"), o.getDouble("lng"), o.getDouble("lat"),
            o.optDouble("distM"), if (o.isNull("image")) null else o.getString("image"))
    }
    val placeTypes: List<Int> = placeRows.map { it.getInt("type") }

    val restStops: List<RestStop> = JSONArray(store.demo("restfoods.json")).let { a ->
        val rows = List(a.length()) { a.getJSONObject(it) }
        rows.groupBy { it.getInt("order") }.toSortedMap().map { (order, g) ->
            RestStop(order, cleanName(g[0].getString("rest")), g[0].getInt("minFromStart"),
                g.map { o ->
                    RestFood(cleanName(o.getString("rest")), cleanFood(o.getString("food")), o.optInt("price").takeIf { it > 0 },
                        o.optBoolean("best"), o.optBoolean("recommended"), if (o.isNull("note")) null else o.getString("note"),
                        o.optJSONArray("materials")?.let { m -> List(m.length()) { m.getString(it) } } ?: emptyList())
                },
                g[0].optDouble("lat").takeIf { !it.isNaN() }, g[0].optDouble("lng").takeIf { !it.isNaN() })
        }
    }

    val chargeStops: List<ChargeStop> = JSONArray(store.demo("chargers.json")).let { a ->
        List(a.length()) { a.getJSONObject(it).let { o ->
            ChargeStop(cleanName(o.getString("name")), o.getString("addr"), o.getInt("chargers"), o.getInt("availableNow"))
        } }
    }

    private val restInfoJson = org.json.JSONObject(DataStore(ctx).demo("restinfo.json"))
    val allFacilities: List<String> = restInfoJson.getJSONArray("allFacilities").let { a -> List(a.length()) { a.getString(it) } }
    private val infos: Map<String, RestInfo> = restInfoJson.getJSONObject("stops").let { o ->
        o.keys().asSequence().associate { key ->
            val v = o.getJSONObject(key)
            cleanName(key) to RestInfo(
                v.optString("gasoline").filter { it.isDigit() }.toIntOrNull(),
                v.getJSONArray("facilities").let { f -> List(f.length()) { f.getString(it) } }
            )
        }
    }
    fun restInfo(stopName: String): RestInfo? = infos[stopName]

    val festivals: List<Festival> = org.json.JSONObject(DataStore(ctx).demo("festivals.json")).getJSONArray("festivals").let { a ->
        List(a.length()) { a.getJSONObject(it).let { o -> Festival(o.getString("name"), o.getString("city"), o.getString("start"), o.getString("end")) } }
    }

    /** 목적지(예: 「강릉」) 도시의 행사 중 이번 달에 열리는 것. 기기 날짜 기준. */
    fun festivalsThisMonth(dest: String): List<Festival> {
        val now = java.time.LocalDate.now()
        val ym = "%04d%02d".format(now.year, now.monthValue)
        return festivals.filter { it.city.startsWith(dest) && it.start.take(6) <= ym && it.end.take(6) >= ym }
    }
}
