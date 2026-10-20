package dz.zakary.grd

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.graphics.Typeface
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class WasteRecord(
    val id: Long, val date: String, val type: String, val department: String,
    val quantity: Double, val recovered: Double, val destination: String,
    val cost: Double, val income: Double, val note: String
)

class MainActivity : Activity() {
    private val records = mutableListOf<WasteRecord>()
    private val types = listOf("بلاستيك", "ورق وكرتون", "معدن", "نفايات عضوية", "نفايات كيميائية", "زيوت مستعملة", "حمأة", "أخرى")
    private val departments = listOf("الإنتاج", "المخازن", "الصيانة", "المخبر", "معالجة المياه", "الإدارة", "أخرى")
    private val destinations = listOf("إعادة التدوير", "التثمين", "المعالجة", "التخلص النهائي", "مخزن مؤقت", "أخرى")
    private val pages = mutableMapOf<String, LinearLayout>()
    private lateinit var root: LinearLayout
    private lateinit var contentHost: LinearLayout
    private lateinit var dashboardPage: LinearLayout
    private lateinit var recordsPage: LinearLayout
    private lateinit var formPage: LinearLayout
    private lateinit var financePage: LinearLayout
    private lateinit var reportsPage: LinearLayout
    private lateinit var dashboardStats: LinearLayout
    private lateinit var recordList: LinearLayout
    private lateinit var dateInput: EditText
    private lateinit var quantityInput: EditText
    private lateinit var recoveredInput: EditText
    private lateinit var costInput: EditText
    private lateinit var incomeInput: EditText
    private lateinit var noteInput: EditText
    private lateinit var typeSpinner: Spinner
    private lateinit var departmentSpinner: Spinner
    private lateinit var destinationSpinner: Spinner
    private lateinit var searchInput: EditText
    private var currentPage = "dashboard"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        loadRecords()
        buildScreen()
        renderRecords()
        showPage("dashboard")
    }

    private fun buildScreen() {
        val scroll = ScrollView(this)
        root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutDirection = View.LAYOUT_DIRECTION_RTL
            setPadding(dp(14), dp(12), dp(14), dp(22))
            setBackgroundColor(0xFFF1F6F5.toInt())
        }
        scroll.addView(root)
        setContentView(scroll)

        val top = TextView(this).apply {
            text = "ZAKARY GRD  ♻"
            textSize = 23f
            setTypeface(null, Typeface.BOLD)
            setTextColor(0xFFFFFFFF.toInt())
            gravity = Gravity.RIGHT or Gravity.CENTER_VERTICAL
            setPadding(dp(18), dp(17), dp(18), dp(17))
            setBackgroundColor(0xFF05090D.toInt())
        }
        root.addView(top, matchWrap())
        val hero = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutDirection = View.LAYOUT_DIRECTION_RTL
            setPadding(dp(18), dp(18), dp(18), dp(18))
            setBackgroundColor(0xFF173650.toInt())
        }
        hero.addView(TextView(this).apply {
            text = "نظام تسيير واسترجاع النفايات الصناعية"
            textSize = 20f
            setTypeface(null, Typeface.BOLD)
            setTextColor(0xFFFFFFFF.toInt())
            gravity = Gravity.RIGHT
        }, matchWrap())
        hero.addView(TextView(this).apply {
            text = "تسجيل • متابعة • تثمين • تقارير"
            textSize = 14f
            setTextColor(0xFFD8F4EA.toInt())
            gravity = Gravity.RIGHT
            setPadding(0, dp(7), 0, 0)
        }, matchWrap())
        root.addView(hero, matchWrap())

        val navScroll = HorizontalScrollViewCompat(this)
        val nav = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; layoutDirection = View.LAYOUT_DIRECTION_RTL }
        navScroll.addView(nav)
        listOf(
            Triple("لوحة القيادة", "dashboard", "▦"),
            Triple("تسجيل نفايات", "form", "＋"),
            Triple("السجلات", "records", "☷"),
            Triple("التكاليف والمداخيل", "finance", "دج"),
            Triple("التقارير والنسخ", "reports", "⇩")
        ).forEach { item ->
            val b = Button(this).apply {
                text = "${item.third}  ${item.first}"
                isAllCaps = false
                textSize = 13f
                setOnClickListener { showPage(item.second) }
            }
            val p = LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, dp(56))
            p.setMargins(dp(3), dp(8), dp(3), dp(8))
            nav.addView(b, p)
        }
        root.addView(navScroll, matchWrap())

        contentHost = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; layoutDirection = View.LAYOUT_DIRECTION_RTL }
        root.addView(contentHost, matchWrap())

        dashboardPage = newPage("لوحة القيادة")
        dashboardStats = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        dashboardPage.addView(dashboardStats, matchWrap())
        val quick = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        quick.addView(actionButton("＋ تسجيل نفايات جديدة") { showPage("form") }, matchWrap())
        quick.addView(actionButton("☷ عرض السجلات") { showPage("records") }, matchWrap())
        quick.addView(actionButton("⇩ تصدير تقرير CSV") { shareCsv() }, matchWrap())
        dashboardPage.addView(quick, matchWrap())
        pages["dashboard"] = dashboardPage

        formPage = newPage("تسجيل عملية نفايات")
        dateInput = field(formPage, "التاريخ (YYYY-MM-DD)").apply { setText(today()) }
        typeSpinner = spinner(formPage, types)
        departmentSpinner = spinner(formPage, departments)
        quantityInput = field(formPage, "الكمية الإجمالية بالكيلوغرام", true)
        recoveredInput = field(formPage, "الكمية المسترجعة بالكيلوغرام", true)
        destinationSpinner = spinner(formPage, destinations)
        costInput = field(formPage, "تكلفة المعالجة أو النقل (دج)", true)
        incomeInput = field(formPage, "مداخيل التثمين أو البيع (دج)", true)
        noteInput = field(formPage, "ملاحظات أو رقم وصل (اختياري)")
        formPage.addView(actionButton("حفظ العملية") { saveRecord() }, matchWrap())
        formPage.addView(actionButton("مسح حقول الإدخال") { clearForm() }, matchWrap())
        pages["form"] = formPage

        recordsPage = newPage("سجل العمليات")
        searchInput = field(recordsPage, "بحث حسب النوع أو القسم أو التاريخ")
        recordsPage.addView(actionButton("بحث / تحديث السجل") { renderRecords() }, matchWrap())
        recordsPage.addView(actionButton("إلغاء البحث") { searchInput.setText(""); renderRecords() }, matchWrap())
        recordList = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        recordsPage.addView(recordList, matchWrap())
        pages["records"] = recordsPage

        financePage = newPage("التكاليف والمداخيل")
        pages["finance"] = financePage

        reportsPage = newPage("التقارير والنسخ الاحتياطي")
        reportsPage.addView(actionButton("تصدير تقرير CSV") { shareCsv() }, matchWrap())
        reportsPage.addView(actionButton("مشاركة / حفظ نسخة احتياطية") { backupData() }, matchWrap())
        reportsPage.addView(actionButton("استيراد نسخة احتياطية") { importBackup() }, matchWrap())
        reportsPage.addView(actionButton("عرض ملخص حسب نوع النفايات") { showTypeSummary() }, matchWrap())
        reportsPage.addView(TextView(this).apply {
            text = "ملاحظة: البيانات محفوظة محليًا على هذا الهاتف. احتفظ بنسخة احتياطية قبل حذف التطبيق أو تغيير الهاتف."
            textSize = 14f
            setTextColor(0xFF795500.toInt())
            setPadding(dp(12), dp(12), dp(12), dp(12))
            setBackgroundColor(0xFFFFF2CC.toInt())
        }, matchWrap())
        pages["reports"] = reportsPage

        refreshDashboard()
        refreshFinance()
    }

    private fun newPage(title: String): LinearLayout {
        val page = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutDirection = View.LAYOUT_DIRECTION_RTL
            setPadding(dp(2), dp(12), dp(2), dp(12))
            visibility = View.GONE
        }
        page.addView(TextView(this).apply {
            text = title
            textSize = 23f
            setTypeface(null, Typeface.BOLD)
            setTextColor(0xFF173650.toInt())
            gravity = Gravity.RIGHT
            setPadding(dp(2), dp(8), dp(2), dp(14))
        }, matchWrap())
        contentHost.addView(page, matchWrap())
        return page
    }

    private fun showPage(name: String) {
        currentPage = name
        pages.values.forEach { it.visibility = if (it === pages[name]) View.VISIBLE else View.GONE }
        if (name == "dashboard") refreshDashboard()
        if (name == "records") renderRecords()
        if (name == "finance") refreshFinance()
        if (name == "reports") Unit
    }

    private fun field(parent: LinearLayout, hintText: String, numeric: Boolean = false): EditText {
        val e = EditText(this).apply {
            hint = hintText
            textSize = 15f
            gravity = Gravity.RIGHT or Gravity.CENTER_VERTICAL
            layoutDirection = View.LAYOUT_DIRECTION_RTL
            setPadding(dp(10), dp(4), dp(10), dp(4))
            setBackgroundColor(0xFFFFFFFF.toInt())
            if (numeric) inputType = android.text.InputType.TYPE_CLASS_NUMBER or android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL
        }
        val p = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(52))
        p.bottomMargin = dp(8)
        parent.addView(e, p)
        return e
    }

    private fun spinner(parent: LinearLayout, items: List<String>): Spinner {
        val s = Spinner(this).apply {
            adapter = ArrayAdapter(this@MainActivity, android.R.layout.simple_spinner_dropdown_item, items)
            layoutDirection = View.LAYOUT_DIRECTION_RTL
            setBackgroundColor(0xFFFFFFFF.toInt())
        }
        val p = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(48))
        p.bottomMargin = dp(8)
        parent.addView(s, p)
        return s
    }

    private fun actionButton(label: String, action: () -> Unit): Button {
        return Button(this).apply {
            text = label
            isAllCaps = false
            textSize = 15f
            setTextColor(if (label.contains("تصدير") || label.contains("حفظ العملية")) 0xFFFFFFFF.toInt() else 0xFF173650.toInt())
            setBackgroundColor(if (label.contains("تصدير") || label.contains("حفظ العملية")) 0xFF247A5A.toInt() else 0xFFE0E9E7.toInt())
            setOnClickListener { action() }
        }
    }

    private fun saveRecord() {
        val date = dateInput.text.toString().trim()
        val quantity = number(quantityInput)
        val recovered = number(recoveredInput)
        val cost = number(costInput, 0.0)
        val income = number(incomeInput, 0.0)
        if (date.isBlank() || quantity == null || recovered == null || cost == null || income == null ||
            quantity <= 0 || recovered < 0 || recovered > quantity || cost < 0 || income < 0) {
            Toast.makeText(this, "تحقق من التاريخ والكميات والتكاليف. المسترجع يجب ألا يتجاوز الإجمالي.", Toast.LENGTH_LONG).show()
            return
        }
        records.add(0, WasteRecord(System.currentTimeMillis(), date, typeSpinner.selectedItem.toString(),
            departmentSpinner.selectedItem.toString(), quantity, recovered, destinationSpinner.selectedItem.toString(),
            cost, income, noteInput.text.toString().trim()))
        persistRecords()
        clearForm()
        renderRecords()
        refreshDashboard()
        refreshFinance()
        Toast.makeText(this, "تم حفظ العملية بنجاح", Toast.LENGTH_SHORT).show()
        showPage("dashboard")
    }

    private fun clearForm() {
        dateInput.setText(today())
        quantityInput.setText("")
        recoveredInput.setText("")
        costInput.setText("")
        incomeInput.setText("")
        noteInput.setText("")
        typeSpinner.setSelection(0)
        departmentSpinner.setSelection(0)
        destinationSpinner.setSelection(0)
    }

    private fun renderRecords() {
        if (!::recordList.isInitialized) return
        recordList.removeAllViews()
        val q = if (::searchInput.isInitialized) searchInput.text.toString().trim().lowercase(Locale.ROOT) else ""
        val filtered = records.filter {
            q.isBlank() || listOf(it.date, it.type, it.department, it.destination, it.note).any { value -> value.lowercase(Locale.ROOT).contains(q) }
        }
        if (filtered.isEmpty()) {
            recordList.addView(TextView(this).apply {
                text = if (records.isEmpty()) "لا توجد سجلات بعد. اضغط «تسجيل نفايات» لإضافة أول عملية." else "لا توجد نتائج مطابقة للبحث."
                textSize = 15f
                setTextColor(0xFF657681.toInt())
                setPadding(dp(10), dp(16), dp(10), dp(16))
            }, matchWrap())
        }
        filtered.forEach { r ->
            val card = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                layoutDirection = View.LAYOUT_DIRECTION_RTL
                setPadding(dp(13), dp(10), dp(13), dp(10))
                setBackgroundColor(0xFFFFFFFF.toInt())
            }
            card.addView(TextView(this).apply {
                text = "${r.type}  •  ${r.date}"
                textSize = 17f
                setTypeface(null, Typeface.BOLD)
                setTextColor(0xFF247A5A.toInt())
            }, matchWrap())
            card.addView(TextView(this).apply {
                text = "القسم: ${r.department}\nالإجمالي: ${fmt(r.quantity)} كغ | المسترجع: ${fmt(r.recovered)} كغ\nالوجهة: ${r.destination}\nالتكلفة: ${fmt(r.cost)} دج | المداخيل: ${fmt(r.income)} دج" +
                    if (r.note.isBlank()) "" else "\nملاحظات: ${r.note}"
                textSize = 14f
                setTextColor(0xFF243746.toInt())
                setPadding(0, dp(7), 0, dp(6))
            }, matchWrap())
            val row = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; layoutDirection = View.LAYOUT_DIRECTION_RTL }
            row.addView(actionButton("تعديل") { editRecord(r) }, LinearLayout.LayoutParams(0, dp(48), 1f))
            row.addView(actionButton("حذف") { confirmDelete(r) }, LinearLayout.LayoutParams(0, dp(48), 1f))
            card.addView(row, matchWrap())
            val p = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            p.bottomMargin = dp(10)
            recordList.addView(card, p)
        }
    }

    private fun confirmDelete(r: WasteRecord) {
        AlertDialog.Builder(this).setTitle("حذف السجل")
            .setMessage("هل تريد حذف عملية ${r.type} بتاريخ ${r.date}؟")
            .setNegativeButton("إلغاء", null)
            .setPositiveButton("حذف") { _, _ ->
                records.removeAll { it.id == r.id }
                persistRecords(); renderRecords(); refreshDashboard(); refreshFinance()
            }.show()
    }

    private fun editRecord(r: WasteRecord) {
        val panel = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(8), 0, dp(8), 0) }
        val q = EditText(this).apply { hint = "الكمية الإجمالية كغ"; setText(fmt(r.quantity)) }
        val rec = EditText(this).apply { hint = "الكمية المسترجعة كغ"; setText(fmt(r.recovered)) }
        val cost = EditText(this).apply { hint = "التكلفة دج"; setText(fmt(r.cost)) }
        val income = EditText(this).apply { hint = "المداخيل دج"; setText(fmt(r.income)) }
        listOf(q, rec, cost, income).forEach { it.inputType = android.text.InputType.TYPE_CLASS_NUMBER or android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL; panel.addView(it) }
        AlertDialog.Builder(this).setTitle("تعديل ${r.type}").setView(panel)
            .setNegativeButton("إلغاء", null)
            .setPositiveButton("حفظ") { _, _ ->
                val nq = q.text.toString().replace(',', '.').toDoubleOrNull()
                val nr = rec.text.toString().replace(',', '.').toDoubleOrNull()
                val nc = cost.text.toString().replace(',', '.').toDoubleOrNull()
                val ni = income.text.toString().replace(',', '.').toDoubleOrNull()
                if (nq == null || nr == null || nc == null || ni == null || nq <= 0 || nr < 0 || nr > nq || nc < 0 || ni < 0) {
                    Toast.makeText(this, "قيم غير صحيحة؛ لم يتم حفظ التعديل.", Toast.LENGTH_LONG).show()
                } else {
                    val index = records.indexOfFirst { it.id == r.id }
                    if (index >= 0) records[index] = r.copy(quantity=nq, recovered=nr, cost=nc, income=ni)
                    persistRecords(); renderRecords(); refreshDashboard(); refreshFinance()
                }
            }.show()
    }

    private fun refreshDashboard() {
        if (!::dashboardStats.isInitialized) return
        dashboardStats.removeAllViews()
        val total = records.sumOf { it.quantity }
        val recovered = records.sumOf { it.recovered }
        val costs = records.sumOf { it.cost }
        val income = records.sumOf { it.income }
        val rate = if (total > 0) recovered * 100 / total else 0.0
        dashboardStats.addView(metric("إجمالي النفايات المنتجة", "${fmt(total)} كغ"), matchWrap())
        dashboardStats.addView(metric("إجمالي الكمية المسترجعة", "${fmt(recovered)} كغ"), matchWrap())
        dashboardStats.addView(metric("الكمية المعالجة / المتخلص منها", "${fmt((total-recovered).coerceAtLeast(0.0))} كغ"), matchWrap())
        dashboardStats.addView(metric("نسبة الاسترجاع", "${String.format(Locale.US, "%.1f", rate)}%"), matchWrap())
        dashboardStats.addView(metric("صافي التكاليف", "${fmt(costs-income)} دج"), matchWrap())
        dashboardStats.addView(metric("عدد السجلات", records.size.toString()), matchWrap())
        val typesBox = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(12), dp(12), dp(12), dp(12)); setBackgroundColor(0xFFFFFFFF.toInt()) }
        typesBox.addView(TextView(this).apply { text="ملخص حسب نوع النفايات"; textSize=18f; setTypeface(null, Typeface.BOLD); setTextColor(0xFF173650.toInt()) }, matchWrap())
        types.distinct().forEach { type ->
            val subset = records.filter { it.type == type }
            if (subset.isNotEmpty()) typesBox.addView(TextView(this).apply {
                text = "$type: ${fmt(subset.sumOf { it.quantity })} كغ منتج | ${fmt(subset.sumOf { it.recovered })} كغ مسترجع"
                textSize = 14f; setTextColor(0xFF243746.toInt()); setPadding(0, dp(5), 0, dp(5))
            }, matchWrap())
        }
        dashboardStats.addView(typesBox, matchWrap())
    }

    private fun metric(label: String, value: String): View {
        val card = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; layoutDirection = View.LAYOUT_DIRECTION_RTL; setPadding(dp(14), dp(12), dp(14), dp(12)); setBackgroundColor(0xFFFFFFFF.toInt()) }
        card.addView(TextView(this).apply { text=label; textSize=15f; setTextColor(0xFF687B89.toInt()) }, matchWrap())
        card.addView(TextView(this).apply { text=value; textSize=24f; setTypeface(null, Typeface.BOLD); setTextColor(0xFF247A5A.toInt()); setPadding(0, dp(6), 0, 0) }, matchWrap())
        val p = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        p.setMargins(dp(3), dp(4), dp(3), dp(7))
        card.layoutParams = p
        return card
    }

    private fun refreshFinance() {
        if (!::financePage.isInitialized) return
        while (financePage.childCount > 1) financePage.removeViewAt(1)
        val costs = records.sumOf { it.cost }
        val income = records.sumOf { it.income }
        val net = costs - income
        financePage.addView(metric("إجمالي تكاليف المعالجة والنقل", "${fmt(costs)} دج"), matchWrap())
        financePage.addView(metric("إجمالي مداخيل التثمين والبيع", "${fmt(income)} دج"), matchWrap())
        financePage.addView(metric("صافي التكاليف (التكاليف - المداخيل)", "${fmt(net)} دج"), matchWrap())
        financePage.addView(actionButton("＋ إضافة عملية مالية عبر تسجيل النفايات") { showPage("form") }, matchWrap())
        financePage.addView(TextView(this).apply {
            text = "أدخل التكلفة والمداخيل عند تسجيل كل عملية. القيم هنا مجموع ما سجلته فقط، وليست بيانات مصنع حقيقية."
            textSize = 14f; setTextColor(0xFF795500.toInt()); setPadding(dp(12), dp(12), dp(12), dp(12)); setBackgroundColor(0xFFFFF2CC.toInt())
        }, matchWrap())
    }

    private fun showTypeSummary() {
        val lines = types.mapNotNull { type ->
            val subset = records.filter { it.type == type }
            if (subset.isEmpty()) null else "$type: إجمالي ${fmt(subset.sumOf { it.quantity })} كغ، مسترجع ${fmt(subset.sumOf { it.recovered })} كغ"
        }
        AlertDialog.Builder(this).setTitle("ملخص أنواع النفايات")
            .setMessage(if (lines.isEmpty()) "لا توجد بيانات بعد." else lines.joinToString("\n\n"))
            .setPositiveButton("إغلاق", null).show()
    }

    private fun backupData() {
        val send = Intent(Intent.ACTION_SEND).apply {
            type = "application/json"
            putExtra(Intent.EXTRA_SUBJECT, "ZAKARY GRD - نسخة احتياطية")
            putExtra(Intent.EXTRA_TEXT, toJson().toString())
        }
        startActivity(Intent.createChooser(send, "حفظ أو مشاركة النسخة الاحتياطية"))
    }

    private fun importBackup() {
        val input = EditText(this).apply { hint="الصق محتوى JSON للنسخة الاحتياطية"; minLines=5; gravity=Gravity.TOP or Gravity.RIGHT }
        AlertDialog.Builder(this).setTitle("استيراد نسخة احتياطية")
            .setMessage("انسخ محتوى النسخة الاحتياطية والصقه هنا. ستتم إضافة السجلات الجديدة دون تكرار المعرفات.")
            .setView(input).setNegativeButton("إلغاء", null)
            .setPositiveButton("استيراد") { _, _ ->
                try {
                    val a = JSONArray(input.text.toString().trim())
                    var added = 0
                    for (i in 0 until a.length()) {
                        val o = a.getJSONObject(i)
                        val id = o.optLong("id", System.currentTimeMillis()+i)
                        if (records.none { it.id == id }) {
                            records.add(readRecord(o, id)); added++
                        }
                    }
                    persistRecords(); renderRecords(); refreshDashboard(); refreshFinance()
                    Toast.makeText(this, "تم استيراد $added سجل", Toast.LENGTH_LONG).show()
                } catch (_: Exception) {
                    Toast.makeText(this, "تعذر قراءة النسخة. تأكد من نسخ JSON كاملًا.", Toast.LENGTH_LONG).show()
                }
            }.show()
    }

    private fun toJson(): JSONArray {
        val a = JSONArray()
        records.forEach { r -> a.put(JSONObject().apply {
            put("id",r.id); put("date",r.date); put("type",r.type); put("department",r.department)
            put("quantity",r.quantity); put("recovered",r.recovered); put("destination",r.destination)
            put("cost",r.cost); put("income",r.income); put("note",r.note)
        }) }
        return a
    }

    private fun readRecord(o: JSONObject, id: Long) = WasteRecord(id, o.optString("date"), o.optString("type"),
        o.optString("department"), o.optDouble("quantity"), o.optDouble("recovered"), o.optString("destination"),
        o.optDouble("cost"), o.optDouble("income"), o.optString("note"))

    private fun persistRecords() {
        getSharedPreferences("zakary_grd", MODE_PRIVATE).edit().putString("records", toJson().toString()).apply()
    }

    private fun loadRecords() {
        try {
            val a = JSONArray(getSharedPreferences("zakary_grd", MODE_PRIVATE).getString("records", "[]") ?: "[]")
            for (i in 0 until a.length()) records.add(readRecord(a.getJSONObject(i), a.getJSONObject(i).optLong("id")))
        } catch (_: Exception) { records.clear() }
    }

    private fun shareCsv() {
        val csv = buildString {
            append("Date,Type,Department,Quantity_kg,Recovered_kg,Destination,Cost_DZD,Income_DZD,Notes\n")
            records.forEach { r ->
                append(listOf(r.date,r.type,r.department,fmt(r.quantity),fmt(r.recovered),r.destination,fmt(r.cost),fmt(r.income),r.note)
                    .joinToString(",") { "\"" + it.replace("\"", "\"\"") + "\"" })
                append("\n")
            }
        }
        val send = Intent(Intent.ACTION_SEND).apply { type="text/csv"; putExtra(Intent.EXTRA_SUBJECT,"ZAKARY GRD - Rapport"); putExtra(Intent.EXTRA_TEXT,csv) }
        startActivity(Intent.createChooser(send, "تصدير أو مشاركة التقرير"))
    }

    private fun number(e: EditText, default: Double? = null): Double? {
        val s = e.text.toString().trim().replace(',', '.')
        if (s.isBlank() && default != null) return default
        return s.toDoubleOrNull()
    }
    private fun today() = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
    private fun fmt(v: Double) = String.format(Locale.US, "%.2f", v)
    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()
    private fun matchWrap() = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
}

class HorizontalScrollViewCompat(context: android.content.Context) : android.widget.HorizontalScrollView(context) {
    init { isHorizontalScrollBarEnabled = false }
}
