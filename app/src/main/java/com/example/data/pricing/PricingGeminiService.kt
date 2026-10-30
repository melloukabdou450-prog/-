package com.example.data.pricing

import android.util.Log
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.content
import com.google.firebase.ai.type.generationConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import java.util.Locale

object PricingGeminiService {

    private const val TAG = "PricingGemini"

    private val generativeModel by lazy {
        Firebase.ai.generativeModel(
            modelName = "gemini-3.8-flash",
            generationConfig = generationConfig {
                temperature = 0.2f
                maxOutputTokens = 300
            },
            systemInstruction = content {
                text(
                    """
أنت خبير إحصائي ومحلل لسوق السيارات الجزائري في منصة "جواز السيارة" (Car Passport).
المهمة: كتابة شرح مقتضب باللغة العربية الفصحى يتكون من 3 جمل فقط لا غير يشرح التقدير الإحصائي العادل لسعر السيارة بناءً على الأرقام الدقيقة المزودة لك فقط.

القواعد الصارمة:
1. اعتمد حصراً على الأرقام المعطاة ولا تخترع أو تخمن أي رقم غير موجود في المعطيات.
2. اكتب بالضبط 3 جمل، لا تزيد ولا تنقص.
3. التزم بلغة محايدة واشرح كيف أثرت المسافة (الممشى)، سنة الصنع، والحالة الفنية على السعر العادل ومقارنته بالسعر المطلوب.
                    """.trimIndent()
                )
            }
        )
    }

    suspend fun generateArabicExplanation(
        make: String,
        model: String,
        year: Int,
        km: Long,
        askingPrice: Double,
        result: EstimationResult
    ): String = withContext(Dispatchers.IO) {
        val lowFormatted = String.format(Locale.US, "%,.0f د.ج", result.lowPrice)
        val fairFormatted = String.format(Locale.US, "%,.0f د.ج", result.fairPrice)
        val highFormatted = String.format(Locale.US, "%,.0f د.ج", result.highPrice)
        val askingFormatted = String.format(Locale.US, "%,.0f د.ج", askingPrice)
        val diffFormatted = String.format(Locale.US, "%+.1f%%", result.diffPercentage)
        val verdict = result.verdictArabic ?: "غير محدد لقلة البيانات"

        val factorsSummary = result.factors.joinToString("، ") {
            "${it.nameArabic}: ${String.format(Locale.US, "%+,.0f د.ج", it.amountDzd)}"
        }

        val promptText = """
معطيات تقييم المركبة:
- المركبة: $year $make $model
- المسافة المقطوعة: $km كم
- السعر المطلوب: $askingFormatted
- السعر العادل المحسوب: $fairFormatted
- النطاق السعري العادل: من $lowFormatted إلى $highFormatted
- الفارق عن السعر العادل: $diffFormatted
- الحكم على السعر: $verdict
- عدد العينات المقارنة: ${result.comparablesCount} (المستوى: ${result.comparisonLevelLabelArabic})
- مستوى الثقة: ${result.confidence}
- أبرز العوامل المؤثرة: $factorsSummary

المطلوب: كتابة شرح دقيق في 3 جمل فقط يوضح هذه النتيجة بالأرقام المذكورة أعلاه.
        """.trimIndent()

        try {
            withTimeout(15000L) {
                val response = generativeModel.generateContent(promptText)
                val text = response.text?.trim()
                if (!text.isNullOrBlank()) {
                    return@withTimeout text
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Gemini pricing explanation failed or timed out, using deterministic explanation", e)
        }

        // Deterministic pristine 3-sentence fallback guaranteed never to invent any number
        buildFallbackExplanation(
            make = make,
            model = model,
            year = year,
            askingFormatted = askingFormatted,
            fairFormatted = fairFormatted,
            lowFormatted = lowFormatted,
            highFormatted = highFormatted,
            diffFormatted = diffFormatted,
            verdict = verdict,
            result = result
        )
    }

    private fun buildFallbackExplanation(
        make: String,
        model: String,
        year: Int,
        askingFormatted: String,
        fairFormatted: String,
        lowFormatted: String,
        highFormatted: String,
        diffFormatted: String,
        verdict: String,
        result: EstimationResult
    ): String {
        val sentence1 = "يقدّر السعر العادل لسيارة $year $make $model بحوالي $fairFormatted ضمن نطاق إحصائي يتراوح بين $lowFormatted و $highFormatted استناداً إلى ${result.comparablesCount} سيارة مشابهة في السوق."
        val sentence2 = if (result.confidence != "low") {
            "يعتبر السعر المطلوب البالغ $askingFormatted سعراً \"$verdict\" بفارق $diffFormatted عن القيمة العادلة المرجعية."
        } else {
            "نظراً لمحدودية العينات في قاعدة البيانات، يعد هذا التقدير استرشادياً أولياً ويحتاج لمزيد من بيانات السوق المقارنة."
        }
        val topFactor = result.factors.firstOrNull()?.nameArabic ?: "الممشى وسنة الصنع"
        val sentence3 = "تأثرت القيمة المحسوبة بشكل رئيسي بعامل $topFactor بالإضافة إلى تعديل الحالة الفنية ومستوى الطلب المحلي."

        return "$sentence1 $sentence2 $sentence3"
    }
}
