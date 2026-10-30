package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.crypto.PassportCrypto
import com.example.data.model.Vehicle
import com.example.data.model.VehicleEvent
import com.example.data.repository.VehicleRepository
import com.example.ui.localization.AppLanguage
import com.example.ui.localization.AppStrings
import com.example.ui.localization.LocalLanguageManager
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimelineScreen(
    vin: String,
    repository: VehicleRepository,
    onNavigateBack: () -> Unit,
    onNavigateToReport: (String) -> Unit,
    onNavigateToInspect: (String) -> Unit,
    onNavigateToInspectionResult: (vin: String, inspectionId: String) -> Unit,
    onNavigateToEstimate: (String) -> Unit
) {
    val languageManager = LocalLanguageManager.current
    val lang = languageManager.currentLanguage
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    val vehicle by repository.observeVehicle(vin).collectAsState(initial = null)
    val events by repository.observeEvents(vin).collectAsState(initial = emptyList())
    val latestInspection by repository.observeLatestInspection(vin).collectAsState(initial = null)
    val latestEstimate by repository.observeLatestEstimate(vin).collectAsState(initial = null)
    val currentUser = Firebase.auth.currentUser

    var showAddDialog by remember { mutableStateOf(false) }
    var showAuthDialog by remember { mutableStateOf(false) }
    var correctionTargetId by remember { mutableStateOf<String?>(null) }
    var isCryptoInfoExpanded by remember { mutableStateOf(false) }

    // Odometer anomaly detection
    val anomalies = remember(events) { PassportCrypto.detectOdometerAnomalies(events) }
    val chainVerification = remember(events) { PassportCrypto.verifyChain(events) }

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = if (vehicle != null) "${vehicle!!.year} ${vehicle!!.make} ${vehicle!!.model}" else vin,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = vin,
                            style = MaterialTheme.typography.labelSmall,
                            fontFamily = FontFamily.Monospace,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
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
                    // Language Switcher
                    IconButton(onClick = { languageManager.toggleLanguage() }) {
                        Icon(
                            imageVector = Icons.Default.Language,
                            contentDescription = "Language",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }

                    // Fair Price Estimate button (Stage 3)
                    IconButton(
                        onClick = { onNavigateToEstimate(vin) },
                        modifier = Modifier.testTag("open_estimate_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Savings,
                            contentDescription = "Fair Price Estimate",
                            tint = Color(0xFF10B981)
                        )
                    }

                    // Shareable Report button (Requirement 5)
                    IconButton(
                        onClick = { onNavigateToReport(vin) },
                        modifier = Modifier.testTag("open_report_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.QrCode,
                            contentDescription = "Report & QR",
                            tint = Color(0xFFD97706)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = {
                    if (currentUser == null) {
                        showAuthDialog = true
                    } else {
                        correctionTargetId = null
                        showAddDialog = true
                    }
                },
                containerColor = Color(0xFF0F172A),
                contentColor = Color.White,
                icon = { Icon(Icons.Default.Add, contentDescription = null, tint = Color(0xFFF59E0B)) },
                text = { Text(AppStrings.addEvent(lang), fontWeight = FontWeight.Bold) },
                modifier = Modifier.testTag("add_event_fab")
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Spacer(modifier = Modifier.height(2.dp))
            }

            // Cryptographic Audit Trail Status Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (chainVerification.isValid) Color(0xFF0F172A) else MaterialTheme.colorScheme.errorContainer
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .padding(14.dp)
                            .clickable { isCryptoInfoExpanded = !isCryptoInfoExpanded }
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
                                    imageVector = Icons.Default.Security,
                                    contentDescription = null,
                                    tint = if (chainVerification.isValid) Color(0xFF10B981) else MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(22.dp)
                                )
                                Column {
                                    Text(
                                        text = AppStrings.cryptoIntegrityVerified(lang),
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = if (chainVerification.isValid) Color.White else MaterialTheme.colorScheme.onErrorContainer
                                    )
                                    Text(
                                        text = "${events.size} ${if (lang == AppLanguage.ARABIC) "سجلات مشفرة ومتسلسلة" else "blocs d'événements chaînés"}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (chainVerification.isValid) Color(0xFF94A3B8) else MaterialTheme.colorScheme.onErrorContainer
                                    )
                                }
                            }

                            Icon(
                                imageVector = if (isCryptoInfoExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                contentDescription = null,
                                tint = Color.White
                            )
                        }

                        AnimatedVisibility(visible = isCryptoInfoExpanded) {
                            Column(
                                modifier = Modifier.padding(top = 10.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                HorizontalDivider(color = Color(0xFF334155))
                                Text(
                                    text = AppStrings.cryptoIntegrityNotice(lang),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color(0xFFCBD5E1)
                                )
                                Text(
                                    text = AppStrings.immutableRulesNotice(lang),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color(0xFFF59E0B)
                                )
                            }
                        }
                    }
                }
            }

            // Stage 2: AI Visual Inspection Action Card
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("timeline_ai_inspection_banner"),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (latestInspection != null) Color(0xFFF8FAFC) else Color(0xFFF0FDF4)
                    ),
                    border = androidx.compose.foundation.BorderStroke(
                        1.5.dp,
                        if (latestInspection != null) Color(0xFF6366F1).copy(alpha = 0.4f) else Color(0xFF10B981).copy(alpha = 0.5f)
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
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
                                        .background(Color(0xFF6366F1).copy(alpha = 0.15f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.DirectionsCar,
                                        contentDescription = null,
                                        tint = Color(0xFF6366F1),
                                        modifier = Modifier.size(20.dp)
                                    )
                                }

                                Column {
                                    Text(
                                        text = AppStrings.aiInspectionTitle(lang),
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = if (latestInspection != null)
                                            "آخر فحص: مؤشر الحالة ${latestInspection!!.overallConditionScore}/100"
                                        else
                                            "فحص بصري دقيق عبر 12 صورة إرشادية",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            // AI Estimate Disclaimer Chip
                            Surface(
                                color = Color(0xFFFEF3C7),
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Text(
                                    text = "AI Estimate",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF92400E),
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }

                        if (latestInspection != null) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Button(
                                    onClick = { onNavigateToInspectionResult(vin, latestInspection!!.id) },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(10.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0F172A))
                                ) {
                                    Text(
                                        text = AppStrings.viewInspectionReport(lang),
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold
                                    )
                                }

                                OutlinedButton(
                                    onClick = {
                                        if (currentUser == null) {
                                            showAuthDialog = true
                                        } else {
                                            onNavigateToInspect(vin)
                                        }
                                    },
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Text(
                                        text = "فحص جديد",
                                        style = MaterialTheme.typography.labelSmall
                                    )
                                }
                            }
                        } else {
                            Button(
                                onClick = {
                                    if (currentUser == null) {
                                        showAuthDialog = true
                                    } else {
                                        onNavigateToInspect(vin)
                                    }
                                },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0F172A))
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

            // Stage 3: Fair Price Estimate Card
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
                        modifier = Modifier.padding(14.dp),
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
                                        imageVector = Icons.Default.Savings,
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
                                        text = if (latestEstimate != null)
                                            "السعر العادل: ${String.format(java.util.Locale.US, "%,.0f د.ج", latestEstimate!!.fairPrice)}"
                                        else
                                            "مطابقة إحصائية مع إعلانات السوق الجزائري",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

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

                        Button(
                            onClick = { onNavigateToEstimate(vin) },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0F172A))
                        ) {
                            Text(
                                text = if (latestEstimate != null) "عرض وتحديث تقدير السعر العادل" else AppStrings.calculateEstimateBtn(lang),
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            // CRITICAL REQUIREMENT 4: Odometer Check Anomaly Flag
            // "Odometer check: flag any reading lower than an earlier one and explain in plain Arabic."
            if (anomalies.isNotEmpty()) {
                items(anomalies, key = { "anomaly_${it.eventId}" }) { anomaly ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = Color(0xFFFEF2F2) // Light Red/Amber
                        ),
                        border = androidx.compose.foundation.BorderStroke(1.5.dp, Color(0xFFEF4444))
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Warning,
                                    contentDescription = null,
                                    tint = Color(0xFFDC2626),
                                    modifier = Modifier.size(24.dp)
                                )
                                Text(
                                    text = if (lang == AppLanguage.ARABIC) "تحذير: تراجع غير طبيعي في عداد المسافات!" else "Alerte : Anomalie de compteur kilométrique !",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color(0xFF991B1B)
                                )
                            }

                            // Plain Arabic & French explanation
                            Text(
                                text = AppStrings.odometerRollbackAlert(
                                    lang = lang,
                                    currentKm = anomaly.currentKm,
                                    currentDate = anomaly.eventDate,
                                    prevKm = anomaly.previousHighestKm,
                                    prevDate = anomaly.previousDate,
                                    dropKm = anomaly.dropKm
                                ),
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color(0xFF7F1D1D),
                                fontWeight = FontWeight.Medium
                            )

                            Row(
                                horizontalArrangement = Arrangement.spacedBy(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "${anomaly.previousHighestKm} km ➔ ${anomaly.currentKm} km",
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFDC2626)
                                )
                                Text(
                                    text = "(-${anomaly.dropKm} km)",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = Color(0xFFDC2626)
                                )
                            }
                        }
                    }
                }
            }

            // Timeline Header
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = AppStrings.timelineTitle(lang),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${events.size} ${if (lang == AppLanguage.ARABIC) "أحداث" else "événements"}",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Empty state
            if (events.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                        )
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.EditNote,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(48.dp)
                            )
                            Text(
                                text = AppStrings.noEventsYet(lang),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            } else {
                items(events, key = { it.id }) { event ->
                    EventCard(
                        event = event,
                        lang = lang,
                        onAddCorrection = {
                            if (currentUser == null) {
                                showAuthDialog = true
                            } else {
                                correctionTargetId = event.id
                                showAddDialog = true
                            }
                        },
                        onCopyHash = { hash ->
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            clipboard.setPrimaryClip(ClipData.newPlainText("hash", hash))
                            Toast.makeText(context, if (lang == AppLanguage.ARABIC) "تم نسخ الهاش" else "Hash copié", Toast.LENGTH_SHORT).show()
                        }
                    )
                }
            }

            item {
                Spacer(modifier = Modifier.height(80.dp)) // Space for FAB
            }
        }
    }

    // Add Event Bottom Sheet
    if (showAddDialog) {
        ModalBottomSheet(
            onDismissRequest = { showAddDialog = false },
            sheetState = sheetState
        ) {
            AddEventSheet(
                vin = vin,
                lang = lang,
                correctionTargetId = correctionTargetId,
                latestOdometer = vehicle?.latestOdometerKm ?: 0L,
                onDismiss = { showAddDialog = false },
                onSubmit = { eventType, title, desc, date, km, verificationLevel, provider, isCorrection, correctedId ->
                    coroutineScope.launch {
                        showAddDialog = false
                        val result = repository.addEvent(
                            vin = vin,
                            eventType = eventType,
                            title = title,
                            description = desc,
                            date = date,
                            odometerKm = km,
                            verificationLevel = verificationLevel,
                            serviceProvider = provider,
                            isCorrection = isCorrection,
                            correctedEventId = correctedId
                        )
                        result.onSuccess {
                            snackbarHostState.showSnackbar(
                                if (lang == AppLanguage.ARABIC) "تم توثيق الحدث وإضافته للسلسلة المشفرة" else "Événement enregistré dans la chaîne cryptographique"
                            )
                        }.onFailure { err ->
                            snackbarHostState.showSnackbar("Error: ${err.message}")
                        }
                    }
                }
            )
        }
    }

    // Auth Dialog
    if (showAuthDialog) {
        androidx.compose.ui.window.Dialog(onDismissRequest = { showAuthDialog = false }) {
            AuthDialog(
                lang = lang,
                onDismiss = { showAuthDialog = false },
                onAuthSuccess = {
                    showAuthDialog = false
                    coroutineScope.launch {
                        snackbarHostState.showSnackbar(
                            if (lang == AppLanguage.ARABIC) "تم تسجيل الدخول بنجاح" else "Connecté avec succès"
                        )
                    }
                }
            )
        }
    }
}

@Composable
fun EventCard(
    event: VehicleEvent,
    lang: AppLanguage,
    onAddCorrection: () -> Unit,
    onCopyHash: (String) -> Unit
) {
    var isExpanded by remember { mutableStateOf(false) }

    val verificationColor = when (event.verificationLevel) {
        "verified" -> Color(0xFF10B981) // Green
        "dealer_certified" -> Color(0xFF2563EB) // Blue
        else -> Color(0xFFD97706) // Amber
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Header: Type Chip, Date, and Verification Badge
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
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFF0F172A))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = AppStrings.eventType(lang, event.eventType),
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    if (event.isCorrection) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(Color(0xFFFEE2E2))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = if (lang == AppLanguage.ARABIC) "تصحيح" else "Correction",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFFDC2626),
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.DateRange,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = event.date,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Title and Description
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = event.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                if (event.description.isNotBlank()) {
                    Text(
                        text = event.description,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Odometer and Provider
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Speed,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = "${event.odometerKm} km",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                if (event.garageName.isNotBlank() || event.serviceProvider.isNotBlank()) {
                    val providerText = if (event.sourceType == "GARAGE") {
                        "الورشة: ${event.garageName.ifBlank { event.serviceProvider }}"
                    } else {
                        event.serviceProvider
                    }
                    Text(
                        text = providerText,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            // Verification Level Badge & Customer Confirmation Status
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(verificationColor.copy(alpha = 0.12f))
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Verified,
                        contentDescription = null,
                        tint = verificationColor,
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        text = AppStrings.verificationLevel(lang, event.verificationLevel),
                        style = MaterialTheme.typography.labelSmall,
                        color = verificationColor,
                        fontWeight = FontWeight.Bold
                    )
                }

                if (event.sourceType == "GARAGE" || event.confirmationStatus.isNotBlank()) {
                    val (statusLabel, statusColor) = when (event.confirmationStatus) {
                        "CONFIRMED" -> Pair("تأكيد الزبون: مؤكد ✓", Color(0xFF10B981))
                        "DISPUTED" -> Pair("تأكيد الزبون: معترض عليه ⚠️", Color(0xFFDC2626))
                        else -> Pair("تأكيد الزبون: معلق ⏳", Color(0xFFD97706))
                    }
                    Surface(
                        color = statusColor.copy(alpha = 0.12f),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = statusLabel,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = statusColor,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }

            HorizontalDivider()

            // Cryptographic Hash & Actions
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = { isExpanded = !isExpanded }) {
                    Text(
                        text = if (isExpanded)
                            (if (lang == AppLanguage.ARABIC) "إخفاء تفاصيل التشفير" else "Masquer les hashs")
                        else
                            (if (lang == AppLanguage.ARABIC) "عرض تفاصيل التشفير SHA-256" else "Voir les hashs SHA-256"),
                        style = MaterialTheme.typography.labelSmall
                    )
                }

                // Add correction button per requirement: "Never delete events; corrections are new events."
                TextButton(onClick = onAddCorrection) {
                    Text(
                        text = AppStrings.addCorrection(lang),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            AnimatedVisibility(visible = isExpanded) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                        .padding(8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    HashRow(
                        label = if (lang == AppLanguage.ARABIC) "هاش الحدث (eventHash)" else "Hash de l'événement",
                        hash = event.eventHash,
                        onCopy = { onCopyHash(event.eventHash) }
                    )
                    HashRow(
                        label = if (lang == AppLanguage.ARABIC) "هاش السابق (prevHash)" else "Hash précédent",
                        hash = event.prevHash,
                        onCopy = { onCopyHash(event.prevHash) }
                    )
                }
            }
        }
    }
}

@Composable
private fun HashRow(label: String, hash: String, onCopy: () -> Unit) {
    Column {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = if (hash.length > 24) hash.take(12) + "..." + hash.takeLast(12) else hash,
                style = MaterialTheme.typography.bodySmall,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Medium
            )
            IconButton(onClick = onCopy, modifier = Modifier.size(28.dp)) {
                Icon(
                    imageVector = Icons.Default.ContentCopy,
                    contentDescription = "Copy Hash",
                    modifier = Modifier.size(14.dp)
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEventSheet(
    vin: String,
    lang: AppLanguage,
    correctionTargetId: String?,
    latestOdometer: Long,
    onDismiss: () -> Unit,
    onSubmit: (
        eventType: String,
        title: String,
        description: String,
        date: String,
        odometerKm: Long,
        verificationLevel: String,
        serviceProvider: String,
        isCorrection: Boolean,
        correctedEventId: String
    ) -> Unit
) {
    val isCorrection = correctionTargetId != null
    var eventType by remember { mutableStateOf(if (isCorrection) "correction" else "service") }
    var title by remember { mutableStateOf(if (isCorrection) "تصحيح سجل: " else "") }
    var description by remember { mutableStateOf("") }
    val today = remember { SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date()) }
    var date by remember { mutableStateOf(today) }
    var odometerStr by remember { mutableStateOf(if (latestOdometer > 0) latestOdometer.toString() else "") }
    var verificationLevel by remember { mutableStateOf("verified") }
    var serviceProvider by remember { mutableStateOf("") }

    val eventTypes = listOf("service", "repair", "odometer", "inspection", "correction")
    val verificationLevels = listOf("verified", "dealer_certified", "self_declared")

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text(
            text = if (isCorrection) AppStrings.addCorrection(lang) else AppStrings.addEvent(lang),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )

        // Event Type Selector
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                text = if (lang == AppLanguage.ARABIC) "نوع الحدث" else "Type d'événement",
                style = MaterialTheme.typography.labelMedium
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                eventTypes.take(3).forEach { type ->
                    FilterChip(
                        selected = eventType == type,
                        onClick = { eventType = type },
                        label = { Text(AppStrings.eventType(lang, type), style = MaterialTheme.typography.labelSmall) }
                    )
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                eventTypes.drop(3).forEach { type ->
                    FilterChip(
                        selected = eventType == type,
                        onClick = { eventType = type },
                        label = { Text(AppStrings.eventType(lang, type), style = MaterialTheme.typography.labelSmall) }
                    )
                }
            }
        }

        // Title
        OutlinedTextField(
            value = title,
            onValueChange = { title = it },
            label = { Text(if (lang == AppLanguage.ARABIC) "عنوان الحدث (مثال: صيانة الـ 50,000 كم)" else "Titre de l'événement") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            shape = RoundedCornerShape(12.dp)
        )

        // Date and Odometer Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedTextField(
                value = date,
                onValueChange = { date = it },
                label = { Text(if (lang == AppLanguage.ARABIC) "التاريخ (YYYY-MM-DD)" else "Date (YYYY-MM-DD)") },
                modifier = Modifier.weight(1f),
                singleLine = true,
                shape = RoundedCornerShape(12.dp)
            )

            OutlinedTextField(
                value = odometerStr,
                onValueChange = { odometerStr = it.filter { c -> c.isDigit() } },
                label = { Text(if (lang == AppLanguage.ARABIC) "العداد (كم)" else "Kilométrage (km)") },
                modifier = Modifier.weight(1f),
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                shape = RoundedCornerShape(12.dp)
            )
        }

        // Verification Level
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                text = if (lang == AppLanguage.ARABIC) "مستوى التوثيق" else "Niveau de vérification",
                style = MaterialTheme.typography.labelMedium
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                verificationLevels.forEach { level ->
                    FilterChip(
                        selected = verificationLevel == level,
                        onClick = { verificationLevel = level },
                        label = { Text(AppStrings.verificationLevel(lang, level), style = MaterialTheme.typography.labelSmall) }
                    )
                }
            }
        }

        // Service Provider / Workshop
        OutlinedTextField(
            value = serviceProvider,
            onValueChange = { serviceProvider = it },
            label = { Text(if (lang == AppLanguage.ARABIC) "اسم الورشة / مركز الصيانة / المالك" else "Prestataire / Atelier") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            shape = RoundedCornerShape(12.dp)
        )

        // Description
        OutlinedTextField(
            value = description,
            onValueChange = { description = it },
            label = { Text(if (lang == AppLanguage.ARABIC) "تفاصيل وملاحظات إضافية" else "Description détaillée") },
            modifier = Modifier.fillMaxWidth(),
            maxLines = 3,
            shape = RoundedCornerShape(12.dp)
        )

        // Submit Button
        Button(
            onClick = {
                val km = odometerStr.toLongOrNull() ?: 0L
                onSubmit(
                    eventType,
                    title.ifBlank { AppStrings.eventType(lang, eventType) },
                    description,
                    date,
                    km,
                    verificationLevel,
                    serviceProvider,
                    isCorrection,
                    correctionTargetId ?: ""
                )
            },
            enabled = date.isNotBlank(),
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0F172A))
        ) {
            Text(
                text = if (lang == AppLanguage.ARABIC) "تشفير وحفظ الحدث في الجواز" else "Signer et enregistrer",
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}
