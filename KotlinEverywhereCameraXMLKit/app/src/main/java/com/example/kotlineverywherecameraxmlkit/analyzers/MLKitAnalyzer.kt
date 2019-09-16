package com.example.kotlineverywherecameraxmlkit.analyzers

import android.graphics.*
import android.media.Image
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.google.firebase.ml.vision.common.FirebaseVisionImage
import com.google.firebase.ml.vision.common.FirebaseVisionImageMetadata
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
                        listener.onProcessed(getFirebaseVisionImage(getImage(), rotationDegrees))
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

            val data = ByteArray(ySize + uSize + vSize)

            yBuffer.get(data, 0, ySize)
            vBuffer.get(data, ySize, vSize)
            uBuffer.get(data, ySize + vSize, uSize)

            val yuvImage = YuvImage(data, ImageFormat.NV21, width, height, null)
            val out = ByteArrayOutputStream()
            yuvImage.compressToJpeg(Rect(0, 0, yuvImage.width, yuvImage.height), 50, out)
            val imageBytes = out.toByteArray()
            val bitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
            val matrix = Matrix().apply { postRotate(degrees.toFloat()) }
            return Bitmap.createBitmap(bitmap, 0, 0, width, height, matrix, true)
        }
        return null
    }

    private fun getFirebaseVisionImage(image: Image?, degrees: Int): FirebaseVisionImage? {
        image?.apply {
            val yBuffer = planes[0].buffer
            val uBuffer = planes[1].buffer
            val vBuffer = planes[2].buffer

            val ySize = yBuffer.remaining()
            val uSize = uBuffer.remaining()
            val vSize = vBuffer.remaining()

            val data = ByteArray(ySize + uSize + vSize)

            yBuffer.get(data, 0, ySize)
            vBuffer.get(data, ySize, vSize)
            uBuffer.get(data, ySize + vSize, uSize)

            val rotation = getRotation(degrees)
            val metadata = FirebaseVisionImageMetadata.Builder()
                .setFormat(FirebaseVisionImageMetadata.IMAGE_FORMAT_YV12)
                .setHeight(height)
                .setWidth(width)
                .setRotation(rotation)
                .build()

            return FirebaseVisionImage.fromByteArray(data, metadata)
        }
        return null
    }

    private fun getRotation(degrees: Int): Int {
        when (degrees) {
            0 -> return FirebaseVisionImageMetadata.ROTATION_0
            90 -> return FirebaseVisionImageMetadata.ROTATION_90
            180 -> return FirebaseVisionImageMetadata.ROTATION_180
            270 -> return FirebaseVisionImageMetadata.ROTATION_270
        }
        throw IllegalArgumentException(
            "Rotation must be 0, 90, 180, or 270."
        )
    }


}

interface MLKitListener {
    fun onProcessed(bitmap: Bitmap?)
    fun onProcessed(firebaseVisionImage: FirebaseVisionImage?)
}