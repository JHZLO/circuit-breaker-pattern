package com.jhzlo.service

import com.jhzlo.dto.ResponseStats
import com.jhzlo.entity.RequestStats
import io.github.resilience4j.circuitbreaker.CallNotPermittedException
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.WebClientResponseException

@Service
class WebClientService(
    private val webClient: WebClient
) {
    companion object {
        const val API = "/api/random-error"
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
        println("Circuit Breaker open")
    }

    private fun fallback(throwable: Throwable) {
        println("default fallback method")
    }

    fun getStats(): ResponseStats {
        return RequestStats.getStats()
    }
}
