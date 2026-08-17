package com.tarzo.ai.features.vision

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.label.ImageLabeling
import com.google.mlkit.vision.label.defaults.ImageLabelerOptions
import com.google.mlkit.vision.objects.ObjectDetection
import com.google.mlkit.vision.objects.defaults.ObjectDetectorOptions
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import com.tarzo.ai.util.Result
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume

/**
 * Structured result from image analysis containing OCR text, detected objects, and image labels.
 */
data class VisionResult(
    val detectedText: String,
    val textBlocks: List<TextBlock>,
    val detectedObjects: List<DetectedObject>,
    val imageLabels: List<ImageLabel>
) {
    data class TextBlock(
        val text: String,
        val boundingBox: String, // descriptive string for accessibility
        val confidence: Float
    )

    data class DetectedObject(
        val label: String,
        val confidence: Float,
        val boundingBox: String
    )

    data class ImageLabel(
        val text: String,
        val confidence: Float,
        val index: Int
    )

    /**
     * Returns a human-readable summary of all analysis results.
     */
    fun toSummary(): String {
        val parts = mutableListOf<String>()
        if (detectedText.isNotBlank()) {
            parts.add("Text found: ${detectedText.take(200)}")
        }
        if (detectedObjects.isNotEmpty()) {
            val objects = detectedObjects.joinToString(", ") { it.label }
            parts.add("Objects detected: $objects")
        }
        if (imageLabels.isNotEmpty()) {
            val labels = imageLabels.take(5).joinToString(", ") { "${it.text} (${(it.confidence * 100).toInt()}%)" }
            parts.add("Labels: $labels")
        }
        return if (parts.isEmpty()) "No content detected in the image." else parts.joinToString("\n")
    }
}

/**
 * Analyzes images using ML Kit: text recognition (OCR), object detection, and image labeling.
 * All processing runs on-device and does not require an internet connection.
 *
 * Each method accepts a content [Uri] and returns a [Result] with a [VisionResult] or error.
 */
@Singleton
class VisionAnalyzer @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val textRecognizer by lazy {
        TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
    }

    private val objectDetector by lazy {
        ObjectDetection.getClient(
            ObjectDetectorOptions.Builder()
                .setDetectorMode(ObjectDetectorOptions.SINGLE_IMAGE_MODE)
                .enableMultipleObjects()
                .build()
        )
    }

    private val imageLabeler by lazy {
        ImageLabeling.getClient(
            ImageLabelerOptions.Builder()
                .setConfidenceThreshold(0.5f)
                .build()
        )
    }

    /**
     * Performs full analysis on the image at [imageUri]: OCR, object detection, and image labeling.
     * @return [VisionResult] containing all detected information.
     */
    suspend fun analyzeImage(imageUri: Uri): Result<VisionResult> = withContext(Dispatchers.IO) {
        try {
            val inputImage = InputImage.fromFilePath(context, imageUri)

            val textResult = recognizeText(inputImage)
            val objectsResult = detectObjects(inputImage)
            val labelsResult = labelImage(inputImage)

            Result.Success(
                VisionResult(
                    detectedText = textResult.getOrDefault(""),
                    textBlocks = textResult.blocks,
                    detectedObjects = objectsResult,
                    imageLabels = labelsResult
                )
            )
        } catch (e: Exception) {
            Result.Error(e, "Image analysis failed: ${e.message}")
        }
    }

    /**
     * Performs OCR text recognition on the given image.
     */
    suspend fun recognizeTextOnly(imageUri: Uri): Result<String> = withContext(Dispatchers.IO) {
        try {
            val inputImage = InputImage.fromFilePath(context, imageUri)
            val result = recognizeText(inputImage)
            val text = result.getOrDefault("")
            if (text.isNotBlank()) {
                Result.Success(text)
            } else {
                Result.Success("No text detected in the image.")
            }
        } catch (e: Exception) {
            Result.Error(e, "Text recognition failed: ${e.message}")
        }
    }

    /**
     * Performs object detection on the given image.
     */
    suspend fun detectObjectsOnly(imageUri: Uri): Result<List<VisionResult.DetectedObject>> =
        withContext(Dispatchers.IO) {
            try {
                val inputImage = InputImage.fromFilePath(context, imageUri)
                val objects = detectObjects(inputImage)
                if (objects.isNotEmpty()) {
                    Result.Success(objects)
                } else {
                    Result.Success(emptyList())
                }
            } catch (e: Exception) {
                Result.Error(e, "Object detection failed: ${e.message}")
            }
        }

    // ── Internal ML Kit Calls ──────────────────────────────────────────

    private suspend fun recognizeText(inputImage: InputImage): OcrResult =
        suspendCancellableCoroutine { cont ->
            textRecognizer.process(inputImage)
                .addOnSuccessListener { visionText ->
                    val blocks = visionText.textBlocks.map { block ->
                        VisionResult.TextBlock(
                            text = block.text,
                            boundingBox = block.boundingBox?.toShortString() ?: "unknown",
                            confidence = block.confidence ?: 0f
                        )
                    }
                    cont.resume(OcrResult(
                        fullText = visionText.text,
                        blocks = blocks
                    ))
                }
                .addOnFailureListener { e ->
                    cont.resume(OcrResult(fullText = "", blocks = emptyList()))
                }
        }

    private suspend fun detectObjects(inputImage: InputImage): List<VisionResult.DetectedObject> =
        suspendCancellableCoroutine { cont ->
            objectDetector.process(inputImage)
                .addOnSuccessListener { detectedObjects ->
                    val objects = detectedObjects.map { obj ->
                        VisionResult.DetectedObject(
                            label = obj.labels.firstOrNull()?.text ?: "Unknown",
                            confidence = obj.labels.firstOrNull()?.confidence ?: 0f,
                            boundingBox = obj.boundingBox.toShortString()
                        )
                    }
                    cont.resume(objects)
                }
                .addOnFailureListener {
                    cont.resume(emptyList())
                }
        }

    private suspend fun labelImage(inputImage: InputImage): List<VisionResult.ImageLabel> =
        suspendCancellableCoroutine { cont ->
            imageLabeler.process(inputImage)
                .addOnSuccessListener { labels ->
                    val imageLabels = labels.map { label ->
                        VisionResult.ImageLabel(
                            text = label.text,
                            confidence = label.confidence,
                            index = label.index
                        )
                    }
                    cont.resume(imageLabels)
                }
                .addOnFailureListener {
                    cont.resume(emptyList())
                }
        }

    // ── Helper Classes ─────────────────────────────────────────────────

    private data class OcrResult(
        val fullText: String,
        val blocks: List<VisionResult.TextBlock>
    ) {
        fun getOrDefault(default: String): String = fullText.ifBlank { default }
    }

    private fun android.graphics.Rect.toShortString(): String {
        return "[${left}, $top, $right, $bottom]"
    }

    /**
     * Loads a [Bitmap] from a content [Uri] for use with ML Kit.
     * Kept as a utility for potential future use with InputImage.fromBitmap.
     */
    private fun loadBitmapFromUri(uri: Uri): Bitmap? {
        return try {
            context.contentResolver.openInputStream(uri)?.use { stream ->
                val options = BitmapFactory.Options()
                options.inSampleSize = 2 // Reduce memory for large images
                BitmapFactory.decodeStream(stream, null, options)
            }
        } catch (e: Exception) {
            null
        }
    }
}
