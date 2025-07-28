package com.example.lol.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.lol.data.Subject

@Composable
fun AddOrEditSubjectDialog(
    initialSubject: Subject?,
    onAdd: (name: String, type: String, threshold: Int) -> Unit,
    onUpdate: (subject: Subject) -> Unit,
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
                SubjectTypeDropdown(selectedType = type, onTypeSelected = { type = it })
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
private fun SubjectTypeDropdown(selectedType: String, onTypeSelected: (String) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    val types = listOf("Core", "Elective", "Lab")

    Box {
        OutlinedButton(onClick = { expanded = true }) {
            Text(selectedType)
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            types.forEach { type ->
                DropdownMenuItem(
                    text = { Text(type) },
                    onClick = {
                        onTypeSelected(type)
                        expanded = false
                    }
                )
            }
        }
    }
}
