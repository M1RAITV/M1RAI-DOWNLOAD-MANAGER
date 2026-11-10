package com.m1rai.downloadmanager

import android.app.DownloadManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.webkit.URLUtil
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Locale
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue

data class DownloadItem(val id: Long, val title: String, val status: String, val done: Long, val total: Long, val percent: Int)

class MainActivity : ComponentActivity() {
    private var incomingSharedUrl by mutableStateOf("")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        incomingSharedUrl = extractSharedUrl(intent)
        setContent {
            M1Theme {
                DownloadHome(
                    sharedUrl = incomingSharedUrl,
                    add = { value, metadata -> addDownload(this, value, metadata) },
                    analyze = { LinkAnalyzer().analyze(it) },
                    cancel = { getSystemService(DownloadManager::class.java).remove(it) },
                    refresh = { readDownloads(this) }
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        val shared = extractSharedUrl(intent)
        if (shared.isNotBlank()) incomingSharedUrl = shared
    }
}

private fun extractSharedUrl(intent: Intent?): String {
    if (intent == null) return ""
    return when (intent.action) {
        Intent.ACTION_SEND -> intent.getStringExtra(Intent.EXTRA_TEXT).orEmpty().trim()
        Intent.ACTION_VIEW -> intent.dataString.orEmpty().trim()
        else -> ""
    }
}

private fun addDownload(context: Context, raw: String, metadata: LinkAnalysisResult?): String? {
    val value = raw.trim()
    if (!DownloadLinkRules.isValidHttpUrl(value)) return "Enter a valid direct HTTP or HTTPS file link."
    val uri = Uri.parse(value)
    val filename = metadata?.fileName?.takeIf { it.isNotBlank() } ?: URLUtil.guessFileName(value, null, null)
    val request = DownloadManager.Request(uri)
        .setTitle(filename)
        .setDescription("M1RAI Download Manager")
        .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
        .setAllowedOverMetered(true)
        .setAllowedOverRoaming(false)
        .setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, filename)
    return runCatching {
        context.getSystemService(DownloadManager::class.java).enqueue(request)
        null
    }.getOrElse { it.localizedMessage ?: "Download could not be queued." }
}

private fun readDownloads(context: Context): List<DownloadItem> {
    val cursor = context.getSystemService(DownloadManager::class.java).query(DownloadManager.Query())
    val result = mutableListOf<DownloadItem>()
    cursor.use {
        val id = it.getColumnIndexOrThrow(DownloadManager.COLUMN_ID)
        val title = it.getColumnIndexOrThrow(DownloadManager.COLUMN_TITLE)
        val status = it.getColumnIndexOrThrow(DownloadManager.COLUMN_STATUS)
        val done = it.getColumnIndexOrThrow(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR)
        val total = it.getColumnIndexOrThrow(DownloadManager.COLUMN_TOTAL_SIZE_BYTES)
        while (it.moveToNext()) {
            val bytes = it.getLong(done)
            val size = it.getLong(total)
            val code = it.getInt(status)
            val label = when (code) {
                DownloadManager.STATUS_PENDING -> "Queued"
                DownloadManager.STATUS_RUNNING -> "Downloading"
                DownloadManager.STATUS_PAUSED -> "Paused"
                DownloadManager.STATUS_SUCCESSFUL -> "Completed"
                DownloadManager.STATUS_FAILED -> "Failed"
                else -> "Waiting"
            }
            val percent = if (size > 0) ((bytes * 100) / size).toInt().coerceIn(0, 100) else 0
            result += DownloadItem(it.getLong(id), it.getString(title) ?: "Download", label, bytes, size, percent)
        }
    }
    return result.sortedByDescending { it.id }
}

private fun sizeText(bytes: Long): String {
    if (bytes < 0) return "Size unknown"
    if (bytes < 1024) return "$bytes B"
    val units = listOf("KB", "MB", "GB", "TB")
    var n = bytes.toDouble()
    var i = -1
    do { n /= 1024.0; i++ } while (n >= 1024 && i < units.lastIndex)
    return String.format(Locale.US, "%.1f %s", n, units[i])
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DownloadHome(sharedUrl: String, add: (String, LinkAnalysisResult?) -> String?, analyze: suspend (String) -> Result<LinkAnalysisResult>, cancel: (Long) -> Unit, refresh: () -> List<DownloadItem>) {
    var url by remember { mutableStateOf(sharedUrl) }
    var message by remember { mutableStateOf<String?>(null) }
    var downloads by remember { mutableStateOf(emptyList<DownloadItem>()) }
    var analysis by remember { mutableStateOf<LinkAnalysisResult?>(null) }
    var analyzing by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    LaunchedEffect(sharedUrl) {
        if (sharedUrl.isNotBlank()) {
            url = sharedUrl
            message = null
        }
    }
    LaunchedEffect(Unit) {
        while (true) {
            downloads = runCatching { refresh() }.getOrDefault(emptyList())
            delay(1000)
        }
    }
    Scaffold(topBar = { TopAppBar(title = { Text("M1RAI", style = MaterialTheme.typography.headlineSmall) }) }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).padding(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Card(shape = RoundedCornerShape(24.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Your downloads. Under your control.", style = MaterialTheme.typography.titleLarge)
                    Text("Paste a direct file link or share one from another app.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    OutlinedTextField(value = url, onValueChange = { url = it; message = null; analysis = null }, modifier = Modifier.fillMaxWidth(), label = { Text("Download link") }, placeholder = { Text("https://example.com/file.zip") }, singleLine = true)
                    Button(
                        onClick = {
                            if (analysis == null) {
                                scope.launch {
                                    analyzing = true
                                    message = null
                                    analysis = null
                                    val result = analyze(url)
                                    result.onFailure { message = it.localizedMessage ?: "Could not analyze this link." }
                                        .onSuccess { metadata ->
                                            if (metadata.httpStatus in 200..299 || metadata.httpStatus == 206) analysis = metadata
                                            else message = "The server returned HTTP ${metadata.httpStatus}. Check the link and try again."
                                        }
                                    analyzing = false
                                }
                            } else {
                                message = add(url, analysis)
                                if (message == null) { url = ""; analysis = null }
                            }
                        },
                        enabled = url.isNotBlank() && !analyzing,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null)
                        Text(if (analyzing) "  Analyzing…" else if (analysis == null) "  Analyze link" else "  Start download")
                    }
                    analysis?.let { info ->
                        Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                            Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                                Text(info.fileName, style = MaterialTheme.typography.titleMedium)
                                Text(info.mimeType ?: "File type not provided", color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(info.contentLength?.let { sizeText(it) } ?: "File size unknown")
                                Text(if (info.supportsRanges) "Resume support reported by server" else "Resume support not confirmed", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                    message?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                }
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Your downloads", style = MaterialTheme.typography.titleLarge)
                Text("${downloads.size} items", style = MaterialTheme.typography.labelLarge)
            }
            if (downloads.isEmpty()) {
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(22.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Default.Download, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Text("Nothing here yet", style = MaterialTheme.typography.titleMedium)
                        Text("New downloads will appear here.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(downloads, key = { it.id }) { item ->
                        Card(Modifier.fillMaxWidth()) {
                            Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                                Text(item.title, style = MaterialTheme.typography.titleMedium)
                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text(item.status, color = MaterialTheme.colorScheme.primary)
                                    Text(if (item.total > 0) "${item.percent}%" else sizeText(item.done))
                                }
                                LinearProgressIndicator(progress = { item.percent / 100f }, modifier = Modifier.fillMaxWidth())
                                Text("${sizeText(item.done)} / ${sizeText(item.total)}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                if (item.status != "Completed") {
                                    OutlinedButton(onClick = { cancel(item.id) }) {
                                        Icon(Icons.Default.Delete, contentDescription = null)
                                        Text("  Cancel")
                                    }
                                }
                            }
                        }
                    }
                }
            }
            Text("Early development build", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun M1Theme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = darkColorScheme(
        primary = Color(0xFFFF4057),
        secondary = Color(0xFFB8C5D9),
        background = Color(0xFF0B0D12),
        surface = Color(0xFF151922),
        surfaceVariant = Color(0xFF202632)
    ), content = content)
}
