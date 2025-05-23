package com.vnamashko.understandme

import android.content.Intent
import android.os.Bundle
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.vnamashko.understandme.camera.HostScreen
import com.vnamashko.understandme.stt.SpeechRecognitionListener
import com.vnamashko.understandme.theme.AppTheme
import com.vnamashko.understandme.translation.model.Language
import com.vnamashko.understandme.translation.screen.TranslationHostScreen
import com.vnamashko.understandme.utils.getLanguageTag
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.serialization.Serializable

@AndroidEntryPoint
class TranslateActivity : ComponentActivity() {
    private var speechRecognizer: SpeechRecognizer? = null

    private var speechRecognitionListener: SpeechRecognitionListener? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        speechRecognitionListener = if (SpeechRecognizer.isRecognitionAvailable(this)) {
            SpeechRecognitionListener()
        } else {
            null
        }

        enableEdgeToEdge()
        setContent {
            val navController = rememberNavController()

            AppTheme {
                NavHost(
                    navController = navController,
                    startDestination = Screen.Camera,
                    enterTransition = { EnterTransition.None },
                    exitTransition = { ExitTransition.None },
                ) {
                    composable<Screen.Translate> {
                        TranslationHostScreen(
                            startListening = ::startListening,
                            stopListening = ::stopListening,
                            destroyListener = ::destroyListener,
                            speechRecognitionListener = speechRecognitionListener
                        )
                    }

                    composable<Screen.Camera> {
                        HostScreen()
                    }
                }
            }
        }
    }

    private fun startListening(sourceLanguage: Language?) {
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
                putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            }

        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this).apply {
            setRecognitionListener(speechRecognitionListener)
        }
        speechRecognizer?.startListening(recognizerIntent)
    }

    private fun stopListening() {
        speechRecognizer?.stopListening()
    }

    private fun destroyListener() {
        speechRecognitionListener?.reset()
        speechRecognizer?.cancel()
        speechRecognizer?.destroy()
        speechRecognizer = null
    }
}

@Serializable
sealed interface Screen {

    @Serializable
    data object Translate : Screen

    @Serializable
    data object Camera : Screen
}