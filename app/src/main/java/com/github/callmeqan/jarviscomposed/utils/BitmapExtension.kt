package com.github.callmeqan.jarviscomposed.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import org.tensorflow.lite.DataType
import org.tensorflow.lite.support.image.ImageProcessor
import org.tensorflow.lite.support.image.TensorImage
import org.tensorflow.lite.support.image.ops.ResizeOp
import java.nio.ByteBuffer

fun loadBitmapFromAssets(context: Context, fileName: String): Bitmap {
    // Mainly for testing purposes; fileName could
    // the name (e.g., "bright.png") of image in
    // app/src/main/assets/images directory.
    val assetManager = context.assets
    val inputStream = assetManager.open("test_images/$fileName")
    return BitmapFactory.decodeStream(inputStream)
}

// Function for CNN model
fun convertBitmapToByteBuffer(bitmapUnresized: Bitmap, width: Int, height: Int): ByteBuffer {
    val imageProcessor = ImageProcessor.Builder()
        .add(ResizeOp(height, width, ResizeOp.ResizeMethod.BILINEAR))
        .build()

    // Initialize TensorImage for float32 model
    val tensorImage = TensorImage(DataType.FLOAT32)
    tensorImage.load(bitmapUnresized)  // loads and applies the Bitmap
    val processedImage = imageProcessor.process(tensorImage)

    val buffer = processedImage.buffer
    return buffer
}

fun Bitmap.rotate(degrees: Float): Bitmap {
    val matrix = Matrix().apply { postRotate(degrees) }
    return Bitmap.createBitmap(this, 0, 0, width, height, matrix, true)
}