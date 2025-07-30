package com.example.lol.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.ui.graphics.vector.ImageVector

sealed class BottomNavItem(val route: String, val label: String, val icon: ImageVector) {
    object Home : BottomNavItem("home", "Home", Icons.Filled.Home)                          
    object Subjects : BottomNavItem("subjects", "Subjects", Icons.Filled.School)            
    object Timetable : BottomNavItem("timetable", "Timetable", Icons.Filled.CalendarMonth)  
    object CommonSlots : BottomNavItem("common_slots", "Common Slots", Icons.Filled.AccessTime) 
}
