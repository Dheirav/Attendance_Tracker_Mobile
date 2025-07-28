package com.example.lol.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.example.lol.data.SubjectRepository
import com.example.lol.ui.screens.HomeScreen
import com.example.lol.ui.screens.SubjectsScreen
import androidx.compose.ui.Modifier

@Composable
fun AppNavHost(
    navController: NavHostController,
    repository: SubjectRepository,
    modifier: Modifier = Modifier 
) {
    NavHost(
        navController = navController,
        startDestination = BottomNavItem.Home.route,
        modifier = modifier 
    ) {
        composable(BottomNavItem.Home.route) {
            HomeScreen()
        }
        composable(BottomNavItem.Subjects.route) {
            SubjectsScreen(repository = repository)
        }
    }
}
