package com.vnamashko.understandme

import android.content.res.Resources
import androidx.lifecycle.viewModelScope
import com.vnamashko.understandme.network.NetworkConnectionManager
import com.vnamashko.understandme.settings.SettingsDataStore
import com.vnamashko.understandme.translation.Translator
import com.vnamashko.understandme.translation.model.Event
import com.vnamashko.understandme.translation.model.Language
import com.vnamashko.understandme.translation.model.LanguageModel
import com.vnamashko.understandme.tts.Tts
import com.vnamashko.undertsndme.translation.screen.TranslationError
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ViewModel @Inject constructor(
    private val dataStore: SettingsDataStore,
    private val networkConnectionManager: NetworkConnectionManager,
    private val translator: Translator,
    private val tts: Tts,
    private val resources: Resources
) : androidx.lifecycle.ViewModel() {

    private val _effect = MutableSharedFlow<UiEffect>()
    val effect = _effect.asSharedFlow()

    private val _originalText: MutableStateFlow<String> = MutableStateFlow("")
    val originalText: StateFlow<String> = _originalText.asStateFlow()
    val translatedText: StateFlow<String?> = translator.translatedText

    private val _sourceLanguage = MutableStateFlow<Language?>(null)
    val sourceLanguage = _sourceLanguage.asStateFlow()
    private val _targetLanguage = MutableStateFlow<Language?>(null)
    val targetLanguage = _targetLanguage.asStateFlow()

    val proposedSourceLanguage: StateFlow<Language?> = combine(
        translator.detectedLanguage,
        _sourceLanguage.map { it?.code }
    ) { detected, selected ->
        if (detected != "und" && detected != selected) translator.supportedLanguages.find { it.code == detected } else null
    }.stateIn(viewModelScope, SharingStarted.Lazily, null)

    val supportedModels =
        combine(
            translator.downloadedModels,
            translator.downloadingModels
        ) { downloaded, downloading ->
            translator.supportedLanguages.map {
                LanguageModel(
                    language = it,
                    isDownloaded = downloaded.contains(it.code),
                    isDownloading = downloading.contains(it.code)
                )
            }.toImmutableList()
        }.stateIn(
            viewModelScope,
            SharingStarted.Lazily,
            persistentListOf()
        )

    fun translate(text: String) {
        _originalText.value = text
        translator.translate(text)
    }

    fun selectProposedLanguage() {
        selectSourceLanguage(proposedSourceLanguage.value ?: return)
    }

    fun deleteModelForLanguage(language: Language) {
        translator.deleteModel(language.code)
    }

    fun downloadModelForLanguage(language: Language) {
        translator.downloadModel(language.code)
    }

    private fun retryTranslate() {
        viewModelScope.launch {
            _effect.emit(UiEffect.ClearError)
        }
        translator.retryTranslate()
    }

    fun playbackOriginal() {
        tts.speak(_originalText.value, _sourceLanguage.value?.code)
    }

    fun playbackTranslated() {
        if (tts.isLanguageAvailable(_targetLanguage.value?.code ?: "")) {
            tts.speak(translatedText.value ?: "", _targetLanguage.value?.code)
        } else {
            viewModelScope.launch {
                _effect.emit(UiEffect.RequestLanguageDownload)
            }
        }
    }

    fun stopPlayback() {
        tts.stop()
    }

    fun selectSourceLanguage(language: Language) {
        if (language == _targetLanguage.value) {
            flipLanguages()
        } else {
            _sourceLanguage.value = language
            viewModelScope.launch {
                dataStore.saveSourceLanguage(language.code)
            }
        }
    }

    fun selectTargetLanguage(language: Language) {
        if (language == _sourceLanguage.value) {
            flipLanguages()
        } else {
            _targetLanguage.value = language
            viewModelScope.launch {
                dataStore.saveTargetLanguage(language.code)
            }
        }
    }

    fun flipLanguages() {
        viewModelScope.launch {
            val source = dataStore.sourceLanguage.firstOrNull()
            val target = dataStore.targetLanguage.firstOrNull()

            if (source != null && target != null) {
                dataStore.saveSourceLanguage(target)
                dataStore.saveTargetLanguage(source)
            }

            val sourceLanguage = _sourceLanguage.value
            val targetLanguage = _targetLanguage.value

            _sourceLanguage.value = targetLanguage
            _targetLanguage.value = sourceLanguage
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

        viewModelScope.launch {
            translator.events.collect {
                when (it) {
                    Event.MODEL_DOES_NOT_EXISTS -> _effect.emit(
                        UiEffect.LanguageModelDoesNotExists(
                            TranslationError(
                                title = resources.getString(R.string.error_translating_title),
                                subtitle = resources.getString(
                                    if (networkConnectionManager.isInternetAvailable()) {
                                        R.string.language_model_not_found
                                    } else {
                                        R.string.language_model_not_found_no_internet
                                    }
                                ),
                                actionText = resources.getString(R.string.error_try_again),
                                onActionClick = {
                                    retryTranslate()
                                }
                            )
                        )
                    )

                    Event.ERROR_TRANSLATING -> _effect.emit(UiEffect.ErrorWhileTranslatingMessage)
                    Event.TRANSLATED -> _effect.emit(UiEffect.ClearError)
                }
            }
        }
    }
}

sealed class UiEffect {
    data object RequestLanguageDownload : UiEffect()
    data object ClearError : UiEffect()
    data class LanguageModelDoesNotExists(val error: TranslationError) : UiEffect()
    data object ErrorWhileTranslatingMessage : UiEffect()
}