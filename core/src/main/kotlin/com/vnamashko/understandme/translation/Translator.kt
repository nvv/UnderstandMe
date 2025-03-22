package com.vnamashko.understandme.translation

import com.google.android.gms.tasks.Tasks
import com.google.mlkit.nl.translate.TranslateLanguage
import com.google.mlkit.nl.translate.Translation
import com.google.mlkit.nl.translate.TranslatorOptions
import com.vnamashko.understandme.translation.model.Language
import com.vnamashko.understandme.translation.model.State
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.util.Locale
import javax.inject.Inject

interface Translator {
    val state: StateFlow<State>

    val supportedLanguages: List<Language>

    suspend fun translate(text: String): String

    fun setSourceLanguage(language: Language)

    fun setTargetLanguage(language: Language)
}

class TranslatorImpl @Inject constructor(): Translator {

    override val state: StateFlow<State> = MutableStateFlow(State.IDLE)

    private val sourceLanguage = MutableStateFlow(TranslateLanguage.ENGLISH)
    private val targetLanguage = MutableStateFlow(TranslateLanguage.SPANISH)

    override val supportedLanguages: List<Language> by lazy {
        TranslateLanguage.getAllLanguages().map { code ->
            Language(code = code, displayName = Locale(code).displayName)
        }
    }

    private val options = TranslatorOptions.Builder()
        .setSourceLanguage(TranslateLanguage.ENGLISH)
        .setTargetLanguage(TranslateLanguage.SPANISH)
        .build()
    private val translator = Translation.getClient(options)

    override suspend fun translate(text: String): String {
        Tasks.await(translator.downloadModelIfNeeded())

        return Tasks.await(translator.translate(text))
    }

    override fun setSourceLanguage(language: Language) {
        sourceLanguage.value = language.code
    }

    override fun setTargetLanguage(language: Language) {
        targetLanguage.value = language.code
    }
}