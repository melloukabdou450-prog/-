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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Handyman
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.ReportProblem
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
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
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.VehicleEvent
import com.example.data.repository.VehicleRepository
import com.example.ui.localization.AppLanguage
import com.example.ui.localization.AppStrings
import com.example.ui.localization.LocalLanguageManager
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun CustomerConfirmationScreen(
    initialVin: String = "",
    initialEventId: String = "",
    initialCode: String = "",
    repository: VehicleRepository,
    onNavigateBack: () -> Unit,
    onNavigateToReport: (String) -> Unit
) {
    val languageManager = LocalLanguageManager.current
    val lang = languageManager.currentLanguage
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    var vinInput by remember { mutableStateOf(initialVin.trim().uppercase()) }
    var eventIdInput by remember { mutableStateOf(initialEventId.trim()) }
    var codeInput by remember { mutableStateOf(initialCode.trim()) }

    var eventDetails by remember { mutableStateOf<VehicleEvent?>(null) }
    var isLoadingEvent by remember { mutableStateOf(false) }
    var isSubmitting by remember { mutableStateOf(false) }
    var confirmationSuccess by remember { mutableStateOf(false) }
    var disputeSuccess by remember { mutableStateOf(false) }

    var showDisputeDialog by remember { mutableStateOf(false) }
    var disputeReason by remember { mutableStateOf("") }

    // Fetch event if VIN and eventId are known
    val fetchEvent: (String, String) -> Unit = { vin, evtId ->
        if (vin.length == 17 && evtId.isNotBlank()) {
            coroutineScope.launch {
                isLoadingEvent = true
                try {
                    val eventsResult = repository.getVehicle(vin)
                    if (eventsResult.isSuccess) {
                        // Observe or query event from subcollection
                        val eventsSnap = repository.getEvents(vin)
                        eventsSnap.onSuccess { list ->
                            val found = list.find { it.id == evtId }
                            eventDetails = found
                        }
                    }
                } catch (_: Exception) {} finally {
                    isLoadingEvent = false
                }
            }
        }
    }

    LaunchedEffect(initialVin, initialEventId) {
        if (initialVin.isNotBlank() && initialEventId.isNotBlank()) {
            fetchEvent(initialVin, initialEventId)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = AppStrings.customerConfirmTitle(lang),
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
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Spacer(modifier = Modifier.height(4.dp))
            }

            // Anti-fake assurance banner
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFF0F172A)
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF1E293B)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Shield,
                                contentDescription = null,
                                tint = Color(0xFF10B981),
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (lang == AppLanguage.ARABIC) "نظام الحماية من السجلات المزيفة" else "Système Anti-Fausses Déclarations",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Text(
                                text = if (lang == AppLanguage.ARABIC)
                                    "تأكيدك كمالك للسيارة يحول السجل إلى 'موثق رسمياً' في جواز سيارتك ويمنع التلاعب."
                                else
                                    "Votre confirmation transforme l'intervention en statut 'Officiellement Vérifié'.",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFF94A3B8)
                            )
                        }
                    }
                }
            }

            // Input Fields Card (if not yet found or manually entered)
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = if (lang == AppLanguage.ARABIC) "بيانات عملية الصيانة والرمز" else "Détails et Code de Confirmation",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )

                        OutlinedTextField(
                            value = vinInput,
                            onValueChange = {
                                vinInput = it.uppercase().replace(" ", "")
                                if (vinInput.length == 17 && eventIdInput.isNotBlank()) {
                                    fetchEvent(vinInput, eventIdInput)
                                }
                            },
                            label = { Text("رقم الهيكل VIN (17 حرفاً)") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("confirm_vin_input"),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters)
                        )

                        OutlinedTextField(
                            value = eventIdInput,
                            onValueChange = {
                                eventIdInput = it.trim()
                                if (vinInput.length == 17 && eventIdInput.isNotBlank()) {
                                    fetchEvent(vinInput, eventIdInput)
                                }
                            },
                            label = { Text("معرّف الحدث (Event ID e.g. evt_gar_...)") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("confirm_event_id_input"),
                            singleLine = true
                        )

                        OutlinedTextField(
                            value = codeInput,
                            onValueChange = { codeInput = it.trim().take(6) },
                            label = { Text(if (lang == AppLanguage.ARABIC) "رمز التأكيد (6 أرقام من رسالة واتساب)" else "Code de confirmation (6 chiffres)") },
                            leadingIcon = { Icon(Icons.Default.Key, contentDescription = null, tint = Color(0xFFD97706)) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("confirm_code_input"),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                        )

                        if (eventDetails == null && (vinInput.length == 17 && eventIdInput.isNotBlank())) {
                            Button(
                                onClick = { fetchEvent(vinInput, eventIdInput) },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Text(if (lang == AppLanguage.ARABIC) "تحميل تفاصيل الخدمة" else "Charger l'intervention")
                            }
                        }
                    }
                }
            }

            if (isLoadingEvent) {
                item {
                    Box(modifier = Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
            }

            // Event Details Preview Card
            val evt = eventDetails
            if (evt != null) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        ),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF10B981).copy(alpha = 0.5f))
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
                                    text = evt.serviceProvider.ifBlank { evt.garageName }.ifBlank { "ورشة معتمدة" },
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = evt.date,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            HorizontalDivider()

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column {
                                    Text(
                                        text = "العداد المسجل",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.Speed, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
                                        Spacer(modifier = Modifier.size(4.dp))
                                        Text(
                                            text = "${evt.odometerKm} كم",
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }

                                if (evt.priceDzd != null && evt.priceDzd > 0) {
                                    Column(horizontalAlignment = Alignment.End) {
                                        Text(
                                            text = "المبلغ الإجمالي",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Text(
                                            text = "${String.format(java.util.Locale.US, "%,.0f", evt.priceDzd)} د.ج",
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF10B981)
                                        )
                                    }
                                }
                            }

                            if (evt.serviceTemplates.isNotEmpty()) {
                                Text(
                                    text = "الخدمات المنجزة:",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold
                                )
                                FlowRow(
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    evt.serviceTemplates.forEach { tId ->
                                        Surface(
                                            shape = RoundedCornerShape(8.dp),
                                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                                        ) {
                                            Text(
                                                text = AppStrings.serviceTemplateName(lang, tId),
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.SemiBold,
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                            )
                                        }
                                    }
                                }
                            }

                            // Current Confirmation Status Badge
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                val statusBadgeColor = when (evt.confirmationStatus) {
                                    "CONFIRMED" -> Color(0xFF10B981)
                                    "DISPUTED" -> Color(0xFFDC2626)
                                    else -> Color(0xFFD97706)
                                }
                                Surface(
                                    color = statusBadgeColor.copy(alpha = 0.15f),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text(
                                        text = when (evt.confirmationStatus) {
                                            "CONFIRMED" -> if (lang == AppLanguage.ARABIC) "مؤكد من الزبون ✓" else "Confirmé par le client ✓"
                                            "DISPUTED" -> if (lang == AppLanguage.ARABIC) "معترض عليه من الزبون ⚠️" else "Contesté par le client ⚠️"
                                            else -> if (lang == AppLanguage.ARABIC) "في انتظار تأكيدك كمالك للسيارة" else "En attente de votre confirmation"
                                        },
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = statusBadgeColor,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Success Confirmation Card
            if (confirmationSuccess) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFF0FDF4)),
                        border = androidx.compose.foundation.BorderStroke(1.5.dp, Color(0xFF10B981))
                    ) {
                        Column(
                            modifier = Modifier.padding(20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF16A34A), modifier = Modifier.size(48.dp))
                            Text(
                                text = if (lang == AppLanguage.ARABIC) "تم تأكيد السجل بنجاح!" else "Intervention confirmée avec succès !",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Black,
                                color = Color(0xFF166534)
                            )
                            Text(
                                text = if (lang == AppLanguage.ARABIC)
                                    "أصبح السجل الآن معتمداً بمستوى (موثق من الورشة + مؤكد من الزبون) ومحفوظاً ضمن سلسلة التشفير الدائمة لسيارتك."
                                else
                                    "L'enregistrement est désormais certifié (Atelier + Client) dans la blockchain du véhicule.",
                                style = MaterialTheme.typography.bodySmall,
                                textAlign = TextAlign.Center,
                                color = Color(0xFF14532D)
                            )
                            Button(
                                onClick = { onNavigateToReport(vinInput) },
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0F172A))
                            ) {
                                Text(AppStrings.viewPassport(lang))
                            }
                        }
                    }
                }
            }

            // Disputed Card
            if (disputeSuccess) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFFEF2F2)),
                        border = androidx.compose.foundation.BorderStroke(1.5.dp, Color(0xFFDC2626))
                    ) {
                        Column(
                            modifier = Modifier.padding(20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(Icons.Default.ReportProblem, contentDescription = null, tint = Color(0xFFDC2626), modifier = Modifier.size(48.dp))
                            Text(
                                text = if (lang == AppLanguage.ARABIC) "تم تسجيل اعتراضك وإرساله للإدارة" else "Contestation enregistrée !",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF991B1B)
                            )
                            Text(
                                text = if (lang == AppLanguage.ARABIC)
                                    "تم تحويل هذا السجل فوراً إلى طابور التدقيق ومكافحة الاحتيال، ولن يظهر كحدث موثوق في جواز السيارة."
                                else
                                    "Cette intervention a été envoyée à la cellule anti-fraude pour enquête.",
                                style = MaterialTheme.typography.bodySmall,
                                textAlign = TextAlign.Center,
                                color = Color(0xFF7F1D1D)
                            )
                        }
                    }
                }
            }

            // Actions Buttons: Confirm / Dispute
            if (!confirmationSuccess && !disputeSuccess) {
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Button(
                            onClick = {
                                if (vinInput.length != 17) {
                                    coroutineScope.launch { snackbarHostState.showSnackbar("يرجى إدخال رقم VIN صحيح (17 حرفاً)") }
                                    return@Button
                                }
                                if (eventIdInput.isBlank()) {
                                    coroutineScope.launch { snackbarHostState.showSnackbar("يرجى إدخال معرّف الحدث") }
                                    return@Button
                                }
                                if (codeInput.length != 6) {
                                    coroutineScope.launch { snackbarHostState.showSnackbar("يرجى إدخال رمز التأكيد المكون من 6 أرقام") }
                                    return@Button
                                }
                                coroutineScope.launch {
                                    isSubmitting = true
                                    val result = repository.confirmServiceEvent(vinInput, eventIdInput, codeInput)
                                    isSubmitting = false
                                    result.onSuccess {
                                        confirmationSuccess = true
                                        snackbarHostState.showSnackbar(if (lang == AppLanguage.ARABIC) "تم التأكيد بنجاح" else "Confirmé avec succès")
                                    }.onFailure { err ->
                                        snackbarHostState.showSnackbar("خطأ: ${err.message}")
                                    }
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp)
                                .testTag("confirm_service_button"),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                            enabled = !isSubmitting
                        ) {
                            if (isSubmitting) {
                                CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                            } else {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Icon(Icons.Default.Verified, contentDescription = null, tint = Color.White)
                                    Text(
                                        text = AppStrings.confirmServiceBtn(lang),
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                }
                            }
                        }

                        OutlinedButton(
                            onClick = { showDisputeDialog = true },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("dispute_service_button"),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFDC2626)),
                            enabled = !isSubmitting
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Icon(Icons.Default.Error, contentDescription = null, tint = Color(0xFFDC2626))
                                Text(
                                    text = AppStrings.disputeServiceBtn(lang),
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFDC2626)
                                )
                            }
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(30.dp))
            }
        }
    }

    // Dispute reason dialog
    if (showDisputeDialog) {
        androidx.compose.ui.window.Dialog(onDismissRequest = { showDisputeDialog = false }) {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Text(
                        text = if (lang == AppLanguage.ARABIC) "اعتراض على سجل صيانة" else "Contester l'intervention",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFDC2626)
                    )

                    Text(
                        text = if (lang == AppLanguage.ARABIC)
                            "يرجى تحديد سبب الاعتراض لنقله إلى فريق مكافحة الاحتيال:"
                        else
                            "Précisez la raison de votre contestation :",
                        style = MaterialTheme.typography.bodySmall
                    )

                    val quickReasons = if (lang == AppLanguage.ARABIC) listOf(
                        "لم أزر هذه الورشة إطلاقاً",
                        "قراءة العداد غير صحيحة",
                        "الخدمات المذكورة لم تنفذ",
                        "السعر المسجل غير صحيح"
                    ) else listOf(
                        "Je n'ai jamais visité cet atelier",
                        "Kilométrage inexact",
                        "Interventions non réalisées",
                        "Tarif erroné"
                    )

                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        quickReasons.forEach { reason ->
                            FilterChip(
                                selected = disputeReason == reason,
                                onClick = { disputeReason = reason },
                                label = { Text(reason, fontSize = 11.sp) }
                            )
                        }
                    }

                    OutlinedTextField(
                        value = disputeReason,
                        onValueChange = { disputeReason = it },
                        label = { Text("سبب الاعتراض أو تفاصيل إضافية") },
                        modifier = Modifier.fillMaxWidth(),
                        maxLines = 3
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        OutlinedButton(onClick = { showDisputeDialog = false }) {
                            Text("إلغاء")
                        }
                        Spacer(modifier = Modifier.size(8.dp))
                        Button(
                            onClick = {
                                if (disputeReason.isBlank()) return@Button
                                showDisputeDialog = false
                                coroutineScope.launch {
                                    isSubmitting = true
                                    val res = repository.disputeServiceEvent(vinInput, eventIdInput, disputeReason)
                                    isSubmitting = false
                                    res.onSuccess {
                                        disputeSuccess = true
                                        snackbarHostState.showSnackbar("تم تسجيل الاعتراض")
                                    }.onFailure { err ->
                                        snackbarHostState.showSnackbar("Error: ${err.message}")
                                    }
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626))
                        ) {
                            Text("تأكيد الاعتراض")
                        }
                    }
                }
            }
        }
    }
}
