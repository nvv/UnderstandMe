package com.vnamashko.understandme

import androidx.lifecycle.viewModelScope
import com.vnamashko.understandme.translation.Translator
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import com.vnamashko.understandme.translation.model.Language
import javax.inject.Inject

@HiltViewModel
class ViewModel @Inject constructor(
    private val translator: Translator
) : androidx.lifecycle.ViewModel() {

    private val _counter = MutableStateFlow<String?>(null)
    val counter : StateFlow<String?> = _counter.asStateFlow()

    private val _sourceLanguage = MutableStateFlow(translator.supportedLanguages.find { it.code == "en" })
    val sourceLanguage = _sourceLanguage.asStateFlow()
    private val _targetLanguage = MutableStateFlow(translator.supportedLanguages.find { it.code == "es" })
    val targetLanguage = _targetLanguage.asStateFlow()

    val supportedLanguages = flowOf(translator.supportedLanguages).stateIn(
        viewModelScope,
        SharingStarted.Lazily,
        emptyList()
    )

    fun translate(text: String) {
        viewModelScope.launch(Dispatchers.Default) {
            _counter.value = translator.translate(text).takeIf { it.isNotEmpty() }
        }
    }

    fun selectSourceLanguage(language: Language) {
        _sourceLanguage.value = language
    }

    fun selectTargetLanguage(language: Language) {
        _targetLanguage.value = language
    }
}