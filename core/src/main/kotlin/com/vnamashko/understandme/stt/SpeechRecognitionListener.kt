package com.vnamashko.understandme.stt

import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.SpeechRecognizer
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.filterNot

class SpeechRecognitionListener: RecognitionListener {

    private val _result = MutableStateFlow<RecognitionResult?>(null)
    val result: StateFlow<RecognitionResult?> = _result.asStateFlow()

    private val _partialResult = MutableStateFlow<String?>(null)
    val partialResult: Flow<String?> = _partialResult
        .filterNot { it?.isEmpty() == true }
        .debounce(100)

    override fun onReadyForSpeech(params: Bundle?) {
        _result.value = RecognitionResult.Listening
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
        _result.value =
            RecognitionResult.Finished(
                results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    .takeIf { it?.isNotEmpty() == true }?.get(0)
            )
    }

    override fun onPartialResults(partialResults: Bundle?) {
        _partialResult.value = null
        _partialResult.value =
            partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                .takeIf { it?.isNotEmpty() == true }?.get(0)
    }

    override fun onEvent(eventType: Int, params: Bundle?) {
    }

    fun reset() {
        _partialResult.value = null
        _result.value = null
    }
}

sealed interface RecognitionResult {
    data object Listening: RecognitionResult
    data class Finished(val text: String?): RecognitionResult
}