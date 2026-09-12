package com.kers.gengyun.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.kers.gengyun.ui.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(vm: MainViewModel, onBack: () -> Unit) {
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
        ) {
            Text("耕耘", style = MaterialTheme.typography.headlineSmall)
            Text("工资计算工具 · 初版 1.0.0", style = MaterialTheme.typography.bodyMedium)
            Spacer(Modifier.height(16.dp))
            Text(
                "功能说明：\n" +
                    "1. 选择月份查看日历\n" +
                    "2. 点击日期设置上班/加班/休息，以及白班/夜班\n" +
                    "3. 配置白班夜班时间段与夜班津贴\n" +
                    "4. 配置正班与各类加班时薪\n" +
                    "5. 添加岗位津贴、效益奖、五险一金、水电费等增减项\n" +
                    "数据仅保存在本机，不会上传。",
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}
