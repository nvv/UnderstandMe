package com.vnamashko.understandme.stt

import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.SpeechRecognizer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class SpeechRecognitionListener: RecognitionListener {

    private val _result = MutableStateFlow<RecognitionResult?>(null)
    val result: StateFlow<RecognitionResult?> = _result.asStateFlow()

    override fun onReadyForSpeech(params: Bundle?) {
        _result.tryEmit(RecognitionResult.Listening)
    }

    override fun onBeginningOfSpeech() {
    }

    override fun onRmsChanged(rmsdB: Float) {
    }

    override fun onBufferReceived(buffer: ByteArray?) {
    }

    override fun onEndOfSpeech() {
    }

    override fun onError(error: Int) {
        _result.tryEmit(RecognitionResult.Finished(null))
    }

    override fun onResults(results: Bundle?) {
        _result.tryEmit(
            RecognitionResult.Finished(
                results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    .takeIf { it?.isNotEmpty() == true }?.get(0)
            )
        )
    }

    override fun onPartialResults(partialResults: Bundle?) {

    }

    override fun onEvent(eventType: Int, params: Bundle?) {
    }
}

sealed interface RecognitionResult {
    data object Listening: RecognitionResult
    data class Finished(val text: String?): RecognitionResult
}