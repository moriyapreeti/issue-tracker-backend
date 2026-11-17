package com.tracker.repository;

import com.tracker.entity.Comment;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface CommentRepository extends JpaRepository<Comment, Long> {

    @EntityGraph(attributePaths = {"author"})
    List<Comment> findByTicketIdOrderByCreatedAtAsc(Long ticketId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("delete from Comment c where c.ticket.id = :ticketId")
    void deleteAllByTicketId(@Param("ticketId") Long ticketId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("delete from Comment c where c.ticket.id in (select t.id from Ticket t where t.project.id = :projectId)")
    void deleteAllByProjectId(@Param("projectId") Long projectId);
}
