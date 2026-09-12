package com.kers.gengyun.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kers.gengyun.domain.*
import com.kers.gengyun.ui.MainViewModel
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    vm: MainViewModel,
    onOpenSettings: () -> Unit,
    onOpenRates: () -> Unit,
    onOpenAdjust: () -> Unit,
    onOpenShift: () -> Unit
) {
    val yearMonth by vm.yearMonth.collectAsState()
    val data by vm.monthData.collectAsState()
    val summary by vm.summary.collectAsState()

    var selectedDay by remember { mutableStateOf<String?>(null) }
    var showDayDialog by remember { mutableStateOf(false) }

    val ym = YearMonth.parse(yearMonth)
    val daysInMonth = ym.lengthOfMonth()
    val firstDayOfWeek = ym.atDay(1).dayOfWeek.value % 7 // 0=Sun
    val dayLabels = listOf("日", "一", "二", "三", "四", "五", "六")

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("耕耘 · 工资计算") },
                actions = {
                    IconButton(onClick = onOpenShift) {
                        Icon(Icons.Default.Schedule, contentDescription = "班次")
                    }
                    IconButton(onClick = onOpenRates) {
                        Icon(Icons.Default.AttachMoney, contentDescription = "时薪")
                    }
                    IconButton(onClick = onOpenAdjust) {
                        Icon(Icons.Default.AddCircle, contentDescription = "增减项")
                    }
                    IconButton(onClick = onOpenSettings) {
                        Icon(Icons.Default.Settings, contentDescription = "设置")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 12.dp)
        ) {
            // 月份选择
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { vm.prevMonth() }) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "上月")
                }
                Text(
                    text = yearMonth,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                IconButton(onClick = { vm.nextMonth() }) {
                    Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "下月")
                }
            }

            Spacer(Modifier.height(8.dp))

            // 汇总卡片
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
            ) {
                Column(Modifier.padding(16.dp)) {
                    Text(
                        "本月预计到手",
                        style = MaterialTheme.typography.labelMedium
                    )
                    Text(
                        "¥ %.2f".format(summary.total),
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(Modifier.height(8.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        SummaryItem("正班", "%.1fh".format(summary.regularHours), "¥%.0f".format(summary.regularPay))
                        SummaryItem("加班", "%.1fh".format(summary.otHours), "¥%.0f".format(summary.otPay))
                        SummaryItem("夜班津贴", "${summary.nightDays}天", "¥%.0f".format(summary.nightAllowanceTotal))
                    }
                    if (summary.adjustIncome > 0 || summary.adjustExpense > 0) {
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "增减项: +¥%.0f / -¥%.0f".format(summary.adjustIncome, summary.adjustExpense),
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }

            Spacer(Modifier.height(12.dp))

            // 日历表头
            Row(Modifier.fillMaxWidth()) {
                dayLabels.forEach { label ->
                    Text(
                        text = label,
                        modifier = Modifier.weight(1f),
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(Modifier.height(4.dp))

            // 日历格子
            val cells = mutableListOf<Int?>()
            repeat(firstDayOfWeek) { cells.add(null) }
            for (d in 1..daysInMonth) cells.add(d)

            LazyVerticalGrid(
                columns = GridCells.Fixed(7),
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                items(cells) { day ->
                    if (day == null) {
                        Box(Modifier.aspectRatio(1f))
                    } else {
                        val dateStr = ym.atDay(day).format(DateTimeFormatter.ISO_LOCAL_DATE)
                        val record = data.days[dateStr]
                        DayCell(
                            day = day,
                            record = record,
                            onClick = {
                                selectedDay = dateStr
                                showDayDialog = true
                            }
                        )
                    }
                }
            }

            // 图例
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                LegendDot(Color(0xFF4CAF50), "上班")
                LegendDot(Color(0xFFFF9800), "加班")
                LegendDot(Color(0xFF2196F3), "夜班")
                LegendDot(Color(0xFF9E9E9E), "休息")
            }
        }
    }

    if (showDayDialog && selectedDay != null) {
        DayEditDialog(
            date = selectedDay!!,
            current = data.days[selectedDay] ?: DayRecord(date = selectedDay!!),
            onDismiss = { showDayDialog = false },
            onSave = { day ->
                vm.updateDay(day)
                showDayDialog = false
            }
        )
    }
}

@Composable
private fun SummaryItem(label: String, value1: String, value2: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, style = MaterialTheme.typography.labelSmall)
        Text(value1, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
        Text(value2, style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
private fun DayCell(day: Int, record: DayRecord?, onClick: () -> Unit) {
    val bg = when {
        record == null || record.status == DayStatus.OFF -> Color(0xFFEEEEEE)
        record.status == DayStatus.OVERTIME -> Color(0xFFFFE0B2)
        record.shiftType == ShiftType.NIGHT -> Color(0xFFBBDEFB)
        else -> Color(0xFFC8E6C9)
    }
    val borderColor = when {
        record?.status == DayStatus.OVERTIME -> Color(0xFFFF9800)
        record?.shiftType == ShiftType.NIGHT -> Color(0xFF2196F3)
        record?.status == DayStatus.WORK -> Color(0xFF4CAF50)
        else -> Color.Transparent
    }

    Box(
        modifier = Modifier
            .aspectRatio(1f)
            .clip(RoundedCornerShape(8.dp))
            .background(bg)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "$day",
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium
            )
            if (record != null && record.status != DayStatus.OFF) {
                Text(
                    text = when {
                        record.status == DayStatus.OVERTIME -> "加"
                        record.shiftType == ShiftType.NIGHT -> "夜"
                        else -> "班"
                    },
                    fontSize = 10.sp,
                    color = borderColor
                )
            }
        }
    }
}

@Composable
private fun LegendDot(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            Modifier
                .size(10.dp)
                .clip(CircleShape)
                .background(color)
        )
        Spacer(Modifier.width(4.dp))
        Text(label, style = MaterialTheme.typography.labelSmall)
    }
}

@Composable
fun DayEditDialog(
    date: String,
    current: DayRecord,
    onDismiss: () -> Unit,
    onSave: (DayRecord) -> Unit
) {
    var status by remember { mutableStateOf(current.status) }
    var shiftType by remember { mutableStateOf(current.shiftType) }
    var isOvertime by remember { mutableStateOf(current.isOvertime || current.status == DayStatus.OVERTIME) }
    var isHoliday by remember { mutableStateOf(current.isHoliday) }
    var otHours by remember { mutableStateOf(if (current.otHours > 0) current.otHours.toString() else "") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(date) },
        text = {
            Column {
                Text("出勤状态", style = MaterialTheme.typography.labelMedium)
                Row {
                    FilterChip(
                        selected = status == DayStatus.OFF,
                        onClick = { status = DayStatus.OFF },
                        label = { Text("不上班") }
                    )
                    Spacer(Modifier.width(8.dp))
                    FilterChip(
                        selected = status == DayStatus.WORK,
                        onClick = { status = DayStatus.WORK; isOvertime = false },
                        label = { Text("上班") }
                    )
                    Spacer(Modifier.width(8.dp))
                    FilterChip(
                        selected = status == DayStatus.OVERTIME,
                        onClick = { status = DayStatus.OVERTIME; isOvertime = true },
                        label = { Text("加班") }
                    )
                }

                if (status != DayStatus.OFF) {
                    Spacer(Modifier.height(12.dp))
                    Text("班次", style = MaterialTheme.typography.labelMedium)
                    Row {
                        FilterChip(
                            selected = shiftType == ShiftType.DAY,
                            onClick = { shiftType = ShiftType.DAY },
                            label = { Text("白班") }
                        )
                        Spacer(Modifier.width(8.dp))
                        FilterChip(
                            selected = shiftType == ShiftType.NIGHT,
                            onClick = { shiftType = ShiftType.NIGHT },
                            label = { Text("夜班") }
                        )
                    }

                    Spacer(Modifier.height(12.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = isHoliday, onCheckedChange = { isHoliday = it })
                        Text("节假日")
                    }

                    if (status == DayStatus.OVERTIME) {
                        Spacer(Modifier.height(8.dp))
                        OutlinedTextField(
                            value = otHours,
                            onValueChange = { otHours = it },
                            label = { Text("加班小时（可选，默认按班次时长）") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val finalStatus = when {
                    status == DayStatus.OFF -> DayStatus.OFF
                    isOvertime || status == DayStatus.OVERTIME -> DayStatus.OVERTIME
                    else -> DayStatus.WORK
                }
                onSave(
                    DayRecord(
                        date = date,
                        status = finalStatus,
                        shiftType = shiftType,
                        isOvertime = finalStatus == DayStatus.OVERTIME,
                        otHours = otHours.toDoubleOrNull() ?: 0.0,
                        isHoliday = isHoliday
                    )
                )
            }) { Text("保存") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消") }
        }
    )
}
