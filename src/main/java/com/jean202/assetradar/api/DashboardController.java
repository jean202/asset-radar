package com.jean202.assetradar.api;

import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class DashboardController {
    @GetMapping("/api/dashboard")
    public Map<String, String> dashboard() {
        return Map.of(
                "status", "bootstrapping",
                "message", "asset-radar skeleton is ready"
        );
    }
}
