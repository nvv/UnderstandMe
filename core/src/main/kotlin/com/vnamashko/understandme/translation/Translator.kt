package com.vnamashko.understandme.translation

import com.google.android.gms.tasks.Tasks
import com.google.mlkit.common.model.RemoteModelManager
import com.google.mlkit.nl.translate.TranslateLanguage
import com.google.mlkit.nl.translate.TranslateRemoteModel
import com.google.mlkit.nl.translate.Translation
import com.google.mlkit.nl.translate.TranslatorOptions
import com.vnamashko.understandme.coroutines.AppCoroutinesScope
import com.vnamashko.understandme.translation.model.Language
import com.vnamashko.understandme.translation.model.State
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.stateIn
import java.util.Locale
import javax.inject.Inject
import com.google.mlkit.nl.translate.Translator as MlKitTranslator

interface Translator {
    val state: StateFlow<State>

    val translatedText: StateFlow<String?>

    val supportedLanguages: List<Language>

    val downloadedModels: StateFlow<List<String>>

    fun translate(text: String)

    fun setSourceLanguage(language: Language)

    fun setTargetLanguage(language: Language)
}

class TranslatorImpl @Inject constructor(
    @AppCoroutinesScope val externalScope: CoroutineScope
): Translator {

    private val modelManager = RemoteModelManager.getInstance()

    private val _state: MutableStateFlow<State> = MutableStateFlow(State.IDLE)
    override val state: StateFlow<State> = _state.asStateFlow()

    private val sourceLanguage = MutableStateFlow(TranslateLanguage.ENGLISH)
    private val targetLanguage = MutableStateFlow(TranslateLanguage.SPANISH)

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

    private val translator: StateFlow<MlKitTranslator?> = combine(sourceLanguage, targetLanguage) { source, target ->
        val options = TranslatorOptions.Builder()
            .setSourceLanguage(source)
            .setTargetLanguage(target)
            .build()

        _state.value = State.LOADING_MODEL
        val translator = Translation.getClient(options)
        Tasks.await(translator.downloadModelIfNeeded())
        _state.value = State.IDLE
        translator
    }.stateIn(externalScope, SharingStarted.Eagerly, null)

    override val translatedText = combine(translator.filterNotNull(), textToTranslate) { translator, text ->
        if (text == null) {
            null
        } else {
           Tasks.await(translator.translate(text))
        }
    }.stateIn(externalScope, SharingStarted.Lazily, null)

    override fun translate(text: String) {
        textToTranslate.value = text.takeIf { it.isNotBlank() }
    }

    override fun setSourceLanguage(language: Language) {
        sourceLanguage.value = language.code
    }

    override fun setTargetLanguage(language: Language) {
        targetLanguage.value = language.code
    }

    init {
        downloadedModels.launchIn(externalScope)
    }
}