package com.example.lol.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.example.lol.data.SubjectRepository
import com.example.lol.ui.screens.HomeScreen
import com.example.lol.ui.screens.SubjectsScreen

@Composable
fun AppNavHost(navController: NavHostController, repository: SubjectRepository) {
    NavHost(navController, startDestination = BottomNavItem.Home.route) {
        composable(BottomNavItem.Home.route) { HomeScreen() }
        composable(BottomNavItem.Subjects.route) { SubjectsScreen(repository = repository) }
    }
}
