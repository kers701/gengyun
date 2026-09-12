package com.kers.gengyun.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.kers.gengyun.domain.*
import kotlinx.coroutines.flow.Flow
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

    private fun monthKey(yearMonth: String) = stringPreferencesKey("month_$yearMonth")

    fun observeMonth(yearMonth: String): Flow<MonthData> {
        val key = monthKey(yearMonth)
        return context.dataStore.data.map { prefs ->
            val raw = prefs[key]
            if (raw.isNullOrBlank()) {
                MonthData(yearMonth = yearMonth)
            } else {
                try {
                    json.decodeFromString<MonthData>(raw)
                } catch (e: Exception) {
                    MonthData(yearMonth = yearMonth)
                }
            }
        }
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
            val current = prefs[key]?.let {
                try { json.decodeFromString<MonthData>(it) } catch (_: Exception) { null }
            } ?: MonthData(yearMonth = yearMonth)
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

    private suspend fun mutate(yearMonth: String, block: (MonthData) -> MonthData) {
        context.dataStore.edit { prefs ->
            val key = monthKey(yearMonth)
            val current = prefs[key]?.let {
                try { json.decodeFromString<MonthData>(it) } catch (_: Exception) { null }
            } ?: MonthData(yearMonth = yearMonth)
            prefs[key] = json.encodeToString(block(current))
        }
    }

    fun calculate(data: MonthData): WageSummary {
        val rates = data.wageRates
        val shift = data.shiftConfig
        var regularHours = 0.0
        var otHours = 0.0
        var regularPay = 0.0
        var otPay = 0.0
        var nightDays = 0
        var workDays = 0

        val ym = YearMonth.parse(data.yearMonth)
        val daysInMonth = ym.lengthOfMonth()

        for (d in 1..daysInMonth) {
            val date = ym.atDay(d)
            val key = date.format(DateTimeFormatter.ISO_LOCAL_DATE)
            val record = data.days[key] ?: continue
            if (record.status == DayStatus.OFF) continue

            workDays++
            val isWeekend = date.dayOfWeek == DayOfWeek.SATURDAY || date.dayOfWeek == DayOfWeek.SUNDAY
            val isHoliday = record.isHoliday

            val baseHours = when (record.shiftType) {
                ShiftType.DAY -> shift.dayShift.durationHours()
                ShiftType.NIGHT -> {
                    nightDays++
                    shift.nightShift.durationHours()
                }
            }

            when (record.status) {
                DayStatus.WORK -> {
                    regularHours += baseHours
                    regularPay += baseHours * rates.regularHourly
                }
                DayStatus.OVERTIME -> {
                    val hours = if (record.otHours > 0) record.otHours else baseHours
                    otHours += hours
                    val rate = when {
                        isHoliday -> rates.holidayOtHourly
                        isWeekend -> rates.weekendOtHourly
                        else -> rates.weekdayOtHourly
                    }
                    otPay += hours * rate
                }
                DayStatus.WORK_AND_OT -> {
                    regularHours += baseHours
                    regularPay += baseHours * rates.regularHourly
                    val ot = if (record.otHours > 0) record.otHours else 0.0
                    otHours += ot
                    val rate = when {
                        isHoliday -> rates.holidayOtHourly
                        isWeekend -> rates.weekendOtHourly
                        else -> rates.weekdayOtHourly
                    }
                    otPay += ot * rate
                }
                else -> {}
            }
        }

        val nightAllowanceTotal = nightDays * data.nightAllowance.perNight
        val adjustIncome = data.adjustItems.filter { it.isIncome }.sumOf { it.amount }
        val adjustExpense = data.adjustItems.filter { !it.isIncome }.sumOf { kotlin.math.abs(it.amount) }

        val total = regularPay + otPay + nightAllowanceTotal + adjustIncome - adjustExpense

        return WageSummary(
            regularHours = regularHours,
            otHours = otHours,
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
