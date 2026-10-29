package com.teamcanvas.project.dto;

import java.time.LocalDate;

public record CompletionTrendPoint(
        LocalDate date,
        long completedTasks) {
}