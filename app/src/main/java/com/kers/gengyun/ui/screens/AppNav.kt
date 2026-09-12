package com.kers.gengyun.ui.screens

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.kers.gengyun.ui.MainViewModel

@Composable
fun AppNav(vm: MainViewModel) {
    val nav = rememberNavController()
    NavHost(navController = nav, startDestination = "home") {
        composable("home") {
            HomeScreen(
                vm = vm,
                onOpenSettings = { nav.navigate("settings") },
                onOpenRates = { nav.navigate("rates") },
                onOpenAdjust = { nav.navigate("adjust") },
                onOpenShift = { nav.navigate("shift") }
            )
        }
        composable("settings") {
            SettingsScreen(vm = vm, onBack = { nav.popBackStack() })
        }
        composable("rates") {
            RatesScreen(vm = vm, onBack = { nav.popBackStack() })
        }
        composable("adjust") {
            AdjustScreen(vm = vm, onBack = { nav.popBackStack() })
        }
        composable("shift") {
            ShiftConfigScreen(vm = vm, onBack = { nav.popBackStack() })
        }
    }
}
