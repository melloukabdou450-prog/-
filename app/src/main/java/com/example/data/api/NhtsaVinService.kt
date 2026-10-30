package com.example.data.api

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.util.concurrent.TimeUnit

data class DecodedVehicleData(
    val vin: String,
    val make: String,
    val model: String,
    val year: Long,
    val engine: String,
    val vehicleType: String,
    val plantCountry: String,
    val hasLowConfidence: Boolean,
    val missingFields: List<String>,
    val rawErrorCode: String = "",
    val rawErrorText: String = ""
)

sealed class VinValidationResult {
    object Valid : VinValidationResult()
    data class Invalid(val reasonArabic: String, val reasonFrench: String) : VinValidationResult()
}

object NhtsaVinService {
    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    private val FORBIDDEN_CHARS = setOf('I', 'O', 'Q', 'i', 'o', 'q')

    /**
     * Strict ISO 3779 VIN Validation (17 alphanumeric characters, excluding I, O, Q)
     */
    fun validateVin(rawVin: String): VinValidationResult {
        val trimmed = rawVin.trim()

        if (trimmed.isEmpty()) {
            return VinValidationResult.Invalid(
                reasonArabic = "يرجى إدخال رقم الشاسي (VIN)",
                reasonFrench = "Veuillez saisir le numéro de châssis (VIN)"
            )
        }

        val forbiddenFound = trimmed.filter { it in FORBIDDEN_CHARS }.toSet()
        if (forbiddenFound.isNotEmpty()) {
            val forbiddenList = forbiddenFound.joinToString(", ") { it.uppercase() }
            return VinValidationResult.Invalid(
                reasonArabic = "رقم VIN لا يمكن أن يحتوي على الأحرف ($forbiddenList) لتجنب الخلط مع الأرقام 1 و 0 وفق معيار ISO 3779",
                reasonFrench = "Le VIN ne doit pas contenir les lettres ($forbiddenList) pour éviter la confusion avec 1 et 0 (norme ISO 3779)"
            )
        }

        if (trimmed.length != 17) {
            return VinValidationResult.Invalid(
                reasonArabic = "يجب أن يتكون رقم VIN من 17 حرفاً ورقم بالضبط (المُدخل حالياً: ${trimmed.length})",
                reasonFrench = "Le numéro VIN doit comporter exactement 17 caractères (actuellement : ${trimmed.length})"
            )
        }

        if (!trimmed.all { it.isLetterOrDigit() }) {
            return VinValidationResult.Invalid(
                reasonArabic = "يجب أن يحتوي رقم VIN على أرقام وحروف إنجليزية فقط دون رموز خاصة",
                reasonFrench = "Le VIN ne doit contenir que des chiffres et des lettres anglaises"
            )
        }

        return VinValidationResult.Valid
    }

    /**
     * Queries NHTSA vPIC API to decode vehicle details.
     */
    suspend fun decodeVin(vin: String): Result<DecodedVehicleData> = withContext(Dispatchers.IO) {
        val cleanVin = vin.trim().uppercase()
        val validation = validateVin(cleanVin)
        if (validation is VinValidationResult.Invalid) {
            return@withContext Result.failure(IllegalArgumentException(validation.reasonArabic))
        }

        try {
            val url = "https://vpic.nhtsa.dot.gov/api/vehicles/decodevinvalues/$cleanVin?format=json"
            val request = Request.Builder()
                .url(url)
                .header("User-Agent", "CarPassport-Android/1.0")
                .get()
                .build()

            val response = httpClient.newCall(request).execute()
            if (!response.isSuccessful) {
                return@withContext Result.failure(Exception("NHTSA API returned HTTP ${response.code}"))
            }

            val body = response.body?.string() ?: return@withContext Result.failure(Exception("Empty API response"))
            val jsonRoot = JSONObject(body)
            val resultsArray = jsonRoot.optJSONArray("Results")

            if (resultsArray == null || resultsArray.length() == 0) {
                return@withContext Result.failure(Exception("No vehicle records found for this VIN"))
            }

            val first = resultsArray.getJSONObject(0)
            val make = first.optString("Make", "").trim()
            val model = first.optString("Model", "").trim()
            val yearStr = first.optString("ModelYear", "").trim()
            val year = yearStr.toLongOrNull() ?: 0L

            val displacement = first.optString("DisplacementL", "").trim()
            val engineConfig = first.optString("EngineConfiguration", "").trim()
            val cylinders = first.optString("EngineCylinders", "").trim()
            val engineHp = first.optString("EngineHP", "").trim()
            val vehicleType = first.optString("VehicleType", "").trim()
            val plantCountry = first.optString("PlantCountry", "").trim()
            val errorCode = first.optString("ErrorCode", "").trim()
            val errorText = first.optString("ErrorText", "").trim()

            // Build engine summary string
            val engineParts = mutableListOf<String>()
            if (displacement.isNotBlank()) engineParts.add("${displacement}L")
            if (engineConfig.isNotBlank()) engineParts.add(engineConfig)
            if (cylinders.isNotBlank()) engineParts.add("${cylinders}-Cyl")
            if (engineHp.isNotBlank()) engineParts.add("${engineHp} HP")
            val engine = if (engineParts.isNotEmpty()) engineParts.joinToString(" ") else "غير محدد / Non spécifié"

            // Evaluate data completeness and confidence
            val missingFields = mutableListOf<String>()
            if (make.isBlank() || make == "0") missingFields.add("المصنّع / Marque")
            if (model.isBlank() || model == "0") missingFields.add("الموديل / Modèle")
            if (year == 0L) missingFields.add("سنة الصنع / Année")
            if (engineParts.isEmpty()) missingFields.add("مواصفات المحرك / Moteur")

            // NHTSA ErrorCode check: 0 is complete success, codes with errors lower confidence
            val hasErrorCodeIssue = errorCode.isNotBlank() && errorCode != "0" && !errorCode.startsWith("0")
            val hasLowConfidence = missingFields.isNotEmpty() || hasErrorCodeIssue

            val decoded = DecodedVehicleData(
                vin = cleanVin,
                make = if (make.isNotBlank() && make != "0") make else "غير معروف / Inconnu",
                model = if (model.isNotBlank() && model != "0") model else "غير معروف / Inconnu",
                year = if (year > 0) year else 2020L,
                engine = engine,
                vehicleType = vehicleType.ifBlank { "سيارة ركاب / Véhicule particulier" },
                plantCountry = plantCountry.ifBlank { "غير محدد / Non spécifié" },
                hasLowConfidence = hasLowConfidence,
                missingFields = missingFields,
                rawErrorCode = errorCode,
                rawErrorText = errorText
            )

            Result.success(decoded)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
