package com.teamcanvas.project;

import com.teamcanvas.project.dto.ProjectAnalyticsResponse;
import com.teamcanvas.security.UserPrincipal;
import java.time.LocalDate;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/projects")
public class ProjectAnalyticsController {

    private final ProjectAnalyticsService analyticsService;

    public ProjectAnalyticsController(ProjectAnalyticsService analyticsService) {
        this.analyticsService = analyticsService;
    }

    @GetMapping("/{projectId}/analytics")
    public ProjectAnalyticsResponse getAnalytics(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long projectId,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return analyticsService.getAnalytics(projectId, principal.getId(), from, to);
    }
}