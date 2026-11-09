package com.vnamashko.understandme.camera.vm

import android.graphics.Bitmap
import android.graphics.Matrix
import androidx.camera.core.ImageProxy
import androidx.compose.ui.graphics.asImageBitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vnamashko.understandme.camera.recognition.TextBlockRecognizer
import com.vnamashko.understandme.coroutines.DefaultDispatcher
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject
import kotlin.math.max
import kotlin.math.min

@HiltViewModel
class CameraViewModel @Inject constructor(
    private val textRecognizer: TextBlockRecognizer,
    @DefaultDispatcher private val defaultDispatcher: CoroutineDispatcher,
) : ViewModel() {

    private val _state = MutableStateFlow(CameraUiState())
    val state: StateFlow<CameraUiState> = _state.asStateFlow()

    private var photoJob: Job? = null

    fun onTakingPhoto() {
        _state.update { it.copy(isTakingPhoto = true) }
    }

    fun onPhotoTaken(image: ImageProxy) {
        photoJob?.cancel()
        photoJob = viewModelScope.launch {
            val photo = withContext(defaultDispatcher) {
                image.use { it.toUprightBitmap(MAX_PHOTO_SIDE) }
            }
            _state.update {
                it.copy(
                    photo = photo.asImageBitmap(),
                    isTakingPhoto = false,
                    textBlocks = persistentListOf(),
                    isRecognizing = true,
                )
            }
            val blocks = textRecognizer.recognize(photo)
            _state.update { it.copy(textBlocks = blocks, isRecognizing = false) }
        }
    }

    fun onPhotoFailed() {
        _state.update { it.copy(isTakingPhoto = false) }
    }

    fun onPhotoDismissed() {
        photoJob?.cancel()
        photoJob = null
        _state.value = CameraUiState()
    }

    override fun onCleared() {
        textRecognizer.close()
    }
}

private const val MAX_PHOTO_SIDE = 2000

private fun ImageProxy.toUprightBitmap(maxSide: Int): Bitmap {
    val bitmap = toBitmap()
    val scale = min(1f, maxSide.toFloat() / max(bitmap.width, bitmap.height))
    val rotation = imageInfo.rotationDegrees
    if (scale == 1f && rotation == 0) return bitmap
    val matrix = Matrix().apply {
        postScale(scale, scale)
        postRotate(rotation.toFloat())
    }
    return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
}
