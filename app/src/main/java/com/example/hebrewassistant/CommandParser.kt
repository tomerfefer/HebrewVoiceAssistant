package com.example.hebrewassistant

import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Sends the Hebrew transcript (plus its English translation as a hint) to Claude and
 * gets back a structured [Action] via forced tool use.
 */
class CommandParser(
    private val apiKey: String = BuildConfig.ANTHROPIC_API_KEY,
    private val model: String = BuildConfig.CLAUDE_MODEL,
    private val http: OkHttpClient = OkHttpClient(),
) {
    private val tool = JSONObject("""
    {
      "name": "execute_action",
      "description": "Choose the single phone action that fulfils the user's spoken command.",
      "input_schema": {
        "type": "object",
        "properties": {
          "type": {"type": "string", "enum": ["set_timer","set_alarm","call","send_sms","navigate","web_search","fallback"]},
          "seconds": {"type": "integer", "description": "set_timer: duration in seconds"},
          "hour": {"type": "integer", "description": "set_alarm: 0-23"},
          "minute": {"type": "integer", "description": "set_alarm: 0-59"},
          "label": {"type": "string"},
          "contact_or_number": {"type": "string", "description": "call: name as spoken (keep Hebrew) or phone number"},
          "recipient": {"type": "string"},
          "message": {"type": "string", "description": "send_sms: message body in the language the user dictated it"},
          "destination": {"type": "string"},
          "query": {"type": "string"},
          "english_text": {"type": "string", "description": "fallback: the command rewritten in English"}
        },
        "required": ["type"]
      }
    }""")

    suspend fun parse(hebrew: String, englishHint: String): Action = withContext(Dispatchers.IO) {
        require(apiKey.isNotBlank()) { "ANTHROPIC_API_KEY missing in local.properties" }
        val body = JSONObject()
            .put("model", model)
            .put("max_tokens", 300)
            .put("system", "You convert spoken Hebrew voice commands for an Android phone into one action. " +
                "The machine translation is only a hint and may be wrong; trust the Hebrew. " +
                "If no native action fits, use fallback with a clean English rewrite.")
            .put("tools", JSONArray().put(tool))
            .put("tool_choice", JSONObject().put("type", "tool").put("name", "execute_action"))
            .put("messages", JSONArray().put(JSONObject()
                .put("role", "user")
                .put("content", "Hebrew: $hebrew\nMachine translation: $englishHint")))

        val req = Request.Builder()
            .url("https://api.anthropic.com/v1/messages")
            .header("x-api-key", apiKey)
            .header("anthropic-version", "2023-06-01")
            .post(body.toString().toRequestBody("application/json".toMediaType()))
            .build()

        http.newCall(req).execute().use { resp ->
            val text = resp.body?.string().orEmpty()
            check(resp.isSuccessful) { "Claude API ${resp.code}: $text" }
            val content = JSONObject(text).getJSONArray("content")
            for (i in 0 until content.length()) {
                val block = content.getJSONObject(i)
                if (block.optString("type") == "tool_use") return@withContext toAction(block.getJSONObject("input"), englishHint)
            }
            Action.Fallback(englishHint)
        }
    }

    private fun toAction(j: JSONObject, englishHint: String): Action = when (j.optString("type")) {
        "set_timer" -> Action.SetTimer(j.optInt("seconds", 60), j.optString("label").ifBlank { null })
        "set_alarm" -> Action.SetAlarm(j.optInt("hour"), j.optInt("minute"), j.optString("label").ifBlank { null })
        "call" -> Action.Call(j.optString("contact_or_number"))
        "send_sms" -> Action.SendSms(j.optString("recipient"), j.optString("message"))
        "navigate" -> Action.Navigate(j.optString("destination"))
        "web_search" -> Action.WebSearch(j.optString("query"))
        else -> Action.Fallback(j.optString("english_text").ifBlank { englishHint })
    }
}
