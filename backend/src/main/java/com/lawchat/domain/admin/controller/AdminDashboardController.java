package com.lawchat.domain.admin.controller;

import com.lawchat.domain.admin.dto.response.AdminDashboardStatsResponse;
import com.lawchat.domain.admin.service.AdminDashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/dashboard")
public class AdminDashboardController {

    private final AdminDashboardService adminDashboardService;

    @GetMapping("/stats")
    public ResponseEntity<AdminDashboardStatsResponse> getStats(@AuthenticationPrincipal Long userId) {
        return ResponseEntity.ok(adminDashboardService.getStats(userId));
    }
}