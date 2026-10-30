package com.example

import com.example.data.model.PriceListing
import com.example.data.pricing.EstimationInput
import com.example.data.pricing.PriceEstimationEngine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PriceEstimationEngineTest {

    private fun createSampleListings(): List<PriceListing> {
        val list = mutableListOf<PriceListing>()
        // 12 Clio 4 listings around 2019, 80,000km, ~2,200,000 DZD
        for (i in 1..12) {
            list.add(
                PriceListing(
                    id = "clio_$i",
                    make = "Renault",
                    model = "Clio 4",
                    year = 2018 + (i % 3), // 2018, 2019, 2020
                    km = 70000L + (i * 2000), // 72k to 94k km
                    price = 2100000.0 + (i * 25000), // ~2.12M to 2.4M
                    wilaya = "16 - Alger",
                    fuel = "Essence",
                    transmission = "Manuelle",
                    source = "csv"
                )
            )
        }
        return list
    }

    @Test
    fun testEstimation_strictMatchingWithMediumConfidence() {
        val listings = createSampleListings()
        val input = EstimationInput(
            make = "Renault",
            model = "Clio 4",
            year = 2019,
            km = 80000L,
            askingPrice = 2250000.0,
            conditionScore = 80L
        )

        val result = PriceEstimationEngine.estimate(input, listings)

        assertEquals("strict", result.comparisonLevel)
        assertEquals("medium", result.confidence)
        assertEquals(12, result.comparablesCount)
        assertTrue(result.fairPrice > 2000000.0)
        assertTrue(result.lowPrice <= result.fairPrice)
        assertTrue(result.highPrice >= result.fairPrice)
        assertNotNull(result.verdictArabic)
        assertEquals("عادل", result.verdictArabic)
    }

    @Test
    fun testEstimation_conditionScoreAdjustment_lowScoreReducesPrice() {
        val listings = createSampleListings()
        // Condition score 30 (< 60): ratio = (60-30)/60 = 0.5 -> -5%
        val inputWithLowCondition = EstimationInput(
            make = "Renault",
            model = "Clio 4",
            year = 2019,
            km = 80000L,
            askingPrice = 2250000.0,
            conditionScore = 30L
        )
        val resultLow = PriceEstimationEngine.estimate(inputWithLowCondition, listings)

        assertTrue("Condition adjustment must be negative for score < 60", resultLow.conditionAdjustmentDzd < 0)
        assertEquals(-5.0, resultLow.conditionAdjustmentPercent, 0.5)
    }

    @Test
    fun testEstimation_conditionScoreAdjustment_highScoreBonus() {
        val listings = createSampleListings()
        // Condition score 100 (> 85): ratio = (100-85)/15 = 1.0 -> +3%
        val inputWithHighScore = EstimationInput(
            make = "Renault",
            model = "Clio 4",
            year = 2019,
            km = 80000L,
            askingPrice = 2250000.0,
            conditionScore = 100L
        )
        val resultHigh = PriceEstimationEngine.estimate(inputWithHighScore, listings)

        assertTrue("Condition adjustment must be positive for score > 85", resultHigh.conditionAdjustmentDzd > 0)
        assertEquals(3.0, resultHigh.conditionAdjustmentPercent, 0.1)
    }

    @Test
    fun testEstimation_lowConfidence_hasNoVerdict() {
        // Only 3 listings (< 10)
        val smallListings = createSampleListings().take(3)
        val input = EstimationInput(
            make = "Renault",
            model = "Clio 4",
            year = 2019,
            km = 80000L,
            askingPrice = 2250000.0
        )

        val result = PriceEstimationEngine.estimate(input, smallListings)

        assertEquals("low", result.confidence)
        assertNull("Low confidence must not show a verdict", result.verdictArabic)
    }

    @Test
    fun testEstimation_outlierRemoval() {
        val listings = createSampleListings().toMutableList()
        // Add extreme outlier (15,000,000 DZD)
        listings.add(
            PriceListing(
                id = "outlier_1",
                make = "Renault",
                model = "Clio 4",
                year = 2019,
                km = 80000L,
                price = 15000000.0,
                wilaya = "16 - Alger"
            )
        )

        val input = EstimationInput(
            make = "Renault",
            model = "Clio 4",
            year = 2019,
            km = 80000L,
            askingPrice = 2250000.0
        )

        val result = PriceEstimationEngine.estimate(input, listings)
        // Outlier should be removed by IQR rule
        assertTrue(result.highPrice < 3500000.0)
    }
}
