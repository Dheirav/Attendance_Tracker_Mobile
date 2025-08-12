package com.example.attendance_tracker

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.Scaffold
import androidx.compose.ui.Modifier
import com.example.attendance_tracker.ui.navigation.AppNavHost
import com.example.attendance_tracker.ui.navigation.BottomNavItem
import com.example.attendance_tracker.ui.theme.LOLTheme
import androidx.navigation.compose.rememberNavController
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.room.Room
import com.example.attendance_tracker.data.AppDatabase
import com.example.attendance_tracker.data.SubjectRepository
import com.example.attendance_tracker.repository.TimetableRepository
import com.example.attendance_tracker.viewmodel.TimetableViewModel
import com.example.attendance_tracker.viewmodel.TimetableViewModelFactory
import androidx.compose.runtime.remember

@OptIn(ExperimentalMaterial3Api::class)
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            LOLTheme {
                val navController = rememberNavController()
                val context = this
                val db = remember {
                    Room.databaseBuilder(
                        context.applicationContext,
                        AppDatabase::class.java,
                        "attendance_db"
                    )
                    .fallbackToDestructiveMigration()
                    .build()
                }
                val repository = remember { SubjectRepository(db.subjectDao()) }
                val timetableRepository = remember { TimetableRepository(db.timetableDao()) }
                val timetableViewModel: TimetableViewModel = androidx.lifecycle.viewmodel.compose.viewModel(
                    factory = TimetableViewModelFactory(timetableRepository)
                )
                val items = listOf(
                    BottomNavItem.Home,
                    BottomNavItem.Subjects,
                    BottomNavItem.Timetable,
                    BottomNavItem.CommonSlots
                )
                val commonSlotViewModel: com.example.attendance_tracker.viewmodel.CommonSlotViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
                // Always call insertDefaultSlotsIfEmpty; it only inserts if table is empty
                commonSlotViewModel.insertDefaultSlotsIfEmpty()
                val attendanceRepository = remember { com.example.attendance_tracker.repository.AttendanceRepository(db.attendanceDao(), db.subjectDao(), repository) }
                val attendanceViewModel = remember { com.example.attendance_tracker.viewmodel.AttendanceViewModel(attendanceRepository) }
                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    bottomBar = {
                        NavigationBar {
                            val currentRoute = navController.currentBackStackEntry?.destination?.route
                            items.forEach { item ->
                                NavigationBarItem(
                                    icon = { Icon(item.icon, contentDescription = item.label) },
                                    label = { Text(item.label) },
                                    selected = currentRoute == item.route,
                                    onClick = {
                                        if (currentRoute != item.route) {
                                            navController.navigate(item.route) {
                                                popUpTo(navController.graph.startDestinationId) { saveState = true }
                                                launchSingleTop = true
                                                restoreState = true
                                            }
                                        }
                                    }
                                )
                            }
                        }
                    }
                ) { innerPadding ->
                    AppNavHost(
                        navController = navController,
                        repository = repository,
                        timetableViewModel = timetableViewModel,
                        commonSlotViewModel = commonSlotViewModel,
                        attendanceViewModel = attendanceViewModel,
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }
}
