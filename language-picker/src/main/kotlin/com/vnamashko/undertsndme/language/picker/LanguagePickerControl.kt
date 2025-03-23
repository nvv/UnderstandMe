package com.vnamashko.undertsndme.language.picker

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.vnamashko.understandme.translation.model.Language
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.launch

@OptIn(FlowPreview::class)
@Composable
fun LanguagePickerControl(
    selectedLanguage: Language?,
    supportedLanguages: List<Language>,
    onSelect: (Language) -> Unit,
    searchTitle: String,
    modifier: Modifier = Modifier
) {
    val selectedIndex = supportedLanguages.indexOf(selectedLanguage)

    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()

    var isSearching by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }

    var filteredLanguages by remember { mutableStateOf(supportedLanguages) }

    LaunchedEffect(searchQuery) {
        snapshotFlow { searchQuery }
            .debounce(100)
            .collect { text ->
                filteredLanguages = supportedLanguages.filter { language ->
                    language.displayName.lowercase().contains(text.lowercase()) ||
                    language.code.lowercase().contains(text.lowercase())
                }
            }
    }

    Column(modifier = modifier.padding(top = 56.dp, start = 16.dp, end = 16.dp)) {

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp)
                .height(56.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (isSearching) {
                TextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text(searchTitle) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = androidx.compose.material3.OutlinedTextFieldDefaults.colors(
                        unfocusedLabelColor = Color.Transparent,
                    ),
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Search"
                        )
                    },
                    trailingIcon = {
                        IconButton(onClick = {
                            isSearching = false
                            searchQuery = ""
                        }) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Close search"
                            )
                        }
                    }
                )

            } else {
                Text(
                    text = searchTitle,
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier
                        .padding(start = 24.dp)
                        .weight(1f)
                )
                IconButton(onClick = { isSearching = true }) {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Search languages"
                    )
                }
            }
        }

        LazyColumn(state = listState) {
            items(
                items = filteredLanguages,
                key = { language -> language.code }
            ) { language ->
                LanguageRow(language, language == selectedLanguage, onSelect)
            }
        }
    }

    LaunchedEffect(selectedIndex) {
        coroutineScope.launch {
            if (selectedIndex > 0) {
                listState.scrollToItem(selectedIndex)
            }
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
