package com.example.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

@IgnoreExtraProperties
data class VehicleEvent(
    val id: String = "",
    val vin: String = "",
    val eventType: String = "service", // service, repair, odometer, inspection, correction
    val title: String = "",
    val description: String = "",
    val date: String = "", // YYYY-MM-DD
    val odometerKm: Long = 0L,
    val verificationLevel: String = "self_declared", // verified, self_declared, dealer_certified, GARAGE_DECLARED, VERIFIED_BY_PARTNER + CUSTOMER_CONFIRMED
    val serviceProvider: String = "",
    val sourceType: String = "OWNER", // "OWNER" | "GARAGE"
    val garageId: String = "",
    val garageName: String = "",
    val serviceTemplates: List<String> = emptyList(),
    val priceDzd: Double? = null,
    val invoicePhotoUrl: String = "",
    val hasOdometerAnomaly: Boolean = false,
    val confirmationCodeHash: String = "",
    val confirmationStatus: String = "PENDING", // PENDING, CONFIRMED, DISPUTED
    val customerPhoneHash: String = "", // ONLY hash of customer phone, NEVER plain text
    val disputedReason: String = "",
    val confirmedAt: Timestamp? = null,
    val prevHash: String = "0000000000000000000000000000000000000000000000000000000000000000",
    val eventHash: String = "",
    val isCorrection: Boolean = false,
    val correctedEventId: String = "",
    val createdBy: String = "",
    @ServerTimestamp
    val createdAt: Timestamp? = null
)
