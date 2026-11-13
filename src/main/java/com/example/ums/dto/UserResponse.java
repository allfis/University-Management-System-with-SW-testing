package com.example.ums.dto;

import com.example.ums.model.Role;
import com.example.ums.model.User;
import com.fasterxml.jackson.annotation.JsonInclude;

/** What the API returns. The password (hash) is never exposed. */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record UserResponse(
        Long id,
        String username,
        String fullName,
        String email,
        Role role,
        String department,
        String designation,
        String studentIdNo,
        Integer semester
) {
    public static UserResponse from(User u) {
        return new UserResponse(
                u.getId(),
                u.getUsername(),
                u.getFullName(),
                u.getEmail(),
                u.getRole(),
                u.getDepartment(),
                u.getDesignation(),
                u.getStudentIdNo(),
                u.getSemester());
    }
}
