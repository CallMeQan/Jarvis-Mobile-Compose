package com.github.callmeqan.jarviscomposed.ui.components

import android.graphics.Bitmap
import android.util.Log
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.LifecycleOwner
import com.github.callmeqan.jarviscomposed.utils.rotate
import java.util.concurrent.Executor

@Composable
fun CameraCaptureOnce(
    onBitmapCaptured: (Bitmap) -> Unit,
    executor: Executor,
    lifecycleOwner: LifecycleOwner
) {
    val imageCapture = remember { ImageCapture.Builder().build() }
    var captured by remember { mutableStateOf(false) }

    Box(Modifier.fillMaxSize()) {
        CameraPreview(imageCapture = imageCapture, modifier = Modifier.matchParentSize(), lifecycleOwner=lifecycleOwner)

        Button(
            onClick = {
                if (!captured) {
                    captured = true
                    imageCapture.takePicture(
                        executor,
                        object : ImageCapture.OnImageCapturedCallback() {
                            override fun onCaptureSuccess(imageProxy: ImageProxy) {
                                // As original image is rotated
                                // we must capture and re-rotate it.
                                // Must be done before putting into UI
                                // as it will mess things up.
                                var bitmap = imageProxy.toBitmap()
                                bitmap = bitmap.rotate(90f)
                                imageProxy.close()
                                onBitmapCaptured(bitmap)
                            }

                            override fun onError(exc: ImageCaptureException) {
                                Log.e("CameraCapture", "Capture failed", exc)
                            }
                        }
                    )
                }
            },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(16.dp)
        ) {
            Text(if (captured) "Captured" else "Capture")
        }
    }
}