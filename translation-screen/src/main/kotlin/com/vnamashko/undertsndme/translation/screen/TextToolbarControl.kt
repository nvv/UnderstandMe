package com.vnamashko.undertsndme.translation.screen

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.vnamashko.undertsndme.translation.screen.icons.CopyIcon
import com.vnamashko.undertsndme.translation.screen.icons.VolumeIcon

@Composable
fun TextToolbarControl(
    onPlaybackClicked: () -> Unit,
    onCopyClicked: () -> Unit,
    tint: Color,
    modifier: Modifier = Modifier
) {
    Row(modifier = modifier.padding(horizontal = 16.dp)) {
        Icon(
            imageVector = VolumeIcon,
            contentDescription = "Speak",
            tint = tint,
            modifier = Modifier.size(24.dp).clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(bounded = false),
                onClick = onPlaybackClicked
            )
        )
        Spacer(modifier = Modifier.weight(1f))
        Icon(
            imageVector = CopyIcon,
            contentDescription = "Copy text",
            tint = tint,
            modifier = Modifier.size(24.dp).clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(bounded = false),
                onClick = onCopyClicked
            )
        )
    }
}

