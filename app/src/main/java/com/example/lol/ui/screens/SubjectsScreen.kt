package com.example.lol.ui.screens

import android.content.Context
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.lol.data.Subject
import com.example.lol.data.SubjectRepository
import com.example.lol.viewmodel.SubjectViewModel
import com.example.lol.viewmodel.SubjectViewModelFactory
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SubjectsScreen(
    repository: SubjectRepository
) {
    val viewModel: SubjectViewModel = viewModel(factory = SubjectViewModelFactory(repository))
    val subjects by viewModel.subjects.collectAsState()
    var showDialog by remember { mutableStateOf(false) }
    var editingSubject by remember { mutableStateOf<Subject?>(null) }
    val snackbarHostState = SnackbarHostState()
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(errorMessage) {
        errorMessage?.let {
            snackbarHostState.showSnackbar(it)
            errorMessage = null
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            TopAppBar(
                title = { Text("Subjects") },
                navigationIcon = {
                    IconButton(onClick = { /* Handle back navigation if needed */ }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
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
            if (subjects.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No subjects yet.")
                }
            } else {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(subjects) { subject ->
                        SubjectItem(
                            subject = subject,
                            onDelete = {
                                viewModel.deleteSubject(subject)
                                errorMessage = "Subject deleted"
                            },
                            onEdit = {
                                editingSubject = subject
                                showDialog = true
                            }
                        )
                    }
                }
            }
        }
        SnackbarHost(hostState = snackbarHostState, modifier = Modifier.align(Alignment.BottomCenter))
        if (showDialog) {
            AddOrEditSubjectDialog(
                initialSubject = editingSubject,
                onAdd = { name, type, threshold ->
                    if (subjects.any { it.name.equals(name, ignoreCase = true) }) {
                        errorMessage = "Subject with this name already exists."
                    } else {
                        viewModel.addSubject(Subject(name = name, type = type, threshold = threshold))
                        showDialog = false
                        errorMessage = "Subject added"
                    }
                },
                onUpdate = { updatedSubject ->
                    if (subjects.any { it.name.equals(updatedSubject.name, ignoreCase = true) && it.id != updatedSubject.id }) {
                        errorMessage = "Another subject with this name already exists."
                    } else {
                        viewModel.updateSubject(updatedSubject)
                        showDialog = false
                        errorMessage = "Subject updated"
                    }
                },
                onDismiss = { showDialog = false }
            )
        }
    }
}

@Composable
fun SubjectItem(subject: Subject, onDelete: () -> Unit, onEdit: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp)
            .clickable { onEdit() },
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(subject.name, style = MaterialTheme.typography.titleMedium)
                Text(subject.type, style = MaterialTheme.typography.bodyMedium)
                Text("Threshold: ${subject.threshold}%", style = MaterialTheme.typography.bodySmall)
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, contentDescription = "Delete")
            }
        }
    }
}

@Composable
fun AddOrEditSubjectDialog(
    initialSubject: Subject?,
    onAdd: (String, String, Int) -> Unit,
    onUpdate: (Subject) -> Unit,
    onDismiss: () -> Unit
) {
    var name by remember { mutableStateOf(initialSubject?.name ?: "") }
    var type by remember { mutableStateOf(initialSubject?.type ?: "Core") }
    var threshold by remember { mutableStateOf(initialSubject?.threshold?.toString() ?: "75") }
    var error by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initialSubject == null) "Add Subject" else "Edit Subject") },
        text = {
            Column {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Name") },
                    isError = error != null && name.isBlank()
                )
                Spacer(modifier = Modifier.height(8.dp))
                DropdownMenuBox(selectedType = type, onTypeSelected = { type = it })
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = threshold,
                    onValueChange = { threshold = it.filter { c -> c.isDigit() } },
                    label = { Text("Threshold (%)") },
                    isError = error != null && (threshold.isBlank() || threshold.toIntOrNull() == null)
                )
                if (error != null) {
                    Text(error!!, color = MaterialTheme.colorScheme.error)
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                val thresholdInt = threshold.toIntOrNull()
                if (name.isBlank()) {
                    error = "Name cannot be empty"
                } else if (thresholdInt == null || thresholdInt !in 1..100) {
                    error = "Threshold must be 1-100"
                } else {
                    error = null
                    if (initialSubject == null) {
                        onAdd(name, type, thresholdInt)
                    } else {
                        onUpdate(initialSubject.copy(name = name, type = type, threshold = thresholdInt))
                    }
                }
            }) {
                Text(if (initialSubject == null) "Add" else "Update")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun DropdownMenuBox(selectedType: String, onTypeSelected: (String) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    val types = listOf("Core", "Elective", "Lab")
    Box {
        OutlinedButton(onClick = { expanded = true }) {
            Text(selectedType)
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            types.forEach { type ->
                DropdownMenuItem(text = { Text(type) }, onClick = {
                    onTypeSelected(type)
                    expanded = false
                })
            }
        }
    }
}
