package com.vnamashko.understandme.tts

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale
import javax.inject.Inject
import android.os.Bundle
import android.speech.tts.TextToSpeech.Engine

interface Tts {
    val isReady: StateFlow<Boolean>
    val isSpeaking: StateFlow<Boolean>
    fun speak(text: String, languageCode: String?)
    fun stop()
    fun setSpeechRate(rate: Float)
}

class TtsImpl @Inject constructor(context: Context) : Tts {

    private val _isReady = MutableStateFlow(false)
    override val isReady: StateFlow<Boolean> = _isReady.asStateFlow()

    private val _isSpeaking = MutableStateFlow(false)
    override val isSpeaking: StateFlow<Boolean> = _isSpeaking.asStateFlow()

    private var textToSpeech: TextToSpeech? = null
    private var speechRate: Float = 1.0f
    private val pauseDuration = 500L // milliseconds pause between sentences

    init {
        textToSpeech = TextToSpeech(context) { status ->
            _isReady.value = status == TextToSpeech.SUCCESS
        }.apply {
            setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                override fun onStart(utteranceId: String?) {
                    _isSpeaking.value = true
                }
                
                override fun onDone(utteranceId: String?) {
                    _isSpeaking.value = false
                }
                
                override fun onError(utteranceId: String?) {
                    _isSpeaking.value = false
                }
            })
        }
    }

    override fun speak(text: String, languageCode: String?) {
        if (languageCode != null) {
            textToSpeech?.language = Locale(languageCode)
        }

        textToSpeech?.let { tts ->
            // Split text into sentences
            val sentences = text.split(Regex("[.!?]+\\s*"))
                .filter { it.isNotBlank() }
                .map { it.trim() }

            // Stop any ongoing speech
            stop()

            // Speak each sentence with a pause
            sentences.forEachIndexed { index, sentence ->
                val params = Bundle().apply {
                    putFloat(Engine.KEY_PARAM_VOLUME, 1.0f)
                }

                // Add silence after each sentence except the last one
                val addPause = index < sentences.size - 1

                tts.speak(
                    sentence,
                    if (index == 0) TextToSpeech.QUEUE_FLUSH else TextToSpeech.QUEUE_ADD,
                    params,
                    "utteranceId_$index"
                )

                if (addPause) {
                    tts.playSilentUtterance(pauseDuration, TextToSpeech.QUEUE_ADD, "utteranceId_${index}_$index")
                }
            }
        }
    }

    override fun stop() {
        textToSpeech?.stop()
        _isSpeaking.value = false
    }

    override fun setSpeechRate(rate: Float) {
        speechRate = rate.coerceIn(0.1f, 2.0f)
    }
}

