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
import com.example.lol.viewmodel.CommonSlotViewModel
import com.example.lol.data.CommonSlotEntity
import java.time.DayOfWeek
import java.time.format.TextStyle
import java.time.format.DateTimeFormatter
import com.vanpra.composematerialdialogs.MaterialDialog
import com.vanpra.composematerialdialogs.MaterialDialogButtons
import com.vanpra.composematerialdialogs.datetime.time.timepicker
import com.vanpra.composematerialdialogs.rememberMaterialDialogState
import java.time.LocalTime
import java.util.*
import java.util.Locale
import kotlinx.coroutines.launch
import java.util.Calendar
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimetableScreen(
    timetableViewModel: TimetableViewModel,
    subjectRepository: SubjectRepository,
    commonSlotViewModel: CommonSlotViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
) {
    val daysOfWeek = DayOfWeek.values().map { it.getDisplayName(TextStyle.FULL, Locale.getDefault()) }
    var selectedDay by remember { mutableStateOf(daysOfWeek[0]) }
    val timetableEntries by timetableViewModel.getEntriesForDay(selectedDay).collectAsState()
    var showDialog by remember { mutableStateOf(false) }
    var editingEntry by remember { mutableStateOf<TimetableEntry?>(null) }
    var subject by remember { mutableStateOf("") } 
    val subjects by subjectRepository.allSubjects.collectAsState(initial = emptyList())
    var startTime by remember { mutableStateOf(LocalTime.parse("09:00 AM", DateTimeFormatter.ofPattern("hh:mm a"))) }
    var endTime by remember { mutableStateOf(LocalTime.parse("10:00 AM", DateTimeFormatter.ofPattern("hh:mm a"))) }
    val snackbarHostState = remember { SnackbarHostState() }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val coroutineScope = rememberCoroutineScope()
    val slotEntities by commonSlotViewModel.slots.collectAsState() 

    LaunchedEffect(errorMessage) {
        errorMessage?.let {
            snackbarHostState.showSnackbar(it)
            errorMessage = null
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Timetable Editor") },
                actions = {
                    IconButton(onClick = {
                        editingEntry = null
                        subject = ""
                        startTime = LocalTime.parse("09:00 AM", DateTimeFormatter.ofPattern("hh:mm a"))
                        endTime = LocalTime.parse("10:00 AM", DateTimeFormatter.ofPattern("hh:mm a"))
                        showDialog = true
                    }) {
                        Icon(Icons.Filled.Add, contentDescription = "Add Timetable Entry")
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) { innerPadding ->
        Column(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(8.dp)) {
                Text("Select Day :", modifier = Modifier.padding(end = 8.dp))
                DropdownMenuBox(daysOfWeek, selectedDay) { selectedDay = it }
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
                                    startTime = try { LocalTime.parse(entry.startTime, DateTimeFormatter.ofPattern("hh:mm a")) } catch (e: Exception) { LocalTime.parse("09:00 AM", DateTimeFormatter.ofPattern("hh:mm a")) }
                                    endTime = try { LocalTime.parse(entry.endTime, DateTimeFormatter.ofPattern("hh:mm a")) } catch (e: Exception) { LocalTime.parse("10:00 AM", DateTimeFormatter.ofPattern("hh:mm a")) }
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
                                    .sortedBy { LocalTime.parse(it.startTime, DateTimeFormatter.ofPattern("hh:mm a")) }
                                val startStr = selectedSlots.first().startTime
                                val endStr = selectedSlots.last().endTime
                                if (editingEntry == null) {
                                    timetableViewModel.addEntry(
                                        TimetableEntry(
                                            dayOfWeek = selectedDay,
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
                                            dayOfWeek = selectedDay,
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
                                    items(slotEntities.sortedBy { LocalTime.parse(it.startTime, DateTimeFormatter.ofPattern("hh:mm a")) }) { slot ->
                                        val checked = selectedSlotIds.contains(slot.id)
                                        Button(
                                            onClick = {
                                                if (!checked) {
                                                    val allSelected = selectedSlotIds + slot.id
                                                    val sortedSlots = slotEntities.filter { allSelected.contains(it.id) }
                                                        .sortedBy { LocalTime.parse(it.startTime, DateTimeFormatter.ofPattern("hh:mm a")) }
                                                    val valid = sortedSlots.zipWithNext().all { (a, b) ->
                                                        val aEnd = LocalTime.parse(a.endTime, DateTimeFormatter.ofPattern("hh:mm a"))
                                                        val bStart = LocalTime.parse(b.startTime, DateTimeFormatter.ofPattern("hh:mm a"))
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

@Composable
fun TimePickerDialogSample(
    selectedTime: LocalTime,
    onTimeSelected: (LocalTime) -> Unit
) {
    val timeDialogState = rememberMaterialDialogState()
    val time = remember { mutableStateOf(selectedTime) }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
            .padding(vertical = 4.dp)
            .clip(MaterialTheme.shapes.medium)
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .clickable { timeDialogState.show() },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = time.value.format(java.time.format.DateTimeFormatter.ofPattern("hh:mm a")),
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
    MaterialDialog(
        dialogState = timeDialogState,
        buttons = {
            positiveButton("OK")
            negativeButton("Cancel")
        }
    ) {
        timepicker(
            initialTime = time.value,
            title = "Select Time"
        ) { newTime ->
            onTimeSelected(newTime)
            time.value = newTime
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
