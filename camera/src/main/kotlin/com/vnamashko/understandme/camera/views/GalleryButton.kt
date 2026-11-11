package com.vnamashko.understandme.camera.views

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.vnamashko.understandme.camera.R
import com.vnamashko.understandme.theme.AppTheme

@Composable
fun GalleryButton(
    onClicked: () -> Unit,
    modifier: Modifier = Modifier,
) {
    IconButton(
        onClick = onClicked,
        colors = IconButtonDefaults.iconButtonColors(
            containerColor = Color.Black.copy(alpha = 0.4f),
            contentColor = Color.White,
        ),
        modifier = modifier.size(48.dp),
    ) {
        Icon(
            painter = painterResource(R.drawable.gallery),
            contentDescription = stringResource(R.string.camera_pick_image),
        )
    }
}

@Preview
@Composable
private fun GalleryButtonPreview() {
    AppTheme {
        Box(modifier = Modifier.background(Color.DarkGray).padding(16.dp)) {
            GalleryButton(onClicked = {})
        }
    }
}
