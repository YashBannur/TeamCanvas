package com.teamcanvas.workspace.dto;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Partial update: a field that is missing (null) is left unchanged.
 * An empty description ("") clears the description.
 */
public record UpdateWorkspaceRequest(
        @Size(max = 100, message = "Name must be at most 100 characters")
        @Pattern(regexp = ".*\\S.*", message = "Name must not be blank")
        String name,

        @Size(max = 500, message = "Description must be at most 500 characters")
        String description) {
}
