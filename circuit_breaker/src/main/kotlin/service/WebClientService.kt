package com.jhzlo.service

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.WebClientResponseException
import reactor.core.publisher.Mono
import java.util.concurrent.atomic.AtomicInteger

@Service
class WebClientService(
    private val webClient: WebClient
) {
    companion object {
        const val API = "/api/random-error"
    }
    private val successCount = AtomicInteger(0)
    private val badRequestCount = AtomicInteger(0)
    private val internalServerErrorCount = AtomicInteger(0)
    private val circuitBreakerBlockedCount = AtomicInteger(0)

    @CircuitBreaker(name = "default", fallbackMethod = "fallbackResponse")
    fun fetchData(): Mono<String> {

        return webClient.get()
            .uri("/api/random-error")
            .retrieve()
            .onStatus({ status -> status.is4xxClientError }) { response ->
                // 400 에러
                badRequestCount.incrementAndGet()
                println("400 ERROR: ${response.statusCode()}")
                Mono.empty()
            }
            .onStatus({ status -> status.is5xxServerError }) { response ->
                // 500 에러
                internalServerErrorCount.incrementAndGet()
                println("500 ERROR: ${response.statusCode()}")
                Mono.empty()
            }
            .bodyToMono(String::class.java)
            .doOnSuccess {
                // 요청 성공
                successCount.incrementAndGet()
            }
            .doOnError { error ->
                // CircuitBreaker로 인해 실패
                if (error is io.github.resilience4j.circuitbreaker.CallNotPermittedException) {
                    circuitBreakerBlockedCount.incrementAndGet()
                }
            }
    }

    fun fallbackResponse(ex: Throwable): Mono<String> {
        return Mono.just("Fallback: (${ex.message})")
    }
}
