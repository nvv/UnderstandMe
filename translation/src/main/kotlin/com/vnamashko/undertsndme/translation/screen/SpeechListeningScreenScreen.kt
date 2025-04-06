package com.vnamashko.undertsndme.translation.screen

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.vnamashko.understandme.translation.model.Language
import com.vnamashko.undertsndme.language.picker.LanguageFor
import com.vnamashko.undertsndme.language.picker.LanguageSelectionControl

@Composable
fun SpeechListeningScreenScreen(
    targetLanguage: Language?,
    sourceLanguage: Language?,
    selectForTarget: (LanguageFor) -> Unit,
    flipLanguages: () -> Unit,
    onStopListening: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .padding(24.dp)
            .fillMaxSize()
    ) {
        Text(
            stringResource(R.string.speak),
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.padding(16.dp)
        )

        Spacer(modifier = Modifier.weight(1f))
        LanguageSelectionControl(
            sourceLanguage = sourceLanguage,
            targetLanguage = targetLanguage,
            selectFor = selectForTarget,
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
            targetLanguage = Language(code = "en", displayName = "English"),
            sourceLanguage = Language(code = "es", displayName = "Spanish"),
            selectForTarget = {},
            flipLanguages = {},
            onStopListening = {}
        )
    }
}