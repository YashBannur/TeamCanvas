package com.teamcanvas.support;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

/**
 * Shared helpers for the API integration tests. Every test creates its own users with random e-mail addresses,
 * so tests never collide and the "teamcanvas_test" database never needs to be reset.
 * All subclasses share ONE Spring context (same configuration), so only the first test class pays the startup cost.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public abstract class ApiTestSupport {

    protected static final String PASSWORD = "Password1";

    @Autowired
    protected MockMvc mockMvc;
    @Autowired
    protected ObjectMapper objectMapper;
    @Autowired
    protected JdbcTemplate jdbcTemplate;

    /** A registered, logged-in user. */
    public record TestUser(long id, String email, String token) {
    }

    // ---------------------------------------------------------------- users

    protected TestUser newUser() throws Exception {
        String email = "user-" + UUID.randomUUID() + "@example.com";

        String registerBody = objectMapper.writeValueAsString(
                Map.of("name", "Test User", "email", email, "password", PASSWORD));
        MvcResult registered = mockMvc.perform(MockMvcRequestBuilders.post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registerBody))
                .andExpect(status().isCreated())
                .andReturn();
        long id = objectMapper.readTree(registered.getResponse().getContentAsString()).get("id").asLong();

        String loginBody = objectMapper.writeValueAsString(Map.of("email", email, "password", PASSWORD));
        MvcResult loggedIn = mockMvc.perform(MockMvcRequestBuilders.post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginBody))
                .andExpect(status().isOk())
                .andReturn();
        String token = objectMapper.readTree(loggedIn.getResponse().getContentAsString())
                .get("accessToken").asText();

        return new TestUser(id, email, token);
    }

    // ---------------------------------------------------------------- authenticated requests

    protected ResultActions get(TestUser user, String url) throws Exception {
        return send(MockMvcRequestBuilders.get(url), user, null);
    }

    protected ResultActions post(TestUser user, String url, Object body) throws Exception {
        return send(MockMvcRequestBuilders.post(url), user, body);
    }

    protected ResultActions patch(TestUser user, String url, Object body) throws Exception {
        return send(MockMvcRequestBuilders.patch(url), user, body);
    }

    protected ResultActions delete(TestUser user, String url) throws Exception {
        return send(MockMvcRequestBuilders.delete(url), user, null);
    }

    private ResultActions send(MockHttpServletRequestBuilder builder, TestUser user, Object body) throws Exception {
        builder.header(HttpHeaders.AUTHORIZATION, "Bearer " + user.token());
        if (body != null) {
            builder.contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(body));
        }
        return mockMvc.perform(builder);
    }

    // ---------------------------------------------------------------- reading responses

    protected JsonNode json(ResultActions actions) throws Exception {
        return objectMapper.readTree(actions.andReturn().getResponse().getContentAsString());
    }

    protected long idOf(ResultActions actions) throws Exception {
        return json(actions).get("id").asLong();
    }

    // ---------------------------------------------------------------- domain set-up helpers

    protected long createWorkspace(TestUser owner) throws Exception {
        return idOf(post(owner, "/api/workspaces", Map.of("name", "Acme " + UUID.randomUUID()))
                .andExpect(status().isCreated()));
    }

    /** Makes "user" a member through the real flow: the owner invites, the user accepts. */
    protected void addMember(TestUser owner, long workspaceId, TestUser user, String role) throws Exception {
        long invitationId = idOf(post(owner, "/api/workspaces/" + workspaceId + "/invitations",
                Map.of("email", user.email(), "role", role))
                .andExpect(status().isCreated()));
        post(user, "/api/invitations/" + invitationId + "/accept", null).andExpect(status().isOk());
    }

    protected long createProject(TestUser adminOrOwner, long workspaceId) throws Exception {
        return idOf(post(adminOrOwner, "/api/workspaces/" + workspaceId + "/projects",
                Map.of("name", "Project " + UUID.randomUUID()))
                .andExpect(status().isCreated()));
    }

    protected long createTask(TestUser user, long projectId, Map<String, ?> body) throws Exception {
        return idOf(post(user, "/api/projects/" + projectId + "/tasks", body).andExpect(status().isCreated()));
    }
}