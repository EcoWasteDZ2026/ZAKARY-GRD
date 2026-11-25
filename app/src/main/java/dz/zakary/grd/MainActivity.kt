package dz.zakary.grd

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
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
    private lateinit var safiraPage: LinearLayout
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
            setBackgroundColor(0xFFF4F8F5.toInt())
        }
        scroll.addView(root)
        setContentView(scroll)

        val top = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            layoutDirection = View.LAYOUT_DIRECTION_RTL
            setPadding(dp(16), dp(12), dp(16), dp(12))
            background = roundedPanel(0xFF087A45.toInt(), 22)
        }
        val brand = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.RIGHT or Gravity.CENTER_VERTICAL
        }
        brand.addView(TextView(this).apply {
            text = "ZAKARY GRD"
            textSize = 23f
            setTypeface(null, Typeface.BOLD)
            setTextColor(0xFFFFFFFF.toInt())
        }, matchWrap())
        brand.addView(TextView(this).apply {
            text = "GESTION & RÉCUPÉRATION DES DÉCHETS"
            textSize = 9f
            setTextColor(0xFFE0F5E9.toInt())
        }, matchWrap())
        top.addView(brand, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
        top.addView(TextView(this).apply {
            text = "♻"
            textSize = 38f
            gravity = Gravity.CENTER
            setTextColor(0xFFFFFFFF.toInt())
            setPadding(dp(8), 0, dp(8), 0)
        }, LinearLayout.LayoutParams(dp(58), dp(58)))
        root.addView(top, matchWrap())

        val hero = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutDirection = View.LAYOUT_DIRECTION_RTL
            setPadding(dp(18), dp(19), dp(18), dp(19))
            background = roundedPanel(0xFF075B3A.toInt(), 20)
        }
        hero.addView(TextView(this).apply {
            text = "إدارة ذكية للنفايات الصناعية"
            textSize = 21f
            setTypeface(null, Typeface.BOLD)
            setTextColor(0xFFFFFFFF.toInt())
            gravity = Gravity.RIGHT
        }, matchWrap())
        hero.addView(TextView(this).apply {
            text = "من التسجيل والمتابعة إلى الاسترجاع والتقارير"
            textSize = 14f
            setTextColor(0xFFE0F5E9.toInt())
            gravity = Gravity.RIGHT
            setPadding(0, dp(7), 0, dp(9))
        }, matchWrap())
        hero.addView(TextView(this).apply {
            text = "HENKEL  •  ZAKARY GRD"
            textSize = 12f
            setTypeface(null, Typeface.BOLD)
            setTextColor(0xFFB8F06A.toInt())
            gravity = Gravity.RIGHT
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
            Triple("التقارير والنسخ", "reports", "⇩"),
            Triple("نظام SAFIRA", "safira", "▣")
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

        safiraPage = newPage("نظام SAFIRA")
        buildSafiraPage()
        pages["safira"] = safiraPage

        refreshDashboard()
        refreshFinance()
    }

    private fun roundedPanel(color: Int, radiusDp: Int): GradientDrawable {
        return GradientDrawable().apply {
            setColor(color)
            cornerRadius = dp(radiusDp).toFloat()
        }
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


    private fun buildSafiraPage() {
        safiraPage.addView(TextView(this).apply {
            text = "SAFIRA — جداول جرد ومتابعة النفايات حسب التصنيفات الظاهرة في وثائق الموقع."
            textSize = 14f
            setTextColor(0xFF52645D.toInt())
            setPadding(dp(12), dp(12), dp(12), dp(12))
            setBackgroundColor(0xFFE8F5EF.toInt())
        }, matchWrap())

        safiraPage.addView(TextView(this).apply {
            text = "Waste Types (Production Sites) — 2026"
            textSize = 18f
            setTypeface(null, Typeface.BOLD)
            setTextColor(0xFF173650.toInt())
            setPadding(0, dp(14), 0, dp(7))
        }, matchWrap())

        val selections = listOf(
            "Production Waste" to true,
            "Obsolete - Non-Production" to true,
            "Municipal waste" to true,
            "Wastewater related Waste" to false,
            "Construction Waste" to true
        )
        selections.forEach { (label, selected) ->
            safiraPage.addView(TextView(this).apply {
                text = (if (selected) "☑  " else "☐  ") + label
                textSize = 14f
                setTextColor(if (selected) 0xFF247A5A.toInt() else 0xFF7A8580.toInt())
                setPadding(dp(8), dp(5), dp(8), dp(5))
            }, matchWrap())
        }

        addSafiraTable(
            "Production Waste",
            "جدول نفايات الإنتاج — البيانات المنقولة من الصورة كما هي.",
            listOf("Waste item", "Waste category", "Treatment / destination"),
            listOf(
                listOf("Packaging Film", "Plastic Waste", "Recycling"),
                listOf("Plastic Big Bag", "Plastic Waste", "Recycling"),
                listOf("Metal Barrel", "Mixed/Contaminated Packaging", "Incineration with energy recovery"),
                listOf("Wood Pallet Waste", "Wood", "Recycling"),
                listOf("laboratory waste", "Chemical Waste", "Incineration with energy recovery"),
                listOf("Contaminated PPE waste", "Chemical Waste", "Incineration with energy recovery")
            ),
            "production_waste"
        )

        addSafiraTable(
            "Obsolete - Non-Production",
            "نفايات ومواد متقادمة غير مرتبطة بالإنتاج. لا تظهر صفوف تفصيلية لهذا الجدول في الصور المرسلة.",
            listOf("Waste item", "Material / category", "Field 3", "Treatment / destination", "Field 5"),
            emptyList(),
            "obsolete_non_production"
        )

        addSafiraTable(
            "Municipal waste",
            "النفايات البلدية. لا تظهر صفوف تفصيلية لهذا الجدول في الصور المرسلة.",
            listOf("Waste item", "Material / category", "Field 3", "Treatment / destination", "Field 5"),
            emptyList(),
            "municipal_waste"
        )

        addSafiraTable(
            "Wastewater related Waste",
            "النفايات المرتبطة بالمياه المستعملة. هذا التصنيف غير محدد في شاشة الاختيار المرسلة لعام 2026.",
            listOf("Waste item", "Material / category", "Field 3", "Treatment / destination", "Field 5"),
            emptyList(),
            "wastewater_related"
        )

        addSafiraTable(
            "Construction Waste",
            "نفايات البناء — الصفوف الظاهرة في الجدول الأصلي.",
            listOf("Waste item", "Material / type", "Field 3", "Treatment / destination", "Field 5"),
            listOf(
                listOf("Construction waste", "Concrete, bricks, tiles and ceramics", "No", "Landfill", "No"),
                listOf("Metallic Waste", "Metals (and their alloys)", "No", "Recycling", "No")
            ),
            "construction_waste"
        )

        safiraPage.addView(TextView(this).apply {
            text = "Material for Recycling"
            textSize = 18f
            setTypeface(null, Typeface.BOLD)
            setTextColor(0xFF173650.toInt())
            setPadding(0, dp(16), 0, dp(6))
        }, matchWrap())

        addSafiraTable(
            "High-Quality Outbound Recycling",
            "المواد الموجهة لإعادة التدوير الخارجي عالية الجودة.",
            listOf("Material", "Material category", "Field 3", "Destination / route", "Field 5"),
            listOf(listOf("Carton", "Paper and Cardboard", "No", "High-Quality Outbound Recycling", "No")),
            "high_quality_outbound_recycling"
        )

        addSafiraTable(
            "Reuse material",
            "المواد القابلة لإعادة الاستخدام.",
            listOf("Material", "Material category", "Reuse route"),
            listOf(listOf("Pallet conforme", "Wood", "Reused material")),
            "reuse_material"
        )

        safiraPage.addView(TextView(this).apply {
            text = "ملاحظة مهمة: عناوين بعض الأعمدة لم تكن ظاهرة في الصور؛ لذلك أبقينا الخانات غير الواضحة باسم Field 3 وField 5 دون افتراض معناها. يمكنك تعديل الصفوف وإضافة صفوف جديدة، وتحفظ التغييرات على الهاتف."
            textSize = 13f
            setTextColor(0xFF795500.toInt())
            setPadding(dp(12), dp(12), dp(12), dp(12))
            setBackgroundColor(0xFFFFF2CC.toInt())
        }, matchWrap())
    }

    private fun addSafiraTable(
        title: String,
        subtitle: String,
        headers: List<String>,
        initialRows: List<List<String>>,
        storageKey: String
    ) {
        val prefs = getSharedPreferences("zakary_grd_safira", MODE_PRIVATE)
        val rows = mutableListOf<MutableList<String>>()
        val saved = prefs.getString(storageKey, null)
        if (saved != null) {
            try {
                val array = JSONArray(saved)
                for (i in 0 until array.length()) {
                    val rowArray = array.getJSONArray(i)
                    val row = mutableListOf<String>()
                    for (j in headers.indices) row.add(if (j < rowArray.length()) rowArray.optString(j) else "")
                    rows.add(row)
                }
            } catch (_: Exception) { rows.clear() }
        } else {
            initialRows.forEach { source -> rows.add(source.toMutableList()) }
            saveSafiraRows(storageKey, rows)
        }

        val card = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutDirection = View.LAYOUT_DIRECTION_RTL
            setPadding(dp(11), dp(10), dp(11), dp(10))
            background = roundedPanel(0xFFFFFFFF.toInt(), 14)
        }
        card.addView(TextView(this).apply {
            text = title
            textSize = 17f
            setTypeface(null, Typeface.BOLD)
            setTextColor(0xFF247A5A.toInt())
        }, matchWrap())
        card.addView(TextView(this).apply {
            text = subtitle
            textSize = 12f
            setTextColor(0xFF65736D.toInt())
            setPadding(0, dp(4), 0, dp(8))
        }, matchWrap())

        val tableHolder = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        fun renderTable() {
            tableHolder.removeAllViews()
            val horizontal = android.widget.HorizontalScrollView(this).apply {
                isHorizontalScrollBarEnabled = true
            }
            val table = android.widget.TableLayout(this).apply {
                isStretchAllColumns = false
                setPadding(0, 0, 0, dp(4))
            }
            fun cell(textValue: String, header: Boolean = false): TextView = TextView(this).apply {
                text = textValue
                textSize = if (header) 12f else 13f
                setTypeface(null, if (header) Typeface.BOLD else Typeface.NORMAL)
                setTextColor(if (header) 0xFFFFFFFF.toInt() else 0xFF243746.toInt())
                gravity = Gravity.CENTER
                minWidth = dp(112)
                setPadding(dp(8), dp(9), dp(8), dp(9))
                setBackgroundColor(if (header) 0xFF247A5A.toInt() else 0xFFF5F8F6.toInt())
            }
            val head = android.widget.TableRow(this)
            headers.forEach { head.addView(cell(it, true)) }
            head.addView(cell("Actions", true))
            table.addView(head)
            if (rows.isEmpty()) {
                val emptyRow = android.widget.TableRow(this)
                emptyRow.addView(cell("No entries yet"))
                table.addView(emptyRow)
            }
            rows.forEachIndexed { index, values ->
                val tr = android.widget.TableRow(this)
                values.forEach { tr.addView(cell(it.ifBlank { "—" })) }
                val actions = LinearLayout(this).apply {
                    orientation = LinearLayout.VERTICAL
                    setPadding(dp(4), dp(3), dp(4), dp(3))
                }
                actions.addView(actionButton("Edit") { editSafiraRow(title, headers, values, index) { updated ->
                    rows[index] = updated.toMutableList()
                    saveSafiraRows(storageKey, rows)
                    renderTable()
                } }, LinearLayout.LayoutParams(dp(88), dp(42)))
                actions.addView(actionButton("Delete") {
                    AlertDialog.Builder(this@MainActivity)
                        .setTitle("Delete row")
                        .setMessage("Delete this row from $title?")
                        .setNegativeButton("Cancel", null)
                        .setPositiveButton("Delete") { _, _ ->
                            rows.removeAt(index)
                            saveSafiraRows(storageKey, rows)
                            renderTable()
                        }.show()
                }, LinearLayout.LayoutParams(dp(88), dp(42)))
                tr.addView(actions)
                table.addView(tr)
            }
            horizontal.addView(table)
            tableHolder.addView(horizontal, matchWrap())
        }
        renderTable()
        card.addView(tableHolder, matchWrap())
        card.addView(actionButton("＋ Add row") {
            editSafiraRow(title, headers, List(headers.size) { "" }, -1) { updated ->
                rows.add(updated.toMutableList())
                saveSafiraRows(storageKey, rows)
                renderTable()
            }
        }, matchWrap())
        val p = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        p.bottomMargin = dp(10)
        safiraPage.addView(card, p)
    }

    private fun editSafiraRow(
        title: String,
        headers: List<String>,
        current: List<String>,
        index: Int,
        onSave: (List<String>) -> Unit
    ) {
        val form = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutDirection = View.LAYOUT_DIRECTION_RTL
            setPadding(dp(8), dp(4), dp(8), dp(4))
        }
        val inputs = headers.mapIndexed { i, header ->
            EditText(this).apply {
                hint = header
                textSize = 14f
                setSingleLine(false)
                minLines = 1
                setText(current.getOrNull(i).orEmpty())
                setPadding(dp(9), dp(5), dp(9), dp(5))
                form.addView(this, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(48)).apply {
                    bottomMargin = dp(5)
                })
            }
        }
        val scroll = ScrollView(this).apply { addView(form) }
        AlertDialog.Builder(this)
            .setTitle(if (index < 0) "Add row — $title" else "Edit row — $title")
            .setView(scroll)
            .setNegativeButton("Cancel", null)
            .setPositiveButton("Save") { _, _ -> onSave(inputs.map { it.text.toString().trim() }) }
            .show()
    }

    private fun saveSafiraRows(storageKey: String, rows: List<List<String>>) {
        val array = JSONArray()
        rows.forEach { row ->
            val item = JSONArray()
            row.forEach { item.put(it) }
            array.put(item)
        }
        getSharedPreferences("zakary_grd_safira", MODE_PRIVATE)
            .edit().putString(storageKey, array.toString()).apply()
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
            if (subset.isNotEmpty()) {
                val qty = subset.sumOf { it.quantity }
                val rec = subset.sumOf { it.recovered }
                val costType = subset.sumOf { it.cost }
                val incomeType = subset.sumOf { it.income }
                val netType = costType - incomeType
                val perKg = if (qty > 0) costType / qty else 0.0
                val typeCard = LinearLayout(this).apply {
                    orientation = LinearLayout.VERTICAL
                    layoutDirection = View.LAYOUT_DIRECTION_RTL
                    setPadding(dp(10), dp(9), dp(10), dp(9))
                    setBackgroundColor(0xFFF1F6F5.toInt())
                }
                typeCard.addView(TextView(this).apply {
                    text = "♻ $type"
                    textSize = 16f
                    setTypeface(null, Typeface.BOLD)
                    setTextColor(0xFF247A5A.toInt())
                }, matchWrap())
                typeCard.addView(TextView(this).apply {
                    text = "منتج: ${fmt(qty)} كغ  |  مسترجع: ${fmt(rec)} كغ\nالتكاليف: ${fmt(costType)} دج\nالمداخيل: ${fmt(incomeType)} دج  |  صافي التكلفة: ${fmt(netType)} دج\nتكلفة الكيلوغرام الواحد: ${fmt(perKg)} دج/كغ"
                    textSize = 14f
                    setTextColor(0xFF243746.toInt())
                    setPadding(0, dp(5), 0, 0)
                }, matchWrap())
                val cp = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
                cp.bottomMargin = dp(7)
                typesBox.addView(typeCard, cp)
            }
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
            text = "تفصيل مالي مستقل لكل نوع نفايات"
            textSize = 19f; setTypeface(null, Typeface.BOLD); setTextColor(0xFF173650.toInt()); setPadding(0, dp(14), 0, dp(8))
        }, matchWrap())
        types.distinct().forEach { type ->
            val subset = records.filter { it.type == type }
            if (subset.isNotEmpty()) {
                val qty = subset.sumOf { it.quantity }
                val recovered = subset.sumOf { it.recovered }
                val costsForType = subset.sumOf { it.cost }
                val incomeForType = subset.sumOf { it.income }
                val netForType = costsForType - incomeForType
                val costPerKg = if (qty > 0) costsForType / qty else 0.0
                val recoveredCostPerKg = if (recovered > 0) costsForType / recovered else 0.0
                val box = LinearLayout(this).apply {
                    orientation = LinearLayout.VERTICAL
                    layoutDirection = View.LAYOUT_DIRECTION_RTL
                    setPadding(dp(13), dp(12), dp(13), dp(12))
                    setBackgroundColor(0xFFFFFFFF.toInt())
                }
                box.addView(TextView(this).apply {
                    text = "♻ $type"
                    textSize = 18f; setTypeface(null, Typeface.BOLD); setTextColor(0xFF247A5A.toInt())
                }, matchWrap())
                box.addView(TextView(this).apply {
                    text = "الكمية المنتجة: ${fmt(qty)} كغ\nالكمية المسترجعة: ${fmt(recovered)} كغ\nتكاليف هذا النوع: ${fmt(costsForType)} دج\nمداخيل هذا النوع: ${fmt(incomeForType)} دج\nصافي تكلفة هذا النوع: ${fmt(netForType)} دج\nتكلفة كل 1 كغ منتج: ${fmt(costPerKg)} دج/كغ\nتكلفة كل 1 كغ مسترجع (مؤشر تقريبي): ${fmt(recoveredCostPerKg)} دج/كغ"
                    textSize = 14f; setTextColor(0xFF243746.toInt()); setPadding(0, dp(7), 0, 0)
                }, matchWrap())
                val bp = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
                bp.bottomMargin = dp(9)
                financePage.addView(box, bp)
            }
        }
        financePage.addView(TextView(this).apply {
            text = "تُحسب تكلفة كل نوع وحده من العمليات المسجلة لذلك النوع، ولا تُوزَّع تكلفة نوع على الأنواع الأخرى. تكلفة الكيلوغرام = تكاليف النوع ÷ كميته المنتجة. أدخل تكاليف النقل والمعالجة لكل سجل بدقة، فهذه الحسابات تعتمد على البيانات التي تدخلها."
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
