package com.vnamashko.understandme.camera.recognition

import android.graphics.Bitmap
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.toComposeRect
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.Text
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import com.vnamashko.understandme.camera.model.RecognizedTextBlock
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

interface TextBlockRecognizer : AutoCloseable {
    suspend fun recognize(bitmap: Bitmap): ImmutableList<RecognizedTextBlock>
}

class TextBlockRecognizerImpl @Inject constructor() : TextBlockRecognizer {

    private val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

    override suspend fun recognize(bitmap: Bitmap): ImmutableList<RecognizedTextBlock> =
        try {
            recognizer.process(InputImage.fromBitmap(bitmap, 0)).await().toBlocks()
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            persistentListOf()
        }

    override fun close() {
        recognizer.close()
    }
}

private fun Text.toBlocks(): ImmutableList<RecognizedTextBlock> =
    textBlocks
        .mapNotNull { block ->
            val bounds = block.boundingBox?.toComposeRect() ?: return@mapNotNull null
            val lineHeights = block.lines.mapNotNull { line -> line.boundingBox?.height()?.toFloat() }
            val corners = block.cornerPoints
                ?.takeIf { it.size == 4 }
                ?.map { Offset(it.x.toFloat(), it.y.toFloat()) }
            RecognizedTextBlock(
                text = block.text,
                bounds = bounds,
                corners = corners ?: listOf(bounds.topLeft, bounds.topRight, bounds.bottomRight, bounds.bottomLeft),
                lineHeight = if (lineHeights.isEmpty()) bounds.height else lineHeights.average().toFloat(),
            )
        }
        .toImmutableList()
