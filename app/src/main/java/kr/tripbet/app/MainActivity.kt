package kr.tripbet.app

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kr.tripbet.app.ui.TripBetApp
import kr.tripbet.app.ui.theme.TripBetTheme

class MainActivity : ComponentActivity() {
    private lateinit var link: VehicleLink
    private var locOk by mutableStateOf(false)
    private val askLoc = registerForActivityResult(ActivityResultContracts.RequestPermission()) { locOk = it }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        link = VehicleLink(applicationContext, ArrivalSink { ArrivalBus.sink?.onArrived(it) }).also { it.start() }
        locOk = checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        if (!locOk) askLoc.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        setContent { TripBetTheme { TripBetApp(locOk) } }
    }
    override fun onDestroy() { link.stop(); super.onDestroy() }
}

