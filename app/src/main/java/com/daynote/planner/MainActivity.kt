package com.daynote.planner

import android.app.Activity
import android.app.AlertDialog
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.os.Bundle
import android.graphics.Color
import android.graphics.Typeface
import android.view.Gravity
import android.widget.*
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Calendar
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

    private val categories = listOf(
        "Личное", "Работа", "Учёба", "Покупки", "Другое"
    )

    data class Task(
        val id: Long,
        var title: String,
        var description: String = "",
        var category: String = "Личное",
        var date: Long? = null,
        var hour: Int? = null,
        var minute: Int? = null,
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
            typeface = Typeface.create(
                "sans-serif-medium", Typeface.NORMAL
            )
            setPadding(0, 12, 0, 4)
        }

        val dateText = TextView(this).apply {
            text = SimpleDateFormat(
                "EEEE, d MMMM", Locale("ru")
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
        root.addView(dateText)
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
        var selectedDate: Long? = null
        var selectedHour: Int? = null
        var selectedMinute: Int? = null

        val container = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(24, 8, 24, 0)
        }

        val titleInput = EditText(this).apply {
            hint = "Название задачи"
            textSize = 16f
            setSingleLine(true)
        }

        val descriptionInput = EditText(this).apply {
            hint = "Описание (необязательно)"
            textSize = 15f
            minLines = 2
            gravity = Gravity.TOP
        }

        val categoryLabel = TextView(this).apply {
            text = "Категория"
            textSize = 14f
            setTextColor(muted)
            setPadding(0, 12, 0, 4)
        }

        val categorySpinner = Spinner(this).apply {
            adapter = ArrayAdapter(
                this@MainActivity,
                android.R.layout.simple_spinner_dropdown_item,
                categories
            )
        }

        val dateButton = Button(this).apply {
            text = "Выбрать дату"
        }

        val timeButton = Button(this).apply {
            text = "Выбрать время"
        }

        val clearDateButton = Button(this).apply {
            text = "Сбросить дату и время"
            textSize = 12f
        }

        dateButton.setOnClickListener {
            val calendar = Calendar.getInstance()

            if (selectedDate != null) {
                calendar.timeInMillis = selectedDate!!
            }

            DatePickerDialog(
                this,
                { _, year, month, day ->
                    val chosen = Calendar.getInstance()
                    chosen.set(year, month, day, 0, 0, 0)
                    chosen.set(Calendar.MILLISECOND, 0)
                    selectedDate = chosen.timeInMillis

                    dateButton.text = SimpleDateFormat(
                        "d MMMM yyyy", Locale("ru")
                    ).format(Date(selectedDate!!))
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
            ).show()
        }

        timeButton.setOnClickListener {
            val calendar = Calendar.getInstance()

            TimePickerDialog(
                this,
                { _, hour, minute ->
                    selectedHour = hour
                    selectedMinute = minute

                    timeButton.text = String.format(
                        Locale.getDefault(),
                        "%02d:%02d",
                        hour,
                        minute
                    )
                },
                calendar.get(Calendar.HOUR_OF_DAY),
                calendar.get(Calendar.MINUTE),
                true
            ).show()
        }

        clearDateButton.setOnClickListener {
            selectedDate = null
            selectedHour = null
            selectedMinute = null
            dateButton.text = "Выбрать дату"
            timeButton.text = "Выбрать время"
        }

        container.addView(titleInput)
        container.addView(descriptionInput)
        container.addView(categoryLabel)
        container.addView(categorySpinner)
        container.addView(dateButton)
        container.addView(timeButton)
        container.addView(clearDateButton)

        val dialog = AlertDialog.Builder(this)
            .setTitle("Новая задача")
            .setView(container)
            .setPositiveButton("Добавить", null)
            .setNegativeButton("Отмена", null)
            .create()

        dialog.setOnShowListener {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE)
                .setOnClickListener {
                    val title = titleInput.text.toString().trim()

                    if (title.isEmpty()) {
                        Toast.makeText(
                            this,
                            "Введи название задачи",
                            Toast.LENGTH_SHORT
                        ).show()
                        return@setOnClickListener
                    }

                    tasks.add(
                        Task(
                            id = System.currentTimeMillis(),
                            title = title,
                            description = descriptionInput.text.toString().trim(),
                            category = categorySpinner.selectedItem.toString(),
                            date = selectedDate,
                            hour = selectedHour,
                            minute = selectedMinute
                        )
                    )

                    saveTasks()
                    refreshTasks()
                    dialog.dismiss()
                }
        }

        dialog.show()
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
                orientation = LinearLayout.VERTICAL
                setPadding(14, 12, 14, 12)
                setBackgroundColor(Color.WHITE)
            }

            val row = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
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
                typeface = Typeface.DEFAULT_BOLD

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

            row.addView(check)
            row.addView(name)
            row.addView(delete)
            card.addView(row)

            val details = mutableListOf<String>()
            details.add(task.category)

            if (task.date != null) {
                details.add(
                    SimpleDateFormat(
                        "d MMM yyyy", Locale("ru")
                    ).format(Date(task.date!!))
                )
            }

            if (task.hour != null && task.minute != null) {
                details.add(
                    String.format(
                        Locale.getDefault(),
                        "%02d:%02d",
                        task.hour,
                        task.minute
                    )
                )
            }

            val info = TextView(this).apply {
                text = details.joinToString("  •  ")
                textSize = 12f
                setTextColor(muted)
                setPadding(42, 0, 0, 2)
            }

            card.addView(info)

            if (task.description.isNotEmpty()) {
                val description = TextView(this).apply {
                    text = task.description
                    textSize = 14f
                    setTextColor(muted)
                    setPadding(42, 6, 0, 0)
                }

                card.addView(description)
            }

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
            item.put("description", it.description)
            item.put("category", it.category)
            item.put("date", it.date ?: JSONObject.NULL)
            item.put("hour", it.hour ?: JSONObject.NULL)
            item.put("minute", it.minute ?: JSONObject.NULL)
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
                    id = item.optLong(
                        "id",
                        System.currentTimeMillis() + i
                    ),
                    title = item.optString("title", ""),
                    description = item.optString("description", ""),
                    category = item.optString("category", "Личное"),
                    date = if (
                        item.has("date") && !item.isNull("date")
                    ) item.optLong("date") else null,
                    hour = if (
                        item.has("hour") && !item.isNull("hour")
                    ) item.optInt("hour") else null,
                    minute = if (
                        item.has("minute") && !item.isNull("minute")
                    ) item.optInt("minute") else null,
                    done = item.optBoolean("done", false)
                )
            )
        }
    }
}
