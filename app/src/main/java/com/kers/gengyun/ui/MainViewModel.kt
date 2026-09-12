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

    /** 调班：已长按选中的第一天 yyyy-MM-dd，null 表示未在调班模式 */
    private val _swapPick = MutableStateFlow<String?>(null)
    val swapPick: StateFlow<String?> = _swapPick.asStateFlow()

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

    fun updateMonthShiftType(type: ShiftType, applyToMarkedDays: Boolean = true) {
        viewModelScope.launch {
            repo.updateMonthShiftType(_yearMonth.value, type, applyToMarkedDays)
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

    /** 长按某日：进入调班选择，或取消当前选择 */
    fun onDayLongPress(date: String) {
        if (_swapPick.value == date) {
            _swapPick.value = null
        } else {
            _swapPick.value = date
        }
    }

    /** 点击某日：若在调班模式则完成调班；否则由 UI 打开编辑 */
    fun onDayClickForSwap(date: String): Boolean {
        val pick = _swapPick.value ?: return false
        if (pick == date) {
            _swapPick.value = null
            return true
        }
        viewModelScope.launch {
            repo.swapDays(pick, date)
            _swapPick.value = null
        }
        return true
    }

    fun cancelSwapPick() {
        _swapPick.value = null
    }

    fun clearSwap(date: String) {
        viewModelScope.launch {
            repo.clearSwap(date)
        }
    }
}
