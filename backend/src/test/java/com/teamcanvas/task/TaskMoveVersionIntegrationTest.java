package com.teamcanvas.task;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.teamcanvas.support.ApiTestSupport;
import java.util.Map;
import org.junit.jupiter.api.Test;

class TaskMoveVersionIntegrationTest extends ApiTestSupport {

    @Test
    void movingTaskDoesNotChangeSiblingTaskVersion() throws Exception {
        TestUser owner = newUser();
        long workspaceId = createWorkspace(owner);
        long projectId = createProject(owner, workspaceId);

        long taskA = createTask(owner, projectId, Map.of("title", "Task A"));
        long taskB = createTask(owner, projectId, Map.of("title", "Task B"));

        String taskBUrl = "/api/tasks/" + taskB;
        long taskBVersionBeforeMove = json(get(owner, taskBUrl)
                .andExpect(status().isOk()))
                .get("version")
                .asLong();

        patch(owner, "/api/tasks/" + taskA + "/move",
                Map.of("status", "TODO", "afterTaskId", taskB))
                .andExpect(status().isOk());

        get(owner, "/api/projects/" + projectId + "/board")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.columns.TODO[0].id").value(taskB))
                .andExpect(jsonPath("$.columns.TODO[1].id").value(taskA));

        long taskBVersionAfterMove = json(get(owner, taskBUrl)
                .andExpect(status().isOk()))
                .get("version")
                .asLong();

        assertEquals(taskBVersionBeforeMove, taskBVersionAfterMove,
                "Reordering a sibling must not increment this task's content version");
    }
}