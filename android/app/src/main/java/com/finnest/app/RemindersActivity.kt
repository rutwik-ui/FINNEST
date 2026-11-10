package com.finnest.app

import android.app.DatePickerDialog
import android.os.Bundle
import android.text.InputFilter
import android.text.InputType
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.doAfterTextChanged
import com.finnest.app.databinding.ActivityDeveloperChecksBinding
import org.json.JSONArray
import org.json.JSONObject
import java.util.Calendar
import java.util.UUID

/** Local dated notes, not operating-system notifications. */
class RemindersActivity : AppCompatActivity() {
    private lateinit var binding: ActivityDeveloperChecksBinding
    private val scope = "local_reminders"
    private val key = "reminders_v1"
    private var loaded = false
    private var suppress = false
    private lateinit var state: JSONObject
    private lateinit var entries: JSONArray
    private lateinit var titleInput: EditText
    private lateinit var noteInput: EditText
    private lateinit var dateButton: Button
    private lateinit var list: LinearLayout
    private var selectedDate = ""
    private fun dp(n: Int) = (n * resources.displayMetrics.density).toInt()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDeveloperChecksBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.stepLabel.text = "A QUIETER WAY TO KEEP TRACK"
        binding.screenTitle.text = "Reminders"
        binding.screenDescription.text = "Keep dated follow-up notes on this device."
        binding.screenNotice.text = "These are in-app notes only. No alerts or operating-system notifications are scheduled."
        binding.saveLater.text = "Home"
        binding.next.text = "Back to dashboard"
        binding.backBottom.text = "Back"
        binding.back.setOnClickListener { leave { finish() } }
        binding.backBottom.setOnClickListener { leave { finish() } }
        binding.saveLater.setOnClickListener { leave { DashboardActivity.show(this) } }
        binding.next.setOnClickListener { leave { DashboardActivity.show(this) } }
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) { override fun handleOnBackPressed() { leave { finish() } } })
        DraftRepository.load(this, scope) { result -> runOnUiThread {
            if (isFinishing || isDestroyed) return@runOnUiThread
            result.fold(onSuccess = { draft ->
                val decoded = runCatching {
                    val obj = draft[key]?.let { JSONObject(it) } ?: JSONObject().put("version", 1).put("pending", JSONObject().put("title", "").put("date", "").put("note", "")).put("entries", JSONArray())
                    require(obj.getInt("version") == 1)
                    val pending = obj.getJSONObject("pending")
                    listOf("title", "date", "note").forEach { require(pending.get(it) is String) }
                    val array = obj.getJSONArray("entries")
                    for (i in 0 until array.length()) {
                        val entry = array.getJSONObject(i)
                        listOf("id", "title", "date", "note").forEach { require(entry.get(it) is String) }
                        require(entry.getString("id").isNotBlank() && entry.getString("title").isNotBlank() && ReminderDate.valid(entry.getString("date")))
                    }
                    require(pending.getString("date").isEmpty() || ReminderDate.valid(pending.getString("date")))
                    obj
                }
                decoded.fold(onSuccess = { state = it; entries = it.getJSONArray("entries"); buildForm(); loaded = true; binding.loading.visibility = View.GONE; binding.next.isEnabled = true },
                    onFailure = { binding.loading.text = "Could not read saved reminders. They have not been changed. Return home and try again." })
            }, onFailure = { binding.loading.text = "Could not load reminders. Return home and try again." })
        } }
    }
    private fun persist() { DraftRepository.save(this, scope, key, state.toString()) }
    private fun leave(action: () -> Unit) {
        if (!loaded) { action(); return }
        DraftRepository.afterPending(scope) { saved -> runOnUiThread {
            if (isFinishing || isDestroyed) return@runOnUiThread
            if (saved) action() else AlertDialog.Builder(this).setTitle("Could not save reminders")
                .setMessage("Check device storage and retry before leaving.")
                .setPositiveButton("Retry") { _, _ -> DraftRepository.retryFailed(this, scope) { runOnUiThread { leave(action) } } }
                .setNegativeButton("Keep editing", null).show()
        } }
    }
    private fun label(parent: LinearLayout, value: String, bold: Boolean = false): TextView = TextView(this).apply {
        text = value; textSize = if (bold) 17f else 14f; setTextColor(getColor(R.color.navy)); setPadding(0, dp(10), 0, dp(4))
        if (bold) setTypeface(null, android.graphics.Typeface.BOLD); parent.addView(this)
    }
    private fun card(): LinearLayout = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL; setPadding(dp(16), dp(8), dp(16), dp(16)); setBackgroundResource(R.drawable.card)
        binding.form.addView(this, LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(14) })
    }
    private fun buildForm() {
        val pending = state.getJSONObject("pending")
        val form = card()
        label(form, "Reminder")
        titleInput = EditText(this).apply {
            hint = "e.g. Ask for updated cost sheet"; contentDescription = "Reminder title"; textSize = 16f
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_CAP_SENTENCES
            filters = arrayOf(InputFilter.LengthFilter(200)); setText(pending.getString("title")); form.addView(this, LinearLayout.LayoutParams(-1, -2))
            doAfterTextChanged { if (!suppress) { pending.put("title", it?.toString().orEmpty()); persist() } }
        }
        label(form, "Date")
        selectedDate = pending.getString("date")
        dateButton = Button(this).apply {
            isAllCaps = false; contentDescription = "Reminder date"
            setOnClickListener {
                val cal = Calendar.getInstance()
                if (ReminderDate.valid(selectedDate)) { val parts = selectedDate.split("-").map { it.toInt() }; cal.set(parts[0], parts[1] - 1, parts[2]) }
                DatePickerDialog(this@RemindersActivity, { _, year, month, day ->
                    val value = ReminderDate.fromParts(year, month + 1, day)
                    if (!ReminderDate.valid(value)) {
                        AlertDialog.Builder(this@RemindersActivity).setMessage("Choose a date from 1900 to 2100.").setPositiveButton("OK", null).show()
                    } else { selectedDate = value; pending.put("date", value); updateDate(); persist() }
                }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)).show()
            }
        }
        updateDate(); form.addView(dateButton, LinearLayout.LayoutParams(-1, -2))
        form.addView(Button(this).apply { text = "Clear reminder date"; isAllCaps = false; setOnClickListener { selectedDate = ""; pending.put("date", ""); updateDate(); persist() } })
        label(form, "Note (optional)")
        noteInput = EditText(this).apply {
            hint = "Context for yourself"; contentDescription = "Reminder note"; textSize = 16f
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_CAP_SENTENCES or InputType.TYPE_TEXT_FLAG_MULTI_LINE
            filters = arrayOf(InputFilter.LengthFilter(1000)); setText(pending.getString("note")); form.addView(this, LinearLayout.LayoutParams(-1, -2))
            doAfterTextChanged { if (!suppress) { pending.put("note", it?.toString().orEmpty()); persist() } }
        }
        form.addView(Button(this).apply {
            text = "Add reminder"; isAllCaps = false
            setOnClickListener {
                val title = titleInput.text.toString().trim()
                if (title.isBlank() || !ReminderDate.valid(selectedDate)) {
                    AlertDialog.Builder(this@RemindersActivity).setMessage("Enter a reminder and choose a date before adding it.").setPositiveButton("OK", null).show()
                } else {
                    entries.put(JSONObject().put("id", UUID.randomUUID().toString()).put("title", title).put("date", selectedDate).put("note", noteInput.text.toString().trim()))
                    // Save the new list and clear the unfinished form in one Room field update.
                    pending.put("title", "").put("date", "").put("note", "")
                    suppress = true; titleInput.setText(""); noteInput.setText(""); suppress = false; selectedDate = ""; updateDate(); persist(); render()
                }
            }
        }, LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(12) })
        list = card(); render()
        val notice = card(); label(notice, "Notification preferences", true)
        label(notice, "System notifications and notification preferences are not available yet. Revisit this screen to see your dates.")
    }
    private fun updateDate() { dateButton.text = if (selectedDate.isEmpty()) "Choose date (dd/mm/yyyy)" else ReminderDate.display(selectedDate) }
    private fun render() {
        list.removeAllViews()
        label(list, "Your reminders", true)
        if (entries.length() == 0) { label(list, "No dated notes yet. Add one above when you want to revisit something."); return }
        val sorted = (0 until entries.length()).map { entries.getJSONObject(it) }.sortedBy { it.getString("date") }
        sorted.forEach { entry ->
            val title = entry.getString("title")
            label(list, title, true); label(list, "Date: ${ReminderDate.display(entry.getString("date"))}")
            val note = entry.getString("note"); if (note.isNotBlank()) label(list, note)
            list.addView(Button(this).apply {
                text = "Delete reminder"; isAllCaps = false; contentDescription = "Delete reminder: $title"
                setOnClickListener { AlertDialog.Builder(this@RemindersActivity).setTitle("Delete this reminder?")
                    .setMessage("This removes the note from this device.").setNegativeButton("Cancel", null)
                    .setPositiveButton("Delete") { _, _ ->
                        val id = entry.getString("id")
                        val index = (0 until entries.length()).firstOrNull { entries.getJSONObject(it).getString("id") == id }
                        if (index != null) { entries.remove(index); persist(); render() }
                    }.show() }
            })
        }
    }
}
