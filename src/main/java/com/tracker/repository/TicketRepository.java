package com.tracker.repository;

import com.tracker.entity.Ticket;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface TicketRepository extends JpaRepository<Ticket, Long>, JpaSpecificationExecutor<Ticket> {

    // Fetch join graph avoids N+1 queries when the list is mapped to DTOs
    @Override
    @EntityGraph(attributePaths = {"project", "project.owner", "reporter", "assignee"})
    Page<Ticket> findAll(Specification<Ticket> spec, Pageable pageable);

    @EntityGraph(attributePaths = {"project", "project.owner", "reporter", "assignee"})
    @Query("select t from Ticket t where t.id = :id")
    Optional<Ticket> findDetailedById(@Param("id") Long id);

    long countByProjectId(Long projectId);

    List<Ticket> findByProjectId(Long projectId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("delete from Ticket t where t.project.id = :projectId")
    void deleteAllByProjectId(@Param("projectId") Long projectId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update Ticket t set t.assignee = null where t.project.id = :projectId and t.assignee.id = :userId")
    void unassignUser(@Param("projectId") Long projectId, @Param("userId") Long userId);
}
