package com.example.core.database

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "clipboard_history")
data class ClipboardEntryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "encrypted_content") val encryptedContent: ByteArray,
    @ColumnInfo(name = "iv") val iv: ByteArray,
    @ColumnInfo(name = "preview") val preview: String,
    @ColumnInfo(name = "pattern_match_id") val patternMatchId: String?,
    @ColumnInfo(name = "pattern_label") val patternLabel: String?,
    @ColumnInfo(name = "timestamp_millis") val timestampMillis: Long,
    @ColumnInfo(name = "tags") val tags: String,                  // Comma-separated tags
    @ColumnInfo(name = "is_favourite") val isFavourite: Boolean = false,
    @ColumnInfo(name = "matched_text") val matchedText: String? = null
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is ClipboardEntryEntity) return false
        if (id != other.id) return false
        return true
    }

    override fun hashCode(): Int {
        return id.hashCode()
    }
}

@Entity(tableName = "regex_patterns")
data class RegexPatternEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "label") val label: String,
    @ColumnInfo(name = "description") val description: String,
    @ColumnInfo(name = "regex_pattern") val regexPattern: String,
    @ColumnInfo(name = "action_type") val actionType: String,
    @ColumnInfo(name = "action_payload") val actionPayload: String,
    @ColumnInfo(name = "is_enabled") val isEnabled: Boolean = true,
    @ColumnInfo(name = "priority") val priority: Int = 0,
    @ColumnInfo(name = "created_at") val createdAt: Long,
    @ColumnInfo(name = "match_count") val matchCount: Long = 0
)

@Dao
interface ClipboardDao {
    @Query("SELECT * FROM clipboard_history ORDER BY timestamp_millis DESC")
    fun getAllHistory(): Flow<List<ClipboardEntryEntity>>

    @Query("SELECT * FROM clipboard_history WHERE preview LIKE '%' || :query || '%' OR pattern_label LIKE '%' || :query || '%' ORDER BY timestamp_millis DESC")
    fun searchHistory(query: String): Flow<List<ClipboardEntryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entry: ClipboardEntryEntity): Long

    @Query("SELECT * FROM clipboard_history WHERE id = :id")
    suspend fun getById(id: Long): ClipboardEntryEntity?

    @Query("UPDATE clipboard_history SET is_favourite = NOT is_favourite WHERE id = :id")
    suspend fun toggleFavourite(id: Long)

    @Query("DELETE FROM clipboard_history WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("DELETE FROM clipboard_history")
    suspend fun clearAll()
}

@Dao
interface RegexPatternDao {
    @Query("SELECT * FROM regex_patterns ORDER BY priority DESC, created_at DESC")
    fun getAllPatternsFlow(): Flow<List<RegexPatternEntity>>

    @Query("SELECT * FROM regex_patterns WHERE is_enabled = 1 ORDER BY priority DESC, created_at DESC")
    suspend fun getActivePatterns(): List<RegexPatternEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(pattern: RegexPatternEntity)

    @Query("DELETE FROM regex_patterns WHERE id = :id")
    suspend fun delete(id: String)

    @Query("UPDATE regex_patterns SET is_enabled = NOT is_enabled WHERE id = :id")
    suspend fun toggleEnabled(id: String)

    @Query("UPDATE regex_patterns SET match_count = match_count + 1 WHERE id = :id")
    suspend fun incrementMatchCount(id: String)
}
