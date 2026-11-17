package com.tracker.dto;

import com.tracker.entity.IssueType;
import com.tracker.entity.Priority;
import com.tracker.entity.Status;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record TicketRequest(
        @NotBlank @Size(max = 200) String title,
        String description,
        Status status,
        Priority priority,
        IssueType type,
        @NotNull Long projectId,
        Long assigneeId) {
}
