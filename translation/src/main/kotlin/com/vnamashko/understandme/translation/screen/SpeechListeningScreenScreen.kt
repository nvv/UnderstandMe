package com.vnamashko.understandme.translation.screen

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.vnamashko.understandme.translation.model.Language
import com.vnamashko.understandme.translation.views.MicButton
import com.vnamashko.understandme.translation.views.TranslationTopBar
import com.vnamashko.understandme.translation.vm.TranslateViewModel
import com.vnamashko.understandme.utils.coreui.activityViewModel
import com.vnamashko.undertsndme.language.picker.LanguageSelectionActions
import com.vnamashko.undertsndme.language.picker.LanguageSelectionControl
import com.vnamashko.undertsndme.language.picker.LanguageSelectionState
import com.vnamashko.undertsndme.translation.screen.R
import kotlinx.coroutines.flow.Flow

/**
 * Listening destination: reads [TranslateViewModel] and wires [SpeechListeningScreenScreen].
 * The recognizer is owned by the Activity; the host passes its partial results as a plain [Flow]
 * (referencing `android.speech` types in this file would break its previews).
 */
@Composable
fun SpeechListeningRoute(
    partialResults: Flow<String?>,
    startListening: (Language?) -> Unit,
    stopListening: () -> Unit,
    languageActions: LanguageSelectionActions,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: TranslateViewModel = activityViewModel(),
) {
    val partialResult by partialResults.collectAsStateWithLifecycle(initialValue = null)
    val languageState by viewModel.languageSelection.collectAsStateWithLifecycle()

    SpeechListeningScreenScreen(
        partialResult = partialResult,
        languageState = languageState,
        languageActions = languageActions,
        onStartListening = { startListening(languageState.source) },
        onStopListening = stopListening,
        onBack = onBack,
        modifier = modifier,
    )
}

@Composable
fun SpeechListeningScreenScreen(
    partialResult: String?,
    languageState: LanguageSelectionState,
    languageActions: LanguageSelectionActions,
    onStartListening: () -> Unit,
    onStopListening: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    LaunchedEffect(Unit) {
        onStartListening()
    }

    Scaffold(
        topBar = { TranslationTopBar(onBack = onBack) },
        modifier = modifier,
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
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
                state = languageState,
                actions = languageActions,
            )
            MicButton(isSpeechToTextListening = true, onClicked = onStopListening)
        }
    }
}

@Preview(showBackground = true)
@Composable
fun SpeechListeningScreenPreview() {
    MaterialTheme {
        SpeechListeningScreenScreen(
            partialResult = null,
            languageState = LanguageSelectionState(
                source = Language(code = "es", displayName = "Spanish"),
                target = Language(code = "en", displayName = "English"),
            ),
            languageActions = LanguageSelectionActions.NoOp,
            onStopListening = {},
            onStartListening = {},
            onBack = {},
        )
    }
}