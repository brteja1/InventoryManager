package com.inventorymanager.app.data.local

import java.math.BigDecimal
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.UUID

fun String.toCentsOrNull(): Long? {
    if (isBlank()) return null
    return runCatching {
        BigDecimal(trim()).movePointRight(2).longValueExact()
    }.getOrNull()
}

fun String.toEpochDayOrNull(): Long? {
    if (isBlank()) return null
    return runCatching {
        val dateFormatter = DateTimeFormatter.ofPattern("dd-MM-yyyy")
        LocalDate.parse(trim(), dateFormatter).toEpochDay()
    }.getOrNull()
}

fun Long.ifZero(fallback: Long): Long = if (this == 0L) fallback else this

fun generateUid(): String = UUID.randomUUID().toString().uppercase().replace("-", "").take(12)
