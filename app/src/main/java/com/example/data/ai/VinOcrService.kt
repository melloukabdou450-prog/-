package com.example.data.ai

import android.graphics.Bitmap
import android.util.Log
import com.example.data.api.NhtsaVinService
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.Schema
import com.google.firebase.ai.type.content
import com.google.firebase.ai.type.generationConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import org.json.JSONObject

data class VinOcrResult(
    val vin: String,
    val isValid: Boolean,
    val confidence: Double,
    val errorMessage: String = ""
)

object VinOcrService {
    private const val TAG = "VinOcrService"

    private val ocrResponseSchema: Schema = Schema.obj(
        properties = mapOf(
            "vin" to Schema.string(description = "17-character Vehicle Identification Number, uppercase without spaces, or empty string if none found"),
            "confidence" to Schema.double(description = "Confidence in the detected VIN between 0.0 and 1.0"),
            "rawTextDetected" to Schema.string(description = "Any relevant raw VIN or chassis number text observed")
        )
    )

    private val generativeModel by lazy {
        Firebase.ai.generativeModel(
            modelName = "gemini-3.8-flash",
            generationConfig = generationConfig {
                responseMimeType = "application/json"
                responseSchema = ocrResponseSchema
            },
            systemInstruction = content {
                text(
                    """
You are an expert automotive OCR scanner for Car Passport (جواز السيارة).
Your job is to read the 17-character Vehicle Identification Number (VIN) from the provided image.
Rules:
1. Standard VINs are exactly 17 characters long.
2. Standard VINs never include the letters I, O, or Q (to prevent confusion with 1 and 0).
3. Look for VIN plates, dashboard corner tags, B-pillar stickers, chassis stampings, registration documents (Carte Grise), or bar codes.
4. If characters are ambiguous, resolve standard VIN confusions (e.g. letter O is digit 0, letter I is digit 1).
5. Return JSON with the extracted uppercase 17-character VIN and confidence score.
                    """.trimIndent()
                )
            }
        )
    }

    /**
     * Extracts 17-character VIN from a photo.
     */
    suspend fun extractVinFromImage(bitmap: Bitmap): VinOcrResult = withContext(Dispatchers.IO) {
        try {
            val scaledBitmap = scaleBitmapDown(bitmap, maxDimension = 1280)
            val promptContent = content {
                image(scaledBitmap)
                text("Scan this image and extract the 17-character vehicle identification number (VIN).")
            }

            val response = withTimeout(20_000L) {
                generativeModel.generateContent(promptContent)
            }

            val rawJson = response.text ?: ""
            Log.d(TAG, "OCR response: $rawJson")
            val json = JSONObject(rawJson)

            val rawVin = json.optString("vin", "").trim().uppercase().replace(" ", "").replace("-", "")
            val confidence = json.optDouble("confidence", 0.0)

            // Validate VIN format
            val validation = NhtsaVinService.validateVin(rawVin)
            if (validation is com.example.data.api.VinValidationResult.Valid) {
                VinOcrResult(
                    vin = rawVin,
                    isValid = true,
                    confidence = confidence
                )
            } else if (rawVin.length == 17) {
                // If 17 chars but invalid (e.g. has I, O, Q), sanitize
                val sanitized = rawVin.replace('I', '1').replace('O', '0').replace('Q', '0')
                val sanValidation = NhtsaVinService.validateVin(sanitized)
                if (sanValidation is com.example.data.api.VinValidationResult.Valid) {
                    VinOcrResult(
                        vin = sanitized,
                        isValid = true,
                        confidence = confidence * 0.9
                    )
                } else {
                    val msg = if (sanValidation is com.example.data.api.VinValidationResult.Invalid) sanValidation.reasonArabic else ""
                    VinOcrResult(
                        vin = rawVin,
                        isValid = false,
                        confidence = confidence,
                        errorMessage = msg
                    )
                }
            } else {
                VinOcrResult(
                    vin = rawVin,
                    isValid = false,
                    confidence = confidence,
                    errorMessage = if (rawVin.isBlank()) "لم يتم العثور على رقم VIN واضح بالصورة" else "رقم VIN غير مكتمل (${rawVin.length}/17)"
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "VIN OCR failed", e)
            VinOcrResult(
                vin = "",
                isValid = false,
                confidence = 0.0,
                errorMessage = "تعذر مسح رقم VIN: ${e.message}"
            )
        }
    }

    private fun scaleBitmapDown(bitmap: Bitmap, maxDimension: Int): Bitmap {
        val width = bitmap.width
        val height = bitmap.height
        val maxSide = maxOf(width, height)
        if (maxSide <= maxDimension) return bitmap

        val scale = maxDimension.toFloat() / maxSide.toFloat()
        val targetWidth = (width * scale).toInt()
        val targetHeight = (height * scale).toInt()
        return Bitmap.createScaledBitmap(bitmap, targetWidth, targetHeight, true)
    }
}
