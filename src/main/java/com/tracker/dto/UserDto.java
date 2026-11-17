package com.tracker.dto;

import com.tracker.entity.Role;

public record UserDto(Long id, String name, String email, Role role) {
}
