package com.vnamashko.understandme.translation

import com.google.android.gms.tasks.Tasks
import com.google.mlkit.common.MlKitException
import com.google.mlkit.common.model.RemoteModelManager
import com.google.mlkit.nl.translate.TranslateLanguage
import com.google.mlkit.nl.translate.TranslateRemoteModel
import com.google.mlkit.nl.translate.Translation
import com.google.mlkit.nl.translate.TranslatorOptions
import com.vnamashko.understandme.coroutines.AppCoroutinesScope
import com.vnamashko.understandme.network.NetworkConnectionManager
import com.vnamashko.understandme.translation.model.Language
import com.vnamashko.understandme.translation.model.Event
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.stateIn
import java.util.Locale
import javax.inject.Inject
import com.google.mlkit.nl.translate.Translator as MlKitTranslator

interface Translator {
    val events: SharedFlow<Event>

    val translatedText: StateFlow<String?>

    val supportedLanguages: List<Language>

    val downloadedModels: StateFlow<List<String>>

    fun translate(text: String)

    fun setSourceLanguage(language: String)

    fun setTargetLanguage(language: String)
}

class TranslatorImpl @Inject constructor(
    @AppCoroutinesScope val externalScope: CoroutineScope,
    private val networkConnectionManager: NetworkConnectionManager
): Translator {

    private val modelManager = RemoteModelManager.getInstance()

    private val _events: MutableSharedFlow<Event> = MutableSharedFlow(1)
    override val events: SharedFlow<Event> = _events.asSharedFlow()

    private val sourceLanguage = MutableStateFlow<String?>(null)
    private val targetLanguage = MutableStateFlow<String?>(null)

    private val textToTranslate = MutableStateFlow<String?>(null)

    override val supportedLanguages: List<Language> by lazy {
        TranslateLanguage.getAllLanguages().map { code ->
            Language(code = code, displayName = Locale(code).displayName)
        }
    }

    override val downloadedModels: StateFlow<List<String>> = flow {
        val models =
            Tasks.await(modelManager.getDownloadedModels(TranslateRemoteModel::class.java))

        emit(models.map { it.language })
    }.stateIn(externalScope, SharingStarted.Lazily, emptyList())

    private val translator: StateFlow<MlKitTranslator?> =
        combine(sourceLanguage.filterNotNull(), targetLanguage.filterNotNull()) { source, target ->
            val options = TranslatorOptions.Builder()
                .setSourceLanguage(source)
                .setTargetLanguage(target)
                .build()

        val translator = Translation.getClient(options)
        if (networkConnectionManager.isInternetAvailable()) {
            _events.emit(Event.LOADING_MODEL)
            Tasks.await(translator.downloadModelIfNeeded())
        }
        translator
    }.stateIn(externalScope, SharingStarted.Eagerly, null)

    override val translatedText = combine(translator.filterNotNull(), textToTranslate) { translator, text ->
        if (text == null) {
            null
        } else {
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
            translated.getOrNull()
        }
    }.stateIn(externalScope, SharingStarted.Lazily, null)

    override fun translate(text: String) {
        textToTranslate.value = text.takeIf { it.isNotBlank() }
    }

    override fun setSourceLanguage(language: String) {
        sourceLanguage.value = language
    }

    override fun setTargetLanguage(language: String) {
        targetLanguage.value = language
    }

    init {
        downloadedModels.launchIn(externalScope)
    }
}