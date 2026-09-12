package com.kers.gengyun.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.kers.gengyun.domain.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.UUID

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "gengyun_wage")

class WageRepository(private val context: Context) {

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    private val dateFmt = DateTimeFormatter.ISO_LOCAL_DATE

    private fun monthKey(yearMonth: String) = stringPreferencesKey("month_$yearMonth")

    fun observeMonth(yearMonth: String): Flow<MonthData> {
        val key = monthKey(yearMonth)
        return context.dataStore.data.map { prefs ->
            decodeMonth(prefs[key], yearMonth)
        }
    }

    private fun decodeMonth(raw: String?, yearMonth: String): MonthData {
        if (raw.isNullOrBlank()) return MonthData(yearMonth = yearMonth)
        return try {
            json.decodeFromString<MonthData>(raw)
        } catch (e: Exception) {
            MonthData(yearMonth = yearMonth)
        }
    }

    private suspend fun loadMonth(yearMonth: String): MonthData {
        val key = monthKey(yearMonth)
        val raw = context.dataStore.data.first()[key]
        return decodeMonth(raw, yearMonth)
    }

    suspend fun saveMonth(data: MonthData) {
        val key = monthKey(data.yearMonth)
        context.dataStore.edit { prefs ->
            prefs[key] = json.encodeToString(data)
        }
    }

    suspend fun updateDay(yearMonth: String, day: DayRecord) {
        context.dataStore.edit { prefs ->
            val key = monthKey(yearMonth)
            val current = decodeMonth(prefs[key], yearMonth)
            val newDays = current.days.toMutableMap().apply { put(day.date, day) }
            prefs[key] = json.encodeToString(current.copy(days = newDays))
        }
    }

    suspend fun updateShiftConfig(yearMonth: String, config: ShiftConfig) {
        mutate(yearMonth) { it.copy(shiftConfig = config) }
    }

    suspend fun updateWageRates(yearMonth: String, rates: WageRates) {
        mutate(yearMonth) { it.copy(wageRates = rates) }
    }

    suspend fun updateNightAllowance(yearMonth: String, config: NightAllowanceConfig) {
        mutate(yearMonth) { it.copy(nightAllowance = config) }
    }

    suspend fun addAdjustItem(yearMonth: String, name: String, amount: Double, isIncome: Boolean) {
        mutate(yearMonth) { data ->
            val item = AdjustItem(
                id = UUID.randomUUID().toString(),
                name = name,
                amount = if (isIncome) amount else -kotlin.math.abs(amount),
                isIncome = isIncome
            )
            data.copy(adjustItems = data.adjustItems + item)
        }
    }

    suspend fun removeAdjustItem(yearMonth: String, id: String) {
        mutate(yearMonth) { it.copy(adjustItems = it.adjustItems.filter { i -> i.id != id }) }
    }

    /**
     * 调班：交换两天的「正班时段计费类型」，支持跨月。
     * 只交换 8点-17点（正班）对应的计费属性；加班仍按各自自然日类型。
     * 若某天已有调班，会先解除旧关系再建立新关系。
     */
    suspend fun swapDays(dateA: String, dateB: String) {
        if (dateA == dateB) return
        val dA = LocalDate.parse(dateA)
        val dB = LocalDate.parse(dateB)
        val ymA = YearMonth.from(dA).toString()
        val ymB = YearMonth.from(dB).toString()

        context.dataStore.edit { prefs ->
            fun read(ym: String) = decodeMonth(prefs[monthKey(ym)], ym)
            fun write(data: MonthData) {
                prefs[monthKey(data.yearMonth)] = json.encodeToString(data)
            }

            var monthA = read(ymA)
            var monthB = if (ymA == ymB) monthA else read(ymB)

            fun getDay(month: MonthData, date: String): DayRecord =
                month.days[date] ?: DayRecord(date = date)

            // 解除 dateA / dateB 各自旧调班
            fun clearOldSwap(month: MonthData, date: String): MonthData {
                val rec = month.days[date] ?: return month
                val partner = rec.swappedWith ?: return month
                val partnerYm = YearMonth.from(LocalDate.parse(partner)).toString()
                var m = month
                // 清自己
                val cleared = rec.copy(swappedWith = null, swapBaseKind = null)
                m = m.copy(days = m.days.toMutableMap().apply { put(date, cleared) })
                // 清对方（可能跨月）
                if (partnerYm == m.yearMonth) {
                    val p = m.days[partner]
                    if (p != null && p.swappedWith == date) {
                        m = m.copy(days = m.days.toMutableMap().apply {
                            put(partner, p.copy(swappedWith = null, swapBaseKind = null))
                        })
                    }
                } else {
                    var pm = if (partnerYm == ymA) monthA else if (partnerYm == ymB) monthB else read(partnerYm)
                    val p = pm.days[partner]
                    if (p != null && p.swappedWith == date) {
                        pm = pm.copy(days = pm.days.toMutableMap().apply {
                            put(partner, p.copy(swappedWith = null, swapBaseKind = null))
                        })
                        if (partnerYm == ymA) monthA = pm
                        else if (partnerYm == ymB) monthB = pm
                        else write(pm)
                    }
                }
                return m
            }

            monthA = clearOldSwap(monthA, dateA)
            if (ymA == ymB) monthB = monthA
            monthB = clearOldSwap(monthB, dateB)
            if (ymA == ymB) monthA = monthB

            val recA = getDay(if (ymA == ymB) monthA else monthA, dateA)
            val recB = getDay(if (ymA == ymB) monthA else monthB, dateB)

            val kindA = naturalPayKind(dA, recA)
            val kindB = naturalPayKind(dB, recB)

            // 正班计费类型互换
            val newA = recA.copy(swappedWith = dateB, swapBaseKind = kindB.name)
            val newB = recB.copy(swappedWith = dateA, swapBaseKind = kindA.name)

            if (ymA == ymB) {
                val days = monthA.days.toMutableMap()
                days[dateA] = newA
                days[dateB] = newB
                write(monthA.copy(days = days))
            } else {
                write(monthA.copy(days = monthA.days.toMutableMap().apply { put(dateA, newA) }))
                write(monthB.copy(days = monthB.days.toMutableMap().apply { put(dateB, newB) }))
            }
        }
    }

    /** 取消某日调班（同时清除对方） */
    suspend fun clearSwap(date: String) {
        val d = LocalDate.parse(date)
        val ym = YearMonth.from(d).toString()
        context.dataStore.edit { prefs ->
            fun read(y: String) = decodeMonth(prefs[monthKey(y)], y)
            fun write(data: MonthData) {
                prefs[monthKey(data.yearMonth)] = json.encodeToString(data)
            }
            var month = read(ym)
            val rec = month.days[date] ?: return@edit
            val partner = rec.swappedWith
            month = month.copy(days = month.days.toMutableMap().apply {
                put(date, rec.copy(swappedWith = null, swapBaseKind = null))
            })
            write(month)
            if (partner != null) {
                val pYm = YearMonth.from(LocalDate.parse(partner)).toString()
                var pm = if (pYm == ym) month else read(pYm)
                val pRec = pm.days[partner]
                if (pRec != null && pRec.swappedWith == date) {
                    pm = pm.copy(days = pm.days.toMutableMap().apply {
                        put(partner, pRec.copy(swappedWith = null, swapBaseKind = null))
                    })
                    write(pm)
                }
            }
        }
    }

    private suspend fun mutate(yearMonth: String, block: (MonthData) -> MonthData) {
        context.dataStore.edit { prefs ->
            val key = monthKey(yearMonth)
            val current = decodeMonth(prefs[key], yearMonth)
            prefs[key] = json.encodeToString(block(current))
        }
    }

    /** 自然日计费类型（未调班时） */
    fun naturalPayKind(date: LocalDate, record: DayRecord): PayKind {
        if (record.isHoliday) return PayKind.HOLIDAY
        val dow = date.dayOfWeek
        if (dow == DayOfWeek.SATURDAY || dow == DayOfWeek.SUNDAY) return PayKind.WEEKEND
        return PayKind.WEEKDAY
    }

    private fun parsePayKind(raw: String?): PayKind? = when (raw) {
        "WEEKDAY" -> PayKind.WEEKDAY
        "WEEKEND" -> PayKind.WEEKEND
        "HOLIDAY" -> PayKind.HOLIDAY
        else -> null
    }

    private fun baseRate(kind: PayKind, rates: WageRates): Double = when (kind) {
        PayKind.WEEKDAY -> rates.regularHourly
        PayKind.WEEKEND -> rates.weekendOtHourly
        PayKind.HOLIDAY -> rates.holidayOtHourly
    }

    private fun otRate(kind: PayKind, rates: WageRates): Double = when (kind) {
        PayKind.WEEKDAY -> rates.weekdayOtHourly
        PayKind.WEEKEND -> rates.weekendOtHourly
        PayKind.HOLIDAY -> rates.holidayOtHourly
    }

    /**
     * 工资计算规则：
     * - 正班时段（班次时长）：按「正班计费类型」——未调班用自然日；调班后用对方的类型
     *   · WEEKDAY → 正班时薪
     *   · WEEKEND/HOLIDAY → 对应加班时薪（记入加班工资）
     * - 正班之外的加班小时：按「本自然日」类型的加班时薪
     *
     * 例：周六与周一调班
     * 周一：正班按周末价 + 后续加班按工作日加班价
     * 周六：正班按正班价 + 后续加班按周末加班价
     */
    fun calculate(data: MonthData): WageSummary {
        val rates = data.wageRates
        val shift = data.shiftConfig
        var regularHours = 0.0
        var otHoursAcc = 0.0
        var regularPay = 0.0
        var otPay = 0.0
        var nightDays = 0
        var workDays = 0

        val ym = YearMonth.parse(data.yearMonth)
        val daysInMonth = ym.lengthOfMonth()

        for (d in 1..daysInMonth) {
            val date = ym.atDay(d)
            val key = date.format(dateFmt)
            val record = data.days[key] ?: continue
            if (record.status == DayStatus.OFF) continue

            workDays++
            val baseHours = when (record.shiftType) {
                ShiftType.DAY -> shift.dayShift.durationHours()
                ShiftType.NIGHT -> {
                    nightDays++
                    shift.nightShift.durationHours()
                }
            }

            val naturalKind = naturalPayKind(date, record)
            // 正班时段：调班后用对方类型，否则用自然日
            val baseKind = parsePayKind(record.swapBaseKind) ?: naturalKind
            // 加班时段：始终按本自然日
            val extraOtKind = naturalKind

            val extraOt = when {
                record.status == DayStatus.OVERTIME || record.status == DayStatus.WORK_AND_OT || record.isOvertime ->
                    if (record.otHours > 0) record.otHours else 0.0
                else -> 0.0
            }

            // 正班时段
            when (baseKind) {
                PayKind.WEEKDAY -> {
                    regularHours += baseHours
                    regularPay += baseHours * rates.regularHourly
                }
                PayKind.WEEKEND, PayKind.HOLIDAY -> {
                    // 休息日性质的正班：整段按加班价，记入加班
                    otHoursAcc += baseHours
                    otPay += baseHours * baseRate(baseKind, rates)
                }
            }

            // 正班之外加班
            if (extraOt > 0) {
                otHoursAcc += extraOt
                otPay += extraOt * otRate(extraOtKind, rates)
            }
        }

        val nightAllowanceTotal = nightDays * data.nightAllowance.perNight
        val adjustIncome = data.adjustItems.filter { it.isIncome }.sumOf { it.amount }
        val adjustExpense = data.adjustItems.filter { !it.isIncome }.sumOf { kotlin.math.abs(it.amount) }
        val total = regularPay + otPay + nightAllowanceTotal + adjustIncome - adjustExpense

        return WageSummary(
            regularHours = regularHours,
            otHours = otHoursAcc,
            regularPay = regularPay,
            otPay = otPay,
            nightAllowanceTotal = nightAllowanceTotal,
            adjustIncome = adjustIncome,
            adjustExpense = adjustExpense,
            total = total,
            workDays = workDays,
            nightDays = nightDays
        )
    }
}
