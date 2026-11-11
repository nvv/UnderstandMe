package com.vnamashko.understandme.camera.vm

import androidx.compose.ui.graphics.ImageBitmap
import com.vnamashko.understandme.camera.model.PhotoSource
import com.vnamashko.understandme.camera.model.TranslatedTextBlock
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

data class CameraUiState(
    val photo: ImageBitmap? = null,
    val photoSource: PhotoSource = PhotoSource.CAMERA,
    val translatedBlocks: ImmutableList<TranslatedTextBlock> = persistentListOf(),
    val isProcessing: Boolean = false,
    val isModelMissing: Boolean = false,
)
