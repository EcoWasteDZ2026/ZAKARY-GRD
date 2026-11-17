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

data class ContractItem(val name: String, var price: Double, var unit: String, var method: String, var companyName: String = "", var contractReference: String = "NEW CONTRAT 2026")

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
    private var activeContractReference = "NEW CONTRAT 2026"
    private var activeContractCompany = "SNC Groupe EL BADISSIA"
    private val contractItems = mutableListOf(
        ContractItem("صناديق كرتونية ونفايات الورق", 2.40, "كغ", "بيع", "SNC Groupe EL BADISSIA"),
        ContractItem("الأغلفة البلاستيكية (المطاطية وأغلفة التغليف)", 5.40, "كغ", "بيع", "SNC Groupe EL BADISSIA"),
        ContractItem("أكياس Big Bags", 4.80, "كغ", "بيع", "SNC Groupe EL BADISSIA"),
        ContractItem("براميل معدنية", 216.0, "وحدة", "بيع", "SNC Groupe EL BADISSIA"),
        ContractItem("نفايات المنصات الخشبية وبقايا الخشب", 2.50, "كغ", "بيع", "SNC Groupe EL BADISSIA"),
        ContractItem("أكياس وأغلفة PE ملوثة بالمنظفات وغير قابلة للاسترجاع", 0.0, "حسب الكمية", "غير قابل للبيع")
    )
    private val types get() = contractItems.filter { it.contractReference == activeContractReference }.map { it.name } + listOf("نفايات صناعية أخرى (تُضاف لاحقاً)")
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
    private lateinit var settingsPage: LinearLayout
    private lateinit var languageSpinner: Spinner
    private lateinit var contractSpinner: Spinner
    private var appLanguage: String = "ar"
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
        appLanguage = getSharedPreferences("zakary_settings", MODE_PRIVATE).getString("language", "ar") ?: "ar"
        loadRecords()
        loadContracts()
        restoreProduction()
        buildScreen()
        renderRecords()
        showPage("dashboard")
        if (cloud.connected()) syncCloud(false)
    }

    private fun tr(value: String): String {
        if (appLanguage == "ar") return value
        val translations = mapOf(
            "لوحة القيادة" to Pair("Tableau de bord", "Dashboard"),
            "تسجيل نفايات" to Pair("Enregistrer un déchet", "Record waste"),
            "السجلات" to Pair("Registres", "Records"),
            "مداخيل البيع" to Pair("Revenus des ventes", "Sales revenue"),
            "التقارير والنسخ" to Pair("Rapports et sauvegardes", "Reports & backups"),
            "العقود والأسعار" to Pair("Contrats et tarifs", "Contracts & prices"),
            "الإعدادات" to Pair("Paramètres", "Settings"),
            "نظام تسيير واسترجاع النفايات الصناعية — مصنع هنكل، ولاية ميلة" to Pair("Système de gestion et de valorisation des déchets industriels — usine Henkel, Mila", "Industrial waste management system — Henkel plant, Mila"),
            "HENKEL MILA • العقود • التتبع • التثمين • التقارير" to Pair("HENKEL MILA • Contrats • Suivi • Valorisation • Rapports", "HENKEL MILA • Contracts • Tracking • Sales • Reports"),
            "＋ تسجيل نفايات جديدة" to Pair("＋ Enregistrer un déchet", "＋ Record new waste"),
            "☷ عرض السجلات" to Pair("☷ Voir les registres", "☷ View records"),
            "⇩ تصدير تقرير CSV" to Pair("⇩ Exporter le rapport CSV", "⇩ Export CSV report"),
            "☁ ربط الحساب والمزامنة" to Pair("☁ Connecter le compte et synchroniser", "☁ Connect account & sync"),
            "⟳ مزامنة البيانات مع الحاسوب" to Pair("⟳ Synchroniser avec l’ordinateur", "⟳ Sync data with computer"),
            "تسجيل عملية نفايات" to Pair("Enregistrer une opération de déchet", "Record a waste transaction"),
            "التاريخ (YYYY-MM-DD)" to Pair("Date (AAAA-MM-JJ)", "Date (YYYY-MM-DD)"),
            "الكمية الإجمالية بالكيلوغرام" to Pair("Quantité totale en kilogrammes", "Total quantity in kilograms"),
            "مداخيل البيع (تحسب آلياً من العقد)" to Pair("Revenu de vente (calculé automatiquement selon le contrat)", "Sales revenue (calculated automatically from contract)"),
            "ملاحظات (اختياري)" to Pair("Remarques (facultatif)", "Notes (optional)"),
            "مرجع وصل الخروج / أمر الخروج" to Pair("Référence du bon de sortie", "Dispatch note reference"),
            "رقم الفاتورة" to Pair("Numéro de facture", "Invoice number"),
            "تاريخ الفاتورة YYYY-MM-DD" to Pair("Date de facture AAAA-MM-JJ", "Invoice date YYYY-MM-DD"),
            "تاريخ الدفع YYYY-MM-DD" to Pair("Date de paiement AAAA-MM-JJ", "Payment date YYYY-MM-DD"),
            "حفظ العملية" to Pair("Enregistrer l’opération", "Save transaction"),
            "مسح حقول الإدخال" to Pair("Effacer les champs", "Clear fields"),
            "سجل العمليات" to Pair("Registre des opérations", "Transaction records"),
            "بحث حسب النوع أو القسم أو التاريخ" to Pair("Rechercher par type, service ou date", "Search by type, department or date"),
            "بحث / تحديث السجل" to Pair("Rechercher / Actualiser", "Search / Refresh"),
            "إلغاء البحث" to Pair("Effacer la recherche", "Clear search"),
            "التقارير والنسخ الاحتياطي" to Pair("Rapports et sauvegarde", "Reports & backup"),
            "ملاحظة: تحفظ البيانات محليًا وتُزامَن مع حساب هنكل المشترك عند الاتصال. احتفظ بنسخة احتياطية دورية، ولا تشارك بيانات الدخول أو مفاتيح الإدارة." to Pair("Remarque : les données sont enregistrées localement et synchronisées avec le compte Henkel lorsqu’il est connecté. Effectuez des sauvegardes régulières et ne partagez pas vos identifiants ni vos clés d’administration.", "Note: Data is stored locally and synced with the shared Henkel account when connected. Keep regular backups and do not share login details or admin keys."),
            "تصدير تقرير CSV" to Pair("Exporter le rapport CSV", "Export CSV report"),
            "مشاركة / حفظ نسخة احتياطية" to Pair("Partager / Enregistrer une sauvegarde", "Share / Save backup"),
            "استيراد نسخة احتياطية" to Pair("Importer une sauvegarde", "Import backup"),
            "عرض ملخص حسب نوع النفايات" to Pair("Afficher le résumé par type de déchet", "View summary by waste type"),
            "تعديل أسعار البنود" to Pair("Modifier les prix des articles", "Edit item prices"),
            "إضافة بند تعاقدي" to Pair("Ajouter un article au contrat", "Add contract item"),
            "عرض شروط العقد" to Pair("Afficher les conditions du contrat", "View contract terms"),
            "مزامنة العقود" to Pair("Synchroniser les contrats", "Sync contracts"),
            "اللغة الشخصية للتطبيق" to Pair("Langue personnelle de l’application", "Personal app language"),
            "اختر لغة واجهة التطبيق على هذا الجهاز. الاختيار محفوظ بشكل مستقل ولا يغيّر إعدادات بقية المستخدمين." to Pair("Choisissez la langue de l’application sur cet appareil. Ce choix est enregistré séparément et ne modifie pas les paramètres des autres utilisateurs.", "Choose the app language on this device. This preference is saved separately and does not change other users’ settings."),
            "حفظ اللغة المختارة" to Pair("Enregistrer la langue choisie", "Save selected language"),
            "إعدادات إضافية" to Pair("Paramètres supplémentaires", "Additional settings"),
            "النسخ الاحتياطي واسترجاع البيانات" to Pair("Sauvegarde et restauration des données", "Backup and restore data"),
            "إعدادات المزامنة" to Pair("Paramètres de synchronisation", "Sync settings"),
            "حول التطبيق" to Pair("À propos de l’application", "About the app"),
            "إغلاق" to Pair("Fermer", "Close"),
            "تم حفظ تفضيل اللغة على هذا الجهاز." to Pair("La préférence linguistique a été enregistrée sur cet appareil.", "Language preference saved on this device."),
            "الشركة المشترية (للبيع فقط)" to Pair("Société acheteuse (vente uniquement)", "Buyer company (sales only)"),
            "القسم" to Pair("Service", "Department"),
            "الوجهة" to Pair("Destination", "Destination"),
            "غير محدد" to Pair("Non défini", "Unspecified"),
            "غير مفوتر" to Pair("Non facturé", "Not invoiced"),
            "فاتورة صادرة - غير مدفوع" to Pair("Facture émise - non payée", "Invoice issued - unpaid"),
            "مدفوع" to Pair("Payé", "Paid"),
            "متأخر" to Pair("En retard", "Overdue"),
            "الإنتاج" to Pair("Production", "Production"),
            "المخازن" to Pair("Magasins", "Warehouses"),
            "الصيانة" to Pair("Maintenance", "Maintenance"),
            "المخبر" to Pair("Laboratoire", "Laboratory"),
            "معالجة المياه" to Pair("Traitement des eaux", "Water treatment"),
            "الإدارة" to Pair("Administration", "Administration"),
            "أخرى" to Pair("Autre", "Other"),
            "خروج إلى الشركة المشترية" to Pair("Sortie vers l’acheteur", "Dispatch to buyer"),
            "الكمية: " to Pair("Quantité : ", "Quantity: "),
            "العملية: " to Pair("Opération : ", "Operation: "),
            "الشركة: " to Pair("Société : ", "Company: "),
            "الوجهة: " to Pair("Destination : ", "Destination: "),
            "مداخيل البيع: " to Pair("Revenu de vente : ", "Sales revenue: "),
            "وصل الخروج: " to Pair("Bon de sortie : ", "Dispatch note: "),
            "الفاتورة: " to Pair("Facture : ", "Invoice: "),
            "حالة الدفع: " to Pair("État du paiement : ", "Payment status: "),
            "تاريخ الفاتورة: " to Pair("Date de facture : ", "Invoice date: "),
            "تاريخ الدفع: " to Pair("Date de paiement : ", "Payment date: "),
            "ملاحظات: " to Pair("Remarques : ", "Notes: "),
            "تعديل" to Pair("Modifier", "Edit"),
            "حذف" to Pair("Supprimer", "Delete"),
            "إلغاء" to Pair("Annuler", "Cancel"),
            "حفظ" to Pair("Enregistrer", "Save"),
            "إضافة" to Pair("Ajouter", "Add"),
            "الكمية الإجمالية كغ" to Pair("Quantité totale (kg)", "Total quantity (kg)"),
            "المداخيل دج (تحسب من العقد)" to Pair("Revenu DZD (calculé selon le contrat)", "Revenue DZD (calculated from contract)"),
            "مرجع وصل الخروج" to Pair("Référence du bon de sortie", "Dispatch note reference"),
            "حالة الدفع" to Pair("État du paiement", "Payment status"),
            "حذف السجل" to Pair("Supprimer l’enregistrement", "Delete record"),
            "هل تريد حذف عملية " to Pair("Voulez-vous supprimer l’opération ", "Delete the transaction "),
            "قيم غير صحيحة؛ لم يتم حفظ التعديل." to Pair("Valeurs incorrectes ; modification non enregistrée.", "Invalid values; changes were not saved."),
            "تم حفظ العملية بنجاح" to Pair("Opération enregistrée avec succès", "Transaction saved successfully"),
            "لا توجد سجلات بعد. اضغط «تسجيل نفايات» لإضافة أول عملية." to Pair("Aucun enregistrement. Appuyez sur « Enregistrer un déchet » pour ajouter la première opération.", "No records yet. Tap “Record waste” to add the first transaction."),
            "لا توجد نتائج مطابقة للبحث." to Pair("Aucun résultat correspondant.", "No matching results."),
            "إجمالي الكميات المسجلة" to Pair("Quantités totales enregistrées", "Total recorded quantities"),
            "إجمالي مداخيل البيع" to Pair("Revenu total des ventes", "Total sales revenue"),
            "عدد عمليات البيع" to Pair("Nombre de ventes", "Number of sales"),
            "المداخيل حسب نوع النفايات" to Pair("Revenus par type de déchet", "Revenue by waste type"),
            "الكمية المباعة: " to Pair("Quantité vendue : ", "Quantity sold: "),
            "مداخيل البيع: " to Pair("Revenu des ventes : ", "Sales revenue: "),
            "＋ تسجيل عملية بيع" to Pair("＋ Enregistrer une vente", "＋ Record a sale"),
            "تفصيل المداخيل حسب نوع النفاية" to Pair("Détail des revenus par type de déchet", "Revenue breakdown by waste type"),
            "الشركة: غير محددة" to Pair("Société : non définie", "Company: unspecified"),
            "هذا النوع غير مدرج للبيع في العقد" to Pair("Ce type n’est pas destiné à la vente selon le contrat", "This waste type is not listed for sale in the contract"),
            "طريقة التعامل: " to Pair("Mode de traitement : ", "Handling method: "),
            "السعر: " to Pair("Prix : ", "Price: "),
            "ملاحظات العقد: الفوترة في نهاية الشهر، والدفع خلال 10 أيام من استلام الفاتورة. أوقات الجمع المذكورة: الأحد إلى الخميس، 08:00–16:00. مدة العقد سنة قابلة للتجديد باتفاق الطرفين وملحق. لا تعتمد هذه الملخصات بدل العقد الأصلي الموقع." to Pair("Notes du contrat : facturation en fin de mois, paiement sous 10 jours après réception de la facture. Collecte du dimanche au jeudi, 08:00–16:00. Durée d’un an, renouvelable par accord et avenant. Ce résumé ne remplace pas le contrat original signé.", "Contract notes: invoicing at month-end, payment within 10 days of invoice receipt. Collection Sunday to Thursday, 08:00–16:00. One-year term, renewable by agreement and amendment. This summary does not replace the signed original contract."),
            "اختر بنداً لتعديل سعره" to Pair("Choisir un article pour modifier son prix", "Choose an item to edit its price"),
            "السعر بالدينار لكل " to Pair("Prix en DZD par ", "Price in DZD per "),
            "تعديل السعر" to Pair("Modifier le prix", "Edit price"),
            "سعر غير صحيح" to Pair("Prix incorrect", "Invalid price"),
            "تم تحديث السعر" to Pair("Prix mis à jour", "Price updated"),
            "اسم نوع النفاية / البند" to Pair("Nom du déchet / article", "Waste type / item name"),
            "السعر بالدينار (0 للإزالة المجانية)" to Pair("Prix en DZD (0 pour enlèvement gratuit)", "Price in DZD (0 for free removal)"),
            "الوحدة: كغ أو وحدة" to Pair("Unité : kg ou unité", "Unit: kg or unit"),
            "طريقة التعامل: بيع / إزالة مجانية" to Pair("Mode : vente / enlèvement gratuit", "Method: sale / free removal"),
            "الشركة المتعاقدة / المشترية" to Pair("Entreprise contractante / acheteur", "Contracted / buyer company"),
            "إضافة بند تعاقدي جديد" to Pair("Ajouter un nouvel article au contrat", "Add a new contract item"),
            "أدخل اسم البند وسعراً صحيحاً" to Pair("Saisissez le nom de l’article et un prix valide", "Enter an item name and a valid price"),
            "هذا البند موجود مسبقاً" to Pair("Cet article existe déjà", "This item already exists"),
            "تمت إضافة البند" to Pair("Article ajouté", "Item added"),
            "بيانات عقد هنكل" to Pair("Données du contrat Henkel", "Henkel contract details"),
            "ملخص أنواع النفايات" to Pair("Résumé des types de déchets", "Waste type summary"),
            "لا توجد بيانات بعد." to Pair("Aucune donnée pour le moment.", "No data yet."),
            "حفظ أو مشاركة النسخة الاحتياطية" to Pair("Enregistrer ou partager la sauvegarde", "Save or share backup"),
            "استيراد نسخة احتياطية" to Pair("Importer une sauvegarde", "Import backup"),
            "الصق محتوى JSON للنسخة الاحتياطية" to Pair("Collez le contenu JSON de la sauvegarde", "Paste backup JSON content"),
            "تم استيراد " to Pair("Importé : ", "Imported "),
            " سجل" to Pair(" enregistrement(s)", " record(s)"),
            "تعذر قراءة النسخة. تأكد من نسخ JSON كاملًا." to Pair("Lecture impossible. Vérifiez que le JSON complet a été copié.", "Could not read backup. Make sure the full JSON was copied."),
            "رابط Supabase: https://xxxx.supabase.co" to Pair("URL Supabase : https://xxxx.supabase.co", "Supabase URL: https://xxxx.supabase.co"),
            "المفتاح العام Publishable / anon" to Pair("Clé publique Publishable / anon", "Public Publishable / anon key"),
            "البريد الإلكتروني لحساب المزامنة" to Pair("E-mail du compte de synchronisation", "Sync account email"),
            "كلمة مرور حساب Supabase" to Pair("Mot de passe du compte Supabase", "Supabase account password"),
            "ربط الهاتف بقاعدة هنكل المشتركة" to Pair("Connecter le téléphone à la base Henkel partagée", "Connect phone to shared Henkel database"),
            "اتصال ومزامنة" to Pair("Connecter et synchroniser", "Connect & sync"),
            "أكمل الرابط والمفتاح العام والبريد وكلمة المرور." to Pair("Complétez l’URL, la clé publique, l’e-mail et le mot de passe.", "Complete the URL, public key, email and password."),
            "جارٍ الاتصال الآمن..." to Pair("Connexion sécurisée...", "Connecting securely..."),
            "تم ربط الهاتف ومزامنة البيانات بنجاح." to Pair("Téléphone connecté et données synchronisées.", "Phone connected and data synced successfully."),
            "تعذر الاتصال: " to Pair("Connexion impossible : ", "Connection failed: "),
            "تمت المزامنة بين الهاتف والحاسوب." to Pair("Synchronisation entre téléphone et ordinateur terminée.", "Phone and computer sync completed."),
            "فشلت المزامنة: " to Pair("Échec de la synchronisation : ", "Sync failed: "),
            "حول التطبيق" to Pair("À propos", "About"),
            "العربية" to Pair("Arabe", "Arabic"),
            "الشركة المشترية يجب أن تطابق الشركة المحددة في العقد." to Pair("L’acheteur doit correspondre à l’entreprise indiquée dans le contrat.", "Buyer must match the company specified in the contract."),
            "لا يمكن بيع هذا النوع دون بند بيع في العقد." to Pair("Ce type ne peut être vendu sans article de vente dans le contrat.", "This type cannot be sold without a sale item in the contract."),
            "تحقق من التاريخ والكمية." to Pair("Vérifiez la date et la quantité.", "Check the date and quantity."),
            "أدخل كمية صحيحة أكبر من صفر." to Pair("Saisissez une quantité valide supérieure à zéro.", "Enter a valid quantity greater than zero."),
            "صناديق كرتونية ونفايات الورق" to Pair("Cartons et déchets de papier", "Cardboard boxes and paper waste"),
            "الأغلفة البلاستيكية (المطاطية وأغلفة التغليف)" to Pair("Films plastiques et emballages", "Plastic wrapping films and packaging"),
            "أكياس Big Bags" to Pair("Sacs Big Bags", "Big Bags"),
            "براميل معدنية" to Pair("Fûts métalliques", "Metal drums"),
            "نفايات المنصات الخشبية وبقايا الخشب" to Pair("Palettes et chutes de bois", "Wooden pallets and wood scraps"),
            "أكياس وأغلفة PE ملوثة بالمنظفات وغير قابلة للاسترجاع" to Pair("Sacs et emballages PE contaminés par des détergents, non valorisables", "PE bags and packaging contaminated with detergents, not recoverable"),
            "نفايات صناعية أخرى (تُضاف لاحقاً)" to Pair("Autres déchets industriels (à ajouter)", "Other industrial waste (to be added)"),
            "بيع مباشر" to Pair("Vente directe", "Direct sale"),
            "كغ" to Pair("kg", "kg"),
            "وحدة" to Pair("unité", "unit"),
            "حسب الكمية" to Pair("Selon quantité", "By quantity"),
            "غير قابل للبيع" to Pair("Non vendable", "Not for sale"),
            "طريقة التعامل: " to Pair("Mode : ", "Method: "),
            "⚙  الإعدادات واللغة" to Pair("⚙  Paramètres et langue", "⚙  Settings & language"),
            "＋ تسجيل نفايات جديدة" to Pair("＋ Nouvelle vente de déchets", "＋ Register waste sale"),
            "☷ عرض السجلات" to Pair("☷ Consulter les registres", "☷ View records"),
            "حفظ العملية" to Pair("Enregistrer la vente", "Save sale"),
            "مسح حقول الإدخال" to Pair("Effacer les champs", "Clear fields"),
            "بحث / تحديث السجل" to Pair("Rechercher / Actualiser", "Search / Refresh"),
            "إلغاء البحث" to Pair("Effacer la recherche", "Clear search"),
            "تعديل" to Pair("Modifier", "Edit"),
            "حذف" to Pair("Supprimer", "Delete"),
            "إلغاء" to Pair("Annuler", "Cancel"),
            "حفظ" to Pair("Enregistrer", "Save"),
            "إضافة" to Pair("Ajouter", "Add"),
            "تم حفظ العملية بنجاح" to Pair("Vente enregistrée avec succès", "Sale saved successfully"),
            "لا يمكن بيع هذا النوع دون بند بيع في العقد." to Pair("Ce déchet ne peut pas être vendu sans ligne de vente dans le contrat.", "This waste cannot be sold without a sales item in the contract."),
            "الشركة المشترية يجب أن تطابق الشركة المحددة في العقد." to Pair("L’acheteur doit correspondre à la société indiquée dans le contrat.", "The buyer must match the company specified in the contract."),
            "تحقق من التاريخ والكمية." to Pair("Vérifiez la date et la quantité.", "Check the date and quantity."),
            "لا توجد سجلات بعد. اضغط «تسجيل نفايات» لإضافة أول عملية." to Pair("Aucun registre. Appuyez sur « Enregistrer un déchet » pour ajouter la première vente.", "No records yet. Tap “Record waste” to add the first sale."),
            "لا توجد نتائج مطابقة للبحث." to Pair("Aucun résultat correspondant à la recherche.", "No matching search results."),
            "الكمية الإجمالية كغ" to Pair("Quantité totale (kg)", "Total quantity (kg)"),
            "المداخيل دج (تحسب من العقد)" to Pair("Revenu (DZD, calculé selon le contrat)", "Revenue (DZD, calculated from contract)"),
            "مرجع وصل الخروج" to Pair("Référence du bon de sortie", "Dispatch note reference"),
            "حالة الدفع" to Pair("État du paiement", "Payment status"),
            "قيم غير صحيحة؛ لم يتم حفظ التعديل." to Pair("Valeurs incorrectes ; modification non enregistrée.", "Invalid values; changes were not saved."),
            "المداخيل حسب نوع النفايات" to Pair("Revenus par type de déchet", "Revenue by waste type"),
            "الكمية المباعة: " to Pair("Quantité vendue : ", "Quantity sold: "),
            "مداخيل البيع: " to Pair("Revenu de vente : ", "Sales revenue: "),
            "تفصيل المداخيل حسب نوع النفاية" to Pair("Détail des revenus par type de déchet", "Revenue breakdown by waste type"),
            "الشركة: " to Pair("Société : ", "Company: "),
            "السعر: " to Pair("Prix : ", "Price: "),
            "غير محددة" to Pair("Non spécifiée", "Not specified"),
            "هذا النوع غير مدرج للبيع في العقد" to Pair("Ce type n’est pas prévu à la vente dans le contrat", "This type is not listed for sale in the contract"),
            "ملاحظات العقد: الفوترة في نهاية الشهر، والدفع خلال 10 أيام من استلام الفاتورة. أوقات الجمع المذكورة: الأحد إلى الخميس، 08:00–16:00. مدة العقد سنة قابلة للتجديد باتفاق الطرفين وملحق. لا تعتمد هذه الملخصات بدل العقد الأصلي الموقع." to Pair("Conditions : facturation en fin de mois, paiement sous 10 jours après réception. Collecte du dimanche au jeudi, 08:00–16:00. Durée : un an, renouvelable par accord et avenant. Le contrat signé reste la référence.", "Contract notes: invoicing at month-end, payment within 10 days of receipt. Collection Sunday–Thursday, 08:00–16:00. One-year term, renewable by agreement and addendum. The signed contract remains authoritative."),
            "اختر بنداً لتعديل سعره" to Pair("Choisissez un article pour modifier son prix", "Choose an item to edit its price"),
            "السعر بالدينار لكل " to Pair("Prix en DZD par ", "Price in DZD per "),
            "تعديل السعر" to Pair("Modifier le prix", "Edit price"),
            "سعر غير صحيح" to Pair("Prix invalide", "Invalid price"),
            "تم تحديث السعر" to Pair("Prix mis à jour", "Price updated"),
            "اسم نوع النفاية / البند" to Pair("Nom du déchet / article", "Waste type / item name"),
            "السعر بالدينار (0 للإزالة المجانية)" to Pair("Prix en DZD (0 pour enlèvement gratuit)", "Price in DZD (0 for free removal)"),
            "الوحدة: كغ أو وحدة" to Pair("Unité : kg ou unité", "Unit: kg or unit"),
            "طريقة التعامل: بيع / إزالة مجانية" to Pair("Mode : vente / enlèvement gratuit", "Method: sale / free removal"),
            "الشركة المتعاقدة / المشترية" to Pair("Société contractante / acheteuse", "Contracting / buyer company"),
            "إضافة بند تعاقدي جديد" to Pair("Ajouter un nouvel article au contrat", "Add new contract item"),
            "أدخل اسم البند وسعراً صحيحاً" to Pair("Saisissez le nom de l’article et un prix valide", "Enter the item name and a valid price"),
            "هذا البند موجود مسبقاً" to Pair("Cet article existe déjà", "This item already exists"),
            "تمت إضافة البند" to Pair("Article ajouté", "Item added"),
            "بيانات عقد هنكل" to Pair("Données du contrat Henkel", "Henkel contract details"),
            "ملخص أنواع النفايات" to Pair("Résumé des types de déchets", "Waste types summary"),
            "لا توجد بيانات بعد." to Pair("Aucune donnée pour le moment.", "No data yet."),
            "الصق محتوى JSON للنسخة الاحتياطية" to Pair("Collez le contenu JSON de la sauvegarde", "Paste backup JSON content"),
            "استيراد نسخة احتياطية" to Pair("Importer une sauvegarde", "Import backup"),
            "انسخ محتوى النسخة الاحتياطية والصقه هنا. ستتم إضافة السجلات الجديدة دون تكرار المعرفات." to Pair("Copiez le contenu de la sauvegarde et collez-le ici. Les nouveaux registres seront ajoutés sans dupliquer les identifiants.", "Copy and paste the backup content here. New records will be added without duplicating IDs."),
            "استيراد" to Pair("Importer", "Import"),
            "تعذر قراءة النسخة. تأكد من نسخ JSON كاملًا." to Pair("Impossible de lire la sauvegarde. Vérifiez que le JSON est complet.", "Could not read backup. Make sure the full JSON was copied."),
            "رابط Supabase: https://xxxx.supabase.co" to Pair("URL Supabase : https://xxxx.supabase.co", "Supabase URL: https://xxxx.supabase.co"),
            "المفتاح العام Publishable / anon" to Pair("Clé publique Publishable / anon", "Public key Publishable / anon"),
            "البريد الإلكتروني لحساب المزامنة" to Pair("E-mail du compte de synchronisation", "Sync account email"),
            "كلمة مرور حساب Supabase" to Pair("Mot de passe du compte Supabase", "Supabase account password"),
            "ربط الهاتف بقاعدة هنكل المشتركة" to Pair("Connecter le téléphone à la base Henkel", "Connect phone to Henkel database"),
            "اتصال ومزامنة" to Pair("Connecter et synchroniser", "Connect & sync"),
            "أكمل الرابط والمفتاح العام والبريد وكلمة المرور." to Pair("Renseignez l’URL, la clé publique, l’e-mail et le mot de passe.", "Complete the URL, public key, email and password."),
            "جارٍ الاتصال الآمن..." to Pair("Connexion sécurisée en cours...", "Connecting securely..."),
            "تم ربط الهاتف ومزامنة البيانات بنجاح." to Pair("Téléphone connecté et données synchronisées.", "Phone connected and data synced successfully."),
            "تمت المزامنة بين الهاتف والحاسوب." to Pair("Synchronisation entre le téléphone et l’ordinateur terminée.", "Phone and computer sync completed."),
            "فشلت المزامنة: " to Pair("Échec de la synchronisation : ", "Sync failed: "),
            "تعذر الاتصال: " to Pair("Connexion impossible : ", "Connection failed: "),
"فتح نافذة إدارة العقود" to Pair("Ouvrir le gestionnaire des contrats", "Open contract manager"),
            "إنشاء عقد جديد" to Pair("Créer un nouveau contrat", "Create new contract"),
            "مرجع العقد: " to Pair("Référence du contrat : ", "Contract reference: "),
            "مرجع العقد / رقم العقد" to Pair("Référence / numéro du contrat", "Contract reference / number"),
            "اسم الشركة المتعاقدة" to Pair("Nom de l’entreprise contractante", "Contracting company name"),
            "تاريخ بداية العقد YYYY-MM-DD" to Pair("Date de début du contrat AAAA-MM-JJ", "Contract start date YYYY-MM-DD"),
            "حفظ العقد" to Pair("Enregistrer le contrat", "Save contract"),
            "إدارة العقود والأسعار" to Pair("Gestion des contrats et des tarifs", "Contract and price management"),
            "عدد العقود: " to Pair("Nombre de contrats : ", "Number of contracts: "),
            "عدد البنود: " to Pair("Nombre d’articles : ", "Number of items: "),
            "العقد النشط: " to Pair("Contrat actif : ", "Active contract: "),
            "إضافة بند إلى العقد النشط" to Pair("Ajouter un article au contrat actif", "Add item to active contract"),
            "تعديل سعر بند" to Pair("Modifier le prix d’un article", "Edit item price"),
            "عرض قائمة البنود والأسعار" to Pair("Afficher les articles et les tarifs", "View items and prices"),
            "هذا البند موجود مسبقاً في هذا العقد" to Pair("Cet article existe déjà dans ce contrat", "This item already exists in this contract"),
            "أدخل مرجع العقد واسم الشركة." to Pair("Saisissez la référence du contrat et le nom de l’entreprise.", "Enter the contract reference and company name."),
            "تم إنشاء العقد. أضف بنوده وأسعاره الآن." to Pair("Contrat créé. Ajoutez maintenant ses articles et tarifs.", "Contract created. Add its items and prices now."),
            "إجمالي الكميات المسجلة" to Pair("Quantités totales enregistrées", "Total recorded quantities"),
            "إجمالي مداخيل البيع" to Pair("Revenu total des ventes", "Total sales revenue"),
            "عدد عمليات البيع" to Pair("Nombre de ventes", "Number of sales"),
            "العملية: " to Pair("Opération : ", "Operation: "),
            "الوجهة: " to Pair("Destination : ", "Destination: "),
            "وصل الخروج: " to Pair("Bon de sortie : ", "Dispatch note: "),
            "الفاتورة: " to Pair("Facture : ", "Invoice: "),
            "حالة الدفع: " to Pair("État du paiement : ", "Payment status: "),
            "تاريخ الفاتورة: " to Pair("Date de facture : ", "Invoice date: "),
            "تاريخ الدفع: " to Pair("Date de paiement : ", "Payment date: "),
            "ملاحظات: " to Pair("Remarques : ", "Notes: "),
            "نظام متابعة بيع النفايات الصناعية وإدارة العقود والتقارير." to Pair("Système de suivi des ventes de déchets industriels, des contrats et des rapports.", "Industrial waste sales, contract and reporting system."),
            "تصدير أو مشاركة التقرير" to Pair("Exporter ou partager le rapport", "Export or share report"),
            "أدخل كمية إنتاج صحيحة بالكيلوغرام." to Pair("Saisissez une quantité de production valide en kilogrammes.", "Enter a valid production quantity in kilograms."),
            "تم حفظ إنتاج الشهر ومزامنته." to Pair("Production mensuelle enregistrée et synchronisée.", "Monthly production saved and synced."),
            "حُفظ محلياً، تعذرت المزامنة: " to Pair("Enregistré localement, synchronisation impossible : ", "Saved locally; sync failed: "),
            "تصدير" to Pair("Exporter", "Export"),
            "العربية" to Pair("Arabe", "Arabic")

        )
        val pair = translations[value] ?: return value
        return if (appLanguage == "fr") pair.first else pair.second
    }


    private fun untr(value: String): String {
        if (appLanguage == "ar") return value
        val reverse = mapOf(
            "Cartons et déchets de papier" to "صناديق كرتونية ونفايات الورق", "Cardboard boxes and paper waste" to "صناديق كرتونية ونفايات الورق",
            "Films plastiques et emballages" to "الأغلفة البلاستيكية (المطاطية وأغلفة التغليف)", "Plastic wrapping films and packaging" to "الأغلفة البلاستيكية (المطاطية وأغلفة التغليف)",
            "Sacs Big Bags" to "أكياس Big Bags", "Big Bags" to "أكياس Big Bags", "Fûts métalliques" to "براميل معدنية", "Metal drums" to "براميل معدنية",
            "Palettes et chutes de bois" to "نفايات المنصات الخشبية وبقايا الخشب", "Wooden pallets and wood scraps" to "نفايات المنصات الخشبية وبقايا الخشب",
            "Sacs et emballages PE contaminés par des détergents, non valorisables" to "أكياس وأغلفة PE ملوثة بالمنظفات وغير قابلة للاسترجاع",
            "PE bags and packaging contaminated with detergents, not recoverable" to "أكياس وأغلفة PE ملوثة بالمنظفات وغير قابلة للاسترجاع",
            "Autres déchets industriels (à ajouter)" to "نفايات صناعية أخرى (تُضاف لاحقاً)", "Other industrial waste (to be added)" to "نفايات صناعية أخرى (تُضاف لاحقاً)",
            "Production" to "الإنتاج", "Magasins" to "المخازن", "Warehouses" to "المخازن", "Maintenance" to "الصيانة",
            "Laboratoire" to "المخبر", "Laboratory" to "المخبر", "Traitement des eaux" to "معالجة المياه", "Water treatment" to "معالجة المياه",
            "Administration" to "الإدارة", "Autre" to "أخرى", "Other" to "أخرى", "Sortie vers l’acheteur" to "خروج إلى الشركة المشترية",
            "Dispatch to buyer" to "خروج إلى الشركة المشترية", "Vente directe" to "بيع مباشر", "Non défini" to "غير محدد",
            "Unspecified" to "غير محدد", "Non facturé" to "غير مفوتر", "Not invoiced" to "غير مفوتر",
            "Facture émise - non payée" to "فاتورة صادرة - غير مدفوع", "Invoice issued - unpaid" to "فاتورة صادرة - غير مدفوع",
            "Payé" to "مدفوع", "Paid" to "مدفوع", "En retard" to "متأخر", "Overdue" to "متأخر",
            "kg" to "كغ", "unité" to "وحدة", "unit" to "وحدة", "Selon quantité" to "حسب الكمية", "By quantity" to "حسب الكمية",
            "Non vendable" to "غير قابل للبيع", "Not for sale" to "غير قابل للبيع"
        )
        return reverse[value] ?: value
    }

    private fun buildScreen() {
        val scroll = ScrollView(this)
        root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutDirection = if (appLanguage == "ar") View.LAYOUT_DIRECTION_RTL else View.LAYOUT_DIRECTION_LTR
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
            text = tr("نظام تسيير واسترجاع النفايات الصناعية — مصنع هنكل، ولاية ميلة")
            textSize = 20f
            setTypeface(null, Typeface.BOLD)
            setTextColor(0xFFFFFFFF.toInt())
            gravity = Gravity.RIGHT
        }, matchWrap())
        hero.addView(TextView(this).apply {
            text = tr("HENKEL MILA • العقود • التتبع • التثمين • التقارير")
            textSize = 14f
            setTextColor(0xFFD8F4EA.toInt())
            gravity = Gravity.RIGHT
            setPadding(0, dp(7), 0, 0)
        }, matchWrap())
        root.addView(hero, matchWrap())

        // Always-visible settings shortcut so users do not need to scroll the horizontal navigation.
        root.addView(actionButton("⚙  الإعدادات واللغة") { showPage("settings") }, matchWrap())

        val navScroll = HorizontalScrollViewCompat(this)
        val nav = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; layoutDirection = View.LAYOUT_DIRECTION_RTL }
        navScroll.addView(nav)
        listOf(
            Triple("لوحة القيادة", "dashboard", "▦"),
            Triple("تسجيل نفايات", "form", "＋"),
            Triple("السجلات", "records", "☷"),
            Triple("مداخيل البيع", "finance", "دج"),
            Triple("التقارير والنسخ", "reports", "⇩"),
            Triple("العقود والأسعار", "contracts", "§"),
            Triple("الإعدادات", "settings", "⚙")
        ).forEach { item ->
            val b = Button(this).apply {
                text = "${item.third}  ${tr(item.first)}"
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
        quick.addView(actionButton("☁ ربط الحساب والمزامنة") { showCloudDialog() }, matchWrap())
        quick.addView(actionButton("⟳ مزامنة البيانات مع الحاسوب") { syncCloud(true) }, matchWrap())
        dashboardPage.addView(quick, matchWrap())
        pages["dashboard"] = dashboardPage

        formPage = newPage("تسجيل عملية نفايات")
        dateInput = field(formPage, "التاريخ (YYYY-MM-DD)").apply { setText(today()) }
        contractSpinner = spinner(formPage, contractItems.map { it.contractReference }.distinct())
        contractSpinner.setSelection((contractItems.map { it.contractReference }.distinct().indexOf(activeContractReference)).coerceAtLeast(0))
        typeSpinner = spinner(formPage, types)
        departmentSpinner = spinner(formPage, departments)
        quantityInput = field(formPage, "الكمية الإجمالية بالكيلوغرام", true)
        destinationSpinner = spinner(formPage, destinations)
        incomeInput = field(formPage, "مداخيل البيع (تحسب آلياً من العقد)", true)
        operationSpinner = spinner(formPage, listOf("بيع مباشر"))
        buyerInput = field(formPage, "الشركة المشترية (للبيع فقط)")
        contractSpinner.onItemSelectedListener = object : android.widget.AdapterView.OnItemSelectedListener {
            override fun onNothingSelected(parent: android.widget.AdapterView<*>?) {}
            override fun onItemSelected(parent: android.widget.AdapterView<*>?, view: View?, position: Int, id: Long) {
                activeContractReference = contractSpinner.selectedItem?.toString() ?: activeContractReference
                typeSpinner.adapter = ArrayAdapter(this@MainActivity, android.R.layout.simple_spinner_dropdown_item,
                    contractItems.filter { it.contractReference == activeContractReference }.map { tr(it.name) } + tr("نفايات صناعية أخرى (تُضاف لاحقاً)"))
                updateIncomePreview()
            }
        }
        typeSpinner.onItemSelectedListener = object : android.widget.AdapterView.OnItemSelectedListener {
            override fun onNothingSelected(parent: android.widget.AdapterView<*>?) {}
            override fun onItemSelected(parent: android.widget.AdapterView<*>?, view: View?, position: Int, id: Long) {
                val item = contractItem(untr(typeSpinner.selectedItem?.toString() ?: ""))
                if (item != null) {
                    buyerInput.setText(if (item.method == "بيع") item.companyName else "")
                    operationSpinner.setSelection(0)
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

        financePage = newPage("مداخيل البيع")
        pages["finance"] = financePage

        reportsPage = newPage("التقارير والنسخ الاحتياطي")
        reportsPage.addView(actionButton("تصدير تقرير CSV") { shareCsv() }, matchWrap())
        reportsPage.addView(actionButton("مشاركة / حفظ نسخة احتياطية") { backupData() }, matchWrap())
        reportsPage.addView(actionButton("استيراد نسخة احتياطية") { importBackup() }, matchWrap())
        reportsPage.addView(actionButton("عرض ملخص حسب نوع النفايات") { showTypeSummary() }, matchWrap())
        reportsPage.addView(TextView(this).apply {
            text = tr("ملاحظة: تحفظ البيانات محليًا وتُزامَن مع حساب هنكل المشترك عند الاتصال. احتفظ بنسخة احتياطية دورية، ولا تشارك بيانات الدخول أو مفاتيح الإدارة.")
            textSize = 14f
            setTextColor(0xFF795500.toInt())
            setPadding(dp(12), dp(12), dp(12), dp(12))
            setBackgroundColor(0xFFFFF2CC.toInt())
        }, matchWrap())
        pages["reports"] = reportsPage

        contractsPage = newPage("العقود والأسعار")
        contractsPage.addView(actionButton("فتح نافذة إدارة العقود") { showContractManager() }, matchWrap())
        contractsPage.addView(actionButton("إنشاء عقد جديد") { createContract() }, matchWrap())
        contractsPage.addView(actionButton("تعديل أسعار البنود") { editContractPrices() }, matchWrap())
        contractsPage.addView(actionButton("إضافة بند تعاقدي") { addContractItem() }, matchWrap())
        contractsPage.addView(actionButton("عرض شروط العقد") { showContractTerms() }, matchWrap())
        contractsPage.addView(actionButton("مزامنة العقود") { syncCloud(true) }, matchWrap())
        pages["contracts"] = contractsPage

        settingsPage = newPage("الإعدادات")
        settingsPage.addView(TextView(this).apply { text = tr("اللغة الشخصية للتطبيق"); textSize = 18f; setTypeface(null, Typeface.BOLD); setTextColor(0xFF173650.toInt()) }, matchWrap())
        settingsPage.addView(TextView(this).apply { text = tr("اختر لغة واجهة التطبيق على هذا الجهاز. الاختيار محفوظ بشكل مستقل ولا يغيّر إعدادات بقية المستخدمين."); textSize = 14f; setTextColor(0xFF526575.toInt()); setPadding(0, dp(6), 0, dp(10)) }, matchWrap())
        languageSpinner = spinner(settingsPage, listOf("العربية", "Français", "English"))
        languageSpinner.setSelection(when (appLanguage) { "fr" -> 1; "en" -> 2; else -> 0 })
        settingsPage.addView(actionButton("حفظ اللغة المختارة") { saveLanguagePreference() }, matchWrap())
        settingsPage.addView(TextView(this).apply { text = tr("إعدادات إضافية"); textSize = 18f; setTypeface(null, Typeface.BOLD); setTextColor(0xFF173650.toInt()); setPadding(0, dp(18), 0, dp(8)) }, matchWrap())
        settingsPage.addView(actionButton("النسخ الاحتياطي واسترجاع البيانات") { backupData() }, matchWrap())
        settingsPage.addView(actionButton("إعدادات المزامنة") { showCloudDialog() }, matchWrap())
        settingsPage.addView(actionButton("حول التطبيق") { AlertDialog.Builder(this).setTitle("HENKEL ALGÉRIE – ZAKARY GRD").setMessage("نظام متابعة بيع النفايات الصناعية وإدارة العقود والتقارير.").setPositiveButton("إغلاق", null).show() }, matchWrap())
        pages["settings"] = settingsPage

        refreshDashboard()
        refreshFinance()
    }

    private fun newPage(title: String): LinearLayout {
        val page = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutDirection = if (appLanguage == "ar") View.LAYOUT_DIRECTION_RTL else View.LAYOUT_DIRECTION_LTR
            setPadding(dp(2), dp(12), dp(2), dp(12))
            visibility = View.GONE
        }
        page.addView(TextView(this).apply {
            text = tr(title)
            textSize = 23f
            setTypeface(null, Typeface.BOLD)
            setTextColor(0xFF173650.toInt())
            gravity = if (appLanguage == "ar") Gravity.RIGHT else Gravity.LEFT
            setPadding(dp(2), dp(8), dp(2), dp(14))
        }, matchWrap())
        contentHost.addView(page, matchWrap())
        return page
    }

    private fun saveLanguagePreference() {
        appLanguage = when (languageSpinner.selectedItemPosition) { 1 -> "fr"; 2 -> "en"; else -> "ar" }
        getSharedPreferences("zakary_settings", MODE_PRIVATE).edit().putString("language", appLanguage).apply()
        Toast.makeText(this, tr("تم حفظ تفضيل اللغة على هذا الجهاز."), Toast.LENGTH_LONG).show()
        buildScreen()
        renderRecords()
        showPage("settings")
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
            hint = tr(hintText)
            textSize = 15f
            gravity = Gravity.RIGHT or Gravity.CENTER_VERTICAL
            layoutDirection = if (appLanguage == "ar") View.LAYOUT_DIRECTION_RTL else View.LAYOUT_DIRECTION_LTR
            gravity = if (appLanguage == "ar") Gravity.RIGHT or Gravity.CENTER_VERTICAL else Gravity.LEFT or Gravity.CENTER_VERTICAL
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
            adapter = ArrayAdapter(this@MainActivity, android.R.layout.simple_spinner_dropdown_item, items.map { tr(it) })
            layoutDirection = if (appLanguage == "ar") View.LAYOUT_DIRECTION_RTL else View.LAYOUT_DIRECTION_LTR
            setBackgroundColor(0xFFFFFFFF.toInt())
        }
        val p = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(48))
        p.bottomMargin = dp(8)
        parent.addView(s, p)
        return s
    }

    private fun actionButton(label: String, action: () -> Unit): Button {
        return Button(this).apply {
            text = tr(label)
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
        val recovered = 0.0
        val cost = 0.0
        val selectedType = untr(typeSpinner.selectedItem.toString())
        val operation = "بيع مباشر"
        val item = contractItem(selectedType)
        val buyer = buyerInput.text.toString().trim()
        val income = if (operation == "بيع مباشر") {
            if (item == null || item.method != "بيع") {
                Toast.makeText(this, "لا يمكن بيع هذا النوع دون بند بيع في العقد.", Toast.LENGTH_LONG).show(); return
            }
            if (buyer.isBlank() || buyer != item.companyName) { Toast.makeText(this, "الشركة المشترية يجب أن تطابق الشركة المحددة في العقد.", Toast.LENGTH_LONG).show(); return }
            (quantity ?: 0.0) * item.price
        } else 0.0
        if (date.isBlank() || quantity == null || quantity <= 0) {
            Toast.makeText(this, "تحقق من التاريخ والكمية.", Toast.LENGTH_LONG).show()
            return
        }
        val newId = UUID.randomUUID().toString()
        records.add(0, WasteRecord(newId, date, selectedType,
            untr(departmentSpinner.selectedItem.toString()), quantity, recovered, untr(destinationSpinner.selectedItem.toString()),
            cost, income, noteInput.text.toString().trim(), if (operation == "بيع مباشر") buyer else "", operation,
            dispatchInput.text.toString().trim(), invoiceNumberInput.text.toString().trim(), invoiceDateInput.text.toString().trim(), untr(paymentStatusSpinner.selectedItem.toString()), paymentDateInput.text.toString().trim()))
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
        val item = contractItem(untr(typeSpinner.selectedItem?.toString() ?: ""))
        val q = number(quantityInput, 0.0) ?: 0.0
        val sale = true
        incomeInput.setText(if (sale && item?.method == "بيع") fmt(q * item.price) else "0")
        if (sale && item != null && item.method == "بيع" && ::buyerInput.isInitialized && buyerInput.text.isNullOrBlank()) buyerInput.setText(item.companyName)
    }

    private fun clearForm() {
        dateInput.setText(today())
        quantityInput.setText("")
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
                text = if (records.isEmpty()) tr("لا توجد سجلات بعد. اضغط «تسجيل نفايات» لإضافة أول عملية.") else tr("لا توجد نتائج مطابقة للبحث.")
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
                text = "الكمية: ${fmt(r.quantity)} ${contractItem(r.type)?.unit ?: "وحدة"}\nالعملية: ${r.operationType} | الشركة: ${r.buyerCompany.ifBlank { "—" }}\nالوجهة: ${r.destination}\nمداخيل البيع: ${fmt(r.income)} دج\nوصل الخروج: ${r.dispatchReference.ifBlank { "—" }} | الفاتورة: ${r.invoiceNumber.ifBlank { "—" }}\nحالة الدفع: ${r.paymentStatus} | تاريخ الفاتورة: ${r.invoiceDate.ifBlank { "—" }} | تاريخ الدفع: ${r.paymentDate.ifBlank { "—" }}" +
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
        val q = EditText(this).apply { hint = tr("الكمية الإجمالية كغ"); setText(fmt(r.quantity)) }
        val income = EditText(this).apply { hint = tr("المداخيل دج (تحسب من العقد)"); setText(fmt(r.income)); isEnabled = false }
        val dispatch = EditText(this).apply { hint = "مرجع وصل الخروج"; setText(r.dispatchReference) }
        val invoiceNo = EditText(this).apply { hint = "رقم الفاتورة"; setText(r.invoiceNumber) }
        val invoiceDate = EditText(this).apply { hint = "تاريخ الفاتورة YYYY-MM-DD"; setText(r.invoiceDate) }
        val paymentStatus = EditText(this).apply { hint = "حالة الدفع"; setText(r.paymentStatus) }
        val paymentDate = EditText(this).apply { hint = "تاريخ الدفع YYYY-MM-DD"; setText(r.paymentDate) }
        listOf(q, income, dispatch, invoiceNo, invoiceDate, paymentStatus, paymentDate).forEach { panel.addView(it) }
        listOf(q, income).forEach { it.inputType = android.text.InputType.TYPE_CLASS_NUMBER or android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL }
        AlertDialog.Builder(this).setTitle("تعديل ${r.type}").setView(panel)
            .setNegativeButton("إلغاء", null)
            .setPositiveButton("حفظ") { _, _ ->
                val nq = q.text.toString().replace(',', '.').toDoubleOrNull()
                val niInput = income.text.toString().replace(',', '.').toDoubleOrNull()
                if (nq == null || niInput == null || nq <= 0 || niInput < 0) {
                    Toast.makeText(this, "قيم غير صحيحة؛ لم يتم حفظ التعديل.", Toast.LENGTH_LONG).show()
                } else {
                    // Preserve the sale's original unit price; new contract prices affect future sales only.
                    val historicalUnitPrice = if (r.quantity > 0.0) r.income / r.quantity else 0.0
                    val ni = if (r.operationType == "بيع مباشر") nq * historicalUnitPrice else 0.0
                    val index = records.indexOfFirst { it.id == r.id }
                    if (index >= 0) records[index] = r.copy(quantity=nq, income=ni, dispatchReference=dispatch.text.toString().trim(), invoiceNumber=invoiceNo.text.toString().trim(), invoiceDate=invoiceDate.text.toString().trim(), paymentStatus=paymentStatus.text.toString().trim().ifBlank { "غير محدد" }, paymentDate=paymentDate.text.toString().trim())
                    cloud.markRecordDirty(r.id)
                    persistRecords(); renderRecords(); refreshDashboard(); refreshFinance()
                    if (cloud.connected()) syncCloud(false)
                }
            }.show()
    }

    private fun refreshDashboard() {
        if (!::dashboardStats.isInitialized) return
        dashboardStats.removeAllViews()
        val sales = records.filter { it.operationType == "بيع مباشر" }
        val unitGroups = sales.groupBy { contractItem(it.type)?.unit ?: "وحدة" }
        val total = unitGroups.entries.joinToString(" + ") { (unit, rows) -> "${fmt(rows.sumOf { it.quantity })} $unit" }.ifBlank { "0" }
        val income = sales.sumOf { it.income }
        dashboardStats.addView(metric(tr("إجمالي الكميات المسجلة"), total), matchWrap())
        dashboardStats.addView(metric(tr("إجمالي مداخيل البيع"), "${fmt(income)} دج"), matchWrap())
        dashboardStats.addView(metric(tr("عدد عمليات البيع"), sales.size.toString()), matchWrap())
        val box = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(12), dp(12), dp(12), dp(12)); setBackgroundColor(0xFFFFFFFF.toInt()) }
        box.addView(TextView(this).apply { text="المداخيل حسب نوع النفايات"; textSize=18f; setTypeface(null, Typeface.BOLD); setTextColor(0xFF173650.toInt()) }, matchWrap())
        sales.groupBy { it.type }.forEach { (type, rows) ->
            val q=rows.sumOf { it.quantity }; val money=rows.sumOf { it.income }; val unit=contractItem(type)?.unit ?: "وحدة"
            box.addView(TextView(this).apply { text="$type\n${tr("الكمية المباعة: ")}${fmt(q)} $unit\n${tr("مداخيل البيع: ")}${fmt(money)} دج"; textSize=14f; setTextColor(0xFF243746.toInt()); setPadding(0,dp(7),0,dp(7)) },matchWrap())
        }
        dashboardStats.addView(box, matchWrap())
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
        val sales = records.filter { it.operationType == "بيع مباشر" }
        financePage.addView(metric(tr("إجمالي مداخيل البيع"), "${fmt(sales.sumOf { it.income })} دج"), matchWrap())
        financePage.addView(actionButton("＋ تسجيل عملية بيع") { showPage("form") }, matchWrap())
        financePage.addView(TextView(this).apply { text=tr("تفصيل المداخيل حسب نوع النفاية"); textSize=19f; setTypeface(null,Typeface.BOLD); setTextColor(0xFF173650.toInt()); setPadding(0,dp(14),0,dp(8)) },matchWrap())
        sales.groupBy { it.type }.forEach { (type, rows) ->
            val q=rows.sumOf { it.quantity }; val income=rows.sumOf { it.income }; val unit=contractItem(type)?.unit ?: "وحدة"
            financePage.addView(metric(type, "${tr("الكمية المباعة: ")}${fmt(q)} $unit\n${tr("مداخيل البيع: ")}${fmt(income)} دج"), matchWrap())
        }
    }

    private fun persistContracts() {
        val a = JSONArray()
        contractItems.forEach { c -> a.put(JSONObject().apply { put("name",c.name); put("price",c.price); put("unit",c.unit); put("method",c.method); put("companyName",c.companyName); put("contractReference",c.contractReference) }) }
        getSharedPreferences("henkel_contracts", MODE_PRIVATE).edit().putString("items", a.toString()).apply()
    }
    private fun loadContracts() {
        try {
            val raw = getSharedPreferences("henkel_contracts", MODE_PRIVATE).getString("items", null) ?: return
            val a = JSONArray(raw); if (a.length() == 0) return
            contractItems.clear()
            for (i in 0 until a.length()) { val o=a.getJSONObject(i); contractItems.add(ContractItem(o.optString("name"),o.optDouble("price"),o.optString("unit","كغ"),o.optString("method","بيع"),o.optString("companyName").ifBlank { if (o.optString("method","بيع")=="بيع") "SNC Groupe EL BADISSIA" else "" },o.optString("contractReference","NEW CONTRAT 2026"))) }
            activeContractReference = getSharedPreferences("henkel_contracts", MODE_PRIVATE).getString("active_reference", contractItems.lastOrNull()?.contractReference ?: activeContractReference) ?: activeContractReference
            activeContractCompany = getSharedPreferences("henkel_contracts", MODE_PRIVATE).getString("active_company", contractItems.lastOrNull()?.companyName ?: activeContractCompany) ?: activeContractCompany
        } catch (_: Exception) { }
    }
    private fun refreshContracts() {
        if (!::contractsPage.isInitialized) return
        while (contractsPage.childCount > 5) contractsPage.removeViewAt(5)
        contractItems.forEach { c ->
            val box = LinearLayout(this).apply { orientation=LinearLayout.VERTICAL; setPadding(dp(12),dp(10),dp(12),dp(10)); setBackgroundColor(0xFFFFFFFF.toInt()) }
            box.addView(TextView(this).apply { text=c.name; textSize=16f; setTypeface(null,Typeface.BOLD); setTextColor(0xFF247A5A.toInt()) }, matchWrap())
            box.addView(TextView(this).apply { text=tr("مرجع العقد: ") + c.contractReference; textSize=12f; setTextColor(0xFF607583.toInt()) }, matchWrap())
            box.addView(TextView(this).apply { text="${tr("الشركة: ")}${c.companyName.ifBlank { tr("غير محددة") }}\n"+(if(c.method=="غير قابل للبيع") tr("هذا النوع غير مدرج للبيع في العقد") else "${tr("السعر: ")}${fmt(c.price)} دج / ${c.unit}  •  ${tr("طريقة التعامل: ")}${tr(c.method)}"); textSize=14f; setTextColor(0xFF243746.toInt()); setPadding(0,dp(5),0,0) }, matchWrap())
            val p=LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.WRAP_CONTENT); p.bottomMargin=dp(8); contractsPage.addView(box,p)
        }
        contractsPage.addView(TextView(this).apply { text="ملاحظات العقد: الفوترة في نهاية الشهر، والدفع خلال 10 أيام من استلام الفاتورة. أوقات الجمع المذكورة: الأحد إلى الخميس، 08:00–16:00. مدة العقد سنة قابلة للتجديد باتفاق الطرفين وملحق. لا تعتمد هذه الملخصات بدل العقد الأصلي الموقع."; textSize=13f; setTextColor(0xFF795500.toInt()); setPadding(dp(12),dp(12),dp(12),dp(12)); setBackgroundColor(0xFFFFF2CC.toInt()) }, matchWrap())
    }
    private fun showContractManager() {
        val panel = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(8), 0, dp(8), 0) }
        val contracts = contractItems.map { it.contractReference }.distinct()
        panel.addView(TextView(this).apply { text = tr("عدد العقود: ") + contracts.size + "   •   " + tr("عدد البنود: ") + contractItems.size; textSize = 16f; setPadding(0, dp(8), 0, dp(12)) }, matchWrap())
        panel.addView(TextView(this).apply { text = tr("العقد النشط: ") + activeContractReference + "\n" + tr("الشركة: ") + activeContractCompany; textSize = 14f }, matchWrap())
        val dialog = AlertDialog.Builder(this).setTitle(tr("إدارة العقود والأسعار")).setView(panel).setNegativeButton(tr("إغلاق"), null).create()
        panel.addView(actionButton("إنشاء عقد جديد") { dialog.dismiss(); createContract() }, matchWrap())
        panel.addView(actionButton("إضافة بند إلى العقد النشط") { dialog.dismiss(); addContractItem() }, matchWrap())
        panel.addView(actionButton("تعديل سعر بند") { dialog.dismiss(); editContractPrices() }, matchWrap())
        panel.addView(actionButton("عرض قائمة البنود والأسعار") { dialog.dismiss(); refreshContracts() }, matchWrap())
        dialog.show()
    }

    private fun createContract() {
        val panel = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(8), 0, dp(8), 0) }
        val ref = EditText(this).apply { hint = tr("مرجع العقد / رقم العقد"); setText("CONTRAT-" + today()) }
        val company = EditText(this).apply { hint = tr("اسم الشركة المتعاقدة"); setText(activeContractCompany) }
        val effective = EditText(this).apply { hint = tr("تاريخ بداية العقد YYYY-MM-DD") }
        listOf(ref, company, effective).forEach { panel.addView(it) }
        AlertDialog.Builder(this).setTitle(tr("إنشاء عقد جديد")).setView(panel)
            .setNegativeButton(tr("إلغاء"), null).setPositiveButton(tr("حفظ العقد")) { _, _ ->
                val r = ref.text.toString().trim(); val c = company.text.toString().trim()
                if (r.isBlank() || c.isBlank()) Toast.makeText(this, tr("أدخل مرجع العقد واسم الشركة."), Toast.LENGTH_LONG).show()
                else {
                    activeContractReference = r; activeContractCompany = c
                    getSharedPreferences("henkel_contracts", MODE_PRIVATE).edit().putString("active_reference", r).putString("active_company", c).putString("active_effective", effective.text.toString().trim()).apply()
                    Toast.makeText(this, tr("تم إنشاء العقد. أضف بنوده وأسعاره الآن."), Toast.LENGTH_LONG).show()
                    addContractItem()
                }
            }.show()
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
        val company=EditText(this).apply { hint=tr("الشركة المتعاقدة / المشترية"); setText(activeContractCompany) }
        val reference=EditText(this).apply { hint=tr("مرجع العقد / رقم العقد"); setText(activeContractReference) }
        listOf(name,price,unit,method,company,reference).forEach { panel.addView(it) }
        AlertDialog.Builder(this).setTitle("إضافة بند تعاقدي جديد").setView(panel).setNegativeButton("إلغاء",null)
            .setPositiveButton("إضافة") { _, _ ->
                val n=name.text.toString().trim(); val p=price.text.toString().replace(',','.').toDoubleOrNull(); val u=unit.text.toString().trim().ifBlank{"كغ"}; val m=method.text.toString().trim().ifBlank{"بيع"}; val co=company.text.toString().trim(); val cr=reference.text.toString().trim()
                if(n.isBlank() || p==null || p<0) Toast.makeText(this,"أدخل اسم البند وسعراً صحيحاً",Toast.LENGTH_LONG).show()
                else { if(contractItems.any{it.name==n && it.contractReference==cr}) Toast.makeText(this,tr("هذا البند موجود مسبقاً في هذا العقد"),Toast.LENGTH_SHORT).show() else { contractItems.add(ContractItem(n,p,u,m,co,cr)); activeContractReference=cr; activeContractCompany=co; val refs=contractItems.map { it.contractReference }.distinct(); contractSpinner.adapter=ArrayAdapter(this@MainActivity,android.R.layout.simple_spinner_dropdown_item,refs); contractSpinner.setSelection(refs.indexOf(cr)); typeSpinner.adapter=ArrayAdapter(this@MainActivity,android.R.layout.simple_spinner_dropdown_item,types.map { tr(it) }); persistContracts(); cloud.markContractsDirty(); refreshContracts(); if(cloud.connected()) syncCloud(false); Toast.makeText(this,tr("تمت إضافة البند"),Toast.LENGTH_SHORT).show() } }
            }.show()
    }
    private fun showContractTerms() {
        AlertDialog.Builder(this).setTitle("بيانات عقد هنكل")
            .setMessage("الطرف: HENKEL ALGÉRIE SPA\\nالمشتري: SNC GROUPE EL BADISSIA\\nمرجع الوثيقة: NEW CONTRAT 2026\\nتاريخ السريان: 08-08-2026\\nتاريخ التحرير: 29-09-2026\\nالمدة: 12 شهراً، والتجديد بملحق موقع من الطرفين\\nالفوترة: نهاية كل شهر\\nالدفع: خلال 10 أيام من استلام الفاتورة\\nالجمع: الأحد إلى الخميس، 08:00–16:00\\nتقرير مفصل كل ثلاثة أشهر\\nالأسعار شاملة الرسوم وثابتة طوال مدة العقد\\nلا يجوز بيع النفايات داخل موقع هنكل، ولا الاستعانة بمشترٍ آخر دون موافقة كتابية مسبقة\\n\\nالأسعار والأنواع هي بنود العقد المرفق، وتظل قابلة للتعديل فقط بملحق تعاقدي موقع.")
            .setPositiveButton("إغلاق",null).show()
    }

    private fun showTypeSummary() {
        val lines = types.mapNotNull { type ->
            val subset = records.filter { it.type == type && it.operationType == "بيع مباشر" }
            if (subset.isEmpty()) null else { val unit = contractItem(type)?.unit ?: "وحدة"; val income = subset.sumOf { it.income }; "$type: الكمية المباعة ${fmt(subset.sumOf { it.quantity })} $unit، السعر ${fmt(contractItem(type)?.price ?: 0.0)} دج/$unit، المداخيل ${fmt(income)} دج" }
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
        val email = EditText(this).apply { hint = "البريد الإلكتروني لحساب المزامنة"; setText(cloud.email()); inputType = android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS }
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
            append("Date,Type,Department,Quantity,Unit,Unit_Price_DZD,Buyer,Quantity_Sold,Sales_Income_DZD,Dispatch_Reference,Invoice,Payment_Status,Notes\n")
            records.filter { it.operationType == "بيع مباشر" }.forEach { r ->
                append(listOf(r.date,r.type,r.department,fmt(r.quantity),contractItem(r.type)?.unit ?: "وحدة",fmt(contractItem(r.type)?.price ?: 0.0),r.buyerCompany,fmt(r.quantity),fmt(r.income),r.dispatchReference,r.invoiceNumber,r.paymentStatus,r.note)
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
