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
import java.util.UUID

data class ContractItem(val name: String, var price: Double, var unit: String, var method: String, var companyName: String = "")

data class WasteRecord(
    val id: String, val date: String, val type: String, val department: String,
    val quantity: Double, val recovered: Double, val destination: String,
    val cost: Double, val income: Double, val note: String, val buyerCompany: String = "", val operationType: String = "بيع مباشر",
    val dispatchReference: String = "", val invoiceNumber: String = "", val invoiceDate: String = "", val paymentStatus: String = "غير محدد", val paymentDate: String = ""
)

class MainActivity : Activity() {
    private val cloud by lazy { SupabaseClient(this) }
    private val records = mutableListOf<WasteRecord>()
    private val productionByMonth = mutableMapOf<String, Double>()
    private lateinit var productionInput: EditText
    private val contractItems = mutableListOf(
        ContractItem("صناديق كرتونية ونفايات الورق", 2.40, "كغ", "بيع", "SNC Groupe EL BADISSIA"),
        ContractItem("الأغلفة البلاستيكية (المطاطية وأغلفة التغليف)", 5.40, "كغ", "بيع", "SNC Groupe EL BADISSIA"),
        ContractItem("أكياس Big Bags", 4.80, "كغ", "بيع", "SNC Groupe EL BADISSIA"),
        ContractItem("براميل معدنية", 216.0, "وحدة", "بيع", "SNC Groupe EL BADISSIA"),
        ContractItem("نفايات المنصات الخشبية وبقايا الخشب", 2.50, "كغ", "بيع", "SNC Groupe EL BADISSIA"),
        ContractItem("أكياس وأغلفة PE ملوثة بالمنظفات وغير قابلة للاسترجاع", 0.0, "حسب الكمية", "غير قابل للبيع")
    )
    private val types get() = contractItems.map { it.name } + listOf("نفايات صناعية أخرى (تُضاف لاحقاً)")
    private fun contractItem(name: String) = contractItems.find { it.name == name }
    private val departments = listOf("الإنتاج", "المخازن", "الصيانة", "المخبر", "معالجة المياه", "الإدارة", "أخرى")
    private val destinations = listOf("خروج إلى الشركة المشترية", "أخرى")
    private val pages = mutableMapOf<String, LinearLayout>()
    private lateinit var root: LinearLayout
    private lateinit var contentHost: LinearLayout
    private lateinit var dashboardPage: LinearLayout
    private lateinit var recordsPage: LinearLayout
    private lateinit var formPage: LinearLayout
    private lateinit var financePage: LinearLayout
    private lateinit var reportsPage: LinearLayout
    private lateinit var contractsPage: LinearLayout
    private lateinit var dashboardStats: LinearLayout
    private lateinit var recordList: LinearLayout
    private lateinit var dateInput: EditText
    private lateinit var quantityInput: EditText
    private lateinit var recoveredInput: EditText
    private lateinit var costInput: EditText
    private lateinit var incomeInput: EditText
    private lateinit var buyerInput: EditText
    private lateinit var operationSpinner: Spinner
    private lateinit var dispatchInput: EditText
    private lateinit var invoiceNumberInput: EditText
    private lateinit var invoiceDateInput: EditText
    private lateinit var paymentDateInput: EditText
    private lateinit var paymentStatusSpinner: Spinner
    private lateinit var noteInput: EditText
    private lateinit var typeSpinner: Spinner
    private lateinit var departmentSpinner: Spinner
    private lateinit var destinationSpinner: Spinner
    private lateinit var searchInput: EditText
    private var currentPage = "dashboard"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        loadRecords()
        loadContracts()
        restoreProduction()
        buildScreen()
        renderRecords()
        showPage("dashboard")
        if (cloud.connected()) syncCloud(false)
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
            text = "HENKEL ALGÉRIE  ♻\nZAKARY GRD"
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
            text = "نظام تسيير واسترجاع النفايات الصناعية — مصنع هنكل، ولاية ميلة"
            textSize = 20f
            setTypeface(null, Typeface.BOLD)
            setTextColor(0xFFFFFFFF.toInt())
            gravity = Gravity.RIGHT
        }, matchWrap())
        hero.addView(TextView(this).apply {
            text = "HENKEL MILA • العقود • التتبع • التثمين • التقارير"
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
            Triple("التقارير والنسخ", "reports", "⇩"),
            Triple("العقود والأسعار", "contracts", "§")
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
        productionInput = field(dashboardPage, "إنتاج المصنع للشهر الحالي بالكيلوغرام", true).apply { setText(loadProduction(today().take(7)).takeIf { it > 0 }?.let { fmt(it) } ?: "") }
        dashboardPage.addView(actionButton("حفظ كمية إنتاج الشهر") { saveProductionMonth() }, matchWrap())
        dashboardStats = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        dashboardPage.addView(dashboardStats, matchWrap())
        val quick = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        quick.addView(actionButton("＋ تسجيل نفايات جديدة") { showPage("form") }, matchWrap())
        quick.addView(actionButton("☷ عرض السجلات") { showPage("records") }, matchWrap())
        quick.addView(actionButton("⇩ تصدير تقرير CSV") { shareCsv() }, matchWrap())
        quick.addView(actionButton("☁ ربط الحساب والمزامنة") { showCloudDialog() }, matchWrap())
        quick.addView(actionButton("⟳ مزامنة البيانات مع الحاسوب") { syncCloud(true) }, matchWrap())
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
        incomeInput = field(formPage, "مداخيل البيع (تحسب آلياً من العقد)", true)
        operationSpinner = spinner(formPage, listOf("توليد/تسجيل الكمية المتولدة", "بيع مباشر"))
        buyerInput = field(formPage, "الشركة المشترية (للبيع فقط)")
        typeSpinner.onItemSelectedListener = object : android.widget.AdapterView.OnItemSelectedListener {
            override fun onNothingSelected(parent: android.widget.AdapterView<*>?) {}
            override fun onItemSelected(parent: android.widget.AdapterView<*>?, view: View?, position: Int, id: Long) {
                val item = contractItem(typeSpinner.selectedItem?.toString() ?: "")
                if (item != null) {
                    buyerInput.setText(if (item.method == "بيع") item.companyName else "")
                    operationSpinner.setSelection(if (item.method == "بيع") 1 else 0)
                    updateIncomePreview()
                }
            }
        }
        quantityInput.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) { updateIncomePreview() }
            override fun afterTextChanged(s: android.text.Editable?) {}
        })
        noteInput = field(formPage, "ملاحظات (اختياري)")
        dispatchInput = field(formPage, "مرجع وصل الخروج / أمر الخروج")
        invoiceNumberInput = field(formPage, "رقم الفاتورة")
        invoiceDateInput = field(formPage, "تاريخ الفاتورة YYYY-MM-DD")
        paymentStatusSpinner = spinner(formPage, listOf("غير محدد", "غير مفوتر", "فاتورة صادرة - غير مدفوع", "مدفوع", "متأخر"))
        paymentDateInput = field(formPage, "تاريخ الدفع YYYY-MM-DD")
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
            text = "ملاحظة: تحفظ البيانات محليًا وتُزامَن مع حساب هنكل المشترك عند الاتصال. احتفظ بنسخة احتياطية دورية، ولا تشارك بيانات الدخول أو مفاتيح الإدارة."
            textSize = 14f
            setTextColor(0xFF795500.toInt())
            setPadding(dp(12), dp(12), dp(12), dp(12))
            setBackgroundColor(0xFFFFF2CC.toInt())
        }, matchWrap())
        pages["reports"] = reportsPage

        contractsPage = newPage("العقود والأسعار")
        contractsPage.addView(actionButton("تعديل أسعار البنود") { editContractPrices() }, matchWrap())
        contractsPage.addView(actionButton("إضافة بند تعاقدي") { addContractItem() }, matchWrap())
        contractsPage.addView(actionButton("عرض شروط العقد") { showContractTerms() }, matchWrap())
        contractsPage.addView(actionButton("مزامنة العقود") { syncCloud(true) }, matchWrap())
        pages["contracts"] = contractsPage

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
        if (name == "contracts") refreshContracts()
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
        val selectedType = typeSpinner.selectedItem.toString()
        val operation = operationSpinner.selectedItem.toString()
        val item = contractItem(selectedType)
        val buyer = buyerInput.text.toString().trim()
        val income = if (operation == "بيع مباشر") {
            if (item == null || item.method != "بيع") {
                Toast.makeText(this, "لا يمكن بيع هذا النوع دون بند بيع في العقد.", Toast.LENGTH_LONG).show(); return
            }
            if (buyer.isBlank() || buyer != item.companyName) { Toast.makeText(this, "الشركة المشترية يجب أن تطابق الشركة المحددة في العقد.", Toast.LENGTH_LONG).show(); return }
            (quantity ?: 0.0) * item.price
        } else 0.0
        if (date.isBlank() || quantity == null || recovered == null || cost == null ||
            quantity <= 0 || recovered < 0 || recovered > quantity || cost < 0) {
            Toast.makeText(this, "تحقق من التاريخ والكميات والتكاليف. المسترجع يجب ألا يتجاوز الإجمالي.", Toast.LENGTH_LONG).show()
            return
        }
        val newId = UUID.randomUUID().toString()
        records.add(0, WasteRecord(newId, date, selectedType,
            departmentSpinner.selectedItem.toString(), quantity, recovered, destinationSpinner.selectedItem.toString(),
            cost, income, noteInput.text.toString().trim(), if (operation == "بيع مباشر") buyer else "", operation,
            dispatchInput.text.toString().trim(), invoiceNumberInput.text.toString().trim(), invoiceDateInput.text.toString().trim(), paymentStatusSpinner.selectedItem.toString(), paymentDateInput.text.toString().trim()))
        persistRecords()
        cloud.markRecordDirty(newId)
        if (cloud.connected()) syncCloud(false)
        clearForm()
        renderRecords()
        refreshDashboard()
        refreshFinance()
        Toast.makeText(this, "تم حفظ العملية بنجاح", Toast.LENGTH_SHORT).show()
        showPage("dashboard")
    }

    private fun updateIncomePreview() {
        if (!::incomeInput.isInitialized || !::typeSpinner.isInitialized || !::quantityInput.isInitialized) return
        val item = contractItem(typeSpinner.selectedItem?.toString() ?: "")
        val q = number(quantityInput, 0.0) ?: 0.0
        val sale = ::operationSpinner.isInitialized && operationSpinner.selectedItem?.toString() == "بيع مباشر"
        incomeInput.setText(if (sale && item?.method == "بيع") fmt(q * item.price) else "0")
        if (sale && item != null && item.method == "بيع" && ::buyerInput.isInitialized && buyerInput.text.isNullOrBlank()) buyerInput.setText(item.companyName)
    }

    private fun clearForm() {
        dateInput.setText(today())
        quantityInput.setText("")
        recoveredInput.setText("")
        costInput.setText("")
        incomeInput.setText("0")
        buyerInput.setText("")
        operationSpinner.setSelection(0)
        noteInput.setText("")
        dispatchInput.setText("")
        invoiceNumberInput.setText("")
        invoiceDateInput.setText("")
        paymentDateInput.setText("")
        paymentStatusSpinner.setSelection(0)
        typeSpinner.setSelection(0)
        departmentSpinner.setSelection(0)
        destinationSpinner.setSelection(0)
    }

    private fun renderRecords() {
        if (!::recordList.isInitialized) return
        recordList.removeAllViews()
        val q = if (::searchInput.isInitialized) searchInput.text.toString().trim().lowercase(Locale.ROOT) else ""
        val filtered = records.filter {
            q.isBlank() || listOf(it.date, it.type, it.department, it.destination, it.note, it.buyerCompany, it.invoiceNumber, it.dispatchReference).any { value -> value.lowercase(Locale.ROOT).contains(q) }
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
                text = "القسم: ${r.department}\nالإجمالي: ${fmt(r.quantity)} ${contractItem(r.type)?.unit ?: "وحدة"} | المسترجع: ${fmt(r.recovered)}\nالعملية: ${r.operationType} | الشركة: ${r.buyerCompany.ifBlank { "—" }}\nالوجهة: ${r.destination}\nالتكلفة: ${fmt(r.cost)} دج | المداخيل: ${fmt(r.income)} دج\nوصل الخروج: ${r.dispatchReference.ifBlank { "—" }} | الفاتورة: ${r.invoiceNumber.ifBlank { "—" }}\nحالة الدفع: ${r.paymentStatus} | تاريخ الفاتورة: ${r.invoiceDate.ifBlank { "—" }} | تاريخ الدفع: ${r.paymentDate.ifBlank { "—" }}" +
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
                cloud.markRecordDeleted(r.id)
                persistRecords(); renderRecords(); refreshDashboard(); refreshFinance()
                if (cloud.connected()) syncCloud(false)
            }.show()
    }

    private fun editRecord(r: WasteRecord) {
        val panel = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(8), 0, dp(8), 0) }
        val q = EditText(this).apply { hint = "الكمية الإجمالية كغ"; setText(fmt(r.quantity)) }
        val rec = EditText(this).apply { hint = "الكمية المسترجعة كغ"; setText(fmt(r.recovered)) }
        val cost = EditText(this).apply { hint = "التكلفة دج"; setText(fmt(r.cost)) }
        val income = EditText(this).apply { hint = "المداخيل دج (تحسب من العقد)"; setText(fmt(r.income)) }
        val dispatch = EditText(this).apply { hint = "مرجع وصل الخروج"; setText(r.dispatchReference) }
        val invoiceNo = EditText(this).apply { hint = "رقم الفاتورة"; setText(r.invoiceNumber) }
        val invoiceDate = EditText(this).apply { hint = "تاريخ الفاتورة YYYY-MM-DD"; setText(r.invoiceDate) }
        val paymentStatus = EditText(this).apply { hint = "حالة الدفع"; setText(r.paymentStatus) }
        val paymentDate = EditText(this).apply { hint = "تاريخ الدفع YYYY-MM-DD"; setText(r.paymentDate) }
        listOf(q, rec, cost, income, dispatch, invoiceNo, invoiceDate, paymentStatus, paymentDate).forEach { panel.addView(it) }
        listOf(q, rec, cost, income).forEach { it.inputType = android.text.InputType.TYPE_CLASS_NUMBER or android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL }
        AlertDialog.Builder(this).setTitle("تعديل ${r.type}").setView(panel)
            .setNegativeButton("إلغاء", null)
            .setPositiveButton("حفظ") { _, _ ->
                val nq = q.text.toString().replace(',', '.').toDoubleOrNull()
                val nr = rec.text.toString().replace(',', '.').toDoubleOrNull()
                val nc = cost.text.toString().replace(',', '.').toDoubleOrNull()
                val niInput = income.text.toString().replace(',', '.').toDoubleOrNull()
                if (nq == null || nr == null || nc == null || niInput == null || nq <= 0 || nr < 0 || nr > nq || nc < 0 || niInput < 0) {
                    Toast.makeText(this, "قيم غير صحيحة؛ لم يتم حفظ التعديل.", Toast.LENGTH_LONG).show()
                } else {
                    val ni = if (r.operationType == "بيع مباشر") nq * (contractItem(r.type)?.price ?: 0.0) else 0.0
                    val index = records.indexOfFirst { it.id == r.id }
                    if (index >= 0) records[index] = r.copy(quantity=nq, recovered=nr, cost=nc, income=ni, dispatchReference=dispatch.text.toString().trim(), invoiceNumber=invoiceNo.text.toString().trim(), invoiceDate=invoiceDate.text.toString().trim(), paymentStatus=paymentStatus.text.toString().trim().ifBlank { "غير محدد" }, paymentDate=paymentDate.text.toString().trim())
                    cloud.markRecordDirty(r.id)
                    persistRecords(); renderRecords(); refreshDashboard(); refreshFinance()
                    if (cloud.connected()) syncCloud(false)
                }
            }.show()
    }

    private fun refreshDashboard() {
        if (!::dashboardStats.isInitialized) return
        if (::productionInput.isInitialized && productionInput.text.isNullOrBlank()) { val v=loadProduction(today().take(7)); if(v>0) productionInput.setText(fmt(v)) }
        dashboardStats.removeAllViews()
        val unitGroups = records.groupBy { contractItem(it.type)?.unit ?: "وحدة" }
        fun unitTotals(selector: (WasteRecord) -> Double) = unitGroups.entries.joinToString(" + ") { (unit, rows) -> "${fmt(rows.sumOf(selector))} $unit" }.ifBlank { "0" }
        val kgRows = records.filter { contractItem(it.type)?.unit == "كغ" }
        val kgTotal = kgRows.sumOf { it.quantity }
        val kgRecovered = kgRows.sumOf { it.recovered }
        val costs = records.sumOf { it.cost }
        val income = records.sumOf { it.income }
        val rateByUnit = unitGroups.entries.joinToString(" | ") { (unit, rows) ->
            val q = rows.sumOf { it.quantity }; val r = rows.sumOf { it.recovered }
            "$unit: ${if(q>0) String.format(Locale.US, "%.1f", r*100/q) else "0"}%"
        }.ifBlank { "0%" }
        dashboardStats.addView(metric("الكميات المسجلة حسب الوحدة", unitTotals { it.quantity }), matchWrap())
        dashboardStats.addView(metric("الكميات المسترجعة حسب الوحدة", unitTotals { it.recovered }), matchWrap())
        dashboardStats.addView(metric("نسبة الاسترجاع حسب الوحدة", rateByUnit), matchWrap())
        dashboardStats.addView(metric("مؤشر النفايات إلى الإنتاج (كغ/100كغ)", if(loadProduction(today().take(7))>0) "${fmt(kgTotal/loadProduction(today().take(7))*100)} كغ/100كغ | المرجع الداخلي 0.96" else "أدخل إنتاج الشهر لحساب المؤشر؛ المرجع الداخلي 0.96"), matchWrap())
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
                    text = "الكمية: ${fmt(qty)} ${contractItem(type)?.unit ?: "وحدة"}  |  مسترجع: ${fmt(rec)}\nالتكاليف: ${fmt(costType)} دج\nالمداخيل: ${fmt(incomeType)} دج  |  صافي التكلفة: ${fmt(netType)} دج\nتكلفة الوحدة: ${fmt(perKg)} دج / ${contractItem(type)?.unit ?: "وحدة"}"
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

    private fun syncProductionCloud() {
        try {
            productionByMonth.forEach { (month,quantity) -> cloud.saveProduction(month,quantity) }
            val remote=cloud.fetchProduction()
            productionByMonth.clear()
            productionByMonth.putAll(remote)
            persistProduction()
        } catch (_: Exception) {
            // Keep locally saved monthly production if the optional production table has not been migrated yet.
        }
    }

    private fun saveProductionMonth() {
        val month=today().take(7)
        val value=number(productionInput)
        if(value==null || value<0) { Toast.makeText(this,"أدخل كمية إنتاج صحيحة بالكيلوغرام.",Toast.LENGTH_LONG).show(); return }
        productionByMonth[month]=value
        persistProduction()
        if(cloud.connected()) Thread { try { cloud.saveProduction(month,value); runOnUiThread { Toast.makeText(this,"تم حفظ إنتاج الشهر ومزامنته.",Toast.LENGTH_SHORT).show(); refreshDashboard() } } catch(e:Exception) { runOnUiThread { Toast.makeText(this,"حُفظ محلياً، تعذرت المزامنة: "+(e.message ?: ""),Toast.LENGTH_LONG).show() } } }.start()
        refreshDashboard()
    }
    private fun loadProduction(month:String):Double = productionByMonth[month] ?: 0.0
    private fun persistProduction() {
        val o=JSONObject(); productionByMonth.forEach { (m,v)->o.put(m,v) }
        getSharedPreferences("henkel_production", MODE_PRIVATE).edit().putString("monthly",o.toString()).apply()
    }
    private fun restoreProduction() {
        try { val o=JSONObject(getSharedPreferences("henkel_production", MODE_PRIVATE).getString("monthly","{}") ?: "{}"); val it=o.keys(); while(it.hasNext()){val k=it.next();productionByMonth[k]=o.optDouble(k)} } catch(_:Exception) {}
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


    private fun persistContracts() {
        val a = JSONArray()
        contractItems.forEach { c -> a.put(JSONObject().apply { put("name",c.name); put("price",c.price); put("unit",c.unit); put("method",c.method); put("companyName",c.companyName) }) }
        getSharedPreferences("henkel_contracts", MODE_PRIVATE).edit().putString("items", a.toString()).apply()
    }
    private fun loadContracts() {
        try {
            val raw = getSharedPreferences("henkel_contracts", MODE_PRIVATE).getString("items", null) ?: return
            val a = JSONArray(raw); if (a.length() == 0) return
            contractItems.clear()
            for (i in 0 until a.length()) { val o=a.getJSONObject(i); contractItems.add(ContractItem(o.optString("name"),o.optDouble("price"),o.optString("unit","كغ"),o.optString("method","بيع"),o.optString("companyName").ifBlank { if (o.optString("method","بيع")=="بيع") "SNC Groupe EL BADISSIA" else "" })) }
        } catch (_: Exception) { }
    }
    private fun refreshContracts() {
        if (!::contractsPage.isInitialized) return
        while (contractsPage.childCount > 5) contractsPage.removeViewAt(5)
        contractItems.forEach { c ->
            val box = LinearLayout(this).apply { orientation=LinearLayout.VERTICAL; setPadding(dp(12),dp(10),dp(12),dp(10)); setBackgroundColor(0xFFFFFFFF.toInt()) }
            box.addView(TextView(this).apply { text=c.name; textSize=16f; setTypeface(null,Typeface.BOLD); setTextColor(0xFF247A5A.toInt()) }, matchWrap())
            box.addView(TextView(this).apply { text="الشركة: ${c.companyName.ifBlank { "غير محددة" }}\n"+(if(c.method=="غير قابل للبيع") "هذا النوع غير مدرج للبيع في العقد" else "السعر: ${fmt(c.price)} دج / ${c.unit}  •  طريقة التعامل: ${c.method}"); textSize=14f; setTextColor(0xFF243746.toInt()); setPadding(0,dp(5),0,0) }, matchWrap())
            val p=LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.WRAP_CONTENT); p.bottomMargin=dp(8); contractsPage.addView(box,p)
        }
        contractsPage.addView(TextView(this).apply { text="ملاحظات العقد: الفوترة في نهاية الشهر، والدفع خلال 10 أيام من استلام الفاتورة. أوقات الجمع المذكورة: الأحد إلى الخميس، 08:00–16:00. مدة العقد سنة قابلة للتجديد باتفاق الطرفين وملحق. لا تعتمد هذه الملخصات بدل العقد الأصلي الموقع."; textSize=13f; setTextColor(0xFF795500.toInt()); setPadding(dp(12),dp(12),dp(12),dp(12)); setBackgroundColor(0xFFFFF2CC.toInt()) }, matchWrap())
    }
    private fun editContractPrices() {
        if (contractItems.isEmpty()) return
        val labels=contractItems.map { it.name }.toTypedArray()
        AlertDialog.Builder(this).setTitle("اختر بنداً لتعديل سعره").setItems(labels) { _, which ->
            val c=contractItems[which]
            val input=EditText(this).apply { hint="السعر بالدينار لكل ${c.unit}"; setText(c.price.toString()); inputType=android.text.InputType.TYPE_CLASS_NUMBER or android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL }
            AlertDialog.Builder(this).setTitle("تعديل السعر").setMessage(c.name).setView(input)
                .setNegativeButton("إلغاء",null).setPositiveButton("حفظ") { _, _ ->
                    val v=input.text.toString().replace(',','.').toDoubleOrNull()
                    if(v==null || v<0) Toast.makeText(this,"سعر غير صحيح",Toast.LENGTH_SHORT).show()
                    else { c.price=v; persistContracts(); cloud.markContractsDirty(); refreshContracts(); if(cloud.connected()) syncCloud(false); Toast.makeText(this,"تم تحديث السعر",Toast.LENGTH_SHORT).show() }
                }.show()
        }.show()
    }
    private fun addContractItem() {
        val panel=LinearLayout(this).apply { orientation=LinearLayout.VERTICAL; setPadding(dp(8),0,dp(8),0) }
        val name=EditText(this).apply { hint="اسم نوع النفاية / البند" }
        val price=EditText(this).apply { hint="السعر بالدينار (0 للإزالة المجانية)"; inputType=android.text.InputType.TYPE_CLASS_NUMBER or android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL }
        val unit=EditText(this).apply { hint="الوحدة: كغ أو وحدة"; setText("كغ") }
        val method=EditText(this).apply { hint="طريقة التعامل: بيع / إزالة مجانية"; setText("بيع") }
        val company=EditText(this).apply { hint="الشركة المتعاقدة / المشترية"; setText("SNC Groupe EL BADISSIA") }
        listOf(name,price,unit,method,company).forEach { panel.addView(it) }
        AlertDialog.Builder(this).setTitle("إضافة بند تعاقدي جديد").setView(panel).setNegativeButton("إلغاء",null)
            .setPositiveButton("إضافة") { _, _ ->
                val n=name.text.toString().trim(); val p=price.text.toString().replace(',','.').toDoubleOrNull(); val u=unit.text.toString().trim().ifBlank{"كغ"}; val m=method.text.toString().trim().ifBlank{"بيع"}; val co=company.text.toString().trim()
                if(n.isBlank() || p==null || p<0) Toast.makeText(this,"أدخل اسم البند وسعراً صحيحاً",Toast.LENGTH_LONG).show()
                else { if(contractItems.any{it.name==n}) Toast.makeText(this,"هذا البند موجود مسبقاً",Toast.LENGTH_SHORT).show() else { contractItems.add(ContractItem(n,p,u,m,co)); typeSpinner.adapter=ArrayAdapter(this@MainActivity,android.R.layout.simple_spinner_dropdown_item,types); persistContracts(); cloud.markContractsDirty(); refreshContracts(); if(cloud.connected()) syncCloud(false); Toast.makeText(this,"تمت إضافة البند",Toast.LENGTH_SHORT).show() } }
            }.show()
    }
    private fun showContractTerms() {
        AlertDialog.Builder(this).setTitle("بيانات عقد هنكل")
            .setMessage("الطرف: HENKEL ALGÉRIE SPA\\nالمشتري: SNC GROUPE EL BADISSIA\\nمرجع الوثيقة: NEW CONTRAT 2026\\nتاريخ السريان: 08-08-2026\\nتاريخ التحرير: 29-09-2026\\nالمدة: 12 شهراً، والتجديد بملحق موقع من الطرفين\\nالفوترة: نهاية كل شهر\\nالدفع: خلال 10 أيام من استلام الفاتورة\\nالجمع: الأحد إلى الخميس، 08:00–16:00\\nتقرير مفصل كل ثلاثة أشهر\\nالأسعار شاملة الرسوم وثابتة طوال مدة العقد\\nلا يجوز بيع النفايات داخل موقع هنكل، ولا الاستعانة بمشترٍ آخر دون موافقة كتابية مسبقة\\n\\nالأسعار والأنواع هي بنود العقد المرفق، وتظل قابلة للتعديل فقط بملحق تعاقدي موقع.")
            .setPositiveButton("إغلاق",null).show()
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
                        val id = normalizeId(o.optString("id", ""))
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
            put("cost",r.cost); put("income",r.income); put("note",r.note); put("buyerCompany",r.buyerCompany); put("operationType",r.operationType); put("dispatchReference",r.dispatchReference); put("invoiceNumber",r.invoiceNumber); put("invoiceDate",r.invoiceDate); put("paymentStatus",r.paymentStatus); put("paymentDate",r.paymentDate)
        }) }
        return a
    }

    private fun readRecord(o: JSONObject, id: String) = WasteRecord(id, o.optString("date"), o.optString("type"),
        o.optString("department"), o.optDouble("quantity"), o.optDouble("recovered"), o.optString("destination"),
        o.optDouble("cost"), o.optDouble("income"), o.optString("note"), o.optString("buyerCompany"), o.optString("operationType", "بيع مباشر"), o.optString("dispatchReference"), o.optString("invoiceNumber"), o.optString("invoiceDate"), o.optString("paymentStatus", "غير محدد"), o.optString("paymentDate"))

    private fun persistRecords() {
        getSharedPreferences("zakary_grd", MODE_PRIVATE).edit().putString("records", toJson().toString()).apply()
    }

    private fun loadRecords() {
        try {
            val a = JSONArray(getSharedPreferences("zakary_grd", MODE_PRIVATE).getString("records", "[]") ?: "[]")
            for (i in 0 until a.length()) { val o=a.getJSONObject(i); records.add(readRecord(o, normalizeId(o.optString("id", "")))) }
            persistRecords()
        } catch (_: Exception) { records.clear() }
    }


    private fun normalizeId(value: String): String {
        return try { UUID.fromString(value).toString() } catch (_: Exception) { UUID.randomUUID().toString() }
    }

    private fun showCloudDialog() {
        val panel = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(10), dp(4), dp(10), dp(4)) }
        val project = EditText(this).apply { hint = "رابط Supabase: https://xxxx.supabase.co"; setText(cloud.projectUrl()) }
        val publicKey = EditText(this).apply { hint = "المفتاح العام Publishable / anon"; setText(cloud.publicKey()); textSize = 12f }
        val email = EditText(this).apply { hint = "البريد الإلكتروني"; setText(cloud.email()); inputType = android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS }
        val password = EditText(this).apply { hint = "كلمة مرور حساب Supabase"; inputType = android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD }
        listOf(project, publicKey, email, password).forEach { panel.addView(it, matchWrap()) }
        AlertDialog.Builder(this).setTitle("ربط الهاتف بقاعدة هنكل المشتركة")
            .setMessage("أدخل رابط المشروع والمفتاح العام فقط، ثم سجّل الدخول بالحساب نفسه المستعمل على الحاسوب. لا تدخل مفتاح service_role.")
            .setView(panel).setNegativeButton("إلغاء", null)
            .setPositiveButton("اتصال ومزامنة", null).create().also { dialog ->
                dialog.setOnShowListener {
                    dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                        val u = project.text.toString().trim()
                        val k = publicKey.text.toString().trim()
                        val e = email.text.toString().trim()
                        val pw = password.text.toString()
                        if (!u.startsWith("https://") || k.isBlank() || e.isBlank() || pw.isBlank()) {
                            Toast.makeText(this, "أكمل الرابط والمفتاح العام والبريد وكلمة المرور.", Toast.LENGTH_LONG).show()
                            return@setOnClickListener
                        }
                        dialog.dismiss()
                        Toast.makeText(this, "جارٍ الاتصال الآمن...", Toast.LENGTH_SHORT).show()
                        Thread {
                            try {
                                cloud.signIn(u, k, e, pw)
                                val result = cloud.sync(records.toList(), contractItems.toList())
                                syncProductionCloud()
                                runOnUiThread {
                                    records.clear(); records.addAll(result.first)
                                    contractItems.clear(); contractItems.addAll(result.second)
                                    persistRecords(); persistContracts()
                                    if (::typeSpinner.isInitialized) typeSpinner.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, types)
                                    renderRecords(); refreshDashboard(); refreshFinance(); refreshContracts()
                                    Toast.makeText(this, "تم ربط الهاتف ومزامنة البيانات بنجاح.", Toast.LENGTH_LONG).show()
                                }
                            } catch (ex: Exception) {
                                runOnUiThread { Toast.makeText(this, "تعذر الاتصال: " + (ex.message ?: "خطأ غير معروف"), Toast.LENGTH_LONG).show() }
                            }
                        }.start()
                    }
                }
                dialog.show()
            }
    }

    private fun syncCloud(showMessage: Boolean) {
        if (!cloud.connected()) {
            if (showMessage) showCloudDialog()
            return
        }
        Thread {
            try {
                val result = cloud.sync(records.toList(), contractItems.toList())
                syncProductionCloud()
                runOnUiThread {
                    records.clear(); records.addAll(result.first)
                    contractItems.clear(); contractItems.addAll(result.second)
                    persistRecords(); persistContracts()
                    if (::typeSpinner.isInitialized) typeSpinner.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, types)
                    renderRecords(); refreshDashboard(); refreshFinance(); refreshContracts()
                    if (showMessage) Toast.makeText(this, "تمت المزامنة بين الهاتف والحاسوب.", Toast.LENGTH_LONG).show()
                }
            } catch (ex: Exception) {
                if (showMessage) runOnUiThread { Toast.makeText(this, "فشلت المزامنة: " + (ex.message ?: "تحقق من الإنترنت وإعداد Supabase"), Toast.LENGTH_LONG).show() }
            }
        }.start()
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
