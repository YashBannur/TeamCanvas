package com.teamcanvas.auth;

import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.Cookie;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

/**
 * End-to-end test through the real security filter chain, controllers, services, Hibernate and MySQL.
 * It uses the separate "teamcanvas_test" database (see application-test.yml). Flyway creates the schema, then
 * Hibernate validates the entities against it, so a schema/entity mismatch fails the whole class at startup.
 * Every test uses a random e-mail address, so runs never collide with each other.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuthIntegrationTest {

    private static final String PASSWORD = "Password1";
    private static final Pattern REFRESH_COOKIE = Pattern.compile("tc_refresh=([^;]*)");

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;

    // ---------- registration ----------

    @Test
    void register_createsUser_andNeverReturnsPasswordData() throws Exception {
        String email = uniqueEmail();

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("name", "Yash", "email", email, "password", PASSWORD))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.email").value(email))
                .andExpect(jsonPath("$.password").doesNotExist())
                .andExpect(jsonPath("$.passwordHash").doesNotExist());
    }

    @Test
    void register_duplicateEmail_ignoringCase_returns409() throws Exception {
        String email = uniqueEmail();
        register(email);

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("name", "Other", "email", email.toUpperCase(), "password", PASSWORD))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("An account with this email already exists"));
    }

    @Test
    void register_invalidInput_returns400WithFieldErrors() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("name", "", "email", "not-an-email", "password", "short"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Validation failed"))
                .andExpect(jsonPath("$.fieldErrors[?(@.field == 'email')]").exists())
                .andExpect(jsonPath("$.fieldErrors[?(@.field == 'password')]").exists())
                .andExpect(jsonPath("$.fieldErrors[?(@.field == 'name')]").exists());
    }

    @Test
    void register_malformedJson_returns400WithoutLeakingDetails() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{ this is not json"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Bad Request"));
    }

    // ---------- login ----------

    @Test
    void login_success_returnsAccessToken_andHttpOnlyRefreshCookie() throws Exception {
        String email = uniqueEmail();
        register(email);

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("email", email, "password", PASSWORD))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.user.email").value(email))
                .andExpect(jsonPath("$.refreshToken").doesNotExist())
                .andExpect(header().string(HttpHeaders.SET_COOKIE, containsString("tc_refresh=")))
                .andExpect(header().string(HttpHeaders.SET_COOKIE, containsString("HttpOnly")))
                .andExpect(header().string(HttpHeaders.SET_COOKIE, containsString("Path=/api/auth")))
                .andExpect(header().string(HttpHeaders.SET_COOKIE, containsString("SameSite=Lax")));
    }

    @Test
    void login_wrongPassword_returns401WithGenericMessage() throws Exception {
        String email = uniqueEmail();
        register(email);

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("email", email, "password", "WrongPassword9"))))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Invalid email or password"));
    }

    @Test
    void login_unknownEmail_returnsExactlyTheSame401() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("email", uniqueEmail(), "password", PASSWORD))))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Invalid email or password"));
    }

    // ---------- protected endpoint ----------

    @Test
    void me_withoutToken_returns401() throws Exception {
        mockMvc.perform(get("/api/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));
    }

    @Test
    void me_withGarbageToken_returns401() throws Exception {
        mockMvc.perform(get("/api/me").header(HttpHeaders.AUTHORIZATION, "Bearer garbage.token.value"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void me_withValidToken_returnsTheCurrentUser() throws Exception {
        String email = uniqueEmail();
        register(email);
        String accessToken = accessTokenFrom(login(email));

        mockMvc.perform(get("/api/me").header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value(email))
                .andExpect(jsonPath("$.passwordHash").doesNotExist());
    }

    // ---------- refresh rotation and logout ----------

    @Test
    void refresh_rotatesTheToken_andTheOldOneStopsWorking() throws Exception {
        String email = uniqueEmail();
        register(email);
        String firstCookie = refreshCookieValue(login(email));

        MvcResult refreshed = mockMvc.perform(post("/api/auth/refresh").cookie(new Cookie("tc_refresh", firstCookie)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andReturn();
        String secondCookie = refreshCookieValue(refreshed);
        assertNotEquals(firstCookie, secondCookie, "refresh must issue a different token");

        // the new access token works
        mockMvc.perform(get("/api/me").header(HttpHeaders.AUTHORIZATION, "Bearer " + accessTokenFrom(refreshed)))
                .andExpect(status().isOk());

        // the already-used token is rejected
        mockMvc.perform(post("/api/auth/refresh").cookie(new Cookie("tc_refresh", firstCookie)))
                .andExpect(status().isUnauthorized());

        // the new one still works
        mockMvc.perform(post("/api/auth/refresh").cookie(new Cookie("tc_refresh", secondCookie)))
                .andExpect(status().isOk());
    }

    @Test
    void refresh_withoutCookie_returns401() throws Exception {
        mockMvc.perform(post("/api/auth/refresh"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void logout_revokesTheRefreshToken_andClearsTheCookie() throws Exception {
        String email = uniqueEmail();
        register(email);
        String cookie = refreshCookieValue(login(email));

        mockMvc.perform(post("/api/auth/logout").cookie(new Cookie("tc_refresh", cookie)))
                .andExpect(status().isNoContent())
                .andExpect(header().string(HttpHeaders.SET_COOKIE, containsString("Max-Age=0")));

        mockMvc.perform(post("/api/auth/refresh").cookie(new Cookie("tc_refresh", cookie)))
                .andExpect(status().isUnauthorized());
    }

    // ---------- helpers ----------

    private String uniqueEmail() {
        return "user-" + UUID.randomUUID() + "@example.com";
    }

    private String json(Object value) throws Exception {
        return objectMapper.writeValueAsString(value);
    }

    private void register(String email) throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("name", "Test User", "email", email, "password", PASSWORD))))
                .andExpect(status().isCreated());
    }

    private MvcResult login(String email) throws Exception {
        return mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("email", email, "password", PASSWORD))))
                .andExpect(status().isOk())
                .andReturn();
    }

    private String accessTokenFrom(MvcResult result) throws Exception {
        return objectMapper.readTree(result.getResponse().getContentAsString()).get("accessToken").asText();
    }

    private String refreshCookieValue(MvcResult result) {
        String setCookie = result.getResponse().getHeader(HttpHeaders.SET_COOKIE);
        Matcher matcher = REFRESH_COOKIE.matcher(setCookie == null ? "" : setCookie);
        assertTrue(matcher.find(), "response should contain the refresh cookie");
        return matcher.group(1);
    }
}
