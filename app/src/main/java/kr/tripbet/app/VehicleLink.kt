package kr.tripbet.app

import android.content.Context
import android.os.Handler
import android.os.Looper
import ai.pleos.playground.vehicle.Vehicle
import ai.pleos.playground.vehicle.constant.control.VehicleGear
import ai.pleos.playground.vehicle.listener.GearSelectionListener
import ai.pleos.playground.navi.data.DestinationArrivedInfo
import ai.pleos.playground.navi.data.RouteStartInfo
import ai.pleos.playground.navi.data.WaypointArrivedInfo
import ai.pleos.playground.navi.helper.NaviHelper
import ai.pleos.playground.navi.helper.listener.NaviHelperEventListener

/**
 * 달린 뒤 실제 값. 내비·차량 SDK 이벤트가 들어오면 채워지고, 비어 있으면 화면은 시연용 값을 쓴다.
 * 주의: 아래 값의 단위·의미(duration 이 초인지, 예상인지 실제인지, 연비 항목이 구간 연비인지)는 아직 실제 주행으로 확인하지 못했다.
 */
object RideMeter {
    @Volatile var startDurationSec: Int? = null      // 출발 때 내비가 알려 준 예상 소요
    @Volatile var arrivalVsNaviMin: Int? = null      // +빨리, −늦게 (분)
    @Volatile var fuelEconomy: Float? = null         // 구간 연비(km/L)
    const val TANK_L = 50f                           // 탱크 용량 가정값(설정). 연료 잔량 % 차이를 리터로 바꿀 때 쓴다
}

/**
 * 차량 기어를 읽어 정차(P)·주행(그 밖) 상태를 DrivingGate 에 넣는다. Pleos Vehicle SDK 의 Gear API 사용.
 * 서비스가 없거나 실패하면 정차로 둔다(화면은 계속 쓸 수 있음). 모르는 기어(UNKNOWN)는 직전 상태 유지.
 */
class VehicleLink(private val ctx: Context, private val sink: ArrivalSink? = null) {
    private var vehicle: Vehicle? = null
    private var navi: NaviHelper? = null
    private val main = Handler(Looper.getMainLooper())

    private var wasDriving = false
    private var odo0: Float? = null
    private var pct0: Int? = null

    /** 출발(P→D) 때 누적 주행거리와 연료 잔량을 적어 두고, 멈출 때(D→P) 차이로 구간 연비를 계산한다. 안 되면 getCurrentFuelEconomy. */
    private fun trip(driving: Boolean) {
        if (driving == wasDriving) return
        wasDriving = driving
        val v = vehicle ?: return
        try {
            if (driving) {
                v.getOdometer().getTripCurrentDrivingDistance({ odo0 = it }, { })
                v.getFuel().getFuelPercent({ pct0 = it }, { })
            } else {
                v.getOdometer().getTripCurrentDrivingDistance({ d1 ->
                    v.getFuel().getFuelPercent({ p1 ->
                        val d0 = odo0; val p0 = pct0
                        val liters = if (p0 != null && p1 != null) (p0 - p1) / 100f * RideMeter.TANK_L else 0f
                        if (d0 != null && d1 != null && liters > 0.05f && d1 - d0 > 0.5f) RideMeter.fuelEconomy = (d1 - d0) / liters
                        else readFuel()
                    }, { readFuel() })
                }, { readFuel() })
            }
        } catch (e: Exception) { readFuel() }
    }

    private fun apply(g: VehicleGear?) {
        val next = when (g) {
            VehicleGear.GEAR_PARK -> DrivingState.PARKED
            null, VehicleGear.GEAR_UNKNOWN -> return
            else -> DrivingState.DRIVING
        }
        main.post { DrivingGate.state = next; trip(next == DrivingState.DRIVING) }
    }

    fun start() {
        try {
            val v = Vehicle(ctx).also { it.initialize() }
            vehicle = v
            val gear = v.getGear()
            gear.getCurrentGearSelectionState({ apply(it) }, { })
            gear.registerGearSelection(object : GearSelectionListener {
                override fun onSelected(gear: VehicleGear?) = apply(gear)
                override fun onFailed(e: Exception) { }
            })
        } catch (e: Exception) { /* 차량 서비스 없음: 정차 상태 유지 */ }
        startNavi()
    }

    private fun readFuel() {
        try { vehicle?.getFuel()?.getCurrentFuelEconomy({ RideMeter.fuelEconomy = it }, { }) } catch (e: Exception) { }
    }

    /** 내비 경로 시작·경유지/목적지 도착 이벤트로 내비 예상과 실제의 차이를 기록한다(자리: 단위 미확인). */
    private fun startNavi() {
        try {
            val n = NaviHelper(ctx); n.initialize(); navi = n
            n.addListener(object : NaviHelperEventListener {
                override fun onRouteStarted(info: RouteStartInfo) { RideMeter.startDurationSec = info.duration }
                override fun onWayPointArrived(info: WaypointArrivedInfo) { arrived(info.duration); sink?.onArrived(info.name ?: "") }
                override fun onDestinationArrived(info: DestinationArrivedInfo) { arrived(info.duration); sink?.onArrived(info.name ?: "") }
            })
        } catch (e: Exception) { /* 내비 서비스 없음: 시연용 값 사용 */ }
    }

    private fun arrived(durationSec: Int) {
        val start = RideMeter.startDurationSec
        if (start != null) RideMeter.arrivalVsNaviMin = (start - durationSec) / 60
        readFuel()
    }

    fun stop() { try { vehicle?.release() } catch (e: Exception) { }; try { navi?.release() } catch (e: Exception) { } }
}





