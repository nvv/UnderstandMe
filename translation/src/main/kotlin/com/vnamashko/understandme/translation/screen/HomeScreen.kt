package com.vnamashko.understandme.translation.screen

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.vnamashko.understandme.translation.model.Language
import com.vnamashko.understandme.translation.views.MicButton
import com.vnamashko.understandme.translation.views.PasteButton
import com.vnamashko.undertsndme.language.picker.LanguageFor
import com.vnamashko.undertsndme.language.picker.LanguageSelectionControl
import com.vnamashko.undertsndme.translation.screen.R

@Composable
fun HomeScreen(
    isPasteAvailable: Boolean,
    sourceLanguage: Language?,
    targetLanguage: Language?,
    selectForTarget: (LanguageFor) -> Unit,
    startListening: (Language?) -> Unit,
    flipLanguages: () -> Unit,
    goToInteractiveTranslation: () -> Unit,
    pasteToInteractiveTranslation: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

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
            startListening = {},
            goToInteractiveTranslation = {},
            pasteToInteractiveTranslation = {},
        )
    }
}