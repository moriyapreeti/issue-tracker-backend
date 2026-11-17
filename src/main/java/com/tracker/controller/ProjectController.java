package com.tracker.controller;

import com.tracker.dto.ProjectDto;
import com.tracker.dto.ProjectRequest;
import com.tracker.service.ProjectService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/projects")
@Tag(name = "Projects")
public class ProjectController {
    private final ProjectService projectService;

    public ProjectController(ProjectService projectService) {
        this.projectService = projectService;
    }

    @GetMapping
    public List<ProjectDto> list() {
        return projectService.list();
    }

    @GetMapping("/{id}")
    public ProjectDto get(@PathVariable Long id) {
        return projectService.get(id);
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    @ResponseStatus(HttpStatus.CREATED)
    public ProjectDto create(@Valid @RequestBody ProjectRequest req) {
        return projectService.create(req);
    }

    @PutMapping("/{id}")
    public ProjectDto update(@PathVariable Long id, @Valid @RequestBody ProjectRequest req) {
        return projectService.update(id, req);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        projectService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/members/{userId}")
    public ProjectDto addMember(@PathVariable Long id, @PathVariable Long userId) {
        return projectService.addMember(id, userId);
    }

    @DeleteMapping("/{id}/members/{userId}")
    public ProjectDto removeMember(@PathVariable Long id, @PathVariable Long userId) {
        return projectService.removeMember(id, userId);
    }
}
