package com.example.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

@IgnoreExtraProperties
data class PanelFinding(
    val panel: String = "",
    val condition: String = "good", // good, fair, poor
    val findings: List<String> = emptyList(), // scratch, dent, rust, crack, possible_repaint, uneven_panel_gap
    val confidence: Double = 1.0,
    val note: String = ""
)

@IgnoreExtraProperties
data class VehicleInspection(
    val id: String = "",
    val vin: String = "",
    val photoUrls: List<String> = emptyList(),
    val odometerKmFromDashboard: Long? = null,
    val odometerConfidence: Double = 0.0,
    val panels: List<Map<String, Any>> = emptyList(),
    val overallConditionScore: Long = 0L,
    val limitations: List<String> = emptyList(),
    val modelVersion: String = "gemini-3.8-flash",
    val createdBy: String = "",
    @ServerTimestamp
    val createdAt: Timestamp? = null
)
