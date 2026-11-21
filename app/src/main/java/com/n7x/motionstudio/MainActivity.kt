package com.n7x.motionstudio

import android.graphics.BitmapFactory
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.n7x.motionstudio.ui.navigation.N7XNavGraph
import com.n7x.motionstudio.ui.theme.N7XTheme

class MainActivity : ComponentActivity() {

    /** True when both bundled sample pictures can be decoded from the APK. */
    private fun samplePicturesLoad(): Boolean {
        return listOf(R.drawable.sample_a, R.drawable.sample_b).all { id ->
            try {
                BitmapFactory.decodeResource(resources, id) != null
            } catch (e: Exception) {
                false
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val app = application as N7XApp
        setContent {
            N7XTheme {
                N7XNavGraph(app)

                var crash by remember { mutableStateOf(CrashLog.read(app)) }
                var samplesBroken by remember { mutableStateOf(!samplePicturesLoad()) }
                if (samplesBroken) {
                    AlertDialog(
                        onDismissRequest = { samplesBroken = false },
                        title = { Text("Sample pictures missing") },
                        text = {
                            Text(
                                "The built-in sample pictures used by the filter and " +
                                    "transition previews could not be loaded."
                            )
                        },
                        confirmButton = {
                            TextButton(onClick = { samplesBroken = false }) { Text("OK") }
                        }
                    )
                }
                val clipboard = LocalClipboardManager.current
                crash?.let { text ->
                    AlertDialog(
                        onDismissRequest = {
                            CrashLog.clear(app)
                            crash = null
                        },
                        title = { Text("App crashed last time") },
                        text = {
                            Text(
                                text.take(3500),
                                fontSize = 10.sp,
                                modifier = Modifier
                                    .heightIn(max = 360.dp)
                                    .verticalScroll(rememberScrollState())
                            )
                        },
                        confirmButton = {
                            TextButton(onClick = { clipboard.setText(AnnotatedString(text)) }) {
                                Text("Copy error")
                            }
                        },
                        dismissButton = {
                            TextButton(onClick = {
                                CrashLog.clear(app)
                                crash = null
                            }) { Text("Close") }
                        }
                    )
                }
            }
        }
    }
}
