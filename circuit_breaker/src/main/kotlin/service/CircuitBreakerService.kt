package com.jhzlo.service

import com.jhzlo.dto.ResponseStats
import com.jhzlo.entity.RequestStats
import io.github.resilience4j.circuitbreaker.CallNotPermittedException
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker
import io.github.resilience4j.circuitbreaker.event.CircuitBreakerEvent
import jakarta.annotation.PostConstruct
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.WebClientResponseException

@Service
class CircuitBreakerService(
    private val webClient: WebClient,
    private val circuitBreakerRegistry: CircuitBreakerRegistry
) {
    companion object {
        private val logger = LoggerFactory.getLogger(CircuitBreakerService::class.java)
        const val API = "/api/random-error"
    }

    @PostConstruct
    fun initCircuitBreakerListener() {
        val circuitBreaker = circuitBreakerRegistry.circuitBreaker("default")

        circuitBreaker.eventPublisher
            .onStateTransition { event ->
                logger.info("CircuitBreaker State Transition: ${event.stateTransition}")
            }
            .onEvent { event: CircuitBreakerEvent ->
                logger.info("CircuitBreaker Event: ${event.eventType}")
            }
    }

    @CircuitBreaker(name = "default", fallbackMethod = "fallbackResponse")
    fun fetchData() {
        webClient.get()
            .uri(API)
            .retrieve()
            .bodyToMono(String::class.java)
            .block()

        RequestStats.incrementSuccess()
    }

    private fun fallbackResponse(exception: WebClientResponseException) {
        when (exception.statusCode.value()) {
            400 -> RequestStats.incrementBadRequest()
            500 -> RequestStats.incrementInternalServerError()
        }
    }

    private fun fallbackResponse(exception: CallNotPermittedException) {
        RequestStats.incrementCircuitBreakerBlocked()
        logger.warn("Circuit Breaker is OPEN. Request blocked.")
    }

    private fun fallback(throwable: Throwable) {
        logger.warn("Fallback method triggered due to: ${throwable.message}")
    }

    fun getStats(): ResponseStats {
        return RequestStats.getStats()
    }
}
