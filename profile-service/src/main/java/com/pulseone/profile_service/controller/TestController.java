package com.pulseone.profile_service.controller;

import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.http.ResponseEntity;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/test")
@CrossOrigin(origins = "*") // Additional CORS annotation for extra safety
public class TestController {

    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> testHealth() {
        Map<String, Object> response = new HashMap<>();
        response.put("status", "UP");
        response.put("message", "Profile Service is running on port 8082");
        response.put("timestamp", System.currentTimeMillis());
        response.put("service", "profile-service");
        return ResponseEntity.ok(response);
    }

    @GetMapping("/cors")
    public ResponseEntity<Map<String, String>> testCors() {
        Map<String, String> response = new HashMap<>();
        response.put("message", "CORS is working properly!");
        response.put("service", "profile-service");
        response.put("port", "8082");
        return ResponseEntity.ok(response);
    }
}