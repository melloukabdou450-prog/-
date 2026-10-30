package com.example.data.pricing

import com.example.data.model.EstimateFactor
import com.example.data.model.PriceListing
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.abs
import kotlin.math.max

data class EstimationInput(
    val make: String,
    val model: String,
    val year: Int,
    val km: Long,
    val askingPrice: Double,
    val wilaya: String = "16 - Alger",
    val conditionScore: Long? = null
)

data class EstimationResult(
    val lowPrice: Double,
    val fairPrice: Double,
    val highPrice: Double,
    val conditionAdjustmentDzd: Double,
    val conditionAdjustmentPercent: Double,
    val adjustedFairPrice: Double,
    val diffPercentage: Double,
    val verdictArabic: String?,
    val verdictFrench: String?,
    val comparablesCount: Int,
    val comparisonLevel: String,
    val comparisonLevelLabelArabic: String,
    val comparisonLevelLabelFrench: String,
    val confidence: String, // high, medium, low
    val freshnessDate: String,
    val factors: List<EstimateFactor>,
    val filteredComparables: List<PriceListing>
)

object PriceEstimationEngine {

    fun estimate(
        input: EstimationInput,
        allListings: List<PriceListing>
    ): EstimationResult {
        val targetMake = input.make.trim().lowercase()
        val targetModel = input.model.trim().lowercase()
        val targetYear = input.year
        val targetKm = input.km

        // Step 1: Step-by-step filtering
        // Level 1 (Strict): same make, same model, year within ±2, km within ±30,000
        var level = "strict"
        var levelLabelAr = "مطابقة دقيقة (نفس الطراز، سنة ±2، ممشى ±30,000 كم)"
        var levelLabelFr = "Correspondance stricte (même modèle, année ±2, km ±30 000)"

        var comparables = allListings.filter { listing ->
            val matchMake = listing.make.trim().lowercase() == targetMake
            val matchModel = isModelMatch(listing.model, targetModel)
            val matchYear = abs(listing.year - targetYear) <= 2
            val matchKm = abs(listing.km - targetKm) <= 30000
            matchMake && matchModel && matchYear && matchKm && listing.price > 0
        }

        // Level 2 (Medium): If fewer than 8, year within ±3, km within ±50,000
        if (comparables.size < 8) {
            level = "medium"
            levelLabelAr = "مطابقة متوسطة (نفس الطراز، سنة ±3، ممشى ±50,000 كم)"
            levelLabelFr = "Correspondance moyenne (même modèle, année ±3, km ±50 000)"
            comparables = allListings.filter { listing ->
                val matchMake = listing.make.trim().lowercase() == targetMake
                val matchModel = isModelMatch(listing.model, targetModel)
                val matchYear = abs(listing.year - targetYear) <= 3
                val matchKm = abs(listing.km - targetKm) <= 50000
                matchMake && matchModel && matchYear && matchKm && listing.price > 0
            }
        }

        // Level 3 (Broad): If still fewer than 8, same make only
        if (comparables.size < 8) {
            level = "broad"
            levelLabelAr = "مطابقة موسعة (نفس الصانع مع نطاق موسع)"
            levelLabelFr = "Correspondance élargie (même marque)"
            comparables = allListings.filter { listing ->
                val matchMake = listing.make.trim().lowercase() == targetMake
                matchMake && listing.price > 0
            }
        }

        // Fallback: If still completely empty, fallback to entire market
        if (comparables.isEmpty() && allListings.isNotEmpty()) {
            level = "broad"
            levelLabelAr = "تقدير عام (بيانات السوق الإجمالية)"
            levelLabelFr = "Estimation globale du marché"
            comparables = allListings.filter { it.price > 0 }
        }

        // Step 2: Remove outliers using IQR rule
        val nonOutliers = removeOutliers(comparables)
        val workingSet = if (nonOutliers.isNotEmpty()) nonOutliers else comparables

        val sampleSize = workingSet.size

        // Step 3: Compute adjustments for km and year difference
        // Linear regression for km when n >= 15
        val kmSlope = if (sampleSize >= 15) {
            computeKmRegressionSlope(workingSet)
        } else {
            null
        }

        // Year slope from data
        val yearSlope = computeYearSlope(workingSet)

        var totalKmAdj = 0.0
        var totalYearAdj = 0.0

        val adjustedPrices = workingSet.map { comp ->
            // Km adjustment: if comparable has higher km than target, target is worth more (comp price adjusted up)
            val kmDiff = (comp.km - targetKm).toDouble()
            val kmAdj = if (kmSlope != null && kmSlope < 0) {
                // kmSlope is negative (e.g. -5 DZD per km).
                // If comp.km > targetKm (comp has more km), then target is better.
                // Adjusted comp price = comp.price + (comp.km - targetKm) * abs(kmSlope)
                kmDiff * abs(kmSlope)
            } else {
                // 0.5% per 10,000 km difference
                (kmDiff / 10000.0) * (0.005 * comp.price)
            }

            // Year adjustment: if target is newer than comparable, target is worth more
            val yearDiff = (targetYear - comp.year).toDouble()
            val yearAdj = if (yearSlope != null && yearSlope > 0) {
                yearDiff * yearSlope
            } else {
                // Default 5% per year
                yearDiff * (0.05 * comp.price)
            }

            totalKmAdj += kmAdj
            totalYearAdj += yearAdj

            max(200000.0, comp.price + kmAdj + yearAdj)
        }

        val avgKmAdj = if (sampleSize > 0) totalKmAdj / sampleSize else 0.0
        val avgYearAdj = if (sampleSize > 0) totalYearAdj / sampleSize else 0.0

        // Step 4: Calculate percentiles
        val sortedAdjusted = if (adjustedPrices.isNotEmpty()) {
            adjustedPrices.sorted()
        } else {
            listOf(input.askingPrice.coerceAtLeast(1000000.0))
        }

        val rawLow = calculatePercentile(sortedAdjusted, 25.0)
        val rawFair = calculatePercentile(sortedAdjusted, 50.0)
        val rawHigh = calculatePercentile(sortedAdjusted, 75.0)

        // Step 5: Apply condition adjustment from Stage 2 inspection score
        // Below 60 reduces fair price by up to 10%, above 85 adds up to 3%
        var conditionPercent = 0.0
        val score = input.conditionScore
        if (score != null) {
            if (score < 60) {
                val ratio = (60.0 - score) / 60.0
                conditionPercent = -0.10 * ratio
            } else if (score > 85) {
                val ratio = (score - 85.0) / 15.0
                conditionPercent = 0.03 * ratio
            }
        }

        val conditionAdjDzd = rawFair * conditionPercent
        val adjustedFair = rawFair + conditionAdjDzd
        val adjustedLow = rawLow + (rawLow * conditionPercent)
        val adjustedHigh = rawHigh + (rawHigh * conditionPercent)

        // Step 6: Confidence classification
        // High (>=25 comparables), Medium (10-24), Low (<10)
        val confidence = when {
            sampleSize >= 25 -> "high"
            sampleSize >= 10 -> "medium"
            else -> "low"
        }

        // Step 7: Verdict
        val asking = input.askingPrice
        val diffPercent = if (adjustedFair > 0) {
            ((asking - adjustedFair) / adjustedFair) * 100.0
        } else {
            0.0
        }

        val (verdictAr, verdictFr) = if (confidence == "low") {
            null to null // Low confidence: do not show a verdict!
        } else {
            when {
                diffPercent < -5.0 -> "أقل من العادل" to "Sous le prix équitable"
                diffPercent > 5.0 -> "أعلى من العادل" to "Au-dessus du prix équitable"
                else -> "عادل" to "Prix équitable"
            }
        }

        // Top factors
        val factors = mutableListOf<EstimateFactor>()

        factors.add(
            EstimateFactor(
                key = "km_diff",
                nameArabic = "فارق المسافة المقطوعة (العداد)",
                nameFrench = "Différence de kilométrage",
                amountDzd = avgKmAdj,
                descriptionArabic = if (avgKmAdj >= 0) "ممشى أقل من متوسط السوق (+${formatDzd(avgKmAdj)})" else "ممشى أعلى من متوسط السوق (${formatDzd(avgKmAdj)})",
                descriptionFrench = if (avgKmAdj >= 0) "Kilométrage inférieur à la moyenne" else "Kilométrage supérieur à la moyenne"
            )
        )

        factors.add(
            EstimateFactor(
                key = "year_diff",
                nameArabic = "فارق سنة الصنع",
                nameFrench = "Différence d'année modèle",
                amountDzd = avgYearAdj,
                descriptionArabic = if (avgYearAdj >= 0) "حداثة سنة الصنع (+${formatDzd(avgYearAdj)})" else "فارق التقادم (${formatDzd(avgYearAdj)})",
                descriptionFrench = if (avgYearAdj >= 0) "Bonus d'année récente" else "Décote d'ancienneté"
            )
        )

        if (score != null) {
            factors.add(
                EstimateFactor(
                    key = "condition_score",
                    nameArabic = "تعديل الفحص البصري (مؤشر الحالة $score/100)",
                    nameFrench = "Ajustement inspection visuelle (score $score/100)",
                    amountDzd = conditionAdjDzd,
                    descriptionArabic = if (conditionAdjDzd >= 0) "حالة ممتازة (+${String.format(Locale.US, "%.1f", conditionPercent * 100)}%)" else "ملاحظات على الهيكل (${String.format(Locale.US, "%.1f", conditionPercent * 100)}%)",
                    descriptionFrench = if (conditionAdjDzd >= 0) "Bonne condition visuelle" else "Défauts constatés"
                )
            )
        }

        // Wilaya regional variation factor
        val wilayaAdj = if (input.wilaya.contains("16") || input.wilaya.contains("Alger", ignoreCase = true)) {
            adjustedFair * 0.015
        } else if (input.wilaya.contains("31") || input.wilaya.contains("Oran", ignoreCase = true)) {
            adjustedFair * 0.01
        } else {
            0.0
        }
        if (wilayaAdj != 0.0) {
            factors.add(
                EstimateFactor(
                    key = "wilaya_market",
                    nameArabic = "الطلب بالولاية (${input.wilaya})",
                    nameFrench = "Marché de la wilaya (${input.wilaya})",
                    amountDzd = wilayaAdj,
                    descriptionArabic = "فارق الطلب الجغرافي بالمنطقة",
                    descriptionFrench = "Variation de la demande locale"
                )
            )
        }

        // Data freshness
        val newestDate = workingSet.mapNotNull { it.createdAt?.toDate() }.maxOrNull()
        val freshnessDate = if (newestDate != null) {
            SimpleDateFormat("yyyy-MM-dd", Locale.US).format(newestDate)
        } else {
            SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
        }

        return EstimationResult(
            lowPrice = adjustedLow,
            fairPrice = adjustedFair,
            highPrice = adjustedHigh,
            conditionAdjustmentDzd = conditionAdjDzd,
            conditionAdjustmentPercent = conditionPercent * 100.0,
            adjustedFairPrice = adjustedFair,
            diffPercentage = diffPercent,
            verdictArabic = verdictAr,
            verdictFrench = verdictFr,
            comparablesCount = sampleSize,
            comparisonLevel = level,
            comparisonLevelLabelArabic = levelLabelAr,
            comparisonLevelLabelFrench = levelLabelFr,
            confidence = confidence,
            freshnessDate = freshnessDate,
            factors = factors,
            filteredComparables = workingSet
        )
    }

    private fun isModelMatch(model1: String, model2: String): Boolean {
        val m1 = model1.trim().lowercase()
        val m2 = model2.trim().lowercase()
        return m1 == m2 || m1.contains(m2) || m2.contains(m1)
    }

    private fun removeOutliers(listings: List<PriceListing>): List<PriceListing> {
        if (listings.size < 4) return listings
        val sorted = listings.sortedBy { it.price }
        val prices = sorted.map { it.price }
        val q1 = calculatePercentile(prices, 25.0)
        val q3 = calculatePercentile(prices, 75.0)
        val iqr = q3 - q1
        val lowerBound = q1 - 1.5 * iqr
        val upperBound = q3 + 1.5 * iqr
        return sorted.filter { it.price in lowerBound..upperBound }
    }

    private fun computeKmRegressionSlope(listings: List<PriceListing>): Double? {
        if (listings.size < 3) return null
        val meanKm = listings.map { it.km.toDouble() }.average()
        val meanPrice = listings.map { it.price }.average()

        var cov = 0.0
        var varKm = 0.0
        for (item in listings) {
            val dKm = item.km - meanKm
            val dPrice = item.price - meanPrice
            cov += dKm * dPrice
            varKm += dKm * dKm
        }
        if (varKm <= 0) return null
        val slope = cov / varKm
        return if (slope < 0) slope else null
    }

    private fun computeYearSlope(listings: List<PriceListing>): Double? {
        if (listings.size < 3) return null
        val meanYear = listings.map { it.year.toDouble() }.average()
        val meanPrice = listings.map { it.price }.average()

        var cov = 0.0
        var varYear = 0.0
        for (item in listings) {
            val dYear = item.year - meanYear
            val dPrice = item.price - meanPrice
            cov += dYear * dPrice
            varYear += dYear * dYear
        }
        if (varYear <= 0) return null
        val slope = cov / varYear
        return if (slope > 0) slope else null
    }

    fun calculatePercentile(sortedValues: List<Double>, percentile: Double): Double {
        if (sortedValues.isEmpty()) return 0.0
        if (sortedValues.size == 1) return sortedValues.first()
        val p = percentile.coerceIn(0.0, 100.0) / 100.0
        val index = p * (sortedValues.size - 1)
        val lower = index.toInt()
        val upper = (lower + 1).coerceAtMost(sortedValues.size - 1)
        val weight = index - lower
        return sortedValues[lower] * (1.0 - weight) + sortedValues[upper] * weight
    }

    private fun formatDzd(amount: Double): String {
        return String.format(Locale.US, "%,.0f د.ج", amount)
    }
}
