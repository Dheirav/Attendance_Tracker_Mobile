package com.example.lol.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.example.lol.data.SubjectRepository
import com.example.lol.ui.screens.HomeScreen
import com.example.lol.viewmodel.TimetableViewModel
import com.example.lol.ui.screens.SubjectsScreen
import com.example.lol.ui.screens.TimetableScreen
import androidx.compose.ui.Modifier

@Composable
fun AppNavHost(
    navController: NavHostController,
    repository: SubjectRepository,
    timetableViewModel: TimetableViewModel,
    modifier: Modifier = Modifier
) {
    NavHost(
        navController = navController,
        startDestination = BottomNavItem.Home.route,
        modifier = modifier
    ) {
        composable(BottomNavItem.Home.route) {
            HomeScreen(
                timetableViewModel = timetableViewModel,
                subjectRepository = repository
            )
        }
        composable(BottomNavItem.Subjects.route) {
            SubjectsScreen(repository = repository)
        }
        composable(BottomNavItem.Timetable.route) {
            TimetableScreen(
                timetableViewModel = timetableViewModel,
                subjectRepository = repository
            )
        }
    }
}
