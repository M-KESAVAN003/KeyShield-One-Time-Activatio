package com.keyshield.securekeyboard.core

import java.security.SecureRandom
import java.util.Random

object DigitLayout {
    /** Order of a conventional phone keypad: 1-9, then 0 in the bottom row. */
    val FIXED: List<Int> = listOf(1, 2, 3, 4, 5, 6, 7, 8, 9, 0)

    private val secureRandom = SecureRandom()

    fun shuffled(random: Random = secureRandom): List<Int> =
        FIXED.toMutableList().also { it.shuffle(random) }
}
