package com.vnamashko.understandme.translation.screen

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.vnamashko.understandme.camera.screen.CameraRoute
import com.vnamashko.understandme.translation.model.LanguagePair
import com.vnamashko.understandme.translation.vm.TranslateViewModel
import com.vnamashko.understandme.utils.coreui.activityViewModel

@Composable
fun TranslationCameraRoute(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: TranslateViewModel = activityViewModel(),
) {
    val languageState by viewModel.languageSelection.collectAsStateWithLifecycle()
    val source = languageState.source
    val target = languageState.target

    CameraRoute(
        languagePair = if (source != null && target != null) LanguagePair(source.code, target.code) else null,
        onBack = onBack,
        modifier = modifier,
    )
}
