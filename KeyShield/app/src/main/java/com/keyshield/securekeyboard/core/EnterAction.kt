package com.keyshield.securekeyboard.core

/** Mirrors the android.view.inputmethod.EditorInfo imeOptions constants. */
object ImeBits {
    const val MASK_ACTION = 0xff
    const val ACTION_UNSPECIFIED = 0
    const val ACTION_NONE = 1
    const val ACTION_GO = 2
    const val ACTION_SEARCH = 3
    const val ACTION_SEND = 4
    const val ACTION_NEXT = 5
    const val ACTION_DONE = 6
    const val ACTION_PREVIOUS = 7
    const val FLAG_NO_ENTER_ACTION = 0x40000000
}

object EnterAction {
    /** Returns the editor action to perform, or null if Enter should be sent as a key. */
    fun resolve(imeOptions: Int, customActionId: Int, hasCustomLabel: Boolean): Int? {
        if (imeOptions and ImeBits.FLAG_NO_ENTER_ACTION != 0) return null
        if (hasCustomLabel) return customActionId
        val action = imeOptions and ImeBits.MASK_ACTION
        return action.takeUnless { it == ImeBits.ACTION_NONE || it == ImeBits.ACTION_UNSPECIFIED }
    }

    fun label(action: Int?): String = when (action) {
        ImeBits.ACTION_GO -> "Go"
        ImeBits.ACTION_SEARCH -> "Search"
        ImeBits.ACTION_SEND -> "Send"
        ImeBits.ACTION_NEXT -> "Next"
        ImeBits.ACTION_DONE -> "Done"
        ImeBits.ACTION_PREVIOUS -> "Prev"
        else -> "Enter"
    }
}
