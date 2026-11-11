package com.vnamashko.understandme.camera.views

import android.content.Context
import android.graphics.Bitmap
import androidx.camera.view.LifecycleCameraController
import androidx.camera.view.PreviewView
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.LocalLifecycleOwner

@Stable
class CameraPreviewState(context: Context) {
    internal val controller = LifecycleCameraController(context).apply { setEnabledUseCases(0) }
    internal var previewView: PreviewView? = null

    fun capture(): Bitmap? = previewView?.bitmap
}

@Composable
fun rememberCameraPreviewState(): CameraPreviewState {
    val context = LocalContext.current
    return remember { CameraPreviewState(context) }
}

@Composable
fun CameraPreview(
    state: CameraPreviewState,
    modifier: Modifier = Modifier,
) {
    val lifecycleOwner = LocalLifecycleOwner.current

    DisposableEffect(lifecycleOwner, state) {
        state.controller.bindToLifecycle(lifecycleOwner)
        onDispose {
            state.controller.unbind()
            state.previewView = null
        }
    }

    AndroidView(
        factory = { viewContext ->
            PreviewView(viewContext).apply {
                controller = state.controller
                state.previewView = this
            }
        },
        modifier = modifier,
    )
}
