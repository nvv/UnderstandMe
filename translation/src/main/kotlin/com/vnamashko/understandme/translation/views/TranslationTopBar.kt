package com.vnamashko.understandme.translation.views

import android.content.res.Configuration
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.vnamashko.understandme.theme.AppTheme
import com.vnamashko.undertsndme.translation.screen.R

/**
 * Top bar used by the translate screens. Each screen places its own instance.
 *
 * @param onBack shows a back arrow when not `null`.
 * @param actions end-aligned content, e.g. [ClearTextAction].
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TranslationTopBar(
    onBack: (() -> Unit)?,
    modifier: Modifier = Modifier,
    isLoading: Boolean = false,
    actions: @Composable RowScope.() -> Unit = {},
) {
    Box(modifier = modifier) {
        TopAppBar(
        title = {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Spacer(Modifier.weight(1f))
                actions()
            }
        },
        navigationIcon = {
            if (onBack != null) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier.padding(start = 16.dp),
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Default.ArrowBack,
                        contentDescription = stringResource(R.string.top_bar_back),
                    )
                }
            }
        },
        )
        if (isLoading) {
            LinearProgressIndicator(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .fillMaxWidth(),
            )
        }
    }
}

/** Clears the entered text. */
@Composable
fun ClearTextAction(onClear: () -> Unit, modifier: Modifier = Modifier) {
    Icon(
        imageVector = Icons.Default.Close,
        contentDescription = stringResource(R.string.top_bar_clear),
        modifier = modifier.clickable(
            onClick = onClear,
            indication = null,
            interactionSource = remember { MutableInteractionSource() },
        ),
    )
}

@Preview
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun TranslationTopBarPreview() {
    AppTheme {
        TranslationTopBar(onBack = {}, isLoading = true, actions = { ClearTextAction(onClear = {}) })
    }
}
