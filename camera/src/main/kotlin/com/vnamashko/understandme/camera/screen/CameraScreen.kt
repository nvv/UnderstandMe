package com.vnamashko.understandme.camera.screen

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.view.CameraController
import androidx.camera.view.LifecycleCameraController
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import com.vnamashko.understandme.camera.R
import com.vnamashko.understandme.camera.model.RecognizedTextBlock
import com.vnamashko.understandme.camera.views.CameraPreview
import com.vnamashko.understandme.camera.views.ShutterButton
import com.vnamashko.understandme.camera.views.TextBlocksOverlay
import com.vnamashko.understandme.camera.vm.CameraViewModel
import com.vnamashko.understandme.theme.AppTheme
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

enum class CameraPermission {
    UNKNOWN,
    GRANTED,
    DENIED,
}

@Composable
fun CameraRoute(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: CameraViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val controller = remember {
        LifecycleCameraController(context).apply { setEnabledUseCases(CameraController.IMAGE_CAPTURE) }
    }

    var permission by rememberSaveable {
        mutableStateOf(if (context.hasCameraPermission()) CameraPermission.GRANTED else CameraPermission.UNKNOWN)
    }
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        permission = if (granted) CameraPermission.GRANTED else CameraPermission.DENIED
    }

    LaunchedEffect(Unit) {
        if (permission == CameraPermission.UNKNOWN) {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    LaunchedEffect(lifecycleOwner) {
        lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            if (context.hasCameraPermission()) {
                permission = CameraPermission.GRANTED
            }
        }
    }

    BackHandler(enabled = state.photo != null, onBack = viewModel::onPhotoDismissed)

    CameraScreen(
        permission = permission,
        photo = state.photo,
        isTakingPhoto = state.isTakingPhoto,
        textBlocks = state.textBlocks,
        isRecognizing = state.isRecognizing,
        onTakePhoto = {
            viewModel.onTakingPhoto()
            controller.takePicture(
                ContextCompat.getMainExecutor(context),
                object : ImageCapture.OnImageCapturedCallback() {
                    override fun onCaptureSuccess(image: ImageProxy) = viewModel.onPhotoTaken(image)
                    override fun onError(exception: ImageCaptureException) = viewModel.onPhotoFailed()
                },
            )
        },
        onBack = if (state.photo != null) viewModel::onPhotoDismissed else onBack,
        onOpenSettings = { context.openAppSettings() },
        cameraPreview = { CameraPreview(controller = controller, modifier = Modifier.fillMaxSize()) },
        modifier = modifier,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CameraScreen(
    permission: CameraPermission,
    photo: ImageBitmap?,
    isTakingPhoto: Boolean,
    textBlocks: ImmutableList<RecognizedTextBlock>,
    isRecognizing: Boolean,
    onTakePhoto: () -> Unit,
    onBack: () -> Unit,
    onOpenSettings: () -> Unit,
    cameraPreview: @Composable () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {},
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.padding(start = 16.dp)) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Default.ArrowBack,
                            contentDescription = stringResource(R.string.camera_back),
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                    navigationIconContentColor = Color.White,
                ),
            )
        },
        containerColor = Color.Black,
        modifier = modifier,
    ) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize()) {
            when (permission) {
                CameraPermission.GRANTED -> if (photo != null) {
                    Image(
                        bitmap = photo,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize(),
                    )
                    TextBlocksOverlay(
                        blocks = textBlocks,
                        imageSize = Size(photo.width.toFloat(), photo.height.toFloat()),
                        modifier = Modifier.fillMaxSize(),
                    )
                    if (isRecognizing) {
                        CircularProgressIndicator(
                            color = Color.White,
                            modifier = Modifier.align(Alignment.Center),
                        )
                    }
                } else {
                    cameraPreview()
                    ShutterButton(
                        onClicked = onTakePhoto,
                        isEnabled = !isTakingPhoto,
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = innerPadding.calculateBottomPadding() + 24.dp),
                    )
                }
                CameraPermission.DENIED -> CameraPermissionDenied(
                    onOpenSettings = onOpenSettings,
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(innerPadding)
                        .padding(24.dp),
                )
                CameraPermission.UNKNOWN -> Unit
            }
        }
    }
}

@Composable
private fun CameraPermissionDenied(onOpenSettings: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(
            text = stringResource(R.string.camera_permission_title),
            style = MaterialTheme.typography.titleLarge,
            color = Color.White,
            textAlign = TextAlign.Center,
        )
        Text(
            text = stringResource(R.string.camera_permission_message),
            style = MaterialTheme.typography.bodyMedium,
            color = Color.White,
            textAlign = TextAlign.Center,
        )
        Button(onClick = onOpenSettings) {
            Text(stringResource(R.string.camera_permission_open_settings))
        }
    }
}

private fun Context.hasCameraPermission(): Boolean =
    ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED

private fun Context.openAppSettings() {
    startActivity(
        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", packageName, null))
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
    )
}

@Preview
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun CameraScreenPreview() {
    AppTheme {
        CameraScreen(
            permission = CameraPermission.GRANTED,
            photo = null,
            isTakingPhoto = false,
            textBlocks = persistentListOf(),
            isRecognizing = false,
            onTakePhoto = {},
            onBack = {},
            onOpenSettings = {},
            cameraPreview = { Box(Modifier.fillMaxSize().background(Color.DarkGray)) },
        )
    }
}

@Preview
@Composable
private fun CameraScreenPermissionDeniedPreview() {
    AppTheme {
        CameraScreen(
            permission = CameraPermission.DENIED,
            photo = null,
            isTakingPhoto = false,
            textBlocks = persistentListOf(),
            isRecognizing = false,
            onTakePhoto = {},
            onBack = {},
            onOpenSettings = {},
            cameraPreview = {},
        )
    }
}
