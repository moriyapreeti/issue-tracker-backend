package com.tracker.service;

import com.tracker.dto.CommentDto;
import com.tracker.dto.CommentRequest;
import com.tracker.entity.Comment;
import com.tracker.entity.Ticket;
import com.tracker.entity.User;
import com.tracker.exception.ResourceNotFoundException;
import com.tracker.repository.CommentRepository;
import com.tracker.repository.TicketRepository;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class CommentService {
    private final CommentRepository commentRepository;
    private final TicketRepository ticketRepository;
    private final CurrentUserService currentUser;
    private final AccessService access;

    public CommentService(CommentRepository commentRepository, TicketRepository ticketRepository,
                          CurrentUserService currentUser, AccessService access) {
        this.commentRepository = commentRepository;
        this.ticketRepository = ticketRepository;
        this.currentUser = currentUser;
        this.access = access;
    }

    @Transactional(readOnly = true)
    public List<CommentDto> list(Long ticketId) {
        User u = currentUser.get();
        Ticket t = findTicket(ticketId);
        access.requireAccess(t.getProject(), u);
        return commentRepository.findByTicketIdOrderByCreatedAtAsc(ticketId).stream()
                .map(c -> toDto(c, u, t)).toList();
    }

    @Transactional
    public CommentDto add(Long ticketId, CommentRequest req) {
        User u = currentUser.get();
        Ticket t = findTicket(ticketId);
        access.requireAccess(t.getProject(), u);
        Comment c = new Comment();
        c.setContent(req.content().trim());
        c.setTicket(t);
        c.setAuthor(u);
        return toDto(commentRepository.save(c), u, t);
    }

    @Transactional
    public void delete(Long commentId) {
        User u = currentUser.get();
        Comment c = commentRepository.findById(commentId)
                .orElseThrow(() -> new ResourceNotFoundException("Comment not found"));
        if (!canDelete(c, u, c.getTicket())) throw new AccessDeniedException("Cannot delete this comment");
        commentRepository.delete(c);
    }

    private Ticket findTicket(Long id) {
        return ticketRepository.findDetailedById(id).orElseThrow(() -> new ResourceNotFoundException("Ticket not found"));
    }

    private boolean canDelete(Comment c, User u, Ticket t) {
        return c.getAuthor().getId().equals(u.getId()) || access.canManage(t.getProject(), u);
    }

    private CommentDto toDto(Comment c, User viewer, Ticket t) {
        return new CommentDto(c.getId(), c.getContent(), Mapper.toUser(c.getAuthor()), t.getId(),
                canDelete(c, viewer, t), c.getCreatedAt());
    }
}
