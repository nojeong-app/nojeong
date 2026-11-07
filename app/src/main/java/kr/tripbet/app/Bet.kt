package kr.tripbet.app

/** 내기 종류. 정답은 차 데이터가 판정한다. */
enum class BetKind(val label: String, val source: String) {
    ARRIVAL_TIME("도착 시각", "NaviHelper requestTrip(eta) / getDestinationInfo(remainTime)"),
    TOLL("통행료", "NaviHelper getDestinationInfo(tollFare)"),
    FUEL_ECONOMY("연비", "Vehicle Fuel(getFuelPercent) · Odometer(getTripCurrentDrivingDistance)"),
    TIME_TO_STOP("다음 휴게소까지 시간", "NaviHelper onWayPointArrived 시각 - 출발 시각"),
}

/** 내기는 정차 중(출발 전·휴게소)에만 건다. 주행 중엔 기록만. */
data class Bet(val kind: BetKind, val bettor: String, val guess: String)

/** 결과 안내 자리: 경유지·목적지 도착(onWayPointArrived/onDestinationArrived) 때 TTS(ON_DEVICE)로 읽는다. */
interface ResultAnnouncer { fun announce(text: String) }

/** 경유지는 최대 2곳(WaypointIndex.FIRST/SECOND). */
const val MAX_WAYPOINTS = 2
