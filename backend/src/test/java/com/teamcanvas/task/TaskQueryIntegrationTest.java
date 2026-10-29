package com.teamcanvas.task;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.teamcanvas.support.ApiTestSupport;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

/** Search, filters, sorting, pagination and the board. */
class TaskQueryIntegrationTest extends ApiTestSupport {

    private record Fixture(TestUser owner, TestUser admin, long projectId) {
    }

    /**
     * Four tasks:
     *   "Fix login bug"   URGENT, assigned to the admin, due 2026-11-05, TODO
     *   "Write docs"      LOW,    unassigned,                           TODO
     *   "Fix signup bug"  HIGH,   unassigned, due 2026-12-20,           IN_PROGRESS
     *   "100% coverage"   MEDIUM, unassigned,                           TODO
     */
    private Fixture fixture() throws Exception {
        TestUser owner = newUser();
        TestUser admin = newUser();
        long workspaceId = createWorkspace(owner);
        addMember(owner, workspaceId, admin, "ADMIN");
        long projectId = createProject(owner, workspaceId);

        createTask(owner, projectId, Map.of("title", "Fix login bug", "priority", "URGENT",
                "assigneeId", admin.id(), "dueDate", "2026-11-05"));
        createTask(owner, projectId, Map.of("title", "Write docs", "priority", "LOW"));
        createTask(owner, projectId, Map.of("title", "Fix signup bug", "priority", "HIGH",
                "status", "IN_PROGRESS", "dueDate", "2026-12-20"));
        createTask(owner, projectId, Map.of("title", "100% coverage", "priority", "MEDIUM"));
        return new Fixture(owner, admin, projectId);
    }

    private String listPath(long projectId) {
        return "/api/projects/" + projectId + "/tasks";
    }

    private String boardPath(long projectId) {
        return "/api/projects/" + projectId + "/board";
    }

    /** Filters go in as real request parameters, so characters such as % are sent exactly as typed. */
    private ResultActions query(TestUser user, String path, Map<String, String> params) throws Exception {
        MockHttpServletRequestBuilder request = MockMvcRequestBuilders.get(path)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + user.token());
        params.forEach((name, value) -> request.param(name, value));
        return mockMvc.perform(request);
    }

    // ---------------------------------------------------------------- board

    @Test
    void board_hasAllFourColumns_evenForAnEmptyProject() throws Exception {
        TestUser owner = newUser();
        long projectId = createProject(owner, createWorkspace(owner));

        query(owner, boardPath(projectId), Map.of())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.columns.TODO.length()").value(0))
                .andExpect(jsonPath("$.columns.IN_PROGRESS.length()").value(0))
                .andExpect(jsonPath("$.columns.IN_REVIEW.length()").value(0))
                .andExpect(jsonPath("$.columns.DONE.length()").value(0))
                .andExpect(jsonPath("$.truncated").value(false));
    }

    @Test
    void board_groupsTasksByColumn_andAppliesFilters() throws Exception {
        Fixture f = fixture();

        query(f.owner(), boardPath(f.projectId()), Map.of())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.columns.TODO.length()").value(3))
                .andExpect(jsonPath("$.columns.IN_PROGRESS.length()").value(1))
                .andExpect(jsonPath("$.columns.DONE.length()").value(0));

        query(f.owner(), boardPath(f.projectId()), Map.of("priority", "URGENT"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.columns.TODO.length()").value(1))
                .andExpect(jsonPath("$.columns.IN_PROGRESS.length()").value(0));
    }

    // ---------------------------------------------------------------- search and filters

    @Test
    void search_filtersCombine() throws Exception {
        Fixture f = fixture();
        String path = listPath(f.projectId());

        query(f.owner(), path, Map.of()).andExpect(jsonPath("$.totalElements").value(4));
        query(f.owner(), path, Map.of("q", "BUG")).andExpect(jsonPath("$.totalElements").value(2));   // case-insensitive
        query(f.owner(), path, Map.of("q", "bug", "priority", "URGENT"))
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].title").value("Fix login bug"));
        query(f.owner(), path, Map.of("status", "IN_PROGRESS"))
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].title").value("Fix signup bug"));
        query(f.owner(), path, Map.of("assigneeId", String.valueOf(f.admin().id())))
                .andExpect(jsonPath("$.totalElements").value(1));
        query(f.owner(), path, Map.of("unassigned", "true")).andExpect(jsonPath("$.totalElements").value(3));
        query(f.owner(), path, Map.of("priority", "LOW")).andExpect(jsonPath("$.totalElements").value(1));
        query(f.owner(), path, Map.of("dueFrom", "2026-11-01", "dueTo", "2026-11-30"))
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].title").value("Fix login bug"));
        query(f.owner(), path, Map.of("dueFrom", "2026-12-01")).andExpect(jsonPath("$.totalElements").value(1));
    }

    /** "%" and "_" typed by a user must be plain characters, not wildcards. */
    @Test
    void search_treatsPercentAndUnderscoreAsPlainText() throws Exception {
        Fixture f = fixture();
        String path = listPath(f.projectId());

        query(f.owner(), path, Map.of("q", "%"))
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].title").value("100% coverage"));
        query(f.owner(), path, Map.of("q", "100%")).andExpect(jsonPath("$.totalElements").value(1));
        query(f.owner(), path, Map.of("q", "_")).andExpect(jsonPath("$.totalElements").value(0));
    }

    // ---------------------------------------------------------------- sorting and pagination

    @Test
    void search_sortsAndPaginates() throws Exception {
        Fixture f = fixture();
        String path = listPath(f.projectId());

        query(f.owner(), path, Map.of("sortBy", "title", "direction", "asc"))
                .andExpect(jsonPath("$.content[0].title").value("100% coverage"))
                .andExpect(jsonPath("$.content[3].title").value("Write docs"));
        query(f.owner(), path, Map.of("sortBy", "title", "direction", "desc"))
                .andExpect(jsonPath("$.content[0].title").value("Write docs"));

        query(f.owner(), path, Map.of("size", "2", "page", "0", "sortBy", "title", "direction", "asc"))
                .andExpect(jsonPath("$.content.length()").value(2))
                .andExpect(jsonPath("$.totalElements").value(4))
                .andExpect(jsonPath("$.totalPages").value(2));
        query(f.owner(), path, Map.of("size", "2", "page", "1", "sortBy", "title", "direction", "asc"))
                .andExpect(jsonPath("$.content.length()").value(2))
                .andExpect(jsonPath("$.content[0].title").value("Fix signup bug"));
    }

    @Test
    void search_rejectsBadParameters() throws Exception {
        Fixture f = fixture();
        String path = listPath(f.projectId());

        query(f.owner(), path, Map.of("sortBy", "hacker")).andExpect(status().isBadRequest());
        query(f.owner(), path, Map.of("direction", "sideways")).andExpect(status().isBadRequest());
        query(f.owner(), path, Map.of("dueFrom", "2026-12-01", "dueTo", "2026-11-01"))
                .andExpect(status().isBadRequest());
        query(f.owner(), path, Map.of("unassigned", "true", "assigneeId", "1")).andExpect(status().isBadRequest());
        query(f.owner(), path, Map.of("dueFrom", "31-12-2026")).andExpect(status().isBadRequest());
        query(f.owner(), path, Map.of("status", "NOPE")).andExpect(status().isBadRequest());
    }

    // ---------------------------------------------------------------- access

    @Test
    void outsider_gets404_forListAndBoard() throws Exception {
        Fixture f = fixture();
        TestUser outsider = newUser();

        query(outsider, listPath(f.projectId()), Map.of())
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Project not found"));
        query(outsider, boardPath(f.projectId()), Map.of())
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Project not found"));
    }
}