package com.example.hebrewassistant

/** Structured result of understanding a spoken command. */
sealed interface Action {
    data class SetTimer(val seconds: Int, val label: String?) : Action
    data class SetAlarm(val hour: Int, val minute: Int, val label: String?) : Action
    data class Call(val contactOrNumber: String) : Action
    data class SendSms(val recipient: String, val message: String) : Action
    data class Navigate(val destination: String) : Action
    data class WebSearch(val query: String) : Action
    /** Anything the app can't do natively; handed to the Google app / Gemini as English text. */
    data class Fallback(val englishText: String) : Action
}
