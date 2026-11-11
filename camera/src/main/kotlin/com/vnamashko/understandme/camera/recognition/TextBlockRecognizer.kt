package com.vnamashko.understandme.camera.recognition

import android.graphics.Bitmap
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
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
            recognizer.process(InputImage.fromBitmap(bitmap, 0)).await()
                .toBlocks()
                .map { block ->
                    bitmap.backgroundColorAround(block.corners)?.let { block.copy(backgroundColor = it) } ?: block
                }
                .toImmutableList()
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            persistentListOf()
        }

    override fun close() {
        recognizer.close()
    }
}

private typealias Row = List<Text.Line>
private typealias Paragraph = List<Row>

private const val ROW_OVERLAP = 0.5f
private const val UNDETERMINED_LANGUAGE = "und"
private val LIST_MARKER = Regex("""(\d{1,3}|[A-Za-z])[.)]|[•·▪◦‣*\-–—]""")

private class Axes(val along: Offset, val down: Offset)

private fun Text.toBlocks(): ImmutableList<RecognizedTextBlock> =
    textBlocks.flatMap { it.toRecognizedBlocks() }.toImmutableList()

private fun Text.TextBlock.toRecognizedBlocks(): List<RecognizedTextBlock> {
    val blockBounds = boundingBox?.toComposeRect() ?: return emptyList()
    val axes = axes()
    val paragraphs = paragraphs(axes)
    return paragraphs.map { paragraph ->
        val lines = paragraph.flatten()
        val corners = if (paragraphs.size == 1) cornerOffsets() ?: blockBounds.corners() else lines.enclosingCorners(axes)
        RecognizedTextBlock(
            text = paragraph.text(),
            bounds = if (paragraphs.size == 1) blockBounds else corners.boundingRect(),
            corners = corners,
            lineHeight = lines.map { it.across(axes).size }.average().toFloat(),
            language = lines.majorityLanguage() ?: recognizedLanguage.takeIf { it.isDetermined() },
            confidence = lines.map { it.confidence }.average().toFloat(),
        )
    }
}

private fun Text.TextBlock.paragraphs(axes: Axes): List<Paragraph> =
    rows(axes).fold(mutableListOf<MutableList<Row>>()) { paragraphs, row ->
        if (paragraphs.isEmpty() || row.startsWithListMarker()) paragraphs += mutableListOf(row) else paragraphs.last() += row
        paragraphs
    }

private fun Text.TextBlock.rows(axes: Axes): List<Row> {
    val rows = mutableListOf<MutableList<Text.Line>>()
    var rowExtent: ClosedFloatingPointRange<Float>? = null
    lines
        .filter { it.points().isNotEmpty() }
        .sortedBy { it.across(axes).center }
        .forEach { line ->
            val extent = line.across(axes)
            val current = rowExtent
            if (current != null && current.overlap(extent) > ROW_OVERLAP * minOf(current.size, extent.size)) {
                rows.last() += line
                rowExtent = minOf(current.start, extent.start)..maxOf(current.endInclusive, extent.endInclusive)
            } else {
                rows += mutableListOf(line)
                rowExtent = extent
            }
        }
    return rows.map { row -> row.sortedBy { it.along(axes).start } }
}

private fun Paragraph.text(): String = joinToString("\n") { row -> row.joinToString(" ") { it.text } }

private fun Row.startsWithListMarker(): Boolean =
    firstOrNull()?.elements?.firstOrNull()?.text?.matches(LIST_MARKER) == true

private fun Text.TextBlock.axes(): Axes {
    val corners = cornerOffsets()
    val along = corners?.let { (it[1] - it[0]).normalized() } ?: return Axes(Offset(1f, 0f), Offset(0f, 1f))
    val perpendicular = Offset(-along.y, along.x)
    val down = if (perpendicular.dot(corners[3] - corners[0]) < 0f) -perpendicular else perpendicular
    return Axes(along, down)
}

private fun Text.TextBlock.cornerOffsets(): List<Offset>? =
    cornerPoints?.takeIf { it.size == 4 }?.map { Offset(it.x.toFloat(), it.y.toFloat()) }

private fun Text.Line.points(): List<Offset> =
    cornerPoints?.takeIf { it.size == 4 }?.map { Offset(it.x.toFloat(), it.y.toFloat()) }
        ?: boundingBox?.toComposeRect()?.corners().orEmpty()

private fun Text.Line.along(axes: Axes): ClosedFloatingPointRange<Float> = points().extentOn(axes.along)

private fun Text.Line.across(axes: Axes): ClosedFloatingPointRange<Float> = points().extentOn(axes.down)

private fun List<Offset>.extentOn(axis: Offset): ClosedFloatingPointRange<Float> {
    val projections = map { it.dot(axis) }
    return projections.min()..projections.max()
}

private fun List<Text.Line>.enclosingCorners(axes: Axes): List<Offset> {
    val points = flatMap { it.points() }
    val along = points.extentOn(axes.along)
    val across = points.extentOn(axes.down)
    fun point(a: Float, d: Float) = axes.along * a + axes.down * d
    return listOf(
        point(along.start, across.start),
        point(along.endInclusive, across.start),
        point(along.endInclusive, across.endInclusive),
        point(along.start, across.endInclusive),
    )
}

private fun List<Text.Line>.majorityLanguage(): String? =
    map { it.recognizedLanguage }
        .filter { it.isDetermined() }
        .groupingBy { it }
        .eachCount()
        .maxByOrNull { it.value }
        ?.key

private fun String?.isDetermined(): Boolean = !isNullOrBlank() && this != UNDETERMINED_LANGUAGE

private fun Rect.corners(): List<Offset> = listOf(topLeft, topRight, bottomRight, bottomLeft)

private fun List<Offset>.boundingRect(): Rect = Rect(
    left = minOf { it.x },
    top = minOf { it.y },
    right = maxOf { it.x },
    bottom = maxOf { it.y },
)

private fun Offset.dot(other: Offset): Float = x * other.x + y * other.y

private fun Offset.normalized(): Offset? = getDistance().takeIf { it > 0f }?.let { this / it }

private val ClosedFloatingPointRange<Float>.size: Float get() = endInclusive - start

private val ClosedFloatingPointRange<Float>.center: Float get() = (start + endInclusive) / 2f

private fun ClosedFloatingPointRange<Float>.overlap(other: ClosedFloatingPointRange<Float>): Float =
    minOf(endInclusive, other.endInclusive) - maxOf(start, other.start)
