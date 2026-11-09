package com.vnamashko.understandme.camera.model

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import kotlin.math.max

data class CropTransform(
    val scale: Float,
    val offset: Offset,
) {
    fun map(point: Offset): Offset = Offset(
        x = point.x * scale + offset.x,
        y = point.y * scale + offset.y,
    )

    companion object {
        fun of(imageSize: Size, viewSize: Size): CropTransform {
            if (imageSize.width <= 0f || imageSize.height <= 0f) return CropTransform(1f, Offset.Zero)
            val scale = max(viewSize.width / imageSize.width, viewSize.height / imageSize.height)
            return CropTransform(
                scale = scale,
                offset = Offset(
                    x = (viewSize.width - imageSize.width * scale) / 2f,
                    y = (viewSize.height - imageSize.height * scale) / 2f,
                ),
            )
        }
    }
}
