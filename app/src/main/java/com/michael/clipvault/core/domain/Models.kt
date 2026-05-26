package com.michael.clipvault.core.domain

import kotlinx.coroutines.flow.Flow

data class ClipboardEntry(
    val id: Long = 0,
    val preview: String,
    val timestampMillis: Long,
    val tags: List<String> = emptyList(),
    val patternMatchId: String? = null,
    val patternLabel: String? = null,
    val isFavourite: Boolean = false,
    val matchedText: String? = null
)

data class RegexPattern(
    val id: String,
    val label: String,
    val description: String,
    val regex: String,
    val actionType: String,            // OPEN_URL | COPY_GROUP | SHARE | TRANSFORM
    val actionPayload: String,         // can be URL, index, or transformation types
    val isEnabled: Boolean = true,
    val priority: Int = 0,
    val createdAt: Long = System.currentTimeMillis(),
    val matchCount: Long = 0
)

data class ClipboardAction(
    val patternId: String?,
    val label: String,
    val matchedText: String,
    val groups: List<String>,
    val actionType: String,
    val actionPayload: String,
    val originalText: String,
    val notificationId: Int = (System.currentTimeMillis() % Int.MAX_VALUE).toInt()
) {
    companion object {
        fun noMatch(rawText: String) = ClipboardAction(
            patternId = null,
            label = "Clipboard Updated",
            matchedText = rawText,
            groups = emptyList(),
            actionType = "NONE",
            actionPayload = "",
            originalText = rawText
        )
    }
}

interface ClipboardHistoryRepository {
    fun observeHistory(query: String = ""): Flow<List<ClipboardEntry>>
    suspend fun insert(entry: ClipboardEntry): Long
    suspend fun getDecrypted(id: Long): String
    suspend fun delete(id: Long)
    suspend fun toggleFavourite(id: Long)
    suspend fun clearAll()
}

interface PatternRepository {
    fun observePatterns(): Flow<List<RegexPattern>>
    suspend fun getActivePatterns(): List<RegexPattern>
    suspend fun upsert(pattern: RegexPattern)
    suspend fun delete(id: String)
    suspend fun toggleEnabled(id: String)
    suspend fun incrementMatchCount(id: String)
}
