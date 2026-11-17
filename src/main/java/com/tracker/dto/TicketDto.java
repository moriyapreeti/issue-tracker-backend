package com.tracker.dto;

import com.tracker.entity.IssueType;
import com.tracker.entity.Priority;
import com.tracker.entity.Status;

import java.time.Instant;

public record TicketDto(Long id, String title, String description, Status status, Priority priority,
                        IssueType type, Long projectId, String projectName, String projectKey,
                        UserDto reporter, UserDto assignee, String attachmentUrl,
                        boolean canEdit, boolean canDelete, Instant createdAt, Instant updatedAt) {
}
