package com.example.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PrivacyTip
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.repository.VehicleRepository
import com.example.ui.localization.AppLanguage
import com.example.ui.localization.LocalLanguageManager
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrivacyPolicyScreen(
    repository: VehicleRepository,
    onNavigateBack: () -> Unit
) {
    val languageManager = LocalLanguageManager.current
    val lang = languageManager.currentLanguage
    val isAr = lang == AppLanguage.ARABIC
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    var selectedTab by remember { mutableIntStateOf(0) } // 0: Privacy, 1: Terms, 2: Consent & Deletion
    var showDeleteDialog by remember { mutableStateOf(false) }
    var deleteReason by remember { mutableStateOf("") }
    var isSubmittingDelete by remember { mutableStateOf(false) }
    var deleteSuccessId by remember { mutableStateOf<String?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Default.Security, contentDescription = null, tint = Color(0xFF10B981))
                        Text(
                            text = if (isAr) "سياسة الخصوصية والشروط" else "Politique de confidentialité & Conditions",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
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
            TabRow(selectedTabIndex = selectedTab) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text(if (isAr) "الخصوصية" else "Confidentialité") }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text(if (isAr) "الشروط" else "Conditions") }
                )
                Tab(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    text = { Text(if (isAr) "الموافقة والبيانات" else "Consentement") }
                )
            }

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                when (selectedTab) {
                    0 -> {
                        item {
                            PrivacyPolicyContent(isAr = isAr)
                        }
                    }
                    1 -> {
                        item {
                            TermsOfUseContent(isAr = isAr)
                        }
                    }
                    2 -> {
                        item {
                            ConsentAndDataDeletionSection(
                                isAr = isAr,
                                onRequestDelete = { showDeleteDialog = true },
                                deleteSuccessId = deleteSuccessId
                            )
                        }
                    }
                }
            }
        }
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { if (!isSubmittingDelete) showDeleteDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                    Text(if (isAr) "طلب حذف بيانات حسابي" else "Demander la suppression de mes données")
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = if (isAr)
                            "سيتم إرسال طلب حذف بياناتك الشخصية والسجلات المرتبطة بك إلى قائمة انتظار الإدارة. لن يتم حذف التقارير العمومية الصادرة سابقاً لحماية نزاهة المشتريين."
                        else
                            "Votre demande de suppression sera envoyée à la file d'attente d'administration. Les rapports publics déjà générés restent archivés pour garantir la transparence des transactions.",
                        style = MaterialTheme.typography.bodyMedium
                    )

                    OutlinedTextField(
                        value = deleteReason,
                        onValueChange = { deleteReason = it },
                        label = { Text(if (isAr) "سبب الحذف (اختياري)" else "Raison (optionnel)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        coroutineScope.launch {
                            isSubmittingDelete = true
                            val res = repository.requestDataDeletion(deleteReason)
                            isSubmittingDelete = false
                            showDeleteDialog = false
                            if (res.isSuccess) {
                                deleteSuccessId = res.getOrNull()
                                snackbarHostState.showSnackbar(
                                    if (isAr) "تم إرسال طلب حذف البيانات بنجاح للإدارة" else "Demande de suppression envoyée avec succès"
                                )
                            } else {
                                snackbarHostState.showSnackbar(
                                    res.exceptionOrNull()?.localizedMessage ?: "Failed to submit request"
                                )
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    enabled = !isSubmittingDelete,
                    modifier = Modifier.testTag("confirm_delete_button")
                ) {
                    if (isSubmittingDelete) {
                        CircularProgressIndicator(modifier = Modifier.size(18.dp), color = Color.White)
                    } else {
                        Text(if (isAr) "تأكيد الطلب" else "Confirmer la demande")
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }, enabled = !isSubmittingDelete) {
                    Text(if (isAr) "إلغاء" else "Annuler")
                }
            }
        )
    }
}

@Composable
private fun PrivacyPolicyContent(isAr: Boolean) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Default.Shield, contentDescription = null, tint = Color(0xFF10B981))
                Text(
                    text = if (isAr) "حماية البيانات الشخصية" else "Protection des données personnelles",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            Text(
                text = if (isAr)
                    "نحن في تطبيق 'جواز السيارة' (Car Passport) نلتزم بأعلى معايير حماية الخصوصية والأمان وفق القوانين الجزائرية والدولية لحماية المعطيات ذات الطابع الشخصي."
                else
                    "Dans l'application 'Car Passport' (جواز السيارة), nous nous engageons à respecter les normes les plus strictes en matière de protection des données personnelles.",
                style = MaterialTheme.typography.bodyMedium
            )

            HorizontalDivider()

            PrivacyBullet(
                title = if (isAr) "1. عدم تخزين أرقام الهواتف الصريحة" else "1. Aucun stockage de numéros en clair",
                desc = if (isAr)
                    "يتم تشفير كافة أرقام هواتف الزبائن باستخدام دالة SHA-256 فور إدخالها. لا يتم حفظ أي رقم هاتف بصيغة نصية في قواعد البيانات أو السجلات العمومية."
                else
                    "Tous les numéros de téléphone sont hachés via SHA-256 dès la saisie. Aucun numéro n'est conservé en clair dans nos bases de données."
            )

            PrivacyBullet(
                title = if (isAr) "2. حجب بيانات الهوية في التقارير العمومية" else "2. Masquage de l'identité dans les rapports",
                desc = if (isAr)
                    "التقارير العامة المتاحة عبر QR Code لا تظهر اسم المالك، رقم هاتفه، أو عنوانه. يتم قناع رقم الشاسي (VIN) بظهور أول 3 وأخر 4 أرقام فقط."
                else
                    "Les rapports publics partagés ne révèlent ni le nom, ni le téléphone du propriétaire. Le numéro VIN est partiellement masqué."
            )

            PrivacyBullet(
                title = if (isAr) "3. معالجة الصور عبر الذكاء الاصطناعي" else "3. Traitement des photos par IA",
                desc = if (isAr)
                    "تُستخدم صور الفحص البصري لحساب تقييم حالة السيارة بواسطة نماذج Gemini الفضائية عبر نظام App Check الآمن دون مشاركتها مع أطراف خارجية."
                else
                    "Les photos sont analysées de manière sécurisée par l'IA Gemini sous vérification Firebase App Check sans vente à des tiers."
            )
        }
    }
}

@Composable
private fun TermsOfUseContent(isAr: Boolean) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Default.PrivacyTip, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Text(
                    text = if (isAr) "شروط الاستخدام والمسؤولية" else "Conditions d'utilisation",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            Text(
                text = if (isAr)
                    "باستخدامك لتطبيق جواز السيارة، فإنك توافق على الشروط والأحكام التالية التي تضمن مصداقية ونزاهة السجلات:"
                else
                    "En utilisant Car Passport, vous acceptez les conditions d'utilisation garantissant la fiabilité de l'historique :",
                style = MaterialTheme.typography.bodyMedium
            )

            HorizontalDivider()

            PrivacyBullet(
                title = if (isAr) "1. طبيعة تقرير الفحص بالذكاء الاصطناعي" else "1. Nature du rapport d'inspection IA",
                desc = if (isAr)
                    "تقرير التقييم البصري الصادر عن الذكاء الاصطناعي هو تقدير استرشادي مبني على الصور المرفقة، ولا يغني بأي حال عن الفحص الميكانيكي المباشر لدى خبير متكفل."
                else
                    "Le rapport d'inspection IA est une estimation indicative basée sur les photos et ne remplace pas une expertise mécanique physique."
            )

            PrivacyBullet(
                title = if (isAr) "2. عدم إمكانية تعديل السجل التاريخي" else "2. Immuabilité de l'historique",
                desc = if (isAr)
                    "أحداث الصيانة المكتوبة محصنة برابط التشفير SHA-256 ولا يمكن تعديلها أو حذفها بعد الاعتماد. أي تصحيح يتم عبر تسجيل حدث تصحيحي جديد."
                else
                    "Les événements enregistrés sont scellés cryptographiquement via SHA-256. Toute correction fait l'objet d'un nouvel événement."
            )

            PrivacyBullet(
                title = if (isAr) "3. الاستخدام العادل ومكافحة الاحتيال" else "3. Usage équitable et anti-fraude",
                desc = if (isAr)
                    "يحظر استخدام التطبيق لإدخال قراءات عداد مزيفة أو وثائق غير صحيحة. يحتفظ المشرفون بحق تعليق حسابات الورش المخالفة وإلغاء اعتمادها."
                else
                    "Toute falsification des données de kilométrage entraînera la suspension immédiate du compte atelier et le signalement."
            )
        }
    }
}

@Composable
private fun ConsentAndDataDeletionSection(
    isAr: Boolean,
    onRequestDelete: () -> Unit,
    deleteSuccessId: String?
) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f))
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Default.CheckCircle, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Text(
                        text = if (isAr) "شروط الموافقة عند التسجيل" else "Consentement lors de l'inscription",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                Text(
                    text = if (isAr)
                        "عند إنشاء حسابك أو تسجيل عمليات الصيانة، فإنك تمنح الموافقة على:"
                    else
                        "En créant votre compte ou en enregistrant des services, vous consentez à :",
                    style = MaterialTheme.typography.bodyMedium
                )

                Text(
                    text = if (isAr)
                        "• رفع ومعالجة صور الهيكل وعداد الكيلومترات لتقييم حالة المركبة.\n• استخدام رقم الهاتف بشكل مشفر (Hash) لإرسال تأكيدات الخدمات عبر WhatsApp.\n• حفظ بيانات الورشة التجارية (السجل التجاري) للتحقق من هوية الشريك."
                    else
                        "• Téléchargement et traitement des photos du véhicule et compteur.\n• Utilisation hachée du numéro de téléphone pour les confirmations WhatsApp.\n• Conservation des données de registre du commerce pour la vérification atelier.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.2f))
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                    Text(
                        text = if (isAr) "طلب حذف البيانات (Right to be forgotten)" else "Droit à l'oubli & Suppression",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.error
                    )
                }

                Text(
                    text = if (isAr)
                        "يحق لك في أي وقت تقديم طلب لحذف بياناتك الشخصية وحسابك من النظام."
                    else
                        "Vous pouvez demander à tout moment la suppression de vos données personnelles et de votre compte.",
                    style = MaterialTheme.typography.bodyMedium
                )

                if (deleteSuccessId != null) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFF10B981).copy(alpha = 0.15f), RoundedCornerShape(8.dp))
                            .padding(12.dp)
                    ) {
                        Text(
                            text = if (isAr)
                                "تم تسجيل طلبك بنجاح برقم: $deleteSuccessId. سيتم معالجته من طرف المشرفين."
                            else
                                "Demande enregistrée avec l'ID: $deleteSuccessId. Elle sera traitée par l'administrateur.",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF047857),
                            fontWeight = FontWeight.Bold
                        )
                    }
                } else {
                    Button(
                        onClick = onRequestDelete,
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                        modifier = Modifier.testTag("request_data_deletion_button")
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(if (isAr) "إرسال طلب حذف بياناتي" else "Demander la suppression de mes données")
                    }
                }
            }
        }
    }
}

@Composable
private fun PrivacyBullet(title: String, desc: String) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(text = title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
        Text(text = desc, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
