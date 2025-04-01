package com.vnamashko.understandme

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
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
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.vnamashko.understandme.language.picker.R.string.translate_from
import com.vnamashko.understandme.language.picker.R.string.translate_to
import com.vnamashko.understandme.stt.SpeechRecognitionListener
import com.vnamashko.understandme.ui.theme.UnderstandMeTheme
import com.vnamashko.understandme.utils.getLanguageTag
import com.vnamashko.undertsndme.language.picker.LanguageFor
import com.vnamashko.undertsndme.language.picker.LanguagePickerControl
import com.vnamashko.undertsndme.translation.screen.TranslationError
import com.vnamashko.undertsndme.translation.screen.TranslationScreen
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch


@AndroidEntryPoint
class TranslateActivity : ComponentActivity() {
    private val viewModel: ViewModel by viewModels()

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val listener: SpeechRecognitionListener?

        val speechRecognizer = if (SpeechRecognizer.isRecognitionAvailable(this)) {
            listener = SpeechRecognitionListener()
            SpeechRecognizer.createSpeechRecognizer(this).apply {
                setRecognitionListener(listener)
            }
        } else {
            listener = null
            null
        }

        enableEdgeToEdge()
        setContent {
            val value by viewModel.translatedText.collectAsStateWithLifecycle()
            val languages by viewModel.supportedLanguages.collectAsStateWithLifecycle()
            val sourceLanguage by viewModel.sourceLanguage.collectAsStateWithLifecycle()
            val targetLanguage by viewModel.targetLanguage.collectAsStateWithLifecycle()
            val speechToText by listener?.result?.collectAsStateWithLifecycle()
                ?: remember { mutableStateOf<String?>(null) }

            var selectFor by remember { mutableStateOf<LanguageFor?>(null) }

            val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
            val scope = rememberCoroutineScope()

            var errorState by remember { mutableStateOf<TranslationError?>(null) }

            val snackbarHostState = remember { SnackbarHostState() }
            var showBottomSheet by remember { mutableStateOf(false) }

            println(">>>> $speechToText")
            UnderstandMeTheme {
                Scaffold(
                    snackbarHost = {
                        SnackbarHost(hostState = snackbarHostState)
                    },
                    modifier = Modifier.fillMaxSize()
                ) { innerPadding ->
                    TranslationScreen(
                        initialText = speechToText ?: intent.getStringExtra(Intent.EXTRA_PROCESS_TEXT),
                        onTextChanged = {
                            viewModel.translate(it)
                        },
                        playbackOriginalText = {
                            viewModel.playbackOriginal()
                        },
                        playbackTranslatedText = {
                            viewModel.playbackTranslated()
                        },
                        onSttRequested = {
                            if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
                                ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.RECORD_AUDIO), 1)
                            } else {
                                val recognizerIntent =
                                    Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                                        putExtra(
                                            RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                                            RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
                                        )
                                        putExtra(
                                            RecognizerIntent.EXTRA_LANGUAGE,
                                            getLanguageTag(sourceLanguage?.code ?: return@apply)
                                        )
                                    }


                                speechRecognizer?.startListening(recognizerIntent)
                            }
                        },
                        translation = value,
                        selectForTarget = { target ->
                            selectFor = target
                            showBottomSheet = true
                        },
                        flipLanguages = {
                            viewModel.flipLanguages()
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
                        is UiEffect.RequestLanguageDownload -> {
                            val intent = Intent(TextToSpeech.Engine.ACTION_INSTALL_TTS_DATA)
                            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                            startActivity(intent)
                        }
                        is UiEffect.ClearError -> { errorState = null }

                        is UiEffect.LanguageModelDoesNotExists -> { errorState = effect.error }

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

