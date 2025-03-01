package com.jhzlo.dto

data class ResponseStats (
    val successCount: Int,
    val badRequestCount: Int,
    val internalServerErrorCount: Int,
    val circuitBreakerBlockedCount: Int
)
