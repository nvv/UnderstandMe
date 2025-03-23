package com.vnamashko.understandme

import androidx.lifecycle.viewModelScope
import com.vnamashko.understandme.settings.SettingsDataStore
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
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.firstOrNull
import javax.inject.Inject

@HiltViewModel
class ViewModel @Inject constructor(
    private val dataStore: SettingsDataStore,
    private val translator: Translator,
) : androidx.lifecycle.ViewModel() {

    val translatedText: StateFlow<String?> = translator.translatedText

    private val _sourceLanguage = MutableStateFlow<Language?>(null)
    val sourceLanguage = _sourceLanguage.asStateFlow()
    private val _targetLanguage = MutableStateFlow<Language?>(null)
    val targetLanguage = _targetLanguage.asStateFlow()

    val supportedLanguages = flowOf(translator.supportedLanguages).stateIn(
        viewModelScope,
        SharingStarted.Lazily,
        emptyList()
    )

    val downloadedLanguages = translator.downloadedModels

    fun translate(text: String) {
        translator.translate(text)
    }

    fun selectSourceLanguage(language: Language) {
        _sourceLanguage.value = language
        viewModelScope.launch {
            dataStore.saveSourceLanguage(language.code)
        }
    }

    fun selectTargetLanguage(language: Language) {
        _targetLanguage.value = language
        viewModelScope.launch {
            dataStore.saveTargetLanguage(language.code)
        }
    }

    init {
        viewModelScope.launch {
            _sourceLanguage.filterNotNull().collect {
                translator.setSourceLanguage(it.code)
            }
        }

        viewModelScope.launch {
            _targetLanguage.filterNotNull().collect {
                translator.setTargetLanguage(it.code)
            }
        }

        viewModelScope.launch {
            _sourceLanguage.value =
                translator.supportedLanguages.find { it.code == dataStore.sourceLanguage.firstOrNull() }
            _targetLanguage.value =
                translator.supportedLanguages.find { it.code == dataStore.targetLanguage.firstOrNull() }
        }
    }
}