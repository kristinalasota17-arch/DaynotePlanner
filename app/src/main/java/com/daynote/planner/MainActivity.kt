package com.daynote.planner

import android.app.Activity
import android.app.AlertDialog
import android.os.Bundle
import android.graphics.Color
import android.graphics.Typeface
import android.view.Gravity
import android.widget.*
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : Activity() {

    private val tasks = mutableListOf<Task>()
    private lateinit var taskList: LinearLayout
    private lateinit var countText: TextView
    private lateinit var prefs: android.content.SharedPreferences

    private val purple = Color.rgb(129, 117, 216)
    private val dark = Color.rgb(39, 36, 55)
    private val muted = Color.rgb(135, 132, 150)
    private val pageBackground = Color.rgb(247, 246, 252)

    data class Task(
        val id: Long,
        var title: String,
        var done: Boolean = false
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        prefs = getSharedPreferences("daynote", MODE_PRIVATE)
        loadTasks()
        showHome()
    }

    private fun showHome() {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(24, 35, 24, 20)
            setBackgroundColor(pageBackground)
        }

        val greeting = TextView(this).apply {
            text = "DAYNOTE"
            textSize = 13f
            letterSpacing = 0.18f
            setTextColor(purple)
            typeface = Typeface.DEFAULT_BOLD
        }

        val heading = TextView(this).apply {
            text = "Мой день"
            textSize = 32f
            setTextColor(dark)
            typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
            setPadding(0, 12, 0, 4)
        }

        val date = TextView(this).apply {
            text = SimpleDateFormat(
                "EEEE, d MMMM",
                Locale("ru")
            ).format(Date())
            textSize = 15f
            setTextColor(muted)
        }

        countText = TextView(this).apply {
            textSize = 14f
            setTextColor(muted)
            setPadding(0, 25, 0, 12)
        }

        val addButton = Button(this).apply {
            text = "+  Новая задача"
            textSize = 15f
            setTextColor(Color.WHITE)
            backgroundTintList =
                android.content.res.ColorStateList.valueOf(purple)
            setOnClickListener { addTaskDialog() }
        }

        taskList = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
        }

        val scroll = ScrollView(this).apply {
            addView(taskList)
        }

        root.addView(greeting)
        root.addView(heading)
        root.addView(date)
        root.addView(countText)
        root.addView(addButton)

        root.addView(
            scroll,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                0,
                1f
            )
        )

        setContentView(root)
        refreshTasks()
    }

    private fun addTaskDialog() {
        val input = EditText(this).apply {
            hint = "Например, купить продукты"
            textSize = 16f
            setSingleLine(true)
        }

        val container = LinearLayout(this).apply {
            setPadding(24, 8, 24, 0)
            addView(input)
        }

        AlertDialog.Builder(this)
            .setTitle("Новая задача")
            .setView(container)
            .setPositiveButton("Добавить") { _, _ ->
                val name = input.text.toString().trim()
                if (name.isNotEmpty()) {
                    tasks.add(Task(System.currentTimeMillis(), name))
                    saveTasks()
                    refreshTasks()
                }
            }
            .setNegativeButton("Отмена", null)
            .show()
    }

    private fun refreshTasks() {
        taskList.removeAllViews()

        val completed = tasks.count { it.done }
        countText.text = "Выполнено: $completed из ${tasks.size}"

        if (tasks.isEmpty()) {
            val empty = TextView(this).apply {
                text = "Пока здесь пусто.\nДобавь свою первую задачу."
                textSize = 16f
                gravity = Gravity.CENTER
                setTextColor(muted)
                setPadding(12, 65, 12, 65)
            }
            taskList.addView(empty)
        }

        tasks.forEach { task ->
            val card = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                setPadding(12, 10, 12, 10)
                setBackgroundColor(Color.WHITE)
            }

            val check = CheckBox(this).apply {
                isChecked = task.done
                buttonTintList =
                    android.content.res.ColorStateList.valueOf(purple)
                setOnCheckedChangeListener { _, checked ->
                    task.done = checked
                    saveTasks()
                    refreshTasks()
                }
            }

            val name = TextView(this).apply {
                text = task.title
                textSize = 16f
                setTextColor(dark)
                if (task.done) {
                    paintFlags = paintFlags or
                        android.graphics.Paint.STRIKE_THRU_TEXT_FLAG
                    alpha = 0.5f
                }
                layoutParams = LinearLayout.LayoutParams(0, -2, 1f)
            }

            val delete = TextView(this).apply {
                text = "×"
                textSize = 25f
                setTextColor(muted)
                setPadding(12, 0, 8, 0)
                setOnClickListener {
                    tasks.remove(task)
                    saveTasks()
                    refreshTasks()
                }
            }

            card.addView(check)
            card.addView(name)
            card.addView(delete)

            val params = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            params.setMargins(0, 6, 0, 6)

            taskList.addView(card, params)
        }
    }

    private fun saveTasks() {
        val array = JSONArray()

        tasks.forEach {
            val item = JSONObject()
            item.put("id", it.id)
            item.put("title", it.title)
            item.put("done", it.done)
            array.put(item)
        }

        prefs.edit().putString("tasks", array.toString()).apply()
    }

    private fun loadTasks() {
        val data = prefs.getString("tasks", "[]") ?: "[]"
        val array = JSONArray(data)

        for (i in 0 until array.length()) {
            val item = array.getJSONObject(i)
            tasks.add(
                Task(
                    item.getLong("id"),
                    item.getString("title"),
                    item.getBoolean("done")
                )
            )
        }
    }
}
