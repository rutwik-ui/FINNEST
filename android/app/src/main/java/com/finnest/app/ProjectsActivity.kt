package com.finnest.app

import android.os.Bundle
import android.content.Intent
import android.text.InputFilter
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.finnest.app.databinding.ActivityProjectsBinding
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

private data class LocalProject(val id: String, val name: String)

/** Step 3 stores names as a single Room draft value, without inventing unseen project fields. */
class ProjectsActivity : AppCompatActivity() {
    private lateinit var binding: ActivityProjectsBinding
    private val journey = "new_property"
    private val key = "shortlist_projects_v1"
    private val projects = mutableListOf<LocalProject>()
    private val catalogue = mutableListOf<ListedProject>()
    private var loaded = false
    private fun dp(value: Int) = (value * resources.displayMetrics.density).toInt()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityProjectsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        FinnestUi.decorate(this, "new_property", "projects")
        binding.back.setOnClickListener { leaveWhenSaved() }
        binding.backBottom.setOnClickListener { leaveWhenSaved() }
        binding.saveLater.setOnClickListener { if (loaded) afterSaves { DashboardActivity.show(this) } else DashboardActivity.show(this) }
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() { leaveWhenSaved() }
        })
        binding.add.setOnClickListener { editName(null) }
        loadCatalogue()
        binding.next.setOnClickListener {
            afterSaves {
                startActivity(Intent(this, DeveloperChecksActivity::class.java).putExtra("visit_observations", true))
            }
        }
        DraftRepository.load(this, journey) { result ->
            runOnUiThread {
                if (isFinishing || isDestroyed) return@runOnUiThread
                result.fold(onSuccess = { draft ->
                    val decoded = runCatching {
                        val array = JSONArray(draft[key] ?: "[]")
                        (0 until array.length()).map { i ->
                            val item = array.getJSONObject(i)
                            LocalProject(item.getString("id"), item.getString("name"))
                        }
                    }
                    decoded.fold(onSuccess = {
                        projects.addAll(it); loaded = true
                        binding.loading.visibility = android.view.View.GONE
                        binding.next.isEnabled = true; render()
                    }, onFailure = { binding.loading.text = "Could not read your shortlist. Your saved data has not been changed." })
                }, onFailure = { binding.loading.text = "Could not load your shortlist. Please return and try again." })
            }
        }
    }

    /** Admin-published projects. Failure is silent: the screen still works offline. */
    private fun loadCatalogue() {
        Thread {
            val listed = runCatching { PublicProjectClient().load() }.getOrDefault(emptyList())
            runOnUiThread {
                if (isFinishing || isDestroyed) return@runOnUiThread
                catalogue.clear(); catalogue.addAll(listed); render()
            }
        }.start()
    }

    private fun addFromCatalogue(project: ListedProject) {
        if (projects.size >= 5) return
        if (projects.any { it.name.equals(project.name, ignoreCase = true) }) return
        projects.add(LocalProject(UUID.randomUUID().toString(), project.name))
        persist()
    }

    private fun persist() {
        val array = JSONArray()
        projects.forEach { array.put(JSONObject().put("id", it.id).put("name", it.name)) }
        DraftRepository.save(this, journey, key, array.toString())
        render()
    }
    private fun afterSaves(action: () -> Unit) {
        DraftRepository.afterPending(journey) { saved ->
            runOnUiThread {
                if (isFinishing || isDestroyed) return@runOnUiThread
                if (saved) action() else AlertDialog.Builder(this).setTitle("Could not save your shortlist")
                    .setMessage("Please check your device storage. Retry saving before leaving this screen.")
                    .setPositiveButton("Retry") { _, _ ->
                        DraftRepository.retryFailed(this, journey) { runOnUiThread { afterSaves(action) } }
                    }.setNegativeButton("Keep editing", null).show()
            }
        }
    }
    private fun leaveWhenSaved() { if (!loaded) finish() else afterSaves { finish() } }

    private fun editName(project: LocalProject?) {
        if (project == null && projects.size >= 5) return
        val input = EditText(this).apply {
            hint = "Project name"; setSingleLine(true); filters = arrayOf(InputFilter.LengthFilter(100))
            setText(project?.name.orEmpty()); setSelection(text.length)
        }
        val dialog = AlertDialog.Builder(this).setTitle(if (project == null) "Add project" else "Rename project")
            .setView(input).setNegativeButton("Cancel", null).setPositiveButton("Save", null).create()
        dialog.setOnShowListener {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                val name = input.text.toString().trim()
                if (name.isEmpty()) { input.error = "Enter a project name"; return@setOnClickListener }
                if (project == null) {
                    if (projects.size >= 5) { dialog.dismiss(); return@setOnClickListener }
                    projects.add(LocalProject(UUID.randomUUID().toString(), name))
                } else {
                    val index = projects.indexOfFirst { it.id == project.id }
                    if (index >= 0) projects[index] = project.copy(name = name)
                }
                persist(); dialog.dismiss()
            }
        }
        dialog.show()
    }
    private fun render() {
        binding.count.text = "${projects.size} / 5 projects · synced with your account when signed in"
        binding.add.isEnabled = loaded && projects.size < 5
        binding.list.removeAllViews()
        if (projects.isEmpty()) {
            binding.list.addView(TextView(this).apply {
                text = "No projects added yet\n\nAdd a project to compare what you know alongside what is still unknown."
                setTextColor(getColor(R.color.navy)); textSize = 16f; setPadding(dp(20), dp(28), dp(20), dp(28)); setBackgroundResource(R.drawable.card)
            }, LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(12) })
        }
        projects.forEach { project ->
            val card = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(16), dp(16), dp(16), dp(12)); setBackgroundResource(R.drawable.card) }
            card.addView(TextView(this).apply { text = project.name; textSize = 18f; setTextColor(getColor(R.color.navy)); setTypeface(null, android.graphics.Typeface.BOLD) })
            card.addView(TextView(this).apply { text = "Your shortlist entry"; textSize = 14f; setTextColor(getColor(R.color.muted)); setPadding(0, dp(6), 0, dp(8)) })
            val actions = LinearLayout(this)
            val rename = android.widget.Button(this).apply { text = "Rename"; isAllCaps = false; setOnClickListener { editName(project) } }
            val remove = android.widget.Button(this).apply {
                text = "Remove"; isAllCaps = false
                setOnClickListener {
                    AlertDialog.Builder(this@ProjectsActivity).setTitle("Remove project?")
                        .setMessage("Remove ${project.name} from this shortlist?")
                        .setNegativeButton("Cancel", null).setPositiveButton("Remove") { _, _ ->
                            projects.removeAll { it.id == project.id }; persist()
                        }.show()
                }
            }
            actions.addView(rename); actions.addView(remove); card.addView(actions)
            binding.list.addView(card, LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(12) })
        }
        if (projects.size >= 5) binding.count.text = "${projects.size} / 5 projects · remove one to add another"

        if (catalogue.isNotEmpty()) {
            binding.list.addView(TextView(this).apply {
                text = "Listed projects"; textSize = 18f; setTextColor(getColor(R.color.navy))
                setTypeface(null, android.graphics.Typeface.BOLD); setPadding(0, dp(24), 0, dp(4))
            }, LinearLayout.LayoutParams(-1, -2))
            binding.list.addView(TextView(this).apply {
                text = "Added by the FINNEST team. Verify RERA registration and approvals yourself before paying anything."
                textSize = 13f; setTextColor(getColor(R.color.muted)); setPadding(0, 0, 0, dp(4))
            }, LinearLayout.LayoutParams(-1, -2))
            catalogue.forEach { listed ->
                val card = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(16), dp(14), dp(16), dp(12)); setBackgroundResource(R.drawable.card) }
                card.addView(TextView(this).apply { text = listed.name; textSize = 17f; setTextColor(getColor(R.color.navy)); setTypeface(null, android.graphics.Typeface.BOLD) })
                val line = listOfNotNull(listed.developer, listed.locality, listed.city).joinToString(" · ")
                if (line.isNotBlank()) card.addView(TextView(this).apply { text = line; textSize = 13f; setTextColor(getColor(R.color.muted)); setPadding(0, dp(4), 0, 0) })
                val detail = listOfNotNull(listed.configuration, listed.priceFrom, listed.possession).joinToString(" · ")
                if (detail.isNotBlank()) card.addView(TextView(this).apply { text = detail; textSize = 13f; setTextColor(getColor(R.color.muted)); setPadding(0, dp(2), 0, 0) })
                listed.approvalNote?.let { note -> card.addView(TextView(this).apply { text = note; textSize = 12f; setTextColor(getColor(R.color.muted)); setPadding(0, dp(2), 0, 0) }) }
                listed.summary?.let { summary -> card.addView(TextView(this).apply { text = summary; textSize = 13f; setTextColor(getColor(R.color.navy)); setPadding(0, dp(6), 0, 0) }) }
                val already = projects.any { it.name.equals(listed.name, ignoreCase = true) }
                val addButton = android.widget.Button(this).apply {
                    text = if (already) "In your shortlist" else "Add to shortlist"
                    isAllCaps = false
                    isEnabled = !already && projects.size < 5
                    setOnClickListener { addFromCatalogue(listed) }
                }
                card.addView(addButton)
                binding.list.addView(card, LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(12) })
            }
        }
    }
}
