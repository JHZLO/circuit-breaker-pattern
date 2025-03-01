package com.jhzlo.service

import com.jhzlo.dto.ResponseStats
import com.jhzlo.entity.RequestStats
import com.jhzlo.repository.RequestStatsRepository
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker
import jakarta.transaction.Transactional
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClient
import reactor.core.publisher.Mono

@Service
class WebClientService(
    private val webClient: WebClient,
    private val requestStatsRepository: RequestStatsRepository
) {
    companion object {
        const val API = "/api/random-error"
    }

    @CircuitBreaker(name = "default", fallbackMethod = "fallbackResponse")
    @Transactional
    fun fetchData(id: Long): Mono<String> {
        val stats = requestStatsRepository.findById(id).orElseGet {
            RequestStats(id = id).also { requestStatsRepository.save(it) }
        }

        return webClient.get()
            .uri(API)
            .retrieve()
            .onStatus({ status -> status.is4xxClientError }) { response ->
                // 400 에러
                stats.incrementBadRequest()
                println("400 ERROR: ${response.statusCode()}")
                Mono.empty()
            }
            .onStatus({ status -> status.is5xxServerError }) { response ->
                // 500 에러
                stats.incrementInternalServerError()
                println("500 ERROR: ${response.statusCode()}")
                Mono.empty()
            }
            .bodyToMono(String::class.java)
            .doOnSuccess {
                // 요청 성공
                stats.incrementSuccess()
            }
            .doOnError { error ->
                // CircuitBreaker로 인해 실패
                if (error is io.github.resilience4j.circuitbreaker.CallNotPermittedException) {
                    stats.incrementCircuitBreakerBlocked()
                }
            }
    }

    fun fallbackResponse(ex: Throwable): Mono<String> {
        return Mono.just("Fallback: (${ex.message})")
    }

    fun getStats(id: Long): ResponseStats {
        val stats = requestStatsRepository.findById(id).orElseThrow {
            IllegalArgumentException("null")
        }
        val responseStats = ResponseStats(
            stats.successCount,
            stats.badRequestCount,
            stats.internalServerErrorCount,
            stats.circuitBreakerBlockedCount
        )
        return responseStats
    }
}
