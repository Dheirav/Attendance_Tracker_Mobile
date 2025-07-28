package com.example.lol.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.lol.data.TimetableEntry
import com.example.lol.repository.TimetableRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class TimetableViewModel(private val repository: TimetableRepository) : ViewModel() {
    fun getEntriesForDay(day: String): StateFlow<List<TimetableEntry>> =
        repository.getEntriesForDay(day).stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    fun addEntry(entry: TimetableEntry) = viewModelScope.launch { repository.insert(entry) }
    fun deleteEntry(entry: TimetableEntry) = viewModelScope.launch { repository.delete(entry) }
}

class TimetableViewModelFactory(private val repository: TimetableRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(TimetableViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return TimetableViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
