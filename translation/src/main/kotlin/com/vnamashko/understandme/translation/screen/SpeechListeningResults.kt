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
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.vnamashko.understandme.translation.model.Language
import com.vnamashko.understandme.translation.views.MicButton
import com.vnamashko.understandme.translation.vm.TranslateViewModel
import com.vnamashko.understandme.utils.coreui.activityViewModel
import com.vnamashko.undertsndme.language.picker.LanguageSelectionActions
import com.vnamashko.undertsndme.language.picker.LanguageSelectionControl
import com.vnamashko.undertsndme.language.picker.LanguageSelectionState

/** Listening results destination: reads [TranslateViewModel] and wires [SpeechListeningResults]. */
@Composable
fun SpeechListeningResultsRoute(
    startListening: (Language?) -> Unit,
    onEditText: () -> Unit,
    languageActions: LanguageSelectionActions,
    modifier: Modifier = Modifier,
    viewModel: TranslateViewModel = activityViewModel(),
) {
    val originalText by viewModel.originalText.collectAsStateWithLifecycle()
    val translatedText by viewModel.translatedText.collectAsStateWithLifecycle()
    val languageState by viewModel.languageSelection.collectAsStateWithLifecycle()

    SpeechListeningResults(
        text = originalText,
        translation = translatedText ?: "",
        languageState = languageState,
        languageActions = languageActions,
        playbackOriginalText = viewModel::playbackOriginal,
        playbackTranslatedText = viewModel::playbackTranslated,
        onStartListening = { startListening(languageState.source) },
        editText = onEditText,
        modifier = modifier,
    )
}

@Composable
fun SpeechListeningResults(
    text: String,
    translation: String,
    languageState: LanguageSelectionState,
    languageActions: LanguageSelectionActions,
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
            state = languageState,
            actions = languageActions,
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
            languageState = LanguageSelectionState(
                source = Language(code = "es", displayName = "Spanish"),
                target = Language(code = "en", displayName = "English"),
            ),
            languageActions = LanguageSelectionActions.NoOp,
            playbackOriginalText = {},
            playbackTranslatedText = {},
            onStartListening = {},
            editText = {}
        )
    }
}