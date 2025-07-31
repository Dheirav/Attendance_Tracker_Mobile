package com.example.lol.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import com.vanpra.composematerialdialogs.MaterialDialog
import com.vanpra.composematerialdialogs.datetime.time.timepicker
import com.vanpra.composematerialdialogs.rememberMaterialDialogState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.lol.data.CommonSlotEntity
import com.example.lol.data.CommonSlotDao
import com.example.lol.viewmodel.CommonSlotViewModel
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.draw.clip
import java.util.Locale


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CommonSlotsManagerScreen(
    commonSlotViewModel: CommonSlotViewModel = androidx.lifecycle.viewmodel.compose.viewModel(),
    onBack: () -> Unit = {}
) {
    val slotEntities by commonSlotViewModel.slots.collectAsState()
    var showAddDialog by remember { mutableStateOf(false) }
    var showEditDialog by remember { mutableStateOf(false) }
    var editSlot: CommonSlotEntity? by remember { mutableStateOf(null) }
    var label by remember { mutableStateOf("") }
    var startTime by remember { mutableStateOf("") }
    var endTime by remember { mutableStateOf("") }
    val timeFormatter = DateTimeFormatter.ofPattern("hh:mm a", Locale.getDefault())

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Manage Common Slots") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.Edit, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = {
                        label = ""
                        startTime = ""
                        endTime = ""
                        showAddDialog = true
                    }) {
                        Icon(Icons.Filled.Add, contentDescription = "Add Slot")
                    }
                }
            )
        },
    ) { innerPadding ->
        Column(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            if (slotEntities.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No common slots defined.")
                }
            } else {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    itemsIndexed(slotEntities) { idx, slot ->
                        Card(modifier = Modifier.fillMaxWidth().padding(8.dp)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(16.dp)
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(slot.label, style = MaterialTheme.typography.titleMedium)
                                    Text("${slot.startTime} - ${slot.endTime}", style = MaterialTheme.typography.bodyMedium)
                                }
                                IconButton(onClick = {
                                    editSlot = slot
                                    label = slot.label
                                    startTime = slot.startTime
                                    endTime = slot.endTime
                                    showEditDialog = true
                                }) {
                                    Icon(Icons.Filled.Edit, contentDescription = "Edit Slot")
                                }
                                IconButton(onClick = {
                                    commonSlotViewModel.deleteSlot(slot)
                                }) {
                                    Icon(Icons.Filled.Delete, contentDescription = "Delete Slot")
                                }
                            }
                        }
                    }
                }
            }
        }

        if (showAddDialog) {
            AlertDialog(
                onDismissRequest = { showAddDialog = false },
                title = { Text("Add Common Slot") },
                text = {
                    Column {
                        OutlinedTextField(
                            value = label,
                            onValueChange = { label = it },
                            label = { Text("Label/Subject") },
                            singleLine = true
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Start Time:")
                        TimePickerDialogSample(
                            selectedTime = if (startTime.isNotBlank()) LocalTime.parse(startTime, timeFormatter) else LocalTime.parse("09:00 AM", timeFormatter),
                            onTimeSelected = { selected: LocalTime -> startTime = selected.format(timeFormatter) }
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("End Time:")
                        TimePickerDialogSample(
                            selectedTime = if (endTime.isNotBlank()) LocalTime.parse(endTime, timeFormatter) else LocalTime.parse("10:00 AM", timeFormatter),
                            onTimeSelected = { selected: LocalTime -> endTime = selected.format(timeFormatter) }
                        )
                    }
                },
                confirmButton = {
                    Button(onClick = {
                        if (label.isNotBlank() && startTime.isNotBlank() && endTime.isNotBlank()) {
                            commonSlotViewModel.insertSlot(
                                CommonSlotEntity(label = label, startTime = startTime, endTime = endTime)
                            )
                            label = ""
                            startTime = ""
                            endTime = ""
                            showAddDialog = false
                        }
                    }) { Text("Add") }
                },
                dismissButton = {
                    OutlinedButton(onClick = { showAddDialog = false }) { Text("Cancel") }
                }
            )
        }

        if (showEditDialog && editSlot != null) {
            AlertDialog(
                onDismissRequest = { showEditDialog = false },
                title = { Text("Edit Common Slot") },
                text = {
                    Column {
                        OutlinedTextField(
                            value = label,
                            onValueChange = { label = it },
                            label = { Text("Label/Subject") },
                            singleLine = true
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Start Time:")
                        TimePickerDialogSample(
                            selectedTime = if (startTime.isNotBlank()) LocalTime.parse(startTime, timeFormatter) else LocalTime.parse("09:00 AM", timeFormatter),
                            onTimeSelected = { selected -> startTime = selected.format(timeFormatter) }
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("End Time:")
                        TimePickerDialogSample(
                            selectedTime = if (endTime.isNotBlank()) LocalTime.parse(endTime, timeFormatter) else LocalTime.parse("10:00 AM", timeFormatter),
                            onTimeSelected = { selected -> endTime = selected.format(timeFormatter) }
                        )
                    }
                },
                confirmButton = {
                    Button(onClick = {
                        if (label.isNotBlank() && startTime.isNotBlank() && endTime.isNotBlank()) {
                            commonSlotViewModel.updateSlot(
                                editSlot!!.copy(label = label, startTime = startTime, endTime = endTime)
                            )
                            showEditDialog = false
                        }
                    }) { Text("Save") }
                },
                dismissButton = {
                    OutlinedButton(onClick = { showEditDialog = false }) { Text("Cancel") }
                }
            )
        }
    }
}