package com.example.attendance_tracker.ui.screens

import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.background
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Upload
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.attendance_tracker.data.*
import com.example.attendance_tracker.ui.components.AddOrEditSubjectDialog
import com.example.attendance_tracker.viewmodel.SubjectViewModel
import com.example.attendance_tracker.viewmodel.SubjectViewModelFactory
import com.example.attendance_tracker.viewmodel.AttendanceViewModel
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import org.apache.poi.ss.usermodel.CellType
import org.apache.poi.ss.usermodel.Row
import java.time.LocalDateTime

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SubjectsScreen(
    repository: SubjectRepository,
    attendanceViewModel: AttendanceViewModel
) {
    val context = LocalContext.current
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
    val dateFormatter = DateTimeFormatter.ofPattern("dd-MM-yyyy", Locale.getDefault())
    val today = LocalDate.now().format(dateFormatter)

    // Search bar state
    var searchQuery by remember { mutableStateOf("") }
    val filteredSubjects = subjects.filter { it.name.contains(searchQuery, ignoreCase = true) }

    LaunchedEffect(errorMessage) {
        errorMessage?.let {
            snackbarHostState.showSnackbar(it)
            errorMessage = null
        }
    }

    // Export state
    var showExportDialog by remember { mutableStateOf(false) }
    var selectedExportType by remember { mutableStateOf("CSV") }
    val exportTypes = listOf("CSV", "Excel", "PDF")

    // Import state
    var showImportDialog by remember { mutableStateOf(false) }
    val importFileLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent(),
        onResult = { uri: android.net.Uri? ->
            if (uri != null) {
                coroutineScope.launch {
                    try {
                        val inputStream = context.contentResolver.openInputStream(uri)
                        val mimeType = context.contentResolver.getType(uri)
                        val importedSubjects = when {
                            mimeType == "text/csv" -> parseCsvToSubjects(inputStream?.bufferedReader()?.readText() ?: "")
                            mimeType == "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet" -> parseExcelToSubjects(inputStream!!)
                            mimeType == "application/pdf" -> parsePdfToSubjects(inputStream!!)
                            else -> emptyList()
                        }
                        if (importedSubjects.isEmpty()) {
                            errorMessage = "Import successful: No subjects or history available."
                        } else {
                            importedSubjects.forEach { subject ->
                                viewModel.addSubject(subject)
                            }
                            errorMessage = "Import successful: ${importedSubjects.size} subjects added."
                        }
                    } catch (e: Exception) {
                        errorMessage = "Import failed: ${e.message}"
                    }
                }
            }
        }
    )

    val exportCsvLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/csv"),
        onResult = { uri: android.net.Uri? ->
            if (uri != null) {
                coroutineScope.launch {
                    val csv = buildCsv(filteredSubjects)
                    context.contentResolver.openOutputStream(uri)?.use { it.write(csv.toByteArray()) }
                }
            }
        }
    )
    val exportExcelLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"),
        onResult = { uri: android.net.Uri? ->
            if (uri != null) {
                coroutineScope.launch {
                    val excelBytes = buildExcel(filteredSubjects)
                    context.contentResolver.openOutputStream(uri)?.use { it.write(excelBytes) }
                }
            }
        }
    )
    val exportPdfLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/pdf"),
        onResult = { uri: android.net.Uri? ->
            if (uri != null) {
                coroutineScope.launch {
                    val pdfBytes = buildPdf(context, filteredSubjects)
                    context.contentResolver.openOutputStream(uri)?.use { it.write(pdfBytes) }
                }
            }
        }
    )

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
                    IconButton(onClick = { showDialog = true }) {
                        Icon(Icons.Default.Add, contentDescription = "Add Subject")
                    }
                    IconButton(onClick = { showExportDialog = true }) {
                        Icon(Icons.Default.Download, contentDescription = "Export")
                    }
                    IconButton(onClick = { showImportDialog = true }) {
                        Icon(Icons.Default.Upload, contentDescription = "Import")
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) { innerPadding ->
        Column(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            // Search bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                label = { Text("Search Subjects") },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                shape = MaterialTheme.shapes.medium, // Rounded rectangle shape
                singleLine = true // Single-line input for a sleek look
            )

            if (filteredSubjects.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No subjects found.")
                }
            } else {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(filteredSubjects) { subject ->
                        SubjectCard(
                            subject = subject,
                            viewModel = viewModel,
                            attendanceViewModel = attendanceViewModel,
                            today = today,
                            attendanceHistory = attendanceViewModel.attendanceHistory.collectAsState().value,
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

            if (showExportDialog) {
                AlertDialog(
                    onDismissRequest = { showExportDialog = false },
                    title = { Text("Export Attendance Report") },
                    text = {
                        var expanded by remember { mutableStateOf(false) }
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Text("Select file type:")
                            Spacer(modifier = Modifier.height(8.dp))
                            Surface(
                                tonalElevation = 2.dp,
                                shape = MaterialTheme.shapes.medium,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceEvenly
                                ) {
                                    exportTypes.forEach { type ->
                                        val isSelected = selectedExportType == type
                                        Button(
                                            onClick = { selectedExportType = type },
                                            colors = if (isSelected)
                                                ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                                            else
                                                ButtonDefaults.outlinedButtonColors(containerColor = MaterialTheme.colorScheme.surface),
                                            modifier = Modifier.weight(1f).padding(horizontal = 4.dp)
                                        ) {
                                            Text(type, color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface)
                                        }
                                    }
                                }
                            }
                        }
                    },
                    confirmButton = {
                        Button(onClick = {
                            showExportDialog = false
                            val fileName = "attendance_export_${LocalDateTime.now()}"
                            when (selectedExportType) {
                                "CSV" -> exportCsvLauncher.launch("$fileName.csv")
                                "Excel" -> exportExcelLauncher.launch("$fileName.xlsx")
                                "PDF" -> exportPdfLauncher.launch("$fileName.pdf")
                            }
                        }) {
                            Text("Export")
                        }
                    },
                    dismissButton = {
                        OutlinedButton(onClick = { showExportDialog = false }) {
                            Text("Cancel")
                        }
                    }
                )
            }

            if (showImportDialog) {
                AlertDialog(
                    onDismissRequest = { showImportDialog = false },
                    title = { Text("Import Subjects") },
                    text = {
                        Column {
                            Text("Select a file to import subjects. Supported formats: CSV, Excel, PDF.")
                        }
                    },
                    confirmButton = {
                        Button(onClick = {
                            showImportDialog = false
                            importFileLauncher.launch("*/*")
                        }) {
                            Text("Import")
                        }
                    },
                    dismissButton = {
                        OutlinedButton(onClick = { showImportDialog = false }) {
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
    attendanceHistory: List<Attendance>, // Explicitly typed
    onEdit: () -> Unit,
    onDeleteSuccess: (String) -> Unit,
    onEditAttendance: () -> Unit
) {
    val attendancePercentage = if (subject.totalClasses > 0) {
        subject.attendedClasses * 100.0 / subject.totalClasses
    } else {
        0.0
    }
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
                    Text("Attended: ${subject.attendedClasses} / Total: ${subject.totalClasses}", style = MaterialTheme.typography.bodySmall)

                    val attended = subject.attendedClasses
                    val total = subject.totalClasses
                    val threshold = subject.threshold
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
            Text("Attendance: ${"%.1f".format(attendancePercentage)}%", style = MaterialTheme.typography.bodyMedium)
            Spacer(modifier = Modifier.height(8.dp))
            Text("History:", style = MaterialTheme.typography.titleSmall)
            if (attendanceHistory.isEmpty()) {
                Text("No attendance records yet.", style = MaterialTheme.typography.bodySmall)
                Text("No history available.", style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.error))
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

fun buildCsv(subjects: List<Subject>): String {
    val header = "Subject,Type,Threshold,Attended,Total,Percentage\n"
    val rows = subjects.joinToString("\n") {
        val percentage = if (it.totalClasses > 0) (it.attendedClasses * 100.0 / it.totalClasses) else 0.0
        "${it.name},${it.type},${it.threshold},${it.attendedClasses},${it.totalClasses},${"%.2f".format(percentage)}"
    }
    return header + rows
}

fun buildExcel(subjects: List<Subject>): ByteArray {
    val workbook = XSSFWorkbook()
    val sheet = workbook.createSheet("Attendance")
    val header = listOf("Subject", "Type", "Threshold", "Attended", "Total", "Percentage")
    val headerRow = sheet.createRow(0)
    header.forEachIndexed { idx, title ->
        val cell = headerRow.createCell(idx, CellType.STRING)
        cell.setCellValue(title)
    }
    subjects.forEachIndexed { rowIdx, subject ->
        val row = sheet.createRow(rowIdx + 1)
        row.createCell(0, CellType.STRING).setCellValue(subject.name)
        row.createCell(1, CellType.STRING).setCellValue(subject.type)
        row.createCell(2, CellType.NUMERIC).setCellValue(subject.threshold.toDouble())
        row.createCell(3, CellType.NUMERIC).setCellValue(subject.attendedClasses.toDouble())
        row.createCell(4, CellType.NUMERIC).setCellValue(subject.totalClasses.toDouble())
        val percentage = if (subject.totalClasses > 0) (subject.attendedClasses * 100.0 / subject.totalClasses) else 0.0
        row.createCell(5, CellType.NUMERIC).setCellValue(percentage)
    }
    val out = java.io.ByteArrayOutputStream()
    workbook.write(out)
    workbook.close()
    return out.toByteArray()
}

fun buildPdf(context: android.content.Context, subjects: List<Subject>): ByteArray {
    val pdfDocument = PdfDocument()
    val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create() // A4 size
    var pageNumber = 1
    var y = 150f
    var page = pdfDocument.startPage(pageInfo)
    val canvas = page.canvas
    val paint = Paint()
    paint.textSize = 18f
    paint.isFakeBoldText = true

    // Load the image from drawable resources
    val bitmap = android.graphics.BitmapFactory.decodeResource(context.resources, com.example.attendance_tracker.R.drawable.ic_launcher_playstore)
    if (bitmap != null) {
        // Draw the image at the top left, scaled to 48x48 px
        canvas.drawBitmap(bitmap, null, android.graphics.Rect(40, 40, 88, 88), null)
        // Draw header text next to the image
        canvas.drawText("Attendance Report", 100f, 70f, paint)
    } else {
        // Fallback: just draw the header text
        canvas.drawText("Attendance Report", 40f, 70f, paint)
    }
    paint.textSize = 16f
    paint.isFakeBoldText = false
    val header = listOf("Subject", "Type", "Threshold", "Attended", "Total", "Percentage")
    val colWidths = listOf(120, 80, 80, 80, 80, 100)
    var x = 40f
    // Draw table header
    header.forEachIndexed { idx, title ->
        canvas.drawText(title, x, y, paint)
        x += colWidths[idx]
    }
    // Draw header line
    canvas.drawLine(40f, y + 6f, 540f, y + 6f, paint)
    y += 40f
    paint.textSize = 14f
    subjects.forEach {
        x = 40f
        val percentage = if (it.totalClasses > 0) (it.attendedClasses * 100.0 / it.totalClasses) else 0.0
        val row = listOf(
            it.name,
            it.type,
            it.threshold.toString(),
            it.attendedClasses.toString(),
            it.totalClasses.toString(),
            "%.2f".format(percentage) + "%"
        )
        row.forEachIndexed { idx, cell ->
            canvas.drawText(cell, x, y, paint)
            x += colWidths[idx]
        }
        // Draw row line
        canvas.drawLine(40f, y + 6f, 540f, y + 6f, paint)
        y += 36f // Increased spacing between lines (was 24f)
        if (y > 800f) {
            pdfDocument.finishPage(page)
            pageNumber++
            page = pdfDocument.startPage(PdfDocument.PageInfo.Builder(595, 842, pageNumber).create())
            y = 60f
            page.canvas.drawText("Attendance Report", 40f, 40f, paint)
            paint.textSize = 16f
            x = 40f
            header.forEachIndexed { idx, title ->
                page.canvas.drawText(title, x, y, paint)
                x += colWidths[idx]
            }
            page.canvas.drawLine(40f, y + 6f, 540f, y + 6f, paint)
            y += 30f
            paint.textSize = 14f
        }
    }
    pdfDocument.finishPage(page)
    val out = java.io.ByteArrayOutputStream()
    pdfDocument.writeTo(out)
    pdfDocument.close()
    return out.toByteArray()
}

fun parseCsvToSubjects(csv: String): List<Subject> {
    val lines = csv.split("\n").filter { it.isNotBlank() }
    return lines.drop(1).map { line -> // Skip header line
        val columns = line.split(",")
        Subject(
            name = columns.getOrNull(0)?.trim() ?: "",
            type = columns.getOrNull(1)?.trim() ?: "",
            threshold = columns.getOrNull(2)?.trim()?.toIntOrNull() ?: 0,
            attendedClasses = columns.getOrNull(3)?.trim()?.toIntOrNull() ?: 0,
            totalClasses = columns.getOrNull(4)?.trim()?.toIntOrNull() ?: 0
        )
    }
}

fun parseExcelToSubjects(inputStream: java.io.InputStream): List<Subject> {
    val subjects = mutableListOf<Subject>()
    val workbook = XSSFWorkbook(inputStream)
    val sheet = workbook.getSheetAt(0)
    for (rowIdx in 1..sheet.lastRowNum) {
        val row = sheet.getRow(rowIdx)
        if (row != null) {
            val name = row.getCell(0)?.stringCellValue ?: ""
            val type = row.getCell(1)?.stringCellValue ?: ""
            val threshold = row.getCell(2)?.numericCellValue?.toInt() ?: 0
            val attended = row.getCell(3)?.numericCellValue?.toInt() ?: 0
            val total = row.getCell(4)?.numericCellValue?.toInt() ?: 0
            subjects.add(Subject(
                name = name,
                type = type,
                threshold = threshold,
                attendedClasses = attended,
                totalClasses = total
            ))
        }
    }
    workbook.close()
    return subjects
}

fun parsePdfToSubjects(inputStream: java.io.InputStream): List<Subject> {
    // TODO: Implement PDF parsing using PDFBox or iText
    // For now, return empty list
    return emptyList()
}
