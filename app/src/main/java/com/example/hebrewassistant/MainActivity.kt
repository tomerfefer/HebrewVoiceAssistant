package com.example.hebrewassistant

import android.Manifest
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private lateinit var speech: SpeechRecognizerHelper
    private val translator = HebrewTranslator()
    private val parser = CommandParser()
    private lateinit var executor: ActionExecutor

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        speech = SpeechRecognizerHelper(this)
        executor = ActionExecutor(this)

        setContent {
            MaterialTheme {
                var hebrew by remember { mutableStateOf("") }
                var english by remember { mutableStateOf("") }
                var typed by remember { mutableStateOf("") }
                var status by remember { mutableStateOf("Tap the mic and speak Hebrew") }
                val scope = rememberCoroutineScope()

                // Shared by voice and typed input: translate, parse with Claude, run the action.
                fun process(heb: String) {
                    hebrew = heb
                    status = "Understanding…"
                    scope.launch {
                        status = runCatching {
                            english = translator.toEnglish(heb)
                            executor.execute(parser.parse(heb, english))
                        }.getOrElse { "Error: ${it.message}" }
                    }
                }

                fun startListening() {
                    status = "Listening…"
                    speech.listen(
                        onPartial = { hebrew = it },
                        onResult = { process(it) },
                        onError = { status = "Speech error $it" },
                    )
                }

                val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
                    if (it) startListening() else status = "Microphone permission denied"
                }

                Column(
                    Modifier.fillMaxSize().padding(24.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(hebrew.ifBlank { "—" }, style = MaterialTheme.typography.headlineMedium)
                    Spacer(Modifier.height(8.dp))
                    Text(english.ifBlank { "—" }, style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(24.dp))
                    Text(status)
                    Spacer(Modifier.height(24.dp))
                    Button(onClick = { permission.launch(Manifest.permission.RECORD_AUDIO) }) { Text("🎤 Speak") }
                    Spacer(Modifier.height(24.dp))
                    // Typed input for testing without a microphone (e.g. on the emulator).
                    OutlinedTextField(
                        value = typed,
                        onValueChange = { typed = it },
                        label = { Text("Or type a command (Hebrew)") },
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Spacer(Modifier.height(8.dp))
                    Button(
                        onClick = { process(typed.trim()) },
                        enabled = typed.isNotBlank(),
                    ) { Text("Send") }
                }
            }
        }
    }

    override fun onDestroy() {
        speech.destroy(); translator.close(); super.onDestroy()
    }
}
