package com.vnamashko.understandme.translation.views

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.vnamashko.undertsndme.translation.screen.R

@Composable
fun PasteButton(onClicked: () -> Unit, modifier: Modifier = Modifier) {
    Button(
        onClick = onClicked,
        modifier = modifier.padding(top = 24.dp, start = 16.dp)
    ) {
        Icon(painter = painterResource(R.drawable.paste), contentDescription = "Paste", modifier = Modifier.size(16.dp))
        Spacer(modifier = Modifier.width(8.dp))
        Text(stringResource(R.string.paste_from_clipboard))
    }
}
