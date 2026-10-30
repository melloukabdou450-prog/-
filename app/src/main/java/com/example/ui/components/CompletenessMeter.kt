package com.example.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.Vehicle
import com.example.data.model.VehicleEvent
import com.example.ui.localization.AppLanguage
import com.example.ui.localization.AppStrings

@Composable
fun CompletenessMeterCard(
    vehicle: Vehicle,
    events: List<VehicleEvent>,
    completenessScore: Int,
    lang: AppLanguage,
    modifier: Modifier = Modifier
) {
    val animatedProgress by animateFloatAsState(
        targetValue = completenessScore / 100f,
        animationSpec = tween(durationMillis = 800),
        label = "completeness_progress"
    )

    val meterColor = when {
        completenessScore >= 75 -> Color(0xFF10B981) // Emerald Green
        completenessScore >= 45 -> Color(0xFFF59E0B) // Amber
        else -> Color(0xFFEF4444) // Red
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = AppStrings.completenessTitle(lang),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                Text(
                    text = "$completenessScore%",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.ExtraBold,
                    color = meterColor
                )
            }

            // Progress bar
            LinearProgressIndicator(
                progress = { animatedProgress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(10.dp)
                    .clip(RoundedCornerShape(5.dp)),
                color = meterColor,
                trackColor = MaterialTheme.colorScheme.surface
            )

            Text(
                text = AppStrings.completenessLevel(lang, completenessScore),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = meterColor
            )

            // Completeness Checklist indicators
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                ChecklistRow(
                    title = if (lang == AppLanguage.ARABIC) "مواصفات المركبة الأساسية" else "Spécifications de base du véhicule",
                    isComplete = !vehicle.hasLowConfidence && vehicle.make.isNotBlank() && vehicle.year > 0
                )
                ChecklistRow(
                    title = if (lang == AppLanguage.ARABIC) "سجلات الصيانة الدورية (${events.size})" else "Historique d'entretien (${events.size} événements)",
                    isComplete = events.isNotEmpty()
                )
                ChecklistRow(
                    title = if (lang == AppLanguage.ARABIC) "توثيق قراءات العداد" else "Relevés kilométriques réguliers",
                    isComplete = events.any { it.odometerKm > 0 }
                )
                ChecklistRow(
                    title = if (lang == AppLanguage.ARABIC) "اعتماد وتوثيق من مراكز خدمة" else "Certification par des tiers agréés",
                    isComplete = events.any { it.verificationLevel == "verified" || it.verificationLevel == "dealer_certified" }
                )
            }

            // Verification Level Breakdown
            val verifiedCount = events.count { it.verificationLevel == "verified" || it.verificationLevel == "dealer_certified" }
            val selfCount = events.count { it.verificationLevel == "self_declared" }
            val total = events.size

            if (total > 0) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = AppStrings.verificationBreakdown(lang),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp))
                ) {
                    if (verifiedCount > 0) {
                        Box(
                            modifier = Modifier
                                .weight(verifiedCount.toFloat())
                                .height(8.dp)
                                .background(Color(0xFF10B981))
                        )
                    }
                    if (selfCount > 0) {
                        Box(
                            modifier = Modifier
                                .weight(selfCount.toFloat())
                                .height(8.dp)
                                .background(Color(0xFFF59E0B))
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF10B981))
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (lang == AppLanguage.ARABIC) "موثق ($verifiedCount)" else "Vérifié ($verifiedCount)",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFF59E0B))
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (lang == AppLanguage.ARABIC) "تصريح شخصي ($selfCount)" else "Déclaré ($selfCount)",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }

            // CRITICAL REQUIREMENT: Never imply "clean" when data is missing
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.35f)
                ),
                shape = RoundedCornerShape(10.dp)
            ) {
                Row(
                    modifier = Modifier.padding(10.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = AppStrings.neverImplyCleanDisclaimer(lang),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                }
            }
        }
    }
}

@Composable
private fun ChecklistRow(title: String, isComplete: Boolean) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(
            imageVector = if (isComplete) Icons.Default.CheckCircle else Icons.Default.Info,
            contentDescription = null,
            tint = if (isComplete) Color(0xFF10B981) else Color(0xFF94A3B8),
            modifier = Modifier.size(16.dp)
        )
        Text(
            text = title,
            style = MaterialTheme.typography.bodySmall,
            color = if (isComplete) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
