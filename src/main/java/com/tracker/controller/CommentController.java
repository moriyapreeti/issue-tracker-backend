package com.tracker.controller;

import com.tracker.dto.CommentDto;
import com.tracker.dto.CommentRequest;
import com.tracker.service.CommentService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
@Tag(name = "Comments")
public class CommentController {
    private final CommentService commentService;

    public CommentController(CommentService commentService) {
        this.commentService = commentService;
    }

    @GetMapping("/tickets/{ticketId}/comments")
    public List<CommentDto> list(@PathVariable Long ticketId) {
        return commentService.list(ticketId);
    }

    @PostMapping("/tickets/{ticketId}/comments")
    @ResponseStatus(HttpStatus.CREATED)
    public CommentDto add(@PathVariable Long ticketId, @Valid @RequestBody CommentRequest req) {
        return commentService.add(ticketId, req);
    }

    @DeleteMapping("/comments/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        commentService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
