package com.vnamashko.understandme.translation.screen

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.vnamashko.understandme.translation.model.Language
import com.vnamashko.understandme.translation.views.MicButton
import com.vnamashko.undertsndme.language.picker.LanguageFor
import com.vnamashko.undertsndme.language.picker.LanguageSelectionControl

@Composable
fun SpeechListeningResults(
    text: String,
    translation: String,
    targetLanguage: Language?,
    sourceLanguage: Language?,
    proposedSourceLanguage: Language?,
    selectForTarget: (LanguageFor) -> Unit,
    selectProposedLanguage: () -> Unit,
    flipLanguages: () -> Unit,
    playbackOriginalText: () -> Unit,
    playbackTranslatedText: () -> Unit,
    onStartListening: () -> Unit,
    editText: () -> Unit,
    modifier: Modifier = Modifier
) {
    val clipboardManager = LocalClipboardManager.current

    Column(
        modifier = modifier
            .padding(24.dp)
            .fillMaxSize()
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.titleLarge,
            modifier = modifier.padding(16.dp).pointerInput(Unit) {
                detectTapGestures(onTap = { _ -> editText() })
            }
        )

        TextToolbarControl(
            onPlaybackClicked = playbackOriginalText,
            onCopyClicked = { clipboardManager.setText(AnnotatedString(text)) },
            tint = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            HorizontalDivider(
                color = MaterialTheme.colorScheme.primary,
                thickness = 1.5.dp,
                modifier = Modifier.fillMaxWidth(0.5f)
            )
        }

        Text(
            text = translation,
            style = MaterialTheme.typography.titleLarge.copy(color = MaterialTheme.colorScheme.primary),
            modifier = modifier.padding(16.dp)
        )

        TextToolbarControl(
            onPlaybackClicked = playbackTranslatedText,
            onCopyClicked = { clipboardManager.setText(AnnotatedString(translation)) },
            tint = MaterialTheme.colorScheme.primary
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
        MicButton(isSpeechToTextListening = false, onClicked = onStartListening)
    }
}

@Preview(showBackground = true)
@Composable
fun SpeechListeningResultsPreview() {
    MaterialTheme {
        SpeechListeningResults(
            text = "Hola, ¿cómo estás?",
            translation = "Hello, how are you?",
            targetLanguage = Language(code = "en", displayName = "English"),
            sourceLanguage = Language(code = "es", displayName = "Spanish"),
            proposedSourceLanguage = null,
            selectForTarget = {},
            selectProposedLanguage = {},
            flipLanguages = {},
            playbackOriginalText = {},
            playbackTranslatedText = {},
            onStartListening = {},
            editText = {}
        )
    }
}