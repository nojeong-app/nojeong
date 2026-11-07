package kr.tripbet.app

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Handler
import android.os.Looper
import android.speech.tts.TextToSpeech
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kr.tripbet.app.data.QuizData
import kr.tripbet.app.data.QuizItem
import kr.tripbet.app.data.QuizPlace
import kr.tripbet.app.data.distanceM
import android.util.Log
import ai.pleos.playground.tts.TextToSpeech as PleosTts
import ai.pleos.playground.tts.constant.ErrorType
import ai.pleos.playground.tts.constant.Mode
import ai.pleos.playground.tts.listener.OnInitializeListener
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * 한 번의 퀴즈: 가장 가까운 명소 하나의 문제(1~2개)만 낸다. 다른 명소 문제는 섞지 않는다.
 * 규칙: 그 명소 문제를 모두 맞혀야 그 명소 시·군 상품권, 운전자는 점수에서 빠진다(답은 동승자 음성만 받는다).
 */
class QuizSession(val place: QuizPlace, val items: List<QuizItem>, val distKm: Double) {
    val picks = mutableStateListOf<Int?>().also { l -> repeat(items.size) { l.add(null) } }
    var asking by mutableStateOf(0)            // 지금 묻는 문제
    var finished by mutableStateOf(false)      // 달리는 중 출제가 끝남
    val fixed = mutableStateListOf<Boolean>().also { l -> repeat(items.size) { l.add(false) } }   // 고치기는 한 번
    var confirmAt by mutableStateOf(0)         // 정차 뒤 확인 중인 문제
    var fixing by mutableStateOf(false)
    var confirmed by mutableStateOf(false)     // 모든 확인이 끝남
    val correctCount get() = items.indices.count { picks[it] == items[it].answer }
    val passed get() = correctCount == items.size
}

/**
 * 주행 중 위치로 명소 근접을 판정해 출제한다.
 *  - 위치: 안드로이드 위치(GPS). Pleos FusedLocation SDK 는 신청 뒤 교체(자리).
 *  - 말하기: 안드로이드 기본 TTS. Pleos TextToSpeech(ON_DEVICE) 로 교체(자리).
 *  - 답: 음성 인식 자리. 시연은 방송 신호(kr.tripbet.ANSWER, choice=보기 번호)로 동승자 답을 넣는다.
 *  - 한 명소는 하루 한 번만 낸다(기록은 SharedPreferences).
 */
class QuizDriver(private val ctx: Context, private val data: QuizData, private val onCaption: (String) -> Unit, private val onLocation: (Location) -> Unit = {}, private val onEvent: (QuizSession) -> Unit) {
    private val lm = ctx.getSystemService(Context.LOCATION_SERVICE) as LocationManager
    private val prefs = ctx.getSharedPreferences("quiz", Context.MODE_PRIVATE)
    private val main = Handler(Looper.getMainLooper())
    private var tts: TextToSpeech? = null
    private var pleos: PleosTts? = null      // Pleos TextToSpeech(ON_DEVICE). 준비되면 이것으로 말하고, 아니면 안드로이드 기본 TTS
    @Volatile private var pleosReady = false
    var session: QuizSession? = null
    var enabled = false
    var driving = false
    var triggerKm = 7.0
    var answerSeconds = 30L

    private val listener = LocationListener { loc -> onLocation(loc); check(loc) }
    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(c: Context, i: Intent) {
            when (i.action) {
                "kr.tripbet.ANSWER" -> answer(i.getIntExtra("choice", 0) - 1)
                "kr.tripbet.RESET" -> prefs.edit().clear().apply()
                "kr.tripbet.ALERT" -> { val m = i.getIntExtra("min", 3); speak(ctx.getString(R.string.tts_alert, m), if (m == 3) listOf("alert3") else emptyList()) }
            }
        }
    }
    private val timeout = Runnable { advance() }

    private fun today() = SimpleDateFormat("yyyyMMdd", Locale.KOREA).format(Date())
    private fun key(place: String) = "${today()}:$place"
    fun alreadyToday(place: String) = prefs.getBoolean(key(place), false)
    fun markToday(place: String) = prefs.edit().putBoolean(key(place), true).apply()

    @SuppressLint("MissingPermission")
    fun start() {
        try {
            val p = PleosTts(ctx, Mode.ON_DEVICE)
            p.setLocale(Locale.KOREAN)
            // ON_DEVICE: 서버 연결·앱 등록 불필요. 11/5 Pleos 에뮬레이터에서는 초기화 응답(onSuccess/onFailure)이 오지 않아 안드로이드 기본 TTS 로 넘어간다.
            p.initialize(object : OnInitializeListener {
                override fun onSuccess() { try { p.setLocale(Locale.KOREAN) } catch (e: Exception) { }; pleos = p; pleosReady = true; Log.i("TripBet", "Pleos TTS ON_DEVICE 준비됨") }
                override fun onFailure(e: ErrorType) { Log.w("TripBet", "Pleos TTS 실패: $e") }
            })
        } catch (e: Exception) { Log.w("TripBet", "Pleos TTS 사용 불가: ${e.message}") }
        tts = TextToSpeech(ctx) { st -> if (st == TextToSpeech.SUCCESS) tts?.language = Locale.KOREAN }
        try { lm.requestLocationUpdates(LocationManager.GPS_PROVIDER, 2000L, 0f, listener) } catch (e: Exception) { }
        val f = IntentFilter().apply { addAction("kr.tripbet.ANSWER"); addAction("kr.tripbet.RESET"); addAction("kr.tripbet.ALERT") }
        if (android.os.Build.VERSION.SDK_INT >= 33) ctx.registerReceiver(receiver, f, Context.RECEIVER_EXPORTED) else ctx.registerReceiver(receiver, f)
    }

    fun stop() {
        main.removeCallbacks(timeout)
        try { lm.removeUpdates(listener) } catch (e: Exception) { }
        try { ctx.unregisterReceiver(receiver) } catch (e: Exception) { }
        tts?.shutdown()
        try { pleos?.release() } catch (e: Exception) { }
        try { player?.release() } catch (e: Exception) { }
    }

    // ── 미리 만든 음성 파일(assets/tts, PC 에서 sherpa-onnx Supertonic 3 로 합성한 기계 음성) 재생 ──
    private var player: android.media.MediaPlayer? = null
    private fun clipExists(key: String) = try { ctx.assets.openFd("tts/$key.m4a").close(); true } catch (e: Exception) { false }

    /** 파일 키 목록을 차례로 재생한다. 하나라도 없으면 false(다른 방법으로 넘어간다). */
    private fun playClips(keys: List<String>): Boolean {
        if (keys.isEmpty() || !keys.all { clipExists(it) }) return false
        try { player?.release() } catch (e: Exception) { }
        fun next(i: Int) {
            if (i >= keys.size) return
            val fd = ctx.assets.openFd("tts/${keys[i]}.m4a")
            val mp = android.media.MediaPlayer()
            mp.setAudioAttributes(android.media.AudioAttributes.Builder().setUsage(android.media.AudioAttributes.USAGE_MEDIA).setContentType(android.media.AudioAttributes.CONTENT_TYPE_SPEECH).build())
            mp.setDataSource(fd.fileDescriptor, fd.startOffset, fd.length); fd.close()
            mp.setOnCompletionListener { it.release(); next(i + 1) }
            mp.setOnErrorListener { m, _, _ -> m.release(); true }
            mp.prepare(); mp.start(); player = mp
            Log.i("TripBet", "CLIP ${keys[i]}")      // 시연 영상에 소리를 입힐 때 시작 시각을 읽는다
        }
        return try { next(0); true } catch (e: Exception) { Log.w("TripBet", "음성 파일 재생 실패: ${e.message}"); false }
    }

    /**
     * 말하고, 같은 문장을 화면 위쪽에 자막으로도 보인다. 순서: Pleos TTS(준비됐으면) → 미리 만든 음성 파일(clips) → 안드로이드 기본 TTS.
     * @param clips 이 문장에 해당하는 음성 파일 키(없으면 빈 목록)
     */
    fun speak(text: String, clips: List<String> = emptyList()) {
        onCaption(text)
        if (pleosReady) { try { pleos?.speak(text, false); return } catch (e: Exception) { Log.w("TripBet", "Pleos 말하기 실패: ${e.message}") } }
        if (playClips(clips)) return
        tts?.speak(text, TextToSpeech.QUEUE_ADD, null, "tb")
    }

    private fun check(loc: Location) {
        if (!enabled || !driving || session != null) return
        val near = data.places.filter { !alreadyToday(it.name) }
            .map { it to distanceM(loc.latitude, loc.longitude, it.lat, it.lng) / 1000.0 }
            .filter { it.second <= triggerKm }.minByOrNull { it.second } ?: return
        val s = QuizSession(near.first, near.first.quiz, near.second)
        session = s
        markToday(near.first.name)
        onEvent(s)
        ask(s)
    }

    private fun ask(s: QuizSession) {
        val q = s.items[s.asking]
        val choices = q.choices.mapIndexed { i, c -> ctx.getString(R.string.tts_choice, i + 1, c) }.joinToString(", ")
        val pi = data.places.indexOf(s.place)
        val keys = if (pi < 0) emptyList() else if (s.asking == 0) listOf("intro_$pi", "q_${pi}_0", "ask_outro") else listOf("q_${pi}_${s.asking}")
        speak(
            if (s.asking == 0) ctx.getString(R.string.tts_quiz_intro, s.place.name, Josa.iGa(s.place.name), q.question, choices)
            else "${q.question} $choices.",
            keys
        )
        main.removeCallbacks(timeout); main.postDelayed(timeout, answerSeconds * 1000)
    }

    /** 동승자 답을 받는다(보기 번호 0부터). 답할 시간이 지나면 다음 문제로 넘어간다. */
    fun answer(idx: Int) {
        val s = session ?: return
        if (s.finished || idx < 0 || idx >= s.items[s.asking].choices.size) return
        s.picks[s.asking] = idx
        advance()
    }

    private fun advance() {
        val s = session ?: return
        if (s.finished) return
        main.removeCallbacks(timeout)
        if (s.asking + 1 < s.items.size) { s.asking += 1; ask(s) } else s.finished = true
    }

    /** 정차 뒤 결과까지 끝나면 호출. 다음 명소를 낼 수 있게 한다. */
    fun clear() { session = null }
}





