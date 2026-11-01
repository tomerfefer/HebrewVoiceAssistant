package com.example.hebrewassistant

import android.app.SearchManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.AlarmClock

/** Turns an [Action] into a standard Android intent. Returns a short human-readable description. */
class ActionExecutor(private val context: Context) {

    fun execute(action: Action): String {
        val (intent, description) = when (action) {
            is Action.SetTimer -> Intent(AlarmClock.ACTION_SET_TIMER).apply {
                putExtra(AlarmClock.EXTRA_LENGTH, action.seconds)
                action.label?.let { putExtra(AlarmClock.EXTRA_MESSAGE, it) }
                putExtra(AlarmClock.EXTRA_SKIP_UI, false)
            } to "Timer for ${action.seconds}s"

            is Action.SetAlarm -> Intent(AlarmClock.ACTION_SET_ALARM).apply {
                putExtra(AlarmClock.EXTRA_HOUR, action.hour)
                putExtra(AlarmClock.EXTRA_MINUTES, action.minute)
                action.label?.let { putExtra(AlarmClock.EXTRA_MESSAGE, it) }
            } to "Alarm at %02d:%02d".format(action.hour, action.minute)

            // ACTION_DIAL opens the dialer without needing CALL_PHONE; contact-name lookup is a TODO.
            is Action.Call -> Intent(Intent.ACTION_DIAL, Uri.parse("tel:${Uri.encode(action.contactOrNumber)}")) to
                "Dialing ${action.contactOrNumber}"

            is Action.SendSms -> Intent(Intent.ACTION_SENDTO, Uri.parse("smsto:${Uri.encode(action.recipient)}")).apply {
                putExtra("sms_body", action.message)
            } to "SMS to ${action.recipient}"

            is Action.Navigate -> Intent(Intent.ACTION_VIEW,
                Uri.parse("google.navigation:q=${Uri.encode(action.destination)}")) to "Navigating to ${action.destination}"

            is Action.WebSearch -> Intent(Intent.ACTION_WEB_SEARCH).apply {
                putExtra(SearchManager.QUERY, action.query)
            } to "Searching: ${action.query}"

            // Best-effort handoff: the Google app/Gemini handles the English query as a search.
            is Action.Fallback -> Intent(Intent.ACTION_WEB_SEARCH).apply {
                putExtra(SearchManager.QUERY, action.englishText)
            } to "Handed off: ${action.englishText}"
        }
        return try {
            context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            description
        } catch (e: android.content.ActivityNotFoundException) {
            "No app can handle: $description"
        }
    }
}
