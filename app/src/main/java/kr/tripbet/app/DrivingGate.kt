package kr.tripbet.app

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/** 주행 상태. 실제 구현은 차량 신호(기어·CarUxRestrictions)로 바꾼다 — VehicleLink 가 값을 넣는다. */
enum class DrivingState { PARKED, DRIVING }

object DrivingGate {
    var state by mutableStateOf(DrivingState.PARKED)
}
