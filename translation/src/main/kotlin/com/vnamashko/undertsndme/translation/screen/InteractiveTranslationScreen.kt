package com.vnamashko.undertsndme.translation.screen

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.vnamashko.understandme.translation.model.Language
import com.vnamashko.undertsndme.language.picker.LanguageFor
import com.vnamashko.undertsndme.language.picker.LanguageSelectionControl
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.debounce

@Composable
fun InteractiveTranslationScreen(
    initialText: String?,
    onTextChanged: (String) -> Unit,
    playbackOriginalText: () -> Unit,
    playbackTranslatedText: () -> Unit,
    translation: String?,
    targetLanguage: Language?,
    sourceLanguage: Language?,
    selectForTarget: (LanguageFor) -> Unit,
    flipLanguages: () -> Unit,
    isPasteAvailable: Boolean,
    error: TranslationError?,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .padding(24.dp)
            .fillMaxSize()
    ) {
        TranslationInputOutput(
            initialText = initialText,
            onTextChanged = onTextChanged,
            playbackOriginalText = playbackOriginalText,
            playbackTranslatedText = playbackTranslatedText,
            translation = translation,
            error = error,
            isPasteAvailable = isPasteAvailable
        )
        Spacer(modifier = Modifier.weight(1f))
        LanguageSelectionControl(
            sourceLanguage = sourceLanguage,
            targetLanguage = targetLanguage,
            selectFor = selectForTarget,
            flipLanguages = flipLanguages
        )
    }
}

@OptIn(FlowPreview::class)
@Composable
private fun TranslationInputOutput(
    initialText: String?,
    onTextChanged: (String) -> Unit,
    playbackOriginalText: () -> Unit,
    playbackTranslatedText: () -> Unit,
    translation: String?,
    error: TranslationError?,
    isPasteAvailable: Boolean,
    modifier: Modifier = Modifier
) {
    var textToTranslate by remember { mutableStateOf(initialText ?: "") }

    val focusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current
    val clipboardManager = LocalClipboardManager.current

    LaunchedEffect(textToTranslate) {
        snapshotFlow { textToTranslate }
            .debounce(250)
            .collect { text ->
                onTextChanged(text)
            }
    }

    LaunchedEffect(initialText) {
        textToTranslate = initialText ?: ""
        onTextChanged(textToTranslate)
    }

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
        keyboardController?.show()
    }

    Column {
        OutlinedTextField(
            value = textToTranslate,
            placeholder = {
                Text(
                    stringResource(R.string.enter_text),
                    style = MaterialTheme.typography.titleLarge
                )
            },
            onValueChange = { textToTranslate = it },
            colors = OutlinedTextFieldDefaults.colors(
                unfocusedBorderColor = Color.Transparent,
                focusedBorderColor = Color.Transparent,
            ),
            textStyle = MaterialTheme.typography.titleLarge,
            modifier = Modifier.focusRequester(focusRequester)
        )

        if (isPasteAvailable && textToTranslate.isEmpty()) {
            PasteButton(onClicked = { textToTranslate = clipboardManager.getText()?.text ?: "" })
        }

        if (translation != null) {
            TextToolbarControl(
                onPlaybackClicked = playbackOriginalText,
                onCopyClicked = { clipboardManager.setText(AnnotatedString(textToTranslate)) },
                tint = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(bottom = 12.dp)
            )
        }

        if (translation != null && error == null) {
            Column {
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
            }
        }

        if (error != null) {
            Column {
                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    HorizontalDivider(
                        color = MaterialTheme.colorScheme.primary,
                        thickness = 1.5.dp,
                        modifier = Modifier.fillMaxWidth(0.5f)
                    )
                }
                ErrorCard(error)
            }
        }

    }
}

@Preview(showBackground = true)
@Composable
fun InteractiveTranslationScreenPreview() {
    MaterialTheme {
        InteractiveTranslationScreen(
            initialText = "Hola, ¿cómo estás?",
            onTextChanged = {},
            playbackOriginalText = {},
            playbackTranslatedText = {},
            translation = "Hello, how are you?",
            targetLanguage = Language(code = "en", displayName = "English"),
            sourceLanguage = Language(code = "es", displayName = "Spanish"),
            selectForTarget = {},
            flipLanguages = {},
            isPasteAvailable = true,
            error = null
        )
    }
}


@Preview(showBackground = true)
@Composable
fun InteractiveTranslationScreenErrorPreview() {
    MaterialTheme {
        InteractiveTranslationScreen(
            initialText = "Hola, ¿cómo estás?",
            onTextChanged = {},
            playbackOriginalText = {},
            playbackTranslatedText = {},
            translation = "Hello, how are you?",
            targetLanguage = Language(code = "en", displayName = "English"),
            sourceLanguage = Language(code = "es", displayName = "Spanish"),
            selectForTarget = {},
            flipLanguages = {},
            isPasteAvailable = true,
            error = TranslationError(
                "Translation Error",
                "Language model not found. If it exists - try to delete and download it again.",
                "Retry",
                {}
                )
        )
    }
}
