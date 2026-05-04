package com.team35.freelance.user.controller;

import com.team35.freelance.user.dto.AuthRequest;
import com.team35.freelance.user.dto.AuthResponse;
import com.team35.freelance.user.dto.RegisterRequest;
import com.team35.freelance.user.model.Status;
import com.team35.freelance.user.model.User;
import com.team35.freelance.user.repository.UserRepository;
import com.team35.freelance.user.security.JwtService;
import com.team35.freelance.user.common.observer.EntityObserver;
import com.team35.freelance.user.common.observer.MongoEventLogger;
import com.team35.freelance.user.model.Role;

import com.team35.freelance.user.service.UserService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    private final List<EntityObserver> observers = new ArrayList<>();
    private final MongoEventLogger mongoEventLogger;
    private final UserService userService;
    public AuthController(UserRepository userRepository,
                          PasswordEncoder passwordEncoder,
                          JwtService jwtService,
                          MongoEventLogger mongoEventLogger, UserService userService) {

        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.mongoEventLogger = mongoEventLogger;
        this.userService = userService;

        this.observers.add(mongoEventLogger);
    }

    private void notifyObservers(String eventType, Object payload) {
        for (EntityObserver observer : observers) {
            observer.onEvent(eventType, payload);
        }
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody RegisterRequest request) {
        User savedUser = userService.registerUser(request);
        String token = jwtService.generateToken(savedUser);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new AuthResponse(
                        token,
                        jwtService.getExpiration(),
                        savedUser.getId(),
                        savedUser.getEmail(),
                        savedUser.getRole().name()
                ));
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@RequestBody AuthRequest request) {
        return ResponseEntity.ok(userService.login(request));
    }
}