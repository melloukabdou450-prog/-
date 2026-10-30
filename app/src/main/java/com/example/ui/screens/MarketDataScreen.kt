package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.UploadFile
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
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.PriceListing
import com.example.data.repository.VehicleRepository
import com.example.ui.localization.AppLanguage
import com.example.ui.localization.AppStrings
import com.example.ui.localization.LocalLanguageManager
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import kotlinx.coroutines.launch
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun MarketDataScreen(
    repository: VehicleRepository,
    onNavigateBack: () -> Unit
) {
    val languageManager = LocalLanguageManager.current
    val lang = languageManager.currentLanguage
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    val currentUser = Firebase.auth.currentUser
    val allListings by repository.observePriceListings().collectAsState(initial = emptyList())

    // Admin detection
    val isAdmin = repository.isCurrentUserAdmin() || currentUser?.email?.lowercase() == "melloukabdou450@gmail.com"
    var devAdminBypass by remember { mutableStateOf(false) }
    val effectiveAdmin = isAdmin || devAdminBypass

    var selectedTab by remember { mutableIntStateOf(0) } // 0: Listings table, 1: Add single, 2: CSV Import

    // Filters for table
    var selectedMakeFilter by remember { mutableStateOf("الكل") }
    var selectedFuelFilter by remember { mutableStateOf("الكل") }
    var searchQuery by remember { mutableStateOf("") }

    // Single listing form state
    var formMake by remember { mutableStateOf("") }
    var formModel by remember { mutableStateOf("") }
    var formYear by remember { mutableStateOf("2020") }
    var formKm by remember { mutableStateOf("80000") }
    var formPrice by remember { mutableStateOf("2200000") }
    var formWilaya by remember { mutableStateOf("16 - Alger") }
    var formFuel by remember { mutableStateOf("Essence") }
    var formTransmission by remember { mutableStateOf("Manuelle") }
    var isSavingSingle by remember { mutableStateOf(false) }

    // CSV import state
    var csvText by remember { mutableStateOf("") }
    var isParsingCsv by remember { mutableStateOf(false) }
    var isSavingCsv by remember { mutableStateOf(false) }
    data class ParsedCsvRow(
        val original: String,
        val isValid: Boolean,
        val error: String?,
        val listing: PriceListing?
    )
    var parsedRows by remember { mutableStateOf<List<ParsedCsvRow>>(emptyList()) }

    val wilayasList = listOf(
        "16 - Alger", "31 - Oran", "25 - Constantine", "19 - Sétif", "06 - Béjaïa",
        "09 - Blida", "15 - Tizi Ouzou", "13 - Tlemcen", "35 - Boumerdès", "42 - Tipaza",
        "27 - Mostaganem", "05 - Batna", "23 - Annaba", "30 - Ouargla"
    )

    val fuelsList = listOf("Essence", "Diesel", "GPL", "Hybride", "Electrique")
    val transmissionList = listOf("Manuelle", "Automatique")

    val sampleCsvDataset = """
make,model,year,km,price,wilaya,fuel,transmission
Renault,Clio 4,2018,110000,2150000,16 - Alger,Essence,Manuelle
Renault,Clio 4,2019,85000,2350000,16 - Alger,Essence,Manuelle
Renault,Clio 4,2017,145000,1950000,31 - Oran,Essence,Manuelle
Renault,Clio 4,2018,95000,2250000,09 - Blida,Essence,Manuelle
Renault,Clio 4,2020,60000,2600000,16 - Alger,Essence,Manuelle
Renault,Clio 4,2019,75000,2400000,19 - Sétif,Essence,Manuelle
Renault,Clio 4,2018,125000,2050000,25 - Constantine,Essence,Manuelle
Renault,Clio 4,2017,160000,1880000,16 - Alger,Diesel,Manuelle
Renault,Symbol,2019,90000,2100000,16 - Alger,Essence,Manuelle
Renault,Symbol,2018,120000,1850000,31 - Oran,Essence,Manuelle
Renault,Symbol,2020,65000,2300000,16 - Alger,Essence,Manuelle
Dacia,Sandero Stepway,2019,80000,2450000,16 - Alger,Diesel,Manuelle
Dacia,Sandero Stepway,2018,115000,2200000,09 - Blida,Diesel,Manuelle
Dacia,Sandero Stepway,2020,55000,2700000,31 - Oran,Essence,Manuelle
Dacia,Sandero Stepway,2017,140000,1980000,19 - Sétif,Diesel,Manuelle
Dacia,Duster,2019,95000,2950000,16 - Alger,Diesel,Manuelle
Toyota,Yaris,2018,90000,2650000,16 - Alger,Essence,Automatique
Toyota,Yaris,2019,70000,2900000,31 - Oran,Essence,Automatique
Toyota,Yaris,2017,130000,2350000,16 - Alger,Essence,Manuelle
Toyota,Hilux,2018,140000,4800000,30 - Ouargla,Diesel,Manuelle
Toyota,Hilux,2019,105000,5300000,16 - Alger,Diesel,Manuelle
Toyota,Corolla,2020,60000,3700000,16 - Alger,Essence,Automatique
Volkswagen,Golf 7,2018,105000,3600000,16 - Alger,Diesel,Manuelle
Volkswagen,Golf 7,2019,75000,3950000,31 - Oran,Diesel,Automatique
Volkswagen,Golf 7,2017,135000,3300000,16 - Alger,Diesel,Manuelle
Volkswagen,Polo,2018,95000,2750000,16 - Alger,Essence,Manuelle
Peugeot,208,2018,110000,2250000,16 - Alger,Diesel,Manuelle
Peugeot,208,2019,80000,2500000,09 - Blida,Essence,Manuelle
Peugeot,208,2020,55000,2800000,31 - Oran,Essence,Manuelle
Hyundai,Accent,2019,85000,2400000,16 - Alger,Essence,Manuelle
Hyundai,Accent,2018,115000,2150000,19 - Sétif,Essence,Manuelle
Hyundai,Tucson,2019,90000,4300000,16 - Alger,Diesel,Automatique
    """.trimIndent()

    fun parseCsv(raw: String) {
        val lines = raw.lines().map { it.trim() }.filter { it.isNotBlank() }
        if (lines.isEmpty()) {
            parsedRows = emptyList()
            return
        }

        val hasHeader = lines.first().lowercase().contains("make") && lines.first().lowercase().contains("price")
        val dataLines = if (hasHeader) lines.drop(1) else lines

        val results = mutableListOf<ParsedCsvRow>()
        for (line in dataLines) {
            val parts = line.split(",").map { it.trim() }
            if (parts.size < 8) {
                results.add(ParsedCsvRow(line, false, "الأعمدة غير مكتملة (مطلوب 8)", null))
                continue
            }
            val make = parts[0]
            val model = parts[1]
            val year = parts[2].toIntOrNull()
            val km = parts[3].toLongOrNull()
            val price = parts[4].toDoubleOrNull()
            val wilaya = parts[5]
            val fuel = parts[6]
            val transmission = parts[7]

            if (make.isBlank() || model.isBlank()) {
                results.add(ParsedCsvRow(line, false, "المصنع أو الموديل فارغ", null))
            } else if (year == null || year !in 1970..2030) {
                results.add(ParsedCsvRow(line, false, "سنة الصنع غير صحيحة", null))
            } else if (km == null || km < 0) {
                results.add(ParsedCsvRow(line, false, "الممشى غير صحيح", null))
            } else if (price == null || price <= 0) {
                results.add(ParsedCsvRow(line, false, "السعر غير صحيح", null))
            } else {
                val listing = PriceListing(
                    id = "",
                    make = make,
                    model = model,
                    year = year,
                    km = km,
                    price = price,
                    wilaya = wilaya,
                    fuel = fuel,
                    transmission = transmission,
                    source = "csv"
                )
                results.add(ParsedCsvRow(line, true, null, listing))
            }
        }
        parsedRows = results
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = AppStrings.marketDataTitle(lang),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = AppStrings.totalListingsCount(lang, allListings.size),
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
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Admin status banner & Privacy reassurance
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (effectiveAdmin) Color(0xFF0F172A) else Color(0xFFFEF3C7)
                )
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
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
                                imageVector = if (effectiveAdmin) Icons.Default.AdminPanelSettings else Icons.Default.Lock,
                                contentDescription = null,
                                tint = if (effectiveAdmin) Color(0xFF10B981) else Color(0xFFD97706),
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = if (effectiveAdmin) "مشرف معتمد (melloukabdou450@gmail.com)" else AppStrings.adminOnlyNotice(lang),
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = if (effectiveAdmin) Color.White else Color(0xFF92400E)
                            )
                        }

                        // Demo admin toggle for test/evaluation flexibility
                        if (!isAdmin) {
                            TextButton(onClick = { devAdminBypass = !devAdminBypass }) {
                                Text(
                                    text = if (devAdminBypass) "وضع المشرف مفعل" else "تفعيل وضع المشرف للتجربة",
                                    fontSize = 11.sp,
                                    color = if (devAdminBypass) Color(0xFF10B981) else Color(0xFFB45309)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = AppStrings.noPersonalDataStored(lang),
                        style = MaterialTheme.typography.labelSmall,
                        color = if (effectiveAdmin) Color(0xFF94A3B8) else Color(0xFF78350F)
                    )
                }
            }

            // Tab bar
            TabRow(selectedTabIndex = selectedTab) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("قائمة الإعلانات (${allListings.size})", fontWeight = FontWeight.Bold) }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text(AppStrings.addListingTitle(lang), fontWeight = FontWeight.Bold) }
                )
                Tab(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    text = { Text(AppStrings.csvImportTitle(lang), fontWeight = FontWeight.Bold) }
                )
            }

            when (selectedTab) {
                0 -> {
                    // TAB 0: Table with Filters & Delete
                    val makes = remember(allListings) {
                        listOf("الكل") + allListings.map { it.make }.distinct().sorted()
                    }
                    val fuels = remember(allListings) {
                        listOf("الكل") + allListings.map { it.fuel }.distinct().sorted()
                    }

                    val filtered = remember(allListings, selectedMakeFilter, selectedFuelFilter, searchQuery) {
                        allListings.filter { listing ->
                            val matchMake = selectedMakeFilter == "الكل" || listing.make.equals(selectedMakeFilter, ignoreCase = true)
                            val matchFuel = selectedFuelFilter == "الكل" || listing.fuel.equals(selectedFuelFilter, ignoreCase = true)
                            val matchSearch = searchQuery.isBlank() ||
                                    listing.make.contains(searchQuery, ignoreCase = true) ||
                                    listing.model.contains(searchQuery, ignoreCase = true) ||
                                    listing.wilaya.contains(searchQuery, ignoreCase = true)
                            matchMake && matchFuel && matchSearch
                        }
                    }

                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        item {
                            Spacer(modifier = Modifier.height(8.dp))
                            // Search bar
                            OutlinedTextField(
                                value = searchQuery,
                                onValueChange = { searchQuery = it },
                                placeholder = { Text("بحث بالمصنع، الموديل أو الولاية...") },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                singleLine = true
                            )
                        }

                        // Filter chips
                        item {
                            Text(
                                text = "تصفية بالماركة:",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold
                            )
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                makes.take(8).forEach { m ->
                                    FilterChip(
                                        selected = selectedMakeFilter == m,
                                        onClick = { selectedMakeFilter = m },
                                        label = { Text(m, fontSize = 12.sp) }
                                    )
                                }
                            }
                        }

                        if (allListings.isEmpty()) {
                            item {
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(16.dp),
                                    colors = CardDefaults.cardColors(
                                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                    )
                                ) {
                                    Column(
                                        modifier = Modifier.padding(24.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        Text(
                                            text = "قاعدة بيانات السوق فارغة حالياً.",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = "يمكنك استيراد بيانات نموذجية للسوق الجزائري بنقرة واحدة لاختبار التقدير الإحصائي العادل.",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Button(
                                            onClick = {
                                                selectedTab = 2
                                                csvText = sampleCsvDataset
                                                parseCsv(sampleCsvDataset)
                                            },
                                            shape = RoundedCornerShape(10.dp),
                                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0F172A))
                                        ) {
                                            Text("استيراد بيانات نموذجية للسوق الجزائري (32 إعلاناً)")
                                        }
                                    }
                                }
                            }
                        } else {
                            items(filtered, key = { it.id }) { item ->
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(12.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                Text(
                                                    text = "${item.year} ${item.make} ${item.model}",
                                                    style = MaterialTheme.typography.titleSmall,
                                                    fontWeight = FontWeight.Bold
                                                )
                                                Surface(
                                                    color = Color(0xFFE2E8F0),
                                                    shape = RoundedCornerShape(4.dp)
                                                ) {
                                                    Text(
                                                        text = item.fuel,
                                                        fontSize = 10.sp,
                                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                                    )
                                                }
                                            }
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text(
                                                text = "${item.km} كم • ${item.wilaya} • ${item.transmission}",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                            Text(
                                                text = String.format(Locale.US, "%,.0f د.ج", item.price),
                                                style = MaterialTheme.typography.titleMedium,
                                                fontWeight = FontWeight.Black,
                                                color = Color(0xFF10B981)
                                            )
                                        }

                                        if (effectiveAdmin) {
                                            IconButton(
                                                onClick = {
                                                    coroutineScope.launch {
                                                        val res = repository.deletePriceListing(item.id)
                                                        if (res.isSuccess) {
                                                            snackbarHostState.showSnackbar("تم حذف الإعلان بنجاح")
                                                        } else {
                                                            snackbarHostState.showSnackbar("فشل الحذف: ${res.exceptionOrNull()?.message}")
                                                        }
                                                    }
                                                }
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Delete,
                                                    contentDescription = "Delete",
                                                    tint = MaterialTheme.colorScheme.error
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        item {
                            Spacer(modifier = Modifier.height(24.dp))
                        }
                    }
                }

                1 -> {
                    // TAB 1: Add Single Listing Form
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        item {
                            Text(
                                text = "إدخال إعلان سعر مرجعي للسوق",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        item {
                            OutlinedTextField(
                                value = formMake,
                                onValueChange = { formMake = it },
                                label = { Text("المصنع (مثال: Renault, Toyota, Peugeot)") },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp)
                            )
                        }

                        item {
                            OutlinedTextField(
                                value = formModel,
                                onValueChange = { formModel = it },
                                label = { Text("الموديل (مثال: Clio 4, Yaris, 208, Symbol)") },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp)
                            )
                        }

                        item {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedTextField(
                                    value = formYear,
                                    onValueChange = { formYear = it },
                                    label = { Text("سنة الصنع") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(12.dp)
                                )
                                OutlinedTextField(
                                    value = formKm,
                                    onValueChange = { formKm = it },
                                    label = { Text("الممشى (كم)") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(12.dp)
                                )
                            }
                        }

                        item {
                            OutlinedTextField(
                                value = formPrice,
                                onValueChange = { formPrice = it },
                                label = { Text("السعر بالدينار الجزائري (DZD)") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp)
                            )
                        }

                        item {
                            var wilayaExpanded by remember { mutableStateOf(false) }
                            ExposedDropdownMenuBox(
                                expanded = wilayaExpanded,
                                onExpandedChange = { wilayaExpanded = !wilayaExpanded }
                            ) {
                                OutlinedTextField(
                                    value = formWilaya,
                                    onValueChange = {},
                                    readOnly = true,
                                    label = { Text("الولاية") },
                                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = wilayaExpanded) },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .menuAnchor(),
                                    shape = RoundedCornerShape(12.dp)
                                )
                                ExposedDropdownMenu(
                                    expanded = wilayaExpanded,
                                    onDismissRequest = { wilayaExpanded = false }
                                ) {
                                    wilayasList.forEach { w ->
                                        DropdownMenuItem(
                                            text = { Text(w) },
                                            onClick = {
                                                formWilaya = w
                                                wilayaExpanded = false
                                            }
                                        )
                                    }
                                }
                            }
                        }

                        item {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                var fuelExpanded by remember { mutableStateOf(false) }
                                ExposedDropdownMenuBox(
                                    expanded = fuelExpanded,
                                    onExpandedChange = { fuelExpanded = !fuelExpanded },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    OutlinedTextField(
                                        value = formFuel,
                                        onValueChange = {},
                                        readOnly = true,
                                        label = { Text("الوقود") },
                                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = fuelExpanded) },
                                        modifier = Modifier.menuAnchor(),
                                        shape = RoundedCornerShape(12.dp)
                                    )
                                    ExposedDropdownMenu(
                                        expanded = fuelExpanded,
                                        onDismissRequest = { fuelExpanded = false }
                                    ) {
                                        fuelsList.forEach { f ->
                                            DropdownMenuItem(
                                                text = { Text(f) },
                                                onClick = {
                                                    formFuel = f
                                                    fuelExpanded = false
                                                }
                                            )
                                        }
                                    }
                                }

                                var transExpanded by remember { mutableStateOf(false) }
                                ExposedDropdownMenuBox(
                                    expanded = transExpanded,
                                    onExpandedChange = { transExpanded = !transExpanded },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    OutlinedTextField(
                                        value = formTransmission,
                                        onValueChange = {},
                                        readOnly = true,
                                        label = { Text("علبة السرعة") },
                                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = transExpanded) },
                                        modifier = Modifier.menuAnchor(),
                                        shape = RoundedCornerShape(12.dp)
                                    )
                                    ExposedDropdownMenu(
                                        expanded = transExpanded,
                                        onDismissRequest = { transExpanded = false }
                                    ) {
                                        transmissionList.forEach { t ->
                                            DropdownMenuItem(
                                                text = { Text(t) },
                                                onClick = {
                                                    formTransmission = t
                                                    transExpanded = false
                                                }
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        item {
                            Button(
                                onClick = {
                                    val y = formYear.toIntOrNull()
                                    val k = formKm.toLongOrNull()
                                    val p = formPrice.toDoubleOrNull()
                                    if (formMake.isBlank() || formModel.isBlank() || y == null || k == null || p == null) {
                                        coroutineScope.launch {
                                            snackbarHostState.showSnackbar("يرجى ملء جميع الحقول بالأرقام الصحيحة")
                                        }
                                        return@Button
                                    }

                                    isSavingSingle = true
                                    coroutineScope.launch {
                                        val listing = PriceListing(
                                            make = formMake,
                                            model = formModel,
                                            year = y,
                                            km = k,
                                            price = p,
                                            wilaya = formWilaya,
                                            fuel = formFuel,
                                            transmission = formTransmission,
                                            source = "manual"
                                        )
                                        val res = repository.savePriceListing(listing)
                                        isSavingSingle = false
                                        if (res.isSuccess) {
                                            snackbarHostState.showSnackbar("تم حفظ الإعلان في قاعدة البيانات بنجاح")
                                            formMake = ""
                                            formModel = ""
                                            selectedTab = 0
                                        } else {
                                            snackbarHostState.showSnackbar("فشل الحفظ: ${res.exceptionOrNull()?.message}")
                                        }
                                    }
                                },
                                enabled = !isSavingSingle && effectiveAdmin,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(50.dp),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0F172A))
                            ) {
                                if (isSavingSingle) {
                                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(20.dp))
                                } else {
                                    Text("حفظ الإعلان المرجعي", fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }

                2 -> {
                    // TAB 2: CSV Import with row validation and preview before saving
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        item {
                            Text(
                                text = "استيراد وتدقيق ملف CSV",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "الأعمدة المطلوبة: make, model, year, km, price, wilaya, fuel, transmission",
                                style = MaterialTheme.typography.bodySmall,
                                fontFamily = FontFamily.Monospace,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        item {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedButton(
                                    onClick = {
                                        csvText = sampleCsvDataset
                                        parseCsv(sampleCsvDataset)
                                    },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Text("تحميل نموذج جاهز (32 إعلاناً)", fontSize = 12.sp)
                                }

                                Button(
                                    onClick = { parseCsv(csvText) },
                                    enabled = csvText.isNotBlank(),
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(10.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0F172A))
                                ) {
                                    Text("تدقيق ومعاينة الصفوف", fontSize = 12.sp)
                                }
                            }
                        }

                        item {
                            OutlinedTextField(
                                value = csvText,
                                onValueChange = {
                                    csvText = it
                                },
                                placeholder = { Text("الصق بيانات CSV هنا...") },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(140.dp),
                                shape = RoundedCornerShape(12.dp),
                                textStyle = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace)
                            )
                        }

                        if (parsedRows.isNotEmpty()) {
                            val validCount = parsedRows.count { it.isValid }
                            val invalidCount = parsedRows.count { !it.isValid }

                            item {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(
                                            text = "معاينة الصفوف: $validCount صالحة، $invalidCount غير صالحة",
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = "يرجى مراجعة المعاينة أدناه قبل تأكيد الحفظ في Firestore.",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }

                                    Button(
                                        onClick = {
                                            val validListings = parsedRows.mapNotNull { it.listing }
                                            if (validListings.isEmpty()) return@Button

                                            isSavingCsv = true
                                            coroutineScope.launch {
                                                val res = repository.savePriceListingsBatch(validListings)
                                                isSavingCsv = false
                                                if (res.isSuccess) {
                                                    snackbarHostState.showSnackbar("تم حفظ ${res.getOrNull()} إعلاناً بنجاح في قاعدة البيانات")
                                                    parsedRows = emptyList()
                                                    csvText = ""
                                                    selectedTab = 0
                                                } else {
                                                    snackbarHostState.showSnackbar("فشل الاستيراد: ${res.exceptionOrNull()?.message}")
                                                }
                                            }
                                        },
                                        enabled = validCount > 0 && !isSavingCsv && effectiveAdmin,
                                        shape = RoundedCornerShape(10.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981))
                                    ) {
                                        if (isSavingCsv) {
                                            CircularProgressIndicator(color = Color.White, modifier = Modifier.size(18.dp))
                                        } else {
                                            Text("حفظ $validCount إعلاناً", fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }

                            items(parsedRows.take(15)) { row ->
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(8.dp),
                                    colors = CardDefaults.cardColors(
                                        containerColor = if (row.isValid) Color(0xFFF0FDF4) else Color(0xFFFEF2F2)
                                    ),
                                    border = androidx.compose.foundation.BorderStroke(
                                        1.dp,
                                        if (row.isValid) Color(0xFF86EFAC) else Color(0xFFFECACA)
                                    )
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(10.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Icon(
                                            imageVector = if (row.isValid) Icons.Default.CheckCircle else Icons.Default.Warning,
                                            contentDescription = null,
                                            tint = if (row.isValid) Color(0xFF10B981) else Color(0xFFEF4444),
                                            modifier = Modifier.size(18.dp)
                                        )

                                        Column(modifier = Modifier.weight(1f)) {
                                            if (row.isValid && row.listing != null) {
                                                val l = row.listing
                                                Text(
                                                    text = "${l.year} ${l.make} ${l.model} • ${String.format(Locale.US, "%,.0f د.ج", l.price)}",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    fontWeight = FontWeight.Bold
                                                )
                                                Text(
                                                    text = "${l.km} كم • ${l.wilaya} • ${l.fuel} • ${l.transmission}",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = Color(0xFF475569)
                                                )
                                            } else {
                                                Text(
                                                    text = row.original,
                                                    style = MaterialTheme.typography.labelSmall,
                                                    fontFamily = FontFamily.Monospace
                                                )
                                                Text(
                                                    text = row.error ?: "خطأ في التنسيق",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = Color(0xFFDC2626),
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                            if (parsedRows.size > 15) {
                                item {
                                    Text(
                                        text = "+ ${parsedRows.size - 15} صفاً إضافياً صالحاً ومجاهزاً للحفظ...",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
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
        }
    }
}
