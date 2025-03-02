package com.jhzlo.entity

import com.jhzlo.dto.ResponseStats
import java.util.concurrent.atomic.AtomicInteger

object RequestStats {
    private val successCount = AtomicInteger(0)
    private val badRequestCount = AtomicInteger(0)
    private val internalServerErrorCount = AtomicInteger(0)
    private val circuitBreakerBlockedCount = AtomicInteger(0)

    fun incrementSuccess() = successCount.incrementAndGet()
    fun incrementBadRequest() = badRequestCount.incrementAndGet()
    fun incrementInternalServerError() = internalServerErrorCount.incrementAndGet()
    fun incrementCircuitBreakerBlocked() = circuitBreakerBlockedCount.incrementAndGet()
    fun getStats(): ResponseStats {
        return ResponseStats(
            successCount.get(),
            badRequestCount.get(),
            internalServerErrorCount.get(),
            circuitBreakerBlockedCount.get()
        )
    }
}
