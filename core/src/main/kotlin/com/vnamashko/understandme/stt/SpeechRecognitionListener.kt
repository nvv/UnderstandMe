package com.vnamashko.understandme.stt

import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.SpeechRecognizer
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow

class SpeechRecognitionListener: RecognitionListener {

    private val _result = MutableStateFlow<String?>(null)
    val result: StateFlow<String?> = _result.asStateFlow()

    private val _event = MutableSharedFlow<Event>(0)
    val event: SharedFlow<Event> = _event.asSharedFlow()

    override fun onReadyForSpeech(params: Bundle?) {
        _event.tryEmit(Event.SPEECH_STARTED)
    }

    override fun onBeginningOfSpeech() {
    }

    override fun onRmsChanged(rmsdB: Float) {
    }

    override fun onBufferReceived(buffer: ByteArray?) {
    }

    override fun onEndOfSpeech() {
        _event.tryEmit(Event.SPEECH_FINISHED)
    }

    override fun onError(error: Int) {
    }

    override fun onResults(results: Bundle?) {
        _result.value = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
            ?.joinToString(separator = ". ")
    }

    override fun onPartialResults(partialResults: Bundle?) {

    }

    override fun onEvent(eventType: Int, params: Bundle?) {
    }
}

enum class Event {
    SPEECH_STARTED, SPEECH_FINISHED
}