package com.keyshield.securekeyboard.core

/** Identifies an editor using metadata only, never its contents. */
data class FieldKey(val packageName: String?, val fieldId: Int, val inputType: Int)

data class KeyboardSession(val key: FieldKey, val mode: KeyboardMode, val digits: List<Int>)

class SessionManager(private val shuffle: () -> List<Int> = { DigitLayout.shuffled() }) {
    var current: KeyboardSession? = null
        private set

    /**
     * Call from onStartInput. A restart of the same field keeps the layout, because some apps
     * restart input while the user is still typing. Anything else begins a new session.
     */
    fun start(key: FieldKey, restarting: Boolean, settings: RandomizationSettings): KeyboardSession {
        val mode = FieldClassifier.classify(key.inputType, settings)
        val previous = current
        if (restarting && previous != null && previous.key == key && previous.mode == mode) {
            return previous
        }
        val randomized = mode is KeyboardMode.Numeric && mode.randomized
        val digits = if (randomized) shuffle() else DigitLayout.FIXED
        return KeyboardSession(key, mode, digits).also { current = it }
    }

    fun finish() {
        current = null
    }
}
