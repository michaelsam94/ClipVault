package com.example.feature.clipboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.core.common.ServiceLocator
import com.example.core.domain.ClipboardEntry
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

@OptIn(FlowPreview::class)
class ClipboardHistoryViewModel : ViewModel() {
    private val historyRepository = ServiceLocator.getHistoryRepository()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    val historyState: StateFlow<List<ClipboardEntry>> = _searchQuery
        .debounce(150)
        .flatMapLatest { query ->
            historyRepository.observeHistory(query)
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun updateQuery(query: String) {
        _searchQuery.value = query
    }

    fun toggleFavourite(id: Long) {
        viewModelScope.launch {
            historyRepository.toggleFavourite(id)
        }
    }

    fun deleteEntry(id: Long) {
        viewModelScope.launch {
            historyRepository.delete(id)
        }
    }

    fun clearAllHistory() {
        viewModelScope.launch {
            historyRepository.clearAll()
        }
    }

    suspend fun decryptEntry(id: Long): String {
        return historyRepository.getDecrypted(id)
    }
}
