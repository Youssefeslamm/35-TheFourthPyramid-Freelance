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

    public AuthController(UserRepository userRepository,
                          PasswordEncoder passwordEncoder,
                          JwtService jwtService,
                          MongoEventLogger mongoEventLogger) {

        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.mongoEventLogger = mongoEventLogger;

        this.observers.add(mongoEventLogger);
    }

    private void notifyObservers(String eventType, Object payload) {
        for (EntityObserver observer : observers) {
            observer.onEvent(eventType, payload);
        }
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Map.of("message", "Email already exists"));
        }


        User user = new User();
        user.setName(request.getName());
        user.setEmail(request.getEmail());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setPhone(request.getPhone());
        if (request.getRole() != null &&
                (request.getRole().name().equals("CLIENT") || request.getRole().name().equals("FREELANCER"))) {
            user.setRole(request.getRole());
        } else {
            user.setRole(Role.CLIENT);
        }
        user.setStatus(Status.ACTIVE);
        user.setPreferences(request.getPreferences());

        User savedUser = userRepository.save(user);
        String token = jwtService.generateToken(savedUser);

        Map<String, Object> payload = new HashMap<>();
        payload.put("action", "REGISTERED");
        payload.put("userId", savedUser.getId());
        payload.put("email", savedUser.getEmail());

        notifyObservers("REGISTERED", payload);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new AuthResponse(
                        token,
                        savedUser.getId(),
                        savedUser.getEmail(),
                        savedUser.getRole().name()
                ));
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody AuthRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElse(null);

        if (user == null || !passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("message", "Invalid email or password"));
        }

        String token = jwtService.generateToken(user);

        Map<String, Object> payload = new HashMap<>();
        payload.put("action", "LOGGED_IN");
        payload.put("userId", user.getId());
        payload.put("email", user.getEmail());

        notifyObservers("LOGGED_IN", payload);

        return ResponseEntity.ok(new AuthResponse(
                token,
                user.getId(),
                user.getEmail(),
                user.getRole().name()
        ));
    }
}