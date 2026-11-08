package kr.tripbet.app

/** 받침에 따라 이/가, 으로/로, 이에요/예요를 고른다. */
object Josa {
    private fun lastIndex(s: String): Int {
        val c = s.trim().lastOrNull { it.isLetterOrDigit() } ?: return 0
        return when (c) {
            in '가'..'힣' -> (c - '가') % 28
            '0', '1', '3', '6', '7', '8' -> 1        // 영·일·삼·육·칠·팔은 받침이 있다
            else -> 0
        }
    }
    fun hasBatchim(s: String) = lastIndex(s) != 0
    fun iGa(s: String) = if (hasBatchim(s)) "이" else "가"
    fun euRo(s: String) = if (!hasBatchim(s) || lastIndex(s) == 8) "로" else "으로"   // 8 = ㄹ 받침
    fun eunNeun(s: String) = if (hasBatchim(s)) "은" else "는"
    fun eulReul(s: String) = if (hasBatchim(s)) "을" else "를"
    fun ieyo(s: String) = if (hasBatchim(s)) "이에요" else "예요"
}

