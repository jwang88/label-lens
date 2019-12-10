package com.android.labellens


import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Matrix
import android.os.Bundle
import android.util.Log
import android.util.Size
import android.view.*
import android.widget.ImageButton
import androidx.fragment.app.Fragment
import android.widget.Toast
import androidx.camera.core.*
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import com.android.labellens.databinding.FragmentCameraDisplayBinding
import java.io.File
import java.util.concurrent.Executors



class CameraDisplay : Fragment() {

    val REQUEST_CODE_PERMISSION = 10 // arbitrary val
    private val REQUIRED_PERMISSIONS = arrayOf(Manifest.permission.CAMERA) // all permissions specified in manifest

    private val executor = Executors.newSingleThreadExecutor()
    private lateinit var viewFinder: TextureView
    private lateinit var captureButton: ImageButton

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {

        val binding = DataBindingUtil.inflate<FragmentCameraDisplayBinding>(inflater,
            R.layout.fragment_camera_display, container, false)

        this.viewFinder = binding.viewFinder
        this.captureButton = binding.captureButton

        // Request camera permission
        if (allPermissionsGranted() ) {
            viewFinder.post {
                startCamera()
            }
        } else {
            this.requestPermissions(REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSION)
        }

        // Every time the provided texture view changes, recompute layout
        viewFinder.addOnLayoutChangeListener {_, _, _, _, _, _, _, _, _ ->
            updateTransform()
        }

        return binding.root
    }

    private fun startCamera() {

        /* Preview USE CASE */

        // Create configuration object for the viewfinder use case
        val previewConfig = PreviewConfig.Builder().apply {
            setTargetResolution(Size(640, 480) )
        }.build()

        // Build the viewfinder use case
        val preview = Preview(previewConfig)

        // Every time the viewfinder is updated, recompute layout
        preview.setOnPreviewOutputUpdateListener {
            // To update he SurfaceTexture, we have to remove it and re-add it
            val parent = viewFinder.parent as ViewGroup
            parent.removeView(viewFinder)
            parent.addView(viewFinder, 0)

            viewFinder.surfaceTexture = it.surfaceTexture
            updateTransform()
        }


        /* Image Capture USE CASE */

        // Build configuration object for the image capture use case
        val imageCaptureConfig = ImageCaptureConfig.Builder().apply {
            setCaptureMode(ImageCapture.CaptureMode.MIN_LATENCY)
        }.build()

        // Build the image capture use case and attach BUTTON click listener
        val imageCapture = ImageCapture(imageCaptureConfig)
        this.captureButton.setOnClickListener {
            //val file = File(externalMediaDirs.first(), "${System.currentTimeMillis()}.jpg")
            val file = File(requireContext().externalMediaDirs.first(), "${System.currentTimeMillis()}.jpg")

            imageCapture.takePicture(file, executor,
                object: ImageCapture.OnImageSavedListener {

                    override fun onError(
                        imageCaptureError: ImageCapture.ImageCaptureError,
                        message: String,
                        exc: Throwable?
                    ) {
                        val msg = "Photo Capture Failed: $message"
                        Log.e("Label Lens", msg, exc)
                        viewFinder.post {
                            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                        }
                    }
                    override fun onImageSaved(file: File) {
                        val msg = "Photo capture Succeeded: ${file.absolutePath}"
                        Log.d("Label Lens", msg)
                        viewFinder.post {
                            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            )
        }


        /* Image Analysis USE CASE */

        // Setup image analysis pipeline that computes average pixel luminance
        val analyzerConfig = ImageAnalysisConfig.Builder().apply {
            // In our analysis, we care more about the latest image than analyzing *every* image
            setImageReaderMode(ImageAnalysis.ImageReaderMode.ACQUIRE_LATEST_IMAGE)
        }.build()

        // Build the image analysis use case and instantiate our analyzer
        val analyzerUseCase = ImageAnalysis(analyzerConfig).apply {
            setAnalyzer(executor, LuminosityAnalyzer() )
        }


        // Bind use cases to lifecycle
        CameraX.bindToLifecycle(this, preview, imageCapture, analyzerUseCase) // if complaining, rebuild
    }


    // Compensate for changes in device orientation to display viewfinder in upright rotation
    private fun updateTransform() {
        val matrix = Matrix()

        // Computer the center of the view finder
        val centerX = viewFinder.width / 2f
        val centerY = viewFinder.height / 2f

        // Correct preview output to account for display rotation
        val rotationDegrees = when (viewFinder.display.rotation) {
            Surface.ROTATION_0 -> 0
            Surface.ROTATION_90 -> 90
            Surface.ROTATION_180 -> 180
            Surface.ROTATION_270 -> 270
            else -> return
        }
        matrix.postRotate(- rotationDegrees.toFloat(), centerX, centerY)

        // Finally, apply transformations to out TextureView
        viewFinder.setTransform(matrix)

    }


    /**
     * Process results from permission request dialog box, has the request been granted?
     * If yes, start Camera. Otherwise display a toast message.
     */
    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<String>, grantResults: IntArray) {

        if (requestCode == REQUEST_CODE_PERMISSION) {
            if (allPermissionsGranted()) {
                viewFinder.post {
                    startCamera()
                }
            } else {
                Toast.makeText(context, "Permission not granted by user", Toast.LENGTH_SHORT).show()
                //finish()
            }
        }
    }


    /**
     * Check if all permission specified in the manifest have been granted
     */
    private fun allPermissionsGranted() = REQUIRED_PERMISSIONS.all {
        ContextCompat.checkSelfPermission(requireContext(), it) == PackageManager.PERMISSION_GRANTED
    }

}
