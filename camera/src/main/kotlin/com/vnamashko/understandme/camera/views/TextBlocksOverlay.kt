package com.vnamashko.understandme.camera.views

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vnamashko.understandme.camera.model.CropTransform
import com.vnamashko.understandme.camera.model.OrientedBox
import com.vnamashko.understandme.camera.model.RecognizedTextBlock
import com.vnamashko.understandme.camera.model.TranslatedTextBlock
import com.vnamashko.understandme.camera.model.orientedBox
import com.vnamashko.understandme.theme.AppTheme
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlin.math.max

private const val FONT_TO_LINE_HEIGHT = 0.75f
private val MIN_FONT_SIZE = 6.sp
private val TEXT_PADDING = 2.dp
private val PATCH_STROKE = 2.dp
private const val MAX_GROWTH = 8f
private const val FONT_PRECISION_PX = 0.5f
private const val GROWTH_PRECISION = 0.01f

private class PlacedTranslation(
    val patch: List<Offset>,
    val box: OrientedBox,
    val backgroundColor: Color,
    val text: TextLayoutResult,
)

@Composable
fun TextBlocksOverlay(
    blocks: ImmutableList<TranslatedTextBlock>,
    imageSize: Size,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop,
) {
    val textMeasurer = rememberTextMeasurer()
    val density = LocalDensity.current
    BoxWithConstraints(modifier = modifier) {
        val viewSize = with(density) { Size(maxWidth.toPx(), maxHeight.toPx()) }
        val placed = remember(blocks, imageSize, viewSize, contentScale, density, textMeasurer) {
            TranslationPlacer(
                transform = CropTransform.of(imageSize = imageSize, viewSize = viewSize, contentScale = contentScale),
                imageSize = imageSize,
                viewSize = viewSize,
                textMeasurer = textMeasurer,
                density = density,
            ).place(blocks)
        }

        Canvas(modifier = Modifier.fillMaxSize()) {
            val stroke = Stroke(width = PATCH_STROKE.toPx(), join = StrokeJoin.Round)
            val padding = TEXT_PADDING.toPx()
            placed.forEach { translation ->
                val path = Path().apply {
                    translation.patch.forEachIndexed { index, point ->
                        if (index == 0) moveTo(point.x, point.y) else lineTo(point.x, point.y)
                    }
                    close()
                }
                drawPath(path = path, color = translation.backgroundColor)
                drawPath(path = path, color = translation.backgroundColor, style = stroke)

                val box = translation.box
                rotate(degrees = box.angleDegrees, pivot = box.origin) {
                    clipRect(
                        left = box.origin.x,
                        top = box.origin.y,
                        right = box.origin.x + box.width,
                        bottom = box.origin.y + box.height,
                    ) {
                        drawText(
                            textLayoutResult = translation.text,
                            topLeft = box.origin + Offset(
                                x = padding,
                                y = max(0f, (box.height - translation.text.size.height) / 2f),
                            ),
                        )
                    }
                }
            }
        }
    }
}

private class TranslationPlacer(
    private val transform: CropTransform,
    imageSize: Size,
    viewSize: Size,
    private val textMeasurer: TextMeasurer,
    private val density: Density,
) {
    private val bounds = Rect(transform.map(Offset.Zero), transform.map(Offset(imageSize.width, imageSize.height)))
        .intersect(Rect(Offset.Zero, viewSize))
    private val minFontPx = with(density) { MIN_FONT_SIZE.toPx() }
    private val paddingPx = with(density) { TEXT_PADDING.toPx() }

    fun place(blocks: List<TranslatedTextBlock>): List<PlacedTranslation> {
        val boxes = blocks.map { transform.map(it.block.orientedBox()) }.toMutableList()
        return blocks.mapIndexed { index, translated ->
            val original = boxes[index]
            val preferredFontPx = max(translated.block.lineHeight * transform.scale * FONT_TO_LINE_HEIGHT, minFontPx)
            val fitted = largestFontThatFits(translated, original, preferredFontPx)
            val placed = if (fitted != null) {
                PlacedTranslation(
                    patch = translated.block.corners.map(transform::map),
                    box = original,
                    backgroundColor = translated.block.backgroundColor,
                    text = fitted,
                )
            } else {
                val others = boxes.filterIndexed { other, _ -> other != index }
                val grown = original.grownBy(growthFor(translated, original, others))
                PlacedTranslation(
                    patch = grown.corners(),
                    box = grown,
                    backgroundColor = translated.block.backgroundColor,
                    text = measure(translated, grown, minFontPx),
                )
            }
            boxes[index] = placed.box
            placed
        }
    }

    private fun largestFontThatFits(
        translated: TranslatedTextBlock,
        box: OrientedBox,
        preferredFontPx: Float,
    ): TextLayoutResult? {
        if (!fits(measure(translated, box, minFontPx), box)) return null
        val fontPx = largestWhere(minFontPx, preferredFontPx, FONT_PRECISION_PX) { fits(measure(translated, box, it), box) }
        return measure(translated, box, fontPx)
    }

    private fun growthFor(translated: TranslatedTextBlock, original: OrientedBox, others: List<OrientedBox>): Float {
        val fitsAt = { factor: Float -> original.grownBy(factor).let { fits(measure(translated, it, minFontPx), it) } }
        val needed = if (fitsAt(MAX_GROWTH)) smallestWhere(1f, MAX_GROWTH, GROWTH_PRECISION, fitsAt) else MAX_GROWTH
        val startsInside = original.isInside(bounds)
        val allowedAt = { factor: Float ->
            val grown = original.grownBy(factor)
            others.none { grown.overlaps(it) } && (!startsInside || grown.isInside(bounds))
        }
        return when {
            allowedAt(needed) -> needed
            !allowedAt(1f) -> 1f
            else -> largestWhere(1f, needed, GROWTH_PRECISION, allowedAt)
        }
    }

    private fun measure(translated: TranslatedTextBlock, box: OrientedBox, fontPx: Float): TextLayoutResult =
        textMeasurer.measure(
            text = translated.translation,
            style = TextStyle(
                color = translated.textColor,
                fontSize = with(density) { fontPx.toSp() },
            ),
            constraints = Constraints(maxWidth = (box.width - 2 * paddingPx).toInt().coerceAtLeast(1)),
            density = density,
        )

    private fun fits(layout: TextLayoutResult, box: OrientedBox): Boolean =
        !layout.didOverflowWidth && layout.size.height <= box.height
}

private fun largestWhere(low: Float, high: Float, precision: Float, predicate: (Float) -> Boolean): Float {
    if (predicate(high)) return high
    var good = low
    var bad = high
    while (bad - good > precision) {
        val middle = (good + bad) / 2f
        if (predicate(middle)) good = middle else bad = middle
    }
    return good
}

private fun smallestWhere(low: Float, high: Float, precision: Float, predicate: (Float) -> Boolean): Float {
    if (predicate(low)) return low
    var bad = low
    var good = high
    while (good - bad > precision) {
        val middle = (good + bad) / 2f
        if (predicate(middle)) good = middle else bad = middle
    }
    return good
}

@Preview(widthDp = 360, heightDp = 640)
@Composable
private fun TextBlocksOverlayPreview() {
    AppTheme {
        TextBlocksOverlay(
            blocks = persistentListOf(
                TranslatedTextBlock(
                    block = RecognizedTextBlock(
                        text = "Salida",
                        bounds = Rect(left = 300f, top = 600f, right = 900f, bottom = 760f),
                        backgroundColor = Color(0xFF2E7D32),
                    ),
                    translation = "Exit",
                    textColor = Color.White,
                ),
                TranslatedTextBlock(
                    block = RecognizedTextBlock(
                        text = "Andén 3",
                        bounds = Rect(left = 400f, top = 960f, right = 1200f, bottom = 1240f),
                        corners = listOf(Offset(400f, 1100f), Offset(1170f, 960f), Offset(1200f, 1100f), Offset(430f, 1240f)),
                        lineHeight = 140f,
                        backgroundColor = Color(0xFFF5F0E6),
                    ),
                    translation = "Platform 3",
                    textColor = Color.Black,
                ),
                TranslatedTextBlock(
                    block = RecognizedTextBlock(
                        text = "PIN",
                        bounds = Rect(left = 300f, top = 1400f, right = 420f, bottom = 1440f),
                        lineHeight = 40f,
                        backgroundColor = Color.White,
                    ),
                    translation = "Персональный идентификационный номер",
                    textColor = Color.Black,
                ),
            ),
            imageSize = Size(1500f, 2000f),
            modifier = Modifier
                .fillMaxSize()
                .background(Color.DarkGray),
        )
    }
}
