package com.keyshield.securekeyboard.keyboard

import android.annotation.SuppressLint
import android.content.Context
import android.content.res.Configuration
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.view.Gravity
import android.view.HapticFeedbackConstants
import android.view.MotionEvent
import android.view.View
import android.view.WindowInsets
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import com.keyshield.securekeyboard.core.KeyboardMode
import com.keyshield.securekeyboard.core.KeyboardSession
import com.keyshield.securekeyboard.core.PadKind

/** Plain Android views keep the IME small and avoid Compose lifecycle wiring in a Service. */
@SuppressLint("ViewConstructor")
class KeyboardView(context: Context, private val listener: Listener) : LinearLayout(context) {

    interface Listener {
        fun onText(text: String)
        fun onBackspace()
        fun onClear()
        fun onEnter()
        fun onSwitchKeyboard()
    }

    private enum class Page { LETTERS, SYMBOLS, MORE_SYMBOLS }
    private enum class Shift { OFF, ONCE, LOCKED }

    private var session: KeyboardSession? = null
    private var enterLabel = "Enter"
    private var haptics = true
    private var page = Page.LETTERS
    private var shift = Shift.OFF
    private var lastShiftTap = 0L

    private val night = resources.configuration.uiMode and
        Configuration.UI_MODE_NIGHT_MASK == Configuration.UI_MODE_NIGHT_YES
    private val landscape =
        resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
    private val boardColor = Color.parseColor(if (night) "#0B1220" else "#E5E9F0")
    private val keyColor = Color.parseColor(if (night) "#1F2A40" else "#FFFFFF")
    private val specialColor = Color.parseColor(if (night) "#101828" else "#CBD3E1")
    private val textColor = Color.parseColor(if (night) "#FFFFFF" else "#101828")
    private val accentColor = Color.parseColor("#2563EB")

    init {
        orientation = VERTICAL
        setBackgroundColor(boardColor)
        // Keep the bottom row above the navigation bar on edge-to-edge devices.
        setOnApplyWindowInsetsListener { view, insets ->
            val bottom = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                insets.getInsets(WindowInsets.Type.navigationBars()).bottom
            } else {
                0
            }
            view.setPadding(dp(3), dp(4), dp(3), dp(4) + bottom)
            insets
        }
        setPadding(dp(3), dp(4), dp(3), dp(4))
    }

    fun show(session: KeyboardSession, enterLabel: String, haptics: Boolean) {
        if (this.session !== session) {
            shift = Shift.OFF
            val mode = session.mode
            page = if (mode is KeyboardMode.Qwerty && mode.startOnSymbols) Page.SYMBOLS else Page.LETTERS
        }
        this.session = session
        this.enterLabel = enterLabel
        this.haptics = haptics
        rebuild()
    }

    private fun rebuild() {
        removeAllViews()
        val current = session ?: return
        when (val mode = current.mode) {
            is KeyboardMode.Numeric -> buildPad(mode, current.digits)
            is KeyboardMode.Qwerty -> buildQwerty()
        }
    }

    private fun buildPad(mode: KeyboardMode.Numeric, digits: List<Int>) {
        addView(TextView(context).apply {
            text = if (mode.randomized) "KeyShield • randomized layout" else "KeyShield • fixed layout"
            setTextColor(textColor)
            textSize = 12f
            gravity = Gravity.CENTER
        })
        val height = if (landscape) 40 else 54
        fun digit(i: Int) = key(digits[i].toString(), description = "Digit ${digits[i]}") {
            listener.onText(digits[i].toString())
        }
        for (start in 0 until 9 step 3) {
            row(height, digit(start), digit(start + 1), digit(start + 2))
        }
        row(height, special("Clear", "Clear field") { listener.onClear() }, digit(9), backspace())

        val extras = buildList {
            if (mode.kind == PadKind.PHONE) addAll(listOf("+", "*", "#", "-", " ", "(", ")", ","))
            if (mode.signed) add("-")
            if (mode.decimal) addAll(listOf(".", ","))
        }
        if (extras.isNotEmpty()) {
            row(height, *extras.map { symbol ->
                val label = if (symbol == " ") "␣" else symbol
                key(label, description = if (symbol == " ") "Space" else null) {
                    listener.onText(symbol)
                }
            }.toTypedArray())
        }
        row(height, switcher(), enter(2f))
    }

    private fun buildQwerty() {
        val height = if (landscape) 38 else 48
        val rows = when (page) {
            Page.LETTERS -> listOf("qwertyuiop", "asdfghjkl", "zxcvbnm")
            Page.SYMBOLS -> listOf("1234567890", "@#$%&-+()", "*\"':;!?")
            Page.MORE_SYMBOLS -> listOf("~`|•√π÷×{}", "£€₹^°=[]\\", "_<>/©®¥")
        }
        row(height, *rows[0].map { character(it) }.toTypedArray())
        row(height, *rows[1].map { character(it) }.toTypedArray())
        val modifier = when (page) {
            Page.LETTERS -> special(
                if (shift == Shift.LOCKED) "⇪" else "⇧",
                when (shift) {
                    Shift.OFF -> "Shift off"
                    Shift.ONCE -> "Shift on"
                    Shift.LOCKED -> "Caps lock on"
                },
                1.5f,
            ) { tapShift() }.also { if (shift != Shift.OFF) style(it, accentColor, Color.WHITE) }
            Page.SYMBOLS -> special("=\\<", "More symbols", 1.5f) { open(Page.MORE_SYMBOLS) }
            Page.MORE_SYMBOLS -> special("?123", "Symbols", 1.5f) { open(Page.SYMBOLS) }
        }
        row(height, modifier, *rows[2].map { character(it) }.toTypedArray(), backspace(1.5f))
        val toggle = if (page == Page.LETTERS) {
            special("?123", "Numbers and symbols", 1.5f) { open(Page.SYMBOLS) }
        } else {
            special("ABC", "Letters", 1.5f) { open(Page.LETTERS) }
        }
        row(
            height,
            toggle,
            switcher(),
            key(",") { listener.onText(",") },
            key("space", 4f, "Space") { listener.onText(" ") },
            key(".") { listener.onText(".") },
            enter(1.8f),
        )
    }

    private fun character(c: Char): Button {
        val upper = page == Page.LETTERS && shift != Shift.OFF
        val label = if (upper) c.uppercaseChar().toString() else c.toString()
        return key(label) {
            listener.onText(label)
            if (shift == Shift.ONCE) {
                shift = Shift.OFF
                rebuild()
            }
        }
    }

    private fun tapShift() {
        val now = System.currentTimeMillis()
        shift = when {
            shift == Shift.ONCE && now - lastShiftTap < 400 -> Shift.LOCKED
            shift == Shift.OFF -> Shift.ONCE
            else -> Shift.OFF
        }
        lastShiftTap = now
        rebuild()
    }

    private fun open(target: Page) {
        page = target
        rebuild()
    }

    private fun enter(weight: Float) =
        key(enterLabel, weight) { listener.onEnter() }.also { style(it, accentColor, Color.WHITE) }

    private fun switcher() = special("🌐", "Choose keyboard") { listener.onSwitchKeyboard() }

    /** Tap deletes once; long press repeats until the finger lifts. */
    @SuppressLint("ClickableViewAccessibility")
    private fun backspace(weight: Float = 1f): Button {
        val button = special("⌫", "Backspace", weight) { listener.onBackspace() }
        val repeat = object : Runnable {
            override fun run() {
                if (!button.isAttachedToWindow) return
                listener.onBackspace()
                button.postDelayed(this, 60)
            }
        }
        button.setOnLongClickListener {
            button.post(repeat)
            true
        }
        button.setOnTouchListener { _, event ->
            if (event.actionMasked == MotionEvent.ACTION_UP ||
                event.actionMasked == MotionEvent.ACTION_CANCEL
            ) {
                button.removeCallbacks(repeat)
            }
            false
        }
        return button
    }

    private fun special(label: String, description: String, weight: Float = 1f, onTap: () -> Unit) =
        key(label, weight, description, onTap).also { style(it, specialColor, textColor) }

    private fun key(
        label: String,
        weight: Float = 1f,
        description: String? = null,
        onTap: () -> Unit,
    ) = Button(context).apply {
        text = label
        contentDescription = description ?: label
        isAllCaps = false
        textSize = if (label.length > 1) 14f else 20f
        setPadding(0, 0, 0, 0)
        minWidth = 0
        minimumWidth = 0
        minHeight = 0
        minimumHeight = 0
        stateListAnimator = null
        layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, weight).apply {
            setMargins(dp(2), dp(3), dp(2), dp(3))
        }
        style(this, keyColor, textColor)
        setOnClickListener {
            if (haptics) it.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
            onTap()
        }
    }

    private fun style(button: Button, background: Int, foreground: Int) {
        button.background = GradientDrawable().apply {
            setColor(background)
            cornerRadius = dp(8).toFloat()
        }
        button.setTextColor(foreground)
    }

    private fun row(heightDp: Int, vararg keys: View) {
        val row = LinearLayout(context).apply { orientation = HORIZONTAL }
        keys.forEach { row.addView(it) }
        addView(row, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(heightDp)))
    }

    private fun dp(value: Int) = (value * resources.displayMetrics.density).toInt()
}
