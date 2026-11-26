package com.keyshield.securekeyboard

import com.keyshield.securekeyboard.core.*
import org.junit.Assert.*
import org.junit.Test

class CoreTest {
    private val on = RandomizationSettings()
    private fun field(type: Int, id: Int = 1) = FieldKey("app", id, type)
    private fun numeric(type: Int, s: RandomizationSettings = on) =
        FieldClassifier.classify(type, s) as KeyboardMode.Numeric

    @Test fun everyShuffleContainsEachDigitExactlyOnce() {
        repeat(5000) { assertEquals((0..9).toList(), DigitLayout.shuffled().sorted()) }
    }

    @Test fun shufflesVary() {
        assertTrue(List(100) { DigitLayout.shuffled() }.toSet().size > 90)
    }

    @Test fun textPasswordAndUnknownUseQwerty() {
        listOf(0, 0x1, 0x81, 0x91, 0xe1, 0xf).forEach {
            assertEquals(KeyboardMode.Qwerty(), FieldClassifier.classify(it, on))
        }
        assertEquals(KeyboardMode.Qwerty(true), FieldClassifier.classify(0x4, on))
    }

    @Test fun numberVariantsAreDetected() {
        assertTrue(numeric(0x2).randomized)
        assertTrue(numeric(0x12).password)
        assertTrue(numeric(0x2002).decimal)
        assertTrue(numeric(0x1002).signed)
        assertFalse(numeric(0x2).decimal || numeric(0x2).signed || numeric(0x2).password)
    }

    @Test fun phoneIsFixedUnlessOptedIn() {
        assertEquals(PadKind.PHONE, numeric(0x3).kind)
        assertFalse(numeric(0x3).randomized)
        assertTrue(numeric(0x3, on.copy(randomizePhone = true)).randomized)
    }

    @Test fun togglesDisableRandomization() {
        assertFalse(numeric(0x2, on.copy(adaptiveEnabled = false)).randomized)
        assertFalse(numeric(0x2, on.copy(randomizeNumbers = false)).randomized)
        val session = SessionManager().start(field(0x2), false, on.copy(adaptiveEnabled = false))
        assertEquals(DigitLayout.FIXED, session.digits)
    }

    @Test fun restartOfSameFieldKeepsLayoutAndNewSessionReshuffles() {
        var calls = 0
        val manager = SessionManager { calls++; DigitLayout.shuffled() }
        val first = manager.start(field(0x2), false, on)
        assertSame(first, manager.start(field(0x2), true, on))
        assertEquals(1, calls)
        manager.start(field(0x2, id = 2), true, on) // different field
        manager.start(field(0x2, id = 2), false, on) // refocus
        manager.finish()
        manager.start(field(0x2, id = 2), true, on) // after finish
        assertEquals(4, calls)
    }

    @Test fun enterActionFollowsEditor() {
        assertEquals(ImeBits.ACTION_DONE, EnterAction.resolve(ImeBits.ACTION_DONE, 0, false))
        assertNull(EnterAction.resolve(ImeBits.ACTION_NONE, 0, false))
        assertNull(EnterAction.resolve(ImeBits.ACTION_UNSPECIFIED, 0, false))
        assertNull(EnterAction.resolve(ImeBits.ACTION_SEND or ImeBits.FLAG_NO_ENTER_ACTION, 0, false))
        assertEquals(99, EnterAction.resolve(0, 99, true))
    }
}
