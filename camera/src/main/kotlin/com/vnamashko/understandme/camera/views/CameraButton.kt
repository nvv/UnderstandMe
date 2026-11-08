package com.vnamashko.understandme.camera.views

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.vnamashko.understandme.theme.AppTheme
import com.vnamashko.understandme.camera.R

@Composable
fun CameraButton(
    onClicked: () -> Unit,
    modifier: Modifier = Modifier,
    isEnabled: Boolean = true,
) {
    val colors = MaterialTheme.colorScheme
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .size(48.dp)
            .clip(CircleShape)
            .background(if (isEnabled) colors.secondaryContainer else colors.surfaceDim)
            .clickable(enabled = isEnabled, onClick = onClicked),
    ) {
        Icon(
            painter = painterResource(R.drawable.camera),
            contentDescription = stringResource(R.string.open_camera),
            tint = colors.onSecondaryContainer,
            modifier = Modifier.size(24.dp),
        )
    }
}

@Preview
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun CameraButtonPreview() {
    AppTheme { CameraButton(onClicked = {}) }
}
