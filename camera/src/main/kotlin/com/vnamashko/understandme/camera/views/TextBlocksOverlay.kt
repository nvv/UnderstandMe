package com.vnamashko.understandme.camera.views

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.vnamashko.understandme.camera.model.CropTransform
import com.vnamashko.understandme.camera.model.RecognizedTextBlock
import com.vnamashko.understandme.theme.AppTheme
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

@Composable
fun TextBlocksOverlay(
    blocks: ImmutableList<RecognizedTextBlock>,
    imageSize: Size,
    modifier: Modifier = Modifier,
) {
    Canvas(modifier = modifier) {
        val transform = CropTransform.of(imageSize = imageSize, viewSize = size)
        val stroke = Stroke(width = 2.dp.toPx(), join = StrokeJoin.Round)
        blocks.forEach { block ->
            val path = Path().apply {
                block.corners.map(transform::map).forEachIndexed { index, point ->
                    if (index == 0) moveTo(point.x, point.y) else lineTo(point.x, point.y)
                }
                close()
            }
            drawPath(path = path, color = Color.White.copy(alpha = 0.2f))
            drawPath(path = path, color = Color.White, style = stroke)
        }
    }
}

@Preview(widthDp = 360, heightDp = 640)
@Composable
private fun TextBlocksOverlayPreview() {
    AppTheme {
        TextBlocksOverlay(
            blocks = persistentListOf(
                RecognizedTextBlock("Salida", Rect(left = 300f, top = 600f, right = 900f, bottom = 760f)),
                RecognizedTextBlock(
                    text = "Andén 3",
                    bounds = Rect(left = 400f, top = 960f, right = 1200f, bottom = 1240f),
                    corners = listOf(Offset(400f, 1100f), Offset(1170f, 960f), Offset(1200f, 1100f), Offset(430f, 1240f)),
                ),
            ),
            imageSize = Size(1500f, 2000f),
            modifier = Modifier
                .fillMaxSize()
                .background(Color.DarkGray),
        )
    }
}
