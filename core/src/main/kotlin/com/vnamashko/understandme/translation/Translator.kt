package com.vnamashko.understandme.translation

import com.google.mlkit.common.MlKitException
import com.google.mlkit.common.model.DownloadConditions
import com.google.mlkit.common.model.RemoteModelManager
import com.google.mlkit.nl.languageid.LanguageIdentification
import com.google.mlkit.nl.languageid.LanguageIdentificationOptions
import com.google.mlkit.nl.translate.TranslateLanguage
import com.google.mlkit.nl.translate.TranslateRemoteModel
import com.google.mlkit.nl.translate.Translation
import com.google.mlkit.nl.translate.TranslatorOptions
import com.vnamashko.understandme.coroutines.AppCoroutinesScope
import com.vnamashko.understandme.translation.model.Language
import com.vnamashko.understandme.translation.model.LanguagePair
import com.vnamashko.understandme.translation.model.TranslationResult
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.Locale
import javax.inject.Inject
import com.google.mlkit.nl.translate.Translator as MlKitTranslator

interface Translator {
    val supportedLanguages: List<Language>

    val downloadedModels: StateFlow<List<String>>

    val downloadingModels: StateFlow<Set<String>>

    suspend fun translate(text: String, pair: LanguagePair): TranslationResult

    suspend fun identifyLanguage(text: String): String?

    suspend fun downloadModelsIfNeeded(pair: LanguagePair)

    fun deleteModel(language: String)

    fun downloadModel(language: String)
}

class TranslatorImpl @Inject constructor(
    @AppCoroutinesScope private val externalScope: CoroutineScope,
) : Translator {

    private val modelManager = RemoteModelManager.getInstance()

    private val languageIdentifier = LanguageIdentification.getClient(
        LanguageIdentificationOptions.Builder()
            .setConfidenceThreshold(0.8f)
            .build()
    )

    override val supportedLanguages: List<Language> by lazy {
        TranslateLanguage.getAllLanguages().map { code ->
            Language(code = code, displayName = Locale(code).displayName)
        }
    }

    private val _downloadingModels = MutableStateFlow<Set<String>>(emptySet())
    override val downloadingModels: StateFlow<Set<String>> = _downloadingModels.asStateFlow()

    private val _downloadedModels = MutableStateFlow<Set<TranslateRemoteModel>>(emptySet())
    override val downloadedModels: StateFlow<List<String>> = _downloadedModels
        .map { models -> models.map { it.language } }
        .stateIn(externalScope, SharingStarted.Lazily, emptyList())

    private var client: Pair<LanguagePair, MlKitTranslator>? = null

    override suspend fun translate(text: String, pair: LanguagePair): TranslationResult =
        try {
            TranslationResult.Translated(clientFor(pair).translate(text).await())
        } catch (e: CancellationException) {
            throw e
        } catch (e: MlKitException) {
            if (e.errorCode == MlKitException.NOT_FOUND) TranslationResult.ModelMissing else TranslationResult.Failed
        } catch (_: Exception) {
            TranslationResult.Failed
        }

    override suspend fun identifyLanguage(text: String): String? =
        try {
            languageIdentifier.identifyLanguage(text).await()
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            null
        }

    override suspend fun downloadModelsIfNeeded(pair: LanguagePair) {
        val missing = setOf(pair.source, pair.target) - _downloadedModels.value.map { it.language }.toSet()
        _downloadingModels.value += missing
        try {
            clientFor(pair).downloadModelIfNeeded().await()
            refreshDownloadedModels()
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            return
        } finally {
            _downloadingModels.value -= missing
        }
    }

    override fun deleteModel(language: String) {
        val model = _downloadedModels.value.find { it.language == language } ?: return
        externalScope.launch {
            runCatching { modelManager.deleteDownloadedModel(model).await() }
            refreshDownloadedModels()
        }
    }

    override fun downloadModel(language: String) {
        externalScope.launch {
            _downloadingModels.value += language
            try {
                val model = TranslateRemoteModel.Builder(language).build()
                runCatching { modelManager.download(model, DownloadConditions.Builder().build()).await() }
                refreshDownloadedModels()
            } finally {
                _downloadingModels.value -= language
            }
        }
    }

    private fun clientFor(pair: LanguagePair): MlKitTranslator =
        client?.takeIf { it.first == pair }?.second
            ?: Translation.getClient(
                TranslatorOptions.Builder()
                    .setSourceLanguage(pair.source)
                    .setTargetLanguage(pair.target)
                    .build()
            ).also { newClient ->
                client?.second?.close()
                client = pair to newClient
            }

    private suspend fun refreshDownloadedModels() {
        runCatching { modelManager.getDownloadedModels(TranslateRemoteModel::class.java).await() }
            .onSuccess { _downloadedModels.value = it }
    }

    init {
        externalScope.launch {
            refreshDownloadedModels()
        }
    }
}
