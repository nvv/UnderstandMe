package com.vnamashko.understandme.translation.screen

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.vnamashko.understandme.translation.model.Language
import com.vnamashko.undertsndme.language.picker.LanguageFor
import com.vnamashko.undertsndme.language.picker.LanguageSelectionControl
import com.vnamashko.undertsndme.translation.screen.R

@Composable
fun SpeechListeningScreenScreen(
    partialResult: String?,
    targetLanguage: Language?,
    sourceLanguage: Language?,
    proposedSourceLanguage: Language?,
    selectForTarget: (LanguageFor) -> Unit,
    selectProposedLanguage: () -> Unit,
    flipLanguages: () -> Unit,
    onStartListening: () -> Unit,
    onStopListening: () -> Unit,
    modifier: Modifier = Modifier
) {
    LaunchedEffect(Unit) {
        onStartListening()
    }

    Column(
        modifier = modifier
            .padding(24.dp)
            .fillMaxSize()
    ) {
        Text(
            partialResult ?: stringResource(R.string.speak),
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.padding(16.dp)
        )

        Spacer(modifier = Modifier.weight(1f))
        LanguageSelectionControl(
            sourceLanguage = sourceLanguage,
            proposedSourceLanguage = proposedSourceLanguage,
            targetLanguage = targetLanguage,
            selectFor = selectForTarget,
            selectProposedLanguage = selectProposedLanguage,
            flipLanguages = flipLanguages
        )
        MicButton(isSpeechToTextListening = true, onClicked = onStopListening)
    }
}

@Preview(showBackground = true)
@Composable
fun SpeechListeningScreenPreview() {
    MaterialTheme {
        SpeechListeningScreenScreen(
            partialResult = null,
            targetLanguage = Language(code = "en", displayName = "English"),
            sourceLanguage = Language(code = "es", displayName = "Spanish"),
            proposedSourceLanguage = null,
            selectForTarget = {},
            selectProposedLanguage = {},
            flipLanguages = {},
            onStopListening = {},
            onStartListening = {}
        )
    }
}