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
    private lateinit var dashboard: TextView
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
            setPadding(dp(18), dp(18), dp(18), dp(24))
            layoutDirection = View.LAYOUT_DIRECTION_RTL
            setBackgroundColor(0xFFF3F6FA.toInt())
        }
        scroll.addView(root)
        setContentView(scroll)

        val hero = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(24), dp(20), dp(24))
            setBackgroundColor(0xFF087F8C.toInt())
        }
        val brand = TextView(this).apply {
            text = "ZAKARY GRD  ♻️"
            textSize = 28f
            setTypeface(null, Typeface.BOLD)
            setTextColor(0xFFFFFFFF.toInt())
            gravity = Gravity.RIGHT
        }
        val subtitle = TextView(this).apply {
            text = "نظام تسيير واسترجاع النفايات الصناعية"
            textSize = 19f
            setTextColor(0xFFE7FAFC.toInt())
            gravity = Gravity.RIGHT
            setPadding(0, dp(10), 0, 0)
        }
        hero.addView(brand)
        hero.addView(subtitle)
        root.addView(hero, matchWrap())

        dashboard = TextView(this).apply {
            textSize = 15f
            setTextColor(0xFF243246.toInt())
            setPadding(dp(16), dp(16), dp(16), dp(16))
            setBackgroundColor(0xFFFFFFFF.toInt())
        }
        val dashParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        dashParams.topMargin = dp(16)
        root.addView(dashboard, dashParams)

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
        button("＋ حفظ العملية") { saveRecord() }
        button("↓ تصدير تقرير Excel/CSV") { shareCsv() }

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
        dashboard.text = "لوحة القيادة\n\nعدد السجلات:  ${records.size}\n\nإجمالي الكمية:  ${fmt(total)} كغ\n\nالكمية المسترجعة:  ${fmt(recovered)} كغ\n\nنسبة الاسترجاع:  ${String.format(Locale.US, "%.1f", rate)}%"
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
