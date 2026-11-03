package com.chris.clipcatch

import android.os.Bundle
import android.os.Environment
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.yausername.youtubedl_android.YoutubeDL
import com.yausername.youtubedl_android.YoutubeDLRequest
import com.yausername.ffmpeg.FFmpeg
import java.io.File
import java.util.concurrent.Executors

class MainActivity : ComponentActivity() {
    private val executor = Executors.newSingleThreadExecutor()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val initError = try {
            YoutubeDL.getInstance().init(this)
            FFmpeg.getInstance().init(this)
            null
        } catch (e: Exception) {
            e.message ?: "Could not initialise the download engine."
        }

        setContent {
            var url by remember { mutableStateOf("") }
            var format by remember { mutableStateOf("MP4") }
            var quality by remember { mutableStateOf("Best available") }
            var qualityMenu by remember { mutableStateOf(false) }
            var busy by remember { mutableStateOf(false) }
            var progress by remember { mutableFloatStateOf(0f) }
            var status by remember { mutableStateOf(initError ?: "Ready to download") }

            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    Column(
                        modifier = Modifier.fillMaxSize().padding(24.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text("ClipCatch", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold)
                        Text("Save videos and audio to your phone.")
                        OutlinedTextField(
                            value = url,
                            onValueChange = { url = it },
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text("Video URL") },
                            placeholder = { Text("Paste a link here") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri)
                        )

                        Text("Download as", fontWeight = FontWeight.SemiBold)
                        RowChoice(
                            selected = format,
                            choices = listOf("MP4", "MP3"),
                            onSelect = { format = it }
                        )

                        if (format == "MP4") {
                            Text("Video quality", fontWeight = FontWeight.SemiBold)
                            Column {
                                TextButton(onClick = { qualityMenu = true }) { Text(quality) }
                                DropdownMenu(expanded = qualityMenu, onDismissRequest = { qualityMenu = false }) {
                                    listOf("Best available", "1080p", "720p", "480p", "360p").forEach { option ->
                                        DropdownMenuItem(
                                            text = { Text(option) },
                                            onClick = { quality = option; qualityMenu = false }
                                        )
                                    }
                                }
                            }
                        }

                        Button(
                            onClick = {
                                if (url.isBlank() || busy || initError != null) return@Button
                                busy = true
                                progress = 0f
                                status = "Starting download…"
                                val selectedUrl = url.trim()
                                val selectedFormat = format
                                val selectedQuality = quality
                                executor.execute {
                                    try {
                                        val folder = File(
                                            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
                                            "ClipCatch"
                                        )
                                        if (!folder.exists() && !folder.mkdirs()) {
                                            error("Could not create the Downloads/ClipCatch folder.")
                                        }
                                        val request = YoutubeDLRequest(selectedUrl)
                                        request.addOption("--no-playlist")
                                        request.addOption("--no-mtime")
                                        request.addOption("-o", File(folder, "%(title).180B.%(ext)s").absolutePath)

                                        if (selectedFormat == "MP3") {
                                            request.addOption("--extract-audio")
                                            request.addOption("--audio-format", "mp3")
                                            request.addOption("--audio-quality", "0")
                                        } else {
                                            val selector = when (selectedQuality) {
                                                "1080p" -> "bestvideo[height<=1080]+bestaudio/best[height<=1080]"
                                                "720p" -> "bestvideo[height<=720]+bestaudio/best[height<=720]"
                                                "480p" -> "bestvideo[height<=480]+bestaudio/best[height<=480]"
                                                "360p" -> "bestvideo[height<=360]+bestaudio/best[height<=360]"
                                                else -> "bestvideo+bestaudio/best"
                                            }
                                            request.addOption("-f", selector)
                                            request.addOption("--merge-output-format", "mp4")
                                        }

                                        // Refresh yt-dlp before downloading so the bundled copy cannot go stale.
                                        runOnUiThread { status = "Checking for yt-dlp updates…" }
                                        YoutubeDL.getInstance().updateYoutubeDL(
                                            this@MainActivity,
                                            YoutubeDL.UpdateChannel.STABLE
                                        )

                                        YoutubeDL.getInstance().execute(request) { percent, eta, _ ->
                                            runOnUiThread {
                                                progress = (percent / 100f).coerceIn(0f, 1f)
                                                status = "Downloading… ${percent.toInt()}%  •  ETA ${eta}s"
                                            }
                                        }
                                        runOnUiThread {
                                            busy = false
                                            progress = 1f
                                            status = "Finished! Saved in Downloads/ClipCatch"
                                        }
                                    } catch (e: Exception) {
                                        runOnUiThread {
                                            busy = false
                                            status = "Download failed: ${e.message ?: "Unknown error"}"
                                        }
                                    }
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            enabled = !busy && initError == null
                        ) {
                            Text(if (busy) "Downloading…" else "Download")
                        }

                        if (busy) {
                            LinearProgressIndicator(progress = { progress }, modifier = Modifier.fillMaxWidth())
                        }
                        Card(modifier = Modifier.fillMaxWidth()) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(status)
                                Spacer(Modifier.height(6.dp))
                                Text("Files are saved to Downloads/ClipCatch.")
                            }
                        }
                        if (initError != null) {
                            Text("Engine setup error: $initError", color = MaterialTheme.colorScheme.error)
                        }
                        Spacer(Modifier.weight(1f))
                        Text("Only download content you have permission to save.")
                    }
                }
            }
        }
    }

    override fun onDestroy() {
        executor.shutdown()
        super.onDestroy()
    }
}

@androidx.compose.runtime.Composable
private fun RowChoice(selected: String, choices: List<String>, onSelect: (String) -> Unit) {
    androidx.compose.foundation.layout.Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        choices.forEach { choice ->
            if (choice == selected) {
                Button(onClick = { onSelect(choice) }) { Text(choice) }
            } else {
                TextButton(onClick = { onSelect(choice) }) { Text(choice) }
            }
        }
    }
}
