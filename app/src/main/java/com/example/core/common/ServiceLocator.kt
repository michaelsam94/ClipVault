package com.example.core.common

import android.content.Context
import com.example.core.database.*
import com.example.core.datastore.PreferencesManager
import com.example.core.domain.ClipboardHistoryRepository
import com.example.core.domain.PatternRepository
import kotlinx.coroutines.Dispatchers

object ServiceLocator {
    private var database: AppDatabase? = null
    private var encryptionService: EncryptionService? = null
    private var historyRepository: ClipboardHistoryRepository? = null
    private var patternRepository: PatternRepository? = null
    private var preferencesManager: PreferencesManager? = null

    fun initialize(context: Context) {
        if (database == null) {
            val db = AppDatabase.getDatabase(context)
            database = db
            val crypto = AndroidKeystoreEncryptionService()
            encryptionService = crypto
            historyRepository = ClipboardHistoryRepositoryImpl(db.clipboardDao(), crypto, Dispatchers.IO)
            patternRepository = PatternRepositoryImpl(db.regexPatternDao(), Dispatchers.IO)
            preferencesManager = PreferencesManager(context)
        }
    }

    fun getHistoryRepository(): ClipboardHistoryRepository {
        return historyRepository ?: throw IllegalStateException("ServiceLocator has not been initialized.")
    }

    fun getPatternRepository(): PatternRepository {
        return patternRepository ?: throw IllegalStateException("ServiceLocator has not been initialized.")
    }

    fun getPreferencesManager(): PreferencesManager {
        return preferencesManager ?: throw IllegalStateException("ServiceLocator has not been initialized.")
    }

    fun getEncryptionService(): EncryptionService {
        return encryptionService ?: throw IllegalStateException("ServiceLocator has not been initialized.")
    }
}
