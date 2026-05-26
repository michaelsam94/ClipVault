package com.michael.clipvault.playstore

import com.michael.clipvault.core.domain.ClipboardEntry
import com.michael.clipvault.core.domain.RegexPattern

object PlayStoreTestFixtures {
    val mockPatterns = listOf(
        RegexPattern(
            id = "iban_filter",
            label = "IBAN Scanner",
            description = "Flags international bank account numbers",
            regex = "[A-Z]{2}\\d{2}[A-Z0-9]{11,30}",
            actionType = "COPY_GROUP",
            actionPayload = "0",
            isEnabled = true,
            priority = 100,
            createdAt = System.currentTimeMillis() - 100000,
            matchCount = 12
        ),
        RegexPattern(
            id = "email_filter",
            label = "Email Address",
            description = "Extracts email contact coordinates",
            regex = "[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}",
            actionType = "SHARE",
            actionPayload = "",
            isEnabled = true,
            priority = 90,
            createdAt = System.currentTimeMillis() - 50000,
            matchCount = 5
        ),
        RegexPattern(
            id = "url_filter",
            label = "Auto URL Opener",
            description = "Opens secure URL links in browser",
            regex = "https://[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}.*",
            actionType = "OPEN_URL",
            actionPayload = "{group0}",
            isEnabled = true,
            priority = 80,
            createdAt = System.currentTimeMillis() - 10000,
            matchCount = 42
        )
    )

    val mockHistory = listOf(
        ClipboardEntry(
            id = 1L,
            preview = "DE89 3704 0044 0532 0130 00",
            timestampMillis = System.currentTimeMillis() - 60000,
            tags = listOf("IBAN"),
            patternMatchId = "iban_filter",
            patternLabel = "IBAN Scanner",
            isFavourite = true,
            matchedText = "DE89370400440532013000"
        ),
        ClipboardEntry(
            id = 2L,
            preview = "Hi Michael, please contact support@clipvault.app for...",
            timestampMillis = System.currentTimeMillis() - 120000,
            tags = listOf("Email"),
            patternMatchId = "email_filter",
            patternLabel = "Email Address",
            isFavourite = false,
            matchedText = "support@clipvault.app"
        ),
        ClipboardEntry(
            id = 3L,
            preview = "https://github.com/michaelsam94/ClipVault",
            timestampMillis = System.currentTimeMillis() - 180000,
            tags = listOf("URL"),
            patternMatchId = "url_filter",
            patternLabel = "Auto URL Opener",
            isFavourite = false,
            matchedText = "https://github.com/michaelsam94/ClipVault"
        ),
        ClipboardEntry(
            id = 4L,
            preview = "Secure API Key: a2b9c8d7e6f5g4h3i2j1",
            timestampMillis = System.currentTimeMillis() - 240000,
            tags = listOf("Secret"),
            patternMatchId = null,
            patternLabel = null,
            isFavourite = false,
            matchedText = null
        )
    )
}
