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
    val id: Long,
    val date: String,
    val type: String,
    val department: String,
    val quantity: Double,
    val recovered: Double,
    val destination: String,
    val note: String
)

class MainActivity : Activity() {
    private val records = mutableListOf<WasteRecord>()
    private val types = listOf("بلاستيك", "ورق وكرتون", "معدن", "نفايات عضوية", "نفايات كيميائية", "زيوت مستعملة", "حمأة", "أخرى")
    private val departments = listOf("الإنتاج", "المخازن", "الصيانة", "المخبر", "معالجة المياه", "الإدارة", "أخرى")
    private val destinations = listOf("إعادة التدوير", "التثمين", "المعالجة", "التخلص النهائي", "مخزن مؤقت", "أخرى")
    private lateinit var root: LinearLayout
    private lateinit var dashboard: LinearLayout
    private lateinit var listArea: LinearLayout
    private lateinit var dateInput: EditText
    private lateinit var quantityInput: EditText
    private lateinit var recoveredInput: EditText
    private lateinit var noteInput: EditText
    private lateinit var typeSpinner: Spinner
    private lateinit var departmentSpinner: Spinner
    private lateinit var destinationSpinner: Spinner

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        loadRecords()
        buildScreen()
        renderRecords()
    }

    private fun buildScreen() {
        val scroll = ScrollView(this)
        root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(18), dp(16), dp(18), dp(26))
            layoutDirection = View.LAYOUT_DIRECTION_RTL
            setBackgroundColor(0xFFF3F6FA.toInt())
        }
        scroll.addView(root)
        setContentView(scroll)

        val topBar = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(2), dp(2), dp(2), dp(12))
        }
        val appTitle = TextView(this).apply {
            text = "ZAKARY GRD  |  تسيير النفايات الصناعية"
            textSize = 18f
            setTypeface(null, Typeface.BOLD)
            setTextColor(0xFF172B3A.toInt())
            gravity = Gravity.RIGHT or Gravity.CENTER_VERTICAL
        }
        topBar.addView(appTitle, LinearLayout.LayoutParams(0, dp(48), 1f))
        root.addView(topBar, matchWrap())

        val hero = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(22), dp(25), dp(22), dp(25))
            setBackgroundColor(0xFF087F8C.toInt())
        }
        val brand = TextView(this).apply {
            text = "ZAKARY GRD   ♻️"
            textSize = 29f
            setTypeface(null, Typeface.BOLD)
            setTextColor(0xFFFFFFFF.toInt())
            gravity = Gravity.RIGHT
        }
        val subtitle = TextView(this).apply {
            text = "نظام تسيير واسترجاع النفايات الصناعية | نسخة محسّنة"
            textSize = 17f
            setTextColor(0xFFE7FAFC.toInt())
            gravity = Gravity.RIGHT
            setPadding(0, dp(12), 0, 0)
        }
        hero.addView(brand)
        hero.addView(subtitle)
        root.addView(hero, matchWrap())

        dashboard = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(2), dp(2), dp(2), dp(2))
        }
        val dashParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        dashParams.topMargin = dp(16)
        root.addView(dashboard, dashParams)

        section("إجراءات سريعة")
        val actionsRow1 = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; layoutDirection = View.LAYOUT_DIRECTION_RTL }
        actionsRow1.addView(actionTile("تسجيل نفايات", "＋") { quantityInput.requestFocus() }, LinearLayout.LayoutParams(0, dp(82), 1f))
        actionsRow1.addView(actionTile("تصدير Excel/CSV", "↓") { shareCsv() }, LinearLayout.LayoutParams(0, dp(82), 1f))
        root.addView(actionsRow1, matchWrap())
        val actionsRow2 = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; layoutDirection = View.LAYOUT_DIRECTION_RTL }
        actionsRow2.addView(actionTile("حفظ نسخة احتياطية", "▣") { backupData() }, LinearLayout.LayoutParams(0, dp(72), 1f))
        actionsRow2.addView(actionTile("استيراد نسخة", "↑") { importBackup() }, LinearLayout.LayoutParams(0, dp(72), 1f))
        root.addView(actionsRow2, matchWrap())

        val info = TextView(this).apply {
            text = "تُحفظ السجلات على هذا الجهاز. استخدم «حفظ نسخة احتياطية» بانتظام، خصوصًا قبل حذف بيانات التطبيق أو تغيير الهاتف."
            textSize = 15f
            setTextColor(0xFF244F9B.toInt())
            setPadding(dp(14), dp(14), dp(14), dp(14))
            setBackgroundColor(0xFFEDF4FF.toInt())
            gravity = Gravity.RIGHT
        }
        val infoParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        infoParams.topMargin = dp(12)
        root.addView(info, infoParams)

        section("الأقسام")
        val tabs = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; layoutDirection = View.LAYOUT_DIRECTION_RTL }
        tabs.addView(actionTile("لوحة القيادة", "▦") { scroll.smoothScrollTo(0, 0) }, LinearLayout.LayoutParams(0, dp(58), 1f))
        tabs.addView(actionTile("تسجيل نفايات", "＋") { scroll.post { scroll.smoothScrollTo(0, dateInput.top) } }, LinearLayout.LayoutParams(0, dp(58), 1f))
        tabs.addView(actionTile("السجلات", "☷") { scroll.post { scroll.smoothScrollTo(0, listArea.top) } }, LinearLayout.LayoutParams(0, dp(58), 1f))
        root.addView(tabs, matchWrap())

        section("إجراءات سريعة")
        button("＋ تسجيل نفايات جديدة") { dateInput.requestFocus(); quantityInput.requestFocus() }
        button("↓ تصدير التقرير Excel/CSV") { shareCsv() }
        button("حفظ نسخة احتياطية") { backupData() }
        button("↑ استيراد نسخة احتياطية") { importBackup() }

        section("تسجيل عملية جديدة")
        dateInput = field("التاريخ (مثال: 2026-10-19)").apply {
            setText(SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date()))
        }
        typeSpinner = spinner(types)
        departmentSpinner = spinner(departments)
        quantityInput = field("الكمية الإجمالية بالكيلوغرام")
        recoveredInput = field("الكمية المسترجعة بالكيلوغرام")
        destinationSpinner = spinner(destinations)
        noteInput = field("ملاحظات (اختياري)")
        button("حفظ العملية") { saveRecord() }

        section("سجل العمليات")
        listArea = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        root.addView(listArea, matchWrap())
        refreshDashboard()
    }

    private fun text(value: String, size: Int, bold: Boolean = false, color: Int = 0xFF243238.toInt()): TextView {
        val view = TextView(this).apply {
            text = value
            textSize = size.toFloat()
            setTextColor(color)
            if (bold) setTypeface(null, Typeface.BOLD)
            gravity = Gravity.RIGHT
            setPadding(0, dp(5), 0, dp(5))
        }
        root.addView(view, matchWrap())
        return view
    }

    private fun section(value: String) {
        val t = TextView(this).apply {
            text = value
            textSize = 18f
            setTypeface(null, Typeface.BOLD)
            setTextColor(0xFF123B53.toInt())
            gravity = Gravity.RIGHT
            setPadding(0, dp(20), 0, dp(10))
        }
        root.addView(t, matchWrap())
    }

    private fun field(hintText: String): EditText {
        val e = EditText(this).apply {
            hint = hintText
            textSize = 15f
            setSingleLine(true)
            gravity = Gravity.RIGHT or Gravity.CENTER_VERTICAL
            layoutDirection = View.LAYOUT_DIRECTION_RTL
            setPadding(dp(10), dp(4), dp(10), dp(4))
            setBackgroundColor(0xFFFFFFFF.toInt())
        }
        val p = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(52))
        p.bottomMargin = dp(8)
        root.addView(e, p)
        return e
    }

    private fun spinner(items: List<String>): Spinner {
        val s = Spinner(this).apply {
            adapter = ArrayAdapter(this@MainActivity, android.R.layout.simple_spinner_dropdown_item, items)
            layoutDirection = View.LAYOUT_DIRECTION_RTL
            setBackgroundColor(0xFFFFFFFF.toInt())
        }
        val p = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(48))
        p.bottomMargin = dp(8)
        root.addView(s, p)
        return s
    }

    private fun button(label: String, action: () -> Unit) {
        val b = Button(this).apply {
            text = label
            isAllCaps = false
            textSize = 16f
            setTextColor(if (label.contains("تصدير")) 0xFFFFFFFF.toInt() else 0xFF183246.toInt())
            setBackgroundColor(if (label.contains("تصدير")) 0xFF087F8C.toInt() else 0xFFE1E8F0.toInt())
            setPadding(dp(10), dp(8), dp(10), dp(8))
            setOnClickListener { action() }
        }
        val p = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(58))
        p.bottomMargin = dp(10)
        root.addView(b, p)
    }

    private fun saveRecord() {
        val date = dateInput.text.toString().trim()
        val quantity = quantityInput.text.toString().trim().replace(',', '.').toDoubleOrNull()
        val recovered = recoveredInput.text.toString().trim().replace(',', '.').toDoubleOrNull()
        if (date.isBlank() || quantity == null || recovered == null || quantity <= 0 || recovered < 0 || recovered > quantity) {
            Toast.makeText(this, "تحقق من التاريخ والكميات. يجب أن تكون الكمية المسترجعة بين صفر والكمية الإجمالية.", Toast.LENGTH_LONG).show()
            return
        }
        val record = WasteRecord(
            System.currentTimeMillis(), date, typeSpinner.selectedItem.toString(),
            departmentSpinner.selectedItem.toString(), quantity, recovered,
            destinationSpinner.selectedItem.toString(), noteInput.text.toString().trim()
        )
        records.add(0, record)
        persistRecords()
        quantityInput.setText("")
        recoveredInput.setText("")
        noteInput.setText("")
        renderRecords()
        Toast.makeText(this, "تم حفظ العملية على هذا الهاتف", Toast.LENGTH_SHORT).show()
    }

    private fun renderRecords() {
        listArea.removeAllViews()
        if (records.isEmpty()) {
            val empty = TextView(this).apply {
                text = "لا توجد عمليات مسجلة بعد."
                textSize = 14f
                setPadding(0, dp(8), 0, dp(8))
            }
            listArea.addView(empty)
        }
        records.forEach { r ->
            val card = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                layoutDirection = View.LAYOUT_DIRECTION_RTL
                setPadding(dp(12), dp(8), dp(12), dp(8))
                setBackgroundColor(0xFFFFFFFF.toInt())
            }
            val title = TextView(this).apply {
                text = "${r.type} — ${r.date}"
                textSize = 16f
                setTypeface(null, Typeface.BOLD)
                setTextColor(0xFF135D52.toInt())
            }
            val detail = TextView(this).apply {
                text = "القسم: ${r.department}\nالإجمالي: ${fmt(r.quantity)} كغ | المسترجع: ${fmt(r.recovered)} كغ\nالوجهة: ${r.destination}" +
                    if (r.note.isBlank()) "" else "\nملاحظات: ${r.note}"
                textSize = 14f
            }
            val del = Button(this).apply {
                text = "حذف السجل"
                isAllCaps = false
                setOnClickListener {
                    AlertDialog.Builder(this@MainActivity)
                        .setTitle("حذف العملية")
                        .setMessage("هل تريد حذف هذه العملية من السجل؟")
                        .setNegativeButton("إلغاء", null)
                        .setPositiveButton("حذف") { _, _ ->
                            records.removeAll { it.id == r.id }
                            persistRecords()
                            renderRecords()
                        }.show()
                }
            }
            card.addView(title)
            card.addView(detail)
            card.addView(del)
            val p = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            p.bottomMargin = dp(10)
            listArea.addView(card, p)
        }
        refreshDashboard()
    }

    private fun refreshDashboard() {
        val total = records.sumOf { it.quantity }
        val recovered = records.sumOf { it.recovered }
        val rate = if (total > 0) recovered * 100.0 / total else 0.0
        dashboard.removeAllViews()
        val title = TextView(this).apply {
            text = "لوحة القيادة"
            textSize = 21f
            setTypeface(null, Typeface.BOLD)
            setTextColor(0xFF17334A.toInt())
            gravity = Gravity.RIGHT
            setPadding(0, dp(6), 0, dp(10))
        }
        dashboard.addView(title, matchWrap())
        val row1 = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; layoutDirection = View.LAYOUT_DIRECTION_RTL }
        row1.addView(metricCard("عدد السجلات", records.size.toString()), LinearLayout.LayoutParams(0, dp(118), 1f))
        row1.addView(metricCard("إجمالي الكمية (كغ)", fmt(total)), LinearLayout.LayoutParams(0, dp(118), 1f))
        dashboard.addView(row1, matchWrap())
        val row2 = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; layoutDirection = View.LAYOUT_DIRECTION_RTL }
        row2.addView(metricCard("الكمية المسترجعة (كغ)", fmt(recovered)), LinearLayout.LayoutParams(0, dp(118), 1f))
        row2.addView(metricCard("نسبة الاسترجاع", String.format(Locale.US, "%.1f%%", rate)), LinearLayout.LayoutParams(0, dp(118), 1f))
        dashboard.addView(row2, matchWrap())
    }

    private fun metricCard(label: String, value: String): View {
        val card = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutDirection = View.LAYOUT_DIRECTION_RTL
            setPadding(dp(12), dp(12), dp(12), dp(10))
            setBackgroundColor(0xFFFFFFFF.toInt())
        }
        val heading = TextView(this).apply {
            text = label
            textSize = 14f
            setTextColor(0xFF6A7A8D.toInt())
            gravity = Gravity.RIGHT
        }
        val number = TextView(this).apply {
            text = value
            textSize = 25f
            setTypeface(null, Typeface.BOLD)
            setTextColor(0xFF1D2D42.toInt())
            gravity = Gravity.RIGHT
            setPadding(0, dp(8), 0, 0)
        }
        card.addView(heading)
        card.addView(number)
        val params = LinearLayout.LayoutParams(0, dp(118), 1f)
        params.setMargins(dp(4), dp(4), dp(4), dp(4))
        card.layoutParams = params
        return card
    }

    private fun actionTile(label: String, icon: String, action: () -> Unit): View {
        val b = Button(this).apply {
            text = "$icon  $label"
            isAllCaps = false
            textSize = 14f
            setTextColor(if (label.contains("تصدير")) 0xFFFFFFFF.toInt() else 0xFF213348.toInt())
            setBackgroundColor(if (label.contains("تصدير")) 0xFF087F8C.toInt() else 0xFFE1E8F0.toInt())
            setOnClickListener { action() }
        }
        val p = LinearLayout.LayoutParams(0, dp(72), 1f)
        p.setMargins(dp(4), dp(4), dp(4), dp(4))
        b.layoutParams = p
        return b
    }

    private fun backupData() {
        val array = JSONArray()
        records.forEach { r ->
            array.put(JSONObject().apply {
                put("id", r.id); put("date", r.date); put("type", r.type)
                put("department", r.department); put("quantity", r.quantity)
                put("recovered", r.recovered); put("destination", r.destination); put("note", r.note)
            })
        }
        val send = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, "ZAKARY GRD - Backup")
            putExtra(Intent.EXTRA_TEXT, array.toString())
        }
        startActivity(Intent.createChooser(send, "حفظ النسخة الاحتياطية أو إرسالها"))
    }

    private fun importBackup() {
        val input = EditText(this).apply {
            hint = "الصق محتوى ملف النسخة الاحتياطية هنا"
            minLines = 4
            gravity = Gravity.TOP or Gravity.RIGHT
        }
        AlertDialog.Builder(this)
            .setTitle("استيراد نسخة احتياطية")
            .setMessage("افتح ملف النسخة الاحتياطية وانسخ محتواه، ثم الصقه هنا. سيُضاف المحتوى إلى السجلات الحالية.")
            .setView(input)
            .setNegativeButton("إلغاء", null)
            .setPositiveButton("استيراد") { _, _ ->
                try {
                    val array = JSONArray(input.text.toString().trim())
                    var added = 0
                    for (i in 0 until array.length()) {
                        val o = array.getJSONObject(i)
                        val id = o.optLong("id", System.currentTimeMillis() + i)
                        if (records.none { it.id == id }) {
                            records.add(WasteRecord(id, o.optString("date"), o.optString("type"),
                                o.optString("department"), o.optDouble("quantity"), o.optDouble("recovered"),
                                o.optString("destination"), o.optString("note")))
                            added++
                        }
                    }
                    persistRecords()
                    renderRecords()
                    Toast.makeText(this, "تم استيراد $added سجل", Toast.LENGTH_LONG).show()
                } catch (_: Exception) {
                    Toast.makeText(this, "تعذر قراءة النسخة. تحقق من نسخ محتوى JSON كاملًا.", Toast.LENGTH_LONG).show()
                }
            }.show()
    }

    private fun persistRecords() {
        val array = JSONArray()
        records.forEach { r ->
            array.put(JSONObject().apply {
                put("id", r.id); put("date", r.date); put("type", r.type)
                put("department", r.department); put("quantity", r.quantity)
                put("recovered", r.recovered); put("destination", r.destination); put("note", r.note)
            })
        }
        getSharedPreferences("zakary_grd", MODE_PRIVATE).edit().putString("records", array.toString()).apply()
    }

    private fun loadRecords() {
        val raw = getSharedPreferences("zakary_grd", MODE_PRIVATE).getString("records", "[]") ?: "[]"
        try {
            val a = JSONArray(raw)
            for (i in 0 until a.length()) {
                val o = a.getJSONObject(i)
                records.add(WasteRecord(o.optLong("id"), o.optString("date"), o.optString("type"),
                    o.optString("department"), o.optDouble("quantity"), o.optDouble("recovered"),
                    o.optString("destination"), o.optString("note")))
            }
        } catch (_: Exception) {
            records.clear()
        }
    }

    private fun shareCsv() {
        val csv = buildString {
            append("Date,Type,Department,Quantity_kg,Recovered_kg,Destination,Notes\n")
            records.forEach { r ->
                append(listOf(r.date, r.type, r.department, fmt(r.quantity), fmt(r.recovered), r.destination, r.note)
                    .joinToString(",") { "\"" + it.replace("\"", "\"\"") + "\"" })
                append("\n")
            }
        }
        val send = Intent(Intent.ACTION_SEND).apply {
            type = "text/csv"
            putExtra(Intent.EXTRA_SUBJECT, "ZAKARY GRD - Waste report")
            putExtra(Intent.EXTRA_TEXT, csv)
        }
        startActivity(Intent.createChooser(send, "مشاركة تقرير النفايات"))
    }

    private fun fmt(v: Double): String = String.format(Locale.US, "%.2f", v)
    private fun dp(v: Int): Int = (v * resources.displayMetrics.density).toInt()
    private fun matchWrap() = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
}
