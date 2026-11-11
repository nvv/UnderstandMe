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
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
import com.vnamashko.understandme.camera.model.PhotoSource
import com.vnamashko.understandme.camera.model.TranslatedTextBlock
import com.vnamashko.understandme.camera.views.GalleryButton
import com.vnamashko.understandme.camera.views.CameraPreview
import com.vnamashko.understandme.camera.views.rememberCameraPreviewState
import com.vnamashko.understandme.camera.views.ShutterButton
import com.vnamashko.understandme.camera.views.TranslatedPhoto
import com.vnamashko.understandme.camera.vm.CameraViewModel
import com.vnamashko.understandme.theme.AppTheme
import com.vnamashko.understandme.translation.model.LanguagePair
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

enum class CameraPermission {
    UNKNOWN,
    GRANTED,
    DENIED,
}

@Composable
fun CameraRoute(
    languagePair: LanguagePair?,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: CameraViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val previewState = rememberCameraPreviewState()

    var permission by rememberSaveable {
        mutableStateOf(if (context.hasCameraPermission()) CameraPermission.GRANTED else CameraPermission.UNKNOWN)
    }
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        permission = if (granted) CameraPermission.GRANTED else CameraPermission.DENIED
    }

    val imagePicker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        uri?.let(viewModel::onImagePicked)
    }

    LaunchedEffect(Unit) {
        if (permission == CameraPermission.UNKNOWN) {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    LaunchedEffect(languagePair) {
        viewModel.onLanguagePairChanged(languagePair)
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
        photoSource = state.photoSource,
        translatedBlocks = state.translatedBlocks,
        isProcessing = state.isProcessing,
        isModelMissing = state.isModelMissing,
        onTakePhoto = { previewState.capture()?.let(viewModel::onPhotoTaken) },
        onPickImage = {
            imagePicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
        },
        onBack = if (state.photo != null) viewModel::onPhotoDismissed else onBack,
        onOpenSettings = { context.openAppSettings() },
        cameraPreview = { CameraPreview(state = previewState, modifier = Modifier.fillMaxSize()) },
        modifier = modifier,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CameraScreen(
    permission: CameraPermission,
    photo: ImageBitmap?,
    photoSource: PhotoSource,
    translatedBlocks: ImmutableList<TranslatedTextBlock>,
    isProcessing: Boolean,
    isModelMissing: Boolean,
    onTakePhoto: () -> Unit,
    onPickImage: () -> Unit,
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
            if (photo != null) {
                TranslatedPhoto(
                    photo = photo,
                    translatedBlocks = translatedBlocks,
                    contentScale = if (photoSource == PhotoSource.GALLERY) ContentScale.Fit else ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
                if (isProcessing) {
                    CircularProgressIndicator(
                        color = Color.White,
                        modifier = Modifier.align(Alignment.Center),
                    )
                }
                if (isModelMissing) {
                    ModelMissingBanner(
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .padding(top = innerPadding.calculateTopPadding())
                            .padding(horizontal = 16.dp),
                    )
                }
            } else {
                when (permission) {
                    CameraPermission.GRANTED -> {
                        cameraPreview()
                        ShutterButton(
                            onClicked = onTakePhoto,
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
                if (permission != CameraPermission.UNKNOWN) {
                    GalleryButton(
                        onClicked = onPickImage,
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(start = 40.dp, bottom = innerPadding.calculateBottomPadding() + 36.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun ModelMissingBanner(modifier: Modifier = Modifier) {
    Surface(
        color = MaterialTheme.colorScheme.secondaryContainer,
        shape = RoundedCornerShape(12.dp),
        modifier = modifier,
    ) {
        Text(
            text = stringResource(R.string.camera_model_missing),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSecondaryContainer,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
        )
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
            photoSource = PhotoSource.CAMERA,
            translatedBlocks = persistentListOf(),
            isProcessing = false,
            isModelMissing = false,
            onTakePhoto = {},
            onPickImage = {},
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
            photoSource = PhotoSource.CAMERA,
            translatedBlocks = persistentListOf(),
            isProcessing = false,
            isModelMissing = false,
            onTakePhoto = {},
            onPickImage = {},
            onBack = {},
            onOpenSettings = {},
            cameraPreview = {},
        )
    }
}
