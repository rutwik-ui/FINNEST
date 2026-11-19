package com.finnest.app

import org.json.JSONArray
import org.json.JSONObject

/** Explicit native key ↔ shared journey payload mapping. Unknown native fields remain local. */
object NativeJourneyContract {
    private val budget = mapOf("loan" to "loan", "down_payment" to "down", "stamp_duty" to "stamp", "gst" to "gst", "brokerage" to "brokerage", "interiors" to "interiors", "contingency" to "contingency", "emi" to "emi", "maintenance" to "maintenance", "carpet_area" to "carpet", "accessibility_needs" to "access")
    private val location = mapOf("workplace" to "Workplace", "schools" to "Schools", "healthcare" to "Healthcare", "transport" to "Transport", "shopping" to "Shopping", "family" to "Family", "safety" to "Safety", "environment" to "Environment", "traffic" to "Traffic", "infrastructure" to "Future infrastructure", "development" to "Surrounding development")
    private val config = mapOf("1bhk" to "1 BHK", "2bhk" to "2 BHK", "3bhk" to "3 BHK", "villa" to "Villa / duplex", "parking" to "Parking", "amenities" to "Amenities", "accessibility" to "Accessibility", "vastu" to "Vastu (optional)")
    private val developer = mapOf("track_record" to "track", "rera" to "rera", "land_title" to "title", "approvals" to "approvals", "legal_history" to "legal", "financial_stability" to "financial")
    private val resaleGroups = mapOf("condition" to "resaleCondition", "seller" to "resaleSeller", "society" to "resaleSociety", "legal" to "resaleLegal", "handover" to "resaleHandover")
    private val checkField = Regex("^(.*)_(status|note|source|date|amount)$")
    private fun date(value: String): String { val parts = value.split('/'); return if (parts.size == 3) "${parts[2]}-${parts[1]}-${parts[0]}" else value }
    private fun nativeDate(value: String): String { val parts = value.split('-'); return if (parts.size == 3) "${parts[2]}/${parts[1]}/${parts[0]}" else value }
    private fun put(root: JSONObject, group: String, id: String, field: String, value: String) {
        val section = root.optJSONObject(group) ?: JSONObject().also { root.put(group, it) }
        val item = section.optJSONObject(id) ?: JSONObject().also { section.put(id, it) }
        item.put(field, if (field == "date") date(value) else value)
    }
    private fun obj(root: JSONObject, group: String, field: String, value: String) {
        val section = root.optJSONObject(group) ?: JSONObject().also { root.put(group, it) }; section.put(field, value)
    }
    private fun choice(root: JSONObject, group: String, label: String, selected: Boolean) {
        val old = root.optJSONArray(group) ?: JSONArray(); val values = (0 until old.length()).map { old.getString(it) }.toMutableList()
        values.removeAll { it == label }; if (selected) values.add(label); root.put(group, JSONArray(values))
    }
    fun apply(payload: JSONObject, scope: String, key: String, value: String): Boolean {
        if (scope == "local_profile") { obj(payload, "localProfile", key, value); return true }
        if (scope == "local_reminders" && key == "reminders_v1") { payload.put("reminders", JSONObject(value).getJSONArray("entries")); return true }
        if (scope == "resale_property") {
            if (key.startsWith("budget_")) { obj(payload,"resaleBudget",key.removePrefix("budget_"),value); return true }
            if (key.startsWith("config_") || key.startsWith("location_")) { val isConfig=key.startsWith("config_"); choice(payload,if(isConfig) "resaleConfiguration" else "resaleLocation",key.substringAfter('_'),value=="true"); return true }
            if (key in listOf("carpet_area","building_age","timeline")) { obj(payload,"resalePreferences",if(key=="carpet_area") "carpet" else key,value);return true }
            if (key=="legal_lawyer_status") { payload.put("resaleLawyerStatus",value);return true }
            for ((prefix, group) in resaleGroups) if (key.startsWith("${prefix}_")) { val m=checkField.matchEntire(key.removePrefix("${prefix}_")) ?: return false;put(payload,group,m.groupValues[1],m.groupValues[2],value);return true }
            return false
        }
        // The selected journey (New / Resale) lives in the New Property row, exactly where the website keeps it.
        if (key=="selected_journey") { if (value in listOf("New Property","Resale Property")) payload.put("journey",value); return true }
        budget[key]?.let { obj(payload,"budget",it,value);return true }
        if (key.startsWith("location_")) { val label=location[key.removePrefix("location_")] ?: return false;choice(payload,"location",label,value=="true");return true }
        if (key.startsWith("config_")) { val label=config[key.removePrefix("config_")] ?: return false;choice(payload,"configuration",label,value=="true");return true }
        if (key=="shortlist_projects_v1") { payload.put("projects",JSONArray(value));return true }
        if (key=="review_next_actions") { payload.put("nextActions",value);return true }
        if (key=="feedback_notes") { val rows=JSONArray(value); for(i in 0 until rows.length()){val r=rows.getJSONObject(i);if(!r.has("id"))r.put("id",java.util.UUID.randomUUID().toString());r.put("date",date(r.optString("date")))};payload.put("feedbackNotes",rows);return true }
        for ((prefix,group) in mapOf("developer" to "checks","visit" to "visitChecks","legal" to "legalChecks","financial_prompt" to "costChecks")) if (key.startsWith("${prefix}_")) {
            val m=checkField.matchEntire(key.removePrefix("${prefix}_")) ?: return false
            val id=if(prefix=="developer") developer[m.groupValues[1]] ?: return false else m.groupValues[1]
            put(payload,group,id,m.groupValues[2],value);return true
        }
        for ((prefix,group) in mapOf("financial_initial_" to "initialCosts","financial_recurring_" to "recurringCosts","financial_comparison_" to "loanTerms")) if(key.startsWith(prefix)){obj(payload,group,key.removePrefix(prefix),value);return true}
        if(key.startsWith("loan_")){val field=key.removePrefix("loan_");val m=checkField.matchEntire(field);if(m!=null)put(payload,"lenderChecks",m.groupValues[1],m.groupValues[2],value) else obj(payload,"loan",field,value);return true}
        return false
    }
    fun decode(payload: JSONObject, scope: String): Map<String,String> {
        val out=mutableMapOf<String,String>()
        fun fields(group: String,prefix: String){payload.optJSONObject(group)?.let { obj -> obj.keys().forEach { out[prefix+it]=obj.optString(it) } }}
        fun checks(group: String,prefix: String,ids: Map<String,String>?=null){payload.optJSONObject(group)?.let { section -> section.keys().forEach { id -> val native=ids?.entries?.firstOrNull { it.value==id }?.key ?: id; section.optJSONObject(id)?.let { row -> row.keys().forEach { f -> out["${prefix}_${native}_$f"]=if(f=="date") nativeDate(row.optString(f)) else row.optString(f) } } } }}
        fun choices(group: String,prefix: String,labels: Map<String,String>){val array=payload.optJSONArray(group) ?: JSONArray();val selected=(0 until array.length()).map { array.getString(it) };labels.forEach { (k,label)->out[prefix+k]=(label in selected).toString() }}
        if(scope=="local_profile"){fields("localProfile","");return out}
        if(scope=="local_reminders"){out["reminders_v1"]=JSONObject().put("version",1).put("pending",JSONObject().put("title","").put("date","").put("note","")).put("entries",payload.optJSONArray("reminders") ?: JSONArray()).toString();return out}
        if(scope=="resale_property"){
            fields("resaleBudget","budget_");payload.optJSONObject("resalePreferences")?.let { o->o.keys().forEach { out[if(it=="carpet") "carpet_area" else it]=o.optString(it) } }
            choices("resaleConfiguration","config_",ResaleFields.configuration.associate { it.first to it.first });choices("resaleLocation","location_",ResaleFields.location.associate { it.first to it.first });resaleGroups.forEach { (prefix,group)->checks(group,prefix) };out["legal_lawyer_status"]=payload.optString("resaleLawyerStatus");return out
        }
        payload.optJSONObject("budget")?.let { o->budget.forEach { (native,web)->if(o.has(web))out[native]=o.optString(web) } }
        choices("location","location_",location);choices("configuration","config_",config)
        checks("checks","developer",developer);checks("visitChecks","visit");checks("legalChecks","legal");checks("lenderChecks","loan");checks("costChecks","financial_prompt")
        fields("loan","loan_");fields("initialCosts","financial_initial_");fields("recurringCosts","financial_recurring_");fields("loanTerms","financial_comparison_")
        payload.optString("journey").takeIf { it=="New Property"||it=="Resale Property" }?.let { out["selected_journey"]=it }
        out["shortlist_projects_v1"]=(payload.optJSONArray("projects") ?: JSONArray()).toString();out["review_next_actions"]=payload.optString("nextActions")
        val feedback=payload.optJSONArray("feedbackNotes") ?: JSONArray();for(i in 0 until feedback.length()){val r=feedback.getJSONObject(i);r.put("date",nativeDate(r.optString("date")))};out["feedback_notes"]=feedback.toString()
        return out
    }
    fun recordedSteps(type: String,payload: JSONObject): JSONArray {
        fun has(v: Any?): Boolean = when(v){null,JSONObject.NULL->false;is String->v.isNotBlank();is JSONObject->v.keys().asSequence().any { has(v.opt(it)) };is JSONArray->v.length()>0;else->v==true}
        val steps=if(type=="resale") listOf("Requirements & location" to listOf("resaleBudget","resalePreferences","resaleConfiguration","resaleLocation"),"Property condition" to listOf("resaleCondition"),"Seller, ownership & title" to listOf("resaleSeller"),"Society documents & dues" to listOf("resaleSociety"),"Legal review" to listOf("resaleLegal"),"Agreement & handover" to listOf("resaleHandover"),"Journey summary" to listOf("resaleLawyerStatus")) else listOf("Requirements & budget" to listOf("budget","location","configuration"),"Developer checks" to listOf("checks"),"Shortlist projects" to listOf("projects"),"Visit observations" to listOf("visitChecks","feedbackNotes"),"Home loan guide" to listOf("loan","lenderChecks"),"Legal due diligence" to listOf("legalChecks"),"Financial due diligence" to listOf("initialCosts","recurringCosts","costChecks","loanTerms"),"Final review" to listOf("nextActions"))
        return JSONArray(steps.filter { (_,keys)->keys.any { has(payload.opt(it)) } }.map { it.first })
    }
}