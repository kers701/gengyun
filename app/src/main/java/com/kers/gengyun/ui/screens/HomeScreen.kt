package com.kers.gengyun.ui.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.material.icons.filled.Close
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
import java.time.YearMonth
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
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
    val swapPick by vm.swapPick.collectAsState()

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
            // 月份选择（调班跨月：可先长按选一天，再切月点另一天）
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

            // 调班提示条
            if (swapPick != null) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF3E0))
                ) {
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "调班中：已选 $swapPick，请点击另一天完成调班（可切换月份）",
                            modifier = Modifier.weight(1f),
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFFE65100)
                        )
                        IconButton(onClick = { vm.cancelSwapPick() }) {
                            Icon(Icons.Default.Close, contentDescription = "取消调班")
                        }
                    }
                }
            }

            Spacer(Modifier.height(4.dp))

            // 汇总卡片
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
            ) {
                Column(Modifier.padding(16.dp)) {
                    Text("本月预计到手", style = MaterialTheme.typography.labelMedium)
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
                            isSwapSource = swapPick == dateStr,
                            isSwapped = record?.swappedWith != null,
                            onClick = {
                                if (vm.onDayClickForSwap(dateStr)) {
                                    // 已处理调班
                                } else {
                                    selectedDay = dateStr
                                    showDayDialog = true
                                }
                            },
                            onLongClick = { vm.onDayLongPress(dateStr) }
                        )
                    }
                }
            }

            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                LegendDot(Color(0xFF4CAF50), "上班")
                LegendDot(Color(0xFFFF9800), "加班")
                LegendDot(Color(0xFF2196F3), "夜班")
                LegendDot(Color(0xFF9C27B0), "调班")
                LegendDot(Color(0xFF9E9E9E), "休息")
            }
            Text(
                "提示：长按日期开始调班，再点另一天（可跨月）完成；只交换正班时段计费。",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 4.dp)
            )
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
            },
            onClearSwap = {
                vm.clearSwap(selectedDay!!)
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

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun DayCell(
    day: Int,
    record: DayRecord?,
    isSwapSource: Boolean,
    isSwapped: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    val bg = when {
        isSwapSource -> Color(0xFFFFE0B2)
        record == null || record.status == DayStatus.OFF -> Color(0xFFEEEEEE)
        record.status == DayStatus.OVERTIME || record.status == DayStatus.WORK_AND_OT -> Color(0xFFFFE0B2)
        record.shiftType == ShiftType.NIGHT -> Color(0xFFBBDEFB)
        else -> Color(0xFFC8E6C9)
    }
    val borderColor = when {
        isSwapSource -> Color(0xFFE65100)
        isSwapped -> Color(0xFF9C27B0)
        record?.status == DayStatus.OVERTIME || record?.status == DayStatus.WORK_AND_OT -> Color(0xFFFF9800)
        record?.shiftType == ShiftType.NIGHT -> Color(0xFF2196F3)
        record?.status == DayStatus.WORK -> Color(0xFF4CAF50)
        else -> Color.Transparent
    }

    Box(
        modifier = Modifier
            .aspectRatio(1f)
            .clip(RoundedCornerShape(8.dp))
            .background(bg)
            .then(
                if (borderColor != Color.Transparent)
                    Modifier.border(2.dp, borderColor, RoundedCornerShape(8.dp))
                else Modifier
            )
            .combinedClickable(onClick = onClick, onLongClick = onLongClick),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(text = "$day", fontSize = 14.sp, fontWeight = FontWeight.Medium)
            if (record != null && record.status != DayStatus.OFF) {
                Text(
                    text = when {
                        isSwapped -> "调"
                        record.status == DayStatus.OVERTIME || record.status == DayStatus.WORK_AND_OT -> "加"
                        record.shiftType == ShiftType.NIGHT -> "夜"
                        else -> "班"
                    },
                    fontSize = 10.sp,
                    color = borderColor
                )
            } else if (isSwapped) {
                Text("调", fontSize = 10.sp, color = Color(0xFF9C27B0))
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
    onSave: (DayRecord) -> Unit,
    onClearSwap: () -> Unit = {}
) {
    var status by remember { mutableStateOf(current.status) }
    var shiftType by remember { mutableStateOf(current.shiftType) }
    var isOvertime by remember {
        mutableStateOf(
            current.isOvertime ||
                current.status == DayStatus.OVERTIME ||
                current.status == DayStatus.WORK_AND_OT
        )
    }
    var isHoliday by remember { mutableStateOf(current.isHoliday) }
    var otHours by remember { mutableStateOf(if (current.otHours > 0) current.otHours.toString() else "") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column {
                Text(date)
                if (current.swappedWith != null) {
                    Text(
                        "已与 ${current.swappedWith} 调班（正班按对方类型计费）",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF9C27B0)
                    )
                }
            }
        },
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
                        selected = status == DayStatus.WORK && !isOvertime,
                        onClick = { status = DayStatus.WORK; isOvertime = false },
                        label = { Text("上班") }
                    )
                    Spacer(Modifier.width(8.dp))
                    FilterChip(
                        selected = isOvertime || status == DayStatus.OVERTIME,
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
                        Text("节假日（按自然日）")
                    }

                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = otHours,
                        onValueChange = { otHours = it },
                        label = { Text("正班之外的加班小时（可选）") },
                        supportingText = {
                            Text("工作日：正班×正班价 + 加班×平时加班价；周末全天加班价。调班只换正班时段类型。")
                        },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                if (current.swappedWith != null) {
                    Spacer(Modifier.height(12.dp))
                    TextButton(onClick = onClearSwap) {
                        Text("取消与此日的调班", color = Color(0xFF9C27B0))
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val hasOt = otHours.toDoubleOrNull()?.let { it > 0 } == true || isOvertime
                val finalStatus = when {
                    status == DayStatus.OFF -> DayStatus.OFF
                    hasOt -> DayStatus.OVERTIME
                    else -> DayStatus.WORK
                }
                onSave(
                    DayRecord(
                        date = date,
                        status = finalStatus,
                        shiftType = shiftType,
                        isOvertime = finalStatus == DayStatus.OVERTIME,
                        otHours = otHours.toDoubleOrNull() ?: 0.0,
                        isHoliday = isHoliday,
                        swappedWith = current.swappedWith,
                        swapBaseKind = current.swapBaseKind
                    )
                )
            }) { Text("保存") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消") }
        }
    )
}
