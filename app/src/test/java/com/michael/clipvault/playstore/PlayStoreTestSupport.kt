package com.michael.clipvault.playstore

import android.content.Context
import com.michael.clipvault.core.common.ServiceLocator
import com.michael.clipvault.core.datastore.PreferencesManager
import com.michael.clipvault.core.domain.ClipboardEntry
import com.michael.clipvault.core.database.EncryptionService
import com.michael.clipvault.core.database.EncryptedPayload
import com.michael.clipvault.core.domain.ClipboardHistoryRepository
import com.michael.clipvault.core.domain.PatternRepository
import com.michael.clipvault.core.domain.RegexPattern
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

class FakeClipboardHistoryRepository(
    private val items: List<ClipboardEntry> = PlayStoreTestFixtures.mockHistory
) : ClipboardHistoryRepository {
    private val flow = MutableStateFlow(items)
    
    override fun observeHistory(query: String): Flow<List<ClipboardEntry>> = flow
    override suspend fun insert(entry: ClipboardEntry): Long = 0
    override suspend fun getDecrypted(id: Long): String {
        return items.firstOrNull { it.id == id }?.preview ?: "Decrypted Payload Sample"
    }
    override suspend fun delete(id: Long) {}
    override suspend fun toggleFavourite(id: Long) {}
    override suspend fun clearAll() {}
}

class FakePatternRepository(
    private val items: List<RegexPattern> = PlayStoreTestFixtures.mockPatterns
) : PatternRepository {
    private val flow = MutableStateFlow(items)
    
    override fun observePatterns(): Flow<List<RegexPattern>> = flow
    override suspend fun getActivePatterns(): List<RegexPattern> = items.filter { it.isEnabled }
    override suspend fun upsert(pattern: RegexPattern) {}
    override suspend fun delete(id: String) {}
    override suspend fun toggleEnabled(id: String) {}
    override suspend fun incrementMatchCount(id: String) {}
}

class FakeEncryptionService : EncryptionService {
    override fun encrypt(plaintext: String): EncryptedPayload = EncryptedPayload(ByteArray(0), ByteArray(0))
    override fun decrypt(iv: ByteArray, ciphertext: ByteArray): String = "Decrypted"
}

object PlayStoreTestSupport {
    fun seedPlayStoreEnvironment(context: Context) {
        ServiceLocator.resetForTesting()
        val historyRepo = FakeClipboardHistoryRepository()
        val patternRepo = FakePatternRepository()
        val prefsManager = PreferencesManager(context)
        val crypto = FakeEncryptionService()
        
        ServiceLocator.setRepositoriesForTesting(
            historyRepo = historyRepo,
            patternRepo = patternRepo,
            prefsManager = prefsManager,
            crypto = crypto
        )
    }
}
