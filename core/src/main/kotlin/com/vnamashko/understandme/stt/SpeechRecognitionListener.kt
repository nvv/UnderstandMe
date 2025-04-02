package com.vnamashko.understandme.stt

import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.SpeechRecognizer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class SpeechRecognitionListener: RecognitionListener {

    private val _result = MutableStateFlow<String?>(null)
    val result: StateFlow<String?> = _result.asStateFlow()

    private val _isListening = MutableStateFlow(false)
    val isListening: StateFlow<Boolean> = _isListening.asStateFlow()

    override fun onReadyForSpeech(params: Bundle?) {
        _isListening.value = true
    }

    override fun onBeginningOfSpeech() {
    }

    override fun onRmsChanged(rmsdB: Float) {
    }

    override fun onBufferReceived(buffer: ByteArray?) {
    }

    override fun onEndOfSpeech() {
        _isListening.value = false
    }

    override fun onError(error: Int) {
    }

    override fun onResults(results: Bundle?) {
        _isListening.value = false
        _result.value = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
            ?.joinToString(separator = ". ")
    }

    override fun onPartialResults(partialResults: Bundle?) {

    }

    override fun onEvent(eventType: Int, params: Bundle?) {
    }
}
