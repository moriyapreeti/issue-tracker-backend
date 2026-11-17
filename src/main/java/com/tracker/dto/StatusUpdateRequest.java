package com.tracker.dto;

import com.tracker.entity.Status;
import jakarta.validation.constraints.NotNull;

public record StatusUpdateRequest(@NotNull Status status) {
}
