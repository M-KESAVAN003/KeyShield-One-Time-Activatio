package com.keyshield.securekeyboard.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.keyshield.securekeyboard.data.KeyShieldSettings
import com.keyshield.securekeyboard.data.SettingsRepository
import kotlinx.coroutines.launch

private val Blue = Color(0xFF2563EB)
private val Purple = Color(0xFF7C3AED)

@Composable
fun SetupScreen(status: SetupStatus, onOpenSettings: () -> Unit, onChooseKeyboard: () -> Unit) {
    val colors = if (isSystemInDarkTheme()) {
        darkColorScheme(primary = Blue, onPrimary = Color.White, secondary = Purple, background = Color(0xFF0B1220), surface = Color(0xFF0B1220))
    } else {
        lightColorScheme(primary = Blue, secondary = Purple)
    }
    val context = LocalContext.current.applicationContext
    val repository = remember { SettingsRepository(context) }
    val settings by repository.settings.collectAsState(initial = KeyShieldSettings())
    val scope = rememberCoroutineScope()
    val random = settings.randomization

    MaterialTheme(colorScheme = colors) {
        Surface(Modifier.fillMaxSize()) {
            Column(
                Modifier.safeDrawingPadding().imePadding().verticalScroll(rememberScrollState()).padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text("KeyShield", style = MaterialTheme.typography.headlineMedium)
                Text("One Switch. Smarter Input. Safer Digital Life.")

                Section("Setup") {
                    Text(
                        "KeyShield shuffles the digit positions in numeric fields, so a record of where " +
                            "you tapped says less about what you typed. It does not hide digits from " +
                            "someone who can see your screen."
                    )
                    Text("1. Enable the keyboard: " + if (status.enabled) "Enabled" else "Disabled")
                    Button(onClick = onOpenSettings) { Text("Enable Keyboard") }
                    Text("2. Make it your keyboard: " + if (status.selected) "Selected" else "Not selected as default")
                    Button(onClick = onChooseKeyboard, enabled = status.enabled) { Text("Select KeyShield") }
                    if (status.enabled && status.selected) {
                        Text("Setup complete. You don't need to open this app again.", color = Color(0xFF16A34A))
                    }
                }

                Section("Secure keyboard") {
                    Toggle("Adaptive Secure Keyboard", random.adaptiveEnabled) {
                        scope.launch { repository.set(SettingsRepository.ADAPTIVE, it) }
                    }
                    Toggle("Randomize number fields", random.randomizeNumbers, random.adaptiveEnabled) {
                        scope.launch { repository.set(SettingsRepository.NUMBERS, it) }
                    }
                    Toggle("Randomize phone fields", random.randomizePhone, random.adaptiveEnabled) {
                        scope.launch { repository.set(SettingsRepository.PHONE, it) }
                    }
                    Toggle("Haptic feedback", settings.haptics) {
                        scope.launch { repository.set(SettingsRepository.HAPTICS, it) }
                    }
                    Text("Changes apply the next time you focus a field.", style = MaterialTheme.typography.bodySmall)
                }

                Section("Test keyboard") {
                    TestField("Normal text", KeyboardType.Text)
                    TestField("Number", KeyboardType.Number)
                    TestField("Number password", KeyboardType.NumberPassword)
                    TestField("Decimal", KeyboardType.Decimal)
                    TestField("Phone", KeyboardType.Phone)
                }

                Text(
                    "Some banking and payment apps use their own PIN pad or refuse third-party " +
                        "keyboards. KeyShield cannot replace those.",
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }
    }
}

@Composable
private fun Section(title: String, content: @Composable () -> Unit) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            content()
        }
    }
}

@Composable
private fun Toggle(label: String, checked: Boolean, enabled: Boolean = true, onChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(label, Modifier.weight(1f))
        Switch(checked = checked, onCheckedChange = onChange, enabled = enabled)
    }
}

/** Held in memory only (not rememberSaveable), so test input is never written to saved state. */
@Composable
private fun TestField(label: String, type: KeyboardType) {
    var value by remember { mutableStateOf("") }
    OutlinedTextField(
        value = value,
        onValueChange = { value = it },
        label = { Text(label) },
        keyboardOptions = KeyboardOptions(keyboardType = type),
        singleLine = true,
        visualTransformation = if (type == KeyboardType.NumberPassword) {
            PasswordVisualTransformation()
        } else {
            VisualTransformation.None
        },
        modifier = Modifier.fillMaxWidth(),
    )
}
