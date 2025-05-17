package com.vnamashko.understandme.translation.views

import androidx.compose.foundation.layout.Row
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import com.vnamashko.undertsndme.translation.screen.R

@Composable
fun LanguageModelDownloadDialog(
    onDismiss: () -> Unit,
    onDownloadNow: () -> Unit,
    onDownloadOnWifi: () -> Unit,
    onAlwaysDownload: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                LocalContext.current.getString(R.string.dialog_download_language_mode),
                style = MaterialTheme.typography.titleMedium
            )
        },
        text = {
            Text(LocalContext.current.getString(R.string.dialog_download_language_mode_description))
        },
        confirmButton = {
            Row {
                TextButton(onClick = onDownloadOnWifi) {
                    Text(LocalContext.current.getString(R.string.dialog_download_wifi_only))
                }
                TextButton(onClick = onDownloadNow) {
                    Text(LocalContext.current.getString(R.string.dialog_download_now_only))
                }
                TextButton(onClick = onAlwaysDownload) {
                    Text(LocalContext.current.getString(R.string.dialog_download_always))
                }
            }
        }
    )
}

@Preview(showBackground = true)
@Composable
private fun LanguageModelDownloadDialogPreview() {
    LanguageModelDownloadDialog(
        {}, {}, {}, {}
    )
}