package com.teamcanvas.notification;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.teamcanvas.support.ApiTestSupport;
import org.junit.jupiter.api.Test;

class NotificationIntegrationTest extends ApiTestSupport {

    @Test
    void invitationNotificationBelongsOnlyToInviteeAndCanBeMarkedRead() throws Exception {
        TestUser owner = newUser();
        TestUser invitee = newUser();
        long workspaceId = createWorkspace(owner);

        long invitationId = idOf(post(owner,
                "/api/workspaces/" + workspaceId + "/invitations",
                java.util.Map.of("email", invitee.email(), "role", "MEMBER"))
                .andExpect(status().isCreated()));

        get(invitee, "/api/notifications")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].type").value("INVITATION_RECEIVED"))
                .andExpect(jsonPath("$.content[0].entityId").value(invitationId))
                .andExpect(jsonPath("$.content[0].isRead").value(false));

        get(owner, "/api/notifications/unread-count")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.count").value(0));

        get(invitee, "/api/notifications/unread-count")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.count").value(1));

        patch(invitee, "/api/notifications/"
                + json(get(invitee, "/api/notifications")).get("content").get(0).get("id").asLong()
                + "/read", null)
                .andExpect(status().isNoContent());

        get(invitee, "/api/notifications/unread-count")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.count").value(0));

        patch(owner, "/api/notifications/"
                + json(get(invitee, "/api/notifications")).get("content").get(0).get("id").asLong()
                + "/read", null)
                .andExpect(status().isNotFound());
    }

    @Test
    void unreadOnlyReturnsOnlyUnreadNotifications() throws Exception {
        TestUser owner = newUser();
        TestUser invitee = newUser();
        long workspaceId = createWorkspace(owner);

        long invitationId = idOf(post(owner,
                "/api/workspaces/" + workspaceId + "/invitations",
                java.util.Map.of("email", invitee.email(), "role", "MEMBER"))
                .andExpect(status().isCreated()));

        long notificationId = json(get(invitee, "/api/notifications"))
                .get("content").get(0).get("id").asLong();

        patch(invitee, "/api/notifications/" + notificationId + "/read", null)
                .andExpect(status().isNoContent());

        get(invitee, "/api/notifications?unreadOnly=true")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(0));

        get(invitee, "/api/notifications?unreadOnly=false")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].entityId").value(invitationId));
    }
}