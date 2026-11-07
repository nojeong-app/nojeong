package kr.tripbet.app

/** 키는 local.properties 에서만 읽는다(저장소 제외). 비어 있으면 호출하지 않는다. */
object Keys {
    val dataGoKr: String = BuildConfig.DATA_GO_KR_KEY
    val ex: String = BuildConfig.EX_API_KEY
}
