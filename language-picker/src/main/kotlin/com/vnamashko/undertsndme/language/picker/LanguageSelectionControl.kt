package com.vnamashko.undertsndme.language.picker

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.vnamashko.understandme.language.picker.R
import com.vnamashko.understandme.theme.dimens
import com.vnamashko.understandme.translation.model.Language

@Composable
fun LanguageSelectionControl(
    sourceLanguage: Language?,
    proposedSourceLanguage: Language?,
    targetLanguage: Language?,
    selectFor: (LanguageFor) -> Unit,
    selectProposedLanguage: () -> Unit,
    flipLanguages: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(12.dp)
    ) {
        if (proposedSourceLanguage != null) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surfaceContainer,
                modifier = modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = ripple(),
                        onClick = selectProposedLanguage
                    ),
            ) {
                Column(
                    modifier = Modifier.padding(
                        horizontal = MaterialTheme.dimens.languagePillHorizontalPadding,
                        vertical = 16.dp
                    )
                ) {
                    Text(
                        text = stringResource(R.string.translate_from),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    Text(
                        text = proposedSourceLanguage.displayName,
                        color = MaterialTheme.colorScheme.primary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
        }

        Row(
            horizontalArrangement = Arrangement.Absolute.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            LanguagePill(
                label = sourceLanguage?.displayName ?: stringResource(R.string.detect_language),
                onSelected = {
                    selectFor(LanguageFor.SOURCE)
                },
                modifier = Modifier.weight(1f)
            )

            Spacer(modifier = Modifier.width(16.dp))
            Icon(
                painter = painterResource(R.drawable.swap),
                contentDescription = "Flip languages",
                modifier = Modifier
                    .size(20.dp)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = ripple(bounded = false),
                        onClick = flipLanguages
                    ),
            )
            Spacer(modifier = Modifier.width(16.dp))

            LanguagePill(
                label = targetLanguage?.displayName ?: stringResource(R.string.select_language),
                onSelected = {
                    selectFor(LanguageFor.TARGET)
                },
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
fun LanguagePill(label: String, onSelected: () -> Unit, modifier: Modifier = Modifier) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceContainer,
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(),
                onClick = onSelected
            ),
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.padding(
                horizontal = MaterialTheme.dimens.languagePillHorizontalPadding,
                vertical = 16.dp
            )
        ) {
            Text(
                text = label,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

enum class LanguageFor {
    SOURCE, TARGET
}
