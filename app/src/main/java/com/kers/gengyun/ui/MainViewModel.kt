package com.kers.gengyun.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.kers.gengyun.data.WageRepository
import com.kers.gengyun.domain.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.YearMonth
import java.time.format.DateTimeFormatter

class MainViewModel(app: Application) : AndroidViewModel(app) {

    private val repo = WageRepository(app)

    private val _yearMonth = MutableStateFlow(
        YearMonth.now().format(DateTimeFormatter.ofPattern("yyyy-MM"))
    )
    val yearMonth: StateFlow<String> = _yearMonth.asStateFlow()

    val monthData: StateFlow<MonthData> = _yearMonth
        .flatMapLatest { repo.observeMonth(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), MonthData(yearMonth = _yearMonth.value))

    val summary: StateFlow<WageSummary> = monthData
        .map { repo.calculate(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), WageSummary())

    fun setYearMonth(ym: String) {
        _yearMonth.value = ym
    }

    fun prevMonth() {
        val ym = YearMonth.parse(_yearMonth.value)
        _yearMonth.value = ym.minusMonths(1).toString()
    }

    fun nextMonth() {
        val ym = YearMonth.parse(_yearMonth.value)
        _yearMonth.value = ym.plusMonths(1).toString()
    }

    fun updateDay(day: DayRecord) {
        viewModelScope.launch {
            repo.updateDay(_yearMonth.value, day)
        }
    }

    fun updateShiftConfig(config: ShiftConfig) {
        viewModelScope.launch {
            repo.updateShiftConfig(_yearMonth.value, config)
        }
    }

    fun updateWageRates(rates: WageRates) {
        viewModelScope.launch {
            repo.updateWageRates(_yearMonth.value, rates)
        }
    }

    fun updateNightAllowance(config: NightAllowanceConfig) {
        viewModelScope.launch {
            repo.updateNightAllowance(_yearMonth.value, config)
        }
    }

    fun addAdjustItem(name: String, amount: Double, isIncome: Boolean) {
        viewModelScope.launch {
            repo.addAdjustItem(_yearMonth.value, name, amount, isIncome)
        }
    }

    fun removeAdjustItem(id: String) {
        viewModelScope.launch {
            repo.removeAdjustItem(_yearMonth.value, id)
        }
    }
}
