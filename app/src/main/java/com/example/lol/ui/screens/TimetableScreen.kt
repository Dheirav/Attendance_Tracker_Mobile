package com.example.lol.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.clickable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.lol.viewmodel.TimetableViewModel
import com.example.lol.data.TimetableEntry
import com.example.lol.data.SubjectRepository
import java.time.DayOfWeek
import java.time.format.TextStyle
import com.vanpra.composematerialdialogs.MaterialDialog
import com.vanpra.composematerialdialogs.MaterialDialogButtons
import com.vanpra.composematerialdialogs.datetime.time.timepicker
import com.vanpra.composematerialdialogs.rememberMaterialDialogState
import java.time.LocalTime
import java.util.*
import java.util.Locale
import kotlinx.coroutines.launch
import java.util.Calendar

// Data class for user-defined common slots
data class CommonSlot(
    val label: String,
    val startTime: String,
    val endTime: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimetableScreen(
    timetableViewModel: TimetableViewModel,
    subjectRepository: SubjectRepository
) {
    val daysOfWeek = DayOfWeek.values().map { it.getDisplayName(TextStyle.FULL, Locale.getDefault()) }
    var selectedDay by remember { mutableStateOf(daysOfWeek[0]) }
    val timetableEntries by timetableViewModel.getEntriesForDay(selectedDay).collectAsState()
    var showDialog by remember { mutableStateOf(false) }
    var editingEntry by remember { mutableStateOf<TimetableEntry?>(null) }
    var subject by remember { mutableStateOf("") }
    val subjects by subjectRepository.allSubjects.collectAsState(initial = emptyList())
    var startTime by remember { mutableStateOf(LocalTime.parse("09:00")) }
    var endTime by remember { mutableStateOf(LocalTime.parse("10:00")) }
    val snackbarHostState = remember { SnackbarHostState() }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(errorMessage) {
        errorMessage?.let {
            snackbarHostState.showSnackbar(it)
            errorMessage = null
        }
    }

    val commonSlots = remember { mutableStateListOf<CommonSlot>() }
    var showAddSlotDialog by remember { mutableStateOf(false) }
    var newSlotLabel by remember { mutableStateOf("") }
    var newSlotStart by remember { mutableStateOf("") }
    var newSlotEnd by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Timetable Editor") })
        },
        floatingActionButton = {
            Row {
                FloatingActionButton(onClick = {
                    editingEntry = null
                    subject = ""
                    startTime = LocalTime.parse("09:00")
                    endTime = LocalTime.parse("10:00")
                    showDialog = true
                }, modifier = Modifier.padding(end = 12.dp)) {
                    Icon(Icons.Filled.Edit, contentDescription = "Add Timetable Entry")
                }
                FloatingActionButton(onClick = {
                    showAddSlotDialog = true
                }) {
                    Icon(Icons.Filled.Add, contentDescription = "Add Common Slot")
                }
            }
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) { innerPadding ->
        Column(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(8.dp)) {
                Text("Select Day :", modifier = Modifier.padding(end = 8.dp))
                DropdownMenuBox(daysOfWeek, selectedDay) { selectedDay = it }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text("Common Time Slots:", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(8.dp))
            if (commonSlots.isEmpty()) {
                Text("No common slots defined", modifier = Modifier.padding(8.dp), color = MaterialTheme.colorScheme.error)
            } else {
                Column(modifier = Modifier.padding(start = 8.dp, end = 8.dp)) {
                    commonSlots.forEachIndexed { idx, slot ->
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)) {
                            Text("${slot.label}: ${slot.startTime} - ${slot.endTime}", modifier = Modifier.weight(1f))
                            IconButton(onClick = { commonSlots.removeAt(idx) }) {
                                Icon(Icons.Filled.Delete, contentDescription = "Delete Slot")
                            }
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            if (timetableEntries.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No timetable entries for $selectedDay.")
                }
            } else {
                Column(modifier = Modifier.fillMaxSize()) {
                    timetableEntries.forEach { entry ->
                        Card(
                            modifier = Modifier.fillMaxWidth().padding(8.dp),
                            elevation = CardDefaults.cardElevation(2.dp)
                        ) {
                            Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(entry.subject, style = MaterialTheme.typography.titleMedium)
                                    Text("${entry.startTime} - ${entry.endTime}", style = MaterialTheme.typography.bodyMedium)
                                }
                                IconButton(onClick = {
                                    editingEntry = entry
                                    subject = entry.subject
                                    startTime = try { LocalTime.parse(entry.startTime) } catch (e: Exception) { LocalTime.parse("09:00") }
                                    endTime = try { LocalTime.parse(entry.endTime) } catch (e: Exception) { LocalTime.parse("10:00") }
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
                            var slotDropdownExpanded by remember { mutableStateOf(false) }
                            val startTimeDialogState = rememberMaterialDialogState()
                            val endTimeDialogState = rememberMaterialDialogState()
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
                            ExposedDropdownMenuBox(
                                expanded = slotDropdownExpanded,
                                onExpandedChange = { slotDropdownExpanded = it }
                            ) {
                                OutlinedTextField(
                                    value = commonSlots.find {
                                        try {
                                            LocalTime.parse(it.startTime) == startTime && LocalTime.parse(it.endTime) == endTime
                                        } catch (e: Exception) { false }
                                    }?.let { "${it.label}: ${it.startTime} - ${it.endTime}" } ?: "Custom",
                                    onValueChange = {},
                                    readOnly = true,
                                    label = { Text("Common Slot") },
                                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = slotDropdownExpanded) },
                                    modifier = Modifier.menuAnchor()
                                )
                                ExposedDropdownMenu(
                                    expanded = slotDropdownExpanded,
                                    onDismissRequest = { slotDropdownExpanded = false }
                                ) {
                                    if (commonSlots.isEmpty()) {
                                        DropdownMenuItem(text = { Text("No common slots defined") }, onClick = { slotDropdownExpanded = false })
                                    } else {
                                        commonSlots.forEach { slot ->
                                            DropdownMenuItem(
                                                text = { Text("${slot.label}: ${slot.startTime} - ${slot.endTime}") },
                                                onClick = {
                                                    startTime = try { LocalTime.parse(slot.startTime) } catch (e: Exception) { startTime }
                                                    endTime = try { LocalTime.parse(slot.endTime) } catch (e: Exception) { endTime }
                                                    slotDropdownExpanded = false
                                                }
                                            )
                                        }
                                    }
                                }
                            }
                            TimePickerDialogSample(selectedTime = startTime, onTimeSelected = { selected -> startTime = selected })
                            Spacer(modifier = Modifier.height(8.dp))
                            TimePickerDialogSample(selectedTime = endTime, onTimeSelected = { selected -> endTime = selected })
                        }
                    },
                    confirmButton = {
                        Button(onClick = {
                            if (subject.isBlank()) {
                                errorMessage = "All fields are required."
                                return@Button
                            }
                            coroutineScope.launch {
                                val startStr = startTime.format(java.time.format.DateTimeFormatter.ofPattern("HH:mm"))
                                val endStr = endTime.format(java.time.format.DateTimeFormatter.ofPattern("HH:mm"))
                                if (editingEntry == null) {
                                    timetableViewModel.addEntry(
                                        TimetableEntry(
                                            dayOfWeek = selectedDay,
                                            subject = subject,
                                            startTime = startStr,
                                            endTime = endStr
                                        )
                                    )
                                    errorMessage = "Entry added"
                                } else {
                                    timetableViewModel.deleteEntry(editingEntry!!)
                                    timetableViewModel.addEntry(
                                        TimetableEntry(
                                            id = editingEntry!!.id,
                                            dayOfWeek = selectedDay,
                                            subject = subject,
                                            startTime = startStr,
                                            endTime = endStr
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
                    }
                )
            }

            if (showAddSlotDialog) {
                AlertDialog(
                    onDismissRequest = { showAddSlotDialog = false },
                    title = { Text("Add Common Slot") },
                    text = {
                        Column {
                            OutlinedTextField(
                                value = newSlotLabel,
                                onValueChange = { newSlotLabel = it },
                                label = { Text("Label/Subject") },
                                singleLine = true
                            )
                            OutlinedTextField(
                                value = newSlotStart,
                                onValueChange = { newSlotStart = it },
                                label = { Text("Start Time (e.g. 09:00)") },
                                singleLine = true
                            )
                            OutlinedTextField(
                                value = newSlotEnd,
                                onValueChange = { newSlotEnd = it },
                                label = { Text("End Time (e.g. 10:00)") },
                                singleLine = true
                            )
                        }
                    },
                    confirmButton = {
                        Button(onClick = {
                            if (newSlotLabel.isNotBlank() && newSlotStart.isNotBlank() && newSlotEnd.isNotBlank()) {
                                commonSlots.add(CommonSlot(newSlotLabel, newSlotStart, newSlotEnd))
                                newSlotLabel = ""
                                newSlotStart = ""
                                newSlotEnd = ""
                                showAddSlotDialog = false
                            }
                        }) { Text("Add") }
                    },
                    dismissButton = {
                        OutlinedButton(onClick = { showAddSlotDialog = false }) { Text("Cancel") }
                    }
                )
            }
        }
    }
}

@Composable
fun TimePickerDialogSample(
    selectedTime: LocalTime,
    onTimeSelected: (LocalTime) -> Unit
) {
    val timeDialogState = rememberMaterialDialogState()
    val time = remember { mutableStateOf(selectedTime) }

    // This opens the dialog when called
    Column {
        Button(onClick = { timeDialogState.show() }) {
            Text("Pick Time: ${time.value}")
        }

        MaterialDialog(
            dialogState = timeDialogState,
            buttons = {
                positiveButton("OK")
                negativeButton("Cancel")
            }
        ) {
            timepicker(
                initialTime = selectedTime,
                title = "Select Time"
            ) { newTime ->
                onTimeSelected(newTime)
                time.value = newTime
            }
        }
    }
}


@Composable
fun DropdownMenuBox(options: List<String>, selected: String, onSelected: (String) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        Button(onClick = { expanded = true }) {
            Text(selected)
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEach { option ->
                DropdownMenuItem(text = { Text(option) }, onClick = {
                    onSelected(option)
                    expanded = false
                })
            }
        }
    }
}
