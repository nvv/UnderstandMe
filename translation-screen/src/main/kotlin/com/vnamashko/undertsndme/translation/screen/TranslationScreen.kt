package com.vnamashko.undertsndme.translation.screen

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.vnamashko.undertsndme.language.picker.LanguageSelectionControl
import com.vnamashko.undertsndme.language.picker.LanguageFor
import kotlinx.coroutines.flow.debounce
import com.vnamashko.understandme.translation.model.Language
import kotlinx.coroutines.FlowPreview

@Composable
fun TranslationScreen(
    onTextChanged: (String) -> Unit,
    translation: String?,
    targetLanguage: Language?,
    sourceLanguage: Language?,
    selectForTarget: (LanguageFor) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxSize()) {
        TranslationInputOutput(onTextChanged, translation)
        Spacer(modifier = Modifier.weight(1f))
        LanguageSelectionControl(
            sourceLanguage = sourceLanguage,
            targetLanguage = targetLanguage,
            selectFor = selectForTarget
        )
    }
}

@OptIn(FlowPreview::class)
@Composable
fun TranslationInputOutput(
    onTextChanged: (String) -> Unit,
    translation: String?,
    modifier: Modifier = Modifier
) {
    var textToTranslate by remember { mutableStateOf("") }

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
                    "Enter text",
                    style = MaterialTheme.typography.titleLarge
                )
            },
            onValueChange = { textToTranslate = it },
            colors = OutlinedTextFieldDefaults.colors(
                unfocusedBorderColor = Color.Transparent,
                focusedBorderColor = Color.Transparent,
            ),
            textStyle = MaterialTheme.typography.titleLarge,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp)
        )

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
            }
        }

    }
}

@Preview(showBackground = true)
@Composable
fun TranslationPreview() {
    MaterialTheme {
    }
}
