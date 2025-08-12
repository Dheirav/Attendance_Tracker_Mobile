package com.example.attendance_tracker.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.example.attendance_tracker.data.SubjectRepository
import com.example.attendance_tracker.ui.screens.HomeScreen
import com.example.attendance_tracker.viewmodel.TimetableViewModel
import com.example.attendance_tracker.ui.screens.SubjectsScreen
import com.example.attendance_tracker.ui.screens.TimetableScreen
import androidx.compose.ui.Modifier
import com.example.attendance_tracker.viewmodel.CommonSlotViewModel
import com.example.attendance_tracker.ui.screens.CommonSlotsManagerScreen

@Composable
fun AppNavHost(
    navController: NavHostController,
    repository: SubjectRepository,
    timetableViewModel: TimetableViewModel,
    commonSlotViewModel: CommonSlotViewModel,
    attendanceViewModel: com.example.attendance_tracker.viewmodel.AttendanceViewModel,
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
                subjectRepository = repository,
                commonSlotViewModel = commonSlotViewModel,
                attendanceViewModel = attendanceViewModel 
            )
        }
        composable(BottomNavItem.Subjects.route) {
            SubjectsScreen(repository = repository, attendanceViewModel = attendanceViewModel)
        }
        composable(BottomNavItem.Timetable.route) {
            TimetableScreen(
                timetableViewModel = timetableViewModel,
                subjectRepository = repository
            )
        }
        composable(BottomNavItem.CommonSlots.route) {
            CommonSlotsManagerScreen(commonSlotViewModel = commonSlotViewModel)
        }
    }
}
