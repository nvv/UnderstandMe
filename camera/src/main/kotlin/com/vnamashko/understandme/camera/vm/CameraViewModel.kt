package com.vnamashko.understandme.camera.vm

import android.graphics.Bitmap
import android.net.Uri
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.luminance
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vnamashko.understandme.camera.image.ImageLoader
import com.vnamashko.understandme.camera.model.PhotoSource
import com.vnamashko.understandme.camera.model.RecognizedTextBlock
import com.vnamashko.understandme.camera.model.TranslatedTextBlock
import com.vnamashko.understandme.camera.recognition.TextBlockRecognizer
import com.vnamashko.understandme.translation.Translator
import com.vnamashko.understandme.translation.model.BatchTranslation
import com.vnamashko.understandme.translation.model.LanguagePair
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CameraViewModel @Inject constructor(
    private val textRecognizer: TextBlockRecognizer,
    private val imageLoader: ImageLoader,
    private val translator: Translator,
) : ViewModel() {

    private val _state = MutableStateFlow(CameraUiState())
    val state: StateFlow<CameraUiState> = _state.asStateFlow()

    private var languagePair: LanguagePair? = null
    private var recognizedBlocks: List<RecognizedTextBlock> = emptyList()
    private var photoJob: Job? = null
    private var translationJob: Job? = null

    fun onLanguagePairChanged(pair: LanguagePair?) {
        if (pair == languagePair) return
        languagePair = pair
        if (_state.value.photo != null) translate(recognizedBlocks)
    }

    fun onPhotoTaken(photo: Bitmap) {
        showPhoto(PhotoSource.CAMERA) { photo }
    }

    fun onImagePicked(uri: Uri) {
        showPhoto(PhotoSource.GALLERY) { imageLoader.load(uri) }
    }

    private fun showPhoto(source: PhotoSource, load: suspend () -> Bitmap?) {
        photoJob?.cancel()
        translationJob?.cancel()
        photoJob = viewModelScope.launch {
            val photo = load() ?: return@launch
            _state.update {
                it.copy(
                    photo = photo.asImageBitmap(),
                    photoSource = source,
                    translatedBlocks = persistentListOf(),
                    isProcessing = true,
                    isModelMissing = false,
                )
            }
            recognizedBlocks = textRecognizer.recognize(photo)
            translate(recognizedBlocks)
        }
    }

    fun onPhotoDismissed() {
        photoJob?.cancel()
        translationJob?.cancel()
        photoJob = null
        translationJob = null
        recognizedBlocks = emptyList()
        _state.value = CameraUiState()
    }

    private fun translate(recognized: List<RecognizedTextBlock>) {
        translationJob?.cancel()
        val pair = languagePair
        val blocks = pair?.let { recognized.filter { block -> block.isTranslatable(it.source) } }.orEmpty()
        if (pair == null || blocks.isEmpty()) {
            _state.update { it.copy(translatedBlocks = persistentListOf(), isProcessing = false) }
            return
        }
        translationJob = viewModelScope.launch {
            _state.update {
                it.copy(translatedBlocks = persistentListOf(), isProcessing = true, isModelMissing = false)
            }
            val texts = blocks.mapTo(mutableSetOf()) { it.text.asSingleLine() }
            when (val result = translator.translateAll(texts, pair)) {
                is BatchTranslation.Translated -> _state.update { state ->
                    state.copy(
                        translatedBlocks = blocks
                            .mapNotNull { block ->
                                val original = block.text.asSingleLine()
                                result.translations[original]
                                    ?.takeUnless { it.trim().equals(original, ignoreCase = true) }
                                    ?.let { translation ->
                                    TranslatedTextBlock(
                                        block = block,
                                        translation = translation,
                                        textColor = textColorOn(block.backgroundColor),
                                    )
                                }
                            }
                            .toImmutableList(),
                        isProcessing = false,
                    )
                }
                BatchTranslation.ModelMissing -> _state.update {
                    it.copy(isProcessing = false, isModelMissing = true)
                }
            }
        }
    }

    override fun onCleared() {
        textRecognizer.close()
    }
}

private const val MIN_BLOCK_CONFIDENCE = 0.7f

private fun RecognizedTextBlock.isTranslatable(source: String): Boolean =
    confidence >= MIN_BLOCK_CONFIDENCE && isInLanguage(source)

private fun RecognizedTextBlock.isInLanguage(code: String): Boolean =
    language?.substringBefore('-')?.equals(code.substringBefore('-'), ignoreCase = true) == true

private fun String.asSingleLine(): String = lines().joinToString(" ") { it.trim() }.trim()

private fun textColorOn(background: Color): Color =
    if (background.luminance() > 0.5f) Color.Black else Color.White
