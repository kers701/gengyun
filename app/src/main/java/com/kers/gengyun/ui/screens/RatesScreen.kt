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
import com.kers.gengyun.domain.WageRates
import com.kers.gengyun.ui.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RatesScreen(vm: MainViewModel, onBack: () -> Unit) {
    val data by vm.monthData.collectAsState()
    var regular by remember(data) { mutableStateOf(data.wageRates.regularHourly.toString()) }
    var weekdayOt by remember(data) { mutableStateOf(data.wageRates.weekdayOtHourly.toString()) }
    var weekendOt by remember(data) { mutableStateOf(data.wageRates.weekendOtHourly.toString()) }
    var holidayOt by remember(data) { mutableStateOf(data.wageRates.holidayOtHourly.toString()) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("时薪配置") },
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
            OutlinedTextField(
                value = regular,
                onValueChange = { regular = it },
                label = { Text("正班每小时工资（元）") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                value = weekdayOt,
                onValueChange = { weekdayOt = it },
                label = { Text("工作日加班每小时（元）") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                value = weekendOt,
                onValueChange = { weekendOt = it },
                label = { Text("周末加班每小时（元）") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                value = holidayOt,
                onValueChange = { holidayOt = it },
                label = { Text("节假日加班每小时（元）") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(32.dp))
            Button(
                onClick = {
                    vm.updateWageRates(
                        WageRates(
                            regularHourly = regular.toDoubleOrNull() ?: 20.0,
                            weekdayOtHourly = weekdayOt.toDoubleOrNull() ?: 30.0,
                            weekendOtHourly = weekendOt.toDoubleOrNull() ?: 40.0,
                            holidayOtHourly = holidayOt.toDoubleOrNull() ?: 60.0
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
