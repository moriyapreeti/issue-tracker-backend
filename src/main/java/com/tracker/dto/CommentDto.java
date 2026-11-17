package com.tracker.dto;

import java.time.Instant;

public record CommentDto(Long id, String content, UserDto author, Long ticketId, boolean canDelete, Instant createdAt) {
}
