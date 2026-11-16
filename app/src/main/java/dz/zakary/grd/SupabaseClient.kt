package dz.zakary.grd

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

class SupabaseClient(context: Context) {
    private val prefs = context.getSharedPreferences("henkel_cloud", Context.MODE_PRIVATE)
    private fun enc(s: String) = URLEncoder.encode(s, "UTF-8")
    fun configured() = !prefs.getString("url", "").isNullOrBlank() && !prefs.getString("key", "").isNullOrBlank()
    fun connected() = configured() && !prefs.getString("access_token", "").isNullOrBlank()
    fun endpoint() = prefs.getString("url", "") ?: ""

    @Synchronized fun signIn(projectUrl: String, publicKey: String, email: String, password: String) {
        val base = projectUrl.trim().trimEnd('/')
        require(base.startsWith("https://")) { "يجب أن يبدأ رابط المشروع بـ https://" }
        require(publicKey.isNotBlank() && email.isNotBlank() && password.isNotBlank()) { "أكمل رابط المشروع والمفتاح والبريد وكلمة المرور." }
        prefs.edit().putString("url", base).putString("key", publicKey.trim()).apply()
        val body = JSONObject().put("email", email.trim()).put("password", password)
        val result = JSONObject(call("$base/auth/v1/token?grant_type=password", "POST", body.toString(), false))
        saveSession(result)
    }

    private fun saveSession(o: JSONObject) {
        val user = o.optJSONObject("user") ?: throw IllegalStateException("لم يرجع خادم الدخول بيانات المستخدم.")
        val token = o.optString("access_token")
        val refresh = o.optString("refresh_token")
        if (token.isBlank() || refresh.isBlank()) throw IllegalStateException("لم يتم إنشاء جلسة دخول صالحة. تحقق من تأكيد البريد.")
        prefs.edit().putString("access_token", token).putString("refresh_token", refresh)
            .putString("user_id", user.optString("id"))
            .putLong("expires_at", System.currentTimeMillis() + o.optLong("expires_in", 3600L) * 1000L)
            .putString("email", user.optString("email")).apply()
    }

    fun email() = prefs.getString("email", "") ?: ""
    fun signOut() { prefs.edit().remove("access_token").remove("refresh_token").remove("user_id").remove("expires_at").remove("email").apply() }

    @Synchronized private fun refreshSession() {
        if (!connected()) throw IllegalStateException("اربط التطبيق بحساب Supabase أولاً.")
        if (System.currentTimeMillis() < prefs.getLong("expires_at", 0L) - 60000L) return
        val base = endpoint()
        val refresh = prefs.getString("refresh_token", "") ?: ""
        val body = JSONObject().put("refresh_token", refresh)
        val result = JSONObject(call("$base/auth/v1/token?grant_type=refresh_token", "POST", body.toString(), false))
        saveSession(result)
    }

    private fun call(url: String, method: String, body: String?, auth: Boolean = true, prefer: String? = null): String {
        val c = (URL(url).openConnection() as HttpURLConnection)
        c.requestMethod = method
        c.connectTimeout = 20000
        c.readTimeout = 30000
        c.setRequestProperty("apikey", prefs.getString("key", "") ?: "")
        c.setRequestProperty("Content-Type", "application/json")
        if (auth) c.setRequestProperty("Authorization", "Bearer " + (prefs.getString("access_token", "") ?: ""))
        if (prefer != null) c.setRequestProperty("Prefer", prefer)
        if (body != null) { c.doOutput = true; c.outputStream.use { it.write(body.toByteArray(Charsets.UTF_8)) } }
        val code = c.responseCode
        val stream = if (code in 200..299) c.inputStream else c.errorStream
        val text = stream?.bufferedReader(Charsets.UTF_8)?.use(BufferedReader::readText) ?: ""
        c.disconnect()
        if (code !in 200..299) throw IllegalStateException("خادم المزامنة ($code): " + text.take(500))
        return text
    }

    fun sync(localRecords: List<WasteRecord>, localContracts: List<ContractItem>): Pair<List<WasteRecord>, List<ContractItem>> {
        refreshSession()
        val base = endpoint() + "/rest/v1/"
        val uid = prefs.getString("user_id", "") ?: throw IllegalStateException("جلسة المستخدم غير صالحة.")
        val dirtyRecords = prefs.getStringSet("dirty_records", emptySet())!!.toMutableSet()
        val dirtyContracts = prefs.getStringSet("dirty_contracts", emptySet())!!.toMutableSet()
        if (!prefs.getBoolean("first_sync_done", false)) {
            dirtyRecords.addAll(localRecords.map { it.id })
            dirtyContracts.addAll(localContracts.map { it.name })
        }

        // Pull first so edits made on the web are not overwritten by stale local copies.
        val remoteBefore = JSONArray(call(base + "waste_records?select=*&order=event_date.desc", "GET", null))
        val remoteIds = (0 until remoteBefore.length()).map { remoteBefore.getJSONObject(it).optString("id") }.toSet()
        val localById = localRecords.associateBy { it.id }
        val recordPayload = JSONArray()
        dirtyRecords.forEach { id ->
            val r = localById[id] ?: return@forEach
            recordPayload.put(JSONObject().put("id", r.id).put("user_id", uid).put("event_date", r.date)
                .put("waste_type", r.type).put("department", r.department).put("quantity", r.quantity)
                .put("recovered", r.recovered).put("destination", r.destination).put("cost_dzd", r.cost)
                .put("income_dzd", r.income).put("note", r.note))
        }
        if (recordPayload.length() > 0) call(base + "waste_records?on_conflict=id", "POST", recordPayload.toString(), true, "resolution=merge-duplicates,return=minimal")

        val deleted = prefs.getStringSet("deleted_records", emptySet())!!.toMutableSet()
        if (deleted.isNotEmpty()) {
            val safeIds = deleted.filter { it.matches(Regex("[0-9a-fA-F-]{36}")) }
            if (safeIds.isNotEmpty()) call(base + "waste_records?id=in.(" + safeIds.joinToString(",") + ")", "DELETE", null, true, "return=minimal")
        }

        val contractPayload = JSONArray()
        localContracts.filter { dirtyContracts.contains(it.name) || dirtyContracts.contains("*") }.forEach { item ->
            contractPayload.put(JSONObject().put("user_id", uid).put("name", item.name).put("price", item.price)
                .put("unit", item.unit).put("method", item.method))
        }
        if (contractPayload.length() > 0) call(base + "contract_items?on_conflict=user_id%2Cname", "POST", contractPayload.toString(), true, "resolution=merge-duplicates,return=minimal")

        val remoteRecords = JSONArray(call(base + "waste_records?select=*&order=event_date.desc", "GET", null))
        val merged = LinkedHashMap<String, WasteRecord>()
        for (i in 0 until remoteRecords.length()) {
            val o = remoteRecords.getJSONObject(i)
            val id = o.optString("id")
            merged[id] = WasteRecord(id, o.optString("event_date"), o.optString("waste_type"), o.optString("department"),
                o.optDouble("quantity"), o.optDouble("recovered"), o.optString("destination"), o.optDouble("cost_dzd"),
                o.optDouble("income_dzd"), o.optString("note"))
        }
        // Preserve records created locally while offline; dirty ones were uploaded above.
        localRecords.forEach { if (!merged.containsKey(it.id)) merged[it.id] = it }
        val remoteContracts = JSONArray(call(base + "contract_items?select=*&order=name", "GET", null))
        val mergedContracts = mutableListOf<ContractItem>()
        for (i in 0 until remoteContracts.length()) {
            val o = remoteContracts.getJSONObject(i)
            mergedContracts.add(ContractItem(o.optString("name"), o.optDouble("price"), o.optString("unit", "كغ"), o.optString("method", "بيع")))
        }
        if (mergedContracts.isEmpty()) mergedContracts.addAll(localContracts)

        prefs.edit().putStringSet("dirty_records", emptySet()).putStringSet("dirty_contracts", emptySet())
            .putStringSet("deleted_records", emptySet()).putBoolean("first_sync_done", true).apply()
        return Pair(merged.values.toList(), mergedContracts)
    }

    fun markRecordDirty(id: String) {
        val s = prefs.getStringSet("dirty_records", emptySet())!!.toMutableSet(); s.add(id)
        prefs.edit().putStringSet("dirty_records", s).apply()
    }
    fun markRecordDeleted(id: String) {
        val s = prefs.getStringSet("deleted_records", emptySet())!!.toMutableSet(); s.add(id)
        val d = prefs.getStringSet("dirty_records", emptySet())!!.toMutableSet(); d.remove(id)
        prefs.edit().putStringSet("deleted_records", s).putStringSet("dirty_records", d).apply()
    }
    fun markContractsDirty() {
        val s = prefs.getStringSet("dirty_contracts", emptySet())!!.toMutableSet()
        // A blank marker means all local contract rows need to be upserted.
        s.add("*")
        prefs.edit().putStringSet("dirty_contracts", s).apply()
    }
}
