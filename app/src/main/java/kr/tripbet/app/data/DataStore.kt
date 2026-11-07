package kr.tripbet.app.data

import android.content.Context
import kr.tripbet.app.BuildConfig
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

/**
 * 데이터 읽기 한 곳. 규칙:
 *  - 제출용(release): 네트워크 없이 assets 의 demo 폴더 json 파일만 읽는다(키가 APK에 없음).
 *  - 개발용(debug): 디스크 캐시 → 없을 때만 실시간 호출(공용 키 보호를 위해 호출을 아끼고, 성공 응답을 저장해 재사용).
 */
class DataStore(private val ctx: Context) {
    fun demo(name: String): String = ctx.assets.open("demo/$name").bufferedReader().use { it.readText() }

    /** cacheName 예: "places_seoul.json". 개발용 빌드에서만 실시간 호출. 실패하면 null. */
    fun cachedOrFetch(cacheName: String, url: String, ttlHours: Long = 24): String? {
        val f = File(ctx.cacheDir, "api/$cacheName")
        if (f.exists() && System.currentTimeMillis() - f.lastModified() < ttlHours * 3_600_000) return f.readText()
        if (!BuildConfig.LIVE_API) return null
        return try {
            val c = URL(url).openConnection() as HttpURLConnection
            c.setRequestProperty("User-Agent", USER_AGENT)
            c.connectTimeout = 10_000; c.readTimeout = 30_000
            val body = c.inputStream.bufferedReader().use { it.readText() }
            f.parentFile?.mkdirs(); f.writeText(body); body
        } catch (e: Exception) { null }
    }
}

