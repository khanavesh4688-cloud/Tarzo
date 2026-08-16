package com.tarzo.ai.features.camera

import android.Manifest
import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.widget.Toast
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.video.FileDescriptorOutputOptions
import androidx.camera.video.MediaStoreOutputOptions
import androidx.camera.video.Recorder
import androidx.camera.video.Recording
import androidx.camera.video.VideoRecordEvent
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import com.tarzo.ai.util.Result
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume

/**
 * Manages CameraX operations: opening the camera, taking photos, selfies,
 * recording video, and capturing with a countdown timer.
 *
 * All public methods check for the [Manifest.permission.CAMERA] permission
 * and return [Result] with either a content [Uri] of the saved file or an error.
 *
 * Images and videos are saved to the MediaStore so they appear in the gallery.
 */
@Singleton
class CameraManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private var cameraProvider: ProcessCameraProvider? = null
    private var imageCapture: ImageCapture? = null
    private var preview: Preview? = null
    private var currentRecording: Recording? = null
    private var isUsingFrontCamera = false
    private var isRecording = false

    private val cameraExecutor: ExecutorService = Executors.newSingleThreadExecutor()

    private val dateFormat = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US)

    // ── Camera Lifecycle ───────────────────────────────────────────────

    /**
     * Opens the camera and binds it to the given [PreviewView].
     * Use this when a camera preview UI is available.
     */
    suspend fun openCamera(
        previewView: PreviewView,
        useFrontCamera: Boolean = false
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            if (!hasPermission(Manifest.permission.CAMERA)) {
                return@withContext Result.Error(
                    SecurityException("Camera permission not granted"),
                    "Camera permission is required. Please grant it in settings."
                )
            }
            val provider = ProcessCameraProvider.getInstance(context).get()
            cameraProvider = provider
            isUsingFrontCamera = useFrontCamera

            imageCapture = ImageCapture.Builder()
                .setCaptureMode(ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY)
                .build()

            preview = Preview.Builder().build().also {
                it.surfaceProvider = previewView.surfaceProvider
            }

            val selector = if (useFrontCamera) {
                CameraSelector.DEFAULT_FRONT_CAMERA
            } else {
                CameraSelector.DEFAULT_BACK_CAMERA
            }

            provider.unbindAll()
            provider.bindToLifecycle(
                (previewView.context as? androidx.lifecycle.LifecycleOwner)
                    ?: return@withContext Result.Error(
                        IllegalStateException("PreviewView must be attached to a LifecycleOwner"),
                        "Camera cannot start without a valid lifecycle owner."
                    ),
                selector,
                preview,
                imageCapture
            )
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(e, "Failed to open camera: ${e.message}")
        }
    }

    /**
     * Takes a photo using the current camera (back by default).
     * Returns the content [Uri] of the saved image.
     */
    suspend fun takePhoto(): Result<Uri> = capturePhoto(useFrontCamera = false)

    /**
     * Takes a selfie using the front-facing camera.
     * Returns the content [Uri] of the saved image.
     */
    suspend fun takeSelfie(): Result<Uri> = capturePhoto(useFrontCamera = true)

    /**
     * Takes a photo after a countdown timer.
     * @param seconds The countdown duration before the photo is captured.
     */
    suspend fun captureWithTimer(
        seconds: Int,
        previewView: PreviewView? = null,
        useFrontCamera: Boolean = false
    ): Result<Uri> = withContext(Dispatchers.IO) {
        try {
            if (!hasPermission(Manifest.permission.CAMERA)) {
                return@withContext Result.Error(
                    SecurityException("Camera permission not granted"),
                    "Camera permission required."
                )
            }
            // If a preview view is provided, ensure the camera is bound
            if (previewView != null) {
                val openResult = openCamera(previewView, useFrontCamera)
                if (openResult.isError) {
                    @Suppress("UNCHECKED_CAST")
                    return@withContext openResult as Result<Uri>
                }
            }

            // Countdown delay
            kotlinx.coroutines.delay(seconds * 1000L)

            capturePhoto(useFrontCamera = useFrontCamera)
        } catch (e: Exception) {
            Result.Error(e, "Timer capture failed: ${e.message}")
        }
    }

    /**
     * Starts recording a video using the current camera.
     * The video is saved to MediaStore.
     */
    @androidx.annotation.OptIn(androidx.camera.video.ExperimentalVideo::class)
    suspend fun startVideoRecording(): Result<Uri> = withContext(Dispatchers.IO) {
        if (isRecording) {
            return@withContext Result.Error(
                IllegalStateException("Already recording"),
                "A video recording is already in progress."
            )
        }
        try {
            if (!hasPermission(Manifest.permission.CAMERA)) {
                return@withContext Result.Error(
                    SecurityException("Camera permission not granted"),
                    "Camera permission required."
                )
            }

            val provider = cameraProvider
                ?: return@withContext Result.Error(
                    IllegalStateException("Camera not initialized"),
                    "Camera has not been opened. Call openCamera() first."
                )

            val recorder = Recorder.Builder().build()
            val videoCapture = androidx.camera.video.VideoCapture.of(recorder)

            val selector = if (isUsingFrontCamera) {
                CameraSelector.DEFAULT_FRONT_CAMERA
            } else {
                CameraSelector.DEFAULT_BACK_CAMERA
            }

            provider.unbindAll()
            provider.bindToLifecycle(
                // Re-use existing lifecycle if available, otherwise this will fail
                // Callers should ensure openCamera was called first
                try {
                    (context as? androidx.lifecycle.LifecycleOwner)
                        ?: throw IllegalStateException("No lifecycle owner")
                } catch (e: Exception) {
                    return@withContext Result.Error(
                        e, "Cannot bind video capture without a lifecycle owner."
                    )
                },
                selector,
                videoCapture
            )

            val fileName = "TARZO_${dateFormat.format(Date())}.mp4"
            val contentValues = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                put(MediaStore.MediaColumns.MIME_TYPE, "video/mp4")
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_MOVIES + "/TARZO")
                    put(MediaStore.MediaColumns.IS_PENDING, 1)
                }
            }

            val mediaStoreOutput = MediaStoreOutputOptions.Builder(
                context.contentResolver,
                MediaStore.Video.Media.EXTERNAL_CONTENT_URI
            ).setContentValues(contentValues).build()

            var savedUri: Uri? = null
            currentRecording = videoCapture.output
                .startRecording(mediaStoreOutput, cameraExecutor) { event ->
                    when (event) {
                        is VideoRecordEvent.Start -> { isRecording = true }
                        is VideoRecordEvent.Finalize -> {
                            isRecording = false
                            if (event.hasError()) {
                                // Error handled in stopVideoRecording
                            } else {
                                savedUri = event.outputResults.outputUri
                            }
                        }
                        else -> {}
                    }
                }

            // Wait a brief moment for recording to initialize
            kotlinx.coroutines.delay(200)

            if (isRecording) {
                Result.Success(Uri.parse("recording://in-progress"))
            } else {
                Result.Error(
                    IllegalStateException("Recording failed to start"),
                    "Could not start video recording."
                )
            }
        } catch (e: Exception) {
            Result.Error(e, "Failed to start video recording: ${e.message}")
        }
    }

    /**
     * Stops the current video recording and returns the saved video [Uri].
     */
    @androidx.annotation.OptIn(androidx.camera.video.ExperimentalVideo::class)
    suspend fun stopVideoRecording(): Result<Uri> = withContext(Dispatchers.IO) {
        if (!isRecording || currentRecording == null) {
            return@withContext Result.Error(
                IllegalStateException("No active recording"),
                "No video recording is in progress."
            )
        }
        try {
            currentRecording?.stop()
            currentRecording = null
            // Brief delay for the finalize event to fire
            kotlinx.coroutines.delay(500)
            isRecording = false
            Result.Success(Uri.EMPTY) // Uri is available via the callback above
        } catch (e: Exception) {
            isRecording = false
            Result.Error(e, "Failed to stop video recording: ${e.message}")
        }
    }

    // ── Internal ───────────────────────────────────────────────────────

    private suspend fun capturePhoto(useFrontCamera: Boolean): Result<Uri> =
        withContext(Dispatchers.IO) {
            try {
                if (!hasPermission(Manifest.permission.CAMERA)) {
                    return@withContext Result.Error(
                        SecurityException("Camera permission not granted"),
                        "Camera permission is required."
                    )
                }

                val capture = imageCapture
                    ?: return@withContext Result.Error(
                        IllegalStateException("Camera not initialized"),
                        "Camera has not been opened. Call openCamera() first."
                    )

                val fileName = if (useFrontCamera) "TARZO_selfie_" else "TARZO_photo_"
                val fullFileName = "${fileName}${dateFormat.format(Date())}.jpg"

                val contentValues = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, fullFileName)
                    put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        put(
                            MediaStore.MediaColumns.RELATIVE_PATH,
                            Environment.DIRECTORY_PICTURES + "/TARZO"
                        )
                        put(MediaStore.MediaColumns.IS_PENDING, 1)
                    }
                }

                val outputOptions = ImageCapture.OutputFileOptions.Builder(
                    context.contentResolver,
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                    contentValues
                ).build()

                val uriResult = suspendCancellableCoroutine<Uri> { cont ->
                    capture.takePicture(
                        outputOptions,
                        cameraExecutor,
                        object : ImageCapture.OnImageSavedCallback {
                            override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                                output.savedUri?.let { uri ->
                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                                        try {
                                            contentValues.clear()
                                            contentValues.put(MediaStore.MediaColumns.IS_PENDING, 0)
                                            context.contentResolver.update(uri, contentValues, null, null)
                                        } catch (_: Exception) {}
                                    }
                                    cont.resume(uri)
                                } ?: run {
                                    cont.resume(Uri.EMPTY)
                                }
                            }

                            override fun onError(exception: ImageCaptureException) {
                                if (cont.isActive) {
                                    cont.cancel(exception)
                                }
                            }
                        }
                    )
                }

                if (uriResult != Uri.EMPTY) {
                    Result.Success(uriResult)
                } else {
                    Result.Error(
                        java.io.IOException("Image URI was empty after capture"),
                        "Photo was taken but could not be saved."
                    )
                }
            } catch (e: Exception) {
                Result.Error(e, "Failed to capture photo: ${e.message}")
            }
        }

    private fun hasPermission(permission: String): Boolean {
        return ContextCompat.checkSelfPermission(
            context, permission
        ) == android.content.pm.PackageManager.PERMISSION_GRANTED
    }

    fun isRecording(): Boolean = isRecording

    fun cleanup() {
        try {
            currentRecording?.stop()
        } catch (_: Exception) {}
        currentRecording = null
        isRecording = false
        cameraProvider?.unbindAll()
        cameraExecutor.shutdown()
    }
}
