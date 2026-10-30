package com.example.data.ai

import android.graphics.Bitmap
import android.util.Log
import com.example.data.model.VehicleInspection
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.Content
import com.google.firebase.ai.type.GenerationConfig
import com.google.firebase.ai.type.Schema
import com.google.firebase.ai.type.content
import com.google.firebase.ai.type.generationConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

data class InspectionStepInfo(
    val id: String,
    val titleArabic: String,
    val titleFrench: String,
    val hintArabic: String,
    val hintFrench: String
)

object AiInspectionService {

    private const val TAG = "AiInspection"

    val REQUIRED_STEPS = listOf(
        InspectionStepInfo(
            id = "front",
            titleArabic = "مقدمة المركبة",
            titleFrench = "Face avant",
            hintArabic = "صورة أمامية مباشرة تظهر المصد والشبك والأنوار الأمامية بوضوح",
            hintFrench = "Vue frontale directe montrant le pare-chocs, la calandre et les phares"
        ),
        InspectionStepInfo(
            id = "rear",
            titleArabic = "خلفية المركبة",
            titleFrench = "Face arrière",
            hintArabic = "صورة خلفية مستقيمة تظهر الصندوق، المصد الخلفي والأنوار",
            hintFrench = "Vue arrière directe montrant le coffre, le pare-chocs et les feux"
        ),
        InspectionStepInfo(
            id = "front_left_corner",
            titleArabic = "الزاوية الأمامية اليسرى",
            titleFrench = "Coin avant gauche",
            hintArabic = "زاوية 45 درجة تشمل الجناح الأمامي الأيسر، العجلة وجزء من الباب",
            hintFrench = "Angle à 45° incluant l'aile avant gauche, la roue et le pare-chocs"
        ),
        InspectionStepInfo(
            id = "front_right_corner",
            titleArabic = "الزاوية الأمامية اليمنى",
            titleFrench = "Coin avant droit",
            hintArabic = "زاوية 45 درجة تشمل الجناح الأمامي الأيمن، العجلة وجزء من الباب",
            hintFrench = "Angle à 45° incluant l'aile avant droite, la roue et le pare-chocs"
        ),
        InspectionStepInfo(
            id = "rear_left_corner",
            titleArabic = "الزاوية الخلفية اليسرى",
            titleFrench = "Coin arrière gauche",
            hintArabic = "زاوية 45 درجة تظهر الرفرف الخلفي الأيسر والعجلة",
            hintFrench = "Angle à 45° montrant l'aile arrière gauche et la roue"
        ),
        InspectionStepInfo(
            id = "rear_right_corner",
            titleArabic = "الزاوية الخلفية اليمنى",
            titleFrench = "Coin arrière droit",
            hintArabic = "زاوية 45 درجة تظهر الرفرف الخلفي الأيمن والعجلة",
            hintFrench = "Angle à 45° montrant l'aile arrière droite et la roue"
        ),
        InspectionStepInfo(
            id = "left_side",
            titleArabic = "الجانب الأيسر كاملاً",
            titleFrench = "Profil gauche complet",
            hintArabic = "صورة أفقية لكامل الجانب الأيسر توضح محاذاة الأبواب واستقامة الهيكل",
            hintFrench = "Vue latérale complète montrant l'alignement des portières"
        ),
        InspectionStepInfo(
            id = "right_side",
            titleArabic = "الجانب الأيمن كاملاً",
            titleFrench = "Profil droit complet",
            hintArabic = "صورة أفقية لكامل الجانب الأيمن توضح محاذاة الأبواب واستقامة الهيكل",
            hintFrench = "Vue latérale complète montrant l'alignement des portières"
        ),
        InspectionStepInfo(
            id = "hood_open",
            titleArabic = "حجرة المحرك (الغطاء مفتوح)",
            titleFrench = "Compartiment moteur ouvert",
            hintArabic = "صورة علوية واضحة لحجرة المحرك والجسور الأمامية وسوائل المحرك",
            hintFrench = "Vue dégagée du compartiment moteur et des longerons"
        ),
        InspectionStepInfo(
            id = "dashboard_odometer",
            titleArabic = "لوحة العدادات مع قراءة الكيلومترات",
            titleFrench = "Tableau de bord et compteur",
            hintArabic = "صورة واضحة وقريبة للوحة العدادات تظهر رقم الكيلومترات بوضوح",
            hintFrench = "Gros plan net du tableau de bord avec le kilométrage lisible"
        ),
        InspectionStepInfo(
            id = "driver_interior",
            titleArabic = "مقعد السائق والمقصورة الداخلية",
            titleFrench = "Poste de conduite & intérieur",
            hintArabic = "صورة توضح مقعد السائق، المقود، والكونسول الوسطي لتقييم الاستهلاك",
            hintFrench = "Vue de l'assise conducteur, du volant et de la console centrale"
        ),
        InspectionStepInfo(
            id = "vin_plate",
            titleArabic = "لوحة رقم الشاسي (VIN Plate)",
            titleFrench = "Plaque constructeur VIN",
            hintArabic = "صورة مقرّبة وقابلة للقراءة لملصق أو لوحة VIN على عمود الباب أو الزجاج",
            hintFrench = "Photo nette et lisible de la plaque ou étiquette VIN constructeur"
        )
    )

    private val panelItemSchema: Schema = Schema.obj(
        properties = mapOf(
            "panel" to Schema.string(description = "Panel name e.g. hood, front_bumper, left_front_fender, right_front_fender, left_doors, right_doors, rear_bumper, roof, trunk, engine_bay, interior"),
            "condition" to Schema.string(description = "Condition: good, fair, or poor"),
            "findings" to Schema.array(
                items = Schema.string(description = "scratch, dent, rust, crack, possible_repaint, uneven_panel_gap"),
                description = "Observed visual findings"
            ),
            "confidence" to Schema.double(description = "Confidence level between 0.0 and 1.0"),
            "note" to Schema.string(description = "Short hedged technical note")
        )
    )

    private val inspectionResponseSchema: Schema = Schema.obj(
        properties = mapOf(
            "odometerKmFromDashboard" to Schema.integer(description = "Detected numeric odometer reading in km, or null if illegible/absent"),
            "odometerConfidence" to Schema.double(description = "Confidence in odometer reading from 0.0 to 1.0"),
            "panels" to Schema.array(
                items = panelItemSchema,
                description = "List of panel assessments"
            ),
            "overallConditionScore" to Schema.integer(description = "Score from 0 to 100"),
            "limitations" to Schema.array(
                items = Schema.string(),
                description = "What could not be assessed from the photos"
            )
        ),
        optionalProperties = listOf("odometerKmFromDashboard")
    )

    private val generativeModel by lazy {
        Firebase.ai.generativeModel(
            modelName = "gemini-3.8-flash",
            generationConfig = generationConfig {
                responseMimeType = "application/json"
                responseSchema = inspectionResponseSchema
            },
            systemInstruction = content {
                text(
                    """
You are an expert impartial automotive damage assessor for Car Passport (جواز السيارة).
You are evaluating 12 photos of a vehicle (VIN provided in prompt):
1. front, 2. rear, 3. front-left corner, 4. front-right corner, 5. rear-left corner, 6. rear-right corner,
7. left side, 8. right side, 9. hood open (engine bay), 10. dashboard with odometer, 11. driver seat/interior, 12. VIN plate.

Mandatory Rules:
1. STRICT JSON ONLY matching the provided responseSchema.
2. NEVER state that an accident happened. Use hedged wording such as "signs consistent with repaint", "panel gap misalignment observed", "minor surface scratch", "wear consistent with normal use", etc.
3. Always recommend a physical inspection by a mechanic.
4. If a photo is unclear, dirty, or angle is insufficient, state so in limitations instead of guessing.
5. Extract odometerKmFromDashboard as integer if readable from photo #10. If not readable or digital dash is off, set odometerKmFromDashboard to null and odometerConfidence to 0.0.
6. Provide overallConditionScore from 0 to 100 based on overall cosmetic and mechanical cleanliness.
                    """.trimIndent()
                )
            }
        )
    }

    /**
     * Executes AI visual inspection on the 12 photos in one request with automatic retry once if needed.
     */
    suspend fun performInspection(
        vin: String,
        photos: List<Bitmap>,
        userId: String
    ): Result<VehicleInspection> = withContext(Dispatchers.IO) {
        if (photos.size < 12) {
            return@withContext Result.failure(IllegalArgumentException("All 12 photos are required for inspection"))
        }

        val inspectionContent = content {
            text("Perform comprehensive AI visual inspection on these 12 photos for vehicle VIN: $vin.")
            for (i in photos.indices) {
                val step = REQUIRED_STEPS[i]
                text("Photo #${i + 1} (${step.id}): ${step.titleFrench} / ${step.titleArabic}")
                image(photos[i])
            }
        }

        var attempts = 0
        var lastException: Exception? = null

        while (attempts < 2) {
            attempts++
            try {
                Log.d(TAG, "Sending multimodal inspection request to Gemini 3.8 Flash (Attempt $attempts)...")
                val response = withTimeout(60_000L) {
                    generativeModel.generateContent(inspectionContent)
                }

                val jsonText = response.text
                if (jsonText.isNullOrBlank()) {
                    throw IllegalStateException("Model returned empty text response")
                }

                val inspection = parseInspectionJson(vin, jsonText, userId)
                return@withContext Result.success(inspection)
            } catch (e: Exception) {
                Log.e(TAG, "Inspection attempt $attempts failed", e)
                lastException = e
            }
        }

        Result.failure(lastException ?: Exception("Inspection failed after retry"))
    }

    private fun parseInspectionJson(
        vin: String,
        rawJson: String,
        userId: String
    ): VehicleInspection {
        val root = JSONObject(rawJson)

        val odoKm = if (root.has("odometerKmFromDashboard") && !root.isNull("odometerKmFromDashboard")) {
            root.optLong("odometerKmFromDashboard", -1L).takeIf { it >= 0 }
        } else null

        val odoConf = root.optDouble("odometerConfidence", 0.0)
        val score = root.optLong("overallConditionScore", 75L).coerceIn(0L, 100L)

        val panelsList = mutableListOf<Map<String, Any>>()
        val panelsJson = root.optJSONArray("panels") ?: JSONArray()
        for (i in 0 until panelsJson.length()) {
            val p = panelsJson.getJSONObject(i)
            val findingsArr = p.optJSONArray("findings") ?: JSONArray()
            val findings = (0 until findingsArr.length()).map { findingsArr.getString(it) }

            panelsList.add(
                mapOf(
                    "panel" to p.optString("panel", "unknown"),
                    "condition" to p.optString("condition", "good"),
                    "findings" to findings,
                    "confidence" to p.optDouble("confidence", 0.8),
                    "note" to p.optString("note", "")
                )
            )
        }

        val limitationsList = mutableListOf<String>()
        val limitationsJson = root.optJSONArray("limitations") ?: JSONArray()
        for (i in 0 until limitationsJson.length()) {
            limitationsList.add(limitationsJson.getString(i))
        }

        val inspectionId = "insp_" + UUID.randomUUID().toString().replace("-", "").take(16)

        return VehicleInspection(
            id = inspectionId,
            vin = vin.trim().uppercase(),
            photoUrls = emptyList(), // Local/Storage references
            odometerKmFromDashboard = odoKm,
            odometerConfidence = odoConf,
            panels = panelsList,
            overallConditionScore = score,
            limitations = limitationsList,
            modelVersion = "gemini-3.8-flash",
            createdBy = userId
        )
    }
}
