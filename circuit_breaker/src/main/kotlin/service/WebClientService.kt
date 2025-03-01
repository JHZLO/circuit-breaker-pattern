package com.jhzlo.service

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker
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
            .uri("/api/random-error")
            .retrieve()
            .bodyToMono(String::class.java)
    }

    fun fallbackResponse(ex: Throwable): Mono<String> {
        return Mono.just("Fallback: (${ex.message})")
    }
}
