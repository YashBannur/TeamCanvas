package com.teamcanvas.workspace;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface WorkspaceMemberRepository extends JpaRepository<WorkspaceMember, Long> {

    Optional<WorkspaceMember> findByWorkspaceIdAndUserId(Long workspaceId, Long userId);

    /** All memberships of one user, with the workspace loaded in the same query (no N+1). */
    @Query("select m from WorkspaceMember m join fetch m.workspace w where m.user.id = :userId order by w.name")
    List<WorkspaceMember> findAllByUserIdWithWorkspace(@Param("userId") Long userId);

    /** All members of one workspace, with the user loaded in the same query (no N+1). */
    @Query("select m from WorkspaceMember m join fetch m.user where m.workspace.id = :workspaceId order by m.joinedAt")
    List<WorkspaceMember> findAllByWorkspaceIdWithUser(@Param("workspaceId") Long workspaceId);

    long countByWorkspaceId(Long workspaceId);

    /** Member counts for many workspaces in ONE query. */
    @Query("select m.workspace.id as workspaceId, count(m) as memberCount "
            + "from WorkspaceMember m where m.workspace.id in :ids group by m.workspace.id")
    List<MemberCount> countByWorkspaceIds(@Param("ids") Collection<Long> ids);

    interface MemberCount {
        Long getWorkspaceId();

        Long getMemberCount();
    }
}
