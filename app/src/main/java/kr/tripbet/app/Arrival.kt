package kr.tripbet.app

import android.location.Location
import kr.tripbet.app.data.RestStop
import kr.tripbet.app.data.distanceM

/** 경유지·목적지에 도착했을 때 알리는 곳. 도착 신호 공급자가 이쪽으로 알린다. */
fun interface ArrivalSink { fun onArrived(name: String) }

/** 실제 공급자(SDK)가 앱 화면 쪽에 도착을 알리는 통로. 화면이 열리면 TripBetApp 이 채운다. */
object ArrivalBus { @Volatile var sink: ArrivalSink? = null }

/**
 * 도착 신호 공급자는 둘이다.
 *  - 실제 공급자: Pleos 내비 SDK 이벤트(onWayPointArrived / onDestinationArrived) → VehicleLink 가 ArrivalSink 로 알린다.
 *  - 시연 공급자(이 파일): 에뮬레이터에 내비 경로·도착 이벤트를 만들 방법이 없어, 위치(GPX 재생)가 고른 휴게소 반경에 들어오면 '도착'을 낸다.
 * DEMO: 시연 공급자가 낸 도착은 가상 차량 시연이다. 실제 차량에서는 SDK 공급자만 쓴다.
 */
class DemoNaviProvider(private val radiusM: Double = 700.0, private val sink: ArrivalSink) {
    private val arrived = HashSet<Int>()

    /** @param stops 사용자가 고른 경유지(좌표가 있는 것만 본다). 달리는 중일 때만 호출한다. */
    fun onLocation(loc: Location, stops: List<RestStop>) {
        for (s in stops) {
            val la = s.lat ?: continue
            val lo = s.lng ?: continue
            if (s.order in arrived) continue
            if (distanceM(loc.latitude, loc.longitude, la, lo) <= radiusM) { arrived.add(s.order); sink.onArrived(s.name) }
        }
    }

    fun reset() = arrived.clear()
}
