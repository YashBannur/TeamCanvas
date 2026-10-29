package com.teamcanvas.project;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ProjectRepository extends JpaRepository<Project, Long> {

    /** One project with its creator loaded in the same query. */
    @Query("select p from Project p join fetch p.createdBy where p.id = :id")
    Optional<Project> findDetailedById(@Param("id") Long id);

    /** Projects of one workspace with their creators loaded in the same query (no N+1). */
    @Query("select p from Project p join fetch p.createdBy "
            + "where p.workspace.id = :workspaceId and p.status = :status order by p.name")
    List<Project> findByWorkspaceAndStatus(@Param("workspaceId") Long workspaceId,
                                           @Param("status") ProjectStatus status);
}
