package com.example.lol.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.lol.data.*
import com.example.lol.ui.components.AddOrEditSubjectDialog
import com.example.lol.viewmodel.SubjectViewModel
import com.example.lol.viewmodel.SubjectViewModelFactory
import com.example.lol.viewmodel.AttendanceViewModel
import com.example.lol.viewmodel.AttendanceViewModelFactory
import com.example.lol.repository.AttendanceRepository
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SubjectsScreen(
    repository: SubjectRepository
) {
    val context = LocalContext.current
    val db = remember { AppDatabase.getInstance(context) }
    val attendanceRepository = remember { AttendanceRepository(db.attendanceDao()) }
    val attendanceViewModel: AttendanceViewModel = viewModel(factory = AttendanceViewModelFactory(attendanceRepository))
    val viewModel: SubjectViewModel = viewModel(factory = SubjectViewModelFactory(repository))
    val subjects by viewModel.subjects.collectAsState()
    var showDialog by remember { mutableStateOf(false) }
    var editingSubject by remember { mutableStateOf<Subject?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val coroutineScope = rememberCoroutineScope()
    val today = LocalDate.now().format(DateTimeFormatter.ofPattern("dd-MM-yyyy"))

    LaunchedEffect(errorMessage) {
        errorMessage?.let {
            snackbarHostState.showSnackbar(it)
            errorMessage = null
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Subjects") },
                navigationIcon = {
                    IconButton(onClick = { /* Handle back navigation */ }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = {
                editingSubject = null
                showDialog = true
            }) {
                Icon(Icons.Default.Add, contentDescription = "Add Subject")
            }
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            if (subjects.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No subjects yet.")
                }
            } else {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(subjects) { subject ->
                        SubjectCard(
                            subject = subject,
                            viewModel = viewModel,
                            attendanceViewModel = attendanceViewModel,
                            today = today,
                            onEdit = {
                                editingSubject = subject
                                showDialog = true
                            },
                            onDeleteSuccess = { msg -> errorMessage = msg }
                        )
                    }
                }
            }

            if (showDialog) {
                AddOrEditSubjectDialog(
                    initialSubject = editingSubject,
                    onAdd = { name, type, threshold ->
                        coroutineScope.launch {
                            val trimmedName = name.trim()
                            when {
                                trimmedName.isEmpty() -> errorMessage = "Name cannot be empty."
                                subjects.any { it.name.equals(trimmedName, ignoreCase = true) } -> errorMessage = "Subject with this name already exists."
                                else -> try {
                                    viewModel.addSubject(Subject(name = trimmedName, type = type, threshold = threshold))
                                    showDialog = false
                                    errorMessage = "Subject added"
                                } catch (e: Exception) {
                                    errorMessage = "Failed to add subject"
                                }
                            }
                        }
                    },
                    onUpdate = { updatedSubject ->
                        coroutineScope.launch {
                            val trimmedName = updatedSubject.name.trim()
                            if (trimmedName.isEmpty() ||
                                subjects.any { it.name.equals(trimmedName, ignoreCase = true) && it.id != updatedSubject.id }) {
                                errorMessage = "Another subject with this name already exists."
                            } else {
                                try {
                                    viewModel.updateSubject(updatedSubject.copy(name = trimmedName))
                                    showDialog = false
                                    errorMessage = "Subject updated"
                                } catch (e: Exception) {
                                    errorMessage = "Failed to update subject"
                                }
                            }
                        }
                    },
                    onDismiss = { showDialog = false }
                )
            }
        }
    }
}

@Composable
fun SubjectCard(
    subject: Subject,
    viewModel: SubjectViewModel,
    attendanceViewModel: AttendanceViewModel,
    today: String,
    onEdit: () -> Unit,
    onDeleteSuccess: (String) -> Unit
) {
    val attendanceHistory by attendanceViewModel.attendanceHistory.collectAsState()
    val attendancePercentage by attendanceViewModel.attendancePercentage.collectAsState()
    val localCoroutineScope = rememberCoroutineScope()

    LaunchedEffect(subject.id) {
        attendanceViewModel.loadAttendance(subject.id)
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp)
            .clickable { onEdit() },
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(subject.name, style = MaterialTheme.typography.titleMedium)
                    Text(subject.type, style = MaterialTheme.typography.bodyMedium)
                    Text("Threshold: ${subject.threshold}%", style = MaterialTheme.typography.bodySmall)
                }
                IconButton(onClick = {
                    localCoroutineScope.launch {
                        viewModel.deleteSubject(subject)
                        onDeleteSuccess("Subject deleted")
                    }
                }) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete")
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Mark today:", modifier = Modifier.padding(end = 8.dp))
                Button(onClick = {
                    localCoroutineScope.launch {
                        attendanceViewModel.markAttendance(subject.id, today, AttendanceStatus.PRESENT)
                        attendanceViewModel.loadAttendance(subject.id)
                    }
                    onDeleteSuccess("Marked present")
                }) { Text("Present") }
                Spacer(modifier = Modifier.width(8.dp))
                Button(onClick = {
                    localCoroutineScope.launch {
                        attendanceViewModel.markAttendance(subject.id, today, AttendanceStatus.ABSENT)
                        attendanceViewModel.loadAttendance(subject.id)
                    }
                    onDeleteSuccess("Marked absent")
                }) { Text("Absent") }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text("Attendance: ${"%.1f".format(attendancePercentage)}%", style = MaterialTheme.typography.bodyMedium)
            Spacer(modifier = Modifier.height(8.dp))
            Text("History:", style = MaterialTheme.typography.titleSmall)
            if (attendanceHistory.isEmpty()) {
                Text("No attendance records yet.", style = MaterialTheme.typography.bodySmall)
            } else {
                attendanceHistory.take(5).forEach { record ->
                    Text("${record.date}: ${record.status}", style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}
