package kr.tripbet.app.data

import kr.tripbet.app.Keys
import java.net.URLEncoder

/**
 * 데이터 계층 자리. 지금은 요청 주소 만들기와 응답 모양만 있고 네트워크 호출은 없다.
 * 모든 요청에 User-Agent 를 넣는다(없으면 도로공사가 400 으로 막음).
 */
const val USER_AGENT = "TripBet/0.1 (Android Automotive)"
private fun enc(s: String) = URLEncoder.encode(s, "UTF-8")

/** 한국관광공사 국문 관광정보 KorService2 — 매뉴얼 v4.4 */
object TourApi {
    private const val BASE = "https://apis.data.go.kr/B551011/KorService2"
    const val TYPE_ATTRACTION = 12
    const val TYPE_RESTAURANT = 39
    /** 위치 기반 조회. radius 는 m(최대 20000). arrange: E=거리순, S=대표이미지 있는 거리순. */
    fun locationBasedList(lng: Double, lat: Double, radiusM: Int, contentTypeId: Int? = null, rows: Int = 10) =
        "$BASE/locationBasedList2?serviceKey=${enc(Keys.dataGoKr)}&numOfRows=$rows&pageNo=1" +
            "&MobileOS=ETC&MobileApp=TripBet&_type=json&mapX=$lng&mapY=$lat&radius=$radiusM&arrange=S" +
            (contentTypeId?.let { "&contentTypeId=$it" } ?: "")
    /** 지역 기반 조회(여행지 고르기). lDongRegnCd = 법정동 시도 코드. */
    fun areaBasedList(regionCode: String, contentTypeId: Int = TYPE_ATTRACTION, rows: Int = 10) =
        "$BASE/areaBasedList2?serviceKey=${enc(Keys.dataGoKr)}&numOfRows=$rows&pageNo=1" +
            "&MobileOS=ETC&MobileApp=TripBet&_type=json&lDongRegnCd=$regionCode&contentTypeId=$contentTypeId"
    fun detailImage(contentId: String) =
        "$BASE/detailImage2?serviceKey=${enc(Keys.dataGoKr)}&MobileOS=ETC&MobileApp=TripBet&_type=json&contentId=$contentId"
}
data class Place(val title: String, val addr: String, val lng: Double, val lat: Double, val distM: Double?, val image: String?)

/** 한국환경공단 전기자동차 충전소 EvCharger (XML) — 가이드 v1.25. 조회 조건은 zcode(지역)·statId, 위치는 응답 lat/lng 로 거른다. */
object EvChargerApi {
    private const val BASE = "https://apis.data.go.kr/B552584/EvCharger"
    fun chargerInfo(zcode: String, rows: Int = 100) =
        "$BASE/getChargerInfo?serviceKey=${enc(Keys.dataGoKr)}&numOfRows=$rows&pageNo=1&dataType=XML&zcode=$zcode"
    fun chargerStatus(zcode: String, rows: Int = 100) =
        "$BASE/getChargerStatus?serviceKey=${enc(Keys.dataGoKr)}&numOfRows=$rows&pageNo=1&dataType=XML&zcode=$zcode"
}
data class Charger(val statId: String, val name: String, val lat: Double, val lng: Double, val available: Boolean?)

/** 한국도로공사 휴게소 푸드메뉴. 시연 키는 test(EX_API_KEY 기본값). */
object RestFoodApi {
    fun bestFood(rows: Int = 50, stdRestNm: String? = null) =
        "https://data.ex.co.kr/openapi/restinfo/restBestfoodList?key=${enc(Keys.ex)}&type=json&numOfRows=$rows&pageNo=1&bestfoodyn=Y" +
            (stdRestNm?.let { "&stdRestNm=${enc(it)}" } ?: "")
}
/** 벌칙 화면용: 다음 휴게소의 대표(bestfoodyn=Y)·추천 메뉴 + 가격 + 설명. */
data class RestFood(val restName: String, val foodName: String, val price: Int?, val best: Boolean, val recommended: Boolean, val note: String?, val materials: List<String> = emptyList())

