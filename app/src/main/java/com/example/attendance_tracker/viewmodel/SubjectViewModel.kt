package com.example.attendance_tracker.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.attendance_tracker.data.Subject
import com.example.attendance_tracker.data.SubjectRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SubjectViewModel(private val repository: SubjectRepository) : ViewModel() {
    val subjects: StateFlow<List<Subject>> = repository.allSubjects
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    fun addSubject(subject: Subject) = viewModelScope.launch {
        repository.insert(subject)
    }

    fun deleteSubject(subject: Subject) = viewModelScope.launch {
        repository.delete(subject)
    }

    fun updateSubject(subject: Subject) = viewModelScope.launch {
        repository.update(subject)
    }
}

class SubjectViewModelFactory(private val repository: SubjectRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(SubjectViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return SubjectViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
