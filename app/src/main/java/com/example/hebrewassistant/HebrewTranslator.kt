package com.example.hebrewassistant

import com.google.mlkit.nl.translate.TranslateLanguage
import com.google.mlkit.nl.translate.Translation
import com.google.mlkit.nl.translate.TranslatorOptions
import com.google.mlkit.common.model.DownloadConditions
import kotlinx.coroutines.tasks.await

/** On-device Hebrew -> English translation (model ~30MB, downloaded on first use). */
class HebrewTranslator {
    private val translator = Translation.getClient(
        TranslatorOptions.Builder()
            .setSourceLanguage(TranslateLanguage.HEBREW)
            .setTargetLanguage(TranslateLanguage.ENGLISH)
            .build()
    )

    suspend fun toEnglish(hebrew: String): String {
        translator.downloadModelIfNeeded(DownloadConditions.Builder().build()).await()
        return translator.translate(hebrew).await()
    }

    fun close() = translator.close()
}
