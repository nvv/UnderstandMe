package com.vnamashko.understandme.camera.views

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.key
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.tooling.preview.Preview
import com.vnamashko.understandme.camera.model.RecognizedTextBlock
import com.vnamashko.understandme.camera.model.TranslatedTextBlock
import com.vnamashko.understandme.theme.AppTheme
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import me.saket.telephoto.zoomable.ZoomSpec
import me.saket.telephoto.zoomable.ZoomableContentLocation
import me.saket.telephoto.zoomable.rememberZoomableState
import me.saket.telephoto.zoomable.zoomable

private const val MAX_ZOOM_FACTOR = 8f

@Composable
fun TranslatedPhoto(
    photo: ImageBitmap,
    translatedBlocks: ImmutableList<TranslatedTextBlock>,
    contentScale: ContentScale,
    modifier: Modifier = Modifier,
) {
    val imageSize = Size(photo.width.toFloat(), photo.height.toFloat())
    key(photo) {
        val zoomableState = rememberZoomableState(zoomSpec = ZoomSpec(maxZoomFactor = MAX_ZOOM_FACTOR))
        LaunchedEffect(imageSize, contentScale) {
            if (contentScale == ContentScale.Fit) {
                zoomableState.setContentLocation(ZoomableContentLocation.scaledToFitAndCenterAligned(imageSize))
            }
        }
        Box(modifier = modifier.zoomable(zoomableState)) {
            Image(
                bitmap = photo,
                contentDescription = null,
                contentScale = contentScale,
                modifier = Modifier.fillMaxSize(),
            )
            TextBlocksOverlay(
                blocks = translatedBlocks,
                imageSize = imageSize,
                contentScale = contentScale,
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}

@Preview(widthDp = 360, heightDp = 640)
@Composable
private fun TranslatedPhotoPreview() {
    AppTheme {
        TranslatedPhoto(
            photo = ImageBitmap(1500, 2000),
            translatedBlocks = persistentListOf(
                TranslatedTextBlock(
                    block = RecognizedTextBlock(
                        text = "Salida",
                        bounds = Rect(left = 300f, top = 600f, right = 900f, bottom = 760f),
                        backgroundColor = Color(0xFF2E7D32),
                    ),
                    translation = "Exit",
                    textColor = Color.White,
                ),
            ),
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .fillMaxSize()
                .background(Color.DarkGray),
        )
    }
}
