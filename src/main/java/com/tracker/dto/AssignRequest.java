package com.tracker.dto;

/** assigneeId == null un-assigns the ticket. */
public record AssignRequest(Long assigneeId) {
}
