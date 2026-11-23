package com.daynote.planner

import android.app.Activity
import android.app.AlertDialog
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.os.Bundle
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.View
import android.widget.*
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class MainActivity : Activity() {

    private val tasks = mutableListOf<Task>()

    private lateinit var content: LinearLayout
    private lateinit var countText: TextView
    private lateinit var prefs: android.content.SharedPreferences

    private val purple = Color.rgb(129, 117, 216)
    private val dark = Color.rgb(39, 36, 55)
    private val muted = Color.rgb(145, 142, 158)
    private val pageBackground = Color.rgb(247, 246, 252)
    private val white = Color.WHITE

    private var currentScreen = "Задачи"

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
        showMain()
    }

    private fun rounded(color: Int, radius: Float): GradientDrawable {
        return GradientDrawable().apply {
            setColor(color)
            cornerRadius = radius
        }
    }

    private fun makeText(
        value: String,
        size: Float,
        color: Int,
        bold: Boolean = false
    ): TextView {
        return TextView(this).apply {
            text = value
            textSize = size
            setTextColor(color)
            typeface = if (bold) {
                Typeface.create("sans-serif-medium", Typeface.NORMAL)
            } else {
                Typeface.create("sans-serif", Typeface.NORMAL)
            }
        }
    }

    private fun showMain() {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(pageBackground)
            setPadding(20, 35, 20, 12)
        }

        val header = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
        }

        val logo = makeText("DAYNOTE", 12f, purple, true)
        logo.letterSpacing = 0.2f

        val heading = makeText(currentScreen, 30f, dark, true)
        heading.setPadding(0, 10, 0, 3)

        val date = makeText(
            SimpleDateFormat(
                "EEEE, d MMMM",
                Locale("ru")
            ).format(Date()),
            14f,
            muted
        )

        header.addView(logo)
        header.addView(heading)
        header.addView(date)

        content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
        }

        val scroll = ScrollView(this).apply {
            isFillViewport = true
            addView(content)
        }

        val bottomBar = createBottomBar()

        root.addView(header)

        root.addView(
            scroll,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                0,
                1f
            ).apply {
                topMargin = 20
            }
        )

        root.addView(bottomBar)

        setContentView(root)
        showScreen()
    }

    private fun createBottomBar(): LinearLayout {
        val bar = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            background = rounded(white, 35f)
            setPadding(5, 8, 5, 8)
            elevation = 5f
        }

        val items = listOf(
            "Задачи" to "☷",
            "Дневник" to "▤",
            "Трекеры" to "◷",
            "+" to "+"
        )

        items.forEach { item ->
            val selected = currentScreen == item.first

            val button = TextView(this).apply {
                text = "${item.second}\n${item.first}"
                textSize = if (item.first == "+") 21f else 11f
                gravity = Gravity.CENTER
                setTextColor(
                    if (selected) purple else muted
                )
                setPadding(5, 5, 5, 5)
                typeface = Typeface.DEFAULT_BOLD

                setOnClickListener {
                    if (item.first == "+") {
                        showMoreMenu()
                    } else {
                        currentScreen = item.first
                        showMain()
                    }
                }
            }

            bar.addView(
                button,
                LinearLayout.LayoutParams(0, 55, 1f)
            )
        }

        return bar
    }

    private fun showScreen() {
        content.removeAllViews()

        when (currentScreen) {
            "Задачи" -> showTasks()
            "Дневник" -> showDiary()
            "Трекеры" -> showTrackers()
            "Календарь" -> showPlaceholder(
                "Календарь",
                "Здесь будет календарь твоих событий и задач."
            )
            "Заметки" -> showPlaceholder(
                "Заметки",
                "Здесь будут храниться твои заметки."
            )
            "Цели" -> showPlaceholder(
                "Цели",
                "Здесь можно будет планировать свои цели."
            )
            "Настройки" -> showPlaceholder(
                "Настройки",
                "Здесь появятся настройки приложения и оформления."
            )
        }
    }

    private fun showTasks() {
        val completed = tasks.count { it.done }

        countText = makeText(
            "Выполнено: $completed из ${tasks.size}",
            14f,
            muted
        )

        val addButton = TextView(this).apply {
            text = "+  Новая задача"
            textSize = 15f
            gravity = Gravity.CENTER
            setTextColor(white)
            typeface = Typeface.DEFAULT_BOLD
            background = rounded(purple, 22f)
            setPadding(15, 16, 15, 16)
            setOnClickListener { addTaskDialog() }
        }

        content.addView(countText)
        content.addView(
            addButton,
            LinearLayout.LayoutParams(
                -1,
                -2
            ).apply {
                topMargin = 15
                bottomMargin = 15
            }
        )

        if (tasks.isEmpty()) {
            val empty = makeText(
                "Пока здесь пусто.\nДобавь свою первую задачу.",
                15f,
                muted
            ).apply {
                gravity = Gravity.CENTER
                setPadding(10, 60, 10, 60)
            }

            content.addView(empty)
        }

        tasks.forEach { task ->
            val card = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                background = rounded(white, 22f)
                setPadding(14, 12, 14, 12)
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
                    showScreen()
                }
            }

            val name = makeText(
                task.title,
                16f,
                dark,
                true
            ).apply {
                if (task.done) {
                    paintFlags = paintFlags or
                        android.graphics.Paint.STRIKE_THRU_TEXT_FLAG
                    alpha = 0.5f
                }
                layoutParams = LinearLayout.LayoutParams(0, -2, 1f)
            }

            val delete = makeText("×", 25f, muted).apply {
                setPadding(12, 0, 8, 0)
                setOnClickListener {
                    tasks.remove(task)
                    saveTasks()
                    showScreen()
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
                        "d MMM yyyy",
                        Locale("ru")
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

            card.addView(
                makeText(
                    details.joinToString("  •  "),
                    12f,
                    muted
                ).apply {
                    setPadding(42, 0, 0, 0)
                }
            )

            if (task.description.isNotEmpty()) {
                card.addView(
                    makeText(
                        task.description,
                        14f,
                        muted
                    ).apply {
                        setPadding(42, 6, 0, 0)
                    }
                )
            }

            content.addView(
                card,
                LinearLayout.LayoutParams(-1, -2).apply {
                    bottomMargin = 10
                }
            )
        }
    }

    private fun showDiary() {
        showSectionCard(
            "Мой дневник",
            "Здесь будет твой личный дневник."
        )
    }

    private fun showTrackers() {
        showSectionCard(
            "Мои трекеры",
            "Здесь можно будет отслеживать привычки и прогресс."
        )
    }

    private fun showPlaceholder(title: String, description: String) {
        showSectionCard(title, description)
    }

    private fun showSectionCard(title: String, description: String) {
        val card = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = rounded(white, 24f)
            setPadding(20, 22, 20, 22)
        }

        card.addView(makeText(title, 20f, dark, true))

        card.addView(
            makeText(description, 14f, muted).apply {
                setPadding(0, 10, 0, 0)
            }
        )

        content.addView(card)
    }

    private fun showMoreMenu() {
        val options = arrayOf(
            "Календарь",
            "Заметки",
            "Цели",
            "Настройки"
        )

        AlertDialog.Builder(this)
            .setTitle("Другие разделы")
            .setItems(options) { _, which ->
                currentScreen = options[which]
                showMain()
            }
            .show()
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
            hint = "Описание"
            textSize = 15f
            minLines = 2
            gravity = Gravity.TOP
        }

        val categoryLabel = makeText(
            "Категория",
            14f,
            muted
        )

        val spinner = Spinner(this).apply {
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

        dateButton.setOnClickListener {
            val calendar = Calendar.getInstance()

            DatePickerDialog(
                this,
                { _, year, month, day ->
                    val chosen = Calendar.getInstance()
                    chosen.set(year, month, day, 0, 0, 0)
                    chosen.set(Calendar.MILLISECOND, 0)
                    selectedDate = chosen.timeInMillis

                    dateButton.text = SimpleDateFormat(
                        "d MMMM yyyy",
                        Locale("ru")
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

        container.addView(titleInput)
        container.addView(descriptionInput)
        container.addView(categoryLabel)
        container.addView(spinner)
        container.addView(dateButton)
        container.addView(timeButton)

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
                            category = spinner.selectedItem.toString(),
                            date = selectedDate,
                            hour = selectedHour,
                            minute = selectedMinute
                        )
                    )

                    saveTasks()
                    showScreen()
                    dialog.dismiss()
                }
        }

        dialog.show()
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
