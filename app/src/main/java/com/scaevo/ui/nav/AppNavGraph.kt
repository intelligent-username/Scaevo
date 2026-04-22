package com.scaevo.ui.nav

import androidx.compose.runtime.Composable
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
import androidx.navigation.NavType
import androidx.navigation.navArgument

@Composable
fun AppNavGraph() {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = "permissions") {
        composable("permissions") {
            val helper = hiltViewModel<PermissionsViewModel>().helper
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
                onNavigateToAppDetail = { pkg -> navController.navigate("app_detail/$pkg") }
            )
        }
        composable("report") {
            ReportScreen(
                viewModel = hiltViewModel(),
                onNavigateToAppDetail = { pkg -> navController.navigate("app_detail/$pkg") }
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
    }
}
