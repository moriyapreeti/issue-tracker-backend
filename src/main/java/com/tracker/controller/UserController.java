package com.tracker.controller;

import com.tracker.dto.RoleUpdateRequest;
import com.tracker.dto.UserDto;
import com.tracker.service.UserService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/users")
@Tag(name = "Users")
public class UserController {
    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/me")
    public UserDto me() {
        return userService.me();
    }

    @GetMapping
    public List<UserDto> list() {
        return userService.list();
    }

    @PatchMapping("/{id}/role")
    @PreAuthorize("hasRole('ADMIN')")
    public UserDto updateRole(@PathVariable Long id, @Valid @RequestBody RoleUpdateRequest req) {
        return userService.updateRole(id, req.role());
    }
}
