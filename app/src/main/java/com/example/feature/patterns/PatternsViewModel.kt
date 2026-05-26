package com.example.feature.patterns

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.core.common.ServiceLocator
import com.example.core.domain.RegexPattern
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.UUID

class PatternsViewModel : ViewModel() {
    private val patternRepository = ServiceLocator.getPatternRepository()

    val patternsState: StateFlow<List<RegexPattern>> = patternRepository.observePatterns()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun toggleEnabled(id: String) {
        viewModelScope.launch {
            patternRepository.toggleEnabled(id)
        }
    }

    fun deletePattern(id: String) {
        viewModelScope.launch {
            patternRepository.delete(id)
        }
    }

    fun upsertPattern(
        label: String,
        description: String,
        regex: String,
        actionType: String,
        actionPayload: String,
        priority: Int,
        existingId: String? = null
    ) {
        viewModelScope.launch {
            val id = existingId ?: UUID.randomUUID().toString()
            val pattern = RegexPattern(
                id = id,
                label = label,
                description = description,
                regex = regex,
                actionType = actionType,
                actionPayload = actionPayload,
                isEnabled = true,
                priority = priority,
                createdAt = System.currentTimeMillis()
            )
            patternRepository.upsert(pattern)
        }
    }

    // Interactive Regex Playground State
    private val _playgroundInput = MutableStateFlow("GPS coords: 52.5200, 13.4050 or parcel ID DE8937040. Call support at +1 (555) 019-2834.")
    val playgroundInput = _playgroundInput.asStateFlow()

    private val _playgroundRegex = MutableStateFlow("""(-?\d{1,3}\.\d+),\s*(-?\d{1,3}\.\d+)""")
    val playgroundRegex = _playgroundRegex.asStateFlow()

    val playgroundResult: StateFlow<PlaygroundResult> = combine(_playgroundInput, _playgroundRegex) { input, patternStr ->
        if (patternStr.isEmpty()) {
            PlaygroundResult.Empty
        } else {
            try {
                val regex = Regex(patternStr)
                val matches = regex.findAll(input).toList()
                if (matches.isNotEmpty()) {
                    val list = matches.map { match ->
                        MatchInfo(
                            value = match.value,
                            range = match.range,
                            groups = match.groupValues
                        )
                    }
                    PlaygroundResult.Success(list)
                } else {
                    PlaygroundResult.NoMatch
                }
            } catch (e: Exception) {
                PlaygroundResult.Error(e.message ?: "Invalid regular expression pattern.")
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), PlaygroundResult.Empty)

    fun updatePlaygroundInput(text: String) {
        _playgroundInput.value = text
    }

    fun updatePlaygroundRegex(regex: String) {
        _playgroundRegex.value = regex
    }
}

data class MatchInfo(
    val value: String,
    val range: IntRange,
    val groups: List<String>
)

sealed interface PlaygroundResult {
    object Empty : PlaygroundResult
    object NoMatch : PlaygroundResult
    data class Success(val matches: List<MatchInfo>) : PlaygroundResult
    data class Error(val message: String) : PlaygroundResult
}
