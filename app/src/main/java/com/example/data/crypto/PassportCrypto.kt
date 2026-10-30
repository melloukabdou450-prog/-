package com.example.data.crypto

import com.example.data.model.VehicleEvent
import java.security.MessageDigest

object PassportCrypto {
    const val GENESIS_HASH = "0000000000000000000000000000000000000000000000000000000000000000"

    fun sha256(input: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val hashBytes = digest.digest(input.toByteArray(Charsets.UTF_8))
        return hashBytes.joinToString("") { "%02x".format(it) }
    }

    /**
     * Computes the deterministic SHA-256 event hash combining canonical event attributes + prevHash.
     */
    fun computeEventHash(
        vin: String,
        eventType: String,
        title: String,
        date: String,
        odometerKm: Long,
        verificationLevel: String,
        prevHash: String
    ): String {
        val canonical = listOf(
            vin.trim().uppercase(),
            eventType.trim().lowercase(),
            title.trim(),
            date.trim(),
            odometerKm.toString(),
            verificationLevel.trim().lowercase(),
            prevHash.trim().lowercase()
        ).joinToString("|")
        return sha256(canonical)
    }

    data class ChainVerificationResult(
        val isValid: Boolean,
        val totalEvents: Int,
        val brokenIndex: Int? = null,
        val brokenEventId: String? = null
    )

    /**
     * Verifies cryptographic chain integrity from genesis to latest event.
     */
    fun verifyChain(events: List<VehicleEvent>): ChainVerificationResult {
        if (events.isEmpty()) return ChainVerificationResult(isValid = true, totalEvents = 0)

        // Events ordered chronologically / by insertion
        var expectedPrevHash = GENESIS_HASH

        for ((index, event) in events.withIndex()) {
            if (index == 0) {
                // First event must either point to genesis or a valid prevHash
                if (event.prevHash != GENESIS_HASH && event.prevHash.length != 64) {
                    return ChainVerificationResult(
                        isValid = false,
                        totalEvents = events.size,
                        brokenIndex = index,
                        brokenEventId = event.id
                    )
                }
            } else {
                if (!event.prevHash.equals(expectedPrevHash, ignoreCase = true)) {
                    return ChainVerificationResult(
                        isValid = false,
                        totalEvents = events.size,
                        brokenIndex = index,
                        brokenEventId = event.id
                    )
                }
            }

            val computed = computeEventHash(
                vin = event.vin,
                eventType = event.eventType,
                title = event.title,
                date = event.date,
                odometerKm = event.odometerKm,
                verificationLevel = event.verificationLevel,
                prevHash = event.prevHash
            )

            if (!event.eventHash.equals(computed, ignoreCase = true)) {
                return ChainVerificationResult(
                    isValid = false,
                    totalEvents = events.size,
                    brokenIndex = index,
                    brokenEventId = event.id
                )
            }

            expectedPrevHash = event.eventHash
        }

        return ChainVerificationResult(isValid = true, totalEvents = events.size)
    }

    data class OdometerAnomaly(
        val eventId: String,
        val eventTitle: String,
        val eventDate: String,
        val currentKm: Long,
        val previousHighestKm: Long,
        val previousDate: String,
        val dropKm: Long
    )

    /**
     * Evaluates chronological odometer progression and flags any rollback or reduction in km.
     */
    fun detectOdometerAnomalies(events: List<VehicleEvent>): List<OdometerAnomaly> {
        val anomalies = mutableListOf<OdometerAnomaly>()
        if (events.isEmpty()) return anomalies

        // Sort events chronologically by date
        val sorted = events.filter { it.odometerKm > 0 }.sortedWith(
            compareBy<VehicleEvent> { it.date }.thenBy { it.createdAt?.seconds ?: 0L }
        )

        var highestKm = 0L
        var highestDate = ""

        for (event in sorted) {
            if (event.odometerKm < highestKm) {
                anomalies.add(
                    OdometerAnomaly(
                        eventId = event.id,
                        eventTitle = event.title,
                        eventDate = event.date,
                        currentKm = event.odometerKm,
                        previousHighestKm = highestKm,
                        previousDate = highestDate,
                        dropKm = highestKm - event.odometerKm
                    )
                )
            } else {
                highestKm = event.odometerKm
                highestDate = event.date
            }
        }

        return anomalies
    }

    /**
     * Calculates data completeness meter percentage (0 - 100%)
     */
    fun calculateCompleteness(
        vehicle: com.example.data.model.Vehicle,
        events: List<VehicleEvent>
    ): Int {
        var score = 0

        // Vehicle spec completeness (up to 40 points)
        if (vehicle.make.isNotBlank()) score += 10
        if (vehicle.model.isNotBlank()) score += 10
        if (vehicle.year > 0) score += 10
        if (vehicle.engine.isNotBlank() && !vehicle.engine.contains("Unknown", ignoreCase = true)) score += 10

        // Events quantity (up to 30 points)
        when {
            events.size >= 5 -> score += 30
            events.size >= 3 -> score += 20
            events.size >= 1 -> score += 10
        }

        // Verification quality (up to 20 points)
        val verifiedCount = events.count { 
            it.verificationLevel == "verified" || 
            it.verificationLevel == "dealer_certified" || 
            it.verificationLevel.contains("VERIFIED_BY_PARTNER")
        }
        when {
            verifiedCount >= 3 -> score += 20
            verifiedCount >= 1 -> score += 10
        }

        // Recent odometer reading (up to 10 points)
        if (events.any { it.odometerKm > 0 }) score += 10

        return score.coerceIn(5, 100)
    }

    /**
     * Cleans an Algerian phone number into canonical format (digits only).
     */
    fun cleanPhone(phone: String): String {
        return phone.replace(Regex("[^0-9]"), "")
    }

    /**
     * Computes SHA-256 hash of a customer phone number so raw numbers are NEVER stored.
     */
    fun hashPhone(phone: String): String {
        val cleaned = cleanPhone(phone)
        if (cleaned.isBlank()) return ""
        return sha256(cleaned)
    }
}
