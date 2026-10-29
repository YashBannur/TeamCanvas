package com.teamcanvas.project;

import com.teamcanvas.project.dto.CreateProjectRequest;
import com.teamcanvas.project.dto.ProjectResponse;
import com.teamcanvas.project.dto.UpdateProjectRequest;
import com.teamcanvas.security.UserPrincipal;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** Thin controller: the caller's id always comes from the verified JWT. */
@RestController
@RequestMapping("/api")
public class ProjectController {

    private final ProjectService projectService;

    public ProjectController(ProjectService projectService) {
        this.projectService = projectService;
    }

    @PostMapping("/workspaces/{workspaceId}/projects")
    @ResponseStatus(HttpStatus.CREATED)
    public ProjectResponse create(@AuthenticationPrincipal UserPrincipal principal,
                                  @PathVariable Long workspaceId,
                                  @Valid @RequestBody CreateProjectRequest request) {
        return projectService.create(workspaceId, principal.getId(), request);
    }

    @GetMapping("/workspaces/{workspaceId}/projects")
    public List<ProjectResponse> list(@AuthenticationPrincipal UserPrincipal principal,
                                      @PathVariable Long workspaceId,
                                      @RequestParam(required = false) ProjectStatus status) {
        return projectService.listForWorkspace(workspaceId, principal.getId(), status);
    }

    @GetMapping("/projects/{projectId}")
    public ProjectResponse getOne(@AuthenticationPrincipal UserPrincipal principal,
                                  @PathVariable Long projectId) {
        return projectService.getOne(projectId, principal.getId());
    }

    @PatchMapping("/projects/{projectId}")
    public ProjectResponse update(@AuthenticationPrincipal UserPrincipal principal,
                                  @PathVariable Long projectId,
                                  @Valid @RequestBody UpdateProjectRequest request) {
        return projectService.update(projectId, principal.getId(), request);
    }

    @DeleteMapping("/projects/{projectId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@AuthenticationPrincipal UserPrincipal principal, @PathVariable Long projectId) {
        projectService.delete(projectId, principal.getId());
    }
}
