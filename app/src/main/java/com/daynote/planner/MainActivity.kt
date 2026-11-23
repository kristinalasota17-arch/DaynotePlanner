package com.daynote.planner

import android.app.Activity
import android.app.AlertDialog
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.app.Dialog
import android.os.Bundle
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.View
import android.view.WindowManager
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
    private val lightPurple = Color.rgb(239, 236, 253)
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

    private fun dp(value: Int): Int {
        return (value * resources.displayMetrics.density).toInt()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        prefs = getSharedPreferences("daynote", MODE_PRIVATE)

        loadTasks()
        showMain()
    }

    private fun rounded(
        color: Int,
        radius: Float
    ): GradientDrawable {
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
                Typeface.create(
                    "sans-serif-medium",
                    Typeface.NORMAL
                )
            } else {
                Typeface.create(
                    "sans-serif",
                    Typeface.NORMAL
                )
            }
        }
    }

    private fun showMain() {

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(pageBackground)
            setPadding(
                dp(20),
                dp(35),
                dp(20),
                dp(12)
            )
        }

        val header = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
        }

        val logo = makeText(
            "DAYNOTE",
            12f,
            purple,
            true
        )

        logo.letterSpacing = 0.2f

        val heading = makeText(
            currentScreen,
            30f,
            dark,
            true
        )

        heading.setPadding(0, dp(10), 0, dp(3))

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
                topMargin = dp(20)
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
            background = rounded(
                white,
                dp(25).toFloat()
            )

            setPadding(
                dp(5),
                dp(7),
                dp(5),
                dp(7)
            )

            elevation = dp(5).toFloat()
        }

        val items = listOf(
            "Задачи" to "☷",
            "Дневник" to "▤",
            "Трекеры" to "◷",
            "+" to "+"
        )

        items.forEach { item ->

            val selected = currentScreen == item.first

            val button = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                gravity = Gravity.CENTER

                background = if (selected) {
                    rounded(
                        lightPurple,
                        dp(17).toFloat()
                    )
                } else {
                    rounded(
                        Color.TRANSPARENT,
                        dp(17).toFloat()
                    )
                }

                setPadding(
                    dp(5),
                    dp(5),
                    dp(5),
                    dp(5)
                )

                setOnClickListener {

                    if (item.first == "+") {
                        showMoreMenu()
                    } else {
                        currentScreen = item.first
                        showMain()
                    }
                }
            }

            val icon = makeText(
                item.second,
                if (item.first == "+") 27f else 24f,
                if (selected) purple else muted,
                true
            )

            icon.gravity = Gravity.CENTER

            val label = makeText(
                item.first,
                11f,
                if (selected) purple else muted,
                selected
            )

            label.gravity = Gravity.CENTER

            button.addView(icon)
            button.addView(label)

            bar.addView(
                button,
                LinearLayout.LayoutParams(
                    0,
                    dp(65),
                    1f
                )
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
                "Здесь появятся настройки приложения."
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

            background = rounded(
                purple,
                dp(18).toFloat()
            )

            setPadding(
                dp(15),
                dp(16),
                dp(15),
                dp(16)
            )

            setOnClickListener {
                addTaskDialog()
            }
        }

        content.addView(countText)

        content.addView(
            addButton,
            LinearLayout.LayoutParams(
                -1,
                -2
            ).apply {
                topMargin = dp(15)
                bottomMargin = dp(15)
            }
        )

        if (tasks.isEmpty()) {

            val empty = makeText(
                "Пока здесь пусто.\nДобавь свою первую задачу.",
                15f,
                muted
            ).apply {
                gravity = Gravity.CENTER
                setPadding(
                    dp(10),
                    dp(60),
                    dp(10),
                    dp(60)
                )
            }

            content.addView(empty)
        }

        tasks.forEach { task ->

            val card = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL

                background = rounded(
                    white,
                    dp(20).toFloat()
                )

                setPadding(
                    dp(14),
                    dp(12),
                    dp(14),
                    dp(12)
                )
            }

            val row = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
            }

            val check = CheckBox(this).apply {
                isChecked = task.done

                buttonTintList =
                    android.content.res.ColorStateList.valueOf(
                        purple
                    )

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

                layoutParams = LinearLayout.LayoutParams(
                    0,
                    -2,
                    1f
                )
            }

            val delete = makeText(
                "×",
                25f,
                muted
            ).apply {

                setPadding(
                    dp(12),
                    0,
                    dp(8),
                    0
                )

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

            if (
                task.hour != null &&
                task.minute != null
            ) {

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
                    setPadding(
                        dp(42),
                        0,
                        0,
                        0
                    )
                }
            )

            if (task.description.isNotEmpty()) {

                card.addView(
                    makeText(
                        task.description,
                        14f,
                        muted
                    ).apply {
                        setPadding(
                            dp(42),
                            dp(6),
                            0,
                            0
                        )
                    }
                )
            }

            content.addView(
                card,
                LinearLayout.LayoutParams(
                    -1,
                    -2
                ).apply {
                    bottomMargin = dp(10)
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

    private fun showPlaceholder(
        title: String,
        description: String
    ) {
        showSectionCard(title, description)
    }

    private fun showSectionCard(
        title: String,
        description: String
    ) {

        val card = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL

            background = rounded(
                white,
                dp(22).toFloat()
            )

            setPadding(
                dp(20),
                dp(22),
                dp(20),
                dp(22)
            )
        }

        card.addView(
            makeText(
                title,
                20f,
                dark,
                true
            )
        )

        card.addView(
            makeText(
                description,
                14f,
                muted
            ).apply {
                setPadding(
                    0,
                    dp(10),
                    0,
                    0
                )
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

    // ОФОРМЛЕННОЕ ОКНО СОЗДАНИЯ ЗАДАЧИ

    private fun addTaskDialog() {

        var selectedDate: Long? = null
        var selectedHour: Int? = null
        var selectedMinute: Int? = null
        var selectedCategory = categories[0]

        val dialog = Dialog(this)

        val container = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL

            background = rounded(
                white,
                dp(28).toFloat()
            )

            setPadding(
                dp(22),
                dp(22),
                dp(22),
                dp(20)
            )
        }

        val title = makeText(
            "Новая задача",
            24f,
            dark,
            true
        )

        val subtitle = makeText(
            "Запланируй что-нибудь хорошее",
            13f,
            muted
        )

        subtitle.setPadding(
            0,
            dp(5),
            0,
            dp(20)
        )

        val titleLabel = makeText(
            "Название",
            13f,
            dark,
            true
        )

        val titleInput = EditText(this).apply {
            hint = "Что нужно сделать?"
            textSize = 15f
            setTextColor(dark)
            setHintTextColor(muted)
            setSingleLine(true)

            background = rounded(
                pageBackground,
                dp(15).toFloat()
            )

            setPadding(
                dp(14),
                dp(13),
                dp(14),
                dp(13)
            )
        }

        val descriptionLabel = makeText(
            "Описание",
            13f,
            dark,
            true
        )

        descriptionLabel.setPadding(
            0,
            dp(15),
            0,
            dp(5)
        )

        val descriptionInput = EditText(this).apply {
            hint = "Дополнительная информация..."
            textSize = 14f
            setTextColor(dark)
            setHintTextColor(muted)
            minLines = 3
            gravity = Gravity.TOP

            background = rounded(
                pageBackground,
                dp(15).toFloat()
            )

            setPadding(
                dp(14),
                dp(13),
                dp(14),
                dp(13)
            )
        }

        val categoryLabel = makeText(
            "Категория",
            13f,
            dark,
            true
        )

        categoryLabel.setPadding(
            0,
            dp(17),
            0,
            dp(9)
        )

        val categoryContainer = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }

        fun updateCategories() {

            categoryContainer.removeAllViews()

            categories.forEach { category ->

                val selected = category == selectedCategory

                val chip = TextView(this).apply {
                    text = category
                    textSize = 12f
                    gravity = Gravity.CENTER

                    setTextColor(
                        if (selected) white else dark
                    )

                    background = rounded(
                        if (selected) purple else pageBackground,
                        dp(14).toFloat()
                    )

                    setPadding(
                        dp(11),
                        dp(10),
                        dp(11),
                        dp(10)
                    )

                    setOnClickListener {
                        selectedCategory = category
                        updateCategories()
                    }
                }

                val params = LinearLayout.LayoutParams(
                    -2,
                    -2
                )

                params.marginEnd = dp(6)

                categoryContainer.addView(
                    chip,
                    params
                )
            }
        }

        updateCategories()

        val dateLabel = makeText(
            "Дата и время",
            13f,
            dark,
            true
        )

        dateLabel.setPadding(
            0,
            dp(17),
            0,
            dp(9)
        )

        val dateRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
        }

        val dateButton = TextView(this).apply {
            text = "Выбрать дату"
            textSize = 13f
            gravity = Gravity.CENTER
            setTextColor(purple)

            background = rounded(
                lightPurple,
                dp(14).toFloat()
            )

            setPadding(
                dp(12),
                dp(14),
                dp(12),
                dp(14)
            )

            setOnClickListener {

                val calendar = Calendar.getInstance()

                DatePickerDialog(
                    this@MainActivity,
                    { _, year, month, day ->

                        val chosen = Calendar.getInstance()

                        chosen.set(
                            year,
                            month,
                            day,
                            0,
                            0,
                            0
                        )

                        chosen.set(
                            Calendar.MILLISECOND,
                            0
                        )

                        selectedDate = chosen.timeInMillis

                        dateButton.text =
                            SimpleDateFormat(
                                "d MMM yyyy",
                                Locale("ru")
                            ).format(
                                Date(selectedDate!!)
                            )
                    },
                    calendar.get(Calendar.YEAR),
                    calendar.get(Calendar.MONTH),
                    calendar.get(Calendar.DAY_OF_MONTH)
                ).show()
            }
        }

        val timeButton = TextView(this).apply {
            text = "Выбрать время"
            textSize = 13f
            gravity = Gravity.CENTER
            setTextColor(purple)

            background = rounded(
                lightPurple,
                dp(14).toFloat()
            )

            setPadding(
                dp(12),
                dp(14),
                dp(12),
                dp(14)
            )

            setOnClickListener {

                val calendar = Calendar.getInstance()

                TimePickerDialog(
                    this@MainActivity,
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
        }

        dateRow.addView(
            dateButton,
            LinearLayout.LayoutParams(
                0,
                -2,
                1f
            ).apply {
                marginEnd = dp(7)
            }
        )

        dateRow.addView(
            timeButton,
            LinearLayout.LayoutParams(
                0,
                -2,
                1f
            )
        )

        val buttons = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            setPadding(0, dp(23), 0, 0)
        }

        val cancelButton = TextView(this).apply {
            text = "Отмена"
            textSize = 14f
            gravity = Gravity.CENTER
            setTextColor(muted)

            background = rounded(
                pageBackground,
                dp(15).toFloat()
            )

            setPadding(
                dp(15),
                dp(14),
                dp(15),
                dp(14)
            )

            setOnClickListener {
                dialog.dismiss()
            }
        }

        val saveButton = TextView(this).apply {
            text = "Добавить"
            textSize = 14f
            gravity = Gravity.CENTER
            setTextColor(white)
            typeface = Typeface.DEFAULT_BOLD

            background = rounded(
                purple,
                dp(15).toFloat()
            )

            setPadding(
                dp(15),
                dp(14),
                dp(15),
                dp(14)
            )

            setOnClickListener {

                val taskTitle =
                    titleInput.text.toString().trim()

                if (taskTitle.isEmpty()) {

                    Toast.makeText(
                        this@MainActivity,
                        "Введи название задачи",
                        Toast.LENGTH_SHORT
                    ).show()

                    return@setOnClickListener
                }

                tasks.add(
                    Task(
                        id = System.currentTimeMillis(),
                        title = taskTitle,
                        description =
                            descriptionInput.text.toString().trim(),
                        category = selectedCategory,
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

        buttons.addView(
            cancelButton,
            LinearLayout.LayoutParams(
                0,
                -2,
                1f
            ).apply {
                marginEnd = dp(8)
            }
        )

        buttons.addView(
            saveButton,
            LinearLayout.LayoutParams(
                0,
                -2,
                1f
            )
        )

        container.addView(title)
        container.addView(subtitle)
        container.addView(titleLabel)
        container.addView(titleInput)
        container.addView(descriptionLabel)
        container.addView(descriptionInput)
        container.addView(categoryLabel)

        val categoryScroll = HorizontalScrollView(this).apply {
            isHorizontalScrollBarEnabled = false
            addView(categoryContainer)
        }

        container.addView(categoryScroll)
        container.addView(dateLabel)
        container.addView(dateRow)
        container.addView(buttons)

        dialog.setContentView(container)

        dialog.window?.setBackgroundDrawableResource(
            android.R.color.transparent
        )

        dialog.show()

        dialog.window?.setLayout(
            resources.displayMetrics.widthPixels - dp(32),
            WindowManager.LayoutParams.WRAP_CONTENT
        )
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

        prefs.edit()
            .putString("tasks", array.toString())
            .apply()
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

                    title = item.optString(
                        "title",
                        ""
                    ),

                    description = item.optString(
                        "description",
                        ""
                    ),

                    category = item.optString(
                        "category",
                        "Личное"
                    ),

                    date = if (
                        item.has("date") &&
                        !item.isNull("date")
                    ) {
                        item.optLong("date")
                    } else null,

                    hour = if (
                        item.has("hour") &&
                        !item.isNull("hour")
                    ) {
                        item.optInt("hour")
                    } else null,

                    minute = if (
                        item.has("minute") &&
                        !item.isNull("minute")
                    ) {
                        item.optInt("minute")
                    } else null,

                    done = item.optBoolean(
                        "done",
                        false
                    )
                )
            )
        }
    }
}
