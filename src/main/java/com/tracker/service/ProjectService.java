package com.tracker.service;

import com.tracker.dto.ProjectDto;
import com.tracker.dto.ProjectRequest;
import com.tracker.dto.UserDto;
import com.tracker.entity.Project;
import com.tracker.entity.Ticket;
import com.tracker.entity.User;
import com.tracker.exception.BadRequestException;
import com.tracker.exception.ResourceNotFoundException;
import com.tracker.repository.CommentRepository;
import com.tracker.repository.ProjectRepository;
import com.tracker.repository.TicketRepository;
import com.tracker.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;

@Service
public class ProjectService {
    private final ProjectRepository projectRepository;
    private final UserRepository userRepository;
    private final TicketRepository ticketRepository;
    private final CommentRepository commentRepository;
    private final CurrentUserService currentUser;
    private final AccessService access;
    private final FileStorageService files;

    public ProjectService(ProjectRepository projectRepository, UserRepository userRepository,
                          TicketRepository ticketRepository, CommentRepository commentRepository,
                          CurrentUserService currentUser, AccessService access, FileStorageService files) {
        this.projectRepository = projectRepository;
        this.userRepository = userRepository;
        this.ticketRepository = ticketRepository;
        this.commentRepository = commentRepository;
        this.currentUser = currentUser;
        this.access = access;
        this.files = files;
    }

    @Transactional(readOnly = true)
    public List<ProjectDto> list() {
        User u = currentUser.get();
        List<Project> projects = access.isAdmin(u)
                ? projectRepository.findAllByOrderByCreatedAtDesc()
                : projectRepository.findByMember(u.getId());
        return projects.stream().map(p -> toDto(p, u)).toList();
    }

    @Transactional(readOnly = true)
    public ProjectDto get(Long id) {
        User u = currentUser.get();
        Project p = find(id);
        access.requireAccess(p, u);
        return toDto(p, u);
    }

    @Transactional
    public ProjectDto create(ProjectRequest req) {
        User u = currentUser.get();
        String key = req.key() != null && !req.key().isBlank() ? req.key().toUpperCase() : generateKey(req.name());
        if (projectRepository.existsByProjectKey(key)) throw new BadRequestException("Project key '" + key + "' is already used");
        Project p = new Project();
        p.setName(req.name().trim());
        p.setProjectKey(key);
        p.setDescription(req.description());
        p.setOwner(u);
        p.getMembers().add(u);
        return toDto(projectRepository.save(p), u);
    }

    @Transactional
    public ProjectDto update(Long id, ProjectRequest req) {
        User u = currentUser.get();
        Project p = find(id);
        access.requireManage(p, u);
        p.setName(req.name().trim());
        p.setDescription(req.description());
        return toDto(p, u);
    }

    @Transactional
    public void delete(Long id) {
        User u = currentUser.get();
        Project p = find(id);
        access.requireManage(p, u);
        for (Ticket t : ticketRepository.findByProjectId(id)) files.delete(t.getAttachment());
        commentRepository.deleteAllByProjectId(id);
        ticketRepository.deleteAllByProjectId(id);
        projectRepository.deleteById(id);
    }

    @Transactional
    public ProjectDto addMember(Long projectId, Long userId) {
        User me = currentUser.get();
        Project p = find(projectId);
        access.requireManage(p, me);
        User member = userRepository.findById(userId).orElseThrow(() -> new ResourceNotFoundException("User not found"));
        p.getMembers().add(member);
        return toDto(p, me);
    }

    @Transactional
    public ProjectDto removeMember(Long projectId, Long userId) {
        User me = currentUser.get();
        Project p = find(projectId);
        access.requireManage(p, me);
        if (p.getOwner().getId().equals(userId)) throw new BadRequestException("The project owner cannot be removed");
        p.getMembers().removeIf(m -> m.getId().equals(userId));
        ticketRepository.unassignUser(projectId, userId);
        return toDto(p, me);
    }

    private Project find(Long id) {
        return projectRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Project not found"));
    }

    private String generateKey(String name) {
        String letters = name.replaceAll("[^A-Za-z]", "").toUpperCase();
        String base = letters.length() >= 3 ? letters.substring(0, Math.min(4, letters.length())) : "PRJ";
        String key = base;
        int i = 2;
        while (projectRepository.existsByProjectKey(key)) key = base + i++;
        return key;
    }

    private ProjectDto toDto(Project p, User viewer) {
        List<UserDto> members = p.getMembers().stream()
                .sorted(Comparator.comparing(m -> m.getName().toLowerCase()))
                .map(Mapper::toUser).toList();
        return new ProjectDto(p.getId(), p.getName(), p.getProjectKey(), p.getDescription(),
                Mapper.toUser(p.getOwner()), members, ticketRepository.countByProjectId(p.getId()),
                access.canManage(p, viewer), p.getCreatedAt());
    }
}
