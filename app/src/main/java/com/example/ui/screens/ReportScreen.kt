package com.example.ui.screens

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.crypto.PassportCrypto
import com.example.data.model.Vehicle
import com.example.data.repository.VehicleRepository
import com.example.ui.components.CompletenessMeterCard
import com.example.ui.components.QrCodeView
import com.example.ui.localization.AppLanguage
import com.example.ui.localization.AppStrings
import com.example.ui.localization.LocalLanguageManager

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportScreen(
    vin: String,
    repository: VehicleRepository,
    onNavigateBack: () -> Unit,
    onNavigateToInspection: ((String) -> Unit)? = null,
    onNavigateToInspectionResult: ((String, String) -> Unit)? = null,
    onNavigateToEstimate: ((String) -> Unit)? = null
) {
    val languageManager = LocalLanguageManager.current
    val lang = languageManager.currentLanguage
    val context = LocalContext.current

    val vehicle by repository.observeVehicle(vin).collectAsState(initial = null)
    val events by repository.observeEvents(vin).collectAsState(initial = emptyList())
    val latestInspection by repository.observeLatestInspection(vin).collectAsState(initial = null)
    val latestEstimate by repository.observeLatestEstimate(vin).collectAsState(initial = null)

    val completenessScore = remember(vehicle, events) {
        if (vehicle != null) PassportCrypto.calculateCompleteness(vehicle!!, events) else 20
    }
    val anomalies = remember(events) { PassportCrypto.detectOdometerAnomalies(events) }
    val chainVerification = remember(events) { PassportCrypto.verifyChain(events) }

    val reportUrl = "https://carpassport.app/v/$vin"

    fun shareReport() {
        val veh = vehicle
        val shareText = buildString {
            append("🚗 ${AppStrings.reportTitle(lang)}\n")
            if (veh != null) {
                append("${veh.year} ${veh.make} ${veh.model}\n")
            }
            append("VIN: $vin\n")
            append("${AppStrings.completenessTitle(lang)}: $completenessScore%\n")
            append("الأحداث الموثقة: ${events.size}\n")
            if (anomalies.isNotEmpty()) {
                append("⚠️ تنبيه: تم رصد انخفاض في قراءة العداد!\n")
            }
            append("\nفحص الشهادة الرقمية عبر الرابط أو رمز QR:\n")
            append(reportUrl)
        }

        val sendIntent = Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_TEXT, shareText)
            type = "text/plain"
        }
        val shareIntent = Intent.createChooser(sendIntent, AppStrings.shareReport(lang))
        context.startActivity(shareIntent)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = AppStrings.reportTitle(lang),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { languageManager.toggleLanguage() }) {
                        Icon(
                            imageVector = Icons.Default.Language,
                            contentDescription = "Language",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    IconButton(onClick = { shareReport() }) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = "Share",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Spacer(modifier = Modifier.height(2.dp))
            }

            // Official Passport Certificate Card
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("report_certificate_card"),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                    border = androidx.compose.foundation.BorderStroke(1.5.dp, Color(0xFFD97706).copy(alpha = 0.5f))
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Certificate Header
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF0F172A)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Shield,
                                    contentDescription = null,
                                    tint = Color(0xFFF59E0B),
                                    modifier = Modifier.size(28.dp)
                                )
                            }

                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = "CAR PASSPORT",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Black,
                                    color = Color(0xFF0F172A)
                                )
                                Text(
                                    text = "جواز السيارة الرقمي الموثق",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFD97706)
                                )
                            }
                        }

                        HorizontalDivider(color = Color(0xFFE2E8F0))

                        // Vehicle Info Header
                        val veh = vehicle
                        if (veh != null) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    text = "${veh.year} ${veh.make} ${veh.model}",
                                    style = MaterialTheme.typography.headlineSmall,
                                    fontWeight = FontWeight.ExtraBold,
                                    textAlign = TextAlign.Center
                                )
                                Text(
                                    text = "VIN: $vin",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = veh.engine,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }

                        // QR Code Section (Requirement 5: A shareable report page with QR code)
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            QrCodeView(
                                data = reportUrl,
                                size = 180.dp,
                                qrColor = Color(0xFF0F172A)
                            )
                            Text(
                                text = AppStrings.scanQrCode(lang),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        // Cryptographic Seal Stamp
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color(0xFF0F172A).copy(alpha = 0.05f))
                                .padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Security,
                                contentDescription = null,
                                tint = Color(0xFF10B981),
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.size(8.dp))
                            Text(
                                text = "SHA-256 Audit Trail • Immutable Record Chain",
                                style = MaterialTheme.typography.labelSmall,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF0F172A)
                            )
                        }
                    }
                }
            }

            // Odometer Progression Status Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (anomalies.isEmpty()) Color(0xFFF0FDF4) else Color(0xFFFEF2F2)
                    ),
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        if (anomalies.isEmpty()) Color(0xFF86EFAC) else Color(0xFFFCA5A5)
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = if (anomalies.isEmpty()) Icons.Default.CheckCircle else Icons.Default.Warning,
                                contentDescription = null,
                                tint = if (anomalies.isEmpty()) Color(0xFF16A34A) else Color(0xFFDC2626)
                            )
                            Text(
                                text = if (anomalies.isEmpty())
                                    (if (lang == AppLanguage.ARABIC) "حالة عداد المسافات: تسلسل طبيعي" else "Compteur : Progression normale")
                                else
                                    (if (lang == AppLanguage.ARABIC) "تنبيه: تم رصد تراجع غير طبيعي بالعداد" else "Alerte : Incohérence kilométrique détectée"),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = if (anomalies.isEmpty()) Color(0xFF166534) else Color(0xFF991B1B)
                            )
                        }

                        if (anomalies.isNotEmpty()) {
                            val firstAnomaly = anomalies.first()
                            Text(
                                text = AppStrings.odometerRollbackAlert(
                                    lang = lang,
                                    currentKm = firstAnomaly.currentKm,
                                    currentDate = firstAnomaly.eventDate,
                                    prevKm = firstAnomaly.previousHighestKm,
                                    prevDate = firstAnomaly.previousDate,
                                    dropKm = firstAnomaly.dropKm
                                ),
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFF7F1D1D)
                            )
                        } else {
                            Text(
                                text = if (lang == AppLanguage.ARABIC)
                                    "جميع القراءات المسجلة للعداد متناسقة وتتصاعد زمنياً دون انخفاضات مشبوهة."
                                else
                                    "Tous les relevés kilométriques enregistrés augmentent régulièrement sans anomalie.",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFF166534)
                            )
                        }
                    }
                }
            }

            // Data Completeness & Verified Breakdown Card (Requirement 5)
            if (vehicle != null) {
                item {
                    CompletenessMeterCard(
                        vehicle = vehicle!!,
                        events = events,
                        completenessScore = completenessScore,
                        lang = lang
                    )
                }
            }

            // STAGE 2 REQUIREMENT 5: AI Visual Inspection Summary Card
            // "Add the inspection summary to the shareable report page, clearly labeled 'AI estimate, not a certified inspection'."
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("report_ai_inspection_summary_card"),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (latestInspection != null) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF6366F1).copy(alpha = 0.4f))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Header with Badge
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
                                    imageVector = Icons.Default.DirectionsCar,
                                    contentDescription = null,
                                    tint = Color(0xFF6366F1),
                                    modifier = Modifier.size(22.dp)
                                )
                                Text(
                                    text = AppStrings.aiInspectionTitle(lang),
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            // Mandatory Label: "AI estimate, not a certified inspection"
                            Surface(
                                color = Color(0xFFFEF3C7),
                                shape = RoundedCornerShape(6.dp),
                                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFF59E0B))
                            ) {
                                Text(
                                    text = AppStrings.aiReportDisclaimer(lang),
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color(0xFF92400E),
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                )
                            }
                        }

                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                        if (latestInspection != null) {
                            val insp = latestInspection!!
                            val scoreColor = when {
                                insp.overallConditionScore >= 80 -> Color(0xFF16A34A)
                                insp.overallConditionScore >= 60 -> Color(0xFFD97706)
                                else -> Color(0xFFDC2626)
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = AppStrings.overallConditionScore(lang),
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = "${insp.overallConditionScore} / 100",
                                        style = MaterialTheme.typography.headlineMedium,
                                        fontWeight = FontWeight.Black,
                                        color = scoreColor
                                    )
                                }

                                Column(horizontalAlignment = Alignment.End) {
                                    Text(
                                        text = "الأجزاء المفحوصة: ${insp.panels.size}",
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    if (insp.odometerKmFromDashboard != null) {
                                        Text(
                                            text = "العداد المرصود: ${insp.odometerKmFromDashboard} كم",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.primary,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }

                            if (insp.limitations.isNotEmpty()) {
                                Text(
                                    text = "حدود التقييم: " + insp.limitations.take(2).joinToString(" • "),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            Text(
                                text = AppStrings.aiReportHedgingNotice(lang),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            if (onNavigateToInspectionResult != null) {
                                androidx.compose.material3.OutlinedButton(
                                    onClick = { onNavigateToInspectionResult(vin, insp.id) },
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Text(
                                        text = AppStrings.viewInspectionReport(lang),
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        } else {
                            Text(
                                text = "لم يتم إجراء فحص بصري بالذكاء الاصطناعي لهذه المركبة بعد. يتطلب التقاط 12 صورة إرشادية لهيكل ومقصورة السيارة.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            if (onNavigateToInspection != null) {
                                androidx.compose.material3.OutlinedButton(
                                    onClick = { onNavigateToInspection(vin) },
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Text(
                                        text = AppStrings.startAiInspection(lang),
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Stage 3: Fair Price Estimate Card in Shareable Report
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    border = androidx.compose.foundation.BorderStroke(
                        1.5.dp,
                        Color(0xFF10B981).copy(alpha = 0.5f)
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
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
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFF10B981).copy(alpha = 0.15f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.DirectionsCar,
                                        contentDescription = null,
                                        tint = Color(0xFF10B981),
                                        modifier = Modifier.size(20.dp)
                                    )
                                }

                                Column {
                                    Text(
                                        text = AppStrings.fairPriceTitle(lang),
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "مقارنة دقيقة مع إعلانات السوق الجزائري",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            // Mandatory label: "تقدير إحصائي وليس تقييماً رسمياً"
                            Surface(
                                color = Color(0xFFFEF3C7),
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Text(
                                    text = AppStrings.statisticalDisclaimer(lang),
                                    style = MaterialTheme.typography.labelSmall,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF92400E),
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }

                        val est = latestEstimate
                        if (est != null) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = AppStrings.medianFairPriceLabel(lang),
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = String.format(java.util.Locale.US, "%,.0f د.ج", est.fairPrice),
                                        style = MaterialTheme.typography.headlineSmall,
                                        fontWeight = FontWeight.Black,
                                        color = Color(0xFF10B981)
                                    )
                                }

                                Column(horizontalAlignment = Alignment.End) {
                                    Text(
                                        text = "النطاق: ${String.format(java.util.Locale.US, "%,.0f", est.lowPrice)} - ${String.format(java.util.Locale.US, "%,.0f د.ج", est.highPrice)}",
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    if (est.verdictArabic.isNotBlank() && est.confidence != "low") {
                                        Text(
                                            text = "الحكم: ${est.verdictArabic} (${String.format(java.util.Locale.US, "%+.1f%%", est.diffPercentage)})",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = if (est.verdictArabic == "عادل" || est.verdictArabic == "أقل من العادل") Color(0xFF10B981) else Color(0xFFDC2626)
                                        )
                                    } else if (est.confidence == "low") {
                                        Text(
                                            text = AppStrings.lowConfidenceMarketWarning(lang),
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFFD97706)
                                        )
                                    }
                                }
                            }

                            if (est.explanation.isNotBlank()) {
                                Surface(
                                    color = Color(0xFFF8FAFC),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Text(
                                        text = est.explanation,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color(0xFF334155),
                                        modifier = Modifier.padding(10.dp)
                                    )
                                }
                            }

                            if (onNavigateToEstimate != null) {
                                androidx.compose.material3.OutlinedButton(
                                    onClick = { onNavigateToEstimate(vin) },
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Text(
                                        text = "تحديث أو فحص تفاصيل التقدير",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        } else {
                            Text(
                                text = "لم يتم إجراء تقدير للسعر العادل لهذه المركبة بعد. اضغط أدناه لإدخال السعر ومطابقته مع إعلانات السوق الجزائري.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            if (onNavigateToEstimate != null) {
                                androidx.compose.material3.OutlinedButton(
                                    onClick = { onNavigateToEstimate(vin) },
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Text(
                                        text = "حساب السعر العادل ومطابقة السوق",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // STAGE 4 REQUIREMENT 6: Vehicle Report Partner Services
            // "Vehicle report: display garage name (no customer info), verification badge, customer confirmation status."
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("report_garage_services_card"),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF0F172A).copy(alpha = 0.2f))
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = if (lang == AppLanguage.ARABIC) "سجل العمليات والصيانة الموثقة" else "Historique des Entretiens Agréés",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "${events.size} ${if (lang == AppLanguage.ARABIC) "سجلات" else "enregistrements"}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        if (events.isEmpty()) {
                            Text(
                                text = if (lang == AppLanguage.ARABIC) "لا توجد عمليات صيانة مسجلة بعد لهذه السيارة." else "Aucune intervention enregistrée pour ce véhicule.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        } else {
                            events.forEach { evt ->
                                Surface(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(10.dp),
                                    color = Color(0xFFF8FAFC)
                                ) {
                                    Column(
                                        modifier = Modifier.padding(12.dp),
                                        verticalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = evt.title,
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = FontWeight.Bold
                                            )
                                            Text(
                                                text = evt.date,
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }

                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            // Garage Name (Requirement 6: Display garage name, NO customer info)
                                            val garageDisplayName = if (evt.sourceType == "GARAGE") {
                                                "الورشة: ${evt.garageName.ifBlank { evt.serviceProvider }}"
                                            } else {
                                                evt.serviceProvider.ifBlank { "مالك المركبة" }
                                            }
                                            Text(
                                                text = garageDisplayName,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.primary,
                                                fontWeight = FontWeight.SemiBold
                                            )

                                            Text(
                                                text = "${evt.odometerKm} كم",
                                                style = MaterialTheme.typography.bodySmall,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }

                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            // Verification badge
                                            val badgeColor = when {
                                                evt.verificationLevel.contains("VERIFIED_BY_PARTNER") || evt.verificationLevel == "verified" -> Color(0xFF10B981)
                                                evt.verificationLevel == "dealer_certified" -> Color(0xFF2563EB)
                                                else -> Color(0xFFD97706)
                                            }
                                            Surface(
                                                color = badgeColor.copy(alpha = 0.12f),
                                                shape = RoundedCornerShape(6.dp)
                                            ) {
                                                Text(
                                                    text = AppStrings.verificationLevel(lang, evt.verificationLevel),
                                                    style = MaterialTheme.typography.labelSmall,
                                                    fontWeight = FontWeight.Bold,
                                                    color = badgeColor,
                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                )
                                            }

                                            // Customer confirmation status
                                            if (evt.sourceType == "GARAGE" || evt.confirmationStatus.isNotBlank()) {
                                                val (statusText, statusColor) = when (evt.confirmationStatus) {
                                                    "CONFIRMED" -> Pair("مؤكد من الزبون ✓", Color(0xFF10B981))
                                                    "DISPUTED" -> Pair("معترض عليه ⚠️", Color(0xFFDC2626))
                                                    else -> Pair("في انتظار التأكيد ⏳", Color(0xFFD97706))
                                                }
                                                Text(
                                                    text = statusText,
                                                    style = MaterialTheme.typography.labelSmall,
                                                    fontWeight = FontWeight.Bold,
                                                    color = statusColor
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Share Button
            item {
                Button(
                    onClick = { shareReport() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .testTag("share_report_button"),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF0F172A)
                    )
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = null,
                            tint = Color(0xFFF59E0B)
                        )
                        Text(
                            text = AppStrings.shareReport(lang),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}
