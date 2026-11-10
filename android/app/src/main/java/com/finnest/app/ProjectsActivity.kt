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
    private var loaded = false
    private fun dp(value: Int) = (value * resources.displayMetrics.density).toInt()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityProjectsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.back.setOnClickListener { leaveWhenSaved() }
        binding.backBottom.setOnClickListener { leaveWhenSaved() }
        binding.saveLater.setOnClickListener { if (loaded) afterSaves { DashboardActivity.show(this) } else DashboardActivity.show(this) }
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() { leaveWhenSaved() }
        })
        binding.add.setOnClickListener { editName(null) }
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
        binding.count.text = "${projects.size} / 5 projects · local shortlist"
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
            card.addView(TextView(this).apply { text = "Your entry · details not yet added"; textSize = 14f; setTextColor(getColor(R.color.muted)); setPadding(0, dp(6), 0, dp(8)) })
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
    }
}
