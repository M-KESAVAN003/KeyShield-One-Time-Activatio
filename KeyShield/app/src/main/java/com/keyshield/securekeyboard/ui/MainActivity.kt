package com.keyshield.securekeyboard.ui

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.view.inputmethod.InputMethodManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

data class SetupStatus(val enabled: Boolean = false, val selected: Boolean = false)

class MainActivity : ComponentActivity() {
    private var status by mutableStateOf(SetupStatus())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            SetupScreen(
                status = status,
                onOpenSettings = { startActivity(Intent(Settings.ACTION_INPUT_METHOD_SETTINGS)) },
                onChooseKeyboard = { inputMethods().showInputMethodPicker() },
            )
        }
    }

    override fun onResume() {
        super.onResume()
        status = readStatus()
    }

    /** The keyboard picker is a dialog, so focus (not resume) tells us it has closed. */
    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) status = readStatus()
    }

    private fun inputMethods() = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager

    private fun readStatus(): SetupStatus {
        val enabled = inputMethods().enabledInputMethodList.any { it.packageName == packageName }
        val current = Settings.Secure.getString(contentResolver, Settings.Secure.DEFAULT_INPUT_METHOD)
        return SetupStatus(enabled, current?.startsWith("$packageName/") == true)
    }
}
