package com.example.lol.ui.screens

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
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.layout.Arrangement
import com.example.lol.ui.components.*

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
                        // Find the slot(s) for this entry
                        val entrySlots = slotEntities.filter {
                            LocalTime.parse(it.startTime, DateTimeFormatter.ofPattern("hh:mm a", Locale.getDefault())) >= LocalTime.parse(entry.startTime, DateTimeFormatter.ofPattern("hh:mm a", Locale.getDefault())) &&
                            LocalTime.parse(it.endTime, DateTimeFormatter.ofPattern("hh:mm a", Locale.getDefault())) <= LocalTime.parse(entry.endTime, DateTimeFormatter.ofPattern("hh:mm a", Locale.getDefault()))
                        }
                        val selectedSlots: List<Int> = entrySlots.map { it.id }
                        val dismissState = rememberCustomDismissState()
                        if (dismissState.value == CustomDismissValue.DismissedToEnd || dismissState.value == CustomDismissValue.DismissedToStart) {
                            val subjectId = subjects.find { it.name == entry.subject }?.id ?: 0
                            coroutineScope.launch {
                                if (dismissState.value == CustomDismissValue.DismissedToEnd) {
                                    attendanceViewModel.markAttendance(
                                        subjectId = subjectId,
                                        selectedSlots = selectedSlots,
                                        date = todayDate.toString(),
                                        status = AttendanceStatus.PRESENT
                                    )
                                    errorMessage = "Marked Present for ${entry.subject}"
                                } else if (dismissState.value == CustomDismissValue.DismissedToStart) {
                                    attendanceViewModel.markAttendance(
                                        subjectId = subjectId,
                                        selectedSlots = selectedSlots,
                                        date = todayDate.toString(),
                                        status = AttendanceStatus.ABSENT
                                    )
                                    errorMessage = "Marked Absent for ${entry.subject}"
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
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(8.dp),
                                    elevation = CardDefaults.cardElevation(2.dp)
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
            
                AlertDialog(
                    onDismissRequest = { showDialog = false },
                    confirmButton = {
                        Button(onClick = {
                            if (selectedSubject.isBlank() || selectedSlotIds.isEmpty() || slotSelectionError != null) {
                                errorMessage = slotSelectionError ?: "Please select subject and valid slots."
                                return@Button
                            }
                            coroutineScope.launch {
                                val selectedSlots = slotEntities.filter { selectedSlotIds.contains(it.id) }
                                    .sortedBy { LocalTime.parse(it.startTime, DateTimeFormatter.ofPattern("hh:mm a", Locale.getDefault())) }
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
                                    items(slotEntities.sortedBy { LocalTime.parse(it.startTime, DateTimeFormatter.ofPattern("hh:mm a", Locale.getDefault())) }) { slot ->
                                        val checked = selectedSlotIds.contains(slot.id)
                                        Button(
                                            onClick = {
                                                if (!checked) {
                                                    val allSelected = selectedSlotIds + slot.id
                                                    val sortedSlots = slotEntities.filter { allSelected.contains(it.id) }
                                                        .sortedBy { LocalTime.parse(it.startTime, DateTimeFormatter.ofPattern("hh:mm a", Locale.getDefault())) }
                                                    val valid = sortedSlots.zipWithNext().all { (a, b) ->
                                                        val aEnd = LocalTime.parse(a.endTime, DateTimeFormatter.ofPattern("hh:mm a", Locale.getDefault()))
                                                        val bStart = LocalTime.parse(b.startTime, DateTimeFormatter.ofPattern("hh:mm a", Locale.getDefault()))
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
                        }
                    }
                )
            }
        }
    }
}
