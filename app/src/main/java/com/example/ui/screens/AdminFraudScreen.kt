package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.ReportProblem
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AntiFraudReport
import com.example.data.model.AntiFraudSignal
import com.example.data.model.GarageAccount
import com.example.data.model.VehicleEvent
import com.example.data.repository.VehicleRepository
import com.example.ui.localization.AppLanguage
import com.example.ui.localization.AppStrings
import com.example.ui.localization.LocalLanguageManager
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AdminFraudScreen(
    repository: VehicleRepository,
    onNavigateBack: () -> Unit,
    onNavigateToVehicle: (String) -> Unit
) {
    val languageManager = LocalLanguageManager.current
    val lang = languageManager.currentLanguage
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    var selectedTab by remember { mutableIntStateOf(0) } // 0: Garages, 1: Anti-Fraud Signals, 2: Disputed Events

    val garages by repository.observeAllGarages().collectAsState(initial = emptyList())
    val disputedEvents by repository.observeDisputedEvents().collectAsState(initial = emptyList())

    var fraudReport by remember { mutableStateOf<AntiFraudReport?>(null) }
    var isLoadingReport by remember { mutableStateOf(false) }

    val refreshReport: () -> Unit = {
        coroutineScope.launch {
            isLoadingReport = true
            val res = repository.getAntiFraudSignals()
            isLoadingReport = false
            res.onSuccess { report ->
                fraudReport = report
            }.onFailure { err ->
                snackbarHostState.showSnackbar("Error fetching anti-fraud signals: ${err.message}")
            }
        }
    }

    LaunchedEffect(Unit) {
        refreshReport()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = AppStrings.antiFraudTitle(lang),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Admin: melloukabdou450@gmail.com",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { refreshReport() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh", tint = MaterialTheme.colorScheme.primary)
                    }
                    IconButton(onClick = { languageManager.toggleLanguage() }) {
                        Icon(Icons.Default.Language, contentDescription = "Language", tint = MaterialTheme.colorScheme.primary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = MaterialTheme.colorScheme.surface
            ) {
                val pendingCount = garages.count { it.status == "pending" }
                val signalsCount = fraudReport?.totalSignals ?: 0
                val disputedCount = disputedEvents.size

                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = {
                        Text(
                            text = if (lang == AppLanguage.ARABIC) "الورش ($pendingCount معلق)" else "Garages ($pendingCount)",
                            fontWeight = if (selectedTab == 0) FontWeight.Bold else FontWeight.Normal,
                            fontSize = 12.sp
                        )
                    }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = {
                        Text(
                            text = if (lang == AppLanguage.ARABIC) "إشارات الاحتيال ($signalsCount)" else "Signaux Fraude ($signalsCount)",
                            fontWeight = if (selectedTab == 1) FontWeight.Bold else FontWeight.Normal,
                            fontSize = 12.sp,
                            color = if (signalsCount > 0) Color(0xFFDC2626) else Color.Unspecified
                        )
                    }
                )
                Tab(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    text = {
                        Text(
                            text = if (lang == AppLanguage.ARABIC) "النزاعات ($disputedCount)" else "Litiges ($disputedCount)",
                            fontWeight = if (selectedTab == 2) FontWeight.Bold else FontWeight.Normal,
                            fontSize = 12.sp
                        )
                    }
                )
            }

            when (selectedTab) {
                0 -> GaragesManagementTab(
                    garages = garages,
                    lang = lang,
                    onApprove = { garageId ->
                        coroutineScope.launch {
                            val res = repository.updateGarageStatus(garageId, "approved")
                            res.onSuccess {
                                snackbarHostState.showSnackbar(if (lang == AppLanguage.ARABIC) "تم اعتماد الورشة بنجاح" else "Atelier approuvé")
                            }.onFailure { err ->
                                snackbarHostState.showSnackbar("Error: ${err.message}")
                            }
                        }
                    },
                    onReject = { garageId, reason ->
                        coroutineScope.launch {
                            val res = repository.updateGarageStatus(garageId, "rejected", reason)
                            res.onSuccess {
                                snackbarHostState.showSnackbar(if (lang == AppLanguage.ARABIC) "تم رفض الورشة" else "Atelier rejeté")
                            }.onFailure { err ->
                                snackbarHostState.showSnackbar("Error: ${err.message}")
                            }
                        }
                    }
                )
                1 -> AntiFraudSignalsTab(
                    report = fraudReport,
                    isLoading = isLoadingReport,
                    lang = lang,
                    onRefresh = { refreshReport() },
                    onNavigateToVehicle = onNavigateToVehicle
                )
                2 -> DisputedEventsTab(
                    events = disputedEvents,
                    lang = lang,
                    onNavigateToVehicle = onNavigateToVehicle
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun GaragesManagementTab(
    garages: List<GarageAccount>,
    lang: AppLanguage,
    onApprove: (String) -> Unit,
    onReject: (String, String) -> Unit
) {
    var filterStatus by remember { mutableStateOf("all") } // "all" | "pending" | "approved" | "rejected"
    val filtered = when (filterStatus) {
        "pending" -> garages.filter { it.status == "pending" }
        "approved" -> garages.filter { it.status == "approved" }
        "rejected" -> garages.filter { it.status == "rejected" }
        else -> garages
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = filterStatus == "all",
                    onClick = { filterStatus = "all" },
                    label = { Text("الكل (${garages.size})") }
                )
                FilterChip(
                    selected = filterStatus == "pending",
                    onClick = { filterStatus = "pending" },
                    label = { Text("قيد الانتظار (${garages.count { it.status == "pending" }})") }
                )
                FilterChip(
                    selected = filterStatus == "approved",
                    onClick = { filterStatus = "approved" },
                    label = { Text("المعتمدة (${garages.count { it.status == "approved" }})") }
                )
                FilterChip(
                    selected = filterStatus == "rejected",
                    onClick = { filterStatus = "rejected" },
                    label = { Text("المرفوضة (${garages.count { it.status == "rejected" }})") }
                )
            }
        }

        if (filtered.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(40.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (lang == AppLanguage.ARABIC) "لا توجد طلبات في هذا التصنيف" else "Aucun atelier dans cette catégorie",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        items(filtered, key = { it.uid }) { garage ->
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
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = garage.garageName,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        val badgeColor = when (garage.status) {
                            "approved" -> Color(0xFF10B981)
                            "rejected" -> Color(0xFFDC2626)
                            else -> Color(0xFFD97706)
                        }
                        Surface(
                            color = badgeColor.copy(alpha = 0.15f),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = when (garage.status) {
                                    "approved" -> "معتمد ✓"
                                    "rejected" -> "مرفوض ✗"
                                    else -> "قيد المراجعة ⏳"
                                },
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = badgeColor,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(Icons.Default.LocationOn, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                        Text(
                            text = "${garage.wilaya} • ${garage.commune} • ${garage.address}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(Icons.Default.Phone, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                        Text(
                            text = garage.phone,
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.SemiBold
                        )
                        if (garage.ownerEmail.isNotBlank()) {
                            Text(
                                text = "(${garage.ownerEmail})",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    if (garage.status == "pending") {
                        HorizontalDivider()
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Button(
                                onClick = { onApprove(garage.uid) },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981))
                            ) {
                                Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.size(4.dp))
                                Text("قبول واعتماد الورشة")
                            }

                            OutlinedButton(
                                onClick = { onReject(garage.uid, "عدم تطابق بيانات السجل التجاري") },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFDC2626))
                            ) {
                                Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.size(4.dp))
                                Text("رفض")
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AntiFraudSignalsTab(
    report: AntiFraudReport?,
    isLoading: Boolean,
    lang: AppLanguage,
    onRefresh: () -> Unit,
    onNavigateToVehicle: (String) -> Unit
) {
    if (isLoading) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    if (report == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("تعذر تحميل تقرير الاحتيال")
        }
        return
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Summary Cards
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                MetricCard(
                    title = "الورش المفحوصة",
                    value = report.totalGarages.toString(),
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.weight(1f)
                )
                MetricCard(
                    title = "إجمالي العمليات",
                    value = report.totalEvents.toString(),
                    color = Color(0xFF0F172A),
                    modifier = Modifier.weight(1f)
                )
                MetricCard(
                    title = "إشارات الخطر",
                    value = report.totalSignals.toString(),
                    color = if (report.totalSignals > 0) Color(0xFFDC2626) else Color(0xFF10B981),
                    modifier = Modifier.weight(1f)
                )
            }
        }

        if (report.signals.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFF0FDF4)),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF86EFAC))
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF16A34A), modifier = Modifier.size(32.dp))
                        Column {
                            Text(
                                text = if (lang == AppLanguage.ARABIC) "لا توجد مؤشرات احتيال نشطة ✓" else "Aucune anomalie de fraude détectée ✓",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF166534)
                            )
                            Text(
                                text = if (lang == AppLanguage.ARABIC)
                                    "جميع معدلات التسجيل اليومية، تكرار العدادات، والنسب ضمن النطاق الطبيعي الآمن."
                                else
                                    "Les rythmes de saisie et cohérences kilométriques respectent les seuils de conformité.",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFF14532D)
                            )
                        }
                    }
                }
            }
        }

        // Active signals list
        items(report.signals) { signal ->
            val isCritical = signal.severity == "CRITICAL"
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (isCritical) Color(0xFFFEF2F2) else Color(0xFFFFFBEB)
                ),
                border = androidx.compose.foundation.BorderStroke(
                    1.5.dp,
                    if (isCritical) Color(0xFFEF4444) else Color(0xFFF59E0B)
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
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
                                imageVector = if (isCritical) Icons.Default.Warning else Icons.Default.Error,
                                contentDescription = null,
                                tint = if (isCritical) Color(0xFFDC2626) else Color(0xFFD97706)
                            )
                            Text(
                                text = if (lang == AppLanguage.ARABIC) signal.titleArabic else signal.titleFrench,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = if (isCritical) Color(0xFF991B1B) else Color(0xFF92400E)
                            )
                        }
                        Surface(
                            color = (if (isCritical) Color(0xFFDC2626) else Color(0xFFD97706)).copy(alpha = 0.2f),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = signal.severity,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.ExtraBold,
                                color = if (isCritical) Color(0xFFDC2626) else Color(0xFFD97706),
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }

                    Text(
                        text = if (lang == AppLanguage.ARABIC) signal.descriptionArabic else signal.descriptionFrench,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF334155)
                    )

                    if (signal.vin.isNotBlank()) {
                        OutlinedButton(
                            onClick = { onNavigateToVehicle(signal.vin) },
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("فحص سجل السيارة (${signal.vin})", fontSize = 11.sp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun DisputedEventsTab(
    events: List<VehicleEvent>,
    lang: AppLanguage,
    onNavigateToVehicle: (String) -> Unit
) {
    if (events.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(40.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = if (lang == AppLanguage.ARABIC) "لا توجد نزاعات أو اعتراضات مفتوحة من الزبائن ✓" else "Aucun litige client en cours ✓",
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        return
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(events, key = { it.id }) { evt ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFDC2626).copy(alpha = 0.5f))
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = evt.serviceProvider.ifBlank { evt.garageName },
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = evt.date,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Text(
                        text = "VIN: ${evt.vin} • العداد: ${evt.odometerKm} كم",
                        style = MaterialTheme.typography.bodySmall,
                        fontFamily = FontFamily.Monospace
                    )

                    Surface(
                        color = Color(0xFFFEF2F2),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text(
                                text = "سبب اعتراض الزبون:",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF991B1B)
                            )
                            Text(
                                text = evt.disputedReason.ifBlank { "اعتراض بدون تفاصيل" },
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFF7F1D1D)
                            )
                        }
                    }

                    Button(
                        onClick = { onNavigateToVehicle(evt.vin) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0F172A))
                    ) {
                        Text("عرض جواز وتاريخ المركبة")
                    }
                }
            }
        }
    }
}

@Composable
private fun MetricCard(
    title: String,
    value: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Black,
                color = color
            )
            Text(
                text = title,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 10.sp
            )
        }
    }
}
