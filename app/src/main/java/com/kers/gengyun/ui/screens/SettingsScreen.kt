package com.kers.gengyun.ui.screens

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.kers.gengyun.BuildConfig
import com.kers.gengyun.ui.MainViewModel
import com.kers.gengyun.util.AppUpdateChecker
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(vm: MainViewModel, onBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var checking by remember { mutableStateOf(false) }
    var downloading by remember { mutableStateOf(false) }
    var progress by remember { mutableStateOf(0f) }
    var statusText by remember { mutableStateOf("") }
    var updateInfo by remember { mutableStateOf<AppUpdateChecker.ReleaseInfo?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("关于 / 设置") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Text("耕耘", style = MaterialTheme.typography.headlineSmall)
            Text(
                "工资计算工具 · ${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})",
                style = MaterialTheme.typography.bodyMedium
            )
            Spacer(Modifier.height(16.dp))

            Text("检查更新", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))
            Text(
                "从 GitHub Releases（kers701/gengyun）检测新版本，逻辑与 wallpaper_app 一致。",
                style = MaterialTheme.typography.bodySmall
            )
            Spacer(Modifier.height(8.dp))

            Button(
                onClick = {
                    checking = true
                    statusText = "正在检查…"
                    updateInfo = null
                    scope.launch {
                        val result = withContext(Dispatchers.IO) {
                            AppUpdateChecker.checkLatest()
                        }
                        checking = false
                        when (result) {
                            is AppUpdateChecker.CheckResult.UpToDate -> {
                                statusText = "已是最新：${result.current}（线上 ${result.latest}）"
                                Toast.makeText(context, "已是最新版本", Toast.LENGTH_SHORT).show()
                            }
                            is AppUpdateChecker.CheckResult.UpdateAvailable -> {
                                updateInfo = result.info
                                statusText = "发现新版本 ${result.info.versionName}"
                            }
                            is AppUpdateChecker.CheckResult.Failed -> {
                                statusText = "检查失败：${result.message}"
                            }
                        }
                    }
                },
                enabled = !checking && !downloading,
                modifier = Modifier.fillMaxWidth()
            ) {
                if (checking) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp
                    )
                    Spacer(Modifier.width(8.dp))
                }
                Text(if (checking) "检查中…" else "检查更新")
            }

            if (statusText.isNotBlank()) {
                Spacer(Modifier.height(8.dp))
                Text(statusText, style = MaterialTheme.typography.bodyMedium)
            }

            updateInfo?.let { info ->
                Spacer(Modifier.height(12.dp))
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(12.dp)) {
                        Text("${info.name} (${info.tag})", style = MaterialTheme.typography.titleSmall)
                        if (info.body.isNotBlank()) {
                            Spacer(Modifier.height(4.dp))
                            Text(
                                info.body.take(500),
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                        Spacer(Modifier.height(8.dp))
                        if (downloading) {
                            LinearProgressIndicator(
                                progress = { progress },
                                modifier = Modifier.fillMaxWidth()
                            )
                            Text(
                                "下载中 ${(progress * 100).toInt()}%",
                                style = MaterialTheme.typography.labelSmall
                            )
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(
                                onClick = {
                                    if (!AppUpdateChecker.canInstallPackages(context)) {
                                        Toast.makeText(
                                            context,
                                            "请先允许安装未知应用",
                                            Toast.LENGTH_LONG
                                        ).show()
                                        AppUpdateChecker.openInstallPermissionSettings(context)
                                        return@Button
                                    }
                                    downloading = true
                                    progress = 0f
                                    statusText = "正在下载…"
                                    scope.launch {
                                        val result = withContext(Dispatchers.IO) {
                                            AppUpdateChecker.downloadApk(context, info) { p ->
                                                progress = p
                                            }
                                        }
                                        downloading = false
                                        when (result) {
                                            is AppUpdateChecker.DownloadResult.Ok -> {
                                                statusText = "下载完成，正在调起安装…"
                                                val ok = AppUpdateChecker.installApk(context, result.file)
                                                if (!ok) {
                                                    statusText = "无法调起安装，请到 Releases 页手动下载"
                                                    AppUpdateChecker.openReleasePage(context, info.htmlUrl)
                                                }
                                            }
                                            is AppUpdateChecker.DownloadResult.Failed -> {
                                                statusText = "下载失败：${result.message}"
                                            }
                                        }
                                    }
                                },
                                enabled = !downloading && info.apkUrl != null
                            ) {
                                Text("下载并安装")
                            }
                            OutlinedButton(
                                onClick = {
                                    AppUpdateChecker.openReleasePage(context, info.htmlUrl)
                                }
                            ) {
                                Text("打开发布页")
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(24.dp))
            Text("功能说明", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))
            Text(
                "1. 点击月份标题：设置本月默认白班/夜班\n" +
                    "2. 点击日期：上班/加班/休息，白班/夜班\n" +
                    "3. 长按日期：调班（可跨月）\n" +
                    "4. 班次时间段、时薪、夜班津贴、增减项\n" +
                    "5. 工作日加班 = 正班×正班价 + 加班×平时加班价\n" +
                    "数据仅保存在本机，不会上传。",
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}
