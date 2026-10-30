package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.TrendingDown
import androidx.compose.material.icons.automirrored.filled.TrendingFlat
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Vehicle
import com.example.data.model.VehicleEstimate
import com.example.data.pricing.EstimationInput
import com.example.data.pricing.EstimationResult
import com.example.data.pricing.PriceEstimationEngine
import com.example.data.pricing.PricingGeminiService
import com.example.data.repository.VehicleRepository
import com.example.ui.localization.AppLanguage
import com.example.ui.localization.AppStrings
import com.example.ui.localization.LocalLanguageManager
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import kotlinx.coroutines.launch
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EstimateScreen(
    vin: String,
    repository: VehicleRepository,
    onNavigateBack: () -> Unit,
    onNavigateToReport: (String) -> Unit
) {
    val languageManager = LocalLanguageManager.current
    val lang = languageManager.currentLanguage
    val focusManager = LocalFocusManager.current
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    val vehicle by repository.observeVehicle(vin).collectAsState(initial = null)
    val latestInspection by repository.observeLatestInspection(vin).collectAsState(initial = null)
    val latestEstimate by repository.observeLatestEstimate(vin).collectAsState(initial = null)
    val allListings by repository.observePriceListings().collectAsState(initial = emptyList())
    val currentUser = Firebase.auth.currentUser

    var askingPriceInput by remember { mutableStateOf("2300000") }
    var isCalculating by remember { mutableStateOf(false) }
    var estimationResult by remember { mutableStateOf<EstimationResult?>(null) }
    var generatedExplanation by remember { mutableStateOf<String?>(null) }
    var showAuthDialog by remember { mutableStateOf(false) }

    // Initialize with existing estimate inputs if available
    LaunchedEffect(latestEstimate) {
        if (latestEstimate != null && estimationResult == null) {
            val ask = latestEstimate!!.askingPrice
            if (ask > 0) {
                askingPriceInput = String.format(Locale.US, "%.0f", ask)
            }
            if (latestEstimate!!.explanation.isNotBlank()) {
                generatedExplanation = latestEstimate!!.explanation
            }
        }
    }

    fun runEstimation() {
        val veh = vehicle
        val asking = askingPriceInput.toDoubleOrNull()
        if (veh == null || asking == null || asking <= 0) {
            coroutineScope.launch {
                snackbarHostState.showSnackbar(if (lang == AppLanguage.ARABIC) "يرجى إدخال سعر بيع صحيح بالدينار" else "Veuillez saisir un prix valide")
            }
            return
        }

        focusManager.clearFocus()
        isCalculating = true

        coroutineScope.launch {
            // Target km from latest odometer or vehicle
            val targetKm = if (latestInspection?.odometerKmFromDashboard != null) {
                latestInspection!!.odometerKmFromDashboard!!
            } else if (veh.latestOdometerKm > 0) {
                veh.latestOdometerKm
            } else {
                80000L
            }

            val input = EstimationInput(
                make = veh.make,
                model = veh.model,
                year = veh.year.toInt(),
                km = targetKm,
                askingPrice = asking,
                wilaya = "16 - Alger",
                conditionScore = latestInspection?.overallConditionScore
            )

            // Step 1: Pure deterministic statistical evaluation
            val result = PriceEstimationEngine.estimate(input, allListings)
            estimationResult = result

            // Step 2: Use Gemini ONLY to generate 3-sentence explanation from the computed numbers
            val explanation = PricingGeminiService.generateArabicExplanation(
                make = veh.make,
                model = veh.model,
                year = veh.year.toInt(),
                km = targetKm,
                askingPrice = asking,
                result = result
            )
            generatedExplanation = explanation

            // Step 3: Automatically save snapshot under vehicles/{vin}/estimates/{id} if signed in
            if (currentUser != null) {
                val estimateDoc = VehicleEstimate(
                    vin = vin,
                    inputs = mapOf(
                        "askingPrice" to asking,
                        "targetKm" to targetKm,
                        "targetYear" to veh.year,
                        "targetMake" to veh.make,
                        "targetModel" to veh.model,
                        "conditionScore" to (latestInspection?.overallConditionScore ?: -1L)
                    ),
                    outputs = mapOf(
                        "lowPrice" to result.lowPrice,
                        "fairPrice" to result.fairPrice,
                        "highPrice" to result.highPrice,
                        "conditionAdjustmentDzd" to result.conditionAdjustmentDzd,
                        "conditionAdjustmentPercent" to result.conditionAdjustmentPercent,
                        "adjustedFairPrice" to result.adjustedFairPrice,
                        "diffPercentage" to result.diffPercentage,
                        "verdictArabic" to (result.verdictArabic ?: ""),
                        "verdictFrench" to (result.verdictFrench ?: "")
                    ),
                    factors = result.factors.map {
                        mapOf(
                            "key" to it.key,
                            "nameArabic" to it.nameArabic,
                            "nameFrench" to it.nameFrench,
                            "amountDzd" to it.amountDzd,
                            "descriptionArabic" to it.descriptionArabic,
                            "descriptionFrench" to it.descriptionFrench
                        )
                    },
                    comparablesCount = result.comparablesCount,
                    comparisonLevel = result.comparisonLevel,
                    comparisonLevelLabelArabic = result.comparisonLevelLabelArabic,
                    comparisonLevelLabelFrench = result.comparisonLevelLabelFrench,
                    confidence = result.confidence,
                    freshnessDate = result.freshnessDate,
                    explanation = explanation
                )
                repository.saveEstimate(vin, estimateDoc)
            }

            isCalculating = false
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = AppStrings.fairPriceTitle(lang),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = if (vehicle != null) "${vehicle!!.year} ${vehicle!!.make} ${vehicle!!.model}" else vin,
                            style = MaterialTheme.typography.labelSmall,
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
                    IconButton(onClick = { languageManager.toggleLanguage() }) {
                        Icon(
                            imageVector = Icons.Default.Language,
                            contentDescription = "Language",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }

                    IconButton(onClick = { onNavigateToReport(vin) }) {
                        Icon(
                            imageVector = Icons.Default.QrCode,
                            contentDescription = "Report",
                            tint = Color(0xFFD97706)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
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

            // Vehicle Specs Summary Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFF0F172A)
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
                                imageVector = Icons.Default.DirectionsCar,
                                contentDescription = null,
                                tint = Color(0xFFF59E0B),
                                modifier = Modifier.size(24.dp)
                            )
                            Text(
                                text = if (vehicle != null) "${vehicle!!.year} ${vehicle!!.make} ${vehicle!!.model}" else "مركبة",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            val kmDisplay = if (latestInspection?.odometerKmFromDashboard != null) {
                                "${latestInspection!!.odometerKmFromDashboard} كم (مرصود بالفحص)"
                            } else if (vehicle?.latestOdometerKm ?: 0 > 0) {
                                "${vehicle!!.latestOdometerKm} كم (السجل)"
                            } else {
                                "غير محدد"
                            }
                            Text(
                                text = "العداد: $kmDisplay",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFFCBD5E1)
                            )

                            if (latestInspection != null) {
                                Text(
                                    text = "مؤشر الفحص: ${latestInspection!!.overallConditionScore}/100",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF10B981)
                                )
                            }
                        }
                    }
                }
            }

            // Asking Price Input Box
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = AppStrings.askingPriceLabel(lang),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )

                        OutlinedTextField(
                            value = askingPriceInput,
                            onValueChange = { askingPriceInput = it.filter { c -> c.isDigit() } },
                            placeholder = { Text(AppStrings.askingPricePlaceholder(lang)) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            trailingIcon = {
                                Text(
                                    text = "د.ج",
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(end = 12.dp)
                                )
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("asking_price_input"),
                            shape = RoundedCornerShape(12.dp),
                            singleLine = true
                        )

                        Button(
                            onClick = { runEstimation() },
                            enabled = !isCalculating && askingPriceInput.isNotBlank(),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                                .testTag("calculate_estimate_button"),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0F172A))
                        ) {
                            if (isCalculating) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(20.dp))
                                    Text("جاري المطابقة وحساب السعر العادل...", color = Color.White)
                                }
                            } else {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(Icons.Default.Savings, contentDescription = null, tint = Color(0xFFF59E0B))
                                    Text(
                                        text = AppStrings.calculateEstimateBtn(lang),
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Results Section
            if (estimationResult != null) {
                val res = estimationResult!!

                // Confidence Banner & Low Confidence Handling
                item {
                    val (badgeBg, badgeText, badgeIcon) = when (res.confidence) {
                        "high" -> Triple(Color(0xFFDCFCE7), Color(0xFF166534), Icons.Default.CheckCircle)
                        "medium" -> Triple(Color(0xFFFEF3C7), Color(0xFF92400E), Icons.Default.Info)
                        else -> Triple(Color(0xFFFEE2E2), Color(0xFF991B1B), Icons.Default.Warning)
                    }

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = badgeBg)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                imageVector = badgeIcon,
                                contentDescription = null,
                                tint = badgeText,
                                modifier = Modifier.size(22.dp)
                            )
                            Column {
                                Text(
                                    text = if (res.confidence == "low")
                                        AppStrings.lowConfidenceMarketWarning(lang)
                                    else
                                        "مستوى الثقة الإحصائية: " + when (res.confidence) {
                                            "high" -> "عالية (>=25 عينة)"
                                            "medium" -> "متوسطة (10-24 عينة)"
                                            else -> "منخفضة (<10 عينات)"
                                        },
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = badgeText
                                )
                                Text(
                                    text = "${res.comparablesCount} عينة مقارنة • المستوى: ${res.comparisonLevelLabelArabic} • الحداثة: ${res.freshnessDate}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = badgeText.copy(alpha = 0.85f)
                                )
                            }
                        }
                    }
                }

                // Fair Price Range Card (Low / Fair / High in DZD)
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        ),
                        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
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
                                Text(
                                    text = AppStrings.fairPriceRangeTitle(lang),
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold
                                )

                                Surface(
                                    color = Color(0xFFE2E8F0),
                                    shape = RoundedCornerShape(6.dp)
                                ) {
                                    Text(
                                        text = AppStrings.statisticalDisclaimer(lang),
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF475569),
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }

                            // 3 Boxes: Low, Fair, High
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                // Low
                                Surface(
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(12.dp),
                                    color = Color(0xFFF1F5F9)
                                ) {
                                    Column(
                                        modifier = Modifier.padding(10.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Text(
                                            text = AppStrings.lowPriceLabel(lang),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Text(
                                            text = formatDzdShort(res.lowPrice),
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }

                                // Median Fair (Prominent)
                                Surface(
                                    modifier = Modifier.weight(1.3f),
                                    shape = RoundedCornerShape(12.dp),
                                    color = Color(0xFF0F172A)
                                ) {
                                    Column(
                                        modifier = Modifier.padding(10.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Text(
                                            text = AppStrings.medianFairPriceLabel(lang),
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFFF59E0B)
                                        )
                                        Text(
                                            text = formatDzdShort(res.adjustedFairPrice),
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Black,
                                            color = Color.White
                                        )
                                    }
                                }

                                // High
                                Surface(
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(12.dp),
                                    color = Color(0xFFF1F5F9)
                                ) {
                                    Column(
                                        modifier = Modifier.padding(10.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Text(
                                            text = AppStrings.highPriceLabel(lang),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Text(
                                            text = formatDzdShort(res.highPrice),
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }

                            // Condition adjustment separate line (Requirement 2)
                            if (res.conditionAdjustmentDzd != 0.0) {
                                HorizontalDivider(color = Color(0xFFE2E8F0))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = AppStrings.conditionAdjustmentLine(lang),
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    val sign = if (res.conditionAdjustmentDzd > 0) "+" else ""
                                    Text(
                                        text = "$sign${String.format(Locale.US, "%,.0f د.ج", res.conditionAdjustmentDzd)} (${String.format(Locale.US, "%+.1f%%", res.conditionAdjustmentPercent)})",
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.Bold,
                                        color = if (res.conditionAdjustmentDzd > 0) Color(0xFF10B981) else Color(0xFFDC2626)
                                    )
                                }
                            }
                        }
                    }
                }

                // Verdict Card (Only if NOT low confidence)
                if (res.confidence != "low" && res.verdictArabic != null) {
                    item {
                        val (vBg, vColor, vIcon) = when (res.verdictArabic) {
                            "أقل من العادل" -> Triple(Color(0xFFDCFCE7), Color(0xFF166534), Icons.AutoMirrored.Filled.TrendingDown)
                            "أعلى من العادل" -> Triple(Color(0xFFFEE2E2), Color(0xFF991B1B), Icons.AutoMirrored.Filled.TrendingUp)
                            else -> Triple(Color(0xFFE0E7FF), Color(0xFF3730A3), Icons.AutoMirrored.Filled.TrendingFlat)
                        }

                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = vBg)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = "التقييم بالنسبة للسعر المطلوب:",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = vColor.copy(alpha = 0.8f)
                                    )
                                    Text(
                                        text = res.verdictArabic!!,
                                        style = MaterialTheme.typography.headlineSmall,
                                        fontWeight = FontWeight.Black,
                                        color = vColor
                                    )
                                    Text(
                                        text = "فارق السعر: ${String.format(Locale.US, "%+.1f%%", res.diffPercentage)} عن القيمة العادلة",
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.Bold,
                                        color = vColor
                                    )
                                }

                                Box(
                                    modifier = Modifier
                                        .size(48.dp)
                                        .clip(CircleShape)
                                        .background(vColor.copy(alpha = 0.15f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = vIcon,
                                        contentDescription = null,
                                        tint = vColor,
                                        modifier = Modifier.size(28.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                // Top Factors Affecting Estimate
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text(
                                text = AppStrings.topFactorsTitle(lang),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )

                            res.factors.forEach { factor ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = if (lang == AppLanguage.ARABIC) factor.nameArabic else factor.nameFrench,
                                            style = MaterialTheme.typography.bodySmall,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                        Text(
                                            text = if (lang == AppLanguage.ARABIC) factor.descriptionArabic else factor.descriptionFrench,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }

                                    val sign = if (factor.amountDzd > 0) "+" else ""
                                    Text(
                                        text = "$sign${String.format(Locale.US, "%,.0f د.ج", factor.amountDzd)}",
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.Bold,
                                        color = if (factor.amountDzd > 0) Color(0xFF10B981) else if (factor.amountDzd < 0) Color(0xFFDC2626) else MaterialTheme.colorScheme.onSurface
                                    )
                                }
                                HorizontalDivider(color = Color(0xFFF1F5F9))
                            }
                        }
                    }
                }

                // Gemini 3-sentence Arabic Explanation Box (Requirement 4)
                if (generatedExplanation != null) {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = Color(0xFFF8FAFC)
                            ),
                            border = androidx.compose.foundation.BorderStroke(1.5.dp, Color(0xFF6366F1).copy(alpha = 0.3f))
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
                                        imageVector = Icons.Default.AutoAwesome,
                                        contentDescription = null,
                                        tint = Color(0xFF6366F1),
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Text(
                                        text = AppStrings.aiExplanationTitle(lang),
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF1E293B)
                                    )
                                }

                                Text(
                                    text = generatedExplanation!!,
                                    style = MaterialTheme.typography.bodyMedium,
                                    lineHeight = 22.sp,
                                    color = Color(0xFF334155)
                                )
                            }
                        }
                    }
                }

                // Action to view on report
                item {
                    OutlinedButton(
                        onClick = { onNavigateToReport(vin) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Default.QrCode, contentDescription = null, tint = Color(0xFFD97706))
                            Text("عرض التقدير في تقرير المركبة الموثق")
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

private fun formatDzdShort(amount: Double): String {
    return String.format(Locale.US, "%,.0f د.ج", amount)
}
