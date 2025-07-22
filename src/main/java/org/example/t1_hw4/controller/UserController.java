package org.example.t1_hw4.controller;

import jakarta.validation.Valid;
import org.example.t1_hw4.dto.LoginDTO;
import org.example.t1_hw4.dto.RefreshDTO;
import org.example.t1_hw4.dto.RegisterDTO;
import org.example.t1_hw4.jwt.JwtTokenProvider;
import org.example.t1_hw4.mapper.UserMapper;
import org.example.t1_hw4.model.User;
import org.example.t1_hw4.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@RestController
public class UserController {
    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private UserMapper userMapper;
    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody @Valid RegisterDTO dto,
                                                 BindingResult bindingResult) {
        if (bindingResult.hasErrors()) {
            List<String> errors = getErrors(bindingResult);
            return ResponseEntity.badRequest().body(errors);
        }

        if (userRepository.findByLogin(dto.getLogin()).isPresent()) {
            List<String> errors = new ArrayList<>() {{
                add("login: this login is taken");
            }};
            return ResponseEntity.status(409)
                    .body(errors);
        }

        User user = userMapper.toUser(dto);
        user.setPasswordDigest(passwordEncoder.encode(dto.getPassword()));
        userRepository.save(user);

        return ResponseEntity.status(HttpStatus.CREATED)
                .contentType(MediaType.APPLICATION_JSON)
                .body(userMapper.toUserDTO(user));
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody @Valid LoginDTO dto,
                                   BindingResult bindingResult) {
        if (bindingResult.hasErrors()) {
            List<String> errors = getErrors(bindingResult);
            return ResponseEntity.badRequest().body(errors);
        }

        Optional<User> user = userRepository.findByLogin(dto.getLogin());

        if (user.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(List.of("User doesn't exist"));
        }

        if (!passwordEncoder.matches(dto.getPassword(), user.get().getPasswordDigest())) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(List.of("Invalid password"));
        }

        User authenticatedUser = user.get();

        String accessToken = jwtTokenProvider.generateAccessToken(authenticatedUser.getLogin());
        String refreshToken = jwtTokenProvider.generateRefreshToken(authenticatedUser.getLogin());

        return ResponseEntity.ok()
                .body(Map.of(
                        "username", authenticatedUser.getLogin(),
                        "role", authenticatedUser.getRole(),
                        "accessToken", accessToken,
                        "refreshToken", refreshToken
                ));
    }

    @PostMapping("/refresh")
    public ResponseEntity<?> refreshToken(@RequestBody RefreshDTO dto) {
        String refreshToken = dto.getRefreshToken();

        if (!jwtTokenProvider.validateToken(refreshToken)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid refresh token");
        }

        String login = jwtTokenProvider.getUsernameFromToken(refreshToken);

        Optional<User> userOptional = userRepository.findByLogin(login);
        if (userOptional.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("User not found");
        }

        String newAccessToken = jwtTokenProvider.generateAccessToken(login);
        return ResponseEntity.ok(Map.of("accessToken", newAccessToken));
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout(@RequestHeader(HttpHeaders.AUTHORIZATION) String authHeader) {
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);
            jwtTokenProvider.blacklistToken(token);
        }
        return ResponseEntity.ok(Map.of("message", "Successfully logged out"));
    }


    private List<String> getErrors(BindingResult bindingResult) {
        List<String> errors = bindingResult.getFieldErrors()
                .stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .collect(Collectors.toList());

        return errors;
    }
}
