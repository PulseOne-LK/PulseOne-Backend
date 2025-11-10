package com.pulseone.profile_service.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.http.ResponseEntity;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/test")
@Tag(name = "Health Check", description = "Service health monitoring endpoints")
public class TestController {

    @Operation(summary = "Service health check", description = "Check if the profile service is running and healthy")
    @ApiResponse(responseCode = "200", description = "Service is healthy", content = @Content(schema = @Schema(implementation = Map.class)))
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> testHealth() {
        Map<String, Object> response = new HashMap<>();
        response.put("status", "UP");
        response.put("message", "Profile Service is running on port 8082");
        response.put("timestamp", System.currentTimeMillis());
        response.put("service", "profile-service");
        return ResponseEntity.ok(response);
    }
}