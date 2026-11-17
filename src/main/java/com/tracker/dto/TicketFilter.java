package com.tracker.dto;

import com.tracker.entity.IssueType;
import com.tracker.entity.Priority;
import com.tracker.entity.Status;

public record TicketFilter(Long projectId, Status status, Priority priority, IssueType type,
                           Long assigneeId, Long reporterId, String q, boolean mine) {
}
