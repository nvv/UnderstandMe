package com.vnamashko.understandme.translation.vm

import android.content.res.Resources
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vnamashko.understandme.data.dao.RecentLanguageDao
import com.vnamashko.understandme.data.model.toRecentLanguage
import com.vnamashko.understandme.network.NetworkConnectionManager
import com.vnamashko.understandme.settings.SettingsDataStore
import com.vnamashko.understandme.translation.Translator
import com.vnamashko.understandme.translation.model.Language
import com.vnamashko.understandme.translation.model.LanguageModel
import com.vnamashko.understandme.translation.model.LanguagePair
import com.vnamashko.understandme.translation.model.TranslationResult
import com.vnamashko.understandme.tts.Tts
import com.vnamashko.understandme.translation.views.TranslationError
import com.vnamashko.undertsndme.language.picker.LanguageSelectionState
import com.vnamashko.undertsndme.translation.screen.R
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class TranslateViewModel @Inject constructor(
    private val dataStore: SettingsDataStore,
    private val networkConnectionManager: NetworkConnectionManager,
    private val translator: Translator,
    private val tts: Tts,
    private val resources: Resources,
    private val recentLanguageDao: RecentLanguageDao,
) : ViewModel() {

    private val _effect = MutableSharedFlow<UiEffect>()
    val effect = _effect.asSharedFlow()

    private val _originalText: MutableStateFlow<String> = MutableStateFlow("")
    val originalText: StateFlow<String> = _originalText.asStateFlow()
    private val _translatedText = MutableStateFlow<String?>(null)
    val translatedText: StateFlow<String?> = _translatedText.asStateFlow()

    private val _detectedLanguage = MutableStateFlow<String?>(null)
    private val retryRequests = MutableStateFlow(0)

    private val _sourceLanguage = MutableStateFlow<Language?>(null)
    val sourceLanguage = _sourceLanguage.asStateFlow()
    private val _targetLanguage = MutableStateFlow<Language?>(null)
    val targetLanguage = _targetLanguage.asStateFlow()

    private val languagePair: Flow<LanguagePair?> = combine(_sourceLanguage, _targetLanguage) { source, target ->
        if (source != null && target != null) LanguagePair(source = source.code, target = target.code) else null
    }.distinctUntilChanged()

    private val _showDownloadModelDialogAction = MutableStateFlow<(suspend () -> Unit)?>(null)
    val showDownloadModelDialogAction: StateFlow<(suspend () -> Unit)?> = _showDownloadModelDialogAction

    val proposedSourceLanguage: StateFlow<Language?> = combine(
        _detectedLanguage,
        _sourceLanguage.map { it?.code }
    ) { detected, selected ->
        if (detected != "und" && detected != selected) translator.supportedLanguages.find { it.code == detected } else null
    }.stateIn(viewModelScope, SharingStarted.Lazily, null)

    /** Everything the language bar shows. */
    val languageSelection: StateFlow<LanguageSelectionState> = combine(
        sourceLanguage,
        targetLanguage,
        proposedSourceLanguage,
    ) { source, target, proposedSource ->
        LanguageSelectionState(source = source, target = target, proposedSource = proposedSource)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), LanguageSelectionState())

    val isDownloadingModel: StateFlow<Boolean> = combine(languagePair, translator.downloadingModels) { pair, downloading ->
        pair != null && (pair.source in downloading || pair.target in downloading)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

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

    val recentLanguages = combine(
        recentLanguageDao.getRecentLanguages().distinctUntilChanged(),
        supportedModels
    ) { recents, supportedModels ->
        supportedModels.filter { model -> recents.any { it.code == model.language.code } }.toImmutableList()
    }.stateIn(viewModelScope, SharingStarted.Lazily, persistentListOf())

    fun translate(text: String) {
        _originalText.value = text
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
        retryRequests.value++
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
        viewModelScope.launch {
            if (shouldShowModelDownloadDialog(language)) {
                _showDownloadModelDialogAction.value = {
                    saveSourceLanguage(language)
                }
            } else {
                saveSourceLanguage(language)
            }
        }
    }

    private suspend fun saveSourceLanguage(language: Language) {
        if (language == _targetLanguage.value) {
            flipLanguages()
        } else {
            _sourceLanguage.value = language
            dataStore.saveSourceLanguage(language.code)
            recentLanguageDao.insertLanguage(language.toRecentLanguage())
            recentLanguageDao.trimLanguages()
        }
    }

    fun selectTargetLanguage(language: Language) {
        viewModelScope.launch {
            if (shouldShowModelDownloadDialog(language)) {
                _showDownloadModelDialogAction.value = {
                    saveTargetLanguage(language)
                }
            } else {
                saveTargetLanguage(language)
            }
        }
    }

    private suspend fun saveTargetLanguage(language: Language) {
        if (language == _sourceLanguage.value) {
            flipLanguages()
        } else {
            _targetLanguage.value = language
            dataStore.saveTargetLanguage(language.code)
            recentLanguageDao.insertLanguage(language.toRecentLanguage())
            recentLanguageDao.trimLanguages()
        }
    }

    private suspend fun shouldShowModelDownloadDialog(language: Language): Boolean {
        return !dataStore.allowDataDownload.first() &&
                !networkConnectionManager.isWifiConnected() &&
                language.code != "en" &&
                !translator.downloadedModels.value.contains(language.code)
    }

    fun resetModelDownloadDialog() {
        _showDownloadModelDialogAction.value = null
    }

    fun allowDataDownload() {
        viewModelScope.launch {
            dataStore.saveAllowDataDownload(true)
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

    private suspend fun showTranslation(text: String, pair: LanguagePair) {
        if (text.isBlank()) {
            _translatedText.value = null
            return
        }
        when (val result = translator.translate(text, pair)) {
            is TranslationResult.Translated -> {
                _translatedText.value = result.text
                _effect.emit(UiEffect.ClearError)
            }
            TranslationResult.ModelMissing -> {
                _translatedText.value = null
                _effect.emit(modelMissingEffect())
            }
            TranslationResult.Failed -> {
                _translatedText.value = null
                _effect.emit(UiEffect.ErrorWhileTranslatingMessage)
            }
        }
    }

    private fun modelMissingEffect() = UiEffect.LanguageModelDoesNotExists(
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

    @OptIn(ExperimentalCoroutinesApi::class)
    private fun observeTranslation() {
        viewModelScope.launch {
            combine(languagePair.filterNotNull(), retryRequests) { pair, _ -> pair }
                .mapLatest { pair ->
                    if (networkConnectionManager.isInternetAvailable()) {
                        translator.downloadModelsIfNeeded(pair)
                    }
                    pair
                }
                .combine(_originalText) { pair, text -> pair to text }
                .collectLatest { (pair, text) -> showTranslation(text, pair) }
        }

        viewModelScope.launch {
            _originalText.collectLatest { text ->
                _detectedLanguage.value = if (text.isBlank()) null else translator.identifyLanguage(text)
            }
        }
    }

    init {
        observeTranslation()

        viewModelScope.launch {
            _sourceLanguage.value =
                translator.supportedLanguages.find { it.code == dataStore.sourceLanguage.firstOrNull() }
            _targetLanguage.value =
                translator.supportedLanguages.find { it.code == dataStore.targetLanguage.firstOrNull() }
        }
    }
}

sealed class UiEffect {
    data object RequestLanguageDownload : UiEffect()
    data object ClearError : UiEffect()
    data class LanguageModelDoesNotExists(val error: TranslationError) : UiEffect()
    data object ErrorWhileTranslatingMessage : UiEffect()
}