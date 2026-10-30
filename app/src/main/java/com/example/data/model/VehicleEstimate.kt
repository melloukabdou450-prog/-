package com.example.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

@IgnoreExtraProperties
data class EstimateFactor(
    val key: String = "",
    val nameArabic: String = "",
    val nameFrench: String = "",
    val amountDzd: Double = 0.0,
    val descriptionArabic: String = "",
    val descriptionFrench: String = ""
)

@IgnoreExtraProperties
data class VehicleEstimate(
    val id: String = "",
    val vin: String = "",
    val inputs: Map<String, Any> = emptyMap(),
    val outputs: Map<String, Any> = emptyMap(),
    val factors: List<Map<String, Any>> = emptyList(),
    val comparablesCount: Int = 0,
    val comparisonLevel: String = "strict",
    val comparisonLevelLabelArabic: String = "",
    val comparisonLevelLabelFrench: String = "",
    val confidence: String = "medium", // high, medium, low
    val freshnessDate: String = "",
    val explanation: String = "",
    val createdBy: String = "",
    @ServerTimestamp
    val createdAt: Timestamp? = null
) {
    // Convenience accessors
    val askingPrice: Double
        get() = (inputs["askingPrice"] as? Number)?.toDouble() ?: 0.0

    val targetKm: Long
        get() = (inputs["targetKm"] as? Number)?.toLong() ?: 0L

    val targetYear: Int
        get() = (inputs["targetYear"] as? Number)?.toInt() ?: 0

    val conditionScore: Long?
        get() = (inputs["conditionScore"] as? Number)?.toLong()

    val lowPrice: Double
        get() = (outputs["lowPrice"] as? Number)?.toDouble() ?: 0.0

    val fairPrice: Double
        get() = (outputs["fairPrice"] as? Number)?.toDouble() ?: 0.0

    val highPrice: Double
        get() = (outputs["highPrice"] as? Number)?.toDouble() ?: 0.0

    val conditionAdjustmentDzd: Double
        get() = (outputs["conditionAdjustmentDzd"] as? Number)?.toDouble() ?: 0.0

    val conditionAdjustmentPercent: Double
        get() = (outputs["conditionAdjustmentPercent"] as? Number)?.toDouble() ?: 0.0

    val adjustedFairPrice: Double
        get() = (outputs["adjustedFairPrice"] as? Number)?.toDouble() ?: fairPrice

    val diffPercentage: Double
        get() = (outputs["diffPercentage"] as? Number)?.toDouble() ?: 0.0

    val verdictArabic: String
        get() = outputs["verdictArabic"] as? String ?: ""

    val verdictFrench: String
        get() = outputs["verdictFrench"] as? String ?: ""
}
