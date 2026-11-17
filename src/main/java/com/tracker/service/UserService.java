package com.tracker.service;

import com.tracker.dto.UserDto;
import com.tracker.entity.Role;
import com.tracker.entity.User;
import com.tracker.exception.BadRequestException;
import com.tracker.exception.ResourceNotFoundException;
import com.tracker.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;

@Service
public class UserService {
    private final UserRepository userRepository;
    private final CurrentUserService currentUser;

    public UserService(UserRepository userRepository, CurrentUserService currentUser) {
        this.userRepository = userRepository;
        this.currentUser = currentUser;
    }

    public UserDto me() {
        return Mapper.toUser(currentUser.get());
    }

    @Transactional(readOnly = true)
    public List<UserDto> list() {
        return userRepository.findAll().stream()
                .sorted(Comparator.comparing(u -> u.getName().toLowerCase()))
                .map(Mapper::toUser).toList();
    }

    @Transactional
    public UserDto updateRole(Long id, Role role) {
        User me = currentUser.get();
        User target = userRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("User not found"));
        if (target.getId().equals(me.getId())) throw new BadRequestException("You cannot change your own role");
        target.setRole(role);
        return Mapper.toUser(target);
    }
}
