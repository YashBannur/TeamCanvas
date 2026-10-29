package com.teamcanvas.comment;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TaskCommentRepository extends JpaRepository<TaskComment, Long> {

    @EntityGraph(attributePaths = "author")
    List<TaskComment> findAllByTask_IdOrderByCreatedAtAscIdAsc(Long taskId);

    @Query("""
            select c
            from TaskComment c
            join fetch c.author
            join fetch c.task t
            join fetch t.project p
            join fetch p.workspace
            where c.id = :commentId
            """)
    Optional<TaskComment> findDetailedById(@Param("commentId") Long commentId);
}