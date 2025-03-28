package com.vnamashko.understandme

import android.content.Intent
import android.os.Bundle
import android.speech.tts.TextToSpeech
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.vnamashko.understandme.language.picker.R.string.translate_from
import com.vnamashko.understandme.language.picker.R.string.translate_to
import com.vnamashko.understandme.ui.theme.UnderstandMeTheme
import com.vnamashko.undertsndme.language.picker.LanguageFor
import com.vnamashko.undertsndme.language.picker.LanguagePickerControl
import com.vnamashko.undertsndme.translation.screen.TranslationError
import com.vnamashko.undertsndme.translation.screen.TranslationScreen
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    private val viewModel: ViewModel by viewModels()

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val value by viewModel.translatedText.collectAsStateWithLifecycle()
            val languages by viewModel.supportedLanguages.collectAsStateWithLifecycle()
            val sourceLanguage by viewModel.sourceLanguage.collectAsStateWithLifecycle()
            val targetLanguage by viewModel.targetLanguage.collectAsStateWithLifecycle()
            val downloadedLanguages by viewModel.downloadedLanguages.collectAsStateWithLifecycle()

            var selectFor by remember { mutableStateOf<LanguageFor?>(null) }

            val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
            val scope = rememberCoroutineScope()

            var errorState by remember { mutableStateOf<TranslationError?>(null) }

            val snackbarHostState = remember { SnackbarHostState() }
            var showBottomSheet by remember { mutableStateOf(false) }

            UnderstandMeTheme {
                Scaffold(
                    snackbarHost = {
                        SnackbarHost(hostState = snackbarHostState)
                    },
                    modifier = Modifier.fillMaxSize()
                ) { innerPadding ->
                    TranslationScreen(
                        onTextChanged = {
                            viewModel.translate(it)
                        },
                        playbackOriginalText = {
                            viewModel.playbackOriginal()
                        },
                        playbackTranslatedText = {
                            viewModel.playbackTranslated()
                        },
                        translation = value,
                        selectForTarget = { target ->
                            selectFor = target
                            showBottomSheet = true
                        },
                        sourceLanguage = sourceLanguage,
                        targetLanguage = targetLanguage,
                        error = errorState,
                        modifier = Modifier
                            .padding(innerPadding)
                            .fillMaxWidth()
                    )

                    if (showBottomSheet) {
                        ModalBottomSheet(
                            onDismissRequest = {
                                selectFor = null
                                showBottomSheet = false
                            },
                            sheetState = sheetState,
                            dragHandle = null,
                            modifier = Modifier.fillMaxSize()
                        ) {
                            LanguagePickerControl(
                                supportedLanguages = languages,
                                selectedLanguage = when (selectFor) {
                                    LanguageFor.SOURCE -> sourceLanguage
                                    LanguageFor.TARGET -> targetLanguage
                                    null -> null
                                },
                                onSelect = {
                                    scope.launch { sheetState.hide() }.invokeOnCompletion {
                                        if (!sheetState.isVisible) {
                                            showBottomSheet = false
                                        }
                                    }

                                    when (selectFor) {
                                        LanguageFor.SOURCE -> viewModel.selectSourceLanguage(it)
                                        LanguageFor.TARGET -> viewModel.selectTargetLanguage(it)
                                        null -> {}
                                    }
                                },
                                searchTitle = when (selectFor) {
                                    LanguageFor.SOURCE -> stringResource(translate_from)
                                    LanguageFor.TARGET -> stringResource(translate_to)
                                    null -> ""
                                }
                            )
                        }
                    }
                }
            }

            LaunchedEffect(Unit) {
                viewModel.effect.collect { effect ->
                    when (effect) {
//                        is UiEffect.RequestLanguageDownload -> {
//                            val intent = Intent(TextToSpeech.Engine.ACTION_INSTALL_TTS_DATA)
//                            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
//                            startActivity(intent)
//                        }
                        is UiEffect.LanguageModelDoesNotExists -> {
                            errorState = TranslationError(
                                title = getString(R.string.error_translating_title),
                                subtitle = getString(
                                    if (effect.isInternetAvailable) {
                                        R.string.language_model_not_found
                                    } else {
                                        R.string.language_model_not_found_no_internet
                                    }
                                ),
                                actionText = getString(R.string.error_try_again),
                                onActionClick = {}
                            )
                        }

                        is UiEffect.ErrorWhileTranslatingMessage -> {
                            scope.launch {
                                snackbarHostState
                                    .showSnackbar(
                                        message = getString(R.string.error_translating_text),
                                        duration = SnackbarDuration.Long
                                    )
                            }
                        }
                    }
                }
            }
        }
    }
}

