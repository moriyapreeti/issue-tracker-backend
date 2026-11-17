package com.tracker.dto;

/** Message pushed over WebSocket (/topic/projects/{id}) when a ticket changes. */
public record TicketEvent(String action, Long ticketId, Long projectId) {
}
