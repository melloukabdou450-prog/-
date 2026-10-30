package com.example.ui.screens

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color as AndroidColor
import android.graphics.Paint
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.ai.AiInspectionService
import com.example.data.ai.InspectionStepInfo
import com.example.data.model.Vehicle
import com.example.data.repository.VehicleRepository
import com.example.ui.localization.AppLanguage
import com.example.ui.localization.AppStrings
import com.example.ui.localization.LocalLanguageManager
import com.example.util.ImageCompressionUtil
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InspectScreen(
    vin: String,
    vehicle: Vehicle?,
    repository: VehicleRepository,
    onBack: () -> Unit,
    onInspectionCompleted: (inspectionId: String) -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler(onBack = onBack)

    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val langManager = LocalLanguageManager.current
    val lang = langManager.currentLanguage

    val steps = remember { AiInspectionService.REQUIRED_STEPS }
    var currentStepIndex by remember { mutableIntStateOf(0) }

    // Map of step index to loaded Bitmap
    val photosMap = remember { mutableStateMapOf<Int, Bitmap>() }
    // Map of step index to low-light / blur flag
    val qualityAlertMap = remember { mutableStateMapOf<Int, Boolean>() }

    var isAnalyzing by remember { mutableStateOf(false) }
    var analysisError by remember { mutableStateOf<String?>(null) }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            scope.launch {
                val compressed = ImageCompressionUtil.compressToMaxDimension(context, uri, 1280)
                if (compressed != null) {
                    val luminance = ImageCompressionUtil.estimateAverageLuminance(compressed)
                    qualityAlertMap[currentStepIndex] = (luminance < 35.0)
                    photosMap[currentStepIndex] = compressed

                    // Auto advance to next empty step if available
                    val nextEmpty = (0 until steps.size).firstOrNull { !photosMap.containsKey(it) }
                    if (nextEmpty != null) {
                        currentStepIndex = nextEmpty
                    }
                } else {
                    Toast.makeText(context, "فشل معالجة الصورة", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    val currentStep = steps[currentStepIndex]
    val currentBitmap = photosMap[currentStepIndex]
    val currentHasQualityAlert = qualityAlertMap[currentStepIndex] ?: false
    val completedCount = photosMap.size
    val allCompleted = completedCount == steps.size

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = AppStrings.aiInspectionTitle(lang),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "VIN: $vin",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.testTag("inspect_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "رجوع"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        modifier = modifier
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Vehicle Header Banner
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.DirectionsCar,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(28.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "${vehicle?.make ?: "مركبة"} ${vehicle?.model ?: ""} ${if ((vehicle?.year ?: 0) > 0) vehicle?.year else ""}".trim(),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "VIN: $vin",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Progress Header
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = AppStrings.guidedCaptureTitle(lang),
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Text(
                            text = "$completedCount / ${steps.size}",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    LinearProgressIndicator(
                        progress = { completedCount.toFloat() / steps.size.toFloat() },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp))
                    )
                }
            }

            // Step Selector Thumbnails
            Text(
                text = "الزوايا الـ 12 المطلوبة (اختر لتحديد الصورة):",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold
            )

            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(horizontal = 2.dp)
            ) {
                itemsIndexed(steps) { idx, step ->
                    val isSelected = idx == currentStepIndex
                    val hasPhoto = photosMap.containsKey(idx)

                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(
                                when {
                                    isSelected -> MaterialTheme.colorScheme.primary
                                    hasPhoto -> MaterialTheme.colorScheme.secondaryContainer
                                    else -> MaterialTheme.colorScheme.surfaceVariant
                                }
                            )
                            .border(
                                width = if (isSelected) 2.dp else 1.dp,
                                color = if (isSelected) MaterialTheme.colorScheme.primary else Color.LightGray,
                                shape = RoundedCornerShape(8.dp)
                            )
                            .clickable { currentStepIndex = idx }
                            .padding(4.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        if (hasPhoto) {
                            val bmp = photosMap[idx]
                            if (bmp != null) {
                                Image(
                                    bitmap = bmp.asImageBitmap(),
                                    contentDescription = step.titleArabic,
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .size(16.dp)
                                        .background(Color(0xFF2E7D32), CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(12.dp)
                                    )
                                }
                            }
                        } else {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Text(
                                    text = "${idx + 1}",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = step.id.take(4),
                                    style = MaterialTheme.typography.labelSmall,
                                    fontSize = 9.sp,
                                    color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }

            // Current Step Card
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = AppStrings.photoProgress(lang, currentStepIndex + 1, steps.size),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = if (lang == AppLanguage.ARABIC) currentStep.titleArabic else currentStep.titleFrench,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = if (lang == AppLanguage.ARABIC) currentStep.hintArabic else currentStep.hintFrench,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    // Image Display or Placeholder Box
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(240.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(12.dp))
                            .clickable {
                                photoPickerLauncher.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        if (currentBitmap != null) {
                            Image(
                                bitmap = currentBitmap.asImageBitmap(),
                                contentDescription = currentStep.titleArabic,
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Fit
                            )
                        } else {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center,
                                modifier = Modifier.padding(16.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AddAPhoto,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(48.dp)
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = AppStrings.takeOrPickPhoto(lang),
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = "اضغط لاختيار صورة واضحة من الجهاز",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    // Low-light / Quality Warning Alert
                    AnimatedVisibility(visible = currentHasQualityAlert) {
                        Surface(
                            color = MaterialTheme.colorScheme.errorContainer,
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 10.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Warning,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.error
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = AppStrings.photoBlurryOrDarkWarning(lang),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onErrorContainer
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Retake or Pick Button
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                photoPickerLauncher.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                            },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("pick_photo_button")
                        ) {
                            Icon(imageVector = Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(text = if (currentBitmap != null) AppStrings.retakePhoto(lang) else AppStrings.takeOrPickPhoto(lang))
                        }

                        if (currentBitmap != null) {
                            OutlinedButton(
                                onClick = {
                                    // User explicitly marks as blurry/dark and rejects
                                    photosMap.remove(currentStepIndex)
                                    qualityAlertMap.remove(currentStepIndex)
                                    photoPickerLauncher.launch(
                                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                    )
                                },
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                                modifier = Modifier.testTag("mark_blurry_button")
                            ) {
                                Text(text = "صورة غير واضحة (إلغاء)")
                            }
                        }
                    }
                }
            }

            // Quick Demo Assistant (Enables testing in emulator without 12 real car pictures)
            OutlinedButton(
                onClick = {
                    scope.launch {
                        val demoBitmaps = generateMockVehiclePhotos(steps)
                        demoBitmaps.forEachIndexed { i, bmp ->
                            photosMap[i] = bmp
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("fill_demo_photos_button")
            ) {
                Icon(imageVector = Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("تعبئة صور اختبارية نموذجية للفحص (Demo Mode)")
            }

            // Error display if any
            if (analysisError != null) {
                Surface(
                    color = MaterialTheme.colorScheme.errorContainer,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = analysisError ?: "",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }

            // Main Analyze Action Button
            Button(
                onClick = {
                    val auth = FirebaseAuth.getInstance()
                    val uid = auth.currentUser?.uid
                    if (uid == null) {
                        Toast.makeText(context, AppStrings.signInRequired(lang), Toast.LENGTH_LONG).show()
                        return@Button
                    }

                    isAnalyzing = true
                    analysisError = null

                    scope.launch {
                        try {
                            val orderedBitmaps = (0 until steps.size).mapNotNull { photosMap[it] }
                            if (orderedBitmaps.size < 12) {
                                analysisError = "يرجى إكمال جميع الصور الـ 12 قبل بدء التحليل."
                                isAnalyzing = false
                                return@launch
                            }

                            // 1. Run multimodal inspection via Gemini 3.8 Flash
                            val aiResult = AiInspectionService.performInspection(
                                vin = vin,
                                photos = orderedBitmaps,
                                userId = uid
                            )

                            aiResult.onSuccess { inspection ->
                                // 2. Save in Firestore and chain events
                                val saveResult = repository.saveInspection(vin, inspection)
                                saveResult.onSuccess { saved ->
                                    isAnalyzing = false
                                    onInspectionCompleted(saved.id)
                                }.onFailure { e ->
                                    isAnalyzing = false
                                    analysisError = "فشل حفظ نتيجة الفحص: ${e.localizedMessage}"
                                }
                            }.onFailure { e ->
                                isAnalyzing = false
                                analysisError = "تعذر إكمال الفحص بالذكاء الاصطناعي: ${e.localizedMessage}. يرجى المحاولة مرة أخرى."
                            }
                        } catch (e: Exception) {
                            isAnalyzing = false
                            analysisError = "حدث خطأ غير متوقع: ${e.localizedMessage}"
                        }
                    }
                },
                enabled = allCompleted && !isAnalyzing,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
                    .testTag("analyze_inspection_button")
            ) {
                if (isAnalyzing) {
                    CircularProgressIndicator(
                        color = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = AppStrings.analyzingPhotosWait(lang),
                        style = MaterialTheme.typography.labelMedium
                    )
                } else {
                    Icon(imageVector = Icons.Default.AutoAwesome, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = AppStrings.analyzePhotosBtn(lang),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

/**
 * Generates 12 representative synthetic vehicle photos for testing and demo purposes,
 * labelled clearly to satisfy multimodal input requirements.
 */
private suspend fun generateMockVehiclePhotos(steps: List<InspectionStepInfo>): List<Bitmap> = withContext(Dispatchers.Default) {
    steps.mapIndexed { idx, step ->
        val bmp = Bitmap.createBitmap(640, 480, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)
        val bgPaint = Paint().apply {
            color = when (idx) {
                0 -> AndroidColor.rgb(33, 150, 243)
                1 -> AndroidColor.rgb(63, 81, 181)
                2, 3 -> AndroidColor.rgb(0, 150, 136)
                4, 5 -> AndroidColor.rgb(76, 175, 80)
                6, 7 -> AndroidColor.rgb(139, 195, 74)
                8 -> AndroidColor.rgb(121, 85, 72)
                9 -> AndroidColor.rgb(38, 50, 56) // Dashboard
                10 -> AndroidColor.rgb(96, 125, 139)
                else -> AndroidColor.rgb(69, 90, 100)
            }
        }
        canvas.drawRect(0f, 0f, 640f, 480f, bgPaint)

        val textPaint = Paint().apply {
            color = AndroidColor.WHITE
            textSize = 34f
            isAntiAlias = true
            textAlign = Paint.Align.CENTER
        }

        val subPaint = Paint().apply {
            color = AndroidColor.LTGRAY
            textSize = 22f
            isAntiAlias = true
            textAlign = Paint.Align.CENTER
        }

        canvas.drawText("CAR PASSPORT - PHOTO ${idx + 1}/12", 320f, 180f, textPaint)
        canvas.drawText(step.id.uppercase(), 320f, 240f, textPaint)
        if (idx == 9) {
            canvas.drawText("ODOMETER: 124,500 KM", 320f, 300f, textPaint)
        } else {
            canvas.drawText(step.titleFrench, 320f, 300f, subPaint)
        }
        bmp
    }
}
