package com.scaevo.ui.nav

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.scaevo.ui.blocklist.BlocklistScreen
import com.scaevo.ui.dashboard.DashboardScreen
import com.scaevo.ui.dashboard.DashboardViewModel
import com.scaevo.ui.permissions.PermissionsScreen
import com.scaevo.ui.permissions.PermissionsViewModel
import com.scaevo.ui.report.ReportScreen
import com.scaevo.ui.appdetail.AppDetailScreen
import com.scaevo.ui.settings.SettingsScreen
import com.scaevo.ui.daysummary.DaySummaryScreen
import androidx.navigation.NavType
import androidx.navigation.navArgument

@Composable
fun AppNavGraph() {
    val navController = rememberNavController()
    val helper = hiltViewModel<PermissionsViewModel>().helper
    val context = androidx.compose.ui.platform.LocalContext.current
    
    val startDest = remember {
        if (helper.hasUsagePermission() && com.scaevo.ui.utils.isAccessibilityServiceEnabled(context)) {
            "dashboard"
        } else {
            "permissions"
        }
    }

    NavHost(navController = navController, startDestination = startDest) {
        composable("permissions") {
            PermissionsScreen(
                onAllGranted = {
                    navController.navigate("dashboard") {
                        popUpTo("permissions") { inclusive = true }
                    }
                },
                usageStatsHelper = helper
            )
        }
        composable("dashboard") {
            val vm: DashboardViewModel = hiltViewModel()
            DashboardScreen(
                viewModel = vm,
                onNavigateToReport = { navController.navigate("report") },
                onNavigateToBlocklist = { navController.navigate("blocklist") },
                onNavigateToAppDetail = { pkg -> navController.navigate("app_detail/$pkg") },
                onNavigateToSettings = { navController.navigate("settings") }
            )
        }
        composable("report") {
            ReportScreen(
                viewModel = hiltViewModel(),
                onNavigateToAppDetail = { pkg -> navController.navigate("app_detail/$pkg") },
                onNavigateToDaySummary = { day -> navController.navigate("day_summary/$day") },
                onNavigateToSettings = { navController.navigate("settings") },
            )
        }
        composable(
            "day_summary/{epochDay}",
            arguments = listOf(navArgument("epochDay") { type = NavType.LongType })
        ) {
            val epochDay = it.arguments?.getLong("epochDay") ?: 0L
            DaySummaryScreen(
                viewModel = hiltViewModel(),
                epochDay = epochDay,
                onNavigateBack = { navController.popBackStack() },
            )
        }
        composable(
            "app_detail/{packageName}",
            arguments = listOf(navArgument("packageName") { type = NavType.StringType })
        ) {
            AppDetailScreen(
                viewModel = hiltViewModel(),
                onNavigateBack = { navController.popBackStack() }
            )
        }
        composable("blocklist") {
            BlocklistScreen(viewModel = hiltViewModel())
        }

        composable("settings") {
            SettingsScreen(
                viewModel = hiltViewModel(),
                onNavigateBack = { navController.popBackStack() },
            )
        }
    }
}
