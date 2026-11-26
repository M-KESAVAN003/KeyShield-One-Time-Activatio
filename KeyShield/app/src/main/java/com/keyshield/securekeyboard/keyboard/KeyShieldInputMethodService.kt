package com.keyshield.securekeyboard.keyboard

import android.inputmethodservice.InputMethodService
import android.view.KeyEvent
import android.view.View
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import com.keyshield.securekeyboard.core.EnterAction
import com.keyshield.securekeyboard.core.FieldKey
import com.keyshield.securekeyboard.core.SessionManager
import com.keyshield.securekeyboard.data.KeyShieldSettings
import com.keyshield.securekeyboard.data.SettingsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

/**
 * The system keyboard. It must never log, store or transmit anything the user types,
 * and it does not read the text already present in a field.
 */
class KeyShieldInputMethodService : InputMethodService(), KeyboardView.Listener {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val sessions = SessionManager()
    private var settings = KeyShieldSettings()
    private var keyboard: KeyboardView? = null

    override fun onCreate() {
        super.onCreate()
        val repository = SettingsRepository(applicationContext)
        // Load once before the first field, so a disabled toggle is honoured immediately.
        settings = runBlocking { repository.settings.first() }
        scope.launch { repository.settings.collect { settings = it } }
    }

    override fun onCreateInputView(): View =
        KeyboardView(this, this).also {
            keyboard = it
            render()
        }

    override fun onStartInput(attribute: EditorInfo, restarting: Boolean) {
        super.onStartInput(attribute, restarting)
        val key = FieldKey(attribute.packageName, attribute.fieldId, attribute.inputType)
        sessions.start(key, restarting, settings.randomization)
    }

    override fun onStartInputView(info: EditorInfo, restarting: Boolean) {
        super.onStartInputView(info, restarting)
        render()
    }

    override fun onFinishInput() {
        super.onFinishInput()
        sessions.finish()
    }

    override fun onDestroy() {
        scope.cancel()
        sessions.finish()
        keyboard = null
        super.onDestroy()
    }

    /** Keeps the app's field visible in landscape instead of the full-screen editor. */
    override fun onEvaluateFullscreenMode(): Boolean = false

    private fun render() {
        val session = sessions.current ?: return
        keyboard?.show(session, EnterAction.label(editorAction()), settings.haptics)
    }

    private fun editorAction(): Int? {
        val info = currentInputEditorInfo ?: return null
        return EnterAction.resolve(info.imeOptions, info.actionId, info.actionLabel != null)
    }

    override fun onText(text: String) {
        currentInputConnection?.commitText(text, 1)
    }

    override fun onBackspace() {
        // A key event removes a selection or a whole character without reading the field.
        sendDownUpKeyEvents(KeyEvent.KEYCODE_DEL)
    }

    override fun onClear() {
        val connection = currentInputConnection ?: return
        connection.beginBatchEdit()
        connection.commitText("", 1)
        connection.deleteSurroundingText(CLEAR_LIMIT, CLEAR_LIMIT)
        connection.endBatchEdit()
    }

    override fun onEnter() {
        val action = editorAction()
        if (action != null) {
            currentInputConnection?.performEditorAction(action)
        } else {
            sendDownUpKeyEvents(KeyEvent.KEYCODE_ENTER)
        }
    }

    override fun onSwitchKeyboard() {
        getSystemService(InputMethodManager::class.java).showInputMethodPicker()
    }

    private companion object {
        const val CLEAR_LIMIT = 10_000
    }
}
