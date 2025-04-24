package com.vnamashko.understandme.translation.screen

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.vnamashko.understandme.translation.model.Language
import com.vnamashko.understandme.translation.model.Screen
import com.vnamashko.understandme.translation.vm.TranslateViewModel
import com.vnamashko.understandme.utils.coreui.activityViewModel
import com.vnamashko.undertsndme.language.picker.LanguageFor
import com.vnamashko.undertsndme.language.picker.LanguageSelectionControl
import com.vnamashko.undertsndme.translation.screen.R

@Composable
fun HomeScreen(
    isPasteAvailable: Boolean,
    selectForTarget: (LanguageFor) -> Unit,
    startListening: (Language?) -> Unit,
    navController: NavController,
    viewModel: TranslateViewModel = activityViewModel<TranslateViewModel>()
) {
    val sourceLanguage by viewModel.sourceLanguage.collectAsStateWithLifecycle()
    val targetLanguage by viewModel.targetLanguage.collectAsStateWithLifecycle()

    val clipboardManager = LocalClipboardManager.current
    val context = LocalContext.current

    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            startListening(sourceLanguage)
        }
    }

    HomeScreen(
        selectForTarget = selectForTarget,
        flipLanguages = {
            viewModel.flipLanguages()
        },
        sourceLanguage = sourceLanguage,
        targetLanguage = targetLanguage,
        isPasteAvailable = isPasteAvailable,
        goToInteractiveTranslation = {
            navController.navigate(Screen.InteractiveTranslate.route)
        },
        pasteToInteractiveTranslation = {
            viewModel.translate(clipboardManager.getText()?.text ?: "")
            navController.navigate(Screen.InteractiveTranslate.route)
        },
        listenButtonClicked = {
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
        },
        modifier = Modifier.fillMaxWidth()
    )
}

@Composable
fun HomeScreen(
    targetLanguage: Language?,
    sourceLanguage: Language?,
    selectForTarget: (LanguageFor) -> Unit,
    flipLanguages: () -> Unit,
    isPasteAvailable: Boolean,
    goToInteractiveTranslation: () -> Unit,
    pasteToInteractiveTranslation: () -> Unit,
    listenButtonClicked: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
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
            sourceLanguage = sourceLanguage,
            proposedSourceLanguage = null,
            targetLanguage = targetLanguage,
            selectFor = selectForTarget,
            selectProposedLanguage = {},
            flipLanguages = flipLanguages
        )
        MicButton(
            isSpeechToTextListening = false,
            isEnabled = targetLanguage != null && sourceLanguage != null,
            onClicked = listenButtonClicked
        )
    }
}

@Preview(showBackground = true)
@Composable
fun HomeScreenPreview() {
    MaterialTheme {
        HomeScreen(
            targetLanguage = Language(code = "en", displayName = "English"),
            sourceLanguage = Language(code = "es", displayName = "Spanish"),
            selectForTarget = {},
            flipLanguages = {},
            isPasteAvailable = true,
            goToInteractiveTranslation = {},
            pasteToInteractiveTranslation = {},
            listenButtonClicked = {}
        )
    }
}