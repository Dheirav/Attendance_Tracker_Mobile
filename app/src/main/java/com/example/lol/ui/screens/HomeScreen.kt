package com.example.lol.ui.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import com.example.lol.viewmodel.TimetableViewModel
import com.example.lol.data.TimetableEntry
import com.example.lol.data.SubjectRepository
import com.example.lol.viewmodel.CommonSlotViewModel
import com.example.lol.viewmodel.AttendanceViewModel
import com.example.lol.data.AttendanceStatus
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale
import java.time.LocalTime
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    timetableViewModel: TimetableViewModel,
    subjectRepository: SubjectRepository,
    commonSlotViewModel: CommonSlotViewModel = androidx.lifecycle.viewmodel.compose.viewModel(),
    attendanceViewModel: AttendanceViewModel 
) {
    val today = LocalDate.now().dayOfWeek.getDisplayName(TextStyle.FULL, Locale.getDefault())
    val timetableEntries by timetableViewModel.getEntriesForDay(today).collectAsState()
    var showDialog by remember { mutableStateOf(false) }
    var editingEntry by remember { mutableStateOf<TimetableEntry?>(null) }
    var subject by remember { mutableStateOf("") }
    val subjects by subjectRepository.allSubjects.collectAsState(initial = emptyList())
    val slotEntities by commonSlotViewModel.slots.collectAsState()
    var selectedSlotLabel by remember { mutableStateOf("") }
    var commonSlotDropdownExpanded by remember { mutableStateOf(false) }
    var startTime by remember { mutableStateOf("") }
    var endTime by remember { mutableStateOf("") }
    val snackbarHostState = remember { SnackbarHostState() }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val coroutineScope = rememberCoroutineScope()
    val todayDate = LocalDate.now()
    val dayOfWeek = todayDate.dayOfWeek.getDisplayName(TextStyle.FULL, Locale.getDefault())
    val dateStr = todayDate.format(java.time.format.DateTimeFormatter.ofPattern("dd MMMM yyyy"))

    LaunchedEffect(errorMessage) {
        errorMessage?.let {
            snackbarHostState.showSnackbar(it)
            errorMessage = null
        }
    }

   Scaffold(
    topBar = {
        TopAppBar(
            title = { Text("Today - $dayOfWeek's Timetable") },
            actions = {
                IconButton(onClick = {
                    editingEntry = null
                    subject = ""
                    startTime = ""
                    endTime = ""
                    selectedSlotLabel = ""
                    showDialog = true
                }) {
                    Icon(Icons.Filled.Add, contentDescription = "Add Timetable Entry")
                }
            }
        )
    },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(text = dateStr, style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(16.dp))
            }
            if (timetableEntries.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No timetable entries for today.")
                }
            } else {
                Column(modifier = Modifier.fillMaxSize()) {
                    timetableEntries.forEach { entry ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(8.dp),
                            elevation = CardDefaults.cardElevation(2.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(entry.subject, style = MaterialTheme.typography.titleMedium)
                                    Text("${entry.startTime} - ${entry.endTime}", style = MaterialTheme.typography.bodyMedium)
                                }
                                // --- Attendance Controls ---
                                var attendanceMarked by remember { mutableStateOf<Boolean?>(null) }
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text("Attendance:", modifier = Modifier.padding(end = 4.dp))
                                    IconButton(onClick = {
                                        attendanceMarked = true
                                        val subjectId = subjects.find { it.name == entry.subject }?.id ?: 0
                                        coroutineScope.launch {
                                            attendanceViewModel.markAttendance(
                                                subjectId = subjectId,
                                                date = todayDate.toString(),
                                                status = AttendanceStatus.PRESENT
                                            )
                                            errorMessage = "Marked Present for ${entry.subject}"
                                        }
                                    }) {
                                        Icon(Icons.Filled.Check, contentDescription = "Present", tint = if (attendanceMarked == true) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface)
                                    }
                                    IconButton(onClick = {
                                        attendanceMarked = false
                                        val subjectId = subjects.find { it.name == entry.subject }?.id ?: 0
                                        coroutineScope.launch {
                                            attendanceViewModel.markAttendance(
                                                subjectId = subjectId,
                                                date = todayDate.toString(),
                                                status = AttendanceStatus.ABSENT
                                            )
                                            errorMessage = "Marked Absent for ${entry.subject}"
                                        }
                                    }) {
                                        Icon(Icons.Filled.Close, contentDescription = "Absent", tint = if (attendanceMarked == false) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface)
                                    }
                                }
                                // --- Edit/Delete Controls ---
                                IconButton(onClick = {
                                    editingEntry = entry
                                    subject = entry.subject
                                    startTime = entry.startTime
                                    endTime = entry.endTime
                                    showDialog = true
                                }) {
                                    Icon(Icons.Filled.Edit, contentDescription = "Edit")
                                }
                                IconButton(onClick = {
                                    coroutineScope.launch {
                                        timetableViewModel.deleteEntry(entry)
                                        errorMessage = "Entry deleted"
                                    }
                                }) {
                                    Icon(Icons.Filled.Delete, contentDescription = "Delete")
                                }
                            }
                        }
                    }
                }
            }

            if (showDialog) {
                AlertDialog(
                    onDismissRequest = { showDialog = false },
                    title = { Text(if (editingEntry == null) "Add Timetable Entry" else "Edit Timetable Entry") },
                    text = {
                        Column {
                            var subjectDropdownExpanded by remember { mutableStateOf(false) }
                            // --- Subject Dropdown ---
                            ExposedDropdownMenuBox(
                                expanded = subjectDropdownExpanded,
                                onExpandedChange = { subjectDropdownExpanded = it }
                            ) {
                                OutlinedTextField(
                                    value = subject,
                                    onValueChange = {},
                                    readOnly = true,
                                    label = { Text("Subject") },
                                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = subjectDropdownExpanded) },
                                    modifier = Modifier.menuAnchor()
                                )
                                ExposedDropdownMenu(
                                    expanded = subjectDropdownExpanded,
                                    onDismissRequest = { subjectDropdownExpanded = false }
                                ) {
                                    subjects.forEach { subj ->
                                        DropdownMenuItem(
                                            text = { Text(subj.name) },
                                            onClick = {
                                                subject = subj.name
                                                subjectDropdownExpanded = false
                                            }
                                        )
                                    }
                                }
                            }
            // --- Common Slot Dropdown (from DB) ---
            ExposedDropdownMenuBox(
                expanded = commonSlotDropdownExpanded,
                onExpandedChange = { commonSlotDropdownExpanded = it }
            ) {
                OutlinedTextField(
                    value = selectedSlotLabel,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Common Slot (optional)") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = commonSlotDropdownExpanded) },
                    modifier = Modifier.menuAnchor()
                )
                ExposedDropdownMenu(
                    expanded = commonSlotDropdownExpanded,
                    onDismissRequest = { commonSlotDropdownExpanded = false }
                ) {
                    if (slotEntities.isEmpty()) {
                        DropdownMenuItem(
                            text = { Text("No common slots defined") },
                            onClick = { commonSlotDropdownExpanded = false }
                        )
                    } else {
                        slotEntities.forEach { slot ->
                            DropdownMenuItem(
                                text = { Text("${slot.label} (${slot.startTime}-${slot.endTime})") },
                                onClick = {
                                    selectedSlotLabel = slot.label
                                    startTime = slot.startTime
                                    endTime = slot.endTime
                                    commonSlotDropdownExpanded = false
                                }
                            )
                        }
                    }
                }
            }
            // Show assigned time if a slot is selected
            val selectedSlot = slotEntities.find { it.label == selectedSlotLabel }
            selectedSlot?.let {
                Text("Assigned Time: ${it.startTime} - ${it.endTime}", style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(vertical = 4.dp))
            }

                            // --- Manual override with TimePickerDialogSample ---
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("Start Time:")
                            TimePickerDialogSample(
                                selectedTime = if (startTime.isNotBlank()) LocalTime.parse(startTime, DateTimeFormatter.ofPattern("hh:mm a")) else LocalTime.parse("09:00 AM", DateTimeFormatter.ofPattern("hh:mm a")),
                                onTimeSelected = { selected -> startTime = selected.format(DateTimeFormatter.ofPattern("hh:mm a")); selectedSlotLabel = "" }
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("End Time:")
                            TimePickerDialogSample(
                                selectedTime = if (endTime.isNotBlank()) LocalTime.parse(endTime, DateTimeFormatter.ofPattern("hh:mm a")) else LocalTime.parse("10:00 AM", DateTimeFormatter.ofPattern("hh:mm a")),
                                onTimeSelected = { selected -> endTime = selected.format(DateTimeFormatter.ofPattern("hh:mm a")); selectedSlotLabel = "" }
                            )
                        }
                    },
                    confirmButton = {
                        Button(onClick = {
                            if (subject.isBlank() || startTime.isBlank() || endTime.isBlank()) {
                                errorMessage = "All fields are required."
                                return@Button
                            }
                            coroutineScope.launch {
                                if (editingEntry == null) {
                                    timetableViewModel.addEntry(
                                        TimetableEntry(
                                            dayOfWeek = today,
                                            subject = subject,
                                            startTime = startTime,
                                            endTime = endTime
                                        )
                                    )
                                    errorMessage = "Entry added"
                                } else {
                                    timetableViewModel.deleteEntry(editingEntry!!)
                                    timetableViewModel.addEntry(
                                        TimetableEntry(
                                            id = editingEntry!!.id,
                                            dayOfWeek = today,
                                            subject = subject,
                                            startTime = startTime,
                                            endTime = endTime
                                        )
                                    )
                                    errorMessage = "Entry updated"
                                }
                                showDialog = false
                                selectedSlotLabel = ""
                            }
                        }) {
                            Text("Save")
                        }
                    },
                    dismissButton = {
                        OutlinedButton(onClick = { showDialog = false }) {
                            Text("Cancel")
                        }
                    }
                )
            }
        }
    }
}
