package com.github.callmeqan.jarviscomposed.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color

@Composable
fun CameraCaptureButton(
    modifier: Modifier = Modifier,
    onClick: () -> Unit, // Context, ImageCapture, Executor
) {
    IconButton(
        onClick = {
            onClick()
        }
    ) {
        Icon(
            imageVector = Icons.Filled.CameraAlt,
            contentDescription = "Capture",
            tint = Color.White
        )
    }
}
