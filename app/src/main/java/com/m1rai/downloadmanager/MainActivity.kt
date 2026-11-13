package com.m1rai.downloadmanager

import android.Manifest
import android.app.DownloadManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.os.Build
import android.os.Environment
import android.webkit.URLUtil
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.Image
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.painterResource
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
                    cancel = { cancelDownload(this, it) },
                    retry = { retryDownload(this, it) },
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
    val sharedText = when (intent.action) {
        Intent.ACTION_SEND -> intent.getStringExtra(Intent.EXTRA_TEXT).orEmpty()
        Intent.ACTION_VIEW -> intent.dataString.orEmpty()
        else -> ""
    }
    return DownloadLinkRules.extractHttpUrl(sharedText).orEmpty()
}

private fun addDownload(context: Context, raw: String, metadata: LinkAnalysisResult?): String? {
    val value = raw.trim()
    if (!DownloadLinkRules.isValidHttpUrl(value)) return "Enter a valid direct HTTP or HTTPS file link."
    val uri = Uri.parse(value)
    val filename = metadata?.fileName?.takeIf { it.isNotBlank() } ?: URLUtil.guessFileName(value, null, null)
    val request = DownloadManager.Request(uri)
        .setTitle(filename)
        .setDescription("M1RAI Downloader")
        .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
        .setAllowedOverMetered(true)
        .setAllowedOverRoaming(false)
        .setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, DownloadDestination.relativePath(filename))
    return runCatching {
        val id = context.getSystemService(DownloadManager::class.java).enqueue(request)
        DownloadTaskStore(context).use { store ->
            store.save(
                StoredDownload(
                    downloadId = id,
                    url = value,
                    fileName = filename,
                    mimeType = metadata?.mimeType,
                    expectedBytes = metadata?.contentLength,
                    createdAt = System.currentTimeMillis()
                )
            )
        }
        null
    }.getOrElse { it.localizedMessage ?: "Download could not be queued." }
}

private fun cancelDownload(context: Context, downloadId: Long) {
    val removed = runCatching {
        context.getSystemService(DownloadManager::class.java).remove(downloadId)
    }.getOrDefault(0)
    if (removed > 0) {
        runCatching {
            DownloadTaskStore(context).use { it.markStatus(downloadId, "Cancelled") }
        }
    }
}

private fun retryDownload(context: Context, downloadId: Long): String? {
    val stored = runCatching {
        DownloadTaskStore(context).use { it.find(downloadId) }
    }.getOrNull() ?: return "The original download details are unavailable."
    val metadata = LinkAnalysisResult(
        fileName = stored.fileName,
        mimeType = stored.mimeType,
        contentLength = stored.expectedBytes,
        supportsRanges = false,
        httpStatus = 200
    )
    return addDownload(context, stored.url, metadata)
}

private fun readDownloads(context: Context): List<DownloadItem> {
    val taskStore = DownloadTaskStore(context)
    val stored = runCatching { taskStore.all().associateBy(StoredDownload::downloadId) }.getOrDefault(emptyMap())
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
            val downloadId = it.getLong(id)
            runCatching { taskStore.updateProgress(downloadId, label, bytes, size) }
            val storedTitle = stored[downloadId]?.fileName
            result += DownloadItem(downloadId, storedTitle ?: (it.getString(title) ?: "Download"), label, bytes, size, percent)
        }
    }
    val visibleIds = result.mapTo(mutableSetOf()) { it.id }
    stored.values.forEach { item ->
        if (visibleIds.add(item.downloadId)) {
            val savedStatus = DownloadHistoryPolicy.statusWhenMissing(item.transferStatus)
            val total = item.totalBytes ?: item.expectedBytes ?: 0L
            val percent = DownloadHistoryPolicy.progressPercent(item.downloadedBytes, total)
            result += DownloadItem(
                item.downloadId,
                item.fileName,
                savedStatus,
                item.downloadedBytes,
                total,
                percent
            )
        }
    }
    taskStore.close()
    return result.sortedByDescending { it.id }
}

internal fun sizeText(bytes: Long): String {
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
private fun DownloadHome(sharedUrl: String, add: (String, LinkAnalysisResult?) -> String?, analyze: suspend (String) -> Result<LinkAnalysisResult>, cancel: (Long) -> Unit, retry: (Long) -> String?, refresh: () -> List<DownloadItem>) {
    var url by remember { mutableStateOf(sharedUrl) }
    var message by remember { mutableStateOf<String?>(null) }
    var downloads by remember { mutableStateOf(emptyList<DownloadItem>()) }
    var analysis by remember { mutableStateOf<LinkAnalysisResult?>(null) }
    var analyzing by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    var pendingStorageAction by remember { mutableStateOf<(() -> Unit)?>(null) }
    val storagePermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        val action = pendingStorageAction
        pendingStorageAction = null
        if (granted) action?.invoke()
        else message = "Storage permission was denied. Allow it in Android Settings to save downloads into M1RAI DOWNLOADS."
    }
    fun withStoragePermission(action: () -> Unit) {
        if (DownloadStoragePermissionPolicy.requiresRuntimePermission(Build.VERSION.SDK_INT) &&
            context.checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED
        ) {
            pendingStorageAction = action
            storagePermissionLauncher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
        } else {
            action()
        }
    }
    val platformDetection = remember(url) { PlatformUrlDetector.detect(url) }
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
    Scaffold(
        containerColor = Color(0xFF080B13),
        topBar = {
            TopAppBar(
                title = {
                    Column(verticalArrangement = Arrangement.spacedBy(0.dp)) {
                        Text("M1RAI Downloader", style = MaterialTheme.typography.titleLarge)
                        Text("SMART DOWNLOAD EXPERIENCE", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.secondary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF0B1020),
                    titleContentColor = Color(0xFFF4F7FF)
                )
            )
        }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).padding(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Card(shape = RoundedCornerShape(24.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Everything you save. One powerful place.", style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.onSurface)
                    Text("Paste a link or share it from any app. M1RAI will inspect supported direct downloads.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    OutlinedTextField(value = url, onValueChange = { url = it; message = null; analysis = null }, modifier = Modifier.fillMaxWidth(), label = { Text("Download link") }, placeholder = { Text("https://example.com/file.zip") }, singleLine = true)
                    AnimatedVisibility(
                        visible = platformDetection is PlatformDetection.Detected ||
                            platformDetection is PlatformDetection.Excluded,
                        enter = fadeIn() + slideInVertically { it / 3 },
                        exit = fadeOut()
                    ) {
                        PlatformIdentityCard(platformDetection)
                    }
                    Button(
                        onClick = {
                            when (val detection = platformDetection) {
                                is PlatformDetection.Detected -> {
                                    message = "${detection.platform.displayName} detected. Its media analysis adapter is not connected yet."
                                }
                                is PlatformDetection.Excluded -> {
                                    message = "${detection.platformName} is excluded from M1RAI's media downloader targets."
                                }
                                else -> {
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
                                        withStoragePermission {
                                            message = add(url, analysis)
                                            if (message == null) { url = ""; analysis = null }
                                        }
                                    }
                                }
                            }
                        },
                        enabled = url.isNotBlank() && !analyzing,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null)
                        Text(
                            when {
                                analyzing -> "  Analyzing…"
                                platformDetection is PlatformDetection.Detected -> "  Platform detected"
                                platformDetection is PlatformDetection.Excluded -> "  Native app download"
                                analysis == null -> "  Analyze link"
                                else -> "  Start download"
                            }
                        )
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
                Text("Downloads", style = MaterialTheme.typography.headlineSmall)
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
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    if (item.status == "Failed") {
                                        OutlinedButton(onClick = { withStoragePermission { message = retry(item.id) } }) {
                                            Text("Retry")
                                        }
                                    }
                                    if (item.status == "Queued" || item.status == "Downloading" || item.status == "Paused") {
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
            }
            Text("M1RAI DOWNLOADER  •  BUILT FOR CONTROL", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun PlatformIdentityCard(detection: PlatformDetection) {
    val platformName: String
    val subtitle: String
    val monogram: String
    when (detection) {
        is PlatformDetection.Detected -> {
            platformName = detection.platform.displayName
            subtitle = "Platform recognized · Media adapter not connected yet"
            monogram = detection.platform.displayName.take(2).uppercase(Locale.ROOT)
        }
        is PlatformDetection.Excluded -> {
            platformName = detection.platformName
            subtitle = "Uses its own native download or save experience"
            monogram = detection.platformName.take(2).uppercase(Locale.ROOT)
        }
        else -> return
    }
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(14.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
        ) {
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.size(48.dp)
            ) {
                Box(contentAlignment = androidx.compose.ui.Alignment.Center) {
                    val logo = when ((detection as? PlatformDetection.Detected)?.platform) {
                        MediaPlatform.YOUTUBE -> R.drawable.ic_platform_youtube
                        MediaPlatform.INSTAGRAM -> R.drawable.ic_platform_instagram
                        MediaPlatform.TIKTOK -> R.drawable.ic_platform_tiktok
                        MediaPlatform.FACEBOOK -> R.drawable.ic_platform_facebook
                        MediaPlatform.REDDIT -> R.drawable.ic_platform_reddit
                        MediaPlatform.PINTEREST -> R.drawable.ic_platform_pinterest
                        MediaPlatform.SNAPCHAT -> R.drawable.ic_platform_snapchat
                        else -> 0
                    }
                    if (logo != 0) Image(painterResource(logo), contentDescription = "$platformName logo", modifier = Modifier.size(30.dp))
                    else Text(monogram, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onPrimaryContainer)
                }
            }
            Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(platformName, style = MaterialTheme.typography.titleMedium)
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun M1Theme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = Color(0xFF38DDF5),
            onPrimary = Color(0xFF04151B),
            primaryContainer = Color(0xFF123B49),
            onPrimaryContainer = Color(0xFFB9F5FF),
            secondary = Color(0xFFB39BFF),
            onSecondary = Color(0xFF20133F),
            secondaryContainer = Color(0xFF35265A),
            onSecondaryContainer = Color(0xFFE5DCFF),
            tertiary = Color(0xFF73E6C2),
            onTertiary = Color(0xFF06251D),
            tertiaryContainer = Color(0xFF123D32),
            onTertiaryContainer = Color(0xFFB8F5E1),
            background = Color(0xFF080B13),
            onBackground = Color(0xFFF2F6FF),
            surface = Color(0xFF101725),
            onSurface = Color(0xFFF2F6FF),
            surfaceVariant = Color(0xFF172235),
            onSurfaceVariant = Color(0xFFA8B7CC),
            outline = Color(0xFF3B4B62),
            error = Color(0xFFFF7185),
            onError = Color(0xFF3A0710)
        ),
        content = content
    )
}
