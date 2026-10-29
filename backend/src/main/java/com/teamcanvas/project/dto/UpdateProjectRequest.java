package com.teamcanvas.project.dto;

import com.teamcanvas.project.ProjectStatus;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

/**
 * Partial update: a field that is missing (null) is left unchanged.
 * An empty description ("") clears the description. Dates cannot be cleared in the MVP.
 * Setting "status" to ARCHIVED / ACTIVE archives / restores the project.
 */
public record UpdateProjectRequest(
        @Size(max = 150, message = "Name must be at most 150 characters")
        @Pattern(regexp = ".*\\S.*", message = "Name must not be blank")
        String name,

        @Size(max = 2000, message = "Description must be at most 2000 characters")
        String description,

        LocalDate startDate,

        LocalDate dueDate,

        ProjectStatus status) {
}
