package com.jhzlo.controller

import com.jhzlo.dto.ResponseStats
import com.jhzlo.service.CircuitBreakerService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/client")
class ClientController(
    private val circuitBreakerService: CircuitBreakerService
) {
    @GetMapping("/test")
    fun callExternalApi() {
        circuitBreakerService.fetchData()
    }

    @GetMapping("/stats")
    fun getStats(): ResponseEntity<ResponseStats> {
        val response = circuitBreakerService.getStats()
        return ResponseEntity.ok(response);
    }
}
