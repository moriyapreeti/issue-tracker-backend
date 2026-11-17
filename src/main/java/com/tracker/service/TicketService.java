package com.tracker.service;

import com.tracker.dto.*;
import com.tracker.entity.*;
import com.tracker.exception.BadRequestException;
import com.tracker.exception.ResourceNotFoundException;
import com.tracker.repository.CommentRepository;
import com.tracker.repository.ProjectRepository;
import com.tracker.repository.TicketRepository;
import com.tracker.repository.UserRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.Set;

@Service
public class TicketService {
    private static final Set<String> SORTABLE = Set.of("createdAt", "updatedAt", "title", "status", "priority", "id");

    private final TicketRepository ticketRepository;
    private final ProjectRepository projectRepository;
    private final UserRepository userRepository;
    private final CommentRepository commentRepository;
    private final CurrentUserService currentUser;
    private final AccessService access;
    private final FileStorageService files;
    private final SimpMessagingTemplate messaging;

    public TicketService(TicketRepository ticketRepository, ProjectRepository projectRepository,
                         UserRepository userRepository, CommentRepository commentRepository,
                         CurrentUserService currentUser, AccessService access,
                         FileStorageService files, SimpMessagingTemplate messaging) {
        this.ticketRepository = ticketRepository;
        this.projectRepository = projectRepository;
        this.userRepository = userRepository;
        this.commentRepository = commentRepository;
        this.currentUser = currentUser;
        this.access = access;
        this.files = files;
        this.messaging = messaging;
    }

    // ------------------------------------------------------------ queries

    @Transactional(readOnly = true)
    public PageResponse<TicketDto> search(TicketFilter filter, Pageable pageable) {
        User u = currentUser.get();
        Long assignee = filter.mine() ? u.getId() : filter.assigneeId();
        TicketFilter f = new TicketFilter(filter.projectId(), filter.status(), filter.priority(), filter.type(),
                assignee, filter.reporterId(), filter.q(), filter.mine());

        if (f.projectId() != null) {
            Project p = projectRepository.findById(f.projectId())
                    .orElseThrow(() -> new ResourceNotFoundException("Project not found"));
            access.requireAccess(p, u);
        }
        var page = ticketRepository.findAll(TicketSpecifications.build(f, u.getId(), access.isAdmin(u)), sanitize(pageable));
        return PageResponse.of(page.map(t -> toDto(t, u)));
    }

    @Transactional(readOnly = true)
    public TicketDto get(Long id) {
        User u = currentUser.get();
        Ticket t = find(id);
        access.requireAccess(t.getProject(), u);
        return toDto(t, u);
    }

    // ------------------------------------------------------------ commands

    @Transactional
    public TicketDto create(TicketRequest req) {
        User u = currentUser.get();
        Project p = projectRepository.findById(req.projectId())
                .orElseThrow(() -> new ResourceNotFoundException("Project not found"));
        access.requireAccess(p, u);

        Ticket t = new Ticket();
        t.setTitle(req.title().trim());
        t.setDescription(req.description());
        t.setStatus(req.status() != null ? req.status() : Status.TODO);
        t.setPriority(req.priority() != null ? req.priority() : Priority.MEDIUM);
        t.setType(req.type() != null ? req.type() : IssueType.TASK);
        t.setProject(p);
        t.setReporter(u);
        t.setAssignee(resolveAssignee(p, req.assigneeId()));
        t = ticketRepository.save(t);
        publish("CREATED", t);
        return toDto(t, u);
    }

    @Transactional
    public TicketDto update(Long id, TicketRequest req) {
        User u = currentUser.get();
        Ticket t = find(id);
        if (!access.canEditTicket(t, u)) throw new AccessDeniedException("Cannot edit this ticket");

        t.setTitle(req.title().trim());
        t.setDescription(req.description());
        if (req.status() != null) t.setStatus(req.status());
        if (req.priority() != null) t.setPriority(req.priority());
        if (req.type() != null) t.setType(req.type());
        t.setAssignee(resolveAssignee(t.getProject(), req.assigneeId()));
        publish("UPDATED", t);
        return toDto(t, u);
    }

    @Transactional
    public TicketDto updateStatus(Long id, Status status) {
        User u = currentUser.get();
        Ticket t = find(id);
        access.requireAccess(t.getProject(), u);   // any project member may move cards on the board
        t.setStatus(status);
        publish("MOVED", t);
        return toDto(t, u);
    }

    @Transactional
    public TicketDto assign(Long id, Long assigneeId) {
        User u = currentUser.get();
        Ticket t = find(id);
        if (!access.canEditTicket(t, u)) throw new AccessDeniedException("Cannot assign this ticket");
        t.setAssignee(resolveAssignee(t.getProject(), assigneeId));
        publish("ASSIGNED", t);
        return toDto(t, u);
    }

    @Transactional
    public void delete(Long id) {
        User u = currentUser.get();
        Ticket t = find(id);
        if (!access.canDeleteTicket(t, u)) throw new AccessDeniedException("Cannot delete this ticket");
        files.delete(t.getAttachment());
        commentRepository.deleteAllByTicketId(id);
        publish("DELETED", t);
        ticketRepository.delete(t);
    }

    @Transactional
    public TicketDto uploadAttachment(Long id, MultipartFile file) {
        User u = currentUser.get();
        Ticket t = find(id);
        if (!access.canEditTicket(t, u)) throw new AccessDeniedException("Cannot edit this ticket");
        String old = t.getAttachment();
        t.setAttachment(files.store(file));
        files.delete(old);
        publish("UPDATED", t);
        return toDto(t, u);
    }

    @Transactional
    public TicketDto removeAttachment(Long id) {
        User u = currentUser.get();
        Ticket t = find(id);
        if (!access.canEditTicket(t, u)) throw new AccessDeniedException("Cannot edit this ticket");
        files.delete(t.getAttachment());
        t.setAttachment(null);
        publish("UPDATED", t);
        return toDto(t, u);
    }

    // ------------------------------------------------------------ helpers

    private Ticket find(Long id) {
        return ticketRepository.findDetailedById(id).orElseThrow(() -> new ResourceNotFoundException("Ticket not found"));
    }

    private User resolveAssignee(Project p, Long assigneeId) {
        if (assigneeId == null) return null;
        User a = userRepository.findById(assigneeId).orElseThrow(() -> new BadRequestException("Assignee not found"));
        if (!p.hasMember(a.getId())) throw new BadRequestException("Assignee must be a member of the project");
        return a;
    }

    private Pageable sanitize(Pageable p) {
        Sort sort = Sort.by(p.getSort().stream().filter(o -> SORTABLE.contains(o.getProperty())).toList());
        if (sort.isUnsorted()) sort = Sort.by(Sort.Direction.DESC, "createdAt");
        sort = sort.and(Sort.by(Sort.Direction.DESC, "id"));
        return PageRequest.of(p.getPageNumber(), Math.min(Math.max(p.getPageSize(), 1), 500), sort);
    }

    private void publish(String action, Ticket t) {
        Long projectId = t.getProject().getId();
        messaging.convertAndSend("/topic/projects/" + projectId, new TicketEvent(action, t.getId(), projectId));
    }

    private TicketDto toDto(Ticket t, User viewer) {
        Project p = t.getProject();
        return new TicketDto(t.getId(), t.getTitle(), t.getDescription(), t.getStatus(), t.getPriority(), t.getType(),
                p.getId(), p.getName(), p.getProjectKey(), Mapper.toUser(t.getReporter()), Mapper.toUser(t.getAssignee()),
                t.getAttachment() == null ? null : "/api/files/" + t.getAttachment(),
                access.canEditTicket(t, viewer), access.canDeleteTicket(t, viewer),
                t.getCreatedAt(), t.getUpdatedAt());
    }
}
