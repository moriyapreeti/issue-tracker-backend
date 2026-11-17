package com.tracker.repository;

import com.tracker.entity.Project;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ProjectRepository extends JpaRepository<Project, Long> {
    boolean existsByProjectKey(String projectKey);

    List<Project> findAllByOrderByCreatedAtDesc();

    @Query("select p from Project p join p.members m where m.id = :userId order by p.createdAt desc")
    List<Project> findByMember(@Param("userId") Long userId);
}
