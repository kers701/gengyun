package com.kers.gengyun.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.kers.gengyun.domain.ShiftConfig
import com.kers.gengyun.domain.TimeRange
import com.kers.gengyun.ui.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShiftConfigScreen(vm: MainViewModel, onBack: () -> Unit) {
    val data by vm.monthData.collectAsState()
    var dayStartH by remember(data) { mutableStateOf(data.shiftConfig.dayShift.startHour.toString()) }
    var dayStartM by remember(data) { mutableStateOf(data.shiftConfig.dayShift.startMinute.toString()) }
    var dayEndH by remember(data) { mutableStateOf(data.shiftConfig.dayShift.endHour.toString()) }
    var dayEndM by remember(data) { mutableStateOf(data.shiftConfig.dayShift.endMinute.toString()) }
    var nightStartH by remember(data) { mutableStateOf(data.shiftConfig.nightShift.startHour.toString()) }
    var nightStartM by remember(data) { mutableStateOf(data.shiftConfig.nightShift.startMinute.toString()) }
    var nightEndH by remember(data) { mutableStateOf(data.shiftConfig.nightShift.endHour.toString()) }
    var nightEndM by remember(data) { mutableStateOf(data.shiftConfig.nightShift.endMinute.toString()) }
    var nightAllowance by remember(data) { mutableStateOf(data.nightAllowance.perNight.toString()) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("班次与夜班津贴") },
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
            Text("白班时间段", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))
            TimeRow("开始", dayStartH, dayStartM, { dayStartH = it }, { dayStartM = it })
            TimeRow("结束", dayEndH, dayEndM, { dayEndH = it }, { dayEndM = it })

            Spacer(Modifier.height(24.dp))
            Text("夜班时间段（可跨天）", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))
            TimeRow("开始", nightStartH, nightStartM, { nightStartH = it }, { nightStartM = it })
            TimeRow("结束", nightEndH, nightEndM, { nightEndH = it }, { nightEndM = it })

            Spacer(Modifier.height(24.dp))
            Text("夜班津贴（每天）", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = nightAllowance,
                onValueChange = { nightAllowance = it },
                label = { Text("元/天") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(32.dp))
            Button(
                onClick = {
                    val config = ShiftConfig(
                        dayShift = TimeRange(
                            dayStartH.toIntOrNull() ?: 8,
                            dayStartM.toIntOrNull() ?: 0,
                            dayEndH.toIntOrNull() ?: 17,
                            dayEndM.toIntOrNull() ?: 0
                        ),
                        nightShift = TimeRange(
                            nightStartH.toIntOrNull() ?: 20,
                            nightStartM.toIntOrNull() ?: 0,
                            nightEndH.toIntOrNull() ?: 8,
                            nightEndM.toIntOrNull() ?: 0
                        )
                    )
                    vm.updateShiftConfig(config)
                    vm.updateNightAllowance(
                        com.kers.gengyun.domain.NightAllowanceConfig(
                            perNight = nightAllowance.toDoubleOrNull() ?: 10.0
                        )
                    )
                    onBack()
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("保存")
            }
        }
    }
}

@Composable
private fun TimeRow(
    label: String,
    hour: String,
    minute: String,
    onHour: (String) -> Unit,
    onMinute: (String) -> Unit
) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(label, modifier = Modifier.width(40.dp), style = MaterialTheme.typography.bodyLarge)
        OutlinedTextField(
            value = hour,
            onValueChange = onHour,
            label = { Text("时") },
            singleLine = true,
            modifier = Modifier.weight(1f)
        )
        OutlinedTextField(
            value = minute,
            onValueChange = onMinute,
            label = { Text("分") },
            singleLine = true,
            modifier = Modifier.weight(1f)
        )
    }
    Spacer(Modifier.height(8.dp))
}
