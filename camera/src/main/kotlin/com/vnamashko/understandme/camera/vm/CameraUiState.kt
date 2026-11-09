package com.vnamashko.understandme.camera.vm

import androidx.compose.ui.graphics.ImageBitmap
import com.vnamashko.understandme.camera.model.RecognizedTextBlock
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

data class CameraUiState(
    val photo: ImageBitmap? = null,
    val isTakingPhoto: Boolean = false,
    val textBlocks: ImmutableList<RecognizedTextBlock> = persistentListOf(),
    val isRecognizing: Boolean = false,
)
