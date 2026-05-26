package com.example.core.database

import com.example.core.domain.ClipboardEntry
import com.example.core.domain.ClipboardHistoryRepository
import com.example.core.domain.PatternRepository
import com.example.core.domain.RegexPattern
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ClipboardHistoryRepositoryImpl(
    private val clipboardDao: ClipboardDao,
    private val encryptionService: EncryptionService,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) : ClipboardHistoryRepository {

    override fun observeHistory(query: String): Flow<List<ClipboardEntry>> {
        val flow = if (query.isEmpty()) {
            clipboardDao.getAllHistory()
        } else {
            clipboardDao.searchHistory(query)
        }
        return flow.map { list ->
            list.map { entity ->
                ClipboardEntry(
                    id = entity.id,
                    preview = entity.preview,
                    timestampMillis = entity.timestampMillis,
                    tags = if (entity.tags.isEmpty()) emptyList() else entity.tags.split(","),
                    patternMatchId = entity.patternMatchId,
                    patternLabel = entity.patternLabel,
                    isFavourite = entity.isFavourite,
                    matchedText = entity.matchedText
                )
            }
        }
    }

    override suspend fun insert(entry: ClipboardEntry): Long = withContext(ioDispatcher) {
        // Enforce decryption check / or simple encrypt of full content during insert
        // The master prompt stores full raw content encrypted, with unencrypted preview.
        val rawText = entry.preview // The calling code passes raw text in entry.preview
        val encryptedPayload = encryptionService.encrypt(rawText)
        
        val wordLimitPreview = if (rawText.length > 100) rawText.take(97) + "..." else rawText

        val entity = ClipboardEntryEntity(
            encryptedContent = encryptedPayload.ciphertext,
            iv = encryptedPayload.iv,
            preview = wordLimitPreview,
            patternMatchId = entry.patternMatchId,
            patternLabel = entry.patternLabel,
            timestampMillis = entry.timestampMillis,
            tags = entry.tags.joinToString(","),
            isFavourite = entry.isFavourite,
            matchedText = entry.matchedText
        )
        clipboardDao.insert(entity)
    }

    override suspend fun getDecrypted(id: Long): String = withContext(ioDispatcher) {
        val entity = clipboardDao.getById(id) ?: return@withContext "Not found"
        runCatching {
            encryptionService.decrypt(entity.iv, entity.encryptedContent)
        }.getOrDefault(entity.preview)
    }

    override suspend fun delete(id: Long) = withContext(ioDispatcher) {
        clipboardDao.delete(id)
    }

    override suspend fun toggleFavourite(id: Long) = withContext(ioDispatcher) {
        clipboardDao.toggleFavourite(id)
    }

    override suspend fun clearAll() = withContext(ioDispatcher) {
        clipboardDao.clearAll()
    }
}

class PatternRepositoryImpl(
    private val regexPatternDao: RegexPatternDao,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) : PatternRepository {

    private val dbScope = CoroutineScope(SupervisorJob() + ioDispatcher)

    init {
        // Asynchronously check and populate built-insurance patterns on creation
        dbScope.launch {
            populateBuiltInIfNecessary()
        }
    }

    private suspend fun populateBuiltInIfNecessary() = withContext(ioDispatcher) {
        val existing = regexPatternDao.getActivePatterns()
        if (existing.isEmpty()) {
            getBuiltInPatterns().forEach {
                regexPatternDao.upsert(it.toEntity())
            }
        }
    }

    override fun observePatterns(): Flow<List<RegexPattern>> {
        return regexPatternDao.getAllPatternsFlow().map { list ->
            list.map { it.toDomain() }
        }
    }

    override suspend fun getActivePatterns(): List<RegexPattern> = withContext(ioDispatcher) {
        populateBuiltInIfNecessary() // Ensure they are populated
        regexPatternDao.getActivePatterns().map { it.toDomain() }
    }

    override suspend fun upsert(pattern: RegexPattern) = withContext(ioDispatcher) {
        regexPatternDao.upsert(pattern.toEntity())
    }

    override suspend fun delete(id: String) = withContext(ioDispatcher) {
        regexPatternDao.delete(id)
    }

    override suspend fun toggleEnabled(id: String) = withContext(ioDispatcher) {
        regexPatternDao.toggleEnabled(id)
    }

    override suspend fun incrementMatchCount(id: String) = withContext(ioDispatcher) {
        regexPatternDao.incrementMatchCount(id)
    }

    private fun RegexPatternEntity.toDomain() = RegexPattern(
        id = id,
        label = label,
        description = description,
        regex = regexPattern,
        actionType = actionType,
        actionPayload = actionPayload,
        isEnabled = isEnabled,
        priority = priority,
        createdAt = createdAt,
        matchCount = matchCount
    )

    private fun RegexPattern.toEntity() = RegexPatternEntity(
        id = id,
        label = label,
        description = description,
        regexPattern = regex,
        actionType = actionType,
        actionPayload = actionPayload,
        isEnabled = isEnabled,
        priority = priority,
        createdAt = createdAt,
        matchCount = matchCount
    )

    companion object {
        fun getBuiltInPatterns(): List<RegexPattern> {
            val now = System.currentTimeMillis()
            return listOf(
                RegexPattern(
                    id = "builtin_iban",
                    label = "IBAN Detected",
                    description = "International Bank Account Number validation and swift action",
                    regex = """[A-Z]{2}\d{2}[A-Z0-9]{12,30}""",
                    actionType = "OPEN_URL",
                    actionPayload = "https://www.ibancalculator.com/iban_validieren.html?tx_placalc_pi1[iban]={group0}",
                    isEnabled = true,
                    priority = 100,
                    createdAt = now
                ),
                RegexPattern(
                    id = "builtin_url",
                    label = "URL Link",
                    description = "Standard dynamic web address layout matcher",
                    regex = """https?://[^\s$.?#].[^\s]*""",
                    actionType = "OPEN_URL",
                    actionPayload = "{group0}",
                    isEnabled = true,
                    priority = 90,
                    createdAt = now - 1
                ),
                RegexPattern(
                    id = "builtin_tracking",
                    label = "Tracking Code",
                    description = "DHL / FedEx / UPS / USPS mail status tracker link",
                    regex = """\b(?:[0-9]{20}|[0-9]{15}|1Z[0-9A-Z]{16}|[0-9]{22}|[A-Z]{2}[0-9]{9}[A-Z]{2})\b""",
                    actionType = "OPEN_URL",
                    actionPayload = "https://parcelsapp.com/en/tracking/{group0}",
                    isEnabled = true,
                    priority = 85,
                    createdAt = now - 2
                ),
                RegexPattern(
                    id = "builtin_email",
                    label = "Email Address",
                    description = "Standard email contact detector",
                    regex = """[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\.[a-zA-Z]{2,}""",
                    actionType = "OPEN_URL",
                    actionPayload = "mailto:{group0}",
                    isEnabled = true,
                    priority = 80,
                    createdAt = now - 3
                ),
                RegexPattern(
                    id = "builtin_phone",
                    label = "Phone Number",
                    description = "Dynamic International number action dialer",
                    regex = """\+?[0-9\s\-()]{9,15}""",
                    actionType = "OPEN_URL",
                    actionPayload = "tel:{group0}",
                    isEnabled = true,
                    priority = 75,
                    createdAt = now - 4
                ),
                RegexPattern(
                    id = "builtin_json",
                    label = "JSON Blob",
                    description = "Automate formatting and minification of structured JSON strings",
                    regex = """^\s*[\[{].*[\]}]\s*$""",
                    actionType = "TRANSFORM",
                    actionPayload = "FORMAT_JSON",
                    isEnabled = true,
                    priority = 60,
                    createdAt = now - 5
                ),
                RegexPattern(
                    id = "builtin_coords",
                    label = "GPS Coordinates",
                    description = "Launch mapping and geo navigation details",
                    regex = """(-?\d{1,3}\.\d+),\s*(-?\d{1,3}\.\d+)""",
                    actionType = "OPEN_URL",
                    actionPayload = "geo:{group1},{group2}",
                    isEnabled = true,
                    priority = 95,
                    createdAt = now - 6
                )
            )
        }
    }
}
