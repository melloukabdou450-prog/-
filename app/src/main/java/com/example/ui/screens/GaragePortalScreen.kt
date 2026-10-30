package com.example.ui.screens

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.HourglassTop
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
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
import androidx.compose.runtime.mutableStateListOf
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
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.ai.VinOcrService
import com.example.data.api.NhtsaVinService
import com.example.data.crypto.PassportCrypto
import com.example.data.model.GarageAccount
import com.example.data.model.VehicleEvent
import com.example.data.repository.VehicleRepository
import com.example.ui.components.QrCodeView
import com.example.ui.localization.AppLanguage
import com.example.ui.localization.AppStrings
import com.example.ui.localization.LocalLanguageManager
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import kotlinx.coroutines.launch
import java.net.URLEncoder
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

// List of 58 Algerian Wilayas
val ALGERIAN_WILAYAS = listOf(
    "01 - أدرار (Adrar)", "02 - الشلف (Chlef)", "03 - الأغواط (Laghouat)", "04 - أم البواقي (Oum El Bouaghi)",
    "05 - باتنة (Batna)", "06 - بجاية (Béjaïa)", "07 - بسكرة (Biskra)", "08 - بشار (Béchar)",
    "09 - البليدة (Blida)", "10 - البويرة (Bouira)", "11 - تمنراست (Tamanrasset)", "12 - تبسة (Tébessa)",
    "13 - تلمسان (Tlemcen)", "14 - تيارت (Tiaret)", "15 - تيزي وزو (Tizi Ouzou)", "16 - الجزائر (Alger)",
    "17 - الجلفة (Djelfa)", "18 - جيجل (Jijel)", "19 - سطيف (Sétif)", "20 - سعيدة (Saïda)",
    "21 - سكيكدة (Skikda)", "22 - سيدي بلعباس (Sidi Bel Abbès)", "23 - عنابة (Annaba)", "24 - قالمة (Guelma)",
    "25 - قسنطينة (Constantine)", "26 - المدية (Médéa)", "27 - مستغانم (Mostaganem)", "28 - المسيلة (M'Sila)",
    "29 - معسكر (Mascara)", "30 - ورقلة (Ouargla)", "31 - وهران (Oran)", "32 - البيض (El Bayadh)",
    "33 - إليزي (Illizi)", "34 - برج بوعريريج (Bordj Bou Arreridj)", "35 - بومرداس (Boumerdès)",
    "36 - الطارف (El Tarf)", "37 - تندوف (Tindouf)", "38 - تيسمسيلت (Tissemsilt)", "39 - الوادي (El Oued)",
    "40 - خنشلة (Khenchela)", "41 - سوق أهراس (Souk Ahras)", "42 - تيبازة (Tipaza)", "43 - ميلة (Mila)",
    "44 - عين الدفلى (Aïn Defla)", "45 - النعامة (Naâma)", "46 - عين تموشنت (Aïn Témouchent)",
    "47 - غرداية (Ghardaïa)", "48 - غليزان (Relizane)", "49 - تيميمون (Timimoun)", "50 - برج باجي مختار (Bordj Badji Mokhtar)",
    "51 - أولاد جلال (Ouled Djellal)", "52 - بني عباس (Béni Abbès)", "53 - عين صالح (In Salah)",
    "54 - عين قزام (In Guezzam)", "55 - تقرت (Touggourt)", "56 - جانت (Djanet)", "57 - المغير (El M'Ghair)",
    "58 - المنيعة (El Meniaa)"
)

// 12 Service Templates prescribed in Prompt
data class ServiceTemplateItem(
    val id: String,
    val nameArabic: String,
    val nameFrench: String,
    val iconEmoji: String
)

val SERVICE_TEMPLATES = listOf(
    ServiceTemplateItem("oil_change", "تغيير الزيت (Vidange)", "Vidange d'huile", "🛢️"),
    ServiceTemplateItem("filters", "تغيير الفلاتر", "Filtres", "💨"),
    ServiceTemplateItem("timing_belt", "حزام التوقيت (Distribution)", "Courroie distribution", "⚙️"),
    ServiceTemplateItem("brakes", "الفرامل (Freins)", "Freins / Plaquettes", "🛑"),
    ServiceTemplateItem("battery", "البطارية", "Batterie", "🔋"),
    ServiceTemplateItem("tires", "العجلات والإطارات", "Pneumatiques", "🛞"),
    ServiceTemplateItem("clutch", "الدبرياج (Embrayage)", "Embrayage", "🦶"),
    ServiceTemplateItem("suspension", "نظام التعليق", "Suspension", "🛞"),
    ServiceTemplateItem("body_repair", "سمكرة وهيكل", "Tôlerie & Structure", "🔨"),
    ServiceTemplateItem("painting", "صباغة ودهان", "Peinture", "🎨"),
    ServiceTemplateItem("diagnostics", "فحص إلكتروني (Scanner)", "Diagnostic Scanner", "💻"),
    ServiceTemplateItem("other", "أعمال أخرى", "Autre prestation", "🔧")
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GaragePortalScreen(
    repository: VehicleRepository,
    onNavigateBack: () -> Unit,
    onNavigateToVehicle: (String) -> Unit,
    onNavigateToAdmin: () -> Unit
) {
    val languageManager = LocalLanguageManager.current
    val lang = languageManager.currentLanguage
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    val currentUser = Firebase.auth.currentUser
    val uid = currentUser?.uid ?: ""

    // Garage reactive state
    val garageAccount by repository.observeGarage(uid).collectAsState(initial = null)
    var isCheckingGarage by remember { mutableStateOf(false) }

    // Selected portal tab: 0 = Fast Service Entry (< 60s), 1 = Garage Dashboard & Poster
    var selectedTab by remember { mutableIntStateOf(0) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFF0F172A)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Business, contentDescription = null, tint = Color(0xFFF59E0B), modifier = Modifier.size(20.dp))
                        }
                        Column {
                            Text(
                                text = AppStrings.garagePortalTitle(lang),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            if (garageAccount != null && garageAccount?.status == "approved") {
                                Text(
                                    text = "شريك معتمد: ${garageAccount?.garageName}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color(0xFF10B981),
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (repository.isUserAdmin()) {
                        IconButton(onClick = onNavigateToAdmin) {
                            Icon(Icons.Default.Security, contentDescription = "Admin", tint = Color(0xFFDC2626))
                        }
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
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (currentUser == null) {
                // Not authenticated
                GarageUnauthenticatedView(lang = lang, onNavigateBack = onNavigateBack)
            } else if (garageAccount == null) {
                // Register garage account view
                GarageRegistrationView(
                    repository = repository,
                    uid = uid,
                    ownerEmail = currentUser.email ?: "",
                    lang = lang,
                    onRegistered = {
                        coroutineScope.launch {
                            snackbarHostState.showSnackbar(if (lang == AppLanguage.ARABIC) "تم إرسال طلب تسجيل الورشة بنجاح" else "Demande d'agrément soumise")
                        }
                    }
                )
            } else if (garageAccount?.status == "pending" && !repository.isUserAdmin()) {
                // Pending approval
                GaragePendingView(
                    garage = garageAccount!!,
                    lang = lang,
                    onRefresh = {
                        coroutineScope.launch {
                            isCheckingGarage = true
                            repository.getGarage(uid)
                            isCheckingGarage = false
                            snackbarHostState.showSnackbar("تم تحديث حالة الحساب")
                        }
                    }
                )
            } else if (garageAccount?.status == "rejected" && !repository.isUserAdmin()) {
                // Rejected view
                GarageRejectedView(garage = garageAccount!!, lang = lang)
            } else {
                // APPROVED GARAGE WORKSPACE (or Admin bypass)
                val approvedGarage = garageAccount ?: GarageAccount(
                    uid = uid,
                    garageName = "ورشة الصيانة المعتمدة",
                    status = "approved"
                )

                Column(modifier = Modifier.fillMaxSize()) {
                    TabRow(
                        selectedTabIndex = selectedTab,
                        containerColor = MaterialTheme.colorScheme.surface
                    ) {
                        Tab(
                            selected = selectedTab == 0,
                            onClick = { selectedTab = 0 },
                            text = {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Icon(Icons.Default.Timer, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Text(
                                        text = AppStrings.fastEntryTitle(lang),
                                        fontWeight = if (selectedTab == 0) FontWeight.Bold else FontWeight.Normal,
                                        fontSize = 12.sp
                                    )
                                }
                            }
                        )
                        Tab(
                            selected = selectedTab == 1,
                            onClick = { selectedTab = 1 },
                            text = {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Icon(Icons.Default.QrCode, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Text(
                                        text = if (lang == AppLanguage.ARABIC) "لوحة التحكم والملصق" else "Tableau & Affiche QR",
                                        fontWeight = if (selectedTab == 1) FontWeight.Bold else FontWeight.Normal,
                                        fontSize = 12.sp
                                    )
                                }
                            }
                        )
                    }

                    when (selectedTab) {
                        0 -> FastServiceEntryTab(
                            garage = approvedGarage,
                            repository = repository,
                            lang = lang,
                            onEventSaved = { vin ->
                                // Optional callback
                            }
                        )
                        1 -> GarageDashboardTab(
                            garage = approvedGarage,
                            repository = repository,
                            lang = lang,
                            onNavigateToVehicle = onNavigateToVehicle
                        )
                    }
                }
            }
        }
    }
}

// -------------------------------------------------------------------------
// 1. Garage Registration Form
// -------------------------------------------------------------------------
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GarageRegistrationView(
    repository: VehicleRepository,
    uid: String,
    ownerEmail: String,
    lang: AppLanguage,
    onRegistered: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var garageName by remember { mutableStateOf("") }
    var selectedWilaya by remember { mutableStateOf(ALGERIAN_WILAYAS[15]) } // Default 16 - Alger
    var wilayaExpanded by remember { mutableStateOf(false) }
    var commune by remember { mutableStateOf("") }
    var address by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var rcPhotoUri by remember { mutableStateOf<Uri?>(null) }
    var isSubmitting by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        rcPhotoUri = uri
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A))
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = AppStrings.garageRegisterTitle(lang),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = if (lang == AppLanguage.ARABIC)
                            "انضم إلى شبكة الورش المعتمدة لجواز السيارة (Car Passport) لتوثيق خدمات الصيانة والإصلاح لزبائنك تشفيرياً."
                        else
                            "Rejoignez le réseau des ateliers agréés Car Passport pour certifier les opérations d'entretien.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF94A3B8)
                    )
                }
            }
        }

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
                    OutlinedTextField(
                        value = garageName,
                        onValueChange = { garageName = it },
                        label = { Text(if (lang == AppLanguage.ARABIC) "اسم الورشة أو مركز الصيانة *" else "Nom de l'atelier *") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("garage_name_input"),
                        singleLine = true
                    )

                    // Wilaya Dropdown
                    ExposedDropdownMenuBox(
                        expanded = wilayaExpanded,
                        onExpandedChange = { wilayaExpanded = !wilayaExpanded }
                    ) {
                        OutlinedTextField(
                            value = selectedWilaya,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("الولاية (58 ولاية)") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = wilayaExpanded) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor()
                        )
                        ExposedDropdownMenu(
                            expanded = wilayaExpanded,
                            onDismissRequest = { wilayaExpanded = false }
                        ) {
                            ALGERIAN_WILAYAS.forEach { w ->
                                DropdownMenuItem(
                                    text = { Text(w) },
                                    onClick = {
                                        selectedWilaya = w
                                        wilayaExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    OutlinedTextField(
                        value = commune,
                        onValueChange = { commune = it },
                        label = { Text(if (lang == AppLanguage.ARABIC) "البلدية *" else "Commune *") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = address,
                        onValueChange = { address = it },
                        label = { Text(if (lang == AppLanguage.ARABIC) "العنوان الكامل للورشة *" else "Adresse complète *") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = phone,
                        onValueChange = { phone = it },
                        label = { Text(if (lang == AppLanguage.ARABIC) "رقم هاتف الورشة *" else "Téléphone atelier *") },
                        leadingIcon = { Icon(Icons.Default.Phone, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        singleLine = true
                    )

                    // Photo of RC or business card
                    OutlinedButton(
                        onClick = { photoPickerLauncher.launch("image/*") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(Icons.Default.PhotoCamera, contentDescription = null)
                            Text(
                                text = if (rcPhotoUri != null)
                                    (if (lang == AppLanguage.ARABIC) "تم إرفاق صورة السجل التجاري / البطاقة ✓" else "Document joint ✓")
                                else
                                    (if (lang == AppLanguage.ARABIC) "إرفاق صورة السجل التجاري أو بطاقة العمل (اختياري)" else "Joindre photo Registre de Commerce (Optionnel)")
                            )
                        }
                    }

                    if (errorMessage.isNotBlank()) {
                        Text(text = errorMessage, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                    }

                    Button(
                        onClick = {
                            if (garageName.isBlank() || commune.isBlank() || address.isBlank() || phone.isBlank()) {
                                errorMessage = "يرجى ملء جميع الحقول الإلزامية (*)"
                                return@Button
                            }
                            isSubmitting = true
                            errorMessage = ""
                            coroutineScope.launch {
                                val newGarage = GarageAccount(
                                    uid = uid,
                                    garageName = garageName,
                                    wilaya = selectedWilaya,
                                    commune = commune,
                                    address = address,
                                    phone = phone,
                                    commercialRegisterPhotoUrl = rcPhotoUri?.toString() ?: "",
                                    status = "pending",
                                    ownerEmail = ownerEmail
                                )
                                val res = repository.registerGarage(newGarage)
                                isSubmitting = false
                                res.onSuccess {
                                    onRegistered()
                                }.onFailure { err ->
                                    errorMessage = "خطأ في التسجيل: ${err.message}"
                                }
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("submit_garage_registration"),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0F172A)),
                        enabled = !isSubmitting
                    ) {
                        if (isSubmitting) {
                            CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                        } else {
                            Text(
                                text = if (lang == AppLanguage.ARABIC) "إرسال طلب الانضمام كورشة معتمدة" else "Soumettre la demande d'agrément",
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}

// -------------------------------------------------------------------------
// 2. Pending Approval Notice
// -------------------------------------------------------------------------
@Composable
fun GaragePendingView(
    garage: GarageAccount,
    lang: AppLanguage,
    onRefresh: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(72.dp)
                .clip(CircleShape)
                .background(Color(0xFFFEF3C7)),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.HourglassTop, contentDescription = null, tint = Color(0xFFD97706), modifier = Modifier.size(36.dp))
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = if (lang == AppLanguage.ARABIC) "حسابك قيد المراجعة والتدقيق" else "Compte en attente de validation",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF92400E)
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = if (lang == AppLanguage.ARABIC)
                "طلب ورشة '${garage.garageName}' في ولاية ${garage.wilaya} قيد التدقيق لدى إدارة جواز السيارة. سيتم تفعيل حسابك فور التحقق من البيانات والسجل التجاري."
            else
                "Votre atelier '${garage.garageName}' (${garage.wilaya}) est en cours d'examen par l'administration Car Passport.",
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            color = Color(0xFF78350F)
        )

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = onRefresh,
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0F172A))
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Default.Refresh, contentDescription = null)
                Text(if (lang == AppLanguage.ARABIC) "التحقق من حالة الاعتماد" else "Vérifier le statut")
            }
        }
    }
}

// -------------------------------------------------------------------------
// 3. Rejected Notice
// -------------------------------------------------------------------------
@Composable
fun GarageRejectedView(garage: GarageAccount, lang: AppLanguage) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(Icons.Default.Close, contentDescription = null, tint = Color(0xFFDC2626), modifier = Modifier.size(60.dp))
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = if (lang == AppLanguage.ARABIC) "تم رفض طلب الاعتماد" else "Demande non retenue",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = Color(0xFFDC2626)
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = if (garage.rejectionReason.isNotBlank()) "السبب: ${garage.rejectionReason}" else "تعذر تأكيد بيانات الورشة أو السجل التجاري.",
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center
        )
    }
}

// -------------------------------------------------------------------------
// 4. Fast Service Entry View (< 60 seconds)
// -------------------------------------------------------------------------
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun FastServiceEntryTab(
    garage: GarageAccount,
    repository: VehicleRepository,
    lang: AppLanguage,
    onEventSaved: (String) -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var vinInput by remember { mutableStateOf("") }
    var licensePlateInput by remember { mutableStateOf("") }
    var odometerKmInput by remember { mutableStateOf("") }
    var priceInput by remember { mutableStateOf("") }
    var notesInput by remember { mutableStateOf("") }
    var customerPhoneInput by remember { mutableStateOf("") }

    // Date defaults to today YYYY-MM-DD
    val todayStr = remember { SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date()) }
    var dateInput by remember { mutableStateOf(todayStr) }

    // Service templates selection
    val selectedTemplates = remember { mutableStateListOf<String>() }

    // Odometer anomaly detection
    var highestKnownKm by remember { mutableStateOf(0L) }
    var isCheckingOdometer by remember { mutableStateOf(false) }

    // OCR Scanning State
    var isOcrScanning by remember { mutableStateOf(false) }
    var ocrErrorMessage by remember { mutableStateOf("") }

    // Submission & Confirmation Code Result Dialog
    var isSubmitting by remember { mutableStateOf(false) }
    var savedEventResult by remember { mutableStateOf<VehicleEvent?>(null) }
    var generatedCode by remember { mutableStateOf("") }
    var showSuccessDialog by remember { mutableStateOf(false) }
    var formError by remember { mutableStateOf("") }

    // Camera / Photo launcher for VIN OCR
    val ocrPhotoLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            coroutineScope.launch {
                isOcrScanning = true
                ocrErrorMessage = ""
                try {
                    val bitmap = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                        ImageDecoder.decodeBitmap(ImageDecoder.createSource(context.contentResolver, uri)) { decoder, _, _ ->
                            decoder.isMutableRequired = true
                        }
                    } else {
                        @Suppress("DEPRECATION")
                        MediaStore.Images.Media.getBitmap(context.contentResolver, uri)
                    }
                    val ocrResult = VinOcrService.extractVinFromImage(bitmap)
                    if (ocrResult.isValid && ocrResult.vin.length == 17) {
                        vinInput = ocrResult.vin
                    } else {
                        ocrErrorMessage = ocrResult.errorMessage.ifBlank { "تعذر استخراج رقم VIN واضح من الصورة" }
                    }
                } catch (e: Exception) {
                    ocrErrorMessage = "خطأ في قراءة الصورة: ${e.message}"
                } finally {
                    isOcrScanning = false
                }
            }
        }
    }

    // Check previous odometer reading for this VIN to flag anomaly live
    LaunchedEffect(vinInput) {
        if (vinInput.length == 17) {
            isCheckingOdometer = true
            try {
                val vehRes = repository.getVehicle(vinInput)
                highestKnownKm = vehRes.getOrNull()?.latestOdometerKm ?: 0L
            } catch (_: Exception) {} finally {
                isCheckingOdometer = false
            }
        } else {
            highestKnownKm = 0L
        }
    }

    val currentEnteredKm = odometerKmInput.toLongOrNull() ?: 0L
    val hasOdometerAnomaly = currentEnteredKm > 0 && highestKnownKm > 0 && currentEnteredKm < highestKnownKm

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Fast Entry Banner (< 60s)
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A))
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(Icons.Default.FlashOn, contentDescription = null, tint = Color(0xFFF59E0B), modifier = Modifier.size(24.dp))
                    Column {
                        Text(
                            text = "تسجيل فوري خلال أقل من 60 ثانية",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = "اختر العمليات، ادخل العداد، وأرسل رابط التأكيد الفوري للزبون عبر واتساب.",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF94A3B8)
                        )
                    }
                }
            }
        }

        // Section 1: Vehicle Identifier (VIN or Plate)
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
                        text = "1. تحديد المركبة (VIN أو الترقيم) *",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = vinInput,
                            onValueChange = { vinInput = it.uppercase().replace(" ", "").take(17) },
                            label = { Text("رقم الهيكل VIN (17 حرفاً)") },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("fast_vin_input"),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters)
                        )

                        // Camera OCR Button via Gemini
                        Button(
                            onClick = { ocrPhotoLauncher.launch("image/*") },
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.height(56.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0F172A)),
                            enabled = !isOcrScanning
                        ) {
                            if (isOcrScanning) {
                                CircularProgressIndicator(color = Color.White, modifier = Modifier.size(20.dp))
                            } else {
                                Icon(Icons.Default.CameraAlt, contentDescription = "Scan VIN via Camera")
                            }
                        }
                    }

                    if (ocrErrorMessage.isNotBlank()) {
                        Text(text = ocrErrorMessage, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                    }

                    // Optional license plate field
                    OutlinedTextField(
                        value = licensePlateInput,
                        onValueChange = { licensePlateInput = it },
                        label = { Text("أو رقم لوحة الترقيم (Matricule e.g. 01234-121-16)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }
            }
        }

        // Section 2: Big Template Buttons (Requirement: "Service templates as big buttons")
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
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "2. نوع الخدمة أو الصيانة المنجزة *",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "(${selectedTemplates.size} محدد)",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        SERVICE_TEMPLATES.forEach { item ->
                            val isSelected = selectedTemplates.contains(item.id)
                            Surface(
                                modifier = Modifier
                                    .clickable {
                                        if (isSelected) selectedTemplates.remove(item.id)
                                        else selectedTemplates.add(item.id)
                                    }
                                    .clip(RoundedCornerShape(12.dp))
                                    .border(
                                        BorderStroke(
                                            width = if (isSelected) 2.dp else 1.dp,
                                            color = if (isSelected) Color(0xFF10B981) else Color(0xFFCBD5E1)
                                        ),
                                        shape = RoundedCornerShape(12.dp)
                                    ),
                                color = if (isSelected) Color(0xFFECFDF5) else Color(0xFFF8FAFC)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text(item.iconEmoji, fontSize = 16.sp)
                                    Text(
                                        text = if (lang == AppLanguage.ARABIC) item.nameArabic else item.nameFrench,
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        color = if (isSelected) Color(0xFF065F46) else Color(0xFF1E293B)
                                    )
                                    if (isSelected) {
                                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF10B981), modifier = Modifier.size(16.dp))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Section 3: Odometer, Date, Price, and Anomaly Warning
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
                        text = "3. قراءة العداد والتاريخ *",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )

                    OutlinedTextField(
                        value = odometerKmInput,
                        onValueChange = { odometerKmInput = it.replace(Regex("[^0-9]"), "") },
                        label = { Text("قراءة العداد الحالية (كم) *") },
                        leadingIcon = { Icon(Icons.Default.Speed, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("fast_odometer_input"),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true
                    )

                    // LIVE ODOMETER ANOMALY BANNER (Non-blocking warning)
                    if (hasOdometerAnomaly) {
                        Surface(
                            color = Color(0xFFFEF2F2),
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.dp, Color(0xFFF87171))
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(Icons.Default.Warning, contentDescription = null, tint = Color(0xFFDC2626))
                                Column {
                                    Text(
                                        text = "⚠️ تنبيه تراجع بالعداد (odometer_anomaly)",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF991B1B)
                                    )
                                    Text(
                                        text = "القراءة المدخلة ($currentEnteredKm كم) أقل من أعلى قراءة مسجلة سابقاً ($highestKnownKm كم). لن يتم حظر العملية، ولكن سيتم وسمها كشبهة تلاعب بالعداد.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color(0xFF7F1D1D)
                                    )
                                }
                            }
                        }
                    }

                    // Date input
                    OutlinedTextField(
                        value = dateInput,
                        onValueChange = { dateInput = it },
                        label = { Text("تاريخ الخدمة (YYYY-MM-DD)") },
                        leadingIcon = { Icon(Icons.Default.DateRange, contentDescription = null) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    // Price (optional)
                    OutlinedTextField(
                        value = priceInput,
                        onValueChange = { priceInput = it.replace(Regex("[^0-9.]"), "") },
                        label = { Text("المبلغ الإجمالي بالدينار (اختياري DZD)") },
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true
                    )
                }
            }
        }

        // Section 4: Customer Phone & Privacy Hashing
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
                        text = "4. هاتف الزبون (لإرسال كود التأكيد عبر واتساب)",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )

                    OutlinedTextField(
                        value = customerPhoneInput,
                        onValueChange = { customerPhoneInput = it },
                        label = { Text("رقم هاتف الزبون (مثال: 0550123456)") },
                        leadingIcon = { Icon(Icons.Default.Phone, contentDescription = null, tint = Color(0xFF10B981)) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("fast_customer_phone_input"),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        singleLine = true
                    )

                    Surface(
                        color = Color(0xFFF1F5F9),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(Icons.Default.Lock, contentDescription = null, modifier = Modifier.size(14.dp), tint = Color(0xFF475569))
                            Text(
                                text = "🔒 حماية الخصوصية: لن يتم حفظ رقم الهاتف في السيرفر. يتم تخزين بصمة مشفرة SHA-256 فقط.",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFF475569),
                                fontSize = 10.sp
                            )
                        }
                    }

                    OutlinedTextField(
                        value = notesInput,
                        onValueChange = { notesInput = it },
                        label = { Text("ملاحظات إضافية أو قطع الغيار المستبدلة (اختياري)") },
                        modifier = Modifier.fillMaxWidth(),
                        maxLines = 2
                    )
                }
            }
        }

        if (formError.isNotBlank()) {
            item {
                Text(text = formError, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
            }
        }

        // Submit Button (Under 60s Fast Logging)
        item {
            Button(
                onClick = {
                    val cleanVin = if (vinInput.isNotBlank()) vinInput.trim().uppercase()
                    else if (licensePlateInput.isNotBlank()) "PLT" + licensePlateInput.replace(Regex("[^0-9A-Z]"), "").take(14).padStart(14, '0')
                    else ""

                    if (cleanVin.length != 17) {
                        formError = "يرجى إدخال رقم VIN صحيح (17 حرفاً)"
                        return@Button
                    }
                    if (selectedTemplates.isEmpty()) {
                        formError = "يرجى تحديد خدمة واحدة على الأقل من الأزرار أعلاه"
                        return@Button
                    }
                    val km = odometerKmInput.toLongOrNull()
                    if (km == null || km <= 0) {
                        formError = "يرجى إدخال قراءة عداد صالحة"
                        return@Button
                    }

                    formError = ""
                    isSubmitting = true

                    val templateTitles = selectedTemplates.joinToString(" + ") { tId ->
                        SERVICE_TEMPLATES.find { it.id == tId }?.nameArabic ?: tId
                    }

                    coroutineScope.launch {
                        val res = repository.addGarageServiceEvent(
                            vin = cleanVin,
                            garage = garage,
                            serviceTemplates = selectedTemplates.toList(),
                            title = templateTitles,
                            description = notesInput,
                            date = dateInput,
                            odometerKm = km,
                            priceDzd = priceInput.toDoubleOrNull(),
                            customerPhone = customerPhoneInput
                        )
                        isSubmitting = false
                        res.onSuccess { pair ->
                            val (savedEvent, rawCode) = pair
                            savedEventResult = savedEvent
                            generatedCode = rawCode
                            showSuccessDialog = true
                            onEventSaved(cleanVin)
                        }.onFailure { err ->
                            formError = "فشل تسجيل العملية: ${err.message}"
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
                    .testTag("submit_service_entry_button"),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                enabled = !isSubmitting
            ) {
                if (isSubmitting) {
                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                } else {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Default.Verified, contentDescription = null, tint = Color.White)
                        Text(
                            text = "توثيق الخدمة وحفظ السجل المعتمد",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(30.dp))
        }
    }

    // Success & WhatsApp Modal Dialog
    if (showSuccessDialog && savedEventResult != null) {
        val evt = savedEventResult!!
        val cleanPhone = PassportCrypto.cleanPhone(customerPhoneInput)

        // WhatsApp prefilled message in Arabic per requirement:
        // "Show a 'Send via WhatsApp' button that opens wa.me with a prefilled Arabic message to the customer's phone:
        // summary of the service, km, date, and the link."
        val confirmLink = "https://carpassport.dz/confirm/${evt.id}?vin=${evt.vin}&code=$generatedCode"
        val whatsappMessage = """
مرحباً بك من ورشة ${garage.garageName}! 🚗
تم تسجيل عملية صيانة لسيارتك (VIN: ${evt.vin}):
• التاريخ: ${evt.date}
• العداد: ${evt.odometerKm} كم
• الخدمات: ${evt.title}
• رمز التأكيد الخاص بك: $generatedCode

يرجى الضغط على الرابط لتأكيد العملية وحفظها رسمياً في جواز سيارتك:
$confirmLink
        """.trimIndent()

        androidx.compose.ui.window.Dialog(onDismissRequest = { showSuccessDialog = false }) {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF16A34A), modifier = Modifier.size(54.dp))

                    Text(
                        text = "تم توثيق العملية وتشفيرها بنجاح!",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Black,
                        color = Color(0xFF166534)
                    )

                    Surface(
                        color = Color(0xFFFEF3C7),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.5.dp, Color(0xFFF59E0B))
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "رمز تأكيد الزبون (6 أرقام):",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFF92400E)
                            )
                            Text(
                                text = generatedCode,
                                style = MaterialTheme.typography.headlineLarge,
                                fontWeight = FontWeight.Black,
                                letterSpacing = 4.sp,
                                color = Color(0xFF78350F)
                            )
                        }
                    }

                    Text(
                        text = "الرابط المخصص: /confirm/${evt.id}",
                        style = MaterialTheme.typography.labelSmall,
                        fontFamily = FontFamily.Monospace,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    // Big Send via WhatsApp Button
                    Button(
                        onClick = {
                            try {
                                val encodedText = URLEncoder.encode(whatsappMessage, "UTF-8")
                                val targetPhone = if (cleanPhone.startsWith("0")) "213" + cleanPhone.substring(1) else cleanPhone
                                val waUri = if (targetPhone.isNotBlank()) {
                                    Uri.parse("https://wa.me/$targetPhone?text=$encodedText")
                                } else {
                                    Uri.parse("https://api.whatsapp.com/send?text=$encodedText")
                                }
                                val intent = Intent(Intent.ACTION_VIEW, waUri)
                                context.startActivity(intent)
                            } catch (_: Exception) {}
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("send_whatsapp_button"),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF25D366)) // WhatsApp brand green
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(Icons.Default.Send, contentDescription = null, tint = Color.White)
                            Text(
                                text = AppStrings.sendViaWhatsApp(lang),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }

                    OutlinedButton(
                        onClick = { showSuccessDialog = false },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("إغلاق والانتقال لعملية جديدة")
                    }
                }
            }
        }
    }
}

// -------------------------------------------------------------------------
// 5. Garage Dashboard & QR Poster Tab
// -------------------------------------------------------------------------
@Composable
fun GarageDashboardTab(
    garage: GarageAccount,
    repository: VehicleRepository,
    lang: AppLanguage,
    onNavigateToVehicle: (String) -> Unit
) {
    val events by repository.observeGarageEvents(garage.uid).collectAsState(initial = emptyList())
    var showQrPosterDialog by remember { mutableStateOf(false) }

    // Compute weekly events count (last 7 days)
    val oneWeekAgo = remember {
        val cal = Calendar.getInstance()
        cal.add(Calendar.DAY_OF_YEAR, -7)
        cal.timeInMillis / 1000L
    }
    val weeklyEvents = events.filter { (it.createdAt?.seconds ?: 0L) >= oneWeekAgo }

    // Confirmation rate %
    val confirmedCount = events.count { it.confirmationStatus == "CONFIRMED" }
    val confirmationRate = if (events.isNotEmpty()) {
        (confirmedCount.toDouble() / events.size.toDouble()) * 100.0
    } else 100.0

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Stats Cards Row
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                MetricCardItem(
                    title = "هذا الأسبوع",
                    value = weeklyEvents.size.toString(),
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.weight(1f)
                )
                MetricCardItem(
                    title = "نسبة التأكيد",
                    value = "${String.format(Locale.US, "%.0f", confirmationRate)}%",
                    color = if (confirmationRate >= 70) Color(0xFF10B981) else Color(0xFFD97706),
                    modifier = Modifier.weight(1f)
                )
                MetricCardItem(
                    title = "إجمالي السجلات",
                    value = events.size.toString(),
                    color = Color(0xFF0F172A),
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // QR Poster Card (Requirement 4: "simple QR poster: Scan to see your car's logbook")
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = AppStrings.qrPosterTitle(lang),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = AppStrings.qrPosterSubtitle(lang),
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFFF59E0B)
                        )
                    }
                    Button(
                        onClick = { showQrPosterDialog = true },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF59E0B)),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Icon(Icons.Default.QrCode, contentDescription = null, tint = Color(0xFF0F172A))
                            Text("عرض الملصق", color = Color(0xFF0F172A), fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // Recent Entries Header
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "آخر السجلات الموثقة (${events.size})",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        if (events.isEmpty()) {
            item {
                Box(modifier = Modifier.fillMaxWidth().padding(40.dp), contentAlignment = Alignment.Center) {
                    Text(
                        text = "لم تقم بتسجيل أي عمليات صيانة بعد. استخدم تبويب 'تسجيل صيانة سريع' لبدء التوثيق.",
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        items(events, key = { it.id }) { evt ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = evt.title,
                            style = MaterialTheme.typography.titleSmall,
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
                        Text(
                            text = "VIN: ${evt.vin} • العداد: ${evt.odometerKm} كم",
                            style = MaterialTheme.typography.bodySmall,
                            fontFamily = FontFamily.Monospace
                        )

                        // Status Badge
                        val (statusText, statusColor) = when (evt.confirmationStatus) {
                            "CONFIRMED" -> Pair("مؤكد من الزبون ✓", Color(0xFF10B981))
                            "DISPUTED" -> Pair("معترض عليه ⚠️", Color(0xFFDC2626))
                            else -> Pair("في انتظار التأكيد ⏳", Color(0xFFD97706))
                        }
                        Surface(
                            color = statusColor.copy(alpha = 0.15f),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = statusText,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = statusColor,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                            )
                        }
                    }

                    if (evt.hasOdometerAnomaly) {
                        Text(
                            text = "⚠️ تم رصد انخفاض بالعداد (odometer_anomaly)",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFFDC2626),
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        OutlinedButton(
                            onClick = { onNavigateToVehicle(evt.vin) },
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("فتح الجواز", fontSize = 11.sp)
                        }
                    }
                }
            }
        }
    }

    // High-Resolution Printable QR Poster Modal
    if (showQrPosterDialog) {
        val portalQrUrl = "https://carpassport.dz/garage/${garage.uid}"
        androidx.compose.ui.window.Dialog(onDismissRequest = { showQrPosterDialog = false }) {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = BorderStroke(2.dp, Color(0xFFD97706))
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // Header
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Default.Verified, contentDescription = null, tint = Color(0xFFD97706), modifier = Modifier.size(28.dp))
                        Text(
                            text = "CAR PASSPORT • جواز السيارة",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Black,
                            color = Color(0xFF0F172A)
                        )
                    }

                    Text(
                        text = "مركز خدمة معتمد: ${garage.garageName}",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1E293B)
                    )

                    HorizontalDivider()

                    // QR Poster Canvas
                    QrCodeView(
                        data = portalQrUrl,
                        size = 200.dp,
                        qrColor = Color(0xFF0F172A)
                    )

                    Text(
                        text = AppStrings.qrPosterSubtitle(lang),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Black,
                        color = Color(0xFF0F172A),
                        textAlign = TextAlign.Center
                    )

                    Text(
                        text = "امسح الكود بكاميرا هاتفك لعرض سجل وتاريخ صيانة سيارتك الموثق بالكامل.",
                        style = MaterialTheme.typography.bodySmall,
                        textAlign = TextAlign.Center,
                        color = Color(0xFF64748B)
                    )

                    Button(
                        onClick = { showQrPosterDialog = false },
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0F172A))
                    ) {
                        Text("إغلاق الملصق")
                    }
                }
            }
        }
    }
}

@Composable
private fun MetricCardItem(
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
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Black,
                color = color
            )
            Text(
                text = title,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 11.sp
            )
        }
    }
}

@Composable
fun GarageUnauthenticatedView(lang: AppLanguage, onNavigateBack: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(Icons.Default.Lock, contentDescription = null, modifier = Modifier.size(56.dp), tint = MaterialTheme.colorScheme.primary)
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = if (lang == AppLanguage.ARABIC) "تسجيل الدخول مطلوب لبوابة الورش" else "Connexion requise pour le portail atelier",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = if (lang == AppLanguage.ARABIC)
                "يرجى تسجيل الدخول بحساب Google أو الهاتف من الشاشة الرئيسية للوصول إلى بوابة الورش واعتمادها."
            else
                "Veuillez vous connecter pour accéder à votre espace atelier agréé.",
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(20.dp))
        Button(onClick = onNavigateBack) {
            Text(if (lang == AppLanguage.ARABIC) "العودة للرئيسية وتسجيل الدخول" else "Retour à l'accueil")
        }
    }
}
