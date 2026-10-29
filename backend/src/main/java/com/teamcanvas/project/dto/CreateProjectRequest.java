package com.teamcanvas.project.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

public record CreateProjectRequest(
        @NotBlank(message = "Name is required")
        @Size(max = 150, message = "Name must be at most 150 characters")
        String name,

        @Size(max = 2000, message = "Description must be at most 2000 characters")
        String description,

        LocalDate startDate,

        LocalDate dueDate) {
}
