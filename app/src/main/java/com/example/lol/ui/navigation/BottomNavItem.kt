package com.example.lol.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.ui.graphics.vector.ImageVector

sealed class BottomNavItem(val route: String, val label: String, val icon: ImageVector) {
    object Home : BottomNavItem("home", "Home", Icons.Filled.Home)
    object Subjects : BottomNavItem("subjects", "Subjects", Icons.Filled.List)
    object Timetable : BottomNavItem("timetable", "Timetable", Icons.Filled.Schedule)
}
