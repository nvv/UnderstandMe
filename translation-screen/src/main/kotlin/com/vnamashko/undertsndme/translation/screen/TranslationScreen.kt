package com.vnamashko.undertsndme.translation.screen

import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import com.vnamashko.understandme.translation.model.Language
import com.vnamashko.undertsndme.language.picker.LanguageFor
import com.vnamashko.undertsndme.language.picker.LanguageSelectionControl
import com.vnamashko.undertsndme.translation.screen.icons.PasteIcon
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.debounce

@Composable
fun TranslationScreen(
    initialText: String?,
    onTextChanged: (String) -> Unit,
    playbackOriginalText: () -> Unit,
    playbackTranslatedText: () -> Unit,
    translation: String?,
    targetLanguage: Language?,
    sourceLanguage: Language?,
    selectForTarget: (LanguageFor) -> Unit,
    flipLanguages: () -> Unit,
    error: TranslationError?,
    modifier: Modifier = Modifier
) {
    val focusRequester = remember { FocusRequester() }

    Column(
        modifier = modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectTapGestures(onTap = { _ -> focusRequester.requestFocus() })
            }
    ) {
        TranslationInputOutput(
            initialText = initialText,
            onTextChanged = onTextChanged,
            playbackOriginalText = playbackOriginalText,
            playbackTranslatedText = playbackTranslatedText,
            translation = translation,
            error = error,
            inputModifier = Modifier
                .fillMaxWidth()
                .focusRequester(focusRequester)
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
fun TranslationInputOutput(
    initialText: String?,
    onTextChanged: (String) -> Unit,
    playbackOriginalText: () -> Unit,
    playbackTranslatedText: () -> Unit,
    translation: String?,
    error: TranslationError?,
    modifier: Modifier = Modifier,
    inputModifier: Modifier = Modifier
) {
    var textToTranslate by remember { mutableStateOf(initialText ?: "") }
    var isPasteAvailable by remember { mutableStateOf(false) }

    val clipboardManager = LocalClipboardManager.current
    val lifecycleOwner = LocalLifecycleOwner.current

    LaunchedEffect(lifecycleOwner) {
        lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            delay(100)
            isPasteAvailable = clipboardManager.hasText()
        }
    }

    LaunchedEffect(textToTranslate) {
        snapshotFlow { textToTranslate }
            .debounce(250)
            .collect { text ->
                onTextChanged(text)
            }
    }

    Column(modifier = modifier.padding(8.dp)) {
        Row(modifier = Modifier.height(24.dp)) {
            Spacer(Modifier.weight(1f))

            if (translation != null) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Clear",
                    modifier = Modifier.clickable {
                        textToTranslate = ""
                        onTextChanged("")
                    }
                )
            }
        }

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
            modifier = inputModifier
        )

        if (isPasteAvailable && textToTranslate.isEmpty()) {
            Button(
                onClick = { textToTranslate = clipboardManager.getText()?.text ?: "" },
                modifier = Modifier.padding(top = 24.dp, start = 16.dp)
            ) {
                Icon(imageVector = PasteIcon, contentDescription = "Paste")
                Spacer(modifier = Modifier.width(8.dp))
                Text(stringResource(R.string.paste_from_clipboard))
            }
        }

        if (translation != null) {
            TextToolbarControl(
                onPlaybackClicked = playbackOriginalText,
                onCopyClicked = { clipboardManager.setText(AnnotatedString(textToTranslate)) },
                tint = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(bottom = 12.dp)
            )
        }

        if (translation != null) {
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

@Composable
fun ErrorCard(error: TranslationError) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.error)
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
        ) {
            Text(
                text = error.title,
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onError
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = error.subtitle,
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Start,
                color = MaterialTheme.colorScheme.onError
            )

            if (error.actionText != null && error.onActionClick != null) {
                Spacer(modifier = Modifier.height(16.dp))

                TextButton(modifier = Modifier.align(Alignment.End), onClick = error.onActionClick) {
                    Text(
                        text = error.actionText,
                        color = MaterialTheme.colorScheme.onError
                    )
                }
            }
        }
    }
}

data class TranslationError(
    val title: String,
    val subtitle: String,
    val actionText: String?,
    val onActionClick: (() -> Unit)?
)

@Preview(showBackground = true)
@Composable
fun TranslationPreview() {
    MaterialTheme {
    }
}
