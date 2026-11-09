package com.vnamashko.understandme.camera.views

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.vnamashko.understandme.camera.R
import com.vnamashko.understandme.theme.AppTheme

@Composable
fun ShutterButton(
    onClicked: () -> Unit,
    modifier: Modifier = Modifier,
    isEnabled: Boolean = true,
) {
    val description = stringResource(R.string.camera_take_photo)
    Box(
        modifier = modifier
            .size(72.dp)
            .border(4.dp, Color.White, CircleShape)
            .padding(8.dp)
            .clip(CircleShape)
            .background(if (isEnabled) Color.White else Color.White.copy(alpha = 0.5f))
            .clickable(enabled = isEnabled, role = Role.Button, onClick = onClicked)
            .semantics { contentDescription = description },
    )
}

@Preview
@Composable
private fun ShutterButtonPreview() {
    AppTheme {
        Box(modifier = Modifier.background(Color.Black).padding(16.dp)) {
            ShutterButton(onClicked = {})
        }
    }
}
