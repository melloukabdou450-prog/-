package com.example.data.model

data class AntiFraudSignal(
    val type: String, // "EXCESSIVE_DAILY_EVENTS", "DUPLICATE_ODOMETER_ACROSS_VINS", "MULTI_GARAGE_SAME_DAY", "LOW_CONFIRMATION_RATE"
    val severity: String, // "CRITICAL", "WARNING", "INFO"
    val titleArabic: String,
    val titleFrench: String,
    val descriptionArabic: String,
    val descriptionFrench: String,
    val garageId: String = "",
    val garageName: String = "",
    val vin: String = "",
    val metricValue: String = ""
)

data class AntiFraudReport(
    val totalGarages: Int = 0,
    val totalEvents: Int = 0,
    val totalSignals: Int = 0,
    val signals: List<AntiFraudSignal> = emptyList(),
    val highRiskGarageIds: Set<String> = emptySet(),
    val disputedEventsCount: Int = 0
)
