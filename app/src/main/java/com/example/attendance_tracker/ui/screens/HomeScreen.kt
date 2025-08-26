package com.example.attendance_tracker.ui.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.background
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Error
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.dp
import com.example.attendance_tracker.ui.components.CustomDismissValue
import java.time.format.DateTimeParseException
import kotlinx.coroutines.launch
import com.example.attendance_tracker.viewmodel.TimetableViewModel
import com.example.attendance_tracker.data.TimetableEntry
import com.example.attendance_tracker.data.SubjectRepository
import com.example.attendance_tracker.viewmodel.CommonSlotViewModel
import com.example.attendance_tracker.viewmodel.AttendanceViewModel
import com.example.attendance_tracker.data.AttendanceStatus
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import com.example.attendance_tracker.ui.components.*

// Define a single DateTimeFormatter instance for reuse
val timeFormatter = DateTimeFormatter.ofPattern("hh:mm a", Locale.US)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    timetableViewModel: TimetableViewModel,
    subjectRepository: SubjectRepository,
    commonSlotViewModel: CommonSlotViewModel = androidx.lifecycle.viewmodel.compose.viewModel(),
    attendanceViewModel: AttendanceViewModel
) {
    val today = LocalDate.now().dayOfWeek.getDisplayName(TextStyle.FULL, Locale.US)
    val timetableEntries by timetableViewModel.getEntriesForDay(today).collectAsState()
    var showDialog by remember { mutableStateOf(false) }
    var editingEntry by remember { mutableStateOf<TimetableEntry?>(null) }
    var subject by remember { mutableStateOf("") }
    val subjects by subjectRepository.allSubjects.collectAsState(initial = emptyList())
    val slotEntities by commonSlotViewModel.slots.collectAsState()
    var selectedSlotLabel by remember { mutableStateOf("") }
    var startTime by remember { mutableStateOf("") }
    var endTime by remember { mutableStateOf("") }
    val snackbarHostState = remember { SnackbarHostState() }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val coroutineScope = rememberCoroutineScope()
    val todayDate = LocalDate.now()
    val dayOfWeek = todayDate.dayOfWeek.getDisplayName(TextStyle.FULL, Locale.US)
    val dateStr = todayDate.format(java.time.format.DateTimeFormatter.ofPattern("dd MMMM yyyy"))

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
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(timetableEntries) { entry ->
                        // Find the slot(s) for this entry
                        val entrySlots = slotEntities.filter {
                            (safeParseTime(it.startTime) ?: LocalTime.MIN) >= (safeParseTime(entry.startTime) ?: LocalTime.MIN) &&
                                    (safeParseTime(it.endTime) ?: LocalTime.MAX) <= (safeParseTime(entry.endTime) ?: LocalTime.MAX)
                        }
                        val selectedSlots: List<Int> = entrySlots.map { it.id }
                        val dismissState = rememberCustomDismissState()
                        if (dismissState.value == CustomDismissValue.DismissedToEnd || dismissState.value == CustomDismissValue.DismissedToStart) {
                            val subjectId = subjects.find { it.name == entry.subject }?.id ?: 0
                            coroutineScope.launch {
                                val attendance = attendanceViewModel.getAttendanceStatusForSubjectOnDate(subjectId, todayDate.toString())
                                if (attendance == null) {
                                    // No record yet, insert new
                                    attendanceViewModel.markAttendance(
                                        subjectId = subjectId,
                                        selectedSlots = selectedSlots,
                                        date = todayDate.toString(),
                                        status = if (dismissState.value == CustomDismissValue.DismissedToEnd) AttendanceStatus.PRESENT else AttendanceStatus.ABSENT
                                    )
                                } else {
                                    // Record exists, update status
                                    attendanceViewModel.updateAttendanceStatusForSubjectOnDate(
                                        subjectId,
                                        todayDate.toString(),
                                        if (dismissState.value == CustomDismissValue.DismissedToEnd) AttendanceStatus.PRESENT else AttendanceStatus.ABSENT
                                    )
                                }
                                errorMessage = if (dismissState.value == CustomDismissValue.DismissedToEnd) {
                                    "Marked Present for ${entry.subject}"
                                } else {
                                    "Marked Absent for ${entry.subject}"
                                }
                                dismissState.value = CustomDismissValue.Default
                            }
                        }
                        CustomSwipeToDismiss(
                            state = dismissState,
                            background = { direction ->
                                val color = when (direction) {
                                    CustomDismissDirection.StartToEnd -> MaterialTheme.colorScheme.primary
                                    CustomDismissDirection.EndToStart -> MaterialTheme.colorScheme.error
                                }
                                Box(
                                    Modifier
                                        .fillMaxSize()
                                        .background(color)
                                )
                            },
                            content = {
                                val subjectId = subjects.find { it.name == entry.subject }?.id
                                val attendanceStatus by produceState<AttendanceStatus?>(null, subjectId, todayDate) {
                                    value = if (subjectId != null) {
                                        attendanceViewModel.getAttendanceStatusForSubjectOnDate(subjectId, todayDate.toString())
                                    } else null
                                }
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(8.dp),
                                    elevation = CardDefaults.cardElevation(2.dp),
                                    colors = CardDefaults.cardColors(
                                        containerColor = when (attendanceStatus) {
                                            AttendanceStatus.PRESENT -> Color(0xFFA5D6A7)
                                            AttendanceStatus.ABSENT -> Color(0xFFEF9A9A)
                                            null -> MaterialTheme.colorScheme.surface
                                            else -> MaterialTheme.colorScheme.surface
                                        }
                                    )
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(16.dp)
                                    ) {
                                        Text(entry.subject, style = MaterialTheme.typography.titleMedium)
                                        Text("${entry.startTime} - ${entry.endTime}", style = MaterialTheme.typography.bodyMedium)
                                        // --- Attendance Progress Bar ---
                                        val subjectObj = subjects.find { it.name == entry.subject }
                                        val attended = subjectObj?.attendedClasses ?: 0
                                        val total = subjectObj?.totalClasses ?: 0
                                        val threshold = subjectObj?.threshold ?: 75
                                        val attendancePercent = if (total > 0) attended * 100 / total else 0
                                        val thresholdPercent = threshold
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Box(modifier = Modifier.fillMaxWidth().height(20.dp)) {
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxSize()
                                                    .background(MaterialTheme.colorScheme.secondary.copy(alpha = 0.5f))
                                            )
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxHeight()
                                                    .fillMaxWidth(fraction = thresholdPercent / 100f)
                                                    .background(MaterialTheme.colorScheme.error.copy(alpha = 0.75f))
                                            )
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxHeight()
                                                    .fillMaxWidth(fraction = attendancePercent / 100f)
                                                    .background(MaterialTheme.colorScheme.primary)
                                            )
                                        }
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text("Attendance: $attendancePercent%", style = MaterialTheme.typography.bodyMedium)
                                            Text("Threshold: $thresholdPercent%", style = MaterialTheme.typography.bodyMedium)
                                        }
                                        // --- Edit/Delete Controls ---
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.End
                                        ) {
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
                        )
                    }
                }
            }

            if (showDialog) {
                var subjectDropdownExpanded by remember { mutableStateOf(false) }
                val subjectOptions = subjects.map { it.name }
                var selectedSubject by remember { mutableStateOf(subjectOptions.firstOrNull() ?: "") }
                val selectedSlotIds = remember { mutableStateListOf<Int>() }
                var slotSelectionError by remember { mutableStateOf<String?>(null) }

                // Reset errorMessage when subject or slot selection changes
                LaunchedEffect(selectedSubject, selectedSlotIds) {
                    errorMessage = null
                }

                AlertDialog(
                    onDismissRequest = { showDialog = false },
                    confirmButton = {
                        Button(onClick = {
                            if (selectedSubject.isBlank() || selectedSlotIds.isEmpty() || slotSelectionError != null) {
                                errorMessage = slotSelectionError ?: "Please select subject and valid slots."
                                return@Button
                            }
                            val selectedSlots = slotEntities.filter { selectedSlotIds.contains(it.id) }
                                .sortedBy { LocalTime.parse(it.startTime, timeFormatter) }
                            val newStart = LocalTime.parse(selectedSlots.first().startTime, timeFormatter)
                            val newEnd = LocalTime.parse(selectedSlots.last().endTime, timeFormatter)
                            val conflict = timetableEntries.any { entry ->
                                if (editingEntry != null && entry.id == editingEntry!!.id) return@any false // skip self when editing
                                val entryStart = LocalTime.parse(entry.startTime, timeFormatter)
                                val entryEnd = LocalTime.parse(entry.endTime, timeFormatter)
                                val timeOverlap = newStart < entryEnd && newEnd > entryStart
                                val slotOverlap = entry.slotIds.any { selectedSlotIds.contains(it) }
                                timeOverlap || slotOverlap
                            }
                            if (conflict) {
                                errorMessage = "Conflict: Overlapping time or duplicate slot with another entry."
                                return@Button
                            }
                            coroutineScope.launch {
                                val startStr = selectedSlots.first().startTime
                                val endStr = selectedSlots.last().endTime
                                if (editingEntry == null) {
                                    timetableViewModel.addEntry(
                                        TimetableEntry(
                                            dayOfWeek = today ,
                                            subject = selectedSubject,
                                            startTime = startStr,
                                            endTime = endStr,
                                            slotIds = selectedSlotIds.toList()
                                        )
                                    )
                                    errorMessage = "Entry added"
                                } else {
                                    timetableViewModel.deleteEntry(editingEntry!!)
                                    timetableViewModel.addEntry(
                                        TimetableEntry(
                                            id = editingEntry!!.id,
                                            dayOfWeek = today ,
                                            subject = selectedSubject,
                                            startTime = startStr,
                                            endTime = endStr,
                                            slotIds = selectedSlotIds.toList()
                                        )
                                    )
                                    errorMessage = "Entry updated"
                                }
                                showDialog = false
                            }
                        }) {
                            Text("Save")
                        }
                    },
                    dismissButton = {
                        OutlinedButton(onClick = { showDialog = false }) {
                            Text("Cancel")
                        }
                    },
                    title = { Text(if (editingEntry == null) "Add Timetable Entry" else "Edit Timetable Entry") },
                    text = {
                        Column {
                            Text("Select Subject:")
                            ExposedDropdownMenuBox(
                                expanded = subjectDropdownExpanded,
                                onExpandedChange = { subjectDropdownExpanded = it }
                            ) {
                                OutlinedTextField(
                                    value = selectedSubject,
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
                                    subjectOptions.forEach { subj ->
                                        DropdownMenuItem(
                                            text = { Text(subj) },
                                            onClick = {
                                                selectedSubject = subj
                                                subjectDropdownExpanded = false
                                            }
                                        )
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(16.dp))
                            Text("Select Time Slots (≤20 min gap):")
                            Box(modifier = Modifier.height(220.dp)) {
                                LazyColumn {
                                    items(slotEntities.sortedBy { LocalTime.parse(it.startTime, timeFormatter) }) { slot ->
                                        val checked = selectedSlotIds.contains(slot.id)
                                        Button(
                                            onClick = {
                                                if (!checked) {
                                                    val allSelected = selectedSlotIds + slot.id
                                                    val sortedSlots = slotEntities.filter { allSelected.contains(it.id) }
                                                        .sortedBy { LocalTime.parse(it.startTime, timeFormatter) }
                                                    val valid = sortedSlots.zipWithNext().all { (a, b) ->
                                                        val aEnd = LocalTime.parse(a.endTime, timeFormatter)
                                                        val bStart = LocalTime.parse(b.startTime, timeFormatter)
                                                        java.time.Duration.between(aEnd, bStart).toMinutes() <= 20
                                                    }
                                                    if (valid) {
                                                        selectedSlotIds.add(slot.id)
                                                        slotSelectionError = null
                                                    } else {
                                                        slotSelectionError = "Selected slots must be consecutive with ≤20 min gap."
                                                    }
                                                } else {
                                                    selectedSlotIds.remove(slot.id)
                                                    slotSelectionError = null
                                                }
                                            },
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = if (checked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
                                                contentColor = if (checked) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                                            ),
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(vertical = 4.dp)
                                        ) {
                                            Text("${slot.label}: ${slot.startTime} - ${slot.endTime}")
                                        }
                                    }
                                }
                            }
                            if (slotSelectionError != null) {
                                Text(slotSelectionError!!, color = MaterialTheme.colorScheme.error)
                            }
                            if (!errorMessage.isNullOrBlank()) {
                                LaunchedEffect(errorMessage) {
                                    errorMessage?.let {
                                        snackbarHostState.showSnackbar(it)
                                    }
                                }
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 8.dp)
                                        .background(
                                            color = MaterialTheme.colorScheme.error.copy(alpha = 0.1f),
                                            shape = MaterialTheme.shapes.medium
                                        )
                                        .padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Error,
                                        contentDescription = "Error",
                                        tint = MaterialTheme.colorScheme.error,
                                        modifier = Modifier.size(24.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = errorMessage!!,
                                        color = MaterialTheme.colorScheme.error,
                                        style = MaterialTheme.typography.bodyMedium,
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }
                        }
                    }
                )
            }
        }
    }
}

// Update LocalTime.parse calls to trim input strings and handle errors
val safeParseTime: (String) -> LocalTime? = { timeString ->
    try {
        val trimmedTime = timeString.trim()
        println("Parsing time: '$trimmedTime'") // Debugging log
        LocalTime.parse(trimmedTime, timeFormatter)
    } catch (e: DateTimeParseException) {
        println("Failed to parse time: '${timeString.trim()}' - ${e.message}") // Debugging log
        null
    }
}

// Enhanced time parsing with fallback and added debugging logs
fun parseTimeWithFallback(timeString: String, defaultHour: Int, defaultMinute: Int): LocalTime {
    return try {
        val trimmedTime = timeString.trim()
        println("Parsing time with fallback: '$trimmedTime'") // Debugging log
        LocalTime.parse(trimmedTime, timeFormatter)
    } catch (e: Exception) {
        println("Failed to parse time with fallback: '$timeString' - ${e.message}") // Debugging log
        LocalTime.of(defaultHour, defaultMinute)
    }
}