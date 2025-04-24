package com.vnamashko.understandme.translation

import com.google.android.gms.tasks.Tasks
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
import com.vnamashko.understandme.coroutines.IODispatcher
import com.vnamashko.understandme.network.NetworkConnectionManager
import com.vnamashko.understandme.translation.model.Event
import com.vnamashko.understandme.translation.model.Language
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Locale
import javax.inject.Inject
import com.google.mlkit.nl.translate.Translator as MlKitTranslator

interface Translator {
    val events: SharedFlow<Event>

    val detectedLanguage: StateFlow<String?>

    val translatedText: StateFlow<String?>

    val supportedLanguages: List<Language>

    val downloadedModels: StateFlow<List<String>>

    val downloadingModels: StateFlow<Set<String>>

    fun translate(text: String)

    fun retryTranslate()

    fun setSourceLanguage(language: String)

    fun setTargetLanguage(language: String)

    fun deleteModel(language: String)

    fun downloadModel(language: String)
}

class TranslatorImpl @Inject constructor(
    @AppCoroutinesScope val externalScope: CoroutineScope,
    @IODispatcher val ioDispatcher: CoroutineDispatcher,
    private val networkConnectionManager: NetworkConnectionManager
): Translator {

    private val modelManager = RemoteModelManager.getInstance()

    private val languageIdentifier = LanguageIdentification
        .getClient(
            LanguageIdentificationOptions.Builder()
                .setConfidenceThreshold(0.8f)
                .build()
        )

    private val _events: MutableSharedFlow<Event> = MutableSharedFlow(1)
    override val events: SharedFlow<Event> = _events.asSharedFlow()

    private val _detectedLanguage: MutableStateFlow<String?> = MutableStateFlow(null)
    override val detectedLanguage: StateFlow<String?> = _detectedLanguage.asStateFlow()

    private val sourceLanguage = MutableStateFlow<String?>(null)
    private val targetLanguage = MutableStateFlow<String?>(null)

    private val textToTranslate = MutableStateFlow<String?>(null)

    private val retryTrigger = MutableSharedFlow<Boolean>(
        replay = 1,
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )

    override val supportedLanguages: List<Language> by lazy {
        TranslateLanguage.getAllLanguages().map { code ->
            Language(code = code, displayName = Locale(code).displayName)
        }
    }

    private val _downloadingModels = MutableStateFlow<Set<String>>(emptySet())
    override val downloadingModels = _downloadingModels

    private val _downloadedModels: MutableStateFlow<Set<TranslateRemoteModel>> = MutableStateFlow(
        emptySet()
    )

    override val downloadedModels: StateFlow<List<String>> = _downloadedModels.map {
        it.map { it.language }
    }.stateIn(externalScope, SharingStarted.Lazily, emptyList())

    private val translator: SharedFlow<MlKitTranslator?> =
        combine(
            sourceLanguage.filterNotNull(),
            targetLanguage.filterNotNull(),
            retryTrigger
        ) { source, target, _ ->
            val options = TranslatorOptions.Builder()
                .setSourceLanguage(source)
                .setTargetLanguage(target)
                .build()

        val translator = Translation.getClient(options)
        if (networkConnectionManager.isInternetAvailable()) {
            // TODO: conditional download
            Tasks.await(translator.downloadModelIfNeeded())
        }
        translator
    }.shareIn(externalScope, SharingStarted.Eagerly, 1)

    override val translatedText = combine(translator.filterNotNull(), textToTranslate) { translator, text ->
        if (text == null) {
            _detectedLanguage.value = null
            null
        } else {

            languageIdentifier.identifyLanguage(text).addOnSuccessListener {
                _detectedLanguage.value = it
            }

            val task = translator.translate(text)
            task.addOnFailureListener { ex ->
                if (ex is MlKitException && ex.errorCode == MlKitException.NOT_FOUND) {
                    _events.tryEmit(Event.MODEL_DOES_NOT_EXISTS)
                } else {
                    _events.tryEmit(Event.ERROR_TRANSLATING)
                }
            }
            val translated = kotlin.runCatching {
                Tasks.await(task)
            }
            translated.getOrNull()?.also {
                // emit event only when translation was successful
                _events.emit(Event.TRANSLATED)
            }
        }
    }.stateIn(externalScope, SharingStarted.Lazily, null)

    override fun translate(text: String) {
        textToTranslate.value = text.takeIf { it.isNotBlank() }
    }

    override fun retryTranslate() {
        // trigger retry by emitting new translator in case it was not downloaded
        retryTrigger.tryEmit(true)
    }

    override fun setSourceLanguage(language: String) {
        sourceLanguage.value = language
    }

    override fun setTargetLanguage(language: String) {
        targetLanguage.value = language
    }

    override fun deleteModel(language: String) {
        _downloadedModels.value.find { it.language == language }?.let {
            externalScope.launch(ioDispatcher) {
                Tasks.await(modelManager.deleteDownloadedModel(it))
                _downloadedModels.value = getDownloadedModels()
            }
        }
    }

    override fun downloadModel(language: String) {
        externalScope.launch(ioDispatcher) {
            _downloadingModels.value += language
            val model = TranslateRemoteModel.Builder(language).build()
            Tasks.await(modelManager.download(model, DownloadConditions.Builder().build()))
            _downloadedModels.value = getDownloadedModels()
            _downloadingModels.value -= language
        }
    }

    private fun getDownloadedModels() =
        Tasks.await(modelManager.getDownloadedModels(TranslateRemoteModel::class.java))

    init {
        externalScope.launch {
            retryTrigger.emit(true)

            _downloadedModels.value = getDownloadedModels()
        }
    }
}