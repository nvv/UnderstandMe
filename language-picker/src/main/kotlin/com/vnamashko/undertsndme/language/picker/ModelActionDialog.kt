package com.vnamashko.undertsndme.language.picker

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.vnamashko.understandme.language.picker.R

@Composable
fun DeleteModelDialog(
    languageString: String,
    onDismissRequest: () -> Unit,
    onConfirmation: () -> Unit
) {
    ModelActionAlertDialog(
        onDismissRequest,
        onConfirmation,
        stringResource(R.string.delete),
        languageString,
        stringResource(R.string.delete_model)
    )
}

@Composable
fun DownloadModelDialog(
    languageString: String,
    onDismissRequest: () -> Unit,
    onConfirmation: () -> Unit
) {
    ModelActionAlertDialog(
        onDismissRequest,
        onConfirmation,
        stringResource(R.string.download),
        languageString,
        stringResource(R.string.download_model)
    )
}

@Composable
private fun ModelActionAlertDialog(
    onDismissRequest: () -> Unit,
    onConfirmation: () -> Unit,
    actionText: String,
    dialogTitle: String,
    dialogText: String,
) {
    AlertDialog(
        icon = null,
        title = {
            Text(text = dialogTitle)
        },
        text = {
            Text(text = dialogText)
        },
        onDismissRequest = {
            onDismissRequest()
        },
        confirmButton = {
            TextButton(onClick = onConfirmation) {
                Text(actionText)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismissRequest) {
                Text(stringResource(R.string.cancel))
            }
        }
    )
}
