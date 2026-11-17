package com.tracker.service;

import com.tracker.dto.UserDto;
import com.tracker.entity.User;

public final class Mapper {
    private Mapper() {}

    public static UserDto toUser(User u) {
        return u == null ? null : new UserDto(u.getId(), u.getName(), u.getEmail(), u.getRole());
    }
}
