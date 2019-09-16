package com.example.kotlineverywherecameraxmlkit.analyzers

import android.graphics.*
import android.media.Image
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import java.io.ByteArrayOutputStream

/**
 * @author Carlos Piñan
 */

class MLKitAnalyzer(
    private val listener: MLKitListener
) : ImageAnalysis.Analyzer {

    companion object {
        private const val MIN_TIME = 1500L
    }

    private var lastSystemMillis = System.currentTimeMillis()

    override fun analyze(image: ImageProxy?, rotationDegrees: Int) {
        if (System.currentTimeMillis() - lastSystemMillis >= MIN_TIME) {
            lastSystemMillis = System.currentTimeMillis()
            if (image?.image != null) {
                image?.let { imageProxy ->
                    imageProxy.apply {
                        listener.onProcessed(getBitmap(getImage(), rotationDegrees))
                    }
                }
            }
        }
    }

    // Boilerplate code
    private fun getBitmap(image: Image?, degrees: Int): Bitmap? {
        image?.apply {
            val yBuffer = planes[0].buffer
            val uBuffer = planes[1].buffer
            val vBuffer = planes[2].buffer

            val ySize = yBuffer.remaining()
            val uSize = uBuffer.remaining()
            val vSize = vBuffer.remaining()

            val nv21 = ByteArray(ySize + uSize + vSize)

            yBuffer.get(nv21, 0, ySize)
            vBuffer.get(nv21, ySize, vSize)
            uBuffer.get(nv21, ySize + vSize, uSize)

            val yuvImage = YuvImage(nv21, ImageFormat.NV21, width, height, null)
            val out = ByteArrayOutputStream()
            yuvImage.compressToJpeg(Rect(0, 0, yuvImage.width, yuvImage.height), 50, out)
            val imageBytes = out.toByteArray()
            val bitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
            val matrix = Matrix().apply { postRotate(degrees.toFloat()) }
            return Bitmap.createBitmap(bitmap, 0, 0, width, height, matrix, true)
        }
        return null
    }

}

interface MLKitListener {
    fun onProcessed(bitmap: Bitmap?)
}