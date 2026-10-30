package com.example.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

@IgnoreExtraProperties
data class GarageAccount(
    val uid: String = "",
    val garageName: String = "",
    val wilaya: String = "",
    val commune: String = "",
    val address: String = "",
    val phone: String = "",
    val commercialRegisterPhotoUrl: String = "",
    val status: String = "pending", // "pending" | "approved" | "rejected"
    val rejectionReason: String = "",
    val ownerEmail: String = "",
    @ServerTimestamp
    val createdAt: Timestamp? = null,
    @ServerTimestamp
    val updatedAt: Timestamp? = null
)
