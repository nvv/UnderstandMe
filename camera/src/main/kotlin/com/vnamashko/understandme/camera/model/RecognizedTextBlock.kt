package com.vnamashko.understandme.camera.model

import androidx.compose.runtime.Immutable
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect

@Immutable
data class RecognizedTextBlock(
    val text: String,
    val bounds: Rect,
    val corners: List<Offset> = listOf(bounds.topLeft, bounds.topRight, bounds.bottomRight, bounds.bottomLeft),
    val lineHeight: Float = bounds.height,
)
