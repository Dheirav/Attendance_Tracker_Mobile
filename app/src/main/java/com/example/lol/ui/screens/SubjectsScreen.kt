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
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SubjectsScreen(
    repository: SubjectRepository,
    attendanceViewModel: AttendanceViewModel
) {
    val context = LocalContext.current
    val db = remember { AppDatabase.getInstance(context) }
    val viewModel: SubjectViewModel = viewModel(factory = SubjectViewModelFactory(repository))
    val subjects by viewModel.subjects.collectAsState()
    var showDialog by remember { mutableStateOf(false) }
    var showEditDialog by remember { mutableStateOf(false) }
    var editingSubject by remember { mutableStateOf<Subject?>(null) }
    var attendedClasses by remember { mutableStateOf(0) }
    var totalClasses by remember { mutableStateOf(0) }
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
                },
                actions = {
                    IconButton(onClick = {
                        editingSubject = null
                        showDialog = true
                    }) {
                        Icon(Icons.Default.Add, contentDescription = "Add Subject")
                    }
                }
            )
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
                            onDeleteSuccess = { msg -> errorMessage = msg },
                            onEditAttendance = {
                                editingSubject = subject
                                attendedClasses = subject.attendedClasses
                                totalClasses = subject.totalClasses
                                showEditDialog = true
                            }
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

            if (showEditDialog && editingSubject != null) {
                AlertDialog(
                    onDismissRequest = { showEditDialog = false },
                    title = { Text("Edit Attendance for ${editingSubject!!.name}") },
                    text = {
                        Column {
                            OutlinedTextField(
                                value = attendedClasses.toString(),
                                onValueChange = { attendedClasses = it.toIntOrNull() ?: 0 },
                                label = { Text("Attended Classes") }
                            )
                            OutlinedTextField(
                                value = totalClasses.toString(),
                                onValueChange = { totalClasses = it.toIntOrNull() ?: 0 },
                                label = { Text("Total Classes") }
                            )
                        }
                    },
                    confirmButton = {
                        Button(onClick = {
                            coroutineScope.launch {
                                attendanceViewModel.updateManualAttendance(
                                    editingSubject!!.id,
                                    attendedClasses,
                                    totalClasses,
                                    note = "Manual record updated",
                                    date = today
                                )
                                errorMessage = "Attendance record updated manually"
                            }
                            showEditDialog = false
                        }) {
                            Text("Save")
                        }
                    },
                    dismissButton = {
                        Button(onClick = { showEditDialog = false }) {
                            Text("Cancel")
                        }
                    }
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
    onDeleteSuccess: (String) -> Unit,
    onEditAttendance: () -> Unit
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
                    // Display attended and total classes
                    Text("Attended: ${subject.attendedClasses} / Total: ${subject.totalClasses}", style = MaterialTheme.typography.bodySmall)
                }
                var showDeleteDialog by remember { mutableStateOf(false) }
                IconButton(onClick = { showDeleteDialog = true }) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete")
                }
                if (showDeleteDialog) {
                    AlertDialog(
                        onDismissRequest = { showDeleteDialog = false },
                        title = { Text("Delete Subject") },
                        text = { Text("Are you sure you want to delete this subject? This action cannot be undone.") },
                        confirmButton = {
                            Button(onClick = {
                                showDeleteDialog = false
                                localCoroutineScope.launch {
                                    viewModel.deleteSubject(subject)
                                    onDeleteSuccess("Subject deleted")
                                }
                            }) { Text("Delete") }
                        },
                        dismissButton = {
                            Button(onClick = { showDeleteDialog = false }) { Text("Cancel") }
                        }
                    )
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
                    val statusText = if (record.note != null && record.note.contains("Manual record updated")) "Updated" else record.status.name
                    Text("${record.date}: $statusText", style = MaterialTheme.typography.bodySmall)
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Button(onClick = onEditAttendance) {
                Text("Edit Attendance")
            }
        }
    }
}
