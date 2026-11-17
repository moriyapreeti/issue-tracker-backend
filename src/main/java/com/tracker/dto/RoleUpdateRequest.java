package com.tracker.dto;

import com.tracker.entity.Role;
import jakarta.validation.constraints.NotNull;

public record RoleUpdateRequest(@NotNull Role role) {
}
