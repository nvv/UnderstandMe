package com.vnamashko.understandme.translation.screen

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.vnamashko.understandme.language.picker.R.string.translate_from
import com.vnamashko.understandme.language.picker.R.string.translate_to
import com.vnamashko.understandme.stt.RecognitionResult
import com.vnamashko.understandme.stt.SpeechRecognitionListener
import com.vnamashko.understandme.translation.model.Language
import com.vnamashko.understandme.translation.model.Screen
import com.vnamashko.understandme.translation.vm.TranslateViewModel
import com.vnamashko.understandme.translation.vm.UiEffect
import com.vnamashko.understandme.utils.coreui.activityViewModel
import com.vnamashko.undertsndme.language.picker.DeleteModelDialog
import com.vnamashko.undertsndme.language.picker.DownloadModelDialog
import com.vnamashko.undertsndme.language.picker.LanguageFor
import com.vnamashko.undertsndme.language.picker.LanguagePickerControl
import com.vnamashko.undertsndme.translation.screen.R.string.error_translating_text
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TranslationHostScreen(
    startListening: (Language?) -> Unit,
    stopListening: () -> Unit,
    destroyListener: () -> Unit,
    speechRecognitionListener: SpeechRecognitionListener?,
    viewModel: TranslateViewModel = activityViewModel<TranslateViewModel>()
) {
    val supportedModels by viewModel.supportedModels.collectAsStateWithLifecycle()
    val recentLanguage by viewModel.recentLanguages.collectAsStateWithLifecycle()
    val sourceLanguage by viewModel.sourceLanguage.collectAsStateWithLifecycle()
    val targetLanguage by viewModel.targetLanguage.collectAsStateWithLifecycle()

    var selectFor by remember { mutableStateOf<LanguageFor?>(null) }

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()

    var errorState by remember { mutableStateOf<TranslationError?>(null) }

    val snackbarHostState = remember { SnackbarHostState() }
    var showBottomSheet by remember { mutableStateOf(false) }

    val navController = rememberNavController()

    val clipboardManager = LocalClipboardManager.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var openDeleteModelDialogForLanguage by remember { mutableStateOf<Language?>(null) }
    var openDownloadModelDialogForLanguage by remember { mutableStateOf<Language?>(null) }

    var isPasteAvailable by remember { mutableStateOf(false) }

    val hasBackNavigation by navController.currentBackStackEntryFlow.map { it.destination.route != Screen.Home.route }
        .collectAsStateWithLifecycle(false)

    val hasClearIcon by combine(
        navController.currentBackStackEntryFlow,
        viewModel.originalText
    ) { nav, text ->
        nav.destination.route == Screen.InteractiveTranslate.route && text.isNotEmpty()
    }.collectAsStateWithLifecycle(false)

    val selectForTarget = { target: LanguageFor? ->
        selectFor = target
        showBottomSheet = true
    }

    Scaffold(
        snackbarHost = {
            SnackbarHost(hostState = snackbarHostState)
        },
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (hasClearIcon) {
                            Spacer(Modifier.weight(1f))
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Clear",
                                modifier = Modifier.clickable(
                                    onClick = {
                                        viewModel.translate("")
                                    },
                                    indication = null,
                                    interactionSource = remember { MutableInteractionSource() }
                                )
                            )
                        }
                    }
                },
                navigationIcon = {
                    if (hasBackNavigation) {
                        IconButton(
                            onClick = { navController.popBackStack() },
                            modifier = Modifier.padding(start = 16.dp)
                        ) {
                            Icon(
                                Icons.AutoMirrored.Default.ArrowBack,
                                contentDescription = "Back"
                            )
                        }
                    }
                }
            )
        },
        modifier = Modifier.fillMaxSize()
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Home.route,
            enterTransition = { EnterTransition.None },
            exitTransition = { ExitTransition.None },
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Screen.Home.route) {
                HomeScreen(
                    selectForTarget = selectForTarget,
                    sourceLanguage = sourceLanguage,
                    targetLanguage = targetLanguage,
                    isPasteAvailable = isPasteAvailable,
                    flipLanguages = viewModel::flipLanguages,
                    startListening = startListening,
                    pasteToInteractiveTranslation = {
                        viewModel.translate(clipboardManager.getText()?.text ?: "")
                        navController.navigate(Screen.InteractiveTranslate.route)
                    },
                    goToInteractiveTranslation = {
                        navController.navigate(Screen.InteractiveTranslate.route)
                    }
                )
            }
            composable(Screen.InteractiveTranslate.route) {
                InteractiveTranslationScreen(
                    selectForTarget = selectForTarget,
                    isPasteAvailable = isPasteAvailable,
                    errorState = errorState,
                )
            }
            composable(Screen.Listen.route) {
                SpeechListeningScreenScreen(
                    selectForTarget = selectForTarget,
                    stopListening = stopListening
                )
            }
            composable(Screen.ListenResults.route) {
                SpeechListeningResults(
                    selectForTarget = selectForTarget,
                    startListening = startListening,
                    navController = navController
                )
            }
        }

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
                    supportedModels = supportedModels,
                    recentLanguage = recentLanguage,
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
                    },
                    onDownloadLanguageModel = {
                        openDownloadModelDialogForLanguage = it
                    },
                    onDeleteLanguageModel = {
                        openDeleteModelDialogForLanguage = it
                    }
                )
            }
        }

        val deleteLanguage = openDeleteModelDialogForLanguage
        if (deleteLanguage != null) {
            DeleteModelDialog(
                onDismissRequest = { openDeleteModelDialogForLanguage = null },
                onConfirmation = {
                    viewModel.deleteModelForLanguage(deleteLanguage)
                    openDeleteModelDialogForLanguage = null
                },
                languageString = deleteLanguage.displayName,
            )
        }

        val downloadLanguage = openDownloadModelDialogForLanguage
        if (downloadLanguage != null) {
            DownloadModelDialog(
                onDismissRequest = { openDownloadModelDialogForLanguage = null },
                onConfirmation = {
                    viewModel.downloadModelForLanguage(downloadLanguage)
                    openDownloadModelDialogForLanguage = null
                },
                languageString = downloadLanguage.displayName,
            )
        }

        LaunchedEffect(lifecycleOwner) {
            lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
                delay(100)
                isPasteAvailable = clipboardManager.hasText()
            }
        }

        LaunchedEffect(Unit) {
            // TODO

//            intent.getStringExtra(Intent.EXTRA_PROCESS_TEXT)?.let {
//                viewModel.translate(it)
//                navController.navigate(Screen.InteractiveTranslate.route)
//            }
        }

        // TODO
        val errorMessage = stringResource(error_translating_text)
        LaunchedEffect(Unit) {
            viewModel.effect.collect { effect ->
                when (effect) {
                    is UiEffect.RequestLanguageDownload -> {
                        // TODO:
                        //val intent = Intent(TextToSpeech.Engine.ACTION_INSTALL_TTS_DATA)
                        //intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                        //startActivity(intent)
                    }
                    is UiEffect.ClearError -> { errorState = null }

                    is UiEffect.LanguageModelDoesNotExists -> { errorState = effect.error }

                    is UiEffect.ErrorWhileTranslatingMessage -> {
                        scope.launch {
                            snackbarHostState
                                .showSnackbar(
                                    message = errorMessage,
                                    duration = SnackbarDuration.Long
                                )
                        }
                    }
                }
            }
        }

        LaunchedEffect(Unit) {
            speechRecognitionListener?.result?.filterNotNull()?.collect {
                when (it) {
                    RecognitionResult.Listening -> {
                        if (navController.currentBackStackEntry?.destination?.route == Screen.ListenResults.route) {
                            navController.popBackStack()
                        }
                        navController.navigate(Screen.Listen.route)
                    }
                    is RecognitionResult.Finished -> {
                        if (it.text != null) {
                            viewModel.translate(it.text!!)
                            navController.popBackStack()
                            navController.navigate(Screen.ListenResults.route)
                        } else {
                            navController.popBackStack()
                        }
                    }
                }
            }
        }

        LaunchedEffect(Unit) {
            navController.currentBackStackEntryFlow.collect { nav ->
                viewModel.stopPlayback()

                if (nav.destination.route == Screen.Home.route) {
                    viewModel.translate("")
                }

                if (nav.destination.route != Screen.Listen.route) {
                    destroyListener()
                }
            }
        }
    }
}

