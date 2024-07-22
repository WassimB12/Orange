package com.example.orange.controllers;

import com.example.orange.entities.User;
import com.example.orange.repository.UserRepository;
import com.example.orange.services.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@Controller
public class UserController {

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private UserService userService;
    @Autowired
    private UserRepository userRepository;

    @PostMapping("/login")
    public ResponseEntity<?> authenticateUser(@RequestBody User user) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(user.getEmail(), user.getPassword())
        );

        SecurityContextHolder.getContext().setAuthentication(authentication);
        // Here you would generate a JWT token and return it
        return ResponseEntity.ok("User logged in successfully");
    }

    @PostMapping("/register")
    public ResponseEntity<?> registerUser(@RequestBody User user) {

        userService.save(user);
        return ResponseEntity.ok("User registered successfully");

    }
}
  /*  @PostMapping("/register")
    public ResponseEntity<?> registerUser(@RequestBody User user) {
        if (userRepository.findByEmail(user.getEmail()).isPresent()) {
            return ResponseEntity.ok("User mail already existed");
        } else {
            userService.save(user);
            return ResponseEntity.ok("User registered successfully");
        }
    }*/


