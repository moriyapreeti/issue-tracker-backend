package com.tracker.dto;

import java.time.Instant;
import java.util.List;

public record ProjectDto(Long id, String name, String key, String description, UserDto owner,
                         List<UserDto> members, long ticketCount, boolean canManage, Instant createdAt) {
}
