package kr.tripbet.app

import android.content.Context
import org.json.JSONObject

/**
 * 질문 은행(assets/questions.json). 종류마다 문장 틀 2~3개를 돌려 쓰고, 정답이 문장 안에 드러나는 틀은 건너뛴다(답 노출 점검).
 * 출제 규칙(같은 휴게소 음식 질문 하나, 카드에 보이는 정보와 겹치는 질문 제외)은 questions.json 머리말과 화면 코드에 있다.
 */
class QuestionBank(ctx: Context) {
    private val root = JSONObject(ctx.assets.open("questions.json").bufferedReader().use { it.readText() })

    fun frames(kind: String): List<String> {
        val a = root.optJSONObject(kind)?.optJSONArray("frames") ?: return emptyList()
        return List(a.length()) { a.getString(it) }
    }

    /** @param seed 틀을 돌려 쓰는 기준 값  @param correct 정답 글자(문장에 들어 있으면 그 틀은 쓰지 않음) */
    fun question(kind: String, seed: Int, vars: Map<String, String>, correct: String): String? {
        val fs = frames(kind)
        if (fs.isEmpty()) return null
        for (off in fs.indices) {
            val f = fs[(seed + off) % fs.size]
            val q = vars.entries.fold(f) { s, (k, v) -> s.replace("{$k}", v) }
            if (!leaks(q, correct)) return q
        }
        return null
    }

    companion object {
        /** 답 노출 점검: 질문 문장에 정답 글자가 그대로 들어 있으면 true. */
        fun leaks(question: String, correct: String) = correct.length >= 2 && question.contains(correct)
    }
}
