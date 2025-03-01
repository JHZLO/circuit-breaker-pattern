package com.jhzlo.entity

import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table

@Entity
@Table(name = "request_stats")
data class RequestStats(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    var successCount: Int = 0,
    var badRequestCount: Int = 0,
    var internalServerErrorCount: Int = 0,
    var circuitBreakerBlockedCount: Int = 0
) {
    fun incrementSuccess() {
        successCount++
    }

    fun incrementBadRequest() {
        badRequestCount++
    }

    fun incrementInternalServerError() {
        internalServerErrorCount++
    }

    fun incrementCircuitBreakerBlocked() {
        circuitBreakerBlockedCount++
    }
}
