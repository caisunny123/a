package com.dsjl.discipline.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.nio.charset.StandardCharsets

/**
 * OpenAI 兼容的 /chat/completions 客户端（支持 DeepSeek、OpenAI、Moonshot 等）。
 * 失败时 fetchAdvice 返回 null（由调用方回退到内置提醒），test 返回可读的错误信息。
 */
object AiClient {

    private const val SYSTEM_PROMPT =
        "你是一位温和而坚定的自律教练。请根据用户昨日的任务完成情况，" +
        "给出简短（不超过150字）、具体、可执行的今日自律建议，语气积极鼓励，" +
        "不要用列表符号，直接给建议。"

    /** 获取今日建议；失败返回 null。 */
    suspend fun fetchAdvice(baseUrl: String, key: String, model: String, userContent: String): String? =
        chatOnce(baseUrl, key, model, SYSTEM_PROMPT, userContent, 300, 0.8)

    /** 通用单轮对话；失败返回 null（由调用方决定回退策略）。 */
    suspend fun chatOnce(
        baseUrl: String,
        key: String,
        model: String,
        system: String,
        user: String,
        maxTokens: Int,
        temperature: Double = 0.8
    ): String? = withContext(Dispatchers.IO) {
        try {
            chat(baseUrl, key, model, system, user, maxTokens, temperature)
        } catch (e: Exception) {
            null
        }
    }

    /** 连通性测试，返回可读结果（供设置页展示）。 */
    suspend fun test(baseUrl: String, key: String, model: String): String =
        withContext(Dispatchers.IO) {
            try {
                chat(baseUrl, key, model, "你是一个测试助手。", "请只回复：ok", 10, 0.0)
                "✅ 连接成功，模型响应正常"
            } catch (e: Exception) {
                "❌ 连接失败：" + (e.message ?: "未知错误").take(160)
            }
        }

    private fun chat(
        baseUrl: String,
        key: String,
        model: String,
        system: String,
        user: String,
        maxTokens: Int,
        temperature: Double
    ): String {
        val url = URL(baseUrl.trimEnd('/') + "/chat/completions")
        val conn = url.openConnection() as HttpURLConnection
        conn.requestMethod = "POST"
        conn.setRequestProperty("Content-Type", "application/json")
        if (key.isNotBlank()) conn.setRequestProperty("Authorization", "Bearer $key")
        conn.connectTimeout = 20_000
        conn.readTimeout = 45_000
        conn.doOutput = true

        val body = JSONObject().apply {
            put("model", model.ifBlank { "deepseek-chat" })
            put("max_tokens", maxTokens)
            put("temperature", temperature)
            put(
                "messages",
                JSONArray().apply {
                    put(JSONObject().put("role", "system").put("content", system))
                    put(JSONObject().put("role", "user").put("content", user))
                }
            )
        }.toString()

        conn.outputStream.use { it.write(body.toByteArray(StandardCharsets.UTF_8)) }

        val code = conn.responseCode
        if (code / 10 != 2) {
            val err = try {
                conn.errorStream?.bufferedReader(StandardCharsets.UTF_8)?.readText().orEmpty().take(200)
            } catch (e: Exception) {
                ""
            }
            throw Exception("HTTP $code $err".trim())
        }

        val resp = conn.inputStream.bufferedReader(StandardCharsets.UTF_8).readText()
        return JSONObject(resp)
            .getJSONArray("choices")
            .getJSONObject(0)
            .getJSONObject("message")
            .getString("content")
            .trim()
            .ifBlank { throw Exception("返回内容为空") }
    }
}
