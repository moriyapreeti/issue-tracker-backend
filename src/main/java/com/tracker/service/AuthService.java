package com.tracker.service;

import com.tracker.dto.AuthResponse;
import com.tracker.dto.LoginRequest;
import com.tracker.dto.RegisterRequest;
import com.tracker.entity.Role;
import com.tracker.entity.User;
import com.tracker.exception.BadRequestException;
import com.tracker.repository.UserRepository;
import com.tracker.security.JwtUtil;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtUtil jwtUtil;

    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder,
                       AuthenticationManager authenticationManager, JwtUtil jwtUtil) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.jwtUtil = jwtUtil;
    }

    public AuthResponse register(RegisterRequest req) {
        String email = req.email().trim().toLowerCase();
        if (userRepository.existsByEmail(email)) throw new BadRequestException("Email is already registered");
        // Public sign-up always creates a DEVELOPER; an admin can promote later.
        User user = userRepository.save(new User(req.name().trim(), email, passwordEncoder.encode(req.password()), Role.DEVELOPER));
        return new AuthResponse(jwtUtil.generateToken(user.getEmail()), Mapper.toUser(user));
    }

    public AuthResponse login(LoginRequest req) {
        String email = req.email().trim().toLowerCase();
        authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(email, req.password()));
        User user = userRepository.findByEmail(email).orElseThrow();
        return new AuthResponse(jwtUtil.generateToken(user.getEmail()), Mapper.toUser(user));
    }
}
