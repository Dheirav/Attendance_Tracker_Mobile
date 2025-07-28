package com.example.lol

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
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.example.lol.ui.navigation.AppNavHost
import com.example.lol.ui.navigation.BottomNavItem
import com.example.lol.ui.theme.LOLTheme
import androidx.navigation.compose.rememberNavController
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Home
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.room.Room
import com.example.lol.data.AppDatabase
import com.example.lol.data.SubjectRepository
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
                val items = listOf(
                    BottomNavItem.Home,
                    BottomNavItem.Subjects
                )
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
                        modifier = Modifier.padding(innerPadding) 
                    )
                }
            }
        }
    }
}
