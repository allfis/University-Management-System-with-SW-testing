package com.example.ums.service;

import com.example.ums.dto.UserRequest;
import com.example.ums.dto.UserResponse;
import com.example.ums.exception.BadRequestException;
import com.example.ums.exception.ConflictException;
import com.example.ums.exception.NotFoundException;
import com.example.ums.model.Role;
import com.example.ums.model.User;
import com.example.ums.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;


@Service
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional(readOnly = true)
    public List<UserResponse> findAll(Role role) {
        return userRepository.findAllByRole(role).stream().map(UserResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public UserResponse findById(Role role, Long id) {
        return UserResponse.from(getEntity(role, id));
    }

    @Transactional(readOnly = true)
    public UserResponse findByUsername(String username) {
        return userRepository.findByUsername(username)
                .map(UserResponse::from)
                .orElseThrow(() -> new NotFoundException("User not found"));
    }

    @Transactional
    public UserResponse create(Role role, UserRequest request) {
        if (request.password() == null || request.password().isBlank()) {
            throw new BadRequestException("Password is required");
        }
        String username = request.username().trim();
        String email = request.email().trim();
        if (userRepository.existsByUsername(username)) {
            throw new ConflictException("Username already taken");
        }
        if (userRepository.existsByEmail(email)) {
            throw new ConflictException("Email already in use");
        }

        User user = new User();
        user.setRole(role);
        user.setPassword(passwordEncoder.encode(request.password()));
        applyProfile(user, request);
        return UserResponse.from(userRepository.saveAndFlush(user));
    }

    @Transactional
    public UserResponse update(Role role, Long id, UserRequest request) {
        User user = getEntity(role, id);
        String username = request.username().trim();
        String email = request.email().trim();
        if (userRepository.existsByUsernameAndIdNot(username, id)) {
            throw new ConflictException("Username already taken");
        }
        if (userRepository.existsByEmailAndIdNot(email, id)) {
            throw new ConflictException("Email already in use");
        }

        if (request.password() != null && !request.password().isBlank()) {
            user.setPassword(passwordEncoder.encode(request.password()));
        }
        applyProfile(user, request);
        return UserResponse.from(userRepository.saveAndFlush(user));
    }

    @Transactional
    public void delete(Role role, Long id) {
        User user = getEntity(role, id);
        if (role == Role.ADMIN && userRepository.countByRole(Role.ADMIN) <= 1) {
            throw new ConflictException("Cannot delete the last remaining admin");
        }
        userRepository.delete(user);
    }

    private User getEntity(Role role, Long id) {
        return userRepository.findByIdAndRole(id, role)
                .orElseThrow(() -> new NotFoundException(role.name().toLowerCase() + " not found with id " + id));
    }

    /** Copies only the fields that make sense for the user's role. */
    private void applyProfile(User user, UserRequest r) {
        user.setUsername(r.username().trim());
        user.setFullName(r.fullName().trim());
        user.setEmail(r.email().trim());
        switch (user.getRole()) {
            case TEACHER -> {
                user.setDepartment(r.department());
                user.setDesignation(r.designation());
            }
            case STUDENT -> {
                user.setDepartment(r.department());
                user.setStudentIdNo(r.studentIdNo());
                user.setSemester(r.semester());
            }
            case ADMIN -> { /* no extra fields */ }
        }
    }
}
