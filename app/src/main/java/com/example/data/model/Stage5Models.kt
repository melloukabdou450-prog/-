package com.example.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

@IgnoreExtraProperties
data class PublicReport(
    val publicId: String = "",
    val vin: String = "",
    val maskedVin: String = "",
    val make: String = "",
    val model: String = "",
    val year: Int = 0,
    val engine: String = "",
    val completenessScore: Int = 0,
    val overallScore: Int = 0,
    val priceVerdict: String = "",
    val fairPrice: Double = 0.0,
    val lowPrice: Double = 0.0,
    val highPrice: Double = 0.0,
    val eventsCount: Int = 0,
    val ownersCount: Int = 1,
    val riskFlagsCount: Int = 0,
    val hasOdometerAnomaly: Boolean = false,
    val eventsData: List<Map<String, Any>> = emptyList(),
    val inspectionSummary: Map<String, Any> = emptyMap(),
    val estimateData: Map<String, Any> = emptyMap(),
    val transparencyData: Map<String, Any> = emptyMap(),
    val generatedBy: String = "",
    @ServerTimestamp
    val generatedAt: Timestamp? = null,
    @ServerTimestamp
    val expiresAt: Timestamp? = null,
    val modelVersion: String = "gemini-3.8-flash"
)

@IgnoreExtraProperties
data class WalletDocument(
    val id: String = "",
    val vin: String = "",
    val docType: String = "carte_grise", // carte_grise, insurance, invoice, other
    val title: String = "",
    val photoUrl: String = "",
    val expiryDate: String = "", // YYYY-MM-DD
    val ownerUid: String = "",
    @ServerTimestamp
    val createdAt: Timestamp? = null
)

@IgnoreExtraProperties
data class MaintenanceReminder(
    val id: String = "",
    val vin: String = "",
    val reminderType: String = "oil", // oil, filters, timing_belt, controle_technique, insurance
    val title: String = "",
    val targetKm: Long = 0L,
    val targetDate: String = "", // YYYY-MM-DD
    val intervalKm: Long = 10000L,
    val intervalMonths: Int = 12,
    val status: String = "OK", // OK, SOON, OVERDUE
    val ownerUid: String = "",
    @ServerTimestamp
    val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class DisputeRecord(
    val id: String = "",
    val vin: String = "",
    val eventId: String = "",
    val reportedBy: String = "",
    val reason: String = "",
    val evidencePhotoUrl: String = "",
    val status: String = "open", // open, under_review, resolved
    val resolutionNotes: String = "",
    @ServerTimestamp
    val createdAt: Timestamp? = null,
    @ServerTimestamp
    val resolvedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class DataDeletionRequest(
    val id: String = "",
    val userId: String = "",
    val userEmail: String = "",
    val reason: String = "",
    val status: String = "pending", // pending, in_progress, completed, rejected
    @ServerTimestamp
    val requestedAt: Timestamp? = null,
    @ServerTimestamp
    val processedAt: Timestamp? = null
)
