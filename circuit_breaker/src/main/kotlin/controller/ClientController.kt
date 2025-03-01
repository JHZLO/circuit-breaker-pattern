package com.jhzlo.controller

import com.jhzlo.dto.ResponseStats
import com.jhzlo.service.WebClientService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/client")
class ClientController(
    private val webClientService: WebClientService
) {
    @GetMapping("/test")
    fun callExternalApi(): String {
        return webClientService.fetchData().block() ?: "Error"
    }

    @GetMapping("/stats/{id}")
    fun getStats(@PathVariable id: Long): ResponseEntity<ResponseStats> {
        val response = webClientService.getStats(id)
        return ResponseEntity.ok(response);
    }
}
