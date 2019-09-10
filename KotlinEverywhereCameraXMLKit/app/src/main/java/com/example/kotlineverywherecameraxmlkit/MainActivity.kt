package com.example.kotlineverywherecameraxmlkit

import android.Manifest
import android.graphics.Matrix
import android.os.Bundle
import android.util.Rational
import android.view.Surface
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.CameraX
import androidx.camera.core.Preview
import androidx.camera.core.PreviewConfig
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import com.example.kotlineverywherecameraxmlkit.extensions.arePermissionsGranted
import com.example.kotlineverywherecameraxmlkit.extensions.askForPermissions
import com.google.android.material.snackbar.Snackbar
import kotlinx.android.synthetic.main.activity_main.*

private const val REQUEST_CAMERA_CODE_PERMISSION = 7777

class MainActivity : AppCompatActivity(), LifecycleOwner {

    private lateinit var lifecycleRegistry: LifecycleRegistry

    private val requiredPermissions = arrayOf(
        Manifest.permission.CAMERA
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

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
            setTargetRotation(Surface.ROTATION_0)
        }.build()
        val preview = Preview(previewConfiguration)

        preview.setOnPreviewOutputUpdateListener {

            val parent = cameraXTextureView.parent as ViewGroup
            parent.removeView(cameraXTextureView)
            parent.addView(cameraXTextureView, 0)

            cameraXTextureView.surfaceTexture = it.surfaceTexture
            updateTransformation()
        }


        if (lifecycleRegistry.currentState.isAtLeast(Lifecycle.State.STARTED)) {
            CameraX.bindToLifecycle(this, preview)
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
}
