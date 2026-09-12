package com.kers.gengyun.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.kers.gengyun.ui.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdjustScreen(vm: MainViewModel, onBack: () -> Unit) {
    val data by vm.monthData.collectAsState()
    var name by remember { mutableStateOf("") }
    var amount by remember { mutableStateOf("") }
    var isIncome by remember { mutableStateOf(true) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("增减项目") },
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
            Text("添加项目", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("名称（如：岗位津贴 / 五险一金）") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = amount,
                onValueChange = { amount = it },
                label = { Text("金额（元）") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(8.dp))
            Row {
                FilterChip(
                    selected = isIncome,
                    onClick = { isIncome = true },
                    label = { Text("额外收入") }
                )
                Spacer(Modifier.width(8.dp))
                FilterChip(
                    selected = !isIncome,
                    onClick = { isIncome = false },
                    label = { Text("额外支出") }
                )
            }
            Spacer(Modifier.height(12.dp))
            Button(
                onClick = {
                    val a = amount.toDoubleOrNull()
                    if (name.isNotBlank() && a != null && a > 0) {
                        vm.addAdjustItem(name.trim(), a, isIncome)
                        name = ""
                        amount = ""
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("添加")
            }

            Spacer(Modifier.height(24.dp))
            Text("本月项目", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))

            if (data.adjustItems.isEmpty()) {
                Text("暂无增减项目", color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else {
                LazyColumn {
                    items(data.adjustItems, key = { it.id }) { item ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                        ) {
                            Row(
                                Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(item.name, style = MaterialTheme.typography.bodyLarge)
                                    Text(
                                        if (item.isIncome) "收入" else "支出",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = if (item.isIncome) Color(0xFF4CAF50) else Color(0xFFF44336)
                                    )
                                }
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        "%s¥%.2f".format(if (item.isIncome) "+" else "-", kotlin.math.abs(item.amount)),
                                        style = MaterialTheme.typography.titleMedium,
                                        color = if (item.isIncome) Color(0xFF4CAF50) else Color(0xFFF44336)
                                    )
                                    IconButton(onClick = { vm.removeAdjustItem(item.id) }) {
                                        Icon(Icons.Default.Delete, contentDescription = "删除", tint = Color.Gray)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
