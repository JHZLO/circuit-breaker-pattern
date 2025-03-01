package com.jhzlo.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.reactive.function.client.WebClient

@Configuration
class WebClientConfig {
    companion object {
        const val BASE_URL = "http://localhost:10001"
    }

    @Bean
    fun webClient(): WebClient {
        return WebClient.builder()
            .baseUrl(BASE_URL)
            .defaultHeader("Content-Type", "application/json")
            .build()
    }
}
