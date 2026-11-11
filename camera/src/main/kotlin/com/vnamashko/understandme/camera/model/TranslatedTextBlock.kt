package com.vnamashko.understandme.camera.model

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

@Immutable
data class TranslatedTextBlock(
    val block: RecognizedTextBlock,
    val translation: String,
    val textColor: Color,
)
