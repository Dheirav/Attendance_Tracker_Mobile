package com.example.lol.ui.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
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
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale

// Data class for a common time slot
data class CommonTimeSlot(val label: String, val startTime: String, val endTime: String)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    timetableViewModel: TimetableViewModel,
    subjectRepository: SubjectRepository
) {
    val today = LocalDate.now().dayOfWeek.getDisplayName(TextStyle.FULL, Locale.getDefault())
    val timetableEntries by timetableViewModel.getEntriesForDay(today).collectAsState()
    var showDialog by remember { mutableStateOf(false) }
    var editingEntry by remember { mutableStateOf<TimetableEntry?>(null) }
    var subject by remember { mutableStateOf("") }
    val subjects by subjectRepository.allSubjects.collectAsState(initial = emptyList())
    val commonSlots = remember { mutableStateListOf<CommonTimeSlot>() }
    val selectedSlotIndex = remember { mutableStateOf(-1) }
    val showSlotDropdown = remember { mutableStateOf(false) }
    var startTime by remember { mutableStateOf("") }
    var endTime by remember { mutableStateOf("") }
    val snackbarHostState = remember { SnackbarHostState() }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(errorMessage) {
        errorMessage?.let {
            snackbarHostState.showSnackbar(it)
            errorMessage = null
        }
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Today's Timetable") }) },
        floatingActionButton = {
            Row {
                FloatingActionButton(onClick = {
                    editingEntry = null
                    subject = ""
                    startTime = ""
                    endTime = ""
                    selectedSlotIndex.value = -1
                    showDialog = true
                }, modifier = Modifier.padding(end = 12.dp)) {
                    Icon(Icons.Filled.Add, contentDescription = "Add Timetable Entry")
                }
                OutlinedButton(onClick = {
                    commonSlots.add(CommonTimeSlot("Period ${commonSlots.size + 1}", "09:00", "09:50"))
                }, modifier = Modifier.align(Alignment.CenterVertically)) {
                    Text("Add Common Slot")
                }
            }
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
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
                            // --- Common Slot Dropdown ---
                            Box {
                                OutlinedButton(onClick = { showSlotDropdown.value = !showSlotDropdown.value }, modifier = Modifier.fillMaxWidth()) {
                                    Text(if (selectedSlotIndex.value >= 0) commonSlots[selectedSlotIndex.value].label else "Choose Slot")
                                }
                                DropdownMenu(
                                    expanded = showSlotDropdown.value,
                                    onDismissRequest = { showSlotDropdown.value = false },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    if (commonSlots.isEmpty()) {
                                        DropdownMenuItem(
                                            text = { Text("No common slots defined") },
                                            onClick = { showSlotDropdown.value = false }
                                        )
                                    } else {
                                        commonSlots.forEachIndexed { index, slot ->
                                            DropdownMenuItem(
                                                onClick = {
                                                    selectedSlotIndex.value = index
                                                    startTime = slot.startTime
                                                    endTime = slot.endTime
                                                    showSlotDropdown.value = false
                                                },
                                                text = { Text("${slot.label} (${slot.startTime} - ${slot.endTime})") }
                                            )
                                        }
                                    }
                                }
                            }
                            // Show assigned time if a slot is selected
                            val selectedSlot = if (selectedSlotIndex.value >= 0) commonSlots[selectedSlotIndex.value] else null
                            selectedSlot?.let {
                                Text("Assigned Time: ${it.startTime} - ${it.endTime}", style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(vertical = 4.dp))
                            }
                            // --- Manual override ---
                            OutlinedTextField(
                                value = startTime,
                                onValueChange = { startTime = it },
                                label = { Text("Start Time (e.g. 09:00)") },
                                singleLine = true
                            )
                            OutlinedTextField(
                                value = endTime,
                                onValueChange = { endTime = it },
                                label = { Text("End Time (e.g. 10:00)") },
                                singleLine = true
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
                                selectedSlotIndex.value = -1
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
