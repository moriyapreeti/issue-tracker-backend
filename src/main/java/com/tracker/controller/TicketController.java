package com.tracker.controller;

import com.tracker.dto.*;
import com.tracker.entity.IssueType;
import com.tracker.entity.Priority;
import com.tracker.entity.Status;
import com.tracker.service.TicketService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/tickets")
@Tag(name = "Tickets")
public class TicketController {
    private final TicketService ticketService;

    public TicketController(TicketService ticketService) {
        this.ticketService = ticketService;
    }

    /** Filter + search + pagination. Example: /api/tickets?projectId=1&status=TODO&q=login&page=0&size=20&sort=createdAt,desc */
    @GetMapping
    public PageResponse<TicketDto> search(
            @RequestParam(required = false) Long projectId,
            @RequestParam(required = false) Status status,
            @RequestParam(required = false) Priority priority,
            @RequestParam(required = false) IssueType type,
            @RequestParam(required = false) Long assigneeId,
            @RequestParam(required = false) Long reporterId,
            @RequestParam(required = false) String q,
            @RequestParam(defaultValue = "false") boolean mine,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return ticketService.search(new TicketFilter(projectId, status, priority, type, assigneeId, reporterId, q, mine), pageable);
    }

    @GetMapping("/{id}")
    public TicketDto get(@PathVariable Long id) {
        return ticketService.get(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public TicketDto create(@Valid @RequestBody TicketRequest req) {
        return ticketService.create(req);
    }

    @PutMapping("/{id}")
    public TicketDto update(@PathVariable Long id, @Valid @RequestBody TicketRequest req) {
        return ticketService.update(id, req);
    }

    @PatchMapping("/{id}/status")
    public TicketDto updateStatus(@PathVariable Long id, @Valid @RequestBody StatusUpdateRequest req) {
        return ticketService.updateStatus(id, req.status());
    }

    @PatchMapping("/{id}/assign")
    public TicketDto assign(@PathVariable Long id, @RequestBody AssignRequest req) {
        return ticketService.assign(id, req.assigneeId());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        ticketService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping(value = "/{id}/attachment", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public TicketDto upload(@PathVariable Long id, @RequestParam("file") MultipartFile file) {
        return ticketService.uploadAttachment(id, file);
    }

    @DeleteMapping("/{id}/attachment")
    public TicketDto removeAttachment(@PathVariable Long id) {
        return ticketService.removeAttachment(id);
    }
}
