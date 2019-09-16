package com.example.kotlineverywherecameraxmlkit

import android.Manifest
import android.graphics.Bitmap
import android.graphics.Matrix
import android.os.Bundle
import android.os.Handler
import android.os.HandlerThread
import android.util.Rational
import android.util.Size
import android.view.Surface
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.*
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import com.example.kotlineverywherecameraxmlkit.analyzers.MLKitAnalyzer
import com.example.kotlineverywherecameraxmlkit.analyzers.MLKitListener
import com.example.kotlineverywherecameraxmlkit.extensions.arePermissionsGranted
import com.example.kotlineverywherecameraxmlkit.extensions.askForPermissions
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.FirebaseApp
import com.google.firebase.ml.vision.FirebaseVision
import com.google.firebase.ml.vision.barcode.FirebaseVisionBarcode
import com.google.firebase.ml.vision.barcode.FirebaseVisionBarcodeDetector
import com.google.firebase.ml.vision.barcode.FirebaseVisionBarcodeDetectorOptions
import com.google.firebase.ml.vision.common.FirebaseVisionImage
import kotlinx.android.synthetic.main.activity_main.*

private const val REQUEST_CAMERA_CODE_PERMISSION = 7777

class MainActivity : AppCompatActivity(), LifecycleOwner, MLKitListener {

    private lateinit var lifecycleRegistry: LifecycleRegistry
    private lateinit var detector: FirebaseVisionBarcodeDetector

    private val requiredPermissions = arrayOf(
        Manifest.permission.CAMERA
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        FirebaseApp.initializeApp(this)
        setContentView(R.layout.activity_main)

        val options = FirebaseVisionBarcodeDetectorOptions.Builder()
            .setBarcodeFormats(
                FirebaseVisionBarcode.FORMAT_QR_CODE
            )
            .build()

        detector = FirebaseVision.getInstance().getVisionBarcodeDetector(options)

        lifecycleRegistry = LifecycleRegistry(this)

        if (arePermissionsGranted(requiredPermissions)) {
            cameraPermissionsGranted()
        } else {
            askForPermissions(requiredPermissions, REQUEST_CAMERA_CODE_PERMISSION)
        }

        cameraXTextureView.addOnLayoutChangeListener { _, _, _, _, _, _, _, _, _ ->
            updateTransformation()
        }

        lifecycleRegistry.markState(Lifecycle.State.CREATED)
    }

    override fun onStart() {
        super.onStart()
        lifecycleRegistry.markState(Lifecycle.State.STARTED)
    }

    override fun onResume() {
        super.onResume()
        lifecycleRegistry.markState(Lifecycle.State.RESUMED)
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        if (requestCode == REQUEST_CAMERA_CODE_PERMISSION) {
            cameraPermissionsGranted()
        } else {
            Snackbar.make(
                coordinatorLayout,
                "Para poder usar la cámara, dar los permisos primero",
                Snackbar.LENGTH_LONG
            ).show()
        }
    }

    override fun getLifecycle(): Lifecycle {
        return lifecycleRegistry
    }

    // CAMERA X EMPTY METHODS
    private fun cameraPermissionsGranted() {
        cameraXTextureView.post {
            startCamera()
        }
    }

    private fun startCamera() {
        val previewConfiguration = PreviewConfig.Builder().apply {
            setTargetAspectRatio(Rational(1, 1))
            setTargetResolution(Size(1024, 768))
        }.build()
        val preview = Preview(previewConfiguration)

        preview.setOnPreviewOutputUpdateListener {

            val parent = cameraXTextureView.parent as ViewGroup
            parent.removeView(cameraXTextureView)
            parent.addView(cameraXTextureView, 0)

            cameraXTextureView.surfaceTexture = it.surfaceTexture
            updateTransformation()
        }

        val analyzerConfig = ImageAnalysisConfig.Builder().apply {
            val analyzerThread = HandlerThread("MLKitAnalyzer").apply { start() }
            setCallbackHandler(Handler(analyzerThread.looper))
            setImageReaderMode(
                ImageAnalysis.ImageReaderMode.ACQUIRE_LATEST_IMAGE
            )
        }.build()

        val analyzerUseCase = ImageAnalysis(analyzerConfig).apply {
            analyzer = MLKitAnalyzer(this@MainActivity)
        }

        if (lifecycleRegistry.currentState.isAtLeast(Lifecycle.State.STARTED)) {
            CameraX.bindToLifecycle(this, preview, analyzerUseCase)
        }
    }

    private fun updateTransformation() {
        val matrix = Matrix()

        val centerX = cameraXTextureView.width * 0.5f
        val centerY = cameraXTextureView.height * 0.5f

        val rotationDegrees = when (cameraXTextureView.display.rotation) {
            Surface.ROTATION_0 -> 0
            Surface.ROTATION_90 -> 90
            Surface.ROTATION_180 -> 180
            Surface.ROTATION_270 -> 270
            else -> return
        }
        matrix.postRotate(-rotationDegrees.toFloat(), centerX, centerY)
        cameraXTextureView.setTransform(matrix)
    }

    private fun processQRCode(firebaseVisionImage: FirebaseVisionImage?) {
        firebaseVisionImage?.let { image ->
            detector.detectInImage(image)
                .addOnSuccessListener { barcodes ->
                    for (barcode in barcodes) {
                        val bounds = barcode.boundingBox
                        val rawValue = barcode.rawValue
                        val builder = StringBuilder()
                        builder.append(bounds.toString())
                        builder.append("\n")
                        builder.append(rawValue)
                        infoTextView.text = builder.toString()
                    }
                }
                .addOnFailureListener { exception ->
                    Snackbar.make(
                        findViewById(android.R.id.content),
                        exception.localizedMessage,
                        Snackbar.LENGTH_LONG
                    ).show()
                }
        }
    }

    override fun onProcessed(bitmap: Bitmap?) {
    }

    override fun onProcessed(firebaseVisionImage: FirebaseVisionImage?) {
        runOnUiThread {
            previewImageView.post {
                previewImageView.apply {
                    setImageBitmap(firebaseVisionImage?.bitmap)
                }
            }
            processQRCode(firebaseVisionImage)
        }
    }
}
