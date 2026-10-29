package com.teamcanvas.invitation;

import jakarta.persistence.LockModeType;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface InvitationRepository extends JpaRepository<WorkspaceInvitation, Long> {

    /** The (at most one) PENDING invitation of a user for a workspace; used for the duplicate check. */
    Optional<WorkspaceInvitation> findByWorkspaceIdAndInviteeIdAndStatus(Long workspaceId, Long inviteeId,
                                                                          InvitationStatus status);

    /**
     * Loads the invitation only if it belongs to this user, and locks the row (SELECT ... FOR UPDATE) so that two
     * simultaneous accept/reject requests cannot both succeed.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<WorkspaceInvitation> findByIdAndInviteeId(Long id, Long inviteeId);

    Optional<WorkspaceInvitation> findByIdAndWorkspaceId(Long id, Long workspaceId);

    /** Invitations a user can still act on. Everything needed for the response is loaded in ONE query. */
    @Query("select i from WorkspaceInvitation i "
            + "join fetch i.workspace join fetch i.invitedBy join fetch i.invitee "
            + "where i.invitee.id = :userId and i.status = :status and i.expiresAt > :now "
            + "order by i.createdAt desc")
    List<WorkspaceInvitation> findActiveForInvitee(@Param("userId") Long userId,
                                                   @Param("status") InvitationStatus status,
                                                   @Param("now") Instant now);

    /** Open invitations of one workspace (for owners/admins). */
    @Query("select i from WorkspaceInvitation i "
            + "join fetch i.workspace join fetch i.invitedBy join fetch i.invitee "
            + "where i.workspace.id = :workspaceId and i.status = :status and i.expiresAt > :now "
            + "order by i.createdAt desc")
    List<WorkspaceInvitation> findActiveForWorkspace(@Param("workspaceId") Long workspaceId,
                                                     @Param("status") InvitationStatus status,
                                                     @Param("now") Instant now);
}
