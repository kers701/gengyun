package com.kers.gengyun.domain

import kotlinx.serialization.Serializable

/** 班次类型 */
enum class ShiftType {
    DAY,   // 白班
    NIGHT  // 夜班
}

/** 某天的出勤状态 */
enum class DayStatus {
    OFF,           // 不上班
    WORK,          // 上班（正班）
    OVERTIME,      // 加班
    WORK_AND_OT    // 正班+加班（预留）
}

@Serializable
data class TimeRange(
    val startHour: Int = 8,
    val startMinute: Int = 0,
    val endHour: Int = 17,
    val endMinute: Int = 0
) {
    fun durationHours(): Double {
        val start = startHour + startMinute / 60.0
        var end = endHour + endMinute / 60.0
        if (end <= start) end += 24 // 跨天
        return end - start
    }

    fun display(): String {
        return "%02d:%02d - %02d:%02d".format(startHour, startMinute, endHour, endMinute)
    }
}

@Serializable
data class ShiftConfig(
    val dayShift: TimeRange = TimeRange(8, 0, 17, 0),
    val nightShift: TimeRange = TimeRange(20, 0, 8, 0)
)

/** 时薪配置 */
@Serializable
data class WageRates(
    val regularHourly: Double = 20.0,          // 正班每小时
    val weekdayOtHourly: Double = 30.0,        // 工作日加班
    val weekendOtHourly: Double = 40.0,        // 周末加班
    val holidayOtHourly: Double = 60.0         // 节假日加班
)

/** 增减项目（收入/支出） */
@Serializable
data class AdjustItem(
    val id: String,
    val name: String,
    val amount: Double,        // 正数收入，负数支出
    val isIncome: Boolean = true
)

/** 某月的夜班津贴配置 */
@Serializable
data class NightAllowanceConfig(
    val perNight: Double = 10.0   // 每天夜班津贴
)

/** 某天记录 */
@Serializable
data class DayRecord(
    val date: String,                 // yyyy-MM-dd
    val status: DayStatus = DayStatus.OFF,
    val shiftType: ShiftType = ShiftType.DAY,
    val isOvertime: Boolean = false,  // 是否加班
    val otHours: Double = 0.0,        // 加班小时（可选自定义）
    val isHoliday: Boolean = false    // 是否节假日
)

/** 某月完整数据 */
@Serializable
data class MonthData(
    val yearMonth: String,            // yyyy-MM
    val shiftConfig: ShiftConfig = ShiftConfig(),
    val wageRates: WageRates = WageRates(),
    val nightAllowance: NightAllowanceConfig = NightAllowanceConfig(),
    val adjustItems: List<AdjustItem> = emptyList(),
    val days: Map<String, DayRecord> = emptyMap()  // key = yyyy-MM-dd
)

/** 计算结果 */
data class WageSummary(
    val regularHours: Double = 0.0,
    val otHours: Double = 0.0,
    val regularPay: Double = 0.0,
    val otPay: Double = 0.0,
    val nightAllowanceTotal: Double = 0.0,
    val adjustIncome: Double = 0.0,
    val adjustExpense: Double = 0.0,
    val total: Double = 0.0,
    val workDays: Int = 0,
    val nightDays: Int = 0
)
