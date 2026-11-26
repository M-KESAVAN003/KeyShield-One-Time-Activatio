package com.keyshield.securekeyboard.core

/** Mirrors the android.text.InputType constants so the rules are testable on the JVM. */
object InputBits {
    const val MASK_CLASS = 0x0000000f
    const val MASK_VARIATION = 0x00000ff0
    const val CLASS_TEXT = 1
    const val CLASS_NUMBER = 2
    const val CLASS_PHONE = 3
    const val CLASS_DATETIME = 4
    const val NUMBER_PASSWORD = 0x10
    const val FLAG_SIGNED = 0x1000
    const val FLAG_DECIMAL = 0x2000
}

enum class PadKind { NUMBER, PHONE }

sealed interface KeyboardMode {
    /** [startOnSymbols] is used for date/time fields, which need digits and separators. */
    data class Qwerty(val startOnSymbols: Boolean = false) : KeyboardMode

    data class Numeric(
        val kind: PadKind,
        val decimal: Boolean,
        val signed: Boolean,
        val password: Boolean,
        val randomized: Boolean,
    ) : KeyboardMode
}

data class RandomizationSettings(
    val adaptiveEnabled: Boolean = true,
    val randomizeNumbers: Boolean = true,
    val randomizePhone: Boolean = false,
)

object FieldClassifier {
    fun classify(inputType: Int, settings: RandomizationSettings): KeyboardMode =
        when (inputType and InputBits.MASK_CLASS) {
            InputBits.CLASS_NUMBER -> KeyboardMode.Numeric(
                kind = PadKind.NUMBER,
                decimal = inputType and InputBits.FLAG_DECIMAL != 0,
                signed = inputType and InputBits.FLAG_SIGNED != 0,
                password = inputType and InputBits.MASK_VARIATION == InputBits.NUMBER_PASSWORD,
                randomized = settings.adaptiveEnabled && settings.randomizeNumbers,
            )
            InputBits.CLASS_PHONE -> KeyboardMode.Numeric(
                kind = PadKind.PHONE,
                decimal = false,
                signed = false,
                password = false,
                randomized = settings.adaptiveEnabled && settings.randomizePhone,
            )
            InputBits.CLASS_DATETIME -> KeyboardMode.Qwerty(startOnSymbols = true)
            // Text (including text passwords), TYPE_NULL and anything unknown.
            else -> KeyboardMode.Qwerty()
        }
}
