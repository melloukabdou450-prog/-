package com.example.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

@IgnoreExtraProperties
data class Vehicle(
    val vin: String = "",
    val make: String = "",
    val model: String = "",
    val year: Long = 0L,
    val engine: String = "",
    val vehicleType: String = "",
    val plantCountry: String = "",
    val hasLowConfidence: Boolean = false,
    val missingFields: List<String> = emptyList(),
    val latestOdometerKm: Long = 0L,
    val createdBy: String = "",
    @ServerTimestamp
    val createdAt: Timestamp? = null,
    @ServerTimestamp
    val updatedAt: Timestamp? = null
)
