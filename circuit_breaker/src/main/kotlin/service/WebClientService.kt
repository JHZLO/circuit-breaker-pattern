package com.jhzlo.service

import com.jhzlo.dto.ResponseStats
import com.jhzlo.entity.RequestStats
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker
import jakarta.transaction.Transactional
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClient
import reactor.core.publisher.Mono

@Service
class WebClientService(
    private val webClient: WebClient
) {
    companion object {
        const val API = "/api/random-error"
    }

    @CircuitBreaker(name = "default", fallbackMethod = "fallbackResponse")
    fun fetchData(): Mono<String> {
        return webClient.get()
            .uri(API)
            .retrieve()
            .onStatus({ status -> status.is4xxClientError }) {
                RequestStats.incrementBadRequest()
                Mono.empty()
            }
            .onStatus({ status -> status.is5xxServerError }) {
                RequestStats.incrementInternalServerError()
                Mono.empty()
            }
            .bodyToMono(String::class.java)
            .doOnSuccess {
                RequestStats.incrementSuccess()
            }
            .doOnError { error ->
                if (error is io.github.resilience4j.circuitbreaker.CallNotPermittedException) {
                    RequestStats.incrementCircuitBreakerBlocked()
                }
            }
    }

    fun fallbackResponse(ex: Throwable): Mono<String> {
        return Mono.just("Fallback: (${ex.message})")
    }

    fun getStats(): ResponseStats {
        return RequestStats.getStats()
    }
}
