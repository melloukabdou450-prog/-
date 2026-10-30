package com.example.ui.localization

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection

enum class AppLanguage(val code: String, val displayName: String, val isRtl: Boolean) {
    ARABIC("ar", "العربية", true),
    FRENCH("fr", "Français", false)
}

class LanguageManager {
    var currentLanguage by mutableStateOf(AppLanguage.ARABIC)
        private set

    fun switchLanguage(lang: AppLanguage) {
        currentLanguage = lang
    }

    fun toggleLanguage() {
        currentLanguage = if (currentLanguage == AppLanguage.ARABIC) AppLanguage.FRENCH else AppLanguage.ARABIC
    }
}

val LocalLanguageManager = compositionLocalOf { LanguageManager() }

@Composable
fun ProvideAppLanguage(
    languageManager: LanguageManager,
    content: @Composable () -> Unit
) {
    val layoutDirection = if (languageManager.currentLanguage.isRtl) {
        LayoutDirection.Rtl
    } else {
        LayoutDirection.Ltr
    }

    CompositionLocalProvider(
        LocalLanguageManager provides languageManager,
        LocalLayoutDirection provides layoutDirection
    ) {
        content()
    }
}

object AppStrings {
    fun appTitle(lang: AppLanguage) = when (lang) {
        AppLanguage.ARABIC -> "جواز السيارة"
        AppLanguage.FRENCH -> "Car Passport"
    }

    fun appSubtitle(lang: AppLanguage) = when (lang) {
        AppLanguage.ARABIC -> "السجل الرقمي المشفر والموثق لتاريخ المركبة"
        AppLanguage.FRENCH -> "Passeport numérique vérifiable de l'historique du véhicule"
    }

    fun enterVin(lang: AppLanguage) = when (lang) {
        AppLanguage.ARABIC -> "رقم الشاسي (VIN)"
        AppLanguage.FRENCH -> "Numéro d'identification (VIN)"
    }

    fun vinPlaceholder(lang: AppLanguage) = when (lang) {
        AppLanguage.ARABIC -> "أدخل 17 حرفاً ورقماً (مثال: 1HGCR2F83HA...)"
        AppLanguage.FRENCH -> "Saisir 17 caractères (ex : 1HGCR2F83HA...)"
    }

    fun decodeVinBtn(lang: AppLanguage) = when (lang) {
        AppLanguage.ARABIC -> "فحص وتدقيق VIN"
        AppLanguage.FRENCH -> "Décoder & Vérifier le VIN"
    }

    fun quickSamples(lang: AppLanguage) = when (lang) {
        AppLanguage.ARABIC -> "نماذج سريعة للتجربة:"
        AppLanguage.FRENCH -> "Exemples rapides à tester :"
    }

    fun lowConfidenceWarning(lang: AppLanguage) = when (lang) {
        AppLanguage.ARABIC -> "⚠️ تنبيه: موثوقية بيانات منخفضة"
        AppLanguage.FRENCH -> "⚠️ Attention : Données partielles"
    }

    fun lowConfidenceDesc(lang: AppLanguage, fields: List<String>) = when (lang) {
        AppLanguage.ARABIC -> "تعذر التحقق التلقائي من بعض المواصفات الأساسية عبر NHTSA: ${fields.joinToString("، ")}. يرجى التأكد من صحة رقم VIN."
        AppLanguage.FRENCH -> "Certaines spécifications n'ont pu être décodées automatiquement via NHTSA : ${fields.joinToString(", ")}. Veuillez vérifier le VIN."
    }

    fun make(lang: AppLanguage) = when (lang) {
        AppLanguage.ARABIC -> "المصنّع"
        AppLanguage.FRENCH -> "Marque"
    }

    fun model(lang: AppLanguage) = when (lang) {
        AppLanguage.ARABIC -> "الموديل"
        AppLanguage.FRENCH -> "Modèle"
    }

    fun year(lang: AppLanguage) = when (lang) {
        AppLanguage.ARABIC -> "سنة الصنع"
        AppLanguage.FRENCH -> "Année"
    }

    fun engine(lang: AppLanguage) = when (lang) {
        AppLanguage.ARABIC -> "المحرك"
        AppLanguage.FRENCH -> "Moteur"
    }

    fun vehicleType(lang: AppLanguage) = when (lang) {
        AppLanguage.ARABIC -> "فئة المركبة"
        AppLanguage.FRENCH -> "Type de véhicule"
    }

    fun country(lang: AppLanguage) = when (lang) {
        AppLanguage.ARABIC -> "بلد المنشأ"
        AppLanguage.FRENCH -> "Pays d'assemblage"
    }

    fun saveVehicle(lang: AppLanguage) = when (lang) {
        AppLanguage.ARABIC -> "تسجيل المركبة في الجواز"
        AppLanguage.FRENCH -> "Enregistrer dans le Passeport"
    }

    fun viewPassport(lang: AppLanguage) = when (lang) {
        AppLanguage.ARABIC -> "عرض جواز المركبة"
        AppLanguage.FRENCH -> "Voir le Passeport"
    }

    fun timelineTitle(lang: AppLanguage) = when (lang) {
        AppLanguage.ARABIC -> "سجل الأحداث والخدمات"
        AppLanguage.FRENCH -> "Journal des événements"
    }

    fun addEvent(lang: AppLanguage) = when (lang) {
        AppLanguage.ARABIC -> "إضافة حدث جديد"
        AppLanguage.FRENCH -> "Ajouter un événement"
    }

    fun eventType(lang: AppLanguage, type: String) = when (type) {
        "service" -> if (lang == AppLanguage.ARABIC) "صيانة دورية" else "Entretien périodique"
        "repair" -> if (lang == AppLanguage.ARABIC) "إصلاح ميكانيكي / كهربائي" else "Réparation"
        "odometer" -> if (lang == AppLanguage.ARABIC) "تسجيل قراءة عداد" else "Relevé kilométrique"
        "inspection" -> if (lang == AppLanguage.ARABIC) "فحص دوري معتمد" else "Contrôle technique"
        "correction" -> if (lang == AppLanguage.ARABIC) "تصحيح سجل سابق" else "Correction d'enregistrement"
        else -> type
    }

    fun verificationLevel(lang: AppLanguage, level: String) = when {
        level.contains("VERIFIED_BY_PARTNER") || level.contains("CUSTOMER_CONFIRMED") ->
            if (lang == AppLanguage.ARABIC) "موثق من الورشة + مؤكد من الزبون ✓" else "Vérifié par l'atelier + Confirmé client ✓"
        level == "GARAGE_DECLARED" ->
            if (lang == AppLanguage.ARABIC) "مصرح من الورشة (في انتظار تأكيد الزبون)" else "Déclaré par l'atelier (En attente confirmation)"
        level == "verified" ->
            if (lang == AppLanguage.ARABIC) "موثق (مركز خدمة معتمد)" else "Vérifié (Atelier agréé)"
        level == "dealer_certified" ->
            if (lang == AppLanguage.ARABIC) "معتمد من الوكيل" else "Certifié Concessionnaire"
        level == "self_declared" ->
            if (lang == AppLanguage.ARABIC) "تصريح شخصي من المالك" else "Déclaration propriétaire"
        else -> level
    }

    fun odometerRollbackAlert(
        lang: AppLanguage,
        currentKm: Long,
        currentDate: String,
        prevKm: Long,
        prevDate: String,
        dropKm: Long
    ) = when (lang) {
        AppLanguage.ARABIC -> "⚠️ تنبيه تلاعب بالعداد: تم رصد انخفاض في قراءة العداد! القراءة المسجلة ($currentKm كم في $currentDate) أقل من القراءة السابقة ($prevKm كم في $prevDate) بفارق $dropKm كم. قد يشير هذا إلى تلاعب في عداد المسافات أو خطأ فادح في التسجيل."
        AppLanguage.FRENCH -> "⚠️ Alerte anomalie compteur : Une diminution du kilométrage a été détectée ! Le relevé ($currentKm km le $currentDate) est inférieur au précédent ($prevKm km le $prevDate) de $dropKm km. Cela peut indiquer une falsification du compteur kilométrique ou une erreur d'enregistrement."
    }

    fun cryptoIntegrityVerified(lang: AppLanguage) = when (lang) {
        AppLanguage.ARABIC -> "سلسلة التشفير سليمة وموثقة بالكامل (SHA-256)"
        AppLanguage.FRENCH -> "Chaîne cryptographique valide et inviolable (SHA-256)"
    }

    fun cryptoIntegrityNotice(lang: AppLanguage) = when (lang) {
        AppLanguage.ARABIC -> "جميع الأحداث مرتبطة تشفيرياً بالسجل السابق ولا يمكن التعديل أو الحذف عليها."
        AppLanguage.FRENCH -> "Chaque événement est lié cryptographiquement au précédent. Aucune suppression ni modification n'est permise."
    }

    fun reportTitle(lang: AppLanguage) = when (lang) {
        AppLanguage.ARABIC -> "تقرير فحص جواز السيارة"
        AppLanguage.FRENCH -> "Rapport Officiel Car Passport"
    }

    fun completenessTitle(lang: AppLanguage) = when (lang) {
        AppLanguage.ARABIC -> "مؤشر اكتمال البيانات"
        AppLanguage.FRENCH -> "Indice de complétude des données"
    }

    fun completenessLevel(lang: AppLanguage, score: Int) = when {
        score >= 80 -> if (lang == AppLanguage.ARABIC) "سجل شامل ومكتمل" else "Historique complet"
        score >= 50 -> if (lang == AppLanguage.ARABIC) "سجل متوسط البيانات" else "Historique partiel"
        else -> if (lang == AppLanguage.ARABIC) "بيانات أولية غير مكتملة" else "Données initiales limitées"
    }

    fun verificationBreakdown(lang: AppLanguage) = when (lang) {
        AppLanguage.ARABIC -> "توزيع مستوى التوثيق"
        AppLanguage.FRENCH -> "Répartition des niveaux de vérification"
    }

    fun verifiedVsDeclared(lang: AppLanguage, verifiedCount: Int, selfCount: Int) = when (lang) {
        AppLanguage.ARABIC -> "موثق / وكيل: $verifiedCount | تصريح شخصي: $selfCount"
        AppLanguage.FRENCH -> "Vérifié / Concessionnaire : $verifiedCount | Déclaré : $selfCount"
    }

    fun neverImplyCleanDisclaimer(lang: AppLanguage) = when (lang) {
        AppLanguage.ARABIC -> "إخلاء مسؤولية مهم: هذا التقرير يعكس البيانات المدخلة والموثقة فقط. عدم وجود سجلات سلبية أو قلة البيانات لا يعني بالضرورة خلو المركبة من المشاكل، الأعطال، أو الحوادث غير المصرح بها."
        AppLanguage.FRENCH -> "Avis important d'intégrité : Ce rapport reflète exclusivement les données saisies et vérifiées. L'absence de signalements ou le manque d'historique ne certifie en aucun cas l'absence de pannes, d'accidents ou de vices cachés."
    }

    fun shareReport(lang: AppLanguage) = when (lang) {
        AppLanguage.ARABIC -> "مشاركة التقرير مع QR"
        AppLanguage.FRENCH -> "Partager le rapport avec QR"
    }

    fun scanQrCode(lang: AppLanguage) = when (lang) {
        AppLanguage.ARABIC -> "امسح الرمز للتحقق من شهادة المركبة"
        AppLanguage.FRENCH -> "Scannez le code QR pour vérifier le certificat"
    }

    fun signInRequired(lang: AppLanguage) = when (lang) {
        AppLanguage.ARABIC -> "تسجيل الدخول مطلوب لإضافة مركبات أو أحداث"
        AppLanguage.FRENCH -> "Connexion requise pour enregistrer ou ajouter un événement"
    }

    fun signInWithGoogle(lang: AppLanguage) = when (lang) {
        AppLanguage.ARABIC -> "تسجيل الدخول عبر Google"
        AppLanguage.FRENCH -> "Continuer avec Google"
    }

    fun continueAsGuest(lang: AppLanguage) = when (lang) {
        AppLanguage.ARABIC -> "المتابعة كزائر (تصفح وقراءة التقارير)"
        AppLanguage.FRENCH -> "Continuer en tant qu'invité (Consultation)"
    }

    fun signOut(lang: AppLanguage) = when (lang) {
        AppLanguage.ARABIC -> "تسجيل الخروج"
        AppLanguage.FRENCH -> "Déconnexion"
    }

    fun signedInAs(lang: AppLanguage, email: String) = when (lang) {
        AppLanguage.ARABIC -> "مسجل كـ: $email"
        AppLanguage.FRENCH -> "Connecté : $email"
    }

    fun noEventsYet(lang: AppLanguage) = when (lang) {
        AppLanguage.ARABIC -> "لا توجد أحداث مسجلة لهذه المركبة بعد. اضغط على 'إضافة حدث' لبدء التوثيق."
        AppLanguage.FRENCH -> "Aucun événement enregistré pour ce véhicule. Cliquez sur 'Ajouter un événement' pour commencer."
    }

    fun recentVehicles(lang: AppLanguage) = when (lang) {
        AppLanguage.ARABIC -> "المركبات المسجلة مؤخراً"
        AppLanguage.FRENCH -> "Véhicules récemment enregistrés"
    }

    fun addCorrection(lang: AppLanguage) = when (lang) {
        AppLanguage.ARABIC -> "إضافة حدث تصحيحي"
        AppLanguage.FRENCH -> "Ajouter un événement correctif"
    }

    fun immutableRulesNotice(lang: AppLanguage) = when (lang) {
        AppLanguage.ARABIC -> "لا يمكن حذف أو تعديل أي حدث. لتعديل خطأ، يتم تسجيل حدث تصحيحي جديد يوثق التغيير."
        AppLanguage.FRENCH -> "Les événements sont immuables. Pour rectifier une saisie, un événement correctif doit être ajouté."
    }

    // --- Stage 2 AI Inspection Strings ---

    fun aiInspectionTitle(lang: AppLanguage) = when (lang) {
        AppLanguage.ARABIC -> "فحص بصري بالذكاء الاصطناعي"
        AppLanguage.FRENCH -> "Inspection Visuelle par IA"
    }

    fun startAiInspection(lang: AppLanguage) = when (lang) {
        AppLanguage.ARABIC -> "بدء فحص بصري بالذكاء الاصطناعي (12 صورة)"
        AppLanguage.FRENCH -> "Démarrer l'inspection IA (12 photos)"
    }

    fun viewInspectionReport(lang: AppLanguage) = when (lang) {
        AppLanguage.ARABIC -> "عرض نتائج الفحص البصري"
        AppLanguage.FRENCH -> "Voir le rapport d'inspection IA"
    }

    fun guidedCaptureTitle(lang: AppLanguage) = when (lang) {
        AppLanguage.ARABIC -> "التقاط الصور الإرشادية (12 زاوية مطلوبة)"
        AppLanguage.FRENCH -> "Capture guidée (12 angles requis)"
    }

    fun photoProgress(lang: AppLanguage, current: Int, total: Int) = when (lang) {
        AppLanguage.ARABIC -> "الصورة $current من $total"
        AppLanguage.FRENCH -> "Photo $current sur $total"
    }

    fun takeOrPickPhoto(lang: AppLanguage) = when (lang) {
        AppLanguage.ARABIC -> "التقاط أو اختيار صورة"
        AppLanguage.FRENCH -> "Prendre ou choisir une photo"
    }

    fun retakePhoto(lang: AppLanguage) = when (lang) {
        AppLanguage.ARABIC -> "إعادة التقاط"
        AppLanguage.FRENCH -> "Reprendre la photo"
    }

    fun photoBlurryOrDarkWarning(lang: AppLanguage) = when (lang) {
        AppLanguage.ARABIC -> "⚠️ الصورة مظلمة أو غير واضحة بما يكفي. يرجى إعادة الالتقاط لضمان دقة التحليل."
        AppLanguage.FRENCH -> "⚠️ La photo est trop sombre ou floue. Veuillez reprendre la photo pour garantir la précision."
    }

    fun markBlurryPrompt(lang: AppLanguage) = when (lang) {
        AppLanguage.ARABIC -> "هل الصورة مهتزة أو مظلمة؟ اضغط لإعادة الالتقاط"
        AppLanguage.FRENCH -> "Photo floue ou sombre ? Cliquez pour la reprendre"
    }

    fun analyzePhotosBtn(lang: AppLanguage) = when (lang) {
        AppLanguage.ARABIC -> "تحليل كافة الصور بواسطة الذكاء الاصطناعي"
        AppLanguage.FRENCH -> "Analyser toutes les photos avec l'IA"
    }

    fun analyzingPhotosWait(lang: AppLanguage) = when (lang) {
        AppLanguage.ARABIC -> "جاري الفحص الدقيق للهيكل واللوحة والعداد عبر Gemini 3.8 Flash..."
        AppLanguage.FRENCH -> "Analyse approfondie de la carrosserie et du compteur via Gemini 3.8 Flash..."
    }

    fun overallConditionScore(lang: AppLanguage) = when (lang) {
        AppLanguage.ARABIC -> "مؤشر الحالة البصرية العام"
        AppLanguage.FRENCH -> "Score d'état visuel global"
    }

    fun panelAssessmentTitle(lang: AppLanguage) = when (lang) {
        AppLanguage.ARABIC -> "تقييم أجزاء الهيكل واللوحات"
        AppLanguage.FRENCH -> "Évaluation des panneaux de carrosserie"
    }

    fun limitationsTitle(lang: AppLanguage) = when (lang) {
        AppLanguage.ARABIC -> "حدود الفحص والتقييم البصري"
        AppLanguage.FRENCH -> "Limites de l'analyse visuelle"
    }

    fun aiReportDisclaimer(lang: AppLanguage) = when (lang) {
        AppLanguage.ARABIC -> "تقدير ذكاء اصطناعي، وليس فحصاً فنياً معتمداً"
        AppLanguage.FRENCH -> "Estimation par IA, non équivalente à un contrôle technique certifié"
    }

    fun aiReportHedgingNotice(lang: AppLanguage) = when (lang) {
        AppLanguage.ARABIC -> "هذا التقرير مبني على تحليل الصور المقدمة فقط ولا يجزم بوقوع حوادث سابقة. نوصي بشدة بإجراء فحص ميكانيكي وهيكلي مباشر لدى مركز فحص معتمد."
        AppLanguage.FRENCH -> "Ce rapport repose exclusivement sur l'analyse visuelle des photos et n'affirme aucun accident. Une vérification physique par un garagiste agréé reste indispensable."
    }

    fun confidenceLabel(lang: AppLanguage, confidence: Double) = when {
        confidence >= 0.8 -> if (lang == AppLanguage.ARABIC) "ثقة عالية" else "Confiance élevée"
        confidence >= 0.6 -> if (lang == AppLanguage.ARABIC) "ثقة متوسطة" else "Confiance moyenne"
        else -> if (lang == AppLanguage.ARABIC) "يحتاج فحصاً مباشراً" else "Nécessite un examen direct"
    }

    fun panelCondition(lang: AppLanguage, condition: String) = when (condition.lowercase()) {
        "good" -> if (lang == AppLanguage.ARABIC) "حالة جيدة" else "Bon état"
        "fair" -> if (lang == AppLanguage.ARABIC) "حالة متوسطة" else "État moyen"
        "poor" -> if (lang == AppLanguage.ARABIC) "حالة تحتاج معاينة" else "À inspecter"
        else -> condition
    }

    fun findingLabel(lang: AppLanguage, finding: String) = when (finding.lowercase()) {
        "scratch" -> if (lang == AppLanguage.ARABIC) "خدوش سطحية" else "Rayure"
        "dent" -> if (lang == AppLanguage.ARABIC) "انبعاج / أثر صدمة" else "Bosse"
        "rust" -> if (lang == AppLanguage.ARABIC) "علامات صدأ وتآكل" else "Traces de rouille"
        "crack" -> if (lang == AppLanguage.ARABIC) "شروخ أو تصدع" else "Fissure"
        "possible_repaint" -> if (lang == AppLanguage.ARABIC) "مؤشرات على إعادة طلاء محتملة" else "Signes compatibles avec repeinture"
        "uneven_panel_gap" -> if (lang == AppLanguage.ARABIC) "عدم تطابق في فواصل الهيكل" else "Écart de panneau irrégulier"
        else -> finding
    }

    fun detectedOdometerFromInspection(lang: AppLanguage, km: Long, confPercent: Int) = when (lang) {
        AppLanguage.ARABIC -> "قراءة العداد المرصودة آلياً: $km كم (ثقة: $confPercent%)"
        AppLanguage.FRENCH -> "Kilométrage détecté par l'IA : $km km (Confiance : $confPercent%)"
    }

    // Stage 3: Market Data & Fair Price Estimation
    fun marketDataTitle(lang: AppLanguage) = when (lang) {
        AppLanguage.ARABIC -> "بيانات السوق المرجعية"
        AppLanguage.FRENCH -> "Données du marché"
    }

    fun marketDataSubtitle(lang: AppLanguage) = when (lang) {
        AppLanguage.ARABIC -> "إدارة أسعار وإعلانات سوق السيارات الجزائري"
        AppLanguage.FRENCH -> "Gestion des prix de référence du marché automobile algérien"
    }

    fun addListingTitle(lang: AppLanguage) = when (lang) {
        AppLanguage.ARABIC -> "إضافة إعلان مرجعي"
        AppLanguage.FRENCH -> "Ajouter une annonce de référence"
    }

    fun csvImportTitle(lang: AppLanguage) = when (lang) {
        AppLanguage.ARABIC -> "استيراد ملف CSV"
        AppLanguage.FRENCH -> "Importation de fichier CSV"
    }

    fun csvPreviewTitle(lang: AppLanguage) = when (lang) {
        AppLanguage.ARABIC -> "معاينة الصفوف قبل الحفظ"
        AppLanguage.FRENCH -> "Aperçu des lignes avant enregistrement"
    }

    fun saveAllListingsBtn(lang: AppLanguage) = when (lang) {
        AppLanguage.ARABIC -> "حفظ الإعلانات في قاعدة البيانات"
        AppLanguage.FRENCH -> "Enregistrer les annonces dans la base"
    }

    fun totalListingsCount(lang: AppLanguage, count: Int) = when (lang) {
        AppLanguage.ARABIC -> "إجمالي العينات المتاحة: $count إعلاناً"
        AppLanguage.FRENCH -> "Total des annonces : $count"
    }

    fun adminOnlyNotice(lang: AppLanguage) = when (lang) {
        AppLanguage.ARABIC -> "خاص بالمشرفين فقط"
        AppLanguage.FRENCH -> "Accès administrateur uniquement"
    }

    fun noPersonalDataStored(lang: AppLanguage) = when (lang) {
        AppLanguage.ARABIC -> "حماية الخصوصية: لا يتم تخزين أسماء البائعين أو أرقام الهواتف إطلاقاً"
        AppLanguage.FRENCH -> "Confidentialité : Aucun nom ni numéro de vendeur n'est collecté"
    }

    fun fairPriceTitle(lang: AppLanguage) = when (lang) {
        AppLanguage.ARABIC -> "تقدير السعر العادل"
        AppLanguage.FRENCH -> "Estimation du juste prix"
    }

    fun askingPriceLabel(lang: AppLanguage) = when (lang) {
        AppLanguage.ARABIC -> "السعر المطلوب (د.ج)"
        AppLanguage.FRENCH -> "Prix demandé (DZD)"
    }

    fun askingPricePlaceholder(lang: AppLanguage) = when (lang) {
        AppLanguage.ARABIC -> "أدخل السعر المعروض للبيع بالدينار..."
        AppLanguage.FRENCH -> "Saisir le prix demandé en Dinars..."
    }

    fun calculateEstimateBtn(lang: AppLanguage) = when (lang) {
        AppLanguage.ARABIC -> "حساب السعر العادل ومطابقة السوق"
        AppLanguage.FRENCH -> "Calculer l'estimation du marché"
    }

    fun fairPriceRangeTitle(lang: AppLanguage) = when (lang) {
        AppLanguage.ARABIC -> "النطاق السعري العادل"
        AppLanguage.FRENCH -> "Fourchette de prix équitable"
    }

    fun lowPriceLabel(lang: AppLanguage) = when (lang) {
        AppLanguage.ARABIC -> "الحد الأدنى"
        AppLanguage.FRENCH -> "Fourchette basse"
    }

    fun medianFairPriceLabel(lang: AppLanguage) = when (lang) {
        AppLanguage.ARABIC -> "السعر العادل (الوسيط)"
        AppLanguage.FRENCH -> "Prix équitable (Médian)"
    }

    fun highPriceLabel(lang: AppLanguage) = when (lang) {
        AppLanguage.ARABIC -> "الحد الأعلى"
        AppLanguage.FRENCH -> "Fourchette haute"
    }

    fun verdictBelowFair(lang: AppLanguage) = when (lang) {
        AppLanguage.ARABIC -> "أقل من العادل"
        AppLanguage.FRENCH -> "Sous le prix équitable"
    }

    fun verdictFair(lang: AppLanguage) = when (lang) {
        AppLanguage.ARABIC -> "عادل"
        AppLanguage.FRENCH -> "Prix équitable"
    }

    fun verdictAboveFair(lang: AppLanguage) = when (lang) {
        AppLanguage.ARABIC -> "أعلى من العادل"
        AppLanguage.FRENCH -> "Au-dessus du prix équitable"
    }

    fun lowConfidenceMarketWarning(lang: AppLanguage) = when (lang) {
        AppLanguage.ARABIC -> "تقدير ضعيف: بيانات السوق غير كافية"
        AppLanguage.FRENCH -> "Estimation faible : données de marché insuffisantes"
    }

    fun topFactorsTitle(lang: AppLanguage) = when (lang) {
        AppLanguage.ARABIC -> "أبرز العوامل المؤثرة على التقدير"
        AppLanguage.FRENCH -> "Facteurs déterminants du prix"
    }

    fun conditionAdjustmentLine(lang: AppLanguage) = when (lang) {
        AppLanguage.ARABIC -> "تعديل الفحص البصري للهيكل"
        AppLanguage.FRENCH -> "Ajustement état visuel carrosserie"
    }

    fun sampleSizeLabel(lang: AppLanguage) = when (lang) {
        AppLanguage.ARABIC -> "عينات المقارنة"
        AppLanguage.FRENCH -> "Échantillon comparable"
    }

    fun comparisonLevelLabel(lang: AppLanguage) = when (lang) {
        AppLanguage.ARABIC -> "مستوى المطابقة"
        AppLanguage.FRENCH -> "Niveau de comparaison"
    }

    fun freshnessLabel(lang: AppLanguage) = when (lang) {
        AppLanguage.ARABIC -> "حداثة البيانات"
        AppLanguage.FRENCH -> "Fraîcheur des données"
    }

    fun statisticalDisclaimer(lang: AppLanguage) = when (lang) {
        AppLanguage.ARABIC -> "تقدير إحصائي وليس تقييماً رسمياً"
        AppLanguage.FRENCH -> "Estimation statistique, non équivalente à une expertise officielle"
    }

    fun aiExplanationTitle(lang: AppLanguage) = when (lang) {
        AppLanguage.ARABIC -> "تحليل وتفسير السعر العادل (3 جمل)"
        AppLanguage.FRENCH -> "Explication du prix équitable (3 phrases)"
    }

    // ==========================================
    // STAGE 4: Garage Portal, Confirmation, Anti-Fraud
    // ==========================================

    fun garagePortalTitle(lang: AppLanguage) = when (lang) {
        AppLanguage.ARABIC -> "بوابة الورش ومراكز الصيانة"
        AppLanguage.FRENCH -> "Portail Ateliers & Garages"
    }

    fun garageRegisterTitle(lang: AppLanguage) = when (lang) {
        AppLanguage.ARABIC -> "تسجيل حساب ورشة جديد"
        AppLanguage.FRENCH -> "Inscription Atelier Agréé"
    }

    fun fastEntryTitle(lang: AppLanguage) = when (lang) {
        AppLanguage.ARABIC -> "تسجيل صيانة سريع (< 60 ثانية)"
        AppLanguage.FRENCH -> "Saisie rapide d'intervention (< 60s)"
    }

    fun serviceTemplateName(lang: AppLanguage, templateId: String) = when (templateId) {
        "oil_change" -> if (lang == AppLanguage.ARABIC) "تغيير الزيت (Vidange)" else "Vidange d'huile moteur"
        "filters" -> if (lang == AppLanguage.ARABIC) "تغيير الفلاتر" else "Filtres (air, huile, carburant)"
        "timing_belt" -> if (lang == AppLanguage.ARABIC) "حزام التوقيت (Distribution)" else "Courroie de distribution"
        "brakes" -> if (lang == AppLanguage.ARABIC) "الفرامل (Freinage)" else "Freinage (plaquettes/disques)"
        "battery" -> if (lang == AppLanguage.ARABIC) "البطارية" else "Batterie"
        "tires" -> if (lang == AppLanguage.ARABIC) "العجلات والإطارات" else "Pneumatiques"
        "clutch" -> if (lang == AppLanguage.ARABIC) "الدبرياج (Embrayage)" else "Kit embrayage"
        "suspension" -> if (lang == AppLanguage.ARABIC) "نظام التعليق" else "Suspension / Amortisseurs"
        "body_repair" -> if (lang == AppLanguage.ARABIC) "سمكرة وهيكل" else "Tôlerie & Carrosserie"
        "painting" -> if (lang == AppLanguage.ARABIC) "صباغة ودهان" else "Peinture"
        "diagnostics" -> if (lang == AppLanguage.ARABIC) "فحص إلكتروني (Scanner)" else "Diagnostic électronique / Scanner"
        "other" -> if (lang == AppLanguage.ARABIC) "أعمال أخرى" else "Autre prestation"
        else -> templateId
    }

    fun sendViaWhatsApp(lang: AppLanguage) = when (lang) {
        AppLanguage.ARABIC -> "إرسال عبر واتساب (WhatsApp)"
        AppLanguage.FRENCH -> "Envoyer via WhatsApp"
    }

    fun customerConfirmTitle(lang: AppLanguage) = when (lang) {
        AppLanguage.ARABIC -> "تأكيد سجل الصيانة للزبون"
        AppLanguage.FRENCH -> "Confirmation Client de l'Intervention"
    }

    fun confirmServiceBtn(lang: AppLanguage) = when (lang) {
        AppLanguage.ARABIC -> "تأكيد السجل واعتماده ✓"
        AppLanguage.FRENCH -> "Confirmer l'intervention ✓"
    }

    fun disputeServiceBtn(lang: AppLanguage) = when (lang) {
        AppLanguage.ARABIC -> "اعتراض على هذا السجل ⚠️"
        AppLanguage.FRENCH -> "Contester cette saisie ⚠️"
    }

    fun qrPosterTitle(lang: AppLanguage) = when (lang) {
        AppLanguage.ARABIC -> "ملصق QR الرسمي للورشة"
        AppLanguage.FRENCH -> "Affiche QR Officielle Atelier"
    }

    fun qrPosterSubtitle(lang: AppLanguage) = when (lang) {
        AppLanguage.ARABIC -> "امسح لرؤية جواز سيارتك"
        AppLanguage.FRENCH -> "Scannez pour consulter le passeport de votre véhicule"
    }

    fun antiFraudTitle(lang: AppLanguage) = when (lang) {
        AppLanguage.ARABIC -> "لوحة مكافحة الاحتيال والاعتمادات"
        AppLanguage.FRENCH -> "Système Anti-Fraude & Agréments"
    }

    // ==========================================
    // STAGE 5: Final Report, Seller Tools, Owner Logbook
    // ==========================================

    fun sellerPortalTitle(lang: AppLanguage) = when (lang) {
        AppLanguage.ARABIC -> "بوابة البائعين ومعارض السيارات"
        AppLanguage.FRENCH -> "Espace Vendeurs & Concessionnaires"
    }

    fun ownerLogbookTitle(lang: AppLanguage) = when (lang) {
        AppLanguage.ARABIC -> "دفتر صيانة المالك ومحفظة المستندات"
        AppLanguage.FRENCH -> "Carnet d'entretien & Portefeuille"
    }

    fun lowCompletenessBanner(lang: AppLanguage) = when (lang) {
        AppLanguage.ARABIC -> "بيانات قليلة: غياب المعلومات لا يعني أن السيارة سليمة"
        AppLanguage.FRENCH -> "Données limitées : L'absence d'information ne garantit pas un bon état"
    }

    fun reportFooterDisclaimer(lang: AppLanguage) = when (lang) {
        AppLanguage.ARABIC -> "هذا التقرير مبني على بيانات متاحة وقت إصداره، ولا يغني عن فحص ميكانيكي مباشر."
        AppLanguage.FRENCH -> "Ce rapport est basé sur les données disponibles au moment de son émission et ne remplace pas une expertise mécanique directe."
    }

    fun verifiedSellerBadgeTitle(lang: AppLanguage) = when (lang) {
        AppLanguage.ARABIC -> "شارة البائع الموثّق"
        AppLanguage.FRENCH -> "Badge Vendeur Certifié"
    }
}
