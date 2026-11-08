package com.vnamashko.understandme.translation.screen

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.vnamashko.understandme.camera.views.CameraButton
import com.vnamashko.understandme.translation.model.Language
import com.vnamashko.understandme.translation.views.MicButton
import com.vnamashko.understandme.translation.views.PasteButton
import com.vnamashko.understandme.translation.views.TranslationTopBar
import com.vnamashko.understandme.translation.views.rememberIsPasteAvailable
import com.vnamashko.understandme.translation.vm.TranslateViewModel
import com.vnamashko.understandme.utils.coreui.activityViewModel
import com.vnamashko.undertsndme.language.picker.LanguageSelectionActions
import com.vnamashko.undertsndme.language.picker.LanguageSelectionControl
import com.vnamashko.undertsndme.language.picker.LanguageSelectionState
import com.vnamashko.undertsndme.translation.screen.R

/** Home destination: reads [TranslateViewModel] and wires [HomeScreen]. Navigation comes from the host. */
@Composable
fun HomeRoute(
    languageActions: LanguageSelectionActions,
    onOpenInteractiveTranslation: () -> Unit,
    onOpenListening: () -> Unit,
    onOpenCamera: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: TranslateViewModel = activityViewModel(),
) {
    val clipboardManager = LocalClipboardManager.current
    val context = LocalContext.current
    val isCameraAvailable = remember {
        context.packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA_ANY)
    }
    val languageState by viewModel.languageSelection.collectAsStateWithLifecycle()

    val isDownloadingModel by viewModel.isDownloadingModel.collectAsStateWithLifecycle()

    HomeScreen(
        isDownloadingModel = isDownloadingModel,
        isPasteAvailable = rememberIsPasteAvailable(),
        languageState = languageState,
        languageActions = languageActions,
        startListening = { onOpenListening() },
        goToInteractiveTranslation = onOpenInteractiveTranslation,
        pasteToInteractiveTranslation = {
            viewModel.translate(clipboardManager.getText()?.text ?: "")
            onOpenInteractiveTranslation()
        },
        isCameraAvailable = isCameraAvailable,
        openCamera = onOpenCamera,
        modifier = modifier,
    )
}

@Composable
fun HomeScreen(
    isDownloadingModel: Boolean,
    isPasteAvailable: Boolean,
    languageState: LanguageSelectionState,
    languageActions: LanguageSelectionActions,
    startListening: (Language?) -> Unit,
    goToInteractiveTranslation: () -> Unit,
    pasteToInteractiveTranslation: () -> Unit,
    isCameraAvailable: Boolean,
    openCamera: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val sourceLanguage = languageState.source

    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            startListening(sourceLanguage)
        }
    }

    val listenButtonClicked = {
        when (PackageManager.PERMISSION_GRANTED) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.RECORD_AUDIO
            ) -> {
                startListening(sourceLanguage)
            }

            else -> {
                launcher.launch(Manifest.permission.RECORD_AUDIO)
            }
        }
    }

    Scaffold(
        topBar = { TranslationTopBar(onBack = null, isLoading = isDownloadingModel) },
        modifier = modifier,
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .padding(24.dp)
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectTapGestures(onTap = { _ -> goToInteractiveTranslation() })
                }
        ) {
            Text(
                stringResource(R.string.enter_text),
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(16.dp)
            )

            if (isPasteAvailable) {
                PasteButton(onClicked = pasteToInteractiveTranslation)
            }

            Spacer(modifier = Modifier.weight(1f))
            LanguageSelectionControl(
                // Home never suggests a detected language.
                state = languageState.copy(proposedSource = null),
                actions = languageActions,
            )
            val languagesSelected = languageState.target != null && sourceLanguage != null
            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                MicButton(
                    isSpeechToTextListening = false,
                    isEnabled = languagesSelected,
                    onClicked = listenButtonClicked
                )
                if (isCameraAvailable) {
                    CameraButton(
                        onClicked = openCamera,
                        isEnabled = languagesSelected,
                        modifier = Modifier
                            .align(Alignment.CenterEnd)
                            .padding(end = 16.dp),
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun HomeScreenPreview() {
    MaterialTheme {
        HomeScreen(
            isDownloadingModel = false,
            languageState = LanguageSelectionState(
                source = Language(code = "es", displayName = "Spanish"),
                target = Language(code = "en", displayName = "English"),
            ),
            languageActions = LanguageSelectionActions.NoOp,
            isPasteAvailable = true,
            startListening = {},
            goToInteractiveTranslation = {},
            pasteToInteractiveTranslation = {},
            isCameraAvailable = true,
            openCamera = {},
        )
    }
}