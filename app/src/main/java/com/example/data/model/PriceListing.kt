package com.example.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

@IgnoreExtraProperties
data class PriceListing(
    val id: String = "",
    val make: String = "",
    val model: String = "",
    val year: Int = 0,
    val km: Long = 0L,
    val price: Double = 0.0,
    val wilaya: String = "",
    val fuel: String = "Essence",
    val transmission: String = "Manuelle",
    val source: String = "manual", // "manual" | "csv"
    val createdBy: String = "",
    @ServerTimestamp
    val createdAt: Timestamp? = null
)
