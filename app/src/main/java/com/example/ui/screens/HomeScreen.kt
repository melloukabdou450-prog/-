package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
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
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.filled.Warning
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
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.ui.unit.sp
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
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import com.example.data.api.DecodedVehicleData
import com.example.data.api.NhtsaVinService
import com.example.data.api.VinValidationResult
import com.example.data.model.Vehicle
import com.example.data.repository.VehicleRepository
import com.example.ui.localization.AppLanguage
import com.example.ui.localization.AppStrings
import com.example.ui.localization.LocalLanguageManager
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun HomeScreen(
    repository: VehicleRepository,
    onNavigateToVehicle: (String) -> Unit,
    onNavigateToMarketData: () -> Unit,
    onNavigateToGaragePortal: () -> Unit = {},
    onNavigateToCustomerConfirm: () -> Unit = {},
    onNavigateToAdminFraud: () -> Unit = {},
    onNavigateToPrivacy: () -> Unit = {}
) {
    val languageManager = LocalLanguageManager.current
    val lang = languageManager.currentLanguage
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    var vinInput by remember { mutableStateOf("") }
    var isDecoding by remember { mutableStateOf(false) }
    var isSaving by remember { mutableStateOf(false) }
    var decodedResult by remember { mutableStateOf<DecodedVehicleData?>(null) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var showAuthDialog by remember { mutableStateOf(false) }

    val currentUser = Firebase.auth.currentUser
    val recentVehicles by repository.observeRecentVehicles(15).collectAsState(initial = emptyList())

    val sampleVins = listOf(
        "Honda Accord" to "1HGCR2F83HA123456",
        "Toyota Camry" to "4T1BF1FK5EU123456",
        "Ford F-150" to "1FTFW1ED4MFA12345",
        "BMW 328i" to "WBA3A5C55CF123456"
    )

    fun executeDecode(targetVin: String) {
        val clean = targetVin.trim().uppercase()
        focusManager.clearFocus()
        errorMessage = null

        val validation = NhtsaVinService.validateVin(clean)
        if (validation is VinValidationResult.Invalid) {
            errorMessage = if (lang == AppLanguage.ARABIC) validation.reasonArabic else validation.reasonFrench
            return
        }

        isDecoding = true
        coroutineScope.launch {
            val result = NhtsaVinService.decodeVin(clean)
            isDecoding = false
            result.onSuccess { data ->
                decodedResult = data
                errorMessage = null
            }.onFailure { err ->
                errorMessage = err.localizedMessage ?: "Failed to decode VIN"
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color(0xFF0F172A)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Shield,
                                contentDescription = null,
                                tint = Color(0xFFF59E0B),
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Column {
                            Text(
                                text = AppStrings.appTitle(lang),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.ExtraBold
                            )
                            Text(
                                text = AppStrings.appSubtitle(lang),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                actions = {
                    // Admin Market Data button (Stage 3)
                    IconButton(
                        onClick = onNavigateToMarketData,
                        modifier = Modifier.testTag("open_market_data_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.AdminPanelSettings,
                            contentDescription = "Market Data",
                            tint = Color(0xFF10B981)
                        )
                    }

                    // Language Switcher Chip
                    FilterChip(
                        selected = true,
                        onClick = { languageManager.toggleLanguage() },
                        label = {
                            Text(
                                text = if (lang == AppLanguage.ARABIC) "Français" else "العربية",
                                fontWeight = FontWeight.Bold
                            )
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Language,
                                contentDescription = "Language",
                                modifier = Modifier.size(16.dp)
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.secondaryContainer
                        )
                    )

                    IconButton(onClick = onNavigateToPrivacy) {
                        Icon(
                            imageVector = Icons.Default.Security,
                            contentDescription = "Privacy Policy",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }

                    Spacer(modifier = Modifier.width(4.dp))

                    // User Account Icon
                    IconButton(
                        onClick = {
                            if (currentUser == null) {
                                showAuthDialog = true
                            } else {
                                val credMgr = CredentialManager.create(context)
                                performSignOut(context, credMgr, coroutineScope) {
                                    coroutineScope.launch {
                                        snackbarHostState.showSnackbar(
                                            if (lang == AppLanguage.ARABIC) "تم تسجيل الخروج" else "Déconnecté"
                                        )
                                    }
                                }
                            }
                        }
                    ) {
                        Icon(
                            imageVector = if (currentUser != null) Icons.Default.AccountCircle else Icons.Default.Lock,
                            contentDescription = "Account",
                            tint = if (currentUser != null) Color(0xFF10B981) else MaterialTheme.colorScheme.onSurfaceVariant
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
                // Auth status banner if guest
                if (currentUser == null) {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.5f)
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showAuthDialog = true }
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Lock,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                                Text(
                                    text = AppStrings.signInRequired(lang),
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                            Text(
                                text = AppStrings.signInWithGoogle(lang),
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = AppStrings.signedInAs(lang, currentUser.email ?: "Google User"),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        TextButton(
                            onClick = {
                                val credMgr = CredentialManager.create(context)
                                performSignOut(context, credMgr, coroutineScope) {}
                            }
                        ) {
                            Text(AppStrings.signOut(lang), style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
            }

            // VIN Input Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f))
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.DirectionsCar,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = AppStrings.enterVin(lang),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        // Text Field with real-time formatting & validation
                        OutlinedTextField(
                            value = vinInput,
                            onValueChange = { newVal ->
                                // Filter out spaces and force uppercase
                                val filtered = newVal.filter { !it.isWhitespace() }.uppercase()
                                if (filtered.length <= 17) {
                                    vinInput = filtered
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("vin_input_field"),
                            placeholder = { Text(AppStrings.vinPlaceholder(lang)) },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(
                                capitalization = KeyboardCapitalization.Characters,
                                imeAction = ImeAction.Search
                            ),
                            keyboardActions = KeyboardActions(
                                onSearch = { executeDecode(vinInput) }
                            ),
                            supportingText = {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    // Forbidden character alert
                                    val hasForbidden = vinInput.any { it in "IOQioq" }
                                    if (hasForbidden) {
                                        Text(
                                            text = if (lang == AppLanguage.ARABIC) "الأحرف (I, O, Q) غير مسموحة!" else "Lettres (I, O, Q) interdites !",
                                            color = MaterialTheme.colorScheme.error,
                                            style = MaterialTheme.typography.labelSmall
                                        )
                                    } else {
                                        Text(
                                            text = if (lang == AppLanguage.ARABIC) "17 حرفاً ورقماً حسب معيار ISO 3779" else "17 caractères selon ISO 3779",
                                            style = MaterialTheme.typography.labelSmall
                                        )
                                    }
                                    Text(
                                        text = "${vinInput.length}/17",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = if (vinInput.length == 17) Color(0xFF10B981) else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = MaterialTheme.colorScheme.outline
                            ),
                            shape = RoundedCornerShape(12.dp)
                        )

                        // Quick sample VIN buttons
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(
                                text = AppStrings.quickSamples(lang),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                sampleVins.forEach { (label, sampleVin) ->
                                    FilterChip(
                                        selected = vinInput == sampleVin,
                                        onClick = {
                                            vinInput = sampleVin
                                            executeDecode(sampleVin)
                                        },
                                        label = { Text(label, style = MaterialTheme.typography.labelSmall) }
                                    )
                                }
                            }
                        }

                        // Decode Button
                        Button(
                            onClick = { executeDecode(vinInput) },
                            enabled = !isDecoding && vinInput.length == 17,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                                .testTag("decode_vin_button"),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF0F172A)
                            )
                        ) {
                            if (isDecoding) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(24.dp),
                                    color = Color.White,
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Search,
                                        contentDescription = null,
                                        tint = Color(0xFFF59E0B)
                                    )
                                    Text(
                                        text = AppStrings.decodeVinBtn(lang),
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                }
                            }
                        }

                        // Error Banner
                        if (errorMessage != null) {
                            Card(
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.errorContainer
                                ),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Warning,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.error
                                    )
                                    Text(
                                        text = errorMessage ?: "",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onErrorContainer
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Decoded Result Card
            if (decodedResult != null) {
                val data = decodedResult!!
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        ),
                        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(18.dp),
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = "${data.year} ${data.make} ${data.model}",
                                        style = MaterialTheme.typography.titleLarge,
                                        fontWeight = FontWeight.ExtraBold
                                    )
                                    Text(
                                        text = data.vin,
                                        style = MaterialTheme.typography.bodySmall,
                                        fontFamily = FontFamily.Monospace,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(Color(0xFF0F172A).copy(alpha = 0.1f))
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Text(
                                        text = data.vehicleType,
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }

                            HorizontalDivider()

                            // Low confidence warning banner per requirement 1:
                            // "Show make/model/year/engine, and a 'low confidence' flag if fields are missing."
                            if (data.hasLowConfidence) {
                                Card(
                                    colors = CardDefaults.cardColors(
                                        containerColor = Color(0xFFFEF3C7) // Amber tint
                                    ),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Column(
                                        modifier = Modifier.padding(12.dp),
                                        verticalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Warning,
                                                contentDescription = null,
                                                tint = Color(0xFFD97706),
                                                modifier = Modifier.size(20.dp)
                                            )
                                            Text(
                                                text = AppStrings.lowConfidenceWarning(lang),
                                                style = MaterialTheme.typography.titleSmall,
                                                fontWeight = FontWeight.Bold,
                                                color = Color(0xFF92400E)
                                            )
                                        }
                                        Text(
                                            text = AppStrings.lowConfidenceDesc(lang, data.missingFields),
                                            style = MaterialTheme.typography.bodySmall,
                                            color = Color(0xFF78350F)
                                        )
                                    }
                                }
                            }

                            // Vehicle Specs Grid
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                SpecRow(AppStrings.make(lang), data.make)
                                SpecRow(AppStrings.model(lang), data.model)
                                SpecRow(AppStrings.year(lang), data.year.toString())
                                SpecRow(AppStrings.engine(lang), data.engine)
                                SpecRow(AppStrings.country(lang), data.plantCountry)
                            }

                            // Save Vehicle in Firestore (Requirement 2: Save the vehicle in Firestore at vehicles/{vin})
                            Button(
                                onClick = {
                                    if (currentUser == null) {
                                        showAuthDialog = true
                                    } else {
                                        isSaving = true
                                        coroutineScope.launch {
                                            val vehicle = Vehicle(
                                                vin = data.vin,
                                                make = data.make,
                                                model = data.model,
                                                year = data.year,
                                                engine = data.engine,
                                                vehicleType = data.vehicleType,
                                                plantCountry = data.plantCountry,
                                                hasLowConfidence = data.hasLowConfidence,
                                                missingFields = data.missingFields,
                                                createdBy = currentUser.uid
                                            )
                                            val saveRes = repository.saveVehicle(vehicle)
                                            isSaving = false
                                            saveRes.onSuccess {
                                                snackbarHostState.showSnackbar(
                                                    if (lang == AppLanguage.ARABIC) "تم حفظ المركبة بنجاح في الجواز" else "Véhicule enregistré avec succès"
                                                )
                                                onNavigateToVehicle(data.vin)
                                            }.onFailure { err ->
                                                snackbarHostState.showSnackbar("Error: ${err.message}")
                                            }
                                        }
                                    }
                                },
                                enabled = !isSaving,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp)
                                    .testTag("save_vehicle_button"),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFF10B981) // Emerald
                                )
                            ) {
                                if (isSaving) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(20.dp),
                                        color = Color.White,
                                        strokeWidth = 2.dp
                                    )
                                } else {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.CheckCircle,
                                            contentDescription = null,
                                            tint = Color.White
                                        )
                                        Text(
                                            text = AppStrings.saveVehicle(lang),
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // STAGE 4: Garage Portal & Customer Confirmation Cards
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A))
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color(0xFF1E293B)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Business,
                                    contentDescription = null,
                                    tint = Color(0xFFF59E0B),
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                            Column {
                                Text(
                                    text = AppStrings.garagePortalTitle(lang),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                Text(
                                    text = if (lang == AppLanguage.ARABIC) "تسجيل صيانة سريع (< 60 ثانية) • كود واتساب" else "Saisie rapide < 60s • Affiche QR & WhatsApp",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color(0xFF94A3B8)
                                )
                            }
                        }

                        Button(
                            onClick = onNavigateToGaragePortal,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("open_garage_portal_button"),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF59E0B))
                        ) {
                            Text(
                                text = if (lang == AppLanguage.ARABIC) "دخول بوابة الورش ومراكز الصيانة" else "Accéder au Portail Atelier",
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF0F172A)
                            )
                        }
                    }
                }
            }

            // Customer Confirmation Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF10B981).copy(alpha = 0.5f))
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
                                imageVector = Icons.Default.Verified,
                                contentDescription = null,
                                tint = Color(0xFF10B981),
                                modifier = Modifier.size(22.dp)
                            )
                            Text(
                                text = AppStrings.customerConfirmTitle(lang),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Text(
                            text = if (lang == AppLanguage.ARABIC)
                                "هل وصلتك رسالة واتساب برمز تأكيد من ورشة معتمدة؟ اضغط هنا لتأكيد أو الاعتراض على سجل صيانة سيارتك."
                            else
                                "Avez-vous reçu un code de confirmation WhatsApp ? Validez ou contestez l'intervention ici.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        OutlinedButton(
                            onClick = onNavigateToCustomerConfirm,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("open_customer_confirm_button"),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text(
                                text = if (lang == AppLanguage.ARABIC) "إدخال رمز التأكيد (6 أرقام)" else "Saisir le code de confirmation",
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            // Admin Anti-Fraud Dashboard Card (if Admin)
            if (repository.isUserAdmin()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFFEF2F2)),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFEF4444))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.Security, contentDescription = null, tint = Color(0xFFDC2626))
                                Column {
                                    Text(
                                        text = AppStrings.antiFraudTitle(lang),
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF991B1B)
                                    )
                                    Text(
                                        text = "مراجعة الورش المعلقة وإشارات الاحتيال",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color(0xFF7F1D1D)
                                    )
                                }
                            }
                            Button(
                                onClick = onNavigateToAdminFraud,
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626)),
                                modifier = Modifier.testTag("open_admin_fraud_button")
                            ) {
                                Text("إدارة", fontSize = 12.sp)
                            }
                        }
                    }
                }
            }

            // Recent Registered Vehicles List
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = AppStrings.recentVehicles(lang),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${recentVehicles.size}",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (recentVehicles.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                        )
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = if (lang == AppLanguage.ARABIC)
                                    "لا توجد مركبات مسجلة بعد. استخدم نموذج VIN أعلاه لتسجيل أول مركبة."
                                else
                                    "Aucun véhicule enregistré pour le moment. Décodez un VIN ci-dessus pour commencer.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            } else {
                items(recentVehicles, key = { it.vin }) { veh ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onNavigateToVehicle(veh.vin) }
                            .testTag("vehicle_card_${veh.vin}"),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        ),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(44.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFF0F172A).copy(alpha = 0.08f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.DirectionsCar,
                                        contentDescription = null,
                                        tint = Color(0xFF0F172A)
                                    )
                                }
                                Column {
                                    Text(
                                        text = "${veh.year} ${veh.make} ${veh.model}",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = veh.vin,
                                        style = MaterialTheme.typography.bodySmall,
                                        fontFamily = FontFamily.Monospace,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    if (veh.latestOdometerKm > 0) {
                                        Text(
                                            text = "${veh.latestOdometerKm} km",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.SemiBold,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }
                            }

                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                contentDescription = "View",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }

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
private fun SpecRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold
        )
    }
}
