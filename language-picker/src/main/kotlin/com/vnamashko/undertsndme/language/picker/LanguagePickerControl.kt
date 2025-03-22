package com.vnamashko.undertsndme.language.picker

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Done
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import com.vnamashko.understandme.translation.model.Language

@Composable
fun LanguagePickerControl(
    selectedLanguage: Language?,
    supportedLanguages: List<Language>,
    onSelect: (Language) -> Unit,
    modifier: Modifier = Modifier
) {
    val selectedIndex = supportedLanguages.indexOf(selectedLanguage)

    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()

    LazyColumn(state = listState, modifier = modifier) {
        items(
            items = supportedLanguages,
            key = { language -> language.code }
        ) { language ->
            LanguageRow(language, language == selectedLanguage, onSelect)
        }
    }

    LaunchedEffect(selectedIndex) {
        coroutineScope.launch {
            listState.scrollToItem(selectedIndex)
        }
    }

}

@Composable
private fun LanguageRow(
    language: Language,
    isSelected: Boolean,
    onSelect: (Language) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onSelect(language) }
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (isSelected) {
            Icon(
                imageVector = Icons.Filled.Done,
                contentDescription = "Selected",
                modifier = Modifier.size(24.dp)
            )

            Spacer(modifier = Modifier.width(8.dp))
        }

        Text(
            text = language.displayName,
            style = MaterialTheme.typography.bodyLarge
        )
    }
}
