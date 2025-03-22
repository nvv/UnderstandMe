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
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vnamashko.undertsndme.language.picker.LanguageSelectionControl
import com.vnamashko.undertsndme.language.picker.LanguageFor
import kotlinx.coroutines.flow.debounce
import com.vnamashko.understandme.translation.model.Language

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

@Composable
fun TranslationInputOutput(
    onTextChanged: (String) -> Unit,
    translation: String?,
    modifier: Modifier = Modifier
) {
    var value by remember { mutableStateOf("") }

    LaunchedEffect(value) {
        snapshotFlow { value }
            .debounce(250)
            .collect { text ->
                onTextChanged(text)
            }
    }

    Column(modifier = modifier.padding(8.dp)) {
        OutlinedTextField(
            value = value,
            placeholder = {
                Text(
                    "Enter text",
                    style = TextStyle(color = MaterialTheme.colorScheme.secondary),
                    fontSize = 18.sp
                )
            },
            onValueChange = { value = it },
            colors = androidx.compose.material3.OutlinedTextFieldDefaults.colors(
                unfocusedBorderColor = Color.Transparent,
                focusedBorderColor = Color.Transparent,
            ),
            modifier = Modifier
                .fillMaxWidth()
        )

        if (translation != null) {
            Column {
                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    HorizontalDivider(
                        color = MaterialTheme.colorScheme.tertiary,
                        thickness = 1.dp,
                        modifier = Modifier.fillMaxWidth(0.6f)
                    )
                }

                Text(text = translation)
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
